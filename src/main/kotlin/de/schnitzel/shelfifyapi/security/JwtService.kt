package de.schnitzel.shelfifyapi.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value($$"${jwt.secret}") private val secret: String,
) {
    private val accessTokenExpiration: Long = 30 * 60 * 1000
    private val refreshTokenExpiration: Long = 100L * 365 * 24 * 60 * 60 * 1000

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: Int): String {
        return buildToken(userId, accessTokenExpiration)
    }

    fun generateRefreshToken(userId: Int): String {
        return buildToken(userId, refreshTokenExpiration)
    }

    private fun buildToken(userId: Int, expirationMs: Long): String {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact()
    }

    fun extractUserId(token: String): Int {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
            .toInt()
    }

    fun isValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}