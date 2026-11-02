package com.cartoniq.studyos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Set your support address here. If empty, the mail app opens without a recipient. */
const val SUPPORT_EMAIL = "cartoniq.business@gmail.com"
private const val PKG = "com.cartoniq.studyos"
private const val PLAY = "https://play.google.com/store/apps/details?id=$PKG"

private val DOCS = linkedMapOf(
    "Privacy Policy" to """CartonIQ StudyOS keeps your subjects, tasks, notes and focus sessions only on your device. There are no accounts, and we run no servers that receive your study data.

The app shows banner ads through Google AdMob. AdMob may collect device and advertising identifiers and usage data under Google's own privacy policy. Your notes, tasks and study history are never sent to AdMob.

When you tap Import in the quiz section, the app contacts opentdb.com (Open Trivia DB) to download questions. Only the topic, difficulty and number you chose are sent, along with your IP address as with any web request. Nothing else leaves your device.

Backups are created only when you tap Export, and are saved where you choose. Uninstalling the app or using Delete My Data removes your data from this device. For questions, use Contact Support.

Visit our website for more information: https://cartoniq.runs-at.dev""",
    "Terms & Conditions" to """By using StudyOS you agree to these terms. The app is provided "as is", without warranties. It is an independent study tool, not an official school or exam authority.

You are responsible for your data. Export backups regularly, because uninstalling the app or clearing its data deletes everything. Ads are supplied by Google AdMob and are not endorsed by CartonIQ.

Do not copy, resell or reverse engineer the app. We may update the app and these terms; continuing to use the app means you accept the changes.""",
    "Licenses & Copyright" to """© 2026 CartonIQ (Mohd Arsh). All rights reserved. The StudyOS name, design and source code belong to CartonIQ.

Open-source components:
• Kotlin and Android Jetpack (Compose, Room, DataStore, Navigation, Lifecycle): Apache License 2.0
• Material Design components: Apache License 2.0
• Google Mobile Ads SDK: Google's terms apply
• Imported quiz questions: Open Trivia DB (opentdb.com), CC BY-SA 4.0

Full license texts are available on each project's website.""",
    "Data collection information" to """Stored on your device only: subjects, tasks, notes, focus sessions, settings, and an optional PIN (saved as a hash, not as plain text).

Sent off your device: none of your study data. The Google Mobile Ads SDK contacts Google to load ads and may collect device and advertising identifiers. Only when you tap Import, the app contacts opentdb.com to download quiz questions.

StudyOS does not use your location, contacts, camera or microphone. Use Delete My Data to erase everything stored by the app.""",
    "Acknowledgements" to """Imported quiz questions come from Open Trivia DB (opentdb.com), licensed under CC BY-SA 4.0. Thank you to the Android and Kotlin open-source communities, the Jetpack and Material Design teams, Google AdMob, and everyone who tested StudyOS and shared feedback. Built with care by CartonIQ."""
)

private fun Context.go(i: Intent) {
    try { startActivity(i) } catch (e: ActivityNotFoundException) { Toast.makeText(this, "No app found to do that", Toast.LENGTH_SHORT).show() }
}
private fun Context.openStore() {
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PKG"))) }
    catch (e: ActivityNotFoundException) { go(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY))) }
}
private fun Context.mail(subject: String, body: String) {
    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body)
    if (SUPPORT_EMAIL.isNotBlank()) i.putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
    go(i)
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, sub: String? = null, tint: Boolean = false, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(icon, null, tint = if (tint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.Medium, color = if (tint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AboutScreen(vm: StudyVm, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    var doc by remember { mutableStateOf<String?>(null) }
    var del by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Card2 {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CartonIQ StudyOS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Version $ver", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Plan, revise and focus, all offline. Made by Mohd Arsh at CartonIQ, an independent Android app brand. StudyOS is a study tool and not an official school or exam authority.",
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card2 {
            ActionRow(Icons.Filled.Lock, "Privacy Policy") { doc = "Privacy Policy" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Create, "Terms & Conditions") { doc = "Terms & Conditions" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.CheckCircle, "Licenses & Copyright") { doc = "Licenses & Copyright" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Info, "Data collection information") { doc = "Data collection information" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Favorite, "Acknowledgements") { doc = "Acknowledgements" }
        }
        Card2 {
            ActionRow(Icons.Filled.Email, "Contact Support") { ctx.mail("StudyOS support", "") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Warning, "Report a Bug", "Adds your Android version and device model") {
                ctx.mail("StudyOS bug report", "What went wrong?\n\n\n---\nApp: StudyOS $ver\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Star, "Rate this app") { ctx.openStore() }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Share, "Share this app") {
                ctx.go(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Try CartonIQ StudyOS, an offline study planner: $PLAY"), null))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ActionRow(Icons.Filled.Refresh, "Check for Updates", "Opens the Play Store page") { ctx.openStore() }
        }
        Card2 { ActionRow(Icons.Filled.Delete, "Delete My Data", "Erase everything stored by this app", tint = true) { del = true } }
        Spacer(Modifier.height(16.dp))
    }

    doc?.let { t ->
        AlertDialog(
            onDismissRequest = { doc = null }, title = { Text(t) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(DOCS.getValue(t), style = MaterialTheme.typography.bodyMedium) } },
            confirmButton = { TextButton({ doc = null }) { Text("Close") } }
        )
    }
    if (del) AlertDialog(
        onDismissRequest = { del = false }, title = { Text("Delete all your data?") },
        text = { Text("This permanently erases all subjects, tasks, notes, focus sessions and settings (including your PIN) from this device. Export a backup first if you want to keep anything.") },
        confirmButton = { TextButton({ del = false; vm.deleteAll() }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ del = false }) { Text("Cancel") } }
    )
}

@Composable
fun LockScreen(stored: String, vm: StudyVm) {
    var p by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    fun unlock() { if (vm.pinOk(p, stored)) vm.locked.value = false else { bad = true; p = "" } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("StudyOS is locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(p, { if (it.all(Char::isDigit) && it.length <= 8) { p = it; bad = false } }, label = { Text("PIN") }, singleLine = true,
            isError = bad, supportingText = { if (bad) Text("Wrong PIN. Try again.") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { unlock() }))
        Spacer(Modifier.height(12.dp))
        Button({ unlock() }, Modifier.fillMaxWidth().height(52.dp)) { Text("Unlock") }
    }
}
