package com.baranova.multiplication

import kotlin.random.Random

data class Question(val a: Int, val b: Int) {
    val answer: Int get() = a * b
}

object Trainer {
    /** Собирает список из [total] примеров на выбранные числа (множитель 2..10), в случайном порядке. */
    fun build(numbers: Set<Int>, total: Int): List<Question> {
        val pool = numbers.flatMap { n ->
            (2..10).map { m ->
                if (Random.nextBoolean()) Question(n, m) else Question(m, n)
            }
        }
        val result = mutableListOf<Question>()
        while (result.size < total) {
            result += pool.shuffled()
        }
        return result.take(total)
    }

    /** Ключ режима для хранения лучшего результата. */
    fun key(numbers: Set<Int>, total: Int): String =
        numbers.sorted().joinToString("-") + "_" + total
}
