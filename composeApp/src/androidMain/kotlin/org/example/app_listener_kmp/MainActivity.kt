package org.example.app_listener_kmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.app_listener_kmp.platform.AppProvider
import org.example.app_listener_kmp.platform.hasUsageStatsPermission
import org.example.app_listener_kmp.platform.provideContext
import org.example.app_listener_kmp.platform.requestUsageStatsPermission
import org.example.app_listener_kmp.ui.AppScreen
import org.example.app_listener_kmp.ui.MainContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MainContent()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}