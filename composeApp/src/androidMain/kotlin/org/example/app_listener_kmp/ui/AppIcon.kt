package org.example.app_listener_kmp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.example.app_listener_kmp.domain.PlatformIcon

@Composable
actual fun AppIcon (icon: PlatformIcon) {
//    icon?.let {
//        Image(
//            painter = rememberDrawablePainter(it.drawable),
//            contentDescription = null,
//            modifier = Modifier.size(40.dp)
//        )
//    }

    icon?.let {
        Image(
            painter = rememberDrawablePainter(it.drawable),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}