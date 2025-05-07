package com.hadzhy.jetquerious.sql;

import org.junit.jupiter.api.Test;

import static com.hadzhy.jetquerious.sql.SQLBuilder.*;
import static com.hadzhy.jetquerious.sql.SelectBuilderTest.log;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertBuilderTest {

    @Test
    void test() {

        assertEquals("INSERT INTO ChessGame (id, player_for_white_rating, player_for_black_rating, time_controlling_type, creation_date, last_updated_date, is_game_over, game_result_status ) VALUES (?, ?, ?, ?, ?, ?, ?, ?); INSERT INTO GamePlayers (chess_game_id, player_for_white_id, player_for_black_id) VALUES (?, ?, ?);",
                batchOf(
                insert()
                        .into("ChessGame")
                        .column("id")
                        .column("player_for_white_rating")
                        .column("player_for_black_rating")
                        .column("time_controlling_type")
                        .column("creation_date")
                        .column("last_updated_date")
                        .column("is_game_over")
                        .column("game_result_status")
                        .values()
                        .build()
                        .toSQlQuery(),
                insert()
                        .into("GamePlayers")
                        .columns("chess_game_id", "player_for_white_id", "player_for_black_id")
                        .values()
                        .build()
                        .toSQlQuery()));


        assertEquals("INSERT INTO ChessGame (id, player_for_white_rating, player_for_black_rating, time_controlling_type, creation_date, last_updated_date, is_game_over, game_result_status ) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ",
                insert()
                .into("ChessGame")
                .column("id")
                .column("player_for_white_rating")
                .column("player_for_black_rating")
                .column("time_controlling_type")
                .column("creation_date")
                .column("last_updated_date")
                .column("is_game_over")
                .column("game_result_status")
                .values()
                .build().sql());

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees DEFAULT VALUES ",
                insert()
                        .defaultValues("employees")
                        .build().sql());

        log();

        assertEquals("WITH temp AS (SELECT id, name FROM old_employees) INSERT INTO employees (id, name) SELECT id, name FROM temp ",
                withAndInsert("temp", SQLQuery.of("SELECT id, name FROM old_employees"))
                        .into("employees", "id", "name")
                        .select()
                        .columns("id", "name")
                        .from("temp")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doNothing()
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary WHERE department_id = ? ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary WHERE department_id = ?")
                        .build().sql());

        log();

        assertEquals("INSERT INTO products (category, price) SELECT category , SUM(price) FROM old_products GROUP BY category ",
                insert()
                        .into("products", "category", "price")
                        .select()
                        .column("category")
                        .sum("price")
                        .from("old_products")
                        .groupBy("category")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) RETURNING id, name ",
                insert()
                        .into("employees")
                        .columns("id", "name", "salary")
                        .values()
                        .returning("id", "name").sql());

        log();

        assertEquals("INSERT INTO orders (order_id, customer_id) VALUES (?, ?) RETURNING *",
                insert()
                        .into("orders", "order_id", "customer_id")
                        .values()
                        .returningAll().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doNothing()
                        .build().sql());

        log();

        assertEquals("INSERT INTO inventory (item_id, quantity) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET quantity = quantity + EXCLUDED.quantity ",
                insert()
                        .into("inventory", "item_id", "quantity")
                        .values()
                        .onConflict("item_id")
                        .doUpdateSet("quantity = quantity + EXCLUDED.quantity")
                        .build().sql());

        log();

        assertEquals("INSERT INTO tasks (task_id, status) VALUES (?, ?) ON CONFLICT (task_id) DO UPDATE SET status = ? WHERE status != ? ",
                insert()
                        .into("tasks", "task_id", "status")
                        .values()
                        .onConflict("task_id")
                        .doUpdateSet("status = ? WHERE status != ?")
                        .build().sql());

        log();

    }

    @Test
    void test2() {
        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) RETURNING id ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .returning("id").sql());

        log();

        assertEquals("INSERT INTO employees (id, name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name ",
                insert()
                        .into("employees", "id", "name")
                        .values()
                        .onConflict("id")
                        .doUpdateSet("name = EXCLUDED.name")
                        .build().sql());

        log();

        assertEquals("INSERT INTO departments (id, department_name) DEFAULT VALUES ",
                insert()
                        .defaultValues("departments", "id", "department_name")
                        .build().sql());

        log();

        assertEquals("INSERT INTO products (category, price) VALUES (?, ?) ON CONFLICT (category) DO NOTHING ",
                insert()
                        .into("products", "category", "price")
                        .values()
                        .onConflict("category")
                        .doNothing()
                        .build().sql());

        log();

        assertEquals("WITH temp AS (SELECT id, name FROM old_employees) INSERT INTO employees (id, name) SELECT id, name FROM temp ",
                withAndInsert("temp", SQLQuery.of("SELECT id, name FROM old_employees"))
                        .into("employees", "id", "name")
                        .select()
                        .columns("id", "name")
                        .from("temp")
                        .build().sql());

        log();

        assertEquals("INSERT INTO orders (order_id, status) VALUES (?, ?) RETURNING *",
                insert()
                        .into("orders", "order_id", "status")
                        .values()
                        .returningAll().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary WHERE department_id = ? ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary WHERE department_id = ?")
                        .build().sql());

        log();

        assertEquals("INSERT INTO projects (project_id, project_name) SELECT project_id, project_name FROM old_projects ",
                insert()
                        .into("projects", "project_id", "project_name")
                        .select()
                        .columns("project_id", "project_name")
                        .from("old_projects")
                        .build().sql());

        log();

        assertEquals("INSERT INTO users (username, email) VALUES (?, ?) ON CONFLICT (username) DO UPDATE SET email = EXCLUDED.email ",
                insert()
                        .into("users", "username", "email")
                        .values()
                        .onConflict("username")
                        .doUpdateSet("email = EXCLUDED.email")
                        .build().sql());

        log();

        assertEquals("INSERT INTO inventory (item_id, quantity) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET quantity = quantity + EXCLUDED.quantity WHERE quantity < 100 ",
                insert()
                        .into("inventory", "item_id", "quantity")
                        .values()
                        .onConflict("item_id")
                        .doUpdateSet("quantity = quantity + EXCLUDED.quantity WHERE quantity < 100")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING RETURNING id ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doNothing()
                        .returning("id").sql());

        log();

        assertEquals("INSERT INTO books (book_id, title, author) VALUES (?, ?, ?) ON CONFLICT (book_id) DO NOTHING ",
                insert()
                        .into("books", "book_id", "title", "author")
                        .values()
                        .onConflict("book_id")
                        .doNothing()
                        .build().sql());

        log();

        assertEquals("INSERT INTO inventory (item_id, price) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET price = EXCLUDED.price ",
                insert()
                        .into("inventory", "item_id", "price")
                        .values()
                        .onConflict("item_id")
                        .doUpdateSet("price = EXCLUDED.price")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name) VALUES (?, ?) RETURNING *",
                insert()
                        .into("employees", "id", "name")
                        .values()
                        .returningAll().sql());

        log();

        assertEquals("INSERT INTO orders (order_id, customer_id, total) VALUES (?, ?, ?) ON CONFLICT (order_id) DO NOTHING ",
                insert()
                        .into("orders", "order_id", "customer_id", "total")
                        .values()
                        .onConflict("order_id")
                        .doNothing()
                        .build().sql());

        log();

        assertEquals("INSERT INTO teams (team_id, team_name) VALUES (?, ?) ON CONFLICT (team_id) DO UPDATE SET team_name = EXCLUDED.team_name ",
                insert()
                        .into("teams", "team_id", "team_name")
                        .values()
                        .onConflict("team_id")
                        .doUpdateSet("team_name = EXCLUDED.team_name")
                        .build().sql());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values()
                        .onConflict("id")
                        .doUpdateSet("name = EXCLUDED.name")
                        .build().sql());

        log();

        assertEquals("INSERT INTO customers (customer_id, customer_name) VALUES (?, ?) ON CONFLICT (customer_id) DO UPDATE SET customer_name = EXCLUDED.customer_name ",
                insert()
                        .into("customers", "customer_id", "customer_name")
                        .values()
                        .onConflict("customer_id")
                        .doUpdateSet("customer_name = EXCLUDED.customer_name")
                        .build().sql());

        log();

        assertEquals("INSERT INTO projects (project_id, project_name) VALUES (?, ?) RETURNING project_name ",
                insert()
                        .into("projects", "project_id", "project_name")
                        .values()
                        .returning("project_name").sql());

        log();

        assertEquals("INSERT INTO items (item_id, item_name) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET item_name = EXCLUDED.item_name ",
                insert()
                        .into("items", "item_id", "item_name")
                        .values()
                        .onConflict("item_id")
                        .doUpdateSet("item_name = EXCLUDED.item_name")
                        .build().sql());

        log();
    }
}