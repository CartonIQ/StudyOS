package com.cartoniq.studyos

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private const val DAY = 86_400_000L

fun examCountdown(at: Long, today: Long): String {
    val d = ((at - today + DAY / 2) / DAY).toInt()
    return when { at < today -> "Passed"; d <= 0 -> "Today"; d == 1 -> "Tomorrow"; else -> "In $d days" }
}

@Composable
fun ExamsScreen(vm: StudyVm, onBack: () -> Unit) {
    val exams by vm.exams.collectAsStateWithLifecycle()
    val mocks by vm.mocks.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val today = remember { dayStart() }
    val fmt = remember { SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()) }
    var add by remember { mutableStateOf(false) }
    var mockFor by remember { mutableStateOf<Exam?>(null) }
    var delExam by remember { mutableStateOf<Exam?>(null) }
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Exams", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (exams.isEmpty()) EmptyState("No exams yet. Tap + to add one and watch the countdown.", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(exams, key = { it.id }) { e ->
                    val ms = mocks.filter { it.examId == e.id }
                    Card2 {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(fmt.format(Date(e.examAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(examCountdown(e.examAt, today), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        val sub = subjects.firstOrNull { it.id == e.subjectId }
                        if (sub != null) {
                            val ids = chapters.filter { it.subjectId == sub.id }.map { it.id }.toSet()
                            val ts = topics.filter { it.chapterId in ids }
                            val done = ts.count { it.status == 2 }
                            Spacer(Modifier.height(8.dp))
                            Text("${sub.name} syllabus: " + if (ts.isEmpty()) "no topics yet" else "$done of ${ts.size} topics done (${(done * 100f / ts.size).roundToInt()}%)",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            if (ts.isNotEmpty()) LinearProgressIndicator(progress = { done / ts.size.toFloat() },
                                Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                        if (ms.isNotEmpty()) {
                            val avg = ms.sumOf { it.score * 100.0 / it.outOf } / ms.size
                            Spacer(Modifier.height(8.dp))
                            Text("Mock tests · average ${avg.roundToInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            ms.take(5).forEach { m ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(m.takenAt)) + "   ${m.score}/${m.outOf}", Modifier.weight(1f))
                                    IconButton({ vm.deleteMock(m) }) { Icon(Icons.Filled.Delete, "Delete mock result") }
                                }
                            }
                        }
                        Row {
                            TextButton({ mockFor = e }) { Text("Add mock result") }
                            Spacer(Modifier.weight(1f))
                            TextButton({ delExam = e }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
        FloatingActionButton({ add = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "Add exam") }
    }

    if (add) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableLongStateOf(0L) }
        var subSel by remember { mutableStateOf<Long?>(null) }
        AlertDialog(
            onDismissRequest = { add = false }, title = { Text("New exam") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Exam name") }, singleLine = true)
                    OutlinedButton({
                        val c = Calendar.getInstance()
                        DatePickerDialog(ctx, { _, y, m, d ->
                            date = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        }, c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]).show()
                    }) { Text(if (date == 0L) "Pick exam date" else fmt.format(Date(date))) }
                    if (subjects.isNotEmpty()) {
                        Text("Syllabus subject (optional)", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            subjects.forEach { x -> FilterChip(selected = subSel == x.id, onClick = { subSel = if (subSel == x.id) null else x.id }, label = { Text(x.name) }) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(enabled = title.isNotBlank() && date > 0, onClick = { vm.saveExam(Exam(title = title.trim(), examAt = date, subjectId = subSel)); add = false }) { Text("Add") } },
            dismissButton = { TextButton({ add = false }) { Text("Cancel") } }
        )
    }
    mockFor?.let { e ->
        var score by remember { mutableStateOf("") }
        var outOf by remember { mutableStateOf("100") }
        val s = score.toIntOrNull(); val o = outOf.toIntOrNull()
        val kb = KeyboardOptions(keyboardType = KeyboardType.Number)
        AlertDialog(
            onDismissRequest = { mockFor = null }, title = { Text("Mock test result") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(score, { if (it.length <= 4 && it.all(Char::isDigit)) score = it }, label = { Text("Score") }, singleLine = true, keyboardOptions = kb, modifier = Modifier.weight(1f))
                    OutlinedTextField(outOf, { if (it.length <= 4 && it.all(Char::isDigit)) outOf = it }, label = { Text("Out of") }, singleLine = true, keyboardOptions = kb, modifier = Modifier.weight(1f))
                }
            },
            confirmButton = {
                TextButton(enabled = s != null && o != null && o > 0 && s <= o, onClick = {
                    vm.addMock(MockTest(examId = e.id, takenAt = System.currentTimeMillis(), score = s!!, outOf = o!!)); mockFor = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ mockFor = null }) { Text("Cancel") } }
        )
    }
    delExam?.let { e ->
        AlertDialog(
            onDismissRequest = { delExam = null }, title = { Text("Delete ${e.title}?") },
            text = { Text("Its mock test results will also be deleted.") },
            confirmButton = { TextButton({ vm.deleteExam(e); delExam = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ delExam = null }) { Text("Cancel") } }
        )
    }
}
