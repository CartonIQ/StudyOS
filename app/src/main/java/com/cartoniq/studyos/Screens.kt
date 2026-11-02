package com.cartoniq.studyos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val DAY = 86_400_000L
private val PRIORITIES = listOf("Low", "Medium", "High")
private val SUBJECT_COLORS = listOf(0xFFD6AD55, 0xFF54B58A, 0xFF6C9BEF, 0xFFE27676, 0xFFB58CE0, 0xFF4FC3C3)

fun dayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun dueLabel(d: Long, today: Long) = when {
    d < today -> "Overdue"
    d < today + DAY -> "Today"
    d < today + 2 * DAY -> "Tomorrow"
    else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(d))
}

private class Tab(val route: String, val label: String, val icon: ImageVector)
private val TABS = listOf(
    Tab("home", "Home", Icons.Filled.Home), Tab("planner", "Planner", Icons.Filled.DateRange),
    Tab("revision", "Revision", Icons.Filled.Refresh), Tab("notes", "Notes", Icons.Filled.Edit),
    Tab("focus", "Focus", Icons.Filled.PlayArrow)
)

@Composable
fun StudyApp(vm: StudyVm) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val go: (String) -> Unit = { r ->
        nav.navigate(r) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true; restoreState = true
        }
    }
    Scaffold(bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            TABS.forEach { t ->
                NavigationBarItem(
                    selected = route == t.route || (t.route == "home" && route in listOf("exams", "settings", "about")), onClick = { go(t.route) },
                    icon = { Icon(t.icon, contentDescription = null) }, label = { Text(t.label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, go) }
            composable("planner") { PlannerScreen(vm) }
            composable("revision") { RevisionScreen(vm) }
            composable("exams") { ExamsScreen(vm) { nav.popBackStack() } }
            composable("notes") { NotesScreen(vm) }
            composable("focus") { FocusScreen(vm) }
            composable("settings") { SettingsScreen(vm) { nav.navigate("about") } }
            composable("about") { AboutScreen(vm) { nav.popBackStack() } }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun HomeScreen(vm: StudyVm, go: (String) -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val goal by vm.goal.collectAsStateWithLifecycle()
    val today = remember { dayStart() }
    val mins = sessions.filter { it.startedAt >= today }.sumOf { it.minutes }
    val open = tasks.filter { !it.done }
    val cards by vm.cards.collectAsStateWithLifecycle()
    val exams by vm.exams.collectAsStateWithLifecycle()
    val dueCards = cards.count { it.dueAt <= System.currentTimeMillis() }
    val nextExam = exams.firstOrNull { it.examAt >= today }
    val slots by vm.slots.collectAsStateWithLifecycle()
    val cal = Calendar.getInstance()
    val wd = cal.get(Calendar.DAY_OF_WEEK)
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val nextSlot = slots.filter { it.weekday == wd && it.endMin > nowMin }.minByOrNull { it.startMin }
    var add by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Subject?>(null) }
    var openSubject by remember { mutableStateOf<Long?>(null) }

    openSubject?.let { id ->
        val sub = subjects.firstOrNull { it.id == id }
        if (sub != null) { SubjectScreen(vm, sub) { openSubject = null }; return }
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Your study day", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                IconButton({ go("settings") }) { Icon(Icons.Filled.Settings, "Settings") }
                }
            }
            item {
                Card2 {
                    Row { Stat("${open.size}", "open tasks", Modifier.weight(1f)); Stat("$mins", "min today", Modifier.weight(1f)); Stat("${streak(sessions)}", "day streak", Modifier.weight(1f)) }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { (mins / goal.toFloat()).coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("$mins of $goal min daily goal", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    open.firstOrNull()?.let {
                        Spacer(Modifier.height(12.dp))
                        Text("Next up: ${it.title}", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    nextExam?.let { e ->
                        Spacer(Modifier.height(4.dp))
                        Text("${e.title}: ${examCountdown(e.examAt, today)}", color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    nextSlot?.let { sl ->
                        Spacer(Modifier.height(4.dp))
                        Text("Today ${hm(sl.startMin)}: ${sl.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ go("planner") }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Task") }
                    FilledTonalButton({ go("notes") }, Modifier.weight(1f)) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(4.dp)); Text("Note") }
                    FilledTonalButton({ go("focus") }, Modifier.weight(1f)) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Focus") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ go("revision") }, Modifier.weight(1f)) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text(if (dueCards > 0) "Review $dueCards" else "Review") }
                    FilledTonalButton({ go("exams") }, Modifier.weight(1f)) { Icon(Icons.Filled.DateRange, null); Spacer(Modifier.width(4.dp)); Text("Exams") }
                }
            }
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton({ add = true }) { Text("Add subject") }
                    }
                    if (subjects.isEmpty()) EmptyState("Add your first subject to organise tasks and notes.")
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects, key = { it.id }) { s ->
                            AssistChip(onClick = { openSubject = s.id }, label = { Text(s.name) },
                                leadingIcon = { Box(Modifier.size(10.dp).background(Color(s.color), CircleShape)) })
                        }
                    }
                }
            }
        }
        AdBanner()
    }

    if (add) {
        var name by remember { mutableStateOf("") }
        var color by remember { mutableLongStateOf(SUBJECT_COLORS[0]) }
        AlertDialog(
            onDismissRequest = { add = false }, title = { Text("New subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SUBJECT_COLORS.forEach { c ->
                            Box(Modifier.size(32.dp).background(Color(c), CircleShape)
                                .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { color = c })
                        }
                    }
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { vm.addSubject(name.trim(), color); add = false }) { Text("Add") } },
            dismissButton = { TextButton({ add = false }) { Text("Cancel") } }
        )
    }
    del?.let { s ->
        AlertDialog(
            onDismissRequest = { del = null }, title = { Text("Delete ${s.name}?") },
            text = { Text("Tasks with this subject are kept but lose the label.") },
            confirmButton = { TextButton({ vm.deleteSubject(s); del = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ del = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TasksPane(vm: StudyVm) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var filter by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = remember { dayStart() }
    val buzz = rememberBuzz(vm)
    val shown = tasks.filter { filter == 2 || (filter == 0) != it.done }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("Tasks", Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Open", "Done", "All").forEachIndexed { i, l -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(l) }) }
            }
            if (shown.isEmpty()) EmptyState("Nothing here yet. Tap + to plan your first task.", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { t ->
                    val sub = subjects.firstOrNull { it.id == t.subjectId }
                    val late = t.dueAt != null && t.dueAt < today && !t.done
                    Card2 {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(t.done, { vm.setDone(t, it); if (it) buzz() })
                            Column(Modifier.weight(1f)) {
                                Text(t.title, fontWeight = FontWeight.Medium,
                                    textDecoration = if (t.done) TextDecoration.LineThrough else null)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (sub != null) {
                                        Box(Modifier.size(8.dp).background(Color(sub.color), CircleShape))
                                        Text(sub.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (t.dueAt != null) Text(dueLabel(t.dueAt, today), style = MaterialTheme.typography.bodySmall,
                                        color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (t.recur > 0) Text(if (t.recur == 1) "Daily" else "Weekly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(PRIORITIES[t.priority], style = MaterialTheme.typography.labelSmall,
                                        color = if (t.priority == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton({
                                vm.deleteTask(t)
                                scope.launch {
                                    if (host.showSnackbar("Task deleted", "Undo") == SnackbarResult.ActionPerformed) vm.addTask(t)
                                }
                            }) { Icon(Icons.Filled.Delete, "Delete task") }
                        }
                    }
                }
            }
            AdBanner()
        }
        FloatingActionButton({ show = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp, 16.dp, 16.dp, 74.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Filled.Add, "Add task")
        }
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp))
    }

    if (show) {
        var title by remember { mutableStateOf("") }
        var sub by remember { mutableStateOf<Long?>(null) }
        var pr by remember { mutableIntStateOf(1) }
        var due by remember { mutableIntStateOf(0) }
        var rec by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { show = false }, title = { Text("New task") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("What needs doing?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (subjects.isNotEmpty()) {
                        Text("Subject", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            subjects.forEach { s -> FilterChip(selected = sub == s.id, onClick = { sub = if (sub == s.id) null else s.id }, label = { Text(s.name) }) }
                        }
                    }
                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRIORITIES.forEachIndexed { i, l -> FilterChip(selected = pr == i, onClick = { pr = i }, label = { Text(l) }) }
                    }
                    Text("Due", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("No date", "Today", "Tomorrow", "In a week").forEachIndexed { i, l ->
                            FilterChip(selected = due == i, onClick = { due = i }, label = { Text(l) })
                        }
                    }
                    Text("Repeat", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Never", "Daily", "Weekly").forEachIndexed { i, l -> FilterChip(selected = rec == i, onClick = { rec = i }, label = { Text(l) }) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    val offset = when (due) { 1 -> 0L; 2 -> 1L; 3 -> 7L; else -> if (rec > 0) 0L else null }
                    vm.addTask(Task(title = title.trim(), subjectId = sub, dueAt = offset?.let { dayStart() + it * DAY }, priority = pr, recur = rec))
                    show = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton({ show = false }) { Text("Cancel") } }
        )
    }
}

fun tagsOf(n: Note): List<String> = n.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

@Composable
fun NotesScreen(vm: StudyVm) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Note?>(null) }
    var q by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf<String?>(null) }
    var folder by remember { mutableStateOf<String?>(null) }
    editing?.let { NoteEditor(vm, it) { editing = null }; return }
    val allTags = notes.flatMap { tagsOf(it) }.distinct().sorted()
    val allFolders = notes.map { it.folder }.filter { it.isNotBlank() }.distinct().sorted()
    val shown = notes.filter { n ->
        (q.isBlank() || n.title.contains(q, true) || n.body.contains(q, true)) && (tag == null || tag in tagsOf(n)) && (folder == null || n.folder == folder)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.large)
            if (allFolders.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = folder == null, onClick = { folder = null }, label = { Text("All folders") })
                allFolders.forEach { fo -> FilterChip(selected = folder == fo, onClick = { folder = if (folder == fo) null else fo }, label = { Text(fo) }) }
            }
            if (allTags.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("All") })
                allTags.forEach { t -> FilterChip(selected = tag == t, onClick = { tag = if (tag == t) null else t }, label = { Text("#$t") }) }
            }
            if (shown.isEmpty()) EmptyState("No notes yet. Tap + and start writing; it saves as you type.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(shown, key = { it.id }) { n ->
                    Card2(Modifier.clip(RoundedCornerShape(18.dp)).clickable { editing = n }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(n.title.ifBlank { "Untitled" }, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (n.pinned) Icon(Icons.Filled.Star, "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        if (n.body.isNotBlank()) Text(n.body, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (n.folder.isNotBlank()) Text("Folder: ${n.folder}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (n.tags.isNotBlank()) Text(tagsOf(n).joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        FloatingActionButton({ editing = Note(title = "", body = "") }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Filled.Add, "New note")
        }
    }
}

@Composable
private fun NoteEditor(vm: StudyVm, note: Note, onClose: () -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    var pinned by remember { mutableStateOf(note.pinned) }
    var tags by remember { mutableStateOf(note.tags) }
    var folder by remember { mutableStateOf(note.folder) }
    var id by remember { mutableLongStateOf(note.id) }
    var saved by remember { mutableStateOf(listOf<Any>(note.title, note.body, note.pinned, note.tags, note.folder)) }
    var askDel by remember { mutableStateOf(false) }
    val snap = listOf<Any>(title, body, pinned, tags, folder)
    val blank = title.isBlank() && body.isBlank()
    fun cur() = Note(id, title, body, pinned, tags = tags, folder = folder)

    LaunchedEffect(snap) {
        if (snap == saved || blank) return@LaunchedEffect
        delay(600)
        id = vm.saveNote(cur())
        saved = snap
    }
    fun close() {
        if (!blank && snap != saved) vm.saveNoteAsync(cur())
        onClose()
    }
    BackHandler { close() }

    val clear = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
    )
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ close() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(if (snap == saved) "Saved" else "Saving…", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton({ pinned = !pinned }) {
                Icon(Icons.Filled.Star, if (pinned) "Unpin note" else "Pin note",
                    tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (id != 0L) IconButton({ askDel = true }) { Icon(Icons.Filled.Delete, "Delete note") }
        }
        TextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = { Text("Title") }, singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), colors = clear)
        TextField(folder, { folder = it }, Modifier.fillMaxWidth(), placeholder = { Text("Folder (optional)") }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium, colors = clear)
        TextField(tags, { tags = it }, Modifier.fillMaxWidth(), placeholder = { Text("Tags, separated by commas") }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary), colors = clear)
        TextField(body, { body = it }, Modifier.fillMaxSize(), placeholder = { Text("Start writing…") }, colors = clear)
    }
    if (askDel) AlertDialog(
        onDismissRequest = { askDel = false }, title = { Text("Delete this note?") },
        text = { Text("This can't be undone.") },
        confirmButton = { TextButton({ vm.deleteNote(cur()); askDel = false; onClose() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ askDel = false }) { Text("Cancel") } }
    )
}

