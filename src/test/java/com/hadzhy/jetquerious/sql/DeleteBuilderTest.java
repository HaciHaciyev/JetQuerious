package com.hadzhy.jetquerious.sql;

import org.junit.jupiter.api.Test;

import static com.hadzhy.jetquerious.sql.SQLBuilder.delete;
import static com.hadzhy.jetquerious.sql.SelectBuilderTest.log;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteBuilderTest {

    @Test
    void test() {
        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' OR department = 'Marketing' ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .or("department = 'Marketing'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' AND NOT age < 30 ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("NOT age < 30")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' AND salary > 100000 ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("salary > 100000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' OR age BETWEEN 30 AND 40 ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .or("age BETWEEN 30 AND 40")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' AND department IS NOT NULL ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("department IS NOT NULL")
                        .build().sql());

        log();
    }

    @Test
    void test2() {
        assertEquals("DELETE FROM employees WHERE department = 'HR' AND position = 'Assistant' ",
                delete()
                        .from("employees")
                        .where("department = 'HR' AND position = 'Assistant'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Engineering' AND salary < 50000 ",
                delete()
                        .from("employees")
                        .where("department = 'Engineering' AND salary < 50000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Finance' AND position = 'Analyst' AND age >= 25 ",
                delete()
                        .from("employees")
                        .where("department = 'Finance' AND position = 'Analyst'")
                        .and("age >= 25")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Manager' AND salary = 120000 ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Manager'")
                        .and("salary = 120000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Marketing' AND status = 'Active' AND hired_date < '2020-01-01' ",
                delete()
                        .from("employees")
                        .where("department = 'Marketing' AND status = 'Active'")
                        .and("hired_date < '2020-01-01'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'IT' AND position = 'Developer' OR department = 'HR' AND status = 'Inactive' ",
                delete()
                        .from("employees")
                        .where("department = 'IT' AND position = 'Developer'")
                        .or("department = 'HR' AND status = 'Inactive'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Operations' AND age BETWEEN 30 AND 40 ",
                delete()
                        .from("employees")
                        .where("department = 'Operations' AND age BETWEEN 30 AND 40")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Assistant' AND salary IS NULL ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Assistant'")
                        .and("salary IS NULL")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Finance' AND position = 'Director' AND last_promotion_date IS NOT NULL ",
                delete()
                        .from("employees")
                        .where("department = 'Finance' AND position = 'Director'")
                        .and("last_promotion_date IS NOT NULL")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'HR' AND department_code IN ('HR001', 'HR002') ",
                delete()
                        .from("employees")
                        .where("department = 'HR' AND department_code IN ('HR001', 'HR002')")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND position = 'Salesperson' AND NOT salary < 50000 ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND position = 'Salesperson'")
                        .and("NOT salary < 50000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Engineering' AND age > 40 AND status = 'Inactive' ",
                delete()
                        .from("employees")
                        .where("department = 'Engineering' AND age > 40")
                        .and("status = 'Inactive'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Finance' AND position = 'Analyst' AND hired_date >= '2019-01-01' ",
                delete()
                        .from("employees")
                        .where("department = 'Finance' AND position = 'Analyst'")
                        .and("hired_date >= '2019-01-01'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Marketing' AND position = 'Coordinator' AND salary BETWEEN 60000 AND 80000 ",
                delete()
                        .from("employees")
                        .where("department = 'Marketing' AND position = 'Coordinator'")
                        .and("salary BETWEEN 60000 AND 80000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Sales' AND status = 'Active' AND NOT department = 'Engineering' ",
                delete()
                        .from("employees")
                        .where("department = 'Sales' AND status = 'Active'")
                        .and("NOT department = 'Engineering'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Operations' AND position = 'Supervisor' AND start_date <= '2015-01-01' ",
                delete()
                        .from("employees")
                        .where("department = 'Operations' AND position = 'Supervisor'")
                        .and("start_date <= '2015-01-01'")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'HR' AND position = 'Manager' AND department_code IS NULL ",
                delete()
                        .from("employees")
                        .where("department = 'HR' AND position = 'Manager'")
                        .and("department_code IS NULL")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'IT' AND position = 'Support' AND salary < 40000 ",
                delete()
                        .from("employees")
                        .where("department = 'IT' AND position = 'Support'")
                        .and("salary < 40000")
                        .build().sql());

        log();

        assertEquals("DELETE FROM employees WHERE department = 'Finance' AND position = 'Intern' AND age IS NOT NULL ",
                delete()
                        .from("employees")
                        .where("department = 'Finance' AND position = 'Intern'")
                        .and("age IS NOT NULL")
                        .build().sql());

        log();
    }
}