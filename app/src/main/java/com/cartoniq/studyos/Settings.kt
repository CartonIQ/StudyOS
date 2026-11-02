package com.cartoniq.studyos

import android.Manifest
import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), Modifier.padding(start = 4.dp, top = 8.dp), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Card2(content = content)
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(
    Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Column(Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked, null)
}

@Composable
fun SettingsScreen(vm: StudyVm, onAbout: () -> Unit) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val goal by vm.goal.collectAsStateWithLifecycle()
    val amoled by vm.amoled.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val fscale by vm.fscale.collectAsStateWithLifecycle()
    val awake by vm.awake.collectAsStateWithLifecycle()
    val haptics by vm.haptics.collectAsStateWithLifecycle()
    val secure by vm.secure.collectAsStateWithLifecycle()
    val pin by vm.pin.collectAsStateWithLifecycle()
    val lockMin by vm.lockMin.collectAsStateWithLifecycle()
    val last by vm.lastBackup.collectAsStateWithLifecycle()
    val msg by vm.toast.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    var pinDlg by remember { mutableStateOf(false) }
    var restoreAsk by remember { mutableStateOf(false) }
    val hasPin = pin?.isNotEmpty() == true
    val ctx = LocalContext.current
    val remOn by vm.remOn.collectAsStateWithLifecycle()
    val remH by vm.remH.collectAsStateWithLifecycle()
    val remM by vm.remM.collectAsStateWithLifecycle()
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.setReminder(true) else vm.toast.value = "Notifications are blocked. Allow them in system settings to get reminders."
    }
    fun enableReminder() {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.setReminder(true)
    }
    LaunchedEffect(msg) { msg?.let { host.showSnackbar(it); vm.toast.value = null } }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> u?.let { vm.exportTo(it) } }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let { vm.importFrom(it) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Section("Appearance") {
                Text("Theme", fontWeight = FontWeight.Medium)
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEachIndexed { i, l -> FilterChip(selected = theme == i, onClick = { vm.setTheme(i) }, label = { Text(l) }) }
                }
                SwitchRow("AMOLED black", "Pure black background in dark theme", amoled) { vm.put(K_AMOLED, it) }
                Text("Accent colour", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ACCENTS.forEachIndexed { i, c ->
                        Box(Modifier.size(44.dp).background(c, CircleShape)
                            .border(if (i == accent) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .selectable(selected = i == accent, role = Role.RadioButton, onClick = { vm.put(K_ACCENT, i) })
                            .semantics { contentDescription = "Accent ${ACCENT_NAMES[i]}" })
                    }
                }
                var s by remember(fscale) { mutableFloatStateOf(fscale / 100f) }
                Text("Text size ${(s * 100).roundToInt()}%", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
                Slider(s, { s = it }, Modifier.semantics { contentDescription = "Text size" }, valueRange = 0.85f..1.3f,
                    onValueChangeFinished = { vm.put(K_FSCALE, (s * 100).roundToInt()) })
            }

            Section("Study") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily focus goal", fontWeight = FontWeight.Medium)
                        Text("$goal min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton({ vm.setGoal(goal - 15) }) { Text("−15") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton({ vm.setGoal(goal + 15) }) { Text("+15") }
                }
                SwitchRow("Keep screen on", "Stay awake while a focus session runs", awake) { vm.put(K_AWAKE, it) }
                SwitchRow("Haptic feedback", "Vibrate on task done and focus start/end", haptics) { vm.put(K_HAPTIC, it) }
            }

            Section("Reminders") {
                SwitchRow("Daily study reminder", "A nudge with your due tasks and cards", remOn) { if (it) enableReminder() else vm.setReminder(false) }
                if (remOn) Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    OutlinedButton({ TimePickerDialog(ctx, { _, h, m -> vm.setReminderTime(h, m) }, remH, remM, true).show() }) { Text("%02d:%02d".format(remH, remM)) }
                }
            }

            Section("Privacy & security") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("App PIN", fontWeight = FontWeight.Medium)
                        Text(if (hasPin) "On. Asked when you return to the app." else "Off", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (hasPin) TextButton({ vm.setPin("") }) { Text("Remove") }
                    OutlinedButton({ pinDlg = true }) { Text(if (hasPin) "Change" else "Set PIN") }
                }
                if (hasPin) {
                    Text("Auto-lock", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Instantly", 1 to "1 min", 5 to "5 min").forEach { (m, l) ->
                            FilterChip(selected = lockMin == m, onClick = { vm.put(K_LOCKMIN, m) }, label = { Text(l) })
                        }
                    }
                }
                SwitchRow("Privacy screen", "Hide the app in recent apps and block screenshots", secure) { vm.put(K_SECURE, it) }
                val ci = remember { UserMessagingPlatform.getConsentInformation(ctx) }
                if (ci.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
                    OutlinedButton({ (ctx as? Activity)?.let { UserMessagingPlatform.showPrivacyOptionsForm(it) { } } }, Modifier.fillMaxWidth()) { Text("Ad privacy options") }
            }

            Section("Data") {
                Text(if (last > 0) "Last backup: " + SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(last)) else "No backup yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ export.launch("studyos-backup-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + ".json") }, Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton({ restoreAsk = true }, Modifier.weight(1f)) { Text("Restore") }
                }
            }

            Section("About") {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onAbout), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("About, legal & support", fontWeight = FontWeight.Medium)
                        Text("Privacy, terms, licenses, contact, delete my data", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
    }

    if (pinDlg) {
        var a by remember { mutableStateOf("") }
        var b by remember { mutableStateOf("") }
        val kb = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        AlertDialog(
            onDismissRequest = { pinDlg = false }, title = { Text(if (hasPin) "Change PIN" else "Set PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use 4 to 8 digits. If you forget it, you must reinstall the app, which erases your data. Export a backup first.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(a, { if (it.all(Char::isDigit) && it.length <= 8) a = it }, label = { Text("PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = kb)
                    OutlinedTextField(b, { if (it.all(Char::isDigit) && it.length <= 8) b = it }, label = { Text("Repeat PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = kb, isError = b.isNotEmpty() && a != b)
                }
            },
            confirmButton = { TextButton(enabled = a.length in 4..8 && a == b, onClick = { vm.setPin(a); pinDlg = false }) { Text("Save") } },
            dismissButton = { TextButton({ pinDlg = false }) { Text("Cancel") } }
        )
    }
    if (restoreAsk) AlertDialog(
        onDismissRequest = { restoreAsk = false }, title = { Text("Restore backup?") },
        text = { Text("This replaces all subjects, tasks, notes and focus sessions on this device with the backup file. If the file is invalid, nothing is changed.") },
        confirmButton = { TextButton({ restoreAsk = false; import.launch(arrayOf("*/*")) }) { Text("Choose file") } },
        dismissButton = { TextButton({ restoreAsk = false }) { Text("Cancel") } }
    )
}
