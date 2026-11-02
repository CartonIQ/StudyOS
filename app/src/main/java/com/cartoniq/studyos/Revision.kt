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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private const val DAY = 86_400_000L

/** Spaced repetition. grade: 0 Again, 1 Hard, 2 Good, 3 Easy. */
fun nextCard(c: Flashcard, g: Int, now: Long): Flashcard {
    var ease = c.ease
    val days: Int
    when (g) {
        0 -> { ease = (ease - 20).coerceAtLeast(130); days = 0 }
        1 -> { ease = (ease - 15).coerceAtLeast(130); days = maxOf(1, (c.intervalDays * 1.2).roundToInt()) }
        2 -> days = if (c.intervalDays == 0) 1 else maxOf(c.intervalDays + 1, (c.intervalDays * ease / 100.0).roundToInt())
        else -> { ease += 15; days = if (c.intervalDays == 0) 3 else maxOf(c.intervalDays + 2, (c.intervalDays * ease * 1.3 / 100.0).roundToInt()) }
    }
    val due = if (days == 0) now + 10 * 60_000L else now + days * DAY
    return c.copy(intervalDays = days, ease = ease, dueAt = due, reviews = c.reviews + 1)
}

private fun dueText(c: Flashcard, now: Long): String {
    if (c.dueAt <= now) return "Due now"
    val d = ((c.dueAt - now) / DAY).toInt()
    return if (d < 1) "Due later today" else "Due in $d day${if (d == 1) "" else "s"}"
}

@Composable
fun NameDialog(title: String, label: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var t by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(t, { t = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { onOk(t.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CardsPane(vm: StudyVm, mode: Int, onMode: (Int) -> Unit) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<Long?>(null) }
    var review by remember { mutableStateOf<Long?>(null) }
    var addDeck by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val due = cards.count { it.dueAt <= now }

    review?.let { r -> ReviewScreen(vm, if (r < 0) null else r) { review = null }; return }
    open?.let { id ->
        val deck = decks.firstOrNull { d -> d.id == id }
        if (deck != null) { DeckScreen(vm, deck, onBack = { open = null }, onReview = { review = deck.id }); return }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Column { Text("Revision", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); ModeChips(mode, onMode) } }
            item {
                Card2 {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$due", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(if (due == 1) "card due" else "cards due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(enabled = due > 0, onClick = { review = -1L }) { Text("Start review") }
                    }
                }
            }
            if (decks.isEmpty()) item { EmptyState("Create a deck, add cards, and review them on a smart schedule.") }
            items(decks, key = { it.id }) { d ->
                val n = cards.count { it.deckId == d.id }
                val dn = cards.count { it.deckId == d.id && it.dueAt <= now }
                Card2(Modifier.clip(RoundedCornerShape(18.dp)).clickable { open = d.id }) {
                    Text(d.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text("$n cards · $dn due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        FloatingActionButton({ addDeck = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "New deck") }
    }
    if (addDeck) NameDialog("New deck", "Deck name", { addDeck = false }) { vm.addDeck(it); addDeck = false }
}

@Composable
private fun DeckScreen(vm: StudyVm, deck: Deck, onBack: () -> Unit, onReview: () -> Unit) {
    val all by vm.cards.collectAsStateWithLifecycle()
    val cards = all.filter { it.deckId == deck.id }
    val now = System.currentTimeMillis()
    val due = cards.count { it.dueAt <= now }
    var edit by remember { mutableStateOf<Flashcard?>(null) }
    var delDeck by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(deck.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton({ delDeck = true }) { Icon(Icons.Filled.Delete, "Delete deck") }
            }
            Button(onReview, Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp), enabled = due > 0) {
                Text(if (due > 0) "Review $due due" else "Nothing due")
            }
            if (cards.isEmpty()) EmptyState("No cards yet. Tap + to add the first one.", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards, key = { it.id }) { c ->
                    Card2(Modifier.clip(RoundedCornerShape(18.dp)).clickable { edit = c }) {
                        Text(c.front, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(c.back, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${dueText(c, now)} · interval ${c.intervalDays}d · reviewed ${c.reviews}×", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        FloatingActionButton({ edit = Flashcard(deckId = deck.id, front = "", back = "") }, Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Filled.Add, "Add card") }
    }
    edit?.let { c ->
        var front by remember(c) { mutableStateOf(c.front) }
        var back by remember(c) { mutableStateOf(c.back) }
        AlertDialog(
            onDismissRequest = { edit = null }, title = { Text(if (c.id == 0L) "New card" else "Edit card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(front, { front = it }, label = { Text("Front (question)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(back, { back = it }, label = { Text("Back (answer)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = front.isNotBlank() && back.isNotBlank(), onClick = { vm.saveCard(c.copy(front = front.trim(), back = back.trim())); edit = null }) { Text("Save") } },
            dismissButton = {
                Row {
                    if (c.id != 0L) TextButton({ vm.deleteCard(c); edit = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton({ edit = null }) { Text("Cancel") }
                }
            }
        )
    }
    if (delDeck) AlertDialog(
        onDismissRequest = { delDeck = false }, title = { Text("Delete this deck?") },
        text = { Text("Its ${cards.size} cards will also be deleted. This can't be undone.") },
        confirmButton = { TextButton({ vm.deleteDeck(deck); delDeck = false; onBack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ delDeck = false }) { Text("Cancel") } }
    )
}

@Composable
private fun ReviewScreen(vm: StudyVm, deckId: Long?, onDone: () -> Unit) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val queue = remember {
        val t = System.currentTimeMillis()
        mutableStateListOf<Long>().apply {
            addAll(cards.filter { (deckId == null || it.deckId == deckId) && it.dueAt <= t }.sortedBy { it.dueAt }.map { it.id })
        }
    }
    val total = remember { queue.size }
    var done by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    BackHandler(onBack = onDone)
    val card = queue.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id } }
    LaunchedEffect(queue.size, card) { if (queue.isNotEmpty() && card == null) queue.removeAt(0) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onDone) { Icon(Icons.Filled.Close, "Close review") }
            Text("$done of $total reviewed", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { if (total == 0) 1f else done / total.toFloat() }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        if (card == null) {
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(12.dp))
                Text("All caught up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("You reviewed $done card${if (done == 1) "" else "s"}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(onDone, Modifier.fillMaxWidth().height(52.dp)) { Text("Done") }
            }
        } else {
            val now = System.currentTimeMillis()
            Card2(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(card.front, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    if (shown) {
                        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outline); Spacer(Modifier.height(16.dp))
                        Text(card.back, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (!shown) Button({ shown = true }, Modifier.fillMaxWidth().height(52.dp)) { Text("Show answer") }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Again", "Hard", "Good", "Easy").forEachIndexed { g, label ->
                    val d = nextCard(card, g, now).intervalDays
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalButton({
                            vm.review(card, g); queue.removeAt(0)
                            if (g == 0) queue.add(card.id) else done++
                            shown = false
                        }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 2.dp)) { Text(label, maxLines = 1) }
                        Text(if (d == 0) "10 min" else "${d}d", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
