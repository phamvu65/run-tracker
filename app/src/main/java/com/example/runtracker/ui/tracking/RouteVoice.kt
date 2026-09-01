package com.example.runtracker.ui.tracking

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Tạo một [TextToSpeech] (tiếng Việt nếu có) gắn với vòng đời composable và trả về hàm đọc.
 * Gọi hàm trả về với chuỗi rỗng sẽ bị bỏ qua.
 */
@Composable
fun rememberRouteVoice(): (String) -> Unit {
    val context = LocalContext.current
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val vi = Locale.forLanguageTag("vi-VN")
                if (engine?.isLanguageAvailable(vi) == TextToSpeech.LANG_AVAILABLE) {
                    engine?.language = vi
                }
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    return remember(tts) {
        { text: String ->
            if (text.isNotBlank()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "route-voice")
            }
        }
    }
}
