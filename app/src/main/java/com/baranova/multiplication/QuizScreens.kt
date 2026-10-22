@file:OptIn(ExperimentalMaterial3Api::class)

package com.baranova.multiplication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Green = Color(0xFF1B8A4B)
private val GreenSoft = Color(0xFFD7F5E3)
private val RedSoft = Color(0xFFFFDDE2)

private fun formatTime(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
fun QuizScreen(
    questions: List<Question>,
    mode: Mode,
    timeLimitSec: Int,
    soundOn: Boolean,
    speak: (String) -> Unit,
    onFinish: (correct: Int, answered: Int, mistakes: List<Question>) -> Unit,
    onExit: () -> Unit
) {
    val timed = mode == Mode.Timed
    val auto = mode != Mode.Manual

    var index by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<Boolean?>(null) }
    var correct by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(timeLimitSec) }
    var finished by remember { mutableStateOf(false) }
    val mistakes = remember { mutableStateListOf<Question>() }

    val q = questions[index]
    val last = index == questions.lastIndex

    BackHandler(onBack = onExit)

    fun finish() {
        if (finished) return
        finished = true
        onFinish(correct, answered, mistakes.toList())
    }

    fun check() {
        if (finished || verdict != null || input.isEmpty()) return
        val ok = input.toIntOrNull() == q.answer
        verdict = ok
        answered++
        if (ok) correct++ else mistakes.add(q)
    }

    fun next() {
        if (finished) return
        if (last) {
            finish()
        } else {
            index++
            input = ""
            verdict = null
        }
    }

    // Озвучка вопроса.
    LaunchedEffect(index) {
        speak("${q.a} умножить на ${q.b}")
    }

    // Озвучка результата ответа (в режиме «На время» не озвучиваем, чтобы не тормозить).
    LaunchedEffect(verdict) {
        if (!timed) {
            when (verdict) {
                true -> speak("Верно!")
                false -> speak("Неверно. ${q.a} умножить на ${q.b} равно ${q.answer}")
                null -> {}
            }
        }
    }

    // Автоматический переход к следующему примеру.
    LaunchedEffect(index, verdict) {
        if (auto && verdict != null) {
            val pauseMs = when {
                timed && verdict == true -> 500L
                timed -> 1500L
                verdict == true -> 1100L
                soundOn -> 3500L
                else -> 2500L
            }
            delay(pauseMs)
            next()
        }
    }

    // Таймер для режима «На время».
    LaunchedEffect(Unit) {
        if (timed) {
            while (timeLeft > 0 && !finished) {
                delay(1000)
                timeLeft--
            }
            finish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕ Выйти", fontSize = 16.sp) }
            if (soundOn) {
                TextButton(onClick = { speak("${q.a} умножить на ${q.b}") }) {
                    Text("🔊", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            if (timed) {
                Text(
                    "Верно: $correct   ⏱ ${formatTime(timeLeft)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "${index + 1} из ${questions.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = {
                if (timed) {
                    timeLeft.toFloat() / timeLimitSec
                } else {
                    (index + if (verdict != null) 1 else 0).toFloat() / questions.size
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "${q.a} × ${q.b}",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            val boxColor = when (verdict) {
                true -> GreenSoft
                false -> RedSoft
                null -> MaterialTheme.colorScheme.primaryContainer
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(boxColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (input.isEmpty()) "?" else input,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (input.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when (verdict) {
                    true -> Text(
                        "Верно! 🎉",
                        color = Green,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    false -> Text(
                        "Правильный ответ: ${q.answer}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    null -> {}
                }
            }
        }

        if (verdict == null) {
            NumPad(
                onDigit = { d ->
                    if (input.length < 3) {
                        input = if (input == "0") d else input + d
                        // В режимах с автопереходом ответ проверяется, как только набрано нужное число цифр.
                        if (auto && input.length == q.answer.toString().length) {
                            check()
                        }
                    }
                },
                onBackspace = { input = input.dropLast(1) },
                onOk = { check() }
            )
        } else {
            Button(
                onClick = { next() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (last) "Результат" else "Дальше", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun NumPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onOk: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    val digitBg = Color.White
    val digitFg = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { d ->
                    PadKey(d, Modifier.weight(1f), digitBg, digitFg) { onDigit(d) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PadKey(
                "⌫",
                Modifier.weight(1f),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.error
            ) { onBackspace() }
            PadKey("0", Modifier.weight(1f), digitBg, digitFg) { onDigit("0") }
            PadKey(
                "OK",
                Modifier.weight(1f),
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary
            ) { onOk() }
        }
    }
}

@Composable
private fun PadKey(
    label: String,
    modifier: Modifier,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResultScreen(
    timed: Boolean,
    correct: Int,
    answered: Int,
    total: Int,
    mistakes: List<Question>,
    best: Int,
    newRecord: Boolean,
    onAgain: () -> Unit,
    onMenu: () -> Unit
) {
    val pct = when {
        timed && answered == 0 -> 0
        timed -> correct * 100 / answered
        else -> correct * 100 / total
    }
    val stars = if (timed) {
        when {
            correct >= 20 -> 3
            correct >= 12 -> 2
            correct >= 6 -> 1
            else -> 0
        }
    } else {
        when {
            pct >= 90 -> 3
            pct >= 70 -> 2
            pct >= 40 -> 1
            else -> 0
        }
    }
    val title = when {
        timed -> "Время вышло!"
        pct == 100 -> "Безупречно!"
        pct >= 90 -> "Великолепно!"
        pct >= 70 -> "Хорошо!"
        pct >= 40 -> "Неплохо!"
        else -> "Давай потренируемся ещё"
    }
    val summary = if (timed) {
        "Верно: $correct из $answered"
    } else {
        "Правильно: $correct из $total ($pct%)"
    }
    val bestText = if (timed) "Рекорд: $best" else "Лучший результат: $best%"

    BackHandler(onBack = onMenu)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = buildString {
                repeat(3) { i -> append(if (i < stars) "★" else "☆") }
            },
            fontSize = 56.sp,
            color = Color(0xFFFFB300)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (newRecord) {
            Text(
                "🏆 Новый рекорд!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (best >= 0) {
            Text(
                bestText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mistakes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Над чем поработать",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mistakes.distinct().forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RedSoft)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${q.a} × ${q.b}", fontSize = 22.sp)
                        Text("= ${q.answer}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Ещё раз", fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("В меню", fontSize = 18.sp)
        }
    }
}
