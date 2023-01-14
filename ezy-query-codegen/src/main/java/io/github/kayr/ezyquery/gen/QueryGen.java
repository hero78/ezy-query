/* (C)2022 */
package io.github.kayr.ezyquery.gen;

import com.squareup.javapoet.*;
import io.github.kayr.ezyquery.EzyQuery;
import io.github.kayr.ezyquery.api.EzyCriteria;
import io.github.kayr.ezyquery.api.Field;
import io.github.kayr.ezyquery.parser.QueryAndParams;
import io.github.kayr.ezyquery.util.Elf;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

public class QueryGen {

  private final String sql;
  private final String className;
  private final String packageName;

  public QueryGen(String packageName, String className, String sql) {
    this.sql = sql;
    this.className = className;
    this.packageName = packageName;
  }

  public Path writeTo(String path) {
    return writeTo(Paths.get(path));
  }

  @lombok.SneakyThrows
  public Path writeTo(Path path) {
    JavaFile javaFile = javaCode();
    return javaFile.writeToPath(path);
  }

  public JavaFile javaCode() throws JSQLParserException {
    Statement statement = CCJSqlParserUtil.parse(sql);

    if (!(statement instanceof Select)) {
      throw new IllegalArgumentException("Only SELECT statements are supported");
    }

    Select select = (Select) statement;

    if (!(select.getSelectBody() instanceof PlainSelect)) {
      throw new IllegalArgumentException("Only SELECT statements are supported");
    }

    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
    List<Field<?>> fieldList = extractFields(plainSelect);

    return buildCode(fieldList, plainSelect);
  }

