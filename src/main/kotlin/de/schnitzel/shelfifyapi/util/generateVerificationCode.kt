package de.schnitzel.shelfifyapi.util

import java.util.*

fun generateVerificationCode(): String {
    val random = Random()
    val code = StringBuilder()
    for (i in 0..5) {
        code.append(random.nextInt(10))
    }
    return code.toString()
}