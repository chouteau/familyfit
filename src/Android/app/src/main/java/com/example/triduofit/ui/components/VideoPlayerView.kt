package com.example.triduofit.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayerView(
    videoResId: Int,
    modifier: Modifier = Modifier
) {
    key(videoResId) {
        val context = LocalContext.current
        var isPlaying by remember { mutableStateOf(true) }
        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

        val videoUri = remember(videoResId) {
            Uri.parse("android.resource://${context.packageName}/$videoResId")
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    videoViewRef?.let { vv ->
                        if (vv.isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            vv.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f) // Vidéo muette en boucle d'accompagnement
                            start()
                            isPlaying = true
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        DisposableEffect(videoResId) {
            onDispose {
                videoViewRef?.stopPlayback()
                videoViewRef = null
            }
        }
    }
}
