package io.github.kayr.ezyquery.api;

import io.github.kayr.ezyquery.api.cnd.ICond;
import io.github.kayr.ezyquery.util.Elf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;

@lombok.Getter
@lombok.Builder(toBuilder = true, access = AccessLevel.PRIVATE)
public class EzyCriteria {

  @Builder.Default private List<String> columns = new ArrayList<>();
  @Builder.Default private List<ICond> conditions = new ArrayList<>();
  @Builder.Default private List<NamedParamValue> paramValues = new ArrayList<>();

  @Builder.Default private List<Sort> sorts = new ArrayList<>();

  @Builder.Default private Integer offset = 0;
  @Builder.Default private Integer limit = 50;

  @Builder.Default private boolean count = false;

  // region Static methods

  /** Convenience method just to better communicate the intention */
  public static EzyCriteria selectAll() {
    return builder().build();
  }

  public static EzyCriteria select(String... columns) {
    return builder().columns(Arrays.asList(columns)).build();
  }

  public static EzyCriteria selectCount() {
    return builder().count(true).build();
  }
  // endregion

  // region Builder methods
  public EzyCriteria addSelect(String... columns) {
    return toBuilder().columns(Elf.addAll(this.columns, columns)).build();
  }

  public EzyCriteria where(ICond... conds) {
    return toBuilder().conditions(Elf.addAll(this.conditions, conds)).build();
  }

  public EzyCriteria offset(@lombok.NonNull Integer offset) {
    return toBuilder().offset(offset).build();
  }

  public EzyCriteria limit(@lombok.NonNull Integer limit) {
    return toBuilder().limit(limit).build();
  }

  public EzyCriteria limit(@lombok.NonNull Integer limit, @lombok.NonNull Integer offset) {
    return toBuilder().limit(limit).offset(offset).build();
  }

  public EzyCriteria count() {
    return toBuilder().count(true).build();
  }

  public EzyCriteria orderBy(Sort... sort) {
    return toBuilder().sorts(Elf.addAll(this.sorts, sort)).build();
  }

  public EzyCriteria orderBy(String... sort) {
    List<Sort> sortList = new ArrayList<>();
    for (String s : sort) {
      List<Sort> parse = Sort.parse(s);
      sortList.addAll(parse);
    }
    return toBuilder().sorts(Elf.combine(this.sorts, sortList)).build();
  }

  public EzyCriteria setParam(NamedParam namedParam, Object value) {
    return toBuilder()
        .paramValues(Elf.addAll(this.paramValues, new NamedParamValue(namedParam, value)))
        .build();
  }

  // endregion

  // region Read only

  public List<String> getColumns() {
    return Collections.unmodifiableList(columns);
  }

  public List<ICond> getConditions() {
    return Collections.unmodifiableList(conditions);
  }

  public List<Sort> getSorts() {
    return Collections.unmodifiableList(sorts);
  }

  // endregion

}
