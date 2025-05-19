package com.hadzhy.jetquerious.jdbc;

import com.hadzhy.jetquerious.config.DBConfig;
import com.hadzhy.jetquerious.sql.QueryForge;
import com.hadzhy.jetquerious.util.Result;
import com.hadzhy.jetquerious.util.TestDataGenerator;
import com.hadzhy.jetquerious.util.UserForm;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JetQueriousExecuteTest extends DBConfig {

    private final JetQuerious jet = JetQuerious.instance();

    @Test
    void validJetQueriousExecute() {
        String insertUser = QueryForge.insert()
                .into("user_account")
                .column("id")
                .column("email")
                .column("username")
                .column("hashed_password")
                .column("is_active")
                .column("created_at")
                .column("updated_at")
                .values()
                .build()
                .sql();

        UserForm userForm = TestDataGenerator.userForm(true);
        Result<Integer, Throwable> execute = jet.execute(insertUser, PreparedStatement::executeUpdate, userForm.id(),
                userForm.email(),
                userForm.username(),
                userForm.password(),
                userForm.isActive(),
                userForm.createdAt(),
                userForm.lastUpdatedAt());

        String sql = QueryForge.select()
                .all()
                .from("user_account")
                .where("id = ?")
                .build()
                .sql();

        Result<UserForm, Throwable> result = jet.execute(
                sql,
                statement -> {
                    ResultSet rs = statement.executeQuery();
                    return new UserForm(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("hashed_password"),
                            rs.getBoolean("is_active"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    );
                },
                userForm.id()
        );

        assertTrue(result.success());
        assertEquals(result.value(), userForm);
    }
}
