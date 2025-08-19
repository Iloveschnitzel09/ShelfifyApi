package com.lager.lagerappapi.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationSettingsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveEmail(String email, int id) {
        jdbcTemplate.update(
                "UPDATE users SET email = ?, notify = FALSE, verified = FALSE WHERE id = ?",
                email, id
        );
    }

    public List<String> getAllNotificationEmails() {
        return jdbcTemplate.queryForList(
            "SELECT email FROM users WHERE notify = TRUE",
            String.class
        );
    }

    public void setCode(String code, int id) {
        jdbcTemplate.update(
                "UPDATE users SET verification_code = ? WHERE id = ?",
                code, id
        );
    }

    public String getEmail(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?",
                String.class,
                userId
        );
    }

    public boolean checkEmail(String email) {
        List<String> emails = getAllNotificationEmails();
        for (String e : emails) {
            if (e != null && e.equals(email)) {
                return false;
            }
        }

        return true;
    }

    public String getToken(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT token from users WHERE id = ?;",
                String.class,
                userId
        );
    }
}