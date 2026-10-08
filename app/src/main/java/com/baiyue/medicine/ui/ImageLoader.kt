package com.baiyue.medicine.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.baiyue.medicine.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MedicationImage(uri: String, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        if (uri.isBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { ins ->
                    BitmapFactory.decodeStream(ins)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEAF4EE))
            .clickable(enabled = bitmap != null) { expanded = true },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            )
        } else {
            Text(stringResource(R.string.image_placeholder), color = Color(0xFF9DBBA8), textAlign = TextAlign.Center)
        }
    }

    if (expanded && bitmap != null) {
        Dialog(onDismissRequest = { expanded = false }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { expanded = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.image_tap_anywhere_to_close), color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}