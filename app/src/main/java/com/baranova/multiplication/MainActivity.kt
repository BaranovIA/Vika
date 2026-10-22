package com.baranova.multiplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

sealed interface Screen {
    data object Menu : Screen
    data class Learn(val number: Int) : Screen
    data object Setup : Screen
    data class Quiz(val numbers: Set<Int>, val total: Int) : Screen
    data class Result(
        val numbers: Set<Int>,
        val total: Int,
        val correct: Int,
        val mistakes: List<Question>,
        val best: Int,
        val newRecord: Boolean
    ) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(applicationContext)
        setContent {
            TrainerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(prefs)
                }
            }
        }
    }
}

@Composable
fun App(prefs: Prefs) {
    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }

    when (val s = screen) {
        is Screen.Menu -> {
            val solved = remember { prefs.totalSolved() }
            MenuScreen(
                solved = solved,
                onLearn = { screen = Screen.Learn(2) },
                onSetup = { screen = Screen.Setup },
                onQuick = { screen = Screen.Quiz((2..9).toSet(), 20) }
            )
        }

        is Screen.Learn -> {
            BackHandler { screen = Screen.Menu }
            LearnScreen(
                number = s.number,
                onPick = { screen = Screen.Learn(it) },
                onTest = { n -> screen = Screen.Quiz(setOf(n), 10) },
                onBack = { screen = Screen.Menu }
            )
        }

        is Screen.Setup -> {
            BackHandler { screen = Screen.Menu }
            SetupScreen(
                onStart = { numbers, total -> screen = Screen.Quiz(numbers, total) },
                onBack = { screen = Screen.Menu }
            )
        }

        is Screen.Quiz -> {
            val questions = remember(s) { Trainer.build(s.numbers, s.total) }
            QuizScreen(
                questions = questions,
                onFinish = { correct, mistakes ->
                    val key = Trainer.key(s.numbers, s.total)
                    val pct = correct * 100 / s.total
                    val prev = prefs.best(key)
                    prefs.saveBest(key, pct)
                    prefs.addSolved(correct)
                    screen = Screen.Result(
                        numbers = s.numbers,
                        total = s.total,
                        correct = correct,
                        mistakes = mistakes,
                        best = maxOf(prev, pct),
                        newRecord = prev >= 0 && pct > prev
                    )
                },
                onExit = { screen = Screen.Menu }
            )
        }

        is Screen.Result -> {
            ResultScreen(
                correct = s.correct,
                total = s.total,
                mistakes = s.mistakes,
                best = s.best,
                newRecord = s.newRecord,
                onAgain = { screen = Screen.Quiz(s.numbers, s.total) },
                onMenu = { screen = Screen.Menu }
            )
        }
    }
}