@Composable
fun FocusScreen(vm: StudyVm) {
    val end by vm.focusEnd.collectAsStateWithLifecycle()
    val rem by vm.focusRem.collectAsStateWithLifecycle()
    val planned by vm.focusMin.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val awake by vm.awake.collectAsStateWithLifecycle()
    val buzz = rememberBuzz(vm)
    val view = LocalView.current
    var pick by remember { mutableIntStateOf(25) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val running = end > 0
    val paused = !running && rem > 0
    val active = running || paused
    LaunchedEffect(running) { while (running) { now = System.currentTimeMillis(); delay(500) } }
    LaunchedEffect(now) { if (running && now >= end) { buzz(); vm.stopFocus(planned) } }
    DisposableEffect(running, awake) { view.keepScreenOn = running && awake; onDispose { view.keepScreenOn = false } }
    val total = (if (active) planned else pick) * 60000L
    val remain = when { running -> (end - now).coerceIn(0, total); paused -> rem.coerceIn(0, total); else -> total }
    val sec = (remain + 999) / 1000

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Focus", Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Box(Modifier.padding(top = 8.dp), Alignment.Center) {
            CircularProgressIndicator(progress = { if (active) 1f - remain / total.toFloat() else 0f },
                Modifier.size(240.dp), strokeWidth = 10.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(sec / 60, sec % 60), fontSize = 52.sp, fontWeight = FontWeight.Bold)
                if (paused) Text("Paused", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 25, 45, 60).forEach { m ->
                FilterChip(selected = (if (active) planned else pick) == m, enabled = !active, onClick = { pick = m }, label = { Text("$m min") })
            }
        }
        if (!active) Button({ buzz(); vm.startFocus(pick) }, Modifier.fillMaxWidth().height(52.dp)) { Text("Start focus", fontSize = 16.sp) }
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (running) OutlinedButton({ vm.pauseFocus() }, Modifier.weight(1f).height(52.dp)) { Text("Pause") }
            else Button({ vm.resumeFocus() }, Modifier.weight(1f).height(52.dp)) { Text("Resume") }
            OutlinedButton({ vm.stopFocus(((total - remain) / 60000L).toInt()) }, Modifier.weight(1f).height(52.dp)) { Text("End session") }
        }
        WeeklyReport(sessions)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (sessions.isEmpty()) Text("No sessions yet. Your first one is a tap away.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            sessions.take(5).forEach {
                Card2 { Row {
                    Text(SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(it.startedAt)), Modifier.weight(1f))
                    Text("${it.minutes} min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                } }
            }
        }
    }
}

