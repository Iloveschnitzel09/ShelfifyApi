package com.shelfify.shelfifyapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

@Repository
public class DatagroupService {

    @Autowired
    private JdbcTemplate jdbc;

    public String createInvitationCode(String datagroup) {
        String invitationCode = generateVerificationCode();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO invitation_codes (code, datagroup, expires_at) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, invitationCode); // ← Datagroup generieren
            ps.setString(2, datagroup);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis() + 5 * 60 * 1000)); // 5 Minuten Gültigkeit
            return ps;
        });
        return invitationCode;
    }

    public String getDatagroupByCode(String code) {
        try {
            return jdbc.queryForObject(
                    "SELECT datagroup FROM invitation_codes WHERE code = ? AND expires_at > ?",
                    String.class,
                    code,
                    new Timestamp(System.currentTimeMillis())
            );
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getEmailsFromDatagroup(String datagroup) {
        return jdbc.queryForList(
                "SELECT email FROM users WHERE datagroup = ?",
                String.class,
                datagroup
        );
    }

    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
