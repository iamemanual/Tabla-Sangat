package com.tablaartist.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tablaartist.app.data.Taal
import com.tablaartist.app.data.VariableType
import com.tablaartist.app.data.seed.TaalSeedData
import com.tablaartist.app.data.seed.TaalVariableSeedData
import com.tablaartist.app.ui.theme.CopperAccent
import com.tablaartist.app.ui.theme.KhaliSky
import com.tablaartist.app.ui.theme.SyahiBorder
import com.tablaartist.app.ui.theme.SyahiSurfaceVariant
import com.tablaartist.app.ui.theme.TaliOrange
import com.tablaartist.app.ui.theme.TextMuted
import com.tablaartist.app.ui.theme.TextPrimary
import com.tablaartist.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun VariableContentScreen(
    taalId: String,
    typeOrdinal: Int,
    navController: NavHostController? = null,
    modifier: Modifier = Modifier
) {
    val type = VariableType.values()[typeOrdinal]
    val taal = TaalSeedData.taals.find { it.id == taalId }
    val taalName = taal?.name ?: taalId
    val variable = TaalVariableSeedData.variables
        .find { it.taalId == taalId && it.type == type }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingIndex by remember { mutableIntStateOf(-1) }
    val scrollState = rememberScrollState()

    // Parse bols into vibhag segments
    val vibhagPartsHi = variable?.bolsHi?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    val vibhagPartsEn = variable?.bols?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    // Flatten all matras for sequential playback
    val allMatras = remember(variable) {
        val list = mutableListOf<Pair<String, String>>()
        if (vibhagPartsHi.isNotEmpty()) {
            vibhagPartsHi.forEachIndexed { vIdx, vStrHi ->
                val vStrEn = vibhagPartsEn.getOrNull(vIdx) ?: ""
                val tokensHi = vStrHi.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val tokensEn = vStrEn.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val count = maxOf(tokensHi.size, tokensEn.size, 1)
                for (i in 0 until count) {
                    val hi = tokensHi.getOrNull(i) ?: "-"
                    val en = tokensEn.getOrNull(i) ?: hi
                    list.add(Pair(hi, en))
                }
            }
        }
        list
    }

    LaunchedEffect(isPlaying, allMatras.size) {
        if (isPlaying && allMatras.isNotEmpty()) {
            currentPlayingIndex = 0
            while (isPlaying) {
                delay(500L) // 120 BPM base
                currentPlayingIndex = (currentPlayingIndex + 1) % allMatras.size
            }
        } else {
            currentPlayingIndex = -1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Navigation Bar
        if (navController != null) {
            Row(
                modifier = Modifier
                    .clickable { navController.popBackStack() }
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back to $taalName Compositions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Title and Metadata Chips
        Text(
            text = "$taalName — ${type.title}",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "${type.description} • Authentic Notation on Millimeter Graph Sheet",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        variable?.let { v ->
            // Meta chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaChip(label = "Matras", value = "${v.matras}")
                MetaChip(label = "Type", value = type.title)
                taal?.let { t ->
                    MetaChip(label = "Vibhags", value = t.vibhagSplit.joinToString("/"))
                }
            }

            // Authentic Faded Millimeter Graph Sheet Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind { drawFadedGraphSheet() }
                    .border(1.dp, SyahiBorder.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var globalIdx = 0
                    vibhagPartsHi.forEachIndexed { vIdx, vStrHi ->
                        val vStrEn = vibhagPartsEn.getOrNull(vIdx) ?: ""
                        val tokensHi = vStrHi.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        val tokensEn = vStrEn.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        val maxCount = maxOf(tokensHi.size, tokensEn.size, 1)

                        val markerVal = if (vIdx == 0) "x" else taal?.markers?.getOrNull(vIdx) ?: ""

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (mIdx in 0 until maxCount) {
                                val curGlobal = globalIdx
                                val isCellPlaying = currentPlayingIndex == curGlobal
                                val marker = if (mIdx == 0) markerVal else ""
                                val hi = tokensHi.getOrNull(mIdx) ?: "-"
                                val en = tokensEn.getOrNull(mIdx) ?: hi

                                VariableMatraCell(
                                    matraNumber = (curGlobal + 1).toString(),
                                    bolHi = hi,
                                    bolEn = en,
                                    marker = marker,
                                    isSam = curGlobal == 0,
                                    isActive = isCellPlaying,
                                    cellSize = 56.dp
                                )
                                globalIdx++
                            }

                            Text(
                                text = "/",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Playback Bar
            Button(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color(0xFF8B2500) else CopperAccent
                )
            ) {
                Text(
                    text = if (isPlaying) "⏹ Stop Composition" else "▶ Play Composition",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Shastra & Structure Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SyahiSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Structure & Shastra Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        color = CopperAccent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = getShastraDescription(type, taalName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        } ?: run {
            Card(
                colors = CardDefaults.cardColors(containerColor = SyahiSurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "No ${type.title} composed yet for $taalName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (navController != null) {
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(text = "← Back to List", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(SyahiSurfaceVariant)
            .border(1.dp, SyahiBorder.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

@Composable
private fun VariableMatraCell(
    matraNumber: String,
    bolHi: String,
    bolEn: String,
    marker: String,
    isSam: Boolean,
    isActive: Boolean,
    cellSize: Dp
) {
    val markerColor = when {
        isSam -> CopperAccent
        marker == "0" -> KhaliSky
        marker.isNotEmpty() -> TaliOrange
        else -> TextMuted
    }

    val borderColor = when {
        isActive -> CopperAccent
        isSam -> CopperAccent.copy(alpha = 0.8f)
        else -> SyahiBorder.copy(alpha = 0.35f)
    }

    val backgroundColor = when {
        isActive -> SyahiSurfaceVariant.copy(alpha = 0.95f)
        else -> SyahiSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .border(if (isActive) 2.dp else 1.dp, borderColor, shape = RectangleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = marker,
                fontSize = 9.sp,
                color = markerColor
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bolHi,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = bolEn,
                    fontSize = 8.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Text(
                text = matraNumber,
                fontSize = 8.sp,
                color = TextMuted
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFadedGraphSheet() {
    val totalWidth = size.width
    val totalHeight = size.height

    val graphBaseColor = Color(0xFF171513)
    val majorGridColor = Color(0xFF4A3628).copy(alpha = 0.32f)
    val minorGridColor = Color(0xFF38291F).copy(alpha = 0.16f)

    drawRect(color = graphBaseColor)

    // Minor 12px grid
    val minorStep = 12f
    var gx = 0f
    while (gx <= totalWidth) {
        drawLine(
            color = minorGridColor,
            start = Offset(gx, 0f),
            end = Offset(gx, totalHeight),
            strokeWidth = 0.5f
        )
        gx += minorStep
    }

    var gy = 0f
    while (gy <= totalHeight) {
        drawLine(
            color = minorGridColor,
            start = Offset(0f, gy),
            end = Offset(totalWidth, gy),
            strokeWidth = 0.5f
        )
        gy += minorStep
    }

    // Major 60px grid
    val majorStep = minorStep * 5f
    gx = 0f
    while (gx <= totalWidth) {
        drawLine(
            color = majorGridColor,
            start = Offset(gx, 0f),
            end = Offset(gx, totalHeight),
            strokeWidth = 1f
        )
        gx += majorStep
    }

    gy = 0f
    while (gy <= totalHeight) {
        drawLine(
            color = majorGridColor,
            start = Offset(0f, gy),
            end = Offset(totalWidth, gy),
            strokeWidth = 1f
        )
        gy += majorStep
    }
}

private fun getShastraDescription(type: VariableType, taalName: String): String {
    return when (type) {
        VariableType.Kayda -> "A foundational pedagogical form born in the Delhi Gharana. It introduces a theme (Mukh) and systematic permutations (Palte), balanced strictly across Bhari (resonant) and Khali (muted) cycles, concluding in a triple cadence (Tihai)."
        VariableType.Rela -> "A rapid cascade of fluid, rolling syllables (Tirakita, Dhire Dhire, DhaGeNa) performed at drut (high) speed, showcasing finger articulation and velocity without losing clarity."
        VariableType.Tukda1, VariableType.Tukda2, VariableType.Tukda3 -> "A crisp, fixed structural composition popular in the Benares and Lucknow traditions. Emphasizes bold pakhawaj-derived open strokes (Dha, Kran, GadiGana) resolving into a dramatic three-fold Tihai on Sam."
        VariableType.ChakkardarTihayi -> "A grand triadic structure where a complex phrase containing its own internal cadence is repeated exactly three full times so that the final stroke hits the Sam with mathematical precision."
        VariableType.Uthaan -> "The dramatic opening flourish of a solo performance. It commands audience attention with resonant open hand strokes before settling smoothly into the theka."
        VariableType.Peshkaar -> "A slow, dignified introductory movement in vilambit laya. Features nuanced wrist modulation, bayan meend (pitch glide), and subtle syncopated rhythmic interplay."
        VariableType.Theka -> "The canonical timekeeping metric cycle that anchors the ensemble and defines $taalName."
    }
}