@Composable
private fun rememberBuzz(vm: StudyVm): () -> Unit {
    val on by vm.haptics.collectAsStateWithLifecycle()
    val h = LocalHapticFeedback.current
    return { if (on) h.performHapticFeedback(HapticFeedbackType.LongPress) }
}

private fun streak(s: List<FocusSession>): Int {
    val off = TimeZone.getDefault().getOffset(System.currentTimeMillis())
    val days = s.map { (it.startedAt + off) / DAY }.toSet()
    var d = (System.currentTimeMillis() + off) / DAY
    if (d !in days) d--
    var n = 0
    while (d in days) { n++; d-- }
    return n
}

@Composable
private fun WeeklyReport(sessions: List<FocusSession>) {
    val now = System.currentTimeMillis()
    val off = TimeZone.getDefault().getOffset(now)
    val today = (now + off) / DAY
    val mins = (6 downTo 0).map { back -> sessions.filter { (it.startedAt + off) / DAY == today - back }.sumOf { it.minutes } }
    val top = (mins.maxOrNull() ?: 0).coerceAtLeast(30)
    val names = (6 downTo 0).map { back -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now - back * DAY)).take(1) }
    Card2(Modifier.clearAndSetSemantics {
        contentDescription = "Weekly report. Focus minutes over the last 7 days: ${mins.joinToString(", ")}. Total ${mins.sum()} minutes."
    }) {
        Row {
            Text("This week", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("${mins.sum()} min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().height(100.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mins.forEachIndexed { i, m ->
                Column(Modifier.weight(1f).fillMaxHeight(), Arrangement.Bottom, Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((60f * m / top).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(6.dp))
                        .background(if (m > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    Text(names[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PlannerScreen(vm: StudyVm) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tasks", "Timetable").forEachIndexed { i, l -> FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(l) }) }
        }
        Box(Modifier.weight(1f)) { if (tab == 0) TasksPane(vm) else TimetableScreen(vm) }
    }
}
