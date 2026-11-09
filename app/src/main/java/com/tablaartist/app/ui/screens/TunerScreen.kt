package com.tablaartist.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay
import com.tablaartist.app.audio.TablaAudioEngine
import com.tablaartist.app.ui.theme.*
import kotlin.math.*

enum class NotationMode(val title: String) {
    STANDARD("Standard (C, C#)"),
    INDIAN_SUR("Indian Sur (काली / सफ़ेद)"),
    SARGAM("Sargam (सा, रे, ग)"),
    SOLFEGE("Solfège (Do, Re, Mi)")
}

data class ScaleOption(
    val note: String,
    val hindiName: String,
    val scaleNumber: String,
    val baseFreq: Float
)

val SCALES = listOf(
    ScaleOption("C#", "काली १ (Kali 1)", "Scale 1 - Kali", 138.59f),
    ScaleOption("D", "सफ़ेद २ (Safed 2)", "Scale 2 - Safed", 146.83f),
    ScaleOption("D#", "काली २ (Kali 2)", "Scale 2 - Kali", 155.56f),
    ScaleOption("E", "सफ़ेद ३ (Safed 3)", "Scale 3 - Safed", 164.81f),
    ScaleOption("F", "सफ़ेद ४ (Safed 4)", "Scale 4 - Safed", 174.61f),
    ScaleOption("F#", "काली ३ (Kali 3)", "Scale 3 - Kali", 185.00f),
    ScaleOption("G", "सफ़ेद ५ (Safed 5)", "Scale 5 - Safed", 196.00f),
    ScaleOption("G#", "काली ४ (Kali 4)", "Scale 4 - Kali", 207.65f),
    ScaleOption("A", "सफ़ेद ६ (Safed 6)", "Scale 6 - Safed", 220.00f),
    ScaleOption("A#", "काली ५ (Kali 5)", "Scale 5 - Kali", 233.08f),
    ScaleOption("B", "सफ़ेद ७ (Safed 7)", "Scale 7 - Safed", 246.94f),
    ScaleOption("C", "सफ़ेद १ (Safed 1)", "Scale 1 - Safed", 130.81f)
)

private val CHROMATIC_NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private val SARGAM_MAP = mapOf(
    "C" to "Sa", "C#" to "re", "D" to "Re", "D#" to "ga", "E" to "Ga",
    "F" to "ma", "F#" to "Ma", "G" to "Pa", "G#" to "dha", "A" to "Dha", "A#" to "ni", "B" to "Ni"
)

private val INDIAN_SUR_MAP = mapOf(
    "C" to "सफ़ेद १", "C#" to "काली १", "D" to "सफ़ेद २", "D#" to "काली २",
    "E" to "सफ़ेद ३", "F" to "सफ़ेद ४", "F#" to "काली ३", "G" to "सफ़ेद ५",
    "G#" to "काली ४", "A" to "सफ़ेद ६", "A#" to "काली ५", "B" to "सफ़ेद ७"
)

