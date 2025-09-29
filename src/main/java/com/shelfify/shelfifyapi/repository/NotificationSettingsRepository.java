package com.shelfify.shelfifyapi.repository;

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

    public List<String> getAllEmails() {
        return jdbcTemplate.queryForList(
                "SELECT email FROM users WHERE email IS NOT NULL",
                String.class
        );
    }

    public void setCode(String code, int id) {
        jdbcTemplate.update(
                "UPDATE users SET verification_code = ? WHERE id = ?",
                code, id
        );
    }

    public boolean checkEmail(String email) {
        List<String> emails = getAllEmails();
        for (String e : emails) {
            if (e != null && e.equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    public String getToken(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT token from users WHERE id = ?;",
                String.class,
                userId
        );
    }

    public boolean checkBlocked(String email, int id) {
        List<String> blockedEmails = jdbcTemplate.queryForList(
                "SELECT blocked_email FROM users u JOIN blocked_datagroups b ON u.datagroup = b.datagroup WHERE id = ?;",
                String.class,
                id
        );
        return blockedEmails.contains(email);
    }

    public String getDatagroup(int id) {
        return jdbcTemplate.queryForObject(
                "SELECT datagroup FROM users WHERE id = ?;",
                String.class,
                id
        );
    }
}