package com.cartoniq.studyos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(vm: StudyVm) {
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableIntStateOf(120) }
    var subject by remember { mutableStateOf("") }
    fun finish() {
        vm.setGoal(goal)
        if (subject.isNotBlank()) vm.addSubject(subject.trim(), 0xFFD6AD55)
        vm.put(K_ONBOARD, true)
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(28.dp),
        Arrangement.Center, Alignment.CenterHorizontally) {
        if (step == 0) {
            Icon(Icons.Filled.Home, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Welcome to StudyOS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("One calm place to plan, revise and focus. Everything stays on your phone. No account needed.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            Button({ step = 1 }, Modifier.fillMaxWidth().height(52.dp)) { Text("Get started") }
            TextButton({ finish() }) { Text("Skip") }
        } else {
            Text("Quick setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Both steps are optional.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Card2 {
                Text("Daily focus goal", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$goal min", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedButton({ goal = (goal - 15).coerceAtLeast(15) }) { Text("−15") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton({ goal = (goal + 15).coerceAtMost(720) }) { Text("+15") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card2 {
                Text("Your first subject", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("e.g. Mathematics") }, singleLine = true)
            }
            Spacer(Modifier.height(24.dp))
            Button({ finish() }, Modifier.fillMaxWidth().height(52.dp)) { Text("Finish") }
            TextButton({ finish() }) { Text("Skip") }
        }
    }
}
