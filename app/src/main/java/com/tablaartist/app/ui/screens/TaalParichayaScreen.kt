package com.tablaartist.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tablaartist.app.data.Taal
import com.tablaartist.app.ui.components.TaalParichayaGrid
import com.tablaartist.app.ui.theme.SyahiSurfaceVariant
import com.tablaartist.app.ui.theme.TextMuted
import com.tablaartist.app.ui.theme.TextPrimary

@Composable
fun TaalParichayaScreen(
    taal: Taal,
    navController: NavHostController? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        if (navController != null) {
            Row(
                modifier = Modifier
                    .clickable { navController.popBackStack() }
                    .padding(bottom = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "\u2190 Back to Taals",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "${taal.name} (${taal.nameHi})",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${taal.matras} matras \u2022 Vibhag: ${taal.vibhagSplit.joinToString(" + ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Markers summary
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryChip("x", "Sam", MaterialTheme.colorScheme.primary)
            SummaryChip("2", "Taali 1", MaterialTheme.colorScheme.primary)
            SummaryChip("3", "Taali 2", MaterialTheme.colorScheme.primary)
            SummaryChip("0", "Khali", MaterialTheme.colorScheme.primary)
        }

        Text(
            text = "Theka",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = SyahiSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            TaalParichayaGrid(
                taal = taal,
                modifier = Modifier.padding(16.dp),
                cellSize = 56.dp,
                rowHeight = 100.dp
            )
        }
    }
}

@Composable
private fun SummaryChip(
    marker: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.15f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = androidx.compose.ui.Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$marker \u2022 $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
