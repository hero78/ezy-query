package io.github.kayr.ezyquery

import io.github.kayr.ezyquery.api.EzyCriteria
import io.github.kayr.ezyquery.api.Field
import io.github.kayr.ezyquery.api.Sort
import io.github.kayr.ezyquery.api.SqlBuilder
import io.github.kayr.ezyquery.api.cnd.Cnd
import io.github.kayr.ezyquery.parser.QueryAndParams
import spock.lang.Shared
import spock.lang.Specification

class EzyQueryTest extends Specification {

    def fields = [name, age, office, maxAge]

    def ezyQuery = new EzyQuery() {
        @Override
        QueryAndParams query(EzyCriteria params) {
            return null
        }

        @Override
        Class resultClass() {
            return null
        }

        @Override
        List<Field<?>> fields() {
            return fields
        }

        @Override
        String schema() {
            return "my_table"
        }
    }
    @Shared
    private Field name = new Field('t.name', 'name')
    @Shared
    private Field age = new Field('t.age', 'age')
    @Shared
    private Field office = new Field('t.office', 'office')
    @Shared
    private Field maxAge = new Field('t.maxAge', 'maxAge')

    def "test build with a filter"() {


        def criteria = EzyCriteria.selectAll()
                .where(Cnd.expr("name = 'ronald'"))
                .where(Cnd.expr("age >  20"))
                .limit(10, 2)


        when:

        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE ((t.name = ?) AND (t.age > ?))\n' +
                'LIMIT 10 OFFSET 2'


    }

    def "test build with filter API"() {


        def criteria = EzyCriteria.selectAll()
                .where(Cnd.orAll(Cnd.eq("#name", 'ronald'),
                        Cnd.gt('#age', 20)))
                .limit(10, 2)

        when:

        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:


        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE (t.name = ? OR t.age > ?)\n' +
                'LIMIT 10 OFFSET 2'

    }

    def "test build with a filter and API"() {


        def criteria = EzyCriteria.selectAll()
                .where(Cnd.orAll(
                        Cnd.eq("#name", 'ronald'),
                        Cnd.gt('#age', 20),
                        Cnd.expr("office = 'NY'"),
                        Cnd.expr('maxAge > 30')))
                .limit(10, 2)

        when:
        def orQuery = SqlBuilder.buildSql(ezyQuery, criteria)


        then:


        orQuery.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE (t.name = ? OR t.age > ? OR (t.office = ?) OR (t.maxAge > ?))\n' +
                'LIMIT 10 OFFSET 2'
        orQuery.params == ['ronald', 20, 'NY', 30]

    }

    def "test build with no filter"() {

        def criteria = EzyCriteria.selectAll()
                .limit(10, 2)

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'LIMIT 10 OFFSET 2'
        query.params == []

    }


    def "test build with only offset uses default limit"() {

        def criteria = EzyCriteria.selectAll()
                .offset(2)

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'LIMIT 50 OFFSET 2'
        query.params == []

    }

    def "test build uses default offset"() {
        def fields = [
                name,
                age,
                office,
                maxAge
        ]

        def criteria = EzyCriteria.selectAll()
                .limit(15)

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age", \n' +
                '  t.office as "office", \n' +
                '  t.maxAge as "maxAge"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds count query"() {

        def criteria = EzyCriteria.selectCount()
                .limit(15)

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                ' COUNT(*) \n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)'
        query.params == []

    }

    def "test builds select query with specified columns"() {

        def criteria = EzyCriteria.select('name', 'age')
                .limit(15)

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds select query with order by clause"() {

        def criteria = EzyCriteria.select('name', 'age')
                .limit(15)
                .orderBy(
                        Sort.by('name', Sort.DIR.ASC),
                        Sort.by('age', Sort.DIR.DESC))

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'ORDER BY t.name ASC, t.age DESC\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds select query with order by clause using strings"() {

        def criteria = EzyCriteria.select('name', 'age')
                .limit(15)
                .orderBy('name', 'age')

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'ORDER BY t.name ASC, t.age ASC\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds select query with order by clause using one string"() {

        def criteria = EzyCriteria.select('name', 'age')
                .limit(15)
                .orderBy('name')

        when:
        def query = SqlBuilder.buildSql(ezyQuery, criteria)

        then:
        query.sql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (1 = 1)\n' +
                'ORDER BY t.name ASC\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds select query with default where clause"() {

        EzyQuery ezyQueryWithWhere = queryWithWhereClause()


        def criteria = EzyCriteria.select('name', 'age')
                .limit(15)
                .orderBy('name')

        when:
        def query = SqlBuilder.buildSql(ezyQueryWithWhere, criteria)


        def generatedSql = query.sql
        then:
        generatedSql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (t.name = 123) AND (1 = 1)\n' +
                'ORDER BY t.name ASC\n' +
                'LIMIT 15 OFFSET 0'
        query.params == []

    }

    def "test builds select query with default where clause does not add extra parenthesis"() {

        EzyQuery ezyQueryWithWhere = queryWithWhereClause()


        def criteria = EzyCriteria.select('name', 'age')
                .where(name.eq("RK").and(age.eq(1)))
                .limit(15)
                .orderBy('name');

        when:
        def query = SqlBuilder.buildSql(ezyQueryWithWhere, criteria)


        def generatedSql = query.sql
        then:
        generatedSql == 'SELECT \n' +
                '  t.name as "name", \n' +
                '  t.age as "age"\n' +
                'FROM my_table\n' +
                'WHERE (t.name = 123) AND (t.name = ? AND t.age = ?)\n' +
                'ORDER BY t.name ASC\n' +
                'LIMIT 15 OFFSET 0'
        query.params == ['RK', 1]

    }

    private EzyQuery queryWithWhereClause() {
        def ezyQueryWithWhere = new EzyQuery() {
            @Override
            QueryAndParams query(EzyCriteria params) {
                return null
            }

            @Override
            Class resultClass() {
                return null
            }

            @Override
            List<Field<?>> fields() {
                return fields
            }

            @Override
            String schema() {
                return "my_table"
            }

            @Override
            Optional<String> whereClause() {
                return Optional.of('t.name = 123')
            }
        }
        ezyQueryWithWhere
    }
}
