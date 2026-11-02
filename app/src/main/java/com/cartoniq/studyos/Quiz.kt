package com.cartoniq.studyos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ModeChips(mode: Int, onMode: (Int) -> Unit) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Cards", "Quizzes").forEachIndexed { i, l -> FilterChip(selected = mode == i, onClick = { onMode(i) }, label = { Text(l) }) }
    }
}

@Composable
fun RevisionScreen(vm: StudyVm) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    if (mode == 0) CardsPane(vm, mode) { mode = it } else QuizPane(vm, mode) { mode = it }
}

@Composable
fun QuizPane(vm: StudyVm, mode: Int, onMode: (Int) -> Unit) {
    val qs by vm.questions.collectAsStateWithLifecycle()
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val msg by vm.toast.collectAsStateWithLifecycle()
    var play by remember { mutableStateOf<List<Question>?>(null) }
    var count by remember { mutableIntStateOf(5) }
    var timer by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf<String?>(null) }
    var edit by remember { mutableStateOf<Question?>(null) }
    var showImport by remember { mutableStateOf(false) }
    val host = remember { SnackbarHostState() }
    LaunchedEffect(msg) { msg?.let { host.showSnackbar(it); vm.toast.value = null } }
    play?.let { list -> QuizPlay(vm, list, timer) { play = null }; return }
    val topics = qs.map { it.topic }.filter { it.isNotBlank() }.distinct().sorted()
    val pool = qs.filter { topic == null || it.topic == topic }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Column { Text("Revision", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); ModeChips(mode, onMode) } }
            item {
                Card2 {
                    Text("${qs.size} question${if (qs.size == 1) "" else "s"} in your bank", fontWeight = FontWeight.SemiBold)
                    attempts.firstOrNull()?.let { a ->
                        Text("Last score: ${a.score}/${a.total} (${(a.score * 100f / a.total).roundToInt()}%)", color = MaterialTheme.colorScheme.primary)
                    }
                    if (topics.isNotEmpty()) {
                        Text("Topic", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = topic == null, onClick = { topic = null }, label = { Text("All") })
                            topics.forEach { t -> FilterChip(selected = topic == t, onClick = { topic = if (topic == t) null else t }, label = { Text(t) }) }
                        }
                    }
                    Text("Questions", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5 to "5", 10 to "10", 0 to "All").forEach { (n, l) -> FilterChip(selected = count == n, onClick = { count = n }, label = { Text(l) }) }
                    }
                    Text("Timer per question", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Off", 15 to "15 s", 30 to "30 s").forEach { (n, l) -> FilterChip(selected = timer == n, onClick = { timer = n }, label = { Text(l) }) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(enabled = pool.isNotEmpty(), onClick = { play = pool.shuffled().let { if (count == 0) it else it.take(count) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Start quiz") }
                    OutlinedButton({ showImport = true }, Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !importing) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(if (importing) "Importing…" else "Import from Open Trivia DB")
                    }
                }
            }
            if (attempts.isNotEmpty()) {
                item { Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(attempts.take(5), key = { "a${it.id}" }) { a ->
                    Card2 { Row {
                        Text(SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(a.takenAt)), Modifier.weight(1f))
                        Text("${a.score}/${a.total}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    } }
                }
            }
            item { Text("Question bank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (qs.isEmpty()) item { EmptyState("Add your own questions with +, or import free ones from Open Trivia DB.") }
            items(qs, key = { "q${it.id}" }) { q ->
                Card2(Modifier.clip(RoundedCornerShape(18.dp)).clickable { edit = q }) {
                    Text(q.prompt, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Answer: ${q.options()[q.correct]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (q.topic.isNotBlank()) Text(q.topic + if (q.source.isNotBlank()) " · ${q.source}" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        FloatingActionButton({ edit = Question(prompt = "", o1 = "", o2 = "") }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "Add question") }
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }

    if (showImport) {
        var cat by remember { mutableIntStateOf(0) }
        var diff by remember { mutableStateOf("") }
        var n by remember { mutableIntStateOf(10) }
        AlertDialog(
            onDismissRequest = { showImport = false }, title = { Text("Import questions") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Downloads free questions from Open Trivia DB (opentdb.com) and saves them on your device. Internet is needed only for this step. Duplicates are skipped.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Topic", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TRIVIA_CATEGORIES.forEach { (id, name) -> FilterChip(selected = cat == id, onClick = { cat = id }, label = { Text(name) }) }
                    }
                    Text("Difficulty", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to "Any", "easy" to "Easy", "medium" to "Medium", "hard" to "Hard").forEach { (v, l) ->
                            FilterChip(selected = diff == v, onClick = { diff = v }, label = { Text(l) })
                        }
                    }
                    Text("How many", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 50).forEach { c -> FilterChip(selected = n == c, onClick = { n = c }, label = { Text("$c") }) }
                    }
                }
            },
            confirmButton = { TextButton(enabled = !importing, onClick = { vm.importTrivia(cat, diff, n); showImport = false }) { Text("Import") } },
            dismissButton = { TextButton({ showImport = false }) { Text("Cancel") } }
        )
    }
    edit?.let { q ->
        var prompt by remember(q) { mutableStateOf(q.prompt) }
        val opts = remember(q) { mutableStateListOf(q.o1, q.o2, q.o3, q.o4) }
        var correct by remember(q) { mutableIntStateOf(q.correct) }
        var expl by remember(q) { mutableStateOf(q.explanation) }
        val ok = prompt.isNotBlank() && opts[0].isNotBlank() && opts[1].isNotBlank() && opts[correct].isNotBlank()
        AlertDialog(
            onDismissRequest = { edit = null }, title = { Text(if (q.id == 0L) "New question" else "Edit question") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(prompt, { prompt = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    opts.forEachIndexed { i, t ->
                        OutlinedTextField(t, { opts[i] = it }, label = { Text("Option ${'A' + i}" + if (i < 2) "" else " (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Text("Correct answer", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { i -> FilterChip(selected = correct == i, onClick = { correct = i }, label = { Text("${'A' + i}") }) }
                    }
                    OutlinedTextField(expl, { expl = it }, label = { Text("Explanation (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = ok, onClick = {
                    vm.saveQuestion(q.copy(prompt = prompt.trim(), o1 = opts[0].trim(), o2 = opts[1].trim(), o3 = opts[2].trim(), o4 = opts[3].trim(), correct = correct, explanation = expl.trim()))
                    edit = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (q.id != 0L) TextButton({ vm.deleteQuestion(q); edit = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton({ edit = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun QuizPlay(vm: StudyVm, qs: List<Question>, secs: Int, onDone: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var left by remember { mutableIntStateOf(secs) }
    val answers = remember { mutableStateListOf<Int>() }
    BackHandler(onBack = onDone)

    if (i >= qs.size) {
        val score = qs.indices.count { answers.getOrNull(it) == qs[it].correct }
        LaunchedEffect(Unit) { vm.addAttempt(QuizAttempt(takenAt = System.currentTimeMillis(), score = score, total = qs.size)) }
        val missed = qs.indices.filter { answers.getOrNull(it) != qs[it].correct }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card2 {
                    Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$score / ${qs.size}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${(score * 100f / qs.size).roundToInt()}% correct", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (missed.isNotEmpty()) item { Text("Review your misses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(missed, key = { it }) { idx ->
                val q = qs[idx]
                val a = answers.getOrNull(idx) ?: -1
                Card2 {
                    Text(q.prompt, fontWeight = FontWeight.Medium)
                    Text(if (a >= 0) "Your answer: ${q.options()[a]}" else "No answer (time ran out)", color = MaterialTheme.colorScheme.error)
                    Text("Correct: ${q.options()[q.correct]}", color = MaterialTheme.colorScheme.tertiary)
                    if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Button(onDone, Modifier.fillMaxWidth().height(52.dp)) { Text("Done") } }
        }
        return
    }

    LaunchedEffect(i, picked) {
        if (secs > 0 && picked == null) {
            left = secs
            while (left > 0) { delay(1000); left-- }
            picked = -1
        }
    }
    val q = qs[i]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onDone) { Icon(Icons.Filled.Close, "Quit quiz") }
            Text("Question ${i + 1} of ${qs.size}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (secs > 0 && picked == null) Text("$left s", color = if (left <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { i / qs.size.toFloat() }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Card2 { Text(q.prompt, style = MaterialTheme.typography.titleLarge) }
        q.options().forEachIndexed { k, text ->
            if (text.isNotBlank()) {
                val p = picked
                val bg = when {
                    p == null -> MaterialTheme.colorScheme.surface
                    k == q.correct -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                    k == p -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val mark = when { p == null -> ""; k == q.correct -> "  ✓ correct"; k == p -> "  ✗ your answer"; else -> "" }
                Surface(onClick = { picked = k }, enabled = p == null, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    color = bg, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Text("${'A' + k}. $text$mark", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        picked?.let { p ->
            Text(when { p == q.correct -> "Correct!"; p < 0 -> "Time's up. The answer is ${q.options()[q.correct]}."; else -> "Not quite. The answer is ${q.options()[q.correct]}." },
                fontWeight = FontWeight.SemiBold, color = if (p == q.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
            if (q.explanation.isNotBlank()) Text(q.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button({ answers.add(p); picked = null; i++ }, Modifier.fillMaxWidth().height(52.dp)) { Text(if (i == qs.size - 1) "See results" else "Next") }
        }
    }
}
