package com.tablaartist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tablaartist.app.audio.TablaAudioEngine
import com.tablaartist.app.data.SupabaseTablaRepository
import com.tablaartist.app.data.seed.TaalSeedData
import com.tablaartist.app.ui.navigation.TablaDrawerScreen
import com.tablaartist.app.ui.screens.HistoryScreen
import com.tablaartist.app.ui.screens.MetronomeScreen
import com.tablaartist.app.ui.screens.PlayerDetailScreen
import com.tablaartist.app.ui.screens.TaalListScreen
import com.tablaartist.app.ui.screens.TaalParichayaScreen
import com.tablaartist.app.ui.screens.TanpuraScreen
import com.tablaartist.app.ui.screens.TheoryDetailScreen
import com.tablaartist.app.ui.screens.TheoryScreen
import com.tablaartist.app.ui.screens.TonalVariableScreen
import com.tablaartist.app.ui.screens.TunerScreen
import com.tablaartist.app.ui.screens.VariableContentScreen
import com.tablaartist.app.ui.screens.VariableDetailScreen
import com.tablaartist.app.ui.theme.TablaArtistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TablaArtistTheme {
                val navController: NavHostController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                TablaDrawerScreen(
                    navController = navController,
                    drawerState = drawerState
                ) {
                    NavHostRoutes(navController = navController)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Defense-in-depth: Ensure microphone is immediately released when losing focus
        TablaAudioEngine.stopPitchDetection()
    }

    override fun onStop() {
        super.onStop()
        // Guarantee clean resource teardown when moving to background
        TablaAudioEngine.stopPitchDetection()
        TablaAudioEngine.stopTanpura()
    }

    @Composable
    private fun NavHostRoutes(navController: NavHostController) {
        val taalsState = remember { mutableStateOf(TaalSeedData.taals) }
        LaunchedEffect(Unit) {
            taalsState.value = SupabaseTablaRepository.fetchTaals()
        }
        val taals = taalsState.value

        NavHost(
            navController = navController,
            startDestination = "taal_list",
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
            composable("taal_list") {
                TaalListScreen(
                    navController = navController,
                    taals = taals
                )
            }
            composable(
                "taal_parichaya/{taalId}",
                arguments = listOf(
                    navArgument("taalId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taalId = backStackEntry.arguments?.getString("taalId") ?: "teentaal"
                val taal = taals.find { it.id == taalId } ?: taals.first()
                TaalParichayaScreen(taal = taal, navController = navController)
            }
            composable("taal_variable") {
                TonalVariableScreen(navController = navController)
            }
            composable(
                "variable_detail/{taalId}",
                arguments = listOf(
                    navArgument("taalId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taalId = backStackEntry.arguments?.getString("taalId") ?: "teentaal"
                VariableDetailScreen(taalId = taalId, navController = navController)
            }
            composable(
                "variable_content/{taalId}/{typeOrdinal}",
                arguments = listOf(
                    navArgument("taalId") { type = NavType.StringType },
                    navArgument("typeOrdinal") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val taalId = backStackEntry.arguments?.getString("taalId") ?: "teentaal"
                val typeOrdinal = backStackEntry.arguments?.getInt("typeOrdinal") ?: 0
                VariableContentScreen(taalId = taalId, typeOrdinal = typeOrdinal, navController = navController)
            }
            composable("history") {
                HistoryScreen(navController = navController)
            }
            composable(
                "player_detail/{playerId}",
                arguments = listOf(
                    navArgument("playerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playerId = backStackEntry.arguments?.getString("playerId") ?: ""
                PlayerDetailScreen(playerId = playerId, navController = navController)
            }
            composable("theory") {
                TheoryScreen(navController = navController)
            }
            composable(
                "theory_detail/{topicId}",
                arguments = listOf(
                    navArgument("topicId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: "pranas"
                TheoryDetailScreen(topicId = topicId, navController = navController)
            }
            composable("tuner") {
                TunerScreen()
            }
            composable("tanpura") {
                TanpuraScreen()
            }
            composable("metronome") {
                MetronomeScreen()
            }
        }
    }
}
