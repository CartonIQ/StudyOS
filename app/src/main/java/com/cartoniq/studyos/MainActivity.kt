package com.cartoniq.studyos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : ComponentActivity() {
    private val vm: StudyVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startConsent()
        setContent {
            val mode by vm.theme.collectAsStateWithLifecycle()
            val amoled by vm.amoled.collectAsStateWithLifecycle()
            val accent by vm.accent.collectAsStateWithLifecycle()
            val fscale by vm.fscale.collectAsStateWithLifecycle()
            val secure by vm.secure.collectAsStateWithLifecycle()
            val pin by vm.pin.collectAsStateWithLifecycle()
            val locked by vm.locked.collectAsStateWithLifecycle()
            val ob by vm.onboarded.collectAsStateWithLifecycle()
            LaunchedEffect(secure) {
                if (secure) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            StudyTheme(mode, amoled, accent, fscale) {
                val p = pin
                when {
                    p == null || ob == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    p.isNotEmpty() && locked -> LockScreen(p, vm)
                    ob == false -> OnboardingScreen(vm)
                    else -> StudyApp(vm)
                }
            }
        }
    }

    /** Google UMP consent. Ads start only when consent allows; the form appears only where required. */
    private fun startConsent() {
        val ci = UserMessagingPlatform.getConsentInformation(this)
        fun ready() {
            if (ci.canRequestAds() && !Consent.ready) { Consent.ready = true; Thread { MobileAds.initialize(this) }.start() }
        }
        ci.requestConsentInfoUpdate(this, ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { ready() } },
            { ready() })
        ready()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.stoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val s = vm.stoppedAt
        if (s > 0 && System.currentTimeMillis() - s >= vm.lockMin.value * 60_000L) vm.locked.value = true
    }
}