private val SOLFEGE_MAP = mapOf(
    "C" to "Do", "C#" to "Do#", "D" to "Re", "D#" to "Re#", "E" to "Mi",
    "F" to "Fa", "F#" to "Fa#", "G" to "Sol", "G#" to "Sol#", "A" to "La", "A#" to "La#", "B" to "Ti"
)

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isListening by remember { mutableStateOf(false) }
    var selectedScale by remember { mutableStateOf(SCALES[0]) } // C# default
    var detectedNote by remember { mutableStateOf("C#") }
    var detectedCents by remember { mutableStateOf(0) }
    var detectedFreq by remember { mutableStateOf(138.6f) }
    var detectedOctave by remember { mutableStateOf(3) }
    var inputDb by remember { mutableStateOf(-60f) }

    // CarlTune Settings State
    var concertPitch by remember { mutableStateOf(440.0) }
    var toleranceCents by remember { mutableStateOf(3) } // ±3 cents
    var notationMode by remember { mutableStateOf(NotationMode.STANDARD) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isPitchPipeMode by remember { mutableStateOf(false) }
    var pitchPipeOctave by remember { mutableStateOf(3) }

    // Peak Potential State: Drum Filter Mode, Auto Peak-Hold & Noise Gate
    var tunerFilterMode by remember { mutableStateOf(TablaAudioEngine.TunerFilterMode.DAYAN) }
    var isPeakHoldEnabled by remember { mutableStateOf(true) }
    var isPeakHeld by remember { mutableStateOf(false) }
    var peakRemainingSecs by remember { mutableStateOf(0f) }
    var heldNote by remember { mutableStateOf("C#") }
    var heldCents by remember { mutableStateOf(0) }
    var heldFreq by remember { mutableStateOf(138.6f) }
    var heldOctave by remember { mutableStateOf(3) }
    var noiseGateDb by remember { mutableStateOf(-46.0f) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            isListening = true
        }
    }

    // Synchronize Concert Pitch & Filters with TablaAudioEngine
    LaunchedEffect(concertPitch) {
        TablaAudioEngine.concertPitchA4 = concertPitch
    }
    LaunchedEffect(tunerFilterMode) {
        TablaAudioEngine.filterMode = tunerFilterMode
    }
    LaunchedEffect(noiseGateDb) {
        TablaAudioEngine.noiseGateDb = noiseGateDb
    }

    // Auto-Strike Peak Hold Countdown Timer
    LaunchedEffect(isPeakHeld) {
        if (isPeakHeld) {
            while (peakRemainingSecs > 0f) {
                delay(100)
                peakRemainingSecs = (peakRemainingSecs - 0.1f).coerceAtLeast(0f)
            }
            isPeakHeld = false
        }
    }

    // Privacy & Lifecycle Guardian
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isListening = false
                isPeakHeld = false
                TablaAudioEngine.stopPitchDetection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            TablaAudioEngine.stopPitchDetection()
        }
    }

    DisposableEffect(isListening) {
        if (isListening && hasPermission) {
            TablaAudioEngine.startPitchDetection(scope) { note, cents, freq, octave, db ->
                if (note.isNotEmpty()) {
                    detectedNote = note
                    detectedCents = cents
                    detectedFreq = freq
                    detectedOctave = octave

                    if (isPeakHoldEnabled && db >= noiseGateDb) {
                        heldNote = note
                        heldCents = cents
                        heldFreq = freq
                        heldOctave = octave
                        peakRemainingSecs = 2.5f
                        isPeakHeld = true
                    }
                }
                inputDb = db
            }
        } else {
            TablaAudioEngine.stopPitchDetection()
        }
        onDispose {
            TablaAudioEngine.stopPitchDetection()
        }
    }

    val effectiveNote = if (isPeakHeld && !isListening) heldNote else if (detectedNote.isNotEmpty()) detectedNote else heldNote
    val effectiveCents = if (isPeakHeld && !isListening) heldCents else detectedCents
    val effectiveFreq = if (isPeakHeld && !isListening) heldFreq else detectedFreq
    val effectiveOctave = if (isPeakHeld && !isListening) heldOctave else detectedOctave

    val isInTune = (effectiveNote == selectedScale.note) && abs(effectiveCents) <= toleranceCents
    val isNearTune = (effectiveNote == selectedScale.note) && abs(effectiveCents) <= 10

    val animatedCents by animateFloatAsState(
        targetValue = effectiveCents.toFloat(),
        animationSpec = tween(durationMillis = 120),
        label = "centsAnim"
    )

    val animatedDb by animateFloatAsState(
        targetValue = inputDb,
        animationSpec = tween(durationMillis = 80),
        label = "dbAnim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SyahiBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CarlTune Top Bar: Concert Pitch, Mode Switcher, Settings Button, Staff View
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Concert Pitch badge (clickable)
            Surface(
                color = SyahiSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SyahiBorder),
                modifier = Modifier.clickable { showSettingsDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "A4",
                        tint = CopperAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A4 = ${String.format("%.1f", concertPitch)} Hz",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            // Center: Mode Switcher (Mic Tuner vs Pitch Pipe)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SyahiSurfaceVariant)
                    .padding(2.dp)
            ) {
                Surface(
                    color = if (!isPitchPipeMode) CopperAccent else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { isPitchPipeMode = false }
                ) {
                    Text(
                        text = "Tuner",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (!isPitchPipeMode) Color.White else TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = if (isPitchPipeMode) CopperAccent else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        isPitchPipeMode = true
                        isListening = false
                        TablaAudioEngine.stopPitchDetection()
                    }
                ) {
                    Text(
                        text = "Pitch Pipe",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isPitchPipeMode) Color.White else TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Right: Settings Icon
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SyahiSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CarlTune Musical Staff + Status Row
        Card(
            colors = CardDefaults.cardColors(containerColor = SyahiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val formattedName = when (notationMode) {
                        NotationMode.STANDARD -> selectedScale.note
                        NotationMode.INDIAN_SUR -> INDIAN_SUR_MAP[selectedScale.note] ?: selectedScale.note
                        NotationMode.SARGAM -> SARGAM_MAP[selectedScale.note] ?: selectedScale.note
                        NotationMode.SOLFEGE -> SOLFEGE_MAP[selectedScale.note] ?: selectedScale.note
                    }
                    Text(
                        text = "Target Sur: $formattedName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SamGold
                    )
                    Text(
                        text = "${selectedScale.hindiName} • ±$toleranceCents ¢ tol",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // CarlTune Mini Staff Canvas
                CarlTuneStaffView(
                    detectedNote = detectedNote,
                    detectedOctave = detectedOctave,
                    isInTune = isInTune,
                    modifier = Modifier
                        .width(110.dp)
                        .height(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scale Selector (Horizontal Scroll)
        Text(
            text = "SELECT TARGET SUR / DAYAN SCALE:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = CopperAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SCALES.forEach { scale ->
                val isSelected = scale.note == selectedScale.note
                Surface(
                    color = if (isSelected) CopperAccent else SyahiSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SyahiBorder),
                    modifier = Modifier.clickable { selectedScale = scale }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = scale.note,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.Black else TextPrimary
                        )
                        Text(
                            text = scale.hindiName.substringBefore(" ("),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) Color.Black.copy(alpha = 0.85f) else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabla Drum Acoustic Mode Filter Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TablaAudioEngine.TunerFilterMode.values().forEach { mode ->
                val isSelected = mode == tunerFilterMode
                Surface(
                    color = if (isSelected) CopperAccent.copy(alpha = 0.15f) else SyahiSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) CopperAccent else SyahiBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { tunerFilterMode = mode }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF10B981) else Color(0xFF5A4A3E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = if (isSelected) CopperAccent else TextPrimary
                            )
                        }
                        Text(
                            text = "${mode.minFreq.toInt()}-${mode.maxFreq.toInt()} Hz",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-Strike Peak Hold & Noise Gate Toolkit Row
        Card(
            colors = CardDefaults.cardColors(containerColor = SyahiSurface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SyahiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isPeakHoldEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else SyahiSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isPeakHoldEnabled) Color(0xFF10B981) else SyahiBorder),
                    modifier = Modifier.clickable { isPeakHoldEnabled = !isPeakHoldEnabled }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱ Auto-Hold: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = if (isPeakHoldEnabled) "2.5s ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (isPeakHoldEnabled) Color(0xFF10B981) else TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gate: ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                    Slider(
                        value = noiseGateDb,
                        onValueChange = { noiseGateDb = it },
                        valueRange = -55f..-25f,
                        colors = SliderDefaults.colors(
                            thumbColor = CopperAccent,
                            activeTrackColor = CopperAccent,
                            inactiveTrackColor = SyahiSurfaceVariant
                        ),
                        modifier = Modifier.width(90.dp).height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${noiseGateDb.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main CarlTune Display: VU Meter (Left) + Side Telemetry (Middle) + 360° Chromatic Dial (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. CarlTune Input VU Meter (PView)
            CarlTuneVuMeter(
                dbLevel = animatedDb,
                isListening = isListening,
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(0.92f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Dedicated Side Telemetry Readout Panel (NO TEXT OVERLAPPING NEEDLE)
            Card(
                colors = CardDefaults.cardColors(containerColor = SyahiSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SyahiBorder),
                modifier = Modifier
                    .width(84.dp)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Octave
                    Column {
                        Text(
                            text = "OCTAVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = TextMuted
                        )
                        Text(
                            text = if (effectiveOctave > 0) "OCT $effectiveOctave" else "--",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    // Frequency
                    Column {
                        Text(
                            text = "FREQUENCY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = TextMuted
                        )
                        Text(
                            text = if (effectiveFreq > 0) "${String.format("%.1f", effectiveFreq)} Hz" else "-- Hz",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = SamGold
                        )
                    }
                    // Deviation Cents
                    Column {
                        Text(
                            text = "DEVIATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = TextMuted
                        )
                        Text(
                            text = if (effectiveFreq > 0) "${if (effectiveCents >= 0) "+" else ""}$effectiveCents ¢" else "-- ¢",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isInTune) Color(0xFF10B981) else if (abs(effectiveCents) > 15) Color(0xFFEF4444) else CopperAccent
                        )
                    }
                    // Status Badge
                    Surface(
                        color = if (isInTune) Color(0xFF10B981) else if (isNearTune) SamGold else SyahiSurfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isInTune) "IN TUNE" else if (effectiveNote == selectedScale.note) (if (effectiveCents < 0) "FLAT" else "SHARP") else if (effectiveNote.isNotEmpty()) "MISMATCH" else "READY",
                            color = if (isInTune || isNearTune) Color.Black else TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                    // Peak Latch Badge (when peak hold active)
                    if (isPeakHeld) {
                        Surface(
                            color = Color(0x333B82F6),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "HELD ${String.format("%.1f", peakRemainingSecs)}s",
                                color = Color(0xFF60A5FA),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. CarlTune 360° Circular Dial (OView)
            Box(
                modifier = Modifier.size(232.dp),
                contentAlignment = Alignment.Center
            ) {
                val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(
                    modifier = Modifier.fillMaxSize(),
                    onDraw = {
                        drawCarlTuneFullDial(
                            detectedNote = effectiveNote,
                            cents = animatedCents,
                            targetNote = selectedScale.note,
                            surfaceVariant = surfaceVar,
                            primaryColor = primaryColor,
                            isInTune = isInTune,
                            toleranceCents = toleranceCents,
                            notationMode = notationMode
                        )
                    }
                )

                // Center Note ONLY: Clean, bold, zero overlap behind the rotating needle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val displayNote = when (notationMode) {
                        NotationMode.STANDARD -> effectiveNote
                        NotationMode.INDIAN_SUR -> INDIAN_SUR_MAP[effectiveNote] ?: effectiveNote
                        NotationMode.SARGAM -> SARGAM_MAP[effectiveNote] ?: effectiveNote
                        NotationMode.SOLFEGE -> SOLFEGE_MAP[effectiveNote] ?: effectiveNote
                    }
                    Text(
                        text = displayNote.ifEmpty { selectedScale.note },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (displayNote.length > 2) 32.sp else 46.sp
                        ),
                        color = if (isInTune) Color(0xFF10B981) else CopperAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Fine Cents Deviation Track (-50 to +50)
        Card(
            colors = CardDefaults.cardColors(containerColor = SyahiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-50¢ FLAT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                    Text("±$toleranceCents¢ IN TUNE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                    Text("+50¢ SHARP", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Visual meter track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SyahiSurfaceVariant)
                ) {
                    val inTuneFraction = (toleranceCents * 2f) / 100f
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(inTuneFraction)
                            .fillMaxHeight()
                            .background(Color(0xFF10B981).copy(alpha = 0.45f))
                    )
                    val fraction = ((animatedCents + 50f) / 100f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isInTune) Color(0xFF10B981) else CopperAccent)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time Tabla Harmonic Spectrum Visualizer
        TablaHarmonicSpectrumCanvas(
            effectiveFreq = effectiveFreq,
            isInTune = isInTune,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // CarlTune Interactive Piano Keyboard Strip (KView)
        Text(
            text = "CARLTUNE PIANO KEYBOARD (TAP TO HEAR PITCH PIPE):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = CopperAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        CarlTunePianoKeyboard(
            detectedNote = detectedNote,
            detectedOctave = detectedOctave,
            onKeyTapped = { note, oct ->
                val freq = TablaAudioEngine.noteToFrequency(note, oct)
                TablaAudioEngine.playReferenceTone(freq, 1200)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPitchPipeMode) {
            // Pitch Pipe Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = SyahiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reference Pitch Pipe Generator",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SamGold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Octave: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            listOf(2, 3, 4, 5).forEach { oct ->
                                Surface(
                                    color = if (pitchPipeOctave == oct) CopperAccent else SyahiSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .clickable { pitchPipeOctave = oct }
                                ) {
                                    Text(
                                        text = "$oct",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (pitchPipeOctave == oct) Color.Black else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap any note below to sound reference acoustic tone:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CHROMATIC_NOTES.forEach { note ->
                            Button(
                                onClick = {
                                    val f = TablaAudioEngine.noteToFrequency(note, pitchPipeOctave)
                                    TablaAudioEngine.playReferenceTone(f, 1500)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SyahiSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = note, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = "${pitchPipeOctave}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = CopperAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Start / Stop Microphone Button
            Button(
                onClick = {
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        isListening = !isListening
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFDC2626) else CopperAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!hasPermission) "Grant Mic Permission" else if (isListening) "Stop Tuning" else "Start Listening",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isListening) "CarlTune Chromatic Autocorrelation active • Strike your Dayan" else "Tap Start to begin live CarlTune pitch analysis",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }

    // CarlTune Settings Dialog (Matching dialog_menu.xml)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "CarlTune Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // 1. Concert Pitch A4 Slider
                    Text(
                        text = "Concert Pitch (A4): ${String.format("%.1f", concertPitch)} Hz",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CopperAccent
                    )
                    Slider(
                        value = concertPitch.toFloat(),
                        onValueChange = { concertPitch = it.toDouble() },
                        valueRange = 430f..450f,
                        steps = 20,
                        colors = SliderDefaults.colors(thumbColor = CopperAccent, activeTrackColor = CopperAccent)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("430 Hz", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("440 Hz (Std)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("450 Hz", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Tolerance Range (±cents)
                    Text(
                        text = "Tuning In-Tune Tolerance:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CopperAccent
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "±1 ¢ (Pro)", 3 to "±3 ¢ (Normal)", 5 to "±5 ¢ (Relaxed)").forEach { (cents, label) ->
                            Surface(
                                color = if (toleranceCents == cents) CopperAccent else SyahiSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { toleranceCents = cents }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (toleranceCents == cents) Color.Black else TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Notation Style
                    Text(
                        text = "Notation Format:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CopperAccent
                    )
                    NotationMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notationMode = mode }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = notationMode == mode,
                                onClick = { notationMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = CopperAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = mode.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CopperAccent)
                ) {
                    Text("Done", color = Color.White)
                }
            },
            containerColor = SyahiSurface
        )
    }
}

/**
 * CarlTune Input Level VU Meter (PView)
 */
@Composable
private fun CarlTuneVuMeter(
    dbLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SyahiSurface),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SyahiBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val fraction = if (isListening) ((dbLevel + 60f) / 60f).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (fraction > 0.85f) Color(0xFFEF4444)
                        else if (fraction > 0.5f) SamGold
                        else Color(0xFF10B981)
                    )
            )
        }
    }
}

/**
 * CarlTune Staff View (StaffView in APK): 5 horizontal staff lines with dynamic note head.
 */
@Composable
private fun CarlTuneStaffView(
    detectedNote: String,
    detectedOctave: Int,
    isInTune: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lineSpacing = h / 6f
        val linePaint = Color(0xFF6B584B)

        // Draw 5 staff lines
        for (i in 1..5) {
            val y = i * lineSpacing
            drawLine(
                color = linePaint,
                start = Offset(6f, y),
                end = Offset(w - 6f, y),
                strokeWidth = 1.5f
            )
        }

        // Treble Clef indicator dot/G-line marker
        drawCircle(
            color = Color(0xFFC85A17),
            radius = 3.5f,
            center = Offset(16f, 4 * lineSpacing)
        )

        // Calculate note head Y position based on note and octave
        val noteIdx = CHROMATIC_NOTES.indexOf(detectedNote).let { if (it < 0) 0 else it }
        val semiFromMiddleC = (detectedOctave - 4) * 12 + noteIdx // C4 = 0
        val staffSteps = (semiFromMiddleC * 0.58f) // rough diatonic mapping
        val noteY = (5 * lineSpacing) - (staffSteps * (lineSpacing * 0.45f))

        val noteHeadColor = if (isInTune) Color(0xFF10B981) else CopperAccent
        drawCircle(
            color = noteHeadColor,
            radius = 5.5f,
            center = Offset(w * 0.65f, noteY.coerceIn(4f, h - 4f))
        )
        // Note stem
        drawLine(
            color = noteHeadColor,
            start = Offset(w * 0.65f + 5f, noteY.coerceIn(4f, h - 4f)),
            end = Offset(w * 0.65f + 5f, (noteY - 18f).coerceIn(2f, h - 2f)),
            strokeWidth = 2f
        )
    }
}

/**
 * CarlTune Piano Keyboard Strip (KView): Interactive 2-octave piano keys.
 */
@Composable
private fun CarlTunePianoKeyboard(
    detectedNote: String,
    detectedOctave: Int,
    onKeyTapped: (note: String, octave: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0E0C))
            .border(1.dp, SyahiBorder, RoundedCornerShape(8.dp))
            .horizontalScroll(scroll)
            .padding(2.dp)
    ) {
        listOf(2, 3, 4, 5).forEach { oct ->
            CHROMATIC_NOTES.forEach { note ->
                val isSharp = note.contains("#")
                val isCurrent = (note == detectedNote && oct == detectedOctave)
                Surface(
                    color = when {
                        isCurrent -> Color(0xFF10B981)
                        isSharp -> Color(0xFF2B2622)
                        else -> Color(0xFFE8DEC8)
                    },
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .width(if (isSharp) 18.dp else 24.dp)
                        .fillMaxHeight(if (isSharp) 0.68f else 1f)
                        .padding(horizontal = 1.dp)
                        .clickable { onKeyTapped(note, oct) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (!isSharp) {
                            Text(
                                text = "$note$oct",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = if (isCurrent) Color.Black else Color(0xFF4A3E36),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws CarlTune complete 360° circular dial with radial chromatic notes, fine subticks,
 * in-tune guidance arc, and high-precision needle.
 */
private fun DrawScope.drawCarlTuneFullDial(
    detectedNote: String,
    cents: Float,
    targetNote: String,
    surfaceVariant: Color,
    primaryColor: Color,
    isInTune: Boolean,
    toleranceCents: Int,
    notationMode: NotationMode
) {
    val center = Offset(size.width / 2, size.height / 2)
    val outerRadius = (size.minDimension / 2) - 8f
    val innerRadius = outerRadius - 36f

    // Outer dial track
    drawCircle(
        color = Color(0xFF1E1A17),
        center = center,
        radius = outerRadius
    )
    drawCircle(
        color = Color(0xFF13110F),
        center = center,
        radius = innerRadius
    )

    // Outer rim stroke
    drawCircle(
        color = if (isInTune) Color(0xFF10B981) else Color(0xFF3E2D22),
        center = center,
        radius = outerRadius,
        style = Stroke(width = if (isInTune) 3.5f else 2f)
    )

    val textPaint = android.graphics.Paint().apply {
        textSize = 30f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    // Draw 12 Chromatic Notes and radial ticks
    CHROMATIC_NOTES.forEachIndexed { idx, noteName ->
        val noteAngleDeg = -90f + idx * 30f
        val rad = (noteAngleDeg * Math.PI / 180f).toDouble()

        val isTarget = noteName == targetNote
        val isCurrent = noteName == detectedNote

        // Tick mark on rim
        val tickOuterX = (center.x + outerRadius * cos(rad)).toFloat()
        val tickOuterY = (center.y + outerRadius * sin(rad)).toFloat()
        val tickLen = if (isTarget) 12f else 8f
        val tickInnerX = (center.x + (outerRadius - tickLen) * cos(rad)).toFloat()
        val tickInnerY = (center.y + (outerRadius - tickLen) * sin(rad)).toFloat()

        drawLine(
            color = when {
                isCurrent && isInTune -> Color(0xFF10B981)
                isCurrent -> Color(0xFFD97706)
                isTarget -> Color(0xFFF59E0B)
                else -> Color(0xFF5A4A3E)
            },
            start = Offset(tickInnerX, tickInnerY),
            end = Offset(tickOuterX, tickOuterY),
            strokeWidth = if (isTarget || isCurrent) 3.5f else 1.5f
        )

        // Note Label Position
        val textDist = outerRadius - 22f
        val tx = (center.x + textDist * cos(rad)).toFloat()
        val ty = (center.y + textDist * sin(rad)).toFloat()

        val label = when (notationMode) {
            NotationMode.STANDARD -> noteName
            NotationMode.INDIAN_SUR -> INDIAN_SUR_MAP[noteName]?.take(4) ?: noteName
            NotationMode.SARGAM -> SARGAM_MAP[noteName] ?: noteName
            NotationMode.SOLFEGE -> SOLFEGE_MAP[noteName] ?: noteName
        }

        textPaint.color = when {
            isCurrent && isInTune -> android.graphics.Color.rgb(16, 185, 129)
            isCurrent -> android.graphics.Color.rgb(217, 119, 6)
            isTarget -> android.graphics.Color.rgb(245, 158, 11)
            else -> android.graphics.Color.rgb(140, 130, 120)
        }
        textPaint.textSize = if (isCurrent || isTarget) 32f else 24f
        drawContext.canvas.nativeCanvas.drawText(label, tx, ty + 10f, textPaint)
    }

    // Draw intermediate sub-ticks (every 10 cents / 3 degrees)
    for (step in 0 until 120) {
        if (step % 10 == 0) continue
        val angleDeg = -90f + step * 3f
        val rad = (angleDeg * Math.PI / 180f).toDouble()
        val sx1 = (center.x + (outerRadius - 4f) * cos(rad)).toFloat()
        val sy1 = (center.y + (outerRadius - 4f) * sin(rad)).toFloat()
        val sx2 = (center.x + outerRadius * cos(rad)).toFloat()
        val sy2 = (center.y + outerRadius * sin(rad)).toFloat()
        drawLine(
            color = Color(0xFF322720),
            start = Offset(sx1, sy1),
            end = Offset(sx2, sy2),
            strokeWidth = 1f
        )
    }

    // CarlTune Dynamic Needle with Cents Deflection
    val detectedIndex = CHROMATIC_NOTES.indexOf(detectedNote).let { if (it < 0) 0 else it }
    val needleAngleDeg = (-90f + detectedIndex * 30f + (cents / 100f) * 30f)
    val needleRad = (needleAngleDeg * Math.PI / 180f).toDouble()
    val needleLen = outerRadius - 4f

    val needleColor = if (isInTune) Color(0xFF10B981) else CopperAccent
    val needleEnd = Offset(
        (center.x + needleLen * cos(needleRad)).toFloat(),
        (center.y + needleLen * sin(needleRad)).toFloat()
    )

    // Needle shadow
    drawLine(
        color = Color(0x66000000),
        start = Offset(center.x + 2f, center.y + 2f),
        end = Offset(needleEnd.x + 2f, needleEnd.y + 2f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    // Needle main line
    drawLine(
        color = needleColor,
        start = center,
        end = needleEnd,
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    // Pivot hub
    drawCircle(color = needleColor, center = center, radius = 9f)
    drawCircle(color = Color(0xFF13110F), center = center, radius = 4f)
}

/**
 * Real-time Tabla Harmonic Spectrum Visualizer
 * Renders fundamental frequency and syahi / gajra overtone contours.
 */
@Composable
fun TablaHarmonicSpectrumCanvas(
    effectiveFreq: Float,
    isInTune: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SyahiSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SyahiBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REAL-TIME HARMONIC SPECTRUM (SYAHI OVERTONES)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                    color = CopperAccent
                )
                Text(
                    text = if (effectiveFreq > 0) "Peak: ${String.format("%.1f", effectiveFreq)} Hz" else "Peak: -- Hz",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                    color = if (isInTune) Color(0xFF10B981) else SamGold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val w = size.width
                val h = size.height
                val maxDisplayFreq = 600f
                val fundX = if (effectiveFreq > 0) (effectiveFreq / maxDisplayFreq).coerceIn(0f, 1f) * w else -1f
                val harm2X = if (effectiveFreq > 0) ((effectiveFreq * 2) / maxDisplayFreq).coerceIn(0f, 1f) * w else -1f

                // Base baseline
                drawLine(
                    color = Color(0xFF2E2620),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )

                if (fundX > 0) {
                    val path = Path().apply {
                        moveTo(0f, h)
                        for (i in 0..100) {
                            val xDouble = (i / 100.0) * w.toDouble()
                            val d1 = (xDouble - fundX.toDouble()) / 20.0
                            val g1 = exp(-0.5 * d1 * d1)
                            val d2 = if (harm2X > 0) (xDouble - harm2X.toDouble()) / 16.0 else 100.0
                            val g2 = exp(-0.5 * d2 * d2) * 0.42
                            val y = h - (h - 4f) * (g1 + g2).toFloat().coerceIn(0f, 1f)
                            lineTo(xDouble.toFloat(), y)
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                (if (isInTune) Color(0xFF10B981) else SamGold).copy(alpha = 0.5f),
                                CopperAccent.copy(alpha = 0.08f)
                            )
                        )
                    )
                    // Peak marker line
                    drawLine(
                        color = if (isInTune) Color(0xFF10B981) else SamGold,
                        start = Offset(fundX, 0f),
                        end = Offset(fundX, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("50Hz", "150Hz", "300Hz", "450Hz", "600Hz").forEach {
                    Text(text = it, style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp), color = TextMuted)
                }
            }
        }
    }
}
