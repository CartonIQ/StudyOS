package com.cartoniq.studyos

import android.app.TimePickerDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar

private val DAY_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")   // index = Calendar weekday - 1
private val DAY_ORDER = listOf(2, 3, 4, 5, 6, 7, 1)

fun hm(min: Int) = "%02d:%02d".format(min / 60, min % 60)

@Composable
fun TimetableScreen(vm: StudyVm) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var day by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    var edit by remember { mutableStateOf<Slot?>(null) }
    val list = slots.filter { it.weekday == day }.sortedBy { it.startMin }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAY_ORDER.forEach { d -> FilterChip(selected = day == d, onClick = { day = d }, label = { Text(DAY_NAMES[d - 1]) }) }
            }
            if (list.isEmpty()) EmptyState("No study sessions on ${DAY_NAMES[day - 1]}. Tap + to add a weekly one.", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { s ->
                    val clash = list.any { it.id != s.id && it.startMin < s.endMin && s.startMin < it.endMin }
                    val sub = subjects.firstOrNull { it.id == s.subjectId }
                    Card2(Modifier.clip(RoundedCornerShape(18.dp)).clickable { edit = s }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${hm(s.startMin)} – ${hm(s.endMin)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(s.title, fontWeight = FontWeight.Medium)
                                if (sub != null) Text(sub.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (clash) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text("Overlap", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            AdBanner()
        }
        FloatingActionButton({ edit = Slot(weekday = day, startMin = 540, endMin = 600, title = "") }, Modifier.align(Alignment.BottomEnd).padding(16.dp, 16.dp, 16.dp, 74.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "Add weekly session") }
    }

    edit?.let { s ->
        val ctx = LocalContext.current
        var title by remember(s) { mutableStateOf(s.title) }
        var wd by remember(s) { mutableIntStateOf(s.weekday) }
        var st by remember(s) { mutableIntStateOf(s.startMin) }
        var en by remember(s) { mutableIntStateOf(s.endMin) }
        var sub by remember(s) { mutableStateOf(s.subjectId) }
        val clash = slots.firstOrNull { it.id != s.id && it.weekday == wd && it.startMin < en && st < it.endMin }
        AlertDialog(
            onDismissRequest = { edit = null }, title = { Text(if (s.id == 0L) "Weekly session" else "Edit session") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("What will you study?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Repeats every", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DAY_ORDER.forEach { d -> FilterChip(selected = wd == d, onClick = { wd = d }, label = { Text(DAY_NAMES[d - 1]) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({
                            TimePickerDialog(ctx, { _, h, m -> st = h * 60 + m; if (en <= st) en = (st + 60).coerceAtMost(1439) }, st / 60, st % 60, true).show()
                        }, Modifier.weight(1f)) { Text("From ${hm(st)}") }
                        OutlinedButton({ TimePickerDialog(ctx, { _, h, m -> en = h * 60 + m }, en / 60, en % 60, true).show() }, Modifier.weight(1f)) { Text("To ${hm(en)}") }
                    }
                    if (subjects.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { x -> FilterChip(selected = sub == x.id, onClick = { sub = if (sub == x.id) null else x.id }, label = { Text(x.name) }) }
                    }
                    if (en <= st) Text("End time must be after the start time.", color = MaterialTheme.colorScheme.error)
                    if (clash != null) Text("Overlaps with ${clash.title} (${hm(clash.startMin)}–${hm(clash.endMin)}). You can still save it.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && en > st, onClick = {
                    vm.saveSlot(s.copy(title = title.trim(), weekday = wd, startMin = st, endMin = en, subjectId = sub)); day = wd; edit = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (s.id != 0L) TextButton({ vm.deleteSlot(s); edit = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton({ edit = null }) { Text("Cancel") }
                }
            }
        )
    }
}
