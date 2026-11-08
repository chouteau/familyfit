package com.example.triduofit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.triduofit.notification.NotificationScheduler
import com.example.triduofit.theme.TriDuofitTheme

class MainActivity : ComponentActivity() {
    private val incomingLink = MutableStateFlow<String?>(null)
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); incomingLink.value = intent.data?.toString() }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationScheduler.scheduleDailyReminder(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingLink.value = intent?.data?.toString()

        // 1. Initialiser le canal de notification et programmer le rappel de 9h00
        NotificationScheduler.createNotificationChannel(this)
        NotificationScheduler.scheduleDailyReminder(this)

        // 2. Demander la permission de notification pour Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            TriDuofitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val link by incomingLink.collectAsState()
                    MainNavigation(link) { incomingLink.value = null }
                }
            }
        }
    }
}
