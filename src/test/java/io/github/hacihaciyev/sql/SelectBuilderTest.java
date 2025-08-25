package io.github.hacihaciyev.sql;

import org.junit.jupiter.api.Test;
import java.util.logging.Logger;

import static io.github.hacihaciyev.sql.QueryForge.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectBuilderTest {

    private static int passesTests = 0;

    private static final Logger Log = Logger.getLogger(SelectBuilderTest.class.getName());

    @Test
    void test() {
        assertEquals("SELECT d.id AS id, d.rides AS rides, d.total_reviews AS total_reviews, d.sum_of_scores AS sum_of_scores , CASE WHEN total_reviews > 0 THEN sum_of_scores::decimal / total_reviews ELSE NULL END AS average FROM driver d ORDER BY average LIMIT ? OFFSET ? ", select()
                .column("d.id").as("id")
                .column("d.rides").as("rides")
                .column("d.total_reviews").as("total_reviews")
                .column("d.sum_of_scores").as("sum_of_scores")
                .caseStatement()
                .when("total_reviews > 0")
                .then("sum_of_scores::decimal / total_reviews")
                .elseCase("NULL")
                .endAs("average")
                .from("driver d")
                .orderBy("average")
                .limitAndOffset()
                .sql());

        assertEquals("SELECT a.id AS id, a.author_id AS author_id, a.header AS header, a.summary AS summary, a.body AS body, a.status AS status, a.creation_date AS creation_date, a.last_updated AS last_updated , COUNT(v.id) AS views , COUNT(l.article_id) AS likes FROM Articles a JOIN Views v ON v.article_id = a.id JOIN Likes l ON l.article_id = a.id WHERE a.id = ? ",
                select()
                .column("a.id").as("id")
                .column("a.author_id").as("author_id")
                .column("a.header").as("header")
                .column("a.summary").as("summary")
                .column("a.body").as("body")
                .column("a.status").as("status")
                .column("a.creation_date").as("creation_date")
                .column("a.last_updated").as("last_updated")
                .count("v.id").as("views")
                .count("l.article_id").as("likes")
                .from("Articles a")
                .join("Views v", "v.article_id = a.id")
                .join("Likes l", "l.article_id = a.id")
                .where("a.id = ?")
                .build()
                .sql());

        assertEquals("SELECT COUNT(v.id) AS views , COUNT(l.article_id) AS likes FROM Articles a JOIN Views v ON v.article_id = a.id JOIN Likes l ON l.article_id = a.id WHERE a.id = ? ", select()
                .count("v.id").as("views")
                .count("l.article_id").as("likes")
                .from("Articles a")
                .join("Views v", "v.article_id = a.id")
                .join("Likes l", "l.article_id = a.id")
                .where("a.id = ?")
                .build()
                .sql());

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active , COUNT(*) AS total FROM users ",
                select()
                .columns("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .count("*").as("total")
                .from("users")
                .build()
                .sql());

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active , COUNT(*) FROM users ",
                select()
                .columns("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .count("*")
                .from("users")
                .build()
                .sql());

        assertEquals("SELECT * FROM UserPartnership WHERE (user_id = ? AND partner_id = ?) OR (user_id = ? AND partner_id = ?) ", select()
                .all()
                .from("UserPartnership")
                .where("(user_id = ?")
                .and("partner_id = ?)")
                .or("(user_id = ?")
                .and("partner_id = ?)")
                .build().sql());

        Log.info("Test -1 passed. Query: {%s}".formatted(
                select()
                .all()
                .from("UserPartnership")
                .where("(user_id = ?")
                .and("partner_id = ?)")
                .or("(user_id = ?")
                .and("partner_id = ?)")
                .build().sql()));

        assertEquals("SELECT username , email , rating FROM UserAccount WHERE username = ? ", select()
                .column("username")
                .column("email")
                .column("rating")
                .from("UserAccount")
                .where("username = ?")
                .build().sql());

        Log.info("Test 0 passed.");

        assertEquals("SELECT name, age FROM users WHERE age > 18 ORDER BY age DESC LIMIT 10 OFFSET 5 ", select()
                .columns("name", "age")
                .from("users")
                .where("age > 18")
                .orderBy("age", Order.DESC)
                .limitAndOffset(10, 5).sql());

        log();

        assertEquals("SELECT t.id AS token_id, t.token AS token, t.is_confirmed AS token_confirmation, t.creation_date AS token_creation_date, u.id AS id, u.username AS username, u.email AS email, u.password AS password, u.user_role AS user_role, u.rating AS rating, u.rating_deviation AS rating_deviation, u.rating_volatility AS rating_volatility, u.is_enable AS is_enable, u.creation_date AS creation_date, u.last_updated_date AS last_updated_date FROM UserToken t INNER JOIN UserAccount u ON t.user_id = u.id WHERE t.token = ? ",
                select()
                .column("t.id").as("token_id")
                .column("t.token").as("token")
                .column("t.is_confirmed").as("token_confirmation")
                .column("t.creation_date").as("token_creation_date")
                .column("u.id").as("id")
                .column("u.username").as("username")
                .column("u.email").as("email")
                .column("u.password").as("password")
                .column("u.user_role").as("user_role")
                .column("u.rating").as("rating")
                .column("u.rating_deviation").as("rating_deviation")
                .column("u.rating_volatility").as("rating_volatility")
                .column("u.is_enable").as("is_enable")
                .column("u.creation_date").as("creation_date")
                .column("u.last_updated_date").as("last_updated_date")
                .from("UserToken t")
                .innerJoin("UserAccount u", "t.user_id = u.id")
                .where("t.token = ?")
                .build().sql());

        log();

        assertEquals("SELECT DISTINCT name FROM customers WHERE city = 'New York' ORDER BY name ", selectDistinct()
                .column("name")
                .from("customers")
                .where("city = 'New York'")
                .orderBy("name")
                .build().sql());

        log();

        assertEquals("SELECT COUNT(id) FROM orders WHERE status = 'shipped' AND total > 1000 ", select()
                .count("id")
                .from("orders")
                .where("status = 'shipped'")
                .and("total > 1000")
                .build().sql());

        log();

        assertEquals("SELECT name , CONCAT(first_name, last_name) AS full_name FROM employees ", select()
                .columns("name")
                .concat("first_name", "last_name")
                .as("full_name")
                .from("employees")
                .build().sql());

        log();

        assertEquals("SELECT name , CONCAT(first_name, last_name) AS full_name FROM employees ", select()
                .column("name")
                .concat("first_name", "last_name")
                .as("full_name")
                .from("employees")
                .build().sql());

        log();

        assertEquals("SELECT * FROM products WHERE price > 100 ORDER BY price DESC LIMIT 5 OFFSET 0 ", select()
                .all()
                .from("products")
                .where("price > 100")
                .orderBy("price", Order.DESC)
                .limitAndOffset(5, 0).sql());

        log();

        assertEquals("SELECT name, age FROM users WHERE age BETWEEN 18 AND 30 ", select()
                .columns("name", "age")
                .from("users")
                .where("age BETWEEN 18 AND 30")
                .build().sql());

        log();

        assertEquals("WITH recent_orders AS (SELECT id FROM orders WHERE created_at > '2024-01-01') SELECT * FROM recent_orders ",
                withAndSelect("recent_orders", SQLQuery.of("SELECT id FROM orders WHERE created_at > '2024-01-01'"))
                .columns("*")
                .from("recent_orders")
                .build().sql());

        log();

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active FROM users ", select()
                .columns("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .from("users")
                .build().sql());

        log();

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active FROM users ", select()
                .column("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .from("users")
                .build().sql());

        log();

        assertEquals("SELECT name FROM customers WHERE age > 30 OR city = 'Los Angeles' ", select()
                .column("name")
                .from("customers")
                .where("age > 30")
                .or("city = 'Los Angeles'")
                .build().sql());

        log();
    }

    @Test
    void test2() {
        assertEquals("SELECT DISTINCT name FROM customers WHERE city = 'New York' ORDER BY name ", selectDistinct()
                .column("name")
                .from("customers")
                .where("city = 'New York'")
                .orderBy("name")
                .build().sql());

        log();

        assertEquals("SELECT COUNT(id) FROM orders WHERE status = 'shipped' AND total > 1000 ", select()
                .count("id")
                .from("orders")
                .where("status = 'shipped'")
                .and("total > 1000")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM employees WHERE department_id IN (1, 2, 3) AND salary > 50000 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id IN (1, 2, 3)")
                .and("salary > 50000")
                .build().sql());

        log();


        assertEquals("SELECT * FROM orders WHERE status = 'pending' LIMIT 10 OFFSET 20 ", select()
                .all()
                .from("orders")
                .where("status = 'pending'")
                .limitAndOffset(10, 20).sql());

        log();

        assertEquals("SELECT category , AVG(price) AS avg_price FROM products GROUP BY category ", select()
                .columns("category")
                .avg("price")
                .as("avg_price")
                .from("products")
                .groupBy("category")
                .build().sql());

        log();

        assertEquals("SELECT category , COUNT(*) AS product_count FROM products GROUP BY category HAVING COUNT(*) > 10 ", select()
                .columns("category")
                .count("*")
                .as("product_count")
                .from("products")
                .groupBy("category")
                .having("COUNT(*) > 10")
                .build().sql());

        log();

        assertEquals("SELECT p.id, p.name FROM products p INNER JOIN categories c ON p.category_id = c.id WHERE c.name = 'Electronics' ", select()
                .columns("p.id", "p.name")
                .from("products p")
                .innerJoin("categories c", "p.category_id = c.id")
                .where("c.name = 'Electronics'")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM employees WHERE (status = 'active' AND age > 30) OR department = 'HR' ", select()
                .columns("id", "name")
                .from("employees")
                .where("(status = 'active' AND age > 30)")
                .or("department = 'HR'")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM users WHERE NOT (age < 18) ", select()
                .columns("id", "name")
                .from("users")
                .whereNot("(age < 18)")
                .build().sql());

        log();

        assertEquals("WITH active_users AS (SELECT id FROM users WHERE status = 'active') SELECT * FROM active_users ",
                withAndSelect("active_users", SQLQuery.of("SELECT id FROM users WHERE status = 'active'"))
                .columns("*")
                .from("active_users")
                .build().sql());

        log();

        assertEquals("SELECT name FROM products WHERE price BETWEEN 50 AND 100 ", select()
                .column("name")
                .from("products")
                .where("price BETWEEN 50 AND 100")
                .build().sql());

        log();

    }

    @Test
    void test3() {
        assertEquals("SELECT id, name FROM employees WHERE department_id = 3 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id = 3")
                .build().sql());

        log();

        assertEquals("SELECT name, age FROM users WHERE age >= 25 AND age <= 40 ", select()
                .columns("name", "age")
                .from("users")
                .where("age >= 25")
                .and("age <= 40")
                .build().sql());

        log();

        assertEquals("SELECT DISTINCT city FROM customers ", selectDistinct()
                .column("city")
                .from("customers")
                .build().sql());

        log();

        assertEquals("SELECT id, email FROM users WHERE email LIKE '%@example.com' ", select()
                .columns("id", "email")
                .from("users")
                .where("email LIKE '%@example.com'")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM products WHERE name IS NOT NULL ", select()
                .columns("id", "name")
                .from("products")
                .where("name IS NOT NULL")
                .build().sql());

        log();

        assertEquals("SELECT category , SUM(price) AS total_price FROM products GROUP BY category ORDER BY total_price DESC ", select()
                .columns("category")
                .sum("price")
                .as("total_price")
                .from("products")
                .groupBy("category")
                .orderBy("total_price", Order.DESC)
                .build().sql());

        log();

        assertEquals("SELECT name FROM users WHERE registration_date >= '2025-01-01' AND status = 'active' ", select()
                .column("name")
                .from("users")
                .where("registration_date >= '2025-01-01'")
                .and("status = 'active'")
                .build().sql());

        log();

        assertEquals("SELECT department_id , COUNT(*) AS employee_count FROM employees GROUP BY department_id HAVING COUNT(*) > 5 ", select()
                .columns("department_id")
                .count("*")
                .as("employee_count")
                .from("employees")
                .groupBy("department_id")
                .having("COUNT(*) > 5")
                .build().sql());

        log();

        assertEquals("SELECT id, name , CASE WHEN salary > 50000 THEN 'high' ELSE 'low' END AS salary_level FROM employees ", select()
                .columns("id", "name")
                .caseStatement()
                .when("salary > 50000")
                .then("'high'")
                .elseCase("'low'")
                .endAs("salary_level")
                .from("employees")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM users WHERE last_login IS NULL ", select()
                .columns("id", "name")
                .from("users")
                .where("last_login IS NULL")
                .build().sql());

        log();

        assertEquals("SELECT country , AVG(age) AS avg_age FROM users GROUP BY country ", select()
                .columns("country")
                .avg("age")
                .as("avg_age")
                .from("users")
                .groupBy("country")
                .build().sql());

        log();

        assertEquals("SELECT name , COUNT(*) AS order_count FROM customers c INNER JOIN orders o ON c.id = o.customer_id GROUP BY c.name ", select()
                .columns("name")
                .count("*")
                .as("order_count")
                .from("customers c")
                .innerJoin("orders o", "c.id = o.customer_id")
                .groupBy("c.name")
                .build().sql());

        log();

        assertEquals("SELECT name , MAX(salary) AS max_salary FROM employees GROUP BY department_id ", select()
                .columns("name")
                .max("salary")
                .as("max_salary")
                .from("employees")
                .groupBy("department_id")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM employees WHERE department_id = 5 OR salary > 60000 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id = 5")
                .or("salary > 60000")
                .build().sql());

        log();

        assertEquals("SELECT id, salary FROM employees WHERE NOT (department_id = 1 AND salary < 40000) ", select()
                .columns("id", "salary")
                .from("employees")
                .whereNot("(department_id = 1 AND salary < 40000)")
                .build().sql());

        log();

        assertEquals("SELECT id, name FROM users WHERE age BETWEEN 20 AND 30 ", select()
                .columns("id", "name")
                .from("users")
                .where("age BETWEEN 20 AND 30")
                .build().sql());

        log();

        assertEquals("SELECT * FROM orders WHERE created_at >= '2025-01-01' ORDER BY created_at ASC LIMIT 10 OFFSET 0 ", select()
                .all()
                .from("orders")
                .where("created_at >= '2025-01-01'")
                .orderBy("created_at", Order.ASC)
                .limitAndOffset(10, 0).sql());

        log();

        assertEquals("SELECT category , SUM(price) AS total_price FROM products WHERE category IS NOT NULL GROUP BY category HAVING SUM(price) > 1000 ", select()
                .columns("category")
                .sum("price")
                .as("total_price")
                .from("products")
                .where("category IS NOT NULL")
                .groupBy("category")
                .having("SUM(price) > 1000")
                .build().sql());

        log();
    }

    @Test
    void test4() {
        assertEquals(
                "SELECT a.id AS id, a.author_id AS author_id, a.header AS header, a.summary AS summary, " +
                        "a.body AS body, a.status AS status, a.creation_date AS creation_date, a.last_updated AS last_updated , " +
                        "COUNT(v.id) AS views , COUNT(l.article_id) AS likes " +
                        "FROM Articles a JOIN Views v ON v.article_id = a.id " +
                        "JOIN Likes l ON l.article_id = a.id WHERE a.id = ? ",
                select()
                        .column("a.id").as("id")
                        .column("a.author_id").as("author_id")
                        .column("a.header").as("header")
                        .column("a.summary").as("summary")
                        .column("a.body").as("body")
                        .column("a.status").as("status")
                        .column("a.creation_date").as("creation_date")
                        .column("a.last_updated").as("last_updated")
                        .count("v.id").as("views")
                        .count("l.article_id").as("likes")
                        .from("Articles a")
                        .join("Views v", "v.article_id = a.id")
                        .join("Likes l", "l.article_id = a.id")
                        .where("a.id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT tag FROM ArticleTags WHERE article_id = ? ",
                select()
                        .column("tag")
                        .from("ArticleTags")
                        .where("article_id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT COUNT(user_id) FROM Views WHERE article_id = ? AND user_id = ? ",
                select()
                        .count("user_id")
                        .from("Views")
                        .where("article_id = ?")
                        .and("user_id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT COUNT(id) FROM Articles WHERE id = ? ",
                select()
                        .count("id")
                        .from("Articles")
                        .where("id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT COUNT(v.id) FROM Views v JOIN UserAccount u ON u.username = ? WHERE v.reader_id = u.id ",
                select()
                        .count("v.id")
                        .from("Views v")
                        .join("UserAccount u", "u.username = ?")
                        .where("v.reader_id = u.id")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT a.id AS id, a.author_id AS author_id, a.header AS header, a.summary AS summary, a.status AS status, " +
                        "a.last_updated AS last_updated, ath.firstname AS firstname, ath.lastname AS lastname, ath.username AS username , " +
                        "COUNT(v.id) AS views , COUNT(l.article_id) AS likes " +
                        "FROM Article a JOIN Views v ON v.article_id = a.id " +
                        "JOIN Likes l ON l.article_id = a.id JOIN UserAccount ath ON ath.id = a.author_id " +
                        "WHERE a.status = 'PUBLISHED' ORDER BY COUNT(v.id) DESC ",
                select()
                        .column("a.id").as("id")
                        .column("a.author_id").as("author_id")
                        .column("a.header").as("header")
                        .column("a.summary").as("summary")
                        .column("a.status").as("status")
                        .column("a.last_updated").as("last_updated")
                        .column("ath.firstname").as("firstname")
                        .column("ath.lastname").as("lastname")
                        .column("ath.username").as("username")
                        .count("v.id").as("views")
                        .count("l.article_id").as("likes")
                        .from("Article a")
                        .join("Views v", "v.article_id = a.id")
                        .join("Likes l", "l.article_id = a.id")
                        .join("UserAccount ath", "ath.id = a.author_id")
                        .where("a.status = 'PUBLISHED'")
                        .orderBy("COUNT(v.id)", Order.DESC)
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "WITH recent_articles AS (" +
                        "SELECT a.header AS header, a.summary AS summary, a.body AS body " +
                        "FROM Article a JOIN Views v ON v.article_id = a.id JOIN UserAccount u ON u.username = ? " +
                        "WHERE v.reader_id = u.id ORDER BY v.creation_date DESC LIMIT 12 OFFSET 0 ) " +
                        "SELECT a.id AS id, a.author_id AS author_id, a.header AS header, a.summary AS summary, " +
                        "a.status AS status, a.last_updated AS last_updated, ath.firstname AS firstname, " +
                        "ath.lastname AS lastname, ath.username AS username , COUNT(v.id) AS views , " +
                        "COUNT(l.article_id) AS likes FROM Article a JOIN Views v ON v.article_id = a.id " +
                        "JOIN Likes l ON l.article_id = a.id JOIN UserAccount ath ON ath.id = a.author_id " +
                        "WHERE " +
                        """
                        a.search_document @@ to_tsquery('english',
                              (SELECT string_agg(header, ' & ') FROM recent_articles) ||
                              ' & ' ||
                              (SELECT string_agg(summary, ' & ') FROM recent_articles) ||
                              ' & ' ||
                              (SELECT string_agg(body, ' & ') FROM recent_articles))
                        """ +
                        " AND a.status = 'PUBLISHED' LIMIT 10 OFFSET 0 ",
                withAndSelect(
                        "recent_articles", select()
                                .column("a.header").as("header")
                                .column("a.summary").as("summary")
                                .column("a.body").as("body")
                                .from("Article a")
                                .join("Views v", "v.article_id = a.id")
                                .join("UserAccount u", "u.username = ?")
                                .where("v.reader_id = u.id")
                                .orderBy("v.creation_date", Order.DESC)
                                .limitAndOffset(12, 0)
                                .toSQlQuery()
                )
                        .column("a.id").as("id")
                        .column("a.author_id").as("author_id")
                        .column("a.header").as("header")
                        .column("a.summary").as("summary")
                        .column("a.status").as("status")
                        .column("a.last_updated").as("last_updated")
                        .column("ath.firstname").as("firstname")
                        .column("ath.lastname").as("lastname")
                        .column("ath.username").as("username")
                        .count("v.id").as("views")
                        .count("l.article_id").as("likes")
                        .from("Article a")
                        .join("Views v", "v.article_id = a.id")
                        .join("Likes l", "l.article_id = a.id")
                        .join("UserAccount ath", "ath.id = a.author_id")
                        .where("""
                        a.search_document @@ to_tsquery('english',
                              (SELECT string_agg(header, ' & ') FROM recent_articles) ||
                              ' & ' ||
                              (SELECT string_agg(summary, ' & ') FROM recent_articles) ||
                              ' & ' ||
                              (SELECT string_agg(body, ' & ') FROM recent_articles))
                        """)
                        .and("a.status = 'PUBLISHED'")
                        .limitAndOffset(10, 0)
                        .sql()
        );
    }

    @Test
    void test5() {
        assertEquals(
                "SELECT id FROM Comments WHERE id = ? ",
                select()
                        .column("id")
                        .from("Comments")
                        .where("id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT id , article_id , user_id , comment_type FROM Comments WHERE id = ? ",
                select()
                        .column("id")
                        .column("article_id")
                        .column("user_id")
                        .column("comment_type")
                        .from("Comments")
                        .where("id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT c.id AS id, c.article_id AS article_id, c.user_id AS user_id, c.comment_type AS comment_type, " +
                        "c.parent_comment_id AS parent_comment_id, c.respond_to_comment AS respond_to_comment, " +
                        "c.creation_date AS creation_date, c.last_updated AS last_updated , " +
                        "COUNT(cl.comment_id) AS likes_count " +
                        "FROM Comments c JOIN CommentLikes cl ON cl.comment_id = c.id WHERE c.id = ? ",
                select()
                        .column("c.id").as("id")
                        .column("c.article_id").as("article_id")
                        .column("c.user_id").as("user_id")
                        .column("c.comment_type").as("comment_type")
                        .column("c.parent_comment_id").as("parent_comment_id")
                        .column("c.respond_to_comment").as("respond_to_comment")
                        .column("c.creation_date").as("creation_date")
                        .column("c.last_updated").as("last_updated")
                        .count("cl.comment_id").as("likes_count")
                        .from("Comments c")
                        .join("CommentLikes cl", "cl.comment_id = c.id")
                        .where("c.id = ?")
                        .build()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT c.id AS id, c.article_id AS article_id, c.user_id AS user_id, c.comment_type AS comment_type, " +
                        "c.parent_comment_id AS parent_comment_id, c.respond_to_comment AS respond_to_comment, " +
                        "c.creation_date AS creation_date, c.last_updated AS last_updated , " +
                        "COUNT(cl.comment_id) AS likes_count " +
                        "FROM Comments c JOIN CommentLikes cl ON cl.comment_id = c.id " +
                        "WHERE c.article_id = ? ORDER BY likes_count DESC LIMIT ? OFFSET ? ",
                select()
                        .column("c.id").as("id")
                        .column("c.article_id").as("article_id")
                        .column("c.user_id").as("user_id")
                        .column("c.comment_type").as("comment_type")
                        .column("c.parent_comment_id").as("parent_comment_id")
                        .column("c.respond_to_comment").as("respond_to_comment")
                        .column("c.creation_date").as("creation_date")
                        .column("c.last_updated").as("last_updated")
                        .count("cl.comment_id").as("likes_count")
                        .from("Comments c")
                        .join("CommentLikes cl", "cl.comment_id = c.id")
                        .where("c.article_id = ?")
                        .orderBy("likes_count", Order.DESC)
                        .limitAndOffset()
                        .sql()
        );

        log();

        assertEquals(
                "SELECT c.id AS id, c.article_id AS article_id, c.user_id AS user_id, c.comment_type AS comment_type, " +
                        "c.parent_comment_id AS parent_comment_id, c.respond_to_comment AS respond_to_comment, " +
                        "c.creation_date AS creation_date, c.last_updated AS last_updated , " +
                        "COUNT(cl.comment_id) AS likes_count " +
                        "FROM Comments c JOIN CommentLikes cl ON cl.comment_id = c.id " +
                        "WHERE c.article_id = ? AND c.parent_comment_id = ? " +
                        "ORDER BY likes_count DESC LIMIT ? OFFSET ? ",
                select()
                        .column("c.id").as("id")
                        .column("c.article_id").as("article_id")
                        .column("c.user_id").as("user_id")
                        .column("c.comment_type").as("comment_type")
                        .column("c.parent_comment_id").as("parent_comment_id")
                        .column("c.respond_to_comment").as("respond_to_comment")
                        .column("c.creation_date").as("creation_date")
                        .column("c.last_updated").as("last_updated")
                        .count("cl.comment_id").as("likes_count")
                        .from("Comments c")
                        .join("CommentLikes cl", "cl.comment_id = c.id")
                        .where("c.article_id = ?")
                        .and("c.parent_comment_id = ?")
                        .orderBy("likes_count", Order.DESC)
                        .limitAndOffset()
                        .sql()
        );
    }

    static void log() {
        Log.info("Test %d passed.".formatted(++passesTests));
    }
}