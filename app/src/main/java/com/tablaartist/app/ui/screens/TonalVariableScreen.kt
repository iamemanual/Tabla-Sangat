package com.tablaartist.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tablaartist.app.data.Taal
import com.tablaartist.app.data.seed.TaalSeedData
import com.tablaartist.app.ui.theme.SyahiSurfaceVariant
import com.tablaartist.app.ui.theme.TextMuted
import com.tablaartist.app.ui.theme.TextPrimary
import androidx.navigation.NavHostController

@Composable
fun TonalVariableScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val taals = TaalSeedData.taals
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Taal Variable",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Select a taal to view its compositions: Theka, Kayda, Rela, Tukda, Chakkardar Tihayi, Uthaan, Peshkaar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(taals, key = { it.id }) { taal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable(onClick = {
                            navController.navigate("variable_detail/${taal.id}")
                        }),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = SyahiSurfaceVariant
                    )
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${taal.name} (${taal.nameHi})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${taal.matras} matras",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "\u25B8",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