  private JavaFile buildCode(List<Field<?>> fieldList, PlainSelect plainSelect) {

    List<FieldSpec> fConstants = fieldConstants(fieldList);

    FieldSpec fSchema = fieldSchema(plainSelect);

    ClassName thisClassName = ClassName.get(packageName, className);
    FieldSpec fSingleton =
        FieldSpec.builder(thisClassName, "QUERY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T()", thisClassName)
            .build();

    FieldSpec fFields = fieldAllFields();

    MethodSpec mConstructor = methodConstructor();

    MethodSpec mInit = methodInit(fConstants, fFields);

    TypeSpec resultClass = resultClass(fieldList);
    ClassName resultClassName = ClassName.get(packageName, className, resultClass.name);

    // main query method
    MethodSpec queryMethod =
        MethodSpec.methodBuilder("query")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(EzyCriteria.class, "criteria")
            .addStatement("return $T.buildQueryAndParams(criteria, this)", EzyQuery.class)
            .returns(QueryAndParams.class)
            .build();

    // result class override method
    MethodSpec resultClassMethod =
        MethodSpec.methodBuilder("resultClass")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ClassName.get(Class.class), resultClassName))
            .addStatement("    return $T.class", resultClassName)
            .build();

    // fields override method
    MethodSpec fieldsMethod =
        MethodSpec.methodBuilder("fields")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(typeListOfFields())
            .addStatement("return this.fields")
            .build();

    // schema override method
    MethodSpec schemaMethod =
        MethodSpec.methodBuilder("schema")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addStatement("return this.schema")
            .build();

    // where override method
    MethodSpec whereMethod =
        MethodSpec.methodBuilder("whereClause")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(Optional.class, String.class))
            .addStatement(
                plainSelect.getWhere() == null
                    ? "return Optional.empty()"
                    : "return Optional.of($S)",
                plainSelect.getWhere())
            .build();

    // orderBy override method
    Optional<String> orderByElements = toOrderByStatement(plainSelect.getOrderByElements());
    MethodSpec orderByMethod =
        MethodSpec.methodBuilder("orderByClause")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(Optional.class, String.class))
            .addStatement(
                orderByElements.isPresent() ? "return Optional.of($S)" : "return Optional.empty()",
                orderByElements.orElse(""))
            .build();

    // the class

    ClassName generatedAnnotation = resolveGeneratedAnnotation();

    TypeSpec finalClazz =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(sql)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(EzyQuery.class), resultClassName))
            .addFields(fConstants)
            .addField(fSchema)
            .addField(fFields)
            .addField(fSingleton)
            .addMethod(mConstructor)
            .addMethod(mInit)
            .addMethod(queryMethod)
            .addMethod(schemaMethod)
            .addMethod(whereMethod)
            .addMethod(orderByMethod)
            .addAnnotation(
                AnnotationSpec.builder(generatedAnnotation)
                    .addMember("value", "$S", QueryGen.class.getName())
                    .addMember("date", "$S", timeStamp())
                    .build())
            .addMethod(fieldsMethod)
            .addMethod(resultClassMethod)
            .addType(resultClass)
            .build();

    return JavaFile.builder(packageName, finalClazz).build();
  }

  private static Optional<String> toOrderByStatement(List<OrderByElement> orderByElements1) {
    if (Elf.isEmpty(orderByElements1)) {
      return Optional.empty();
    }
    StringBuilder sb = new StringBuilder();
    for (OrderByElement orderByElement : orderByElements1) {
      sb.append(orderByElement).append(", ");
    }
    sb.setLength(sb.length() - 2);

    return Optional.of(sb.toString());
  }

  protected String timeStamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
  }

  private static ClassName resolveGeneratedAnnotation() {
    // if Generated annotation is available, add it
    ClassName generatedAnnotation;
    if (Elf.classExists("javax.annotation.Generated")) {
      generatedAnnotation = ClassName.get("javax.annotation", "Generated");
    } else {
      generatedAnnotation = ClassName.get("javax.annotation.processing", "Generated");
    }
    return generatedAnnotation;
  }

  private CodeBlock.Builder toStringMethodBody(List<Field<?>> fieldList) {
    CodeBlock.Builder toStringMethodBody =
        CodeBlock.builder().add("return \"$L.Result{\"\n", className);

    boolean isFirst = true;
    for (Field<?> f : fieldList) {
      if (isFirst) {
        toStringMethodBody.add("+ $S + $L\n", f.getAlias() + " = ", f.getAlias());
      } else {
        toStringMethodBody.add("+ $S + $L\n", ", " + f.getAlias() + " = ", f.getAlias());
      }
      isFirst = false;
    }

    toStringMethodBody.addStatement(" + $S", "}");
    return toStringMethodBody;
  }

  private TypeSpec resultClass(List<Field<?>> fieldList) {
    TypeSpec.Builder resultClassBuilder =
        TypeSpec.classBuilder("Result").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    for (Field<?> f : fieldList) {
      resultClassBuilder.addField(
          FieldSpec.builder(f.getDataType(), f.getAlias(), Modifier.PRIVATE).build());
    }

    // add getters
    for (Field<?> f : fieldList) {
      resultClassBuilder.addMethod(
          MethodSpec.methodBuilder(toGetterName(f.getAlias()))
              .addModifiers(Modifier.PUBLIC)
              .returns(f.getDataType())
              .addStatement("return $L", f.getAlias())
              .build());
    }

    // to string methods
    CodeBlock.Builder toStringMethodBody = toStringMethodBody(fieldList);
    MethodSpec toStringMethod =
        MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addCode(toStringMethodBody.build())
            .build();

    resultClassBuilder.addMethod(toStringMethod);

    return resultClassBuilder.build();
  }

  private static String toGetterName(String fieldName) {
    return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  /** Init() method */
  private MethodSpec methodInit(List<FieldSpec> fConstants, FieldSpec fFields) {
    MethodSpec.Builder initBuilder =
        MethodSpec.methodBuilder("init").addModifiers(Modifier.PRIVATE);
    for (FieldSpec field : fConstants) {
      initBuilder.addStatement("$N.add($N)", fFields, field);
    }
    return initBuilder.build();
  }

  /** Constructor() method */
  private MethodSpec methodConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement("init()")
        .build();
  }

  /** allFields field */
  private FieldSpec fieldAllFields() {

    ParameterizedTypeName fListOfFields = typeListOfFields();

    return FieldSpec.builder(fListOfFields, "fields", Modifier.PRIVATE, Modifier.FINAL)
        .initializer("new $T<$T<?>>()", ArrayList.class, Field.class)
        .build();
  }

  private static ParameterizedTypeName typeListOfFields() {
    ParameterizedTypeName wildField = typeFieldWildCard();
    return ParameterizedTypeName.get(ClassName.get(List.class), wildField);
  }

  private static ParameterizedTypeName typeFieldWildCard() {
    WildcardTypeName wildcardTypeName = WildcardTypeName.subtypeOf(Object.class);
    return ParameterizedTypeName.get(ClassName.get(Field.class), wildcardTypeName);
  }

  /** schema field */
  private FieldSpec fieldSchema(PlainSelect plainSelect) {
    CodeBlock.Builder schemaString1 =
        CodeBlock.builder().add("$S\n", plainSelect.getFromItem().toString() + "\n");
    List<Join> joins = Optional.ofNullable(plainSelect.getJoins()).orElse(Collections.emptyList());
    for (Join j : joins) {
      schemaString1.add("          + $S\n", j.toString() + "\n");
    }

    return FieldSpec.builder(String.class, "schema", Modifier.PRIVATE, Modifier.FINAL)
        .initializer(schemaString1.build())
        .build();
  }

  private List<FieldSpec> fieldConstants(List<Field<?>> fieldList) {
    // Constant fields
    return fieldList.stream()
        .map(
            f ->
                FieldSpec.builder(
                        ParameterizedTypeName.get(Field.class, f.getDataType()),
                        constantName(f.getAlias()),
                        Modifier.PUBLIC,
                        Modifier.STATIC)
                    .initializer(
                        "$T.of($S, $S, $T.class)",
                        Field.class,
                        f.getSqlField(),
                        f.getAlias(),
                        f.getDataType())
                    .build())
        .collect(Collectors.toList());
  }

  String constantName(String name) {
    return toSnakeCase(name);
  }

  String toSnakeCase(String name) {
    return name.replaceAll("([A-Z])", "_$1").toUpperCase();
  }

  public List<Field<?>> extractFields(PlainSelect plainSelect) {

    return plainSelect.getSelectItems().stream()
        .map(selectItem -> toField((SelectExpressionItem) selectItem))
        .collect(Collectors.toList());
  }

  private Field<?> toField(SelectExpressionItem selectItem) {
    String alias =
        Objects.requireNonNull(
                selectItem.getAlias(), "Alias if required for filed [" + selectItem + "]")
            .getName();
    alias = unquote(alias);

    String[] parts = alias.contains("_") ? alias.split("_") : new String[] {alias, "object"};

    String aliasName = parts[0];
    String typeName = parts[1];

    Class<?> type = resolveType(typeName);
    String sqlField = selectItem.getExpression().toString();
    return Field.of(sqlField, aliasName, type);
  }

  Class<?> resolveType(String type) {
    Map<String, Class<?>> typeMap = new HashMap<>();

    typeMap.put("int", Integer.class);
    typeMap.put("long", Long.class);
    typeMap.put("float", Float.class);
    typeMap.put("double", Double.class);
    typeMap.put("boolean", Boolean.class);
    typeMap.put("string", String.class);
    typeMap.put("date", Date.class);
    typeMap.put("time", java.sql.Timestamp.class);
    typeMap.put("decimal", java.math.BigDecimal.class);
    typeMap.put("bigint", java.math.BigInteger.class);
    typeMap.put("byte", Byte.class);
    typeMap.put("object", Object.class);

    Class<?> aClass = typeMap.get(type);
    if (aClass == null) throw new IllegalArgumentException("Unsupported type: " + type);
    return aClass;
  }

  String unquote(String s) {
    return s.replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
  }
}
