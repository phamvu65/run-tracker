package com.example.runtracker.domain.beacon

import kotlin.random.Random

/**
 * Mã beacon 6 ký tự để chia sẻ (bỏ các ký tự dễ nhầm: I, O, 0, 1).
 * Thuần JVM.
 */
object BeaconCode {

    const val LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun random(random: Random = Random.Default): String =
        buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Chuẩn hoá đầu vào người dùng: viết hoa, bỏ khoảng trắng và ký tự lạ. */
    fun normalize(input: String): String =
        input.trim().uppercase().filter { it in ALPHABET }

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
