package com.cartoniq.studyos

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Google TEST banner unit. Replace with your real unit id for release.
private const val BANNER_ID = "ca-app-pub-9771662959477951/5229649981"

private fun online(c: Context): Boolean {
    val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** True once Google's consent flow allows requesting ads. */
object Consent { var ready by mutableStateOf(false) }

/** Fixed reserved slot: an ad failure or offline state never shifts the layout. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ok = remember { online(ctx) }
    Box(modifier.fillMaxWidth().height(50.dp), Alignment.Center) {
        if (ok && Consent.ready) AndroidView(
            factory = { c ->
                AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = BANNER_ID; loadAd(AdRequest.Builder().build()) }
            },
            onRelease = { it.destroy() }
        )
    }
}
