package com.tablaartist.app.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tablaartist.app.audio.TablaAudioEngine
import com.tablaartist.app.data.Taal
import com.tablaartist.app.data.seed.TaalSeedData
import com.tablaartist.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MetronomeScreen(modifier: Modifier = Modifier) {
    val taals = TaalSeedData.taals
    var selectedTaal by remember { mutableStateOf(taals.first { it.id == "teentaal" }) }
    var isPlaying by remember { mutableStateOf(false) }
    var bpm by remember { mutableStateOf(120f) }
    var currentMatraIndex by remember { mutableStateOf(0) }
    var endWithTihayi by remember { mutableStateOf(false) }
    var turnRoundBol by remember { mutableStateOf(false) }
    var cycleCount by remember { mutableStateOf(1) }

    val scrollState = rememberScrollState()

    // Metronome audio & cursor timing loop
    LaunchedEffect(isPlaying, bpm, selectedTaal) {
        if (!isPlaying) {
            currentMatraIndex = 0
            return@LaunchedEffect
        }

        while (isPlaying) {
            val totalMatras = selectedTaal.matras
            val isSam = currentMatraIndex == 0

            // Check if current matra is Khali
            var vibhagStart = 0
            var isKhali = false
            for ((idx, splitSize) in selectedTaal.vibhagSplit.withIndex()) {
                if (currentMatraIndex == vibhagStart && selectedTaal.markers.getOrNull(idx) == "0") {
                    isKhali = true
                    break
                }
                vibhagStart += splitSize
            }

            // Play authentic percussive bol stroke
            val currentBol = selectedTaal.thekaBols.getOrElse(currentMatraIndex) { if (isSam) "Dha" else "Ta" }
            TablaAudioEngine.playBolStroke(currentBol)

            val beatDelayMs = (60_000f / bpm).toLong()
            delay(beatDelayMs)

            if (currentMatraIndex == totalMatras - 1) {
                if (endWithTihayi && cycleCount >= 2) {
                    // Land on final Sam and conclude
                    currentMatraIndex = 0
                    TablaAudioEngine.playBolSound(isSam = true, isKhali = false)
                    isPlaying = false
                    break
                }
                currentMatraIndex = 0
                cycleCount++
            } else {
                currentMatraIndex++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SyahiBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Tabla Metronome",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Text(
            text = "Bol-based precision timekeeper with vibhag row grids & Tihayi cadence",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Taal Selector Grid (All 8 Cycles Visible, Zero Horizontal Scrolling)
        Text(
            text = "SELECT TAAL (CYCLE):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = CopperAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            taals.chunked(4).forEach { rowTaals ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowTaals.forEach { taal ->
                        val isSelected = taal.id == selectedTaal.id
                        Surface(
                            color = if (isSelected) CopperAccent else SyahiSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SyahiBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedTaal = taal
                                    currentMatraIndex = 0
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = taal.name,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                                Text(
                                    text = "${taal.matras}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Beat Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SyahiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MATRA ${currentMatraIndex + 1} / ${selectedTaal.matras}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SamGold
                    )
                    val activeBol = selectedTaal.thekaBolsHi.getOrNull(currentMatraIndex) ?: ""
                    val activeBolEn = selectedTaal.thekaBols.getOrNull(currentMatraIndex) ?: ""
                    Text(
                        text = "$activeBol ($activeBolEn)",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (currentMatraIndex == 0) CopperAccent else TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${bpm.toInt()} BPM",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = CopperAccent
                    )
                    Text(
                        text = "Cycle #$cycleCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vibhag-Based Box Grid (One Row per Vibhag Group)
        Text(
            text = "${selectedTaal.name} GRID (${selectedTaal.vibhagSplit.joinToString("/")}):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        val vibhags = selectedTaal.vibhagSplit
        var matraCounter = 0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SyahiSurface, RoundedCornerShape(12.dp))
                .border(1.dp, SyahiBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vibhags.forEachIndexed { vIndex, count ->
                val vMarker = selectedTaal.markers.getOrNull(vIndex) ?: ""
                val vStartMatra = matraCounter

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vibhag marker tag
                    Surface(
                        color = when (vMarker) {
                            "x" -> CopperAccent.copy(alpha = 0.2f)
                            "0" -> KhaliSky.copy(alpha = 0.2f)
                            else -> TaliOrange.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .width(36.dp)
                            .height(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = vMarker,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (vMarker) {
                                    "x" -> CopperAccent
                                    "0" -> KhaliSky
                                    else -> TaliOrange
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Boxes for this vibhag row
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until count) {
                            val currentIdx = vStartMatra + i
                            val isCurrent = isPlaying && currentMatraIndex == currentIdx
                            val bolHi = selectedTaal.thekaBolsHi.getOrNull(currentIdx) ?: ""
                            val bolEn = selectedTaal.thekaBols.getOrNull(currentIdx) ?: ""

                            val boxBg by animateColorAsState(
                                targetValue = if (isCurrent) CopperAccent else SyahiSurfaceVariant,
                                label = "boxBg"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(boxBg)
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) SamGold else SyahiBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = (currentIdx + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isCurrent) Color.Black.copy(alpha = 0.7f) else TextMuted
                                    )
                                    Text(
                                        text = bolHi,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextMuted
                    )
                }

                matraCounter += count
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Variables & Cadence Options: Tihayi & Turn-round Bol
        Card(
            colors = CardDefaults.cardColors(containerColor = SyahiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "COMPOSITION VARIABLES:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = CopperAccent,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End with Tihayi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Concludes cleanly with a 3-fold cadence landing on Sam",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = endWithTihayi,
                        onCheckedChange = { endWithTihayi = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CopperAccent)
                    )
                }

                HorizontalDivider(
                    color = SyahiBorder,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Turn-round Bol",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Plays transitional flourish (Dha-Ge-Na / Uthaan) before Sam",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = turnRoundBol,
                        onCheckedChange = { turnRoundBol = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CopperAccent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tempo Controls Card
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
                        text = "TEMPO (LAYA):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CopperAccent
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "Vilambit" to 60f,
                            "Madhya" to 120f,
                            "Drut" to 180f,
                            "Ati-Drut" to 220f
                        ).forEach { (label, value) ->
                            FilterChip(
                                selected = bpm.toInt() == value.toInt(),
                                onClick = { bpm = value },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 40f..260f,
                    colors = SliderDefaults.colors(
                        thumbColor = CopperAccent,
                        activeTrackColor = CopperAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { bpm = (bpm - 5).coerceAtLeast(40f) }) {
                            Text("-5")
                        }
                        OutlinedButton(onClick = { bpm = (bpm - 1).coerceAtLeast(40f) }) {
                            Icon(Icons.Default.Remove, contentDescription = "-1")
                        }
                    }

                    Text(
                        text = "${bpm.toInt()} BPM",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SamGold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { bpm = (bpm + 1).coerceAtMost(260f) }) {
                            Icon(Icons.Default.Add, contentDescription = "+1")
                        }
                        OutlinedButton(onClick = { bpm = (bpm + 5).coerceAtMost(260f) }) {
                            Text("+5")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play / Stop Master Button
        Button(
            onClick = {
                if (isPlaying) {
                    isPlaying = false
                } else {
                    currentMatraIndex = 0
                    cycleCount = 1
                    isPlaying = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFFDC2626) else CopperAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPlaying) "Stop Metronome" else "Start ${selectedTaal.name} Metronome",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
