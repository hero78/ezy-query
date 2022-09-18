package io.github.kayr.ezyquery.it;

import io.github.kayr.ezyquery.EzyQuery;
import io.github.kayr.ezyquery.api.Field;
import io.github.kayr.ezyquery.api.FilterParams;
import io.github.kayr.ezyquery.parser.QueryAndParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;

/**
 * SELECT c.customerName AS customerName, e.employeeNumber AS employeeRep, o.addressLine1 AS
 * employeeOffice, o.country AS employeeCounty FROM offices o LEFT JOIN employees e ON o.officeCode
 * = e.officeCode LEFT JOIN customers c ON e.employeeNumber = c.salesRepEmployeeNumber
 */
@Generated(value = "io.github.kayr.ezyquery.gen.QueryGen", date = "2022-09-18T14:08:34.959")
public class CustomerReps implements EzyQuery<CustomerReps.Result> {
  public static Field<Object> FIELD_CUSTOMER_NAME =
      Field.of("c.customerName", "customerName", Object.class);

  public static Field<Object> FIELD_EMPLOYEE_REP =
      Field.of("e.employeeNumber", "employeeRep", Object.class);

  public static Field<Object> FIELD_EMPLOYEE_OFFICE =
      Field.of("o.addressLine1", "employeeOffice", Object.class);

  public static Field<Object> FIELD_EMPLOYEE_COUNTY =
      Field.of("o.country", "employeeCounty", Object.class);

  public static final CustomerReps Q = new CustomerReps();

  private final String schema =
      "offices o\n"
          + "LEFT JOIN employees e ON o.officeCode = e.officeCode\n"
          + "LEFT JOIN customers c ON e.employeeNumber = c.salesRepEmployeeNumber\n";

  private final List<Field<?>> fields = new ArrayList<Field<?>>();

  public CustomerReps() {
    init();
  }

  private void init() {
    fields.add(FIELD_CUSTOMER_NAME);
    fields.add(FIELD_EMPLOYEE_REP);
    fields.add(FIELD_EMPLOYEE_OFFICE);
    fields.add(FIELD_EMPLOYEE_COUNTY);
  }

  public QueryAndParams query(FilterParams criteria) {
    return EzyQuery.buildQueryAndParams(criteria, fields, schema);
  }

  @Override
  public List<Field<?>> fields() {
    return Collections.emptyList();
  }

  @Override
  public Class<Result> resultClass() {
    return Result.class;
  }

  public static class Result {
    public Object customerName;

    public Object employeeRep;

    public Object employeeOffice;

    public Object employeeCounty;
  }
}