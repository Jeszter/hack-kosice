package com.equipay.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.VoiceParseResponse
import com.equipay.app.ui.theme.*
import com.equipay.app.ui.viewmodels.AppState
import com.equipay.app.ui.viewmodels.VoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private enum class AssistantMode { VOICE, CHAT, CAMERA, GALLERY }

// Language options for speech recognition
private data class SpeechLang(val code: String, val label: String, val flag: String)
private val SPEECH_LANGUAGES = listOf(
    SpeechLang("en-US", "English", "🇺🇸"),
    SpeechLang("sk-SK", "Slovenčina", "🇸🇰"),
    SpeechLang("ru-RU", "Русский", "🇷🇺"),
    SpeechLang("uk-UA", "Українська", "🇺🇦"),
    SpeechLang("cs-CZ", "Čeština", "🇨🇿"),
    SpeechLang("de-DE", "Deutsch", "🇩🇪"),
)

@Composable
fun VoiceAssistantScreen(
    onBack: () -> Unit,
    onConfirm: (VoiceParseResponse) -> Unit
) {
    val vm: VoiceViewModel = viewModel()
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AssistantMode.VOICE) }
    var chatText by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLang by remember { mutableStateOf(SPEECH_LANGUAGES[0]) }
    var showLangPicker by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    // ========== VOICE SETUP ==========
    var audioPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx) else null
    }
    var pendingVoice by remember { mutableStateOf(false) }
    val latestRecognizer by rememberUpdatedState(recognizer)
    val latestTranscript by rememberUpdatedState(state.transcript)
    val latestLang by rememberUpdatedState(selectedLang)

    fun launchListening() {
        val rec = latestRecognizer ?: run {
            vm.setError("Speech recognition not available on this device")
            return
        }
        mode = AssistantMode.VOICE
        vm.reset()
        rec.cancel()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) // more candidates for better accuracy
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, latestLang.code)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, latestLang.code)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US")) // fallback
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { vm.setListening(true); vm.setError(null) }
            override fun onBeginningOfSpeech() { vm.setListening(true) }
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { vm.setListening(false) }
            override fun onError(err: Int) {
                vm.setListening(false)
                vm.setError(
                    when (err) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that — try again"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy — try again"
                        else -> "Speech error ($err) — try again"
                    }
                )
            }
            override fun onResults(results: Bundle?) {
                // Use highest-confidence result (first in list)
                val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = candidates?.firstOrNull()
                if (!best.isNullOrBlank()) {
                    vm.setTranscript(best)
                    vm.parse(best)
                } else if (latestTranscript.isNotBlank()) {
                    vm.parse(latestTranscript)
                } else {
                    vm.setError("Couldn't understand — speak clearly and try again")
                }
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.let { vm.setTranscript(it) }
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        rec.startListening(intent)
    }

    val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audioPermissionGranted = granted
        if (granted && pendingVoice) { pendingVoice = false; launchListening() }
        else if (!granted) { pendingVoice = false; vm.setError("Microphone permission denied") }
    }

    fun startListening() {
        if (!audioPermissionGranted) { pendingVoice = true; audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        else launchListening()
    }

    DisposableEffect(Unit) { onDispose { recognizer?.cancel(); recognizer?.destroy() } }

    // ========== CAMERA / GALLERY SETUP ==========
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.setError("Camera permission denied")
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            scope.launch {
                val base64 = withContext(Dispatchers.IO) { uriToBase64Jpeg(ctx, uri) }
                if (base64 != null) vm.parseImage(base64, "image/jpeg")
                else vm.setError("Couldn't read the photo")
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            mode = AssistantMode.GALLERY
            scope.launch {
                val base64 = withContext(Dispatchers.IO) { uriToBase64Jpeg(ctx, uri) }
                if (base64 != null) vm.parseImage(base64, "image/jpeg")
                else vm.setError("Couldn't read the image")
            }
        }
    }

    fun launchCamera() {
        val cameraGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!cameraGranted) { cameraPermLauncher.launch(Manifest.permission.CAMERA); return }
        val uri = createTempImageUri(ctx)
        if (uri == null) { vm.setError("Camera storage not available"); return }
        cameraUri = uri
        mode = AssistantMode.CAMERA
        try { takePictureLauncher.launch(uri) } catch (e: Exception) { vm.setError("Camera not available: ${e.message}") }
    }

    fun sendChat() {
        val t = chatText.trim()
        if (t.isBlank()) return
        mode = AssistantMode.CHAT
        keyboard?.hide()
        vm.parse(t)
        chatText = ""
    }

    // ========== LANG PICKER OVERLAY ==========
    if (showLangPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showLangPicker = false }) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Surface1).padding(20.dp)
            ) {
                Column {
                    Text("Speech language", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    SPEECH_LANGUAGES.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (lang.code == selectedLang.code) Surface2 else Color.Transparent)
                                .clickable { selectedLang = lang; showLangPicker = false }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.flag, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(lang.label, color = TextPrimary, fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            if (lang.code == selectedLang.code) {
                                Icon(Icons.Default.Check, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== UI ==========
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(horizontal = 20.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() })
            // Language selector
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Surface1)
                    .clickable { showLangPicker = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(selectedLang.flag, fontSize = 16.sp)
                Text(selectedLang.label, color = TextSecondary, fontSize = 12.sp)
                Icon(Icons.Default.ExpandMore, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("SplitFlow Assistant", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            text = when {
                mode == AssistantMode.VOICE && state.listening -> "Listening (${selectedLang.label})..."
                state.loading && mode == AssistantMode.CAMERA -> "Scanning receipt..."
                state.loading && mode == AssistantMode.GALLERY -> "Scanning image..."
                state.loading -> "Thinking..."
                mode == AssistantMode.CHAT -> "Describe what to split"
                mode == AssistantMode.CAMERA -> "Snap a receipt"
                mode == AssistantMode.GALLERY -> "Pick receipt from gallery"
                else -> "Tap mic, type, or scan a receipt"
            },
            color = TextSecondary, fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        Waveform(
            active = mode == AssistantMode.VOICE && state.listening,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )

        Spacer(Modifier.height(16.dp))

        // App context hint
        if (!state.listening && state.parsed == null && state.transcript.isEmpty()) {
            val accountId = AppState.selectedAccountId.value
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Surface1).padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = TextMuted, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (accountId != null)
                            "I know your group and members. Tell me: \"Split 45€ pizza between everyone\" or \"I paid 120€ for groceries\""
                        else
                            "Create or select a group first, then come back to split expenses by voice.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Scroll area
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (state.transcript.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Surface1).padding(14.dp)
                ) {
                    Text(state.transcript, color = TextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            state.parsed?.let { parsed ->
                AiReplyBubble(parsed = parsed, onConfirm = { onConfirm(parsed) }, onRetry = { vm.reset() })
                Spacer(Modifier.height(12.dp))
            }

            if (!state.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Red.copy(alpha = 0.1f)).border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.error!!, color = Red, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (mode == AssistantMode.CHAT) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Surface1).border(1.dp, Surface2, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        singleLine = false, maxLines = 4,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendChat() }),
                        decorationBox = { inner ->
                            if (chatText.isEmpty()) Text("e.g. Split 40€ for pizza between us", color = TextMuted, fontSize = 15.sp)
                            inner()
                        }
                    )
                    Spacer(Modifier.size(8.dp))
                    val canSend = chatText.isNotBlank() && !state.loading
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (canSend) AccentWhite else Surface2)
                            .clickable(enabled = canSend) { sendChat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null,
                            tint = if (canSend) AccentOnWhite else TextMuted,
                            modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Try saying:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Split 40€ for pizza equally",
                    "I paid 12€ for Uber, split it",
                    "Museum 60€, 3 people"
                ).forEach { example ->
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Surface1).clickable { chatText = example; sendChat() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(example, color = TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            ModeButton(Icons.Default.ChatBubbleOutline, "Chat", mode == AssistantMode.CHAT) {
                mode = AssistantMode.CHAT; vm.setError(null)
            }
            ModeButton(Icons.Default.CameraAlt, "Camera", mode == AssistantMode.CAMERA) { launchCamera() }

            // Voice — centered, bigger
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(78.dp).clip(CircleShape)
                        .background(if (mode == AssistantMode.VOICE && state.listening) AccentWhite else Surface2)
                        .border(width = if (mode == AssistantMode.VOICE) 2.dp else 0.dp, color = AccentWhite, shape = CircleShape)
                        .clickable { mode = AssistantMode.VOICE; startListening() },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.loading && mode == AssistantMode.VOICE) {
                        CircularProgressIndicator(color = TextPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.Mic, null,
                            tint = if (state.listening && mode == AssistantMode.VOICE) AccentOnWhite else TextPrimary,
                            modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (mode == AssistantMode.VOICE && state.listening) "Listening..." else "Voice",
                    color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }

            ModeButton(Icons.Default.Image, "Gallery", mode == AssistantMode.GALLERY) {
                mode = AssistantMode.GALLERY
                try { pickImageLauncher.launch("image/*") } catch (e: Exception) { vm.setError("Gallery unavailable") }
            }
            ModeButton(Icons.Default.AutoAwesome, "Reset", false) {
                vm.reset(); chatText = ""; mode = AssistantMode.VOICE
            }
        }
    }
}

@Composable
private fun ModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape)
                .background(if (active) AccentWhite else Surface2).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) AccentOnWhite else TextPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun AiReplyBubble(parsed: VoiceParseResponse, onConfirm: () -> Unit, onRetry: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = TextPrimary, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                .background(Surface1).padding(14.dp)
        ) {
            Text(parsed.confirmationText, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val canConfirm = parsed.amountCents > 0
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (canConfirm) AccentWhite else Surface2)
                        .clickable(enabled = canConfirm) { onConfirm() }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Confirm", color = if (canConfirm) AccentOnWhite else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(Surface2).clickable { onRetry() }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Try again", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 52
        val spacing = size.width / barCount
        val midY = size.height / 2f
        for (i in 0 until barCount) {
            val x = i * spacing + spacing / 2f
            val centerDist = abs(i - barCount / 2f) / (barCount / 2f)
            val envelope = (1f - centerDist * 0.6f).coerceIn(0f, 1f)
            val amp = if (active) (sin(phase + i * 0.4f) * 0.55f + 0.45f).toFloat() else 0.08f
            val h = (envelope * amp * size.height * 0.92f).coerceAtLeast(2f)
            drawLine(
                color = Color.White.copy(alpha = if (active) 0.9f else 0.2f),
                start = androidx.compose.ui.geometry.Offset(x, midY - h / 2),
                end = androidx.compose.ui.geometry.Offset(x, midY + h / 2),
                strokeWidth = 2.2f, cap = StrokeCap.Round
            )
        }
    }
}

// ========== IMAGE HELPERS ==========

private fun createTempImageUri(ctx: android.content.Context): Uri? = try {
    val file = File.createTempFile("receipt_", ".jpg", ctx.cacheDir)
    androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
} catch (e: Exception) { null }

private fun uriToBase64Jpeg(ctx: android.content.Context, uri: Uri): String? {
    return try {
        val input = ctx.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(input) ?: return null
        input.close()

        val orientedBitmap = try {
            val exifInput = ctx.contentResolver.openInputStream(uri)
            val orientation = if (exifInput != null) {
                val exif = ExifInterface(exifInput)
                exifInput.close()
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } else ExifInterface.ORIENTATION_NORMAL
            rotateBitmap(originalBitmap, orientation)
        } catch (e: Exception) { originalBitmap }

        val maxDim = 1024
        val w = orientedBitmap.width; val h = orientedBitmap.height
        val scaled = if (w > maxDim || h > maxDim) {
            val ratio = maxDim.toFloat() / maxOf(w, h)
            Bitmap.createScaledBitmap(orientedBitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
        } else orientedBitmap

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
        val bytes = baos.toByteArray()
        if (scaled !== originalBitmap) scaled.recycle()
        if (orientedBitmap !== originalBitmap) orientedBitmap.recycle()
        originalBitmap.recycle()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

private fun rotateBitmap(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return source
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}


