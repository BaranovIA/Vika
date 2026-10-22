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

/** Длительность режима «На время», секунд. */
const val TIMED_SECONDS = 60

/** Manual — кнопка «Дальше», Auto — автопереход, Timed — на время. */
enum class Mode { Manual, Auto, Timed }

sealed interface Screen {
    data object Menu : Screen
    data class Learn(val number: Int) : Screen
    data object Setup : Screen
    data class Quiz(val numbers: Set<Int>, val total: Int, val mode: Mode) : Screen
    data class Result(
        val numbers: Set<Int>,
        val total: Int,
        val mode: Mode,
        val correct: Int,
        val answered: Int,
        val mistakes: List<Question>,
        val best: Int,
        val newRecord: Boolean
    ) : Screen
}

class MainActivity : ComponentActivity() {
    private var speaker: Speaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(applicationContext)
        val voice = Speaker(this)
        speaker = voice
        setContent {
            TrainerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(prefs, voice)
                }
            }
        }
    }

    override fun onDestroy() {
        speaker?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun App(prefs: Prefs, speaker: Speaker) {
    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
    var soundOn by remember { mutableStateOf(prefs.soundOn) }

    val say: (String) -> Unit = { text -> if (soundOn) speaker.speak(text) }

    when (val s = screen) {
        is Screen.Menu -> {
            val solved = remember { prefs.totalSolved() }
            val bestTimed = remember { prefs.best("timed_$TIMED_SECONDS") }
            MenuScreen(
                solved = solved,
                bestTimed = bestTimed,
                soundOn = soundOn,
                onToggleSound = {
                    soundOn = !soundOn
                    prefs.soundOn = soundOn
                    if (!soundOn) speaker.stop()
                },
                onLearn = { screen = Screen.Learn(2) },
                onSetup = { screen = Screen.Setup },
                onQuick = { screen = Screen.Quiz((2..9).toSet(), 20, Mode.Auto) },
                onTimed = { screen = Screen.Quiz((2..9).toSet(), 200, Mode.Timed) }
            )
        }

        is Screen.Learn -> {
            BackHandler { screen = Screen.Menu }
            LearnScreen(
                number = s.number,
                onPick = { screen = Screen.Learn(it) },
                onTest = { n -> screen = Screen.Quiz(setOf(n), 10, Mode.Manual) },
                onBack = { screen = Screen.Menu }
            )
        }

        is Screen.Setup -> {
            BackHandler { screen = Screen.Menu }
            SetupScreen(
                onStart = { numbers, total, auto ->
                    screen = Screen.Quiz(numbers, total, if (auto) Mode.Auto else Mode.Manual)
                },
                onBack = { screen = Screen.Menu }
            )
        }

        is Screen.Quiz -> {
            val questions = remember(s) { Trainer.build(s.numbers, s.total) }
            QuizScreen(
                questions = questions,
                mode = s.mode,
                timeLimitSec = TIMED_SECONDS,
                soundOn = soundOn,
                speak = say,
                onFinish = { correct, answered, mistakes ->
                    val timed = s.mode == Mode.Timed
                    val key = if (timed) {
                        "timed_$TIMED_SECONDS"
                    } else {
                        Trainer.key(s.numbers, s.total)
                    }
                    val score = if (timed) correct else correct * 100 / s.total
                    val prev = prefs.best(key)
                    prefs.saveBest(key, score)
                    prefs.addSolved(correct)
                    speaker.stop()
                    screen = Screen.Result(
                        numbers = s.numbers,
                        total = s.total,
                        mode = s.mode,
                        correct = correct,
                        answered = answered,
                        mistakes = mistakes,
                        best = maxOf(prev, score),
                        newRecord = prev >= 0 && score > prev
                    )
                },
                onExit = {
                    speaker.stop()
                    screen = Screen.Menu
                }
            )
        }

        is Screen.Result -> {
            ResultScreen(
                timed = s.mode == Mode.Timed,
                correct = s.correct,
                answered = s.answered,
                total = s.total,
                mistakes = s.mistakes,
                best = s.best,
                newRecord = s.newRecord,
                onAgain = { screen = Screen.Quiz(s.numbers, s.total, s.mode) },
                onMenu = { screen = Screen.Menu }
            )
        }
    }
}
