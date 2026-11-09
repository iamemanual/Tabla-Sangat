package com.tablaartist.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tablaartist.app.data.Taal
import com.tablaartist.app.ui.theme.CopperAccent
import com.tablaartist.app.ui.theme.KhaliSky
import com.tablaartist.app.ui.theme.SyahiBorder
import com.tablaartist.app.ui.theme.SyahiSurfaceVariant
import com.tablaartist.app.ui.theme.TaliOrange
import com.tablaartist.app.ui.theme.TextMuted
import com.tablaartist.app.ui.theme.TextPrimary
import com.tablaartist.app.ui.theme.TextSecondary

@Composable
fun TaalParichayaGrid(
    taal: Taal,
    modifier: Modifier = Modifier,
    cellSize: Dp = 56.dp,
    rowHeight: Dp = 100.dp
) {
    val vibhagSplit = taal.vibhagSplit
    val bols = taal.thekaBolsHi

    val rows = mutableListOf<List<Int>>()
    var currentMatra = 0
    for (vibhagSize in vibhagSplit) {
        rows.add((currentMatra until currentMatra + vibhagSize).toList())
        currentMatra += vibhagSize
    }

    val matraMarker = mutableMapOf<Int, String>()
    currentMatra = 0
    for ((idx, vibhagSize) in vibhagSplit.withIndex()) {
        val marker = taal.markers.getOrNull(idx) ?: ""
        matraMarker[currentMatra] = marker
        currentMatra += vibhagSize
    }

    val totalCells = taal.matras
    val columns = rows.maxOfOrNull { it.size } ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawGraphSheet()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { i, matraIdx ->
                    val isLast = i == row.lastIndex
                    MatraCell(
                        matraNumber = (matraIdx + 1).toString(),
                        bol = bols.getOrNull(matraIdx) ?: "",
                        marker = matraMarker[matraIdx] ?: "",
                        isSam = matraIdx == 0,
                        isLastInVibhag = isLast,
                        cellSize = cellSize
                    )
                }
                if (row.lastIndex >= 0) {
                    SlashDivider()
                }
            }
        }
    }
}

private fun DrawScope.drawGraphSheet() {
    val totalWidth = size.width
    val totalHeight = size.height

    // Draw millimeter graph paper lines
    val graphBaseColor = Color(0xFF1E1712)
    val majorGridColor = Color(0xFF4A3628).copy(alpha = 0.35f)
    val minorGridColor = Color(0xFF38291F).copy(alpha = 0.18f)

    drawRect(color = graphBaseColor)

    // Fine 10px minor graph lines
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

    // Major 60px grid lines
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

private fun vibhagRowCount(totalCells: Int, columns: Int): Int {
    return if (columns == 0) 0 else Math.ceil(totalCells.toDouble() / columns).toInt()
}

@Composable
private fun SlashDivider() {
    Text(
        text = "/",
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun MatraCell(
    matraNumber: String,
    bol: String,
    marker: String,
    isSam: Boolean,
    isLastInVibhag: Boolean,
    cellSize: Dp
) {
    val markerColor = when {
        isSam -> CopperAccent
        marker == "0" -> KhaliSky
        marker.isNotEmpty() -> TaliOrange
        else -> TextMuted
    }

    val borderWidth = if (isLastInVibhag) 2.dp else 1.dp
    val borderColor = if (isLastInVibhag) CopperAccent else SyahiBorder.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .size(cellSize)
            .border(borderWidth, borderColor, shape = RectangleShape)
            .background(SyahiSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (marker.isNotEmpty()) {
                Text(
                    text = marker,
                    style = MaterialTheme.typography.labelMedium,
                    color = markerColor,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, top = 2.dp)
                )
            }

            Text(
                text = bol,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = matraNumber,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
