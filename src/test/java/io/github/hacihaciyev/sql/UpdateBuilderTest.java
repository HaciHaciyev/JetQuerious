package io.github.hacihaciyev.sql;

import org.junit.jupiter.api.Test;

import static io.github.hacihaciyev.sql.QueryForge.update;
import static io.github.hacihaciyev.sql.SelectBuilderTest.log;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateBuilderTest {

    @Test
    void test() {
        assertEquals("UPDATE employees SET salary = 50000 WHERE department = 'Sales' ", update("employees")
                        .set("salary = 50000")
                        .where("department = 'Sales'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000 WHERE department = 'Sales' RETURNING employee_id, salary ", update("employees")
                        .set("salary = 50000")
                        .where("department = 'Sales'")
                        .returning("employee_id", "salary").sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000, position = 'Manager' WHERE department = 'Sales' ", update("employees")
                        .set("salary = 50000, position = 'Manager'")
                        .where("department = 'Sales'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000 ", update("employees")
                        .set("salary = 50000")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 55000 WHERE department = 'Sales' RETURNING employee_id, salary ", update("employees")
                        .set("salary = 55000")
                        .where("department = 'Sales'")
                        .returning("employee_id", "salary").sql());

        log();

        assertEquals("UPDATE employees SET salary = 55000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 55000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .build().sql());

        log();
    }

    @Test
    void test2() {
        assertEquals("UPDATE employees SET salary = 50000 WHERE department = 'Sales' ", update("employees")
                        .set("salary = 50000")
                        .where("department = 'Sales'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000 WHERE department = 'Sales' RETURNING employee_id, salary ", update("employees")
                        .set("salary = 50000")
                        .where("department = 'Sales'")
                        .returning("employee_id", "salary").sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000, position = 'Manager' WHERE department = 'Sales' ", update("employees")
                        .set("salary = 50000, position = 'Manager'")
                        .where("department = 'Sales'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 50000 ", update("employees")
                        .set("salary = 50000")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 55000 WHERE department = 'Sales' RETURNING employee_id, salary ", update("employees")
                        .set("salary = 55000")
                        .where("department = 'Sales'")
                        .returning("employee_id", "salary").sql());

        log();

        assertEquals("UPDATE employees SET salary = 55000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 55000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 60000 WHERE department = 'Sales' OR position = 'Manager' ", update("employees")
                        .set("salary = 60000")
                        .where("department = 'Sales'")
                        .or("position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 60000 WHERE NOT (department = 'Sales' AND position = 'Manager') ", update("employees")
                        .set("salary = 60000")
                        .whereNot("(department = 'Sales' AND position = 'Manager')")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 70000 WHERE department = 'Sales' AND position IS NOT NULL ", update("employees")
                        .set("salary = 70000")
                        .where("department = 'Sales' AND position IS NOT NULL")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 80000 WHERE department = 'Sales' AND position = 'Manager' OR position = 'Developer' ", update("employees")
                        .set("salary = 80000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .or("position = 'Developer'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 80000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 80000")
                        .where("department = 'Sales'")
                        .and("position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 90000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 90000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 90000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 90000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 100000 WHERE department = 'Sales' AND position = 'Manager' ", update("employees")
                        .set("salary = 100000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 110000 WHERE department = 'Sales' AND position = 'Manager' AND age > 30 ", update("employees")
                        .set("salary = 110000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("age > 30")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 120000 WHERE department = 'Sales' AND position = 'Manager' OR department = 'Marketing' ", update("employees")
                        .set("salary = 120000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .or("department = 'Marketing'")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 130000 WHERE department = 'Sales' AND position = 'Manager' AND NOT age < 30 ", update("employees")
                        .set("salary = 130000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("NOT age < 30")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 140000 WHERE department = 'Sales' AND position = 'Manager' AND salary > 100000 ", update("employees")
                        .set("salary = 140000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("salary > 100000")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 150000 WHERE department = 'Sales' AND position = 'Manager' OR age BETWEEN 30 AND 40 ", update("employees")
                        .set("salary = 150000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .or("age BETWEEN 30 AND 40")
                        .build().sql());

        log();

        assertEquals("UPDATE employees SET salary = 160000 WHERE department = 'Sales' AND position = 'Manager' AND department IS NOT NULL ", update("employees")
                        .set("salary = 160000")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("department IS NOT NULL")
                        .build().sql());

        log();
    }
}