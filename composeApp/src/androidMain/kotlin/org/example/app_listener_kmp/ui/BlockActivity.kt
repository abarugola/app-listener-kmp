package org.example.app_listener_kmp.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.example.app_listener_kmp.ui.features.block.BlockContent

class BlockActivity: ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra("BLOCKED_PACKAGE") ?: "App desconocida"

        var appName: String
        var appIcon: Drawable?

        try {
            val packageInfo = packageManager.getApplicationInfo(blockedPackage, 0)
            appName = packageManager.getApplicationLabel(packageInfo).toString()
            appIcon = packageManager.getApplicationIcon(blockedPackage)
        } catch (e: Exception) {
            appName = blockedPackage
            appIcon = null
        }

        val imageBitmap = appIcon?.toBitmap()?.asImageBitmap()

        setContent {
            BlockContent(
                appName = appName,
                appIcon = imageBitmap,
                onClose = {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}