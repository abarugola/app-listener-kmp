package org.example.app_listener_kmp.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

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

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                appIcon?.let { drawable ->
                    // toBitmap() convierte el Drawable al formato que Compose entiende
                    // Necesitas el import: androidx.core.graphics.drawable.toBitmap
                    val bitmap = drawable.toBitmap()
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Ícono de $appName",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("🔒 App bloqueada", color = Color.White, fontSize = 24.sp)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = appName,
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }) {// cierra este Activity
                    Text("Cerrar")
                }
            }
        }
    }
}