package de.schnitzel.shelfifyapi.util

import java.security.SecureRandom

fun generateCode(): String {
    val random = SecureRandom()
    val code = StringBuilder()
    for (i in 0..5) {
        code.append(random.nextInt(10))
    }
    return code.toString()
}