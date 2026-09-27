package de.schnitzel.shelfifyapi.controller

import de.schnitzel.shelfifyapi.entities.UserEntity
import de.schnitzel.shelfifyapi.repositories.UserRepository
import de.schnitzel.shelfifyapi.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class RefreshRequest(val refreshToken: String)
data class AuthResponse(val accessToken: String, val refreshToken: String, val id: Int)

@RestController
class UserAccController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    @GetMapping("/appSync")
    fun appSync(): ResponseEntity<AuthResponse> {
        val datagroup = UUID.randomUUID().toString()

        // 1. User vorerst ohne Refresh Token anlegen
        val newUser = userRepository.save(
            UserEntity(
                datagroup = datagroup,
                ownDatagroup = datagroup
            )
        )

        val accessToken = jwtService.generateAccessToken(newUser.id)
        val refreshToken = jwtService.generateRefreshToken(newUser.id)

        newUser.refreshToken = refreshToken
        userRepository.save(newUser)

        return ResponseEntity.ok(
            AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                id = newUser.id
            )
        )
    }

    @PostMapping("/refreshToken")
    fun refreshToken(@RequestBody request: RefreshRequest): ResponseEntity<Map<String, String>> {
        val token = request.refreshToken

        if (!jwtService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val userId = jwtService.extractUserId(token)
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (user.refreshToken != token) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val newAccessToken = jwtService.generateAccessToken(userId)

        return ResponseEntity.ok(mapOf("accessToken" to newAccessToken))
    }
}