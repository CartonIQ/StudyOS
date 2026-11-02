package com.cartoniq.studyos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private val STATUS_ICON = listOf("○", "◐", "●")
private val STATUS_TEXT = listOf("Not started", "In progress", "Done")

@Composable
fun SubjectScreen(vm: StudyVm, subject: Subject, onBack: () -> Unit) {
    val allCh by vm.chapters.collectAsStateWithLifecycle()
    val allTp by vm.topics.collectAsStateWithLifecycle()
    val chapters = allCh.filter { it.subjectId == subject.id }
    val ids = chapters.map { it.id }.toSet()
    val topics = allTp.filter { it.chapterId in ids }
    val done = topics.count { it.status == 2 }
    var addChapter by remember { mutableStateOf(false) }
    var addTopicFor by remember { mutableStateOf<Chapter?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(subject.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton({ confirm = "Delete ${subject.name}? Its chapters and topics are deleted too. Tasks keep existing but lose the label." to { vm.deleteSubject(subject); onBack() } }) {
                    Icon(Icons.Filled.Delete, "Delete subject")
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card2 {
                        Text("Syllabus progress", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { if (topics.isEmpty()) 0f else done / topics.size.toFloat() },
                            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(if (topics.isEmpty()) "No topics yet" else "$done of ${topics.size} topics done (${(done * 100f / topics.size).roundToInt()}%)",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap a topic to change its status.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (chapters.isEmpty()) item { EmptyState("Add a chapter with +, then add topics to track your syllabus.") }
                items(chapters, key = { it.id }) { ch ->
                    val ts = topics.filter { it.chapterId == ch.id }
                    Card2 {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ch.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            IconButton({ confirm = "Delete chapter ${ch.title} and its ${ts.size} topics?" to { vm.deleteChapter(ch) } }) { Icon(Icons.Filled.Delete, "Delete chapter") }
                        }
                        ts.forEach { t ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { vm.saveTopic(t.copy(status = (t.status + 1) % 3)) }, verticalAlignment = Alignment.CenterVertically) {
                                Text(STATUS_ICON[t.status], Modifier.width(28.dp), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                Text(t.title, Modifier.weight(1f), textDecoration = if (t.status == 2) TextDecoration.LineThrough else null)
                                Text(STATUS_TEXT[t.status], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton({ vm.deleteTopic(t) }) { Icon(Icons.Filled.Close, "Delete topic ${t.title}") }
                            }
                        }
                        TextButton({ addTopicFor = ch }) { Text("Add topic") }
                    }
                }
            }
        }
        FloatingActionButton({ addChapter = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "Add chapter") }
    }
    if (addChapter) NameDialog("New chapter", "Chapter title", { addChapter = false }) { vm.saveChapter(Chapter(subjectId = subject.id, title = it)); addChapter = false }
    addTopicFor?.let { ch -> NameDialog("New topic in ${ch.title}", "Topic title", { addTopicFor = null }) { vm.saveTopic(Topic(chapterId = ch.id, title = it, status = 0)); addTopicFor = null } }
    confirm?.let { (text, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null }, title = { Text("Are you sure?") }, text = { Text(text) },
            confirmButton = { TextButton({ action(); confirm = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } }
        )
    }
}
