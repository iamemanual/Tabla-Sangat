package com.tablaartist.app.data

import com.tablaartist.app.data.seed.HistorySeedData
import com.tablaartist.app.data.seed.TaalSeedData
import com.tablaartist.app.data.seed.TaalVariableSeedData
import com.tablaartist.app.data.seed.TheorySeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * SupabaseTablaRepository
 * Manages remote data synchronization from Supabase with offline-first local cache fallback.
 */
object SupabaseTablaRepository {
    private const val SUPABASE_URL = "https://eqwgrxcitwoyickthfpd.supabase.co"
    private const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_skaLU__XWX2Nx8lAUcy50A_ImTZpWZG"

    // Sanitized publishable key accessor
    var publishableKey: String = DEFAULT_PUBLISHABLE_KEY
        set(value) {
            if (value.isNotBlank()) {
                field = value.trim()
            }
        }

    /**
     * Fetch Taals from Supabase tabla_taals table, fallback to TaalSeedData.
     */
    suspend fun fetchTaals(): List<Taal> = withContext(Dispatchers.IO) {
        try {
            val response = executeGetRequest("/rest/v1/tabla_taals?select=*")
            if (response != null) {
                val array = JSONArray(response)
                val list = mutableListOf<Taal>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Taal(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            nameHi = obj.optString("name_hi", obj.getString("name")),
                            matras = obj.getInt("matras"),
                            vibhagSplit = jsonArrayToIntList(obj.getJSONArray("vibhag_split")),
                            markers = jsonArrayToStringList(obj.getJSONArray("markers")),
                            thekaBols = jsonArrayToStringList(obj.getJSONArray("theka_bols")),
                            thekaBolsHi = jsonArrayToStringList(obj.optJSONArray("theka_bols_hi") ?: obj.getJSONArray("theka_bols"))
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (_: Exception) {}
        TaalSeedData.taals
    }

    /**
     * Fetch Variables from Supabase tabla_variables table, fallback to TaalVariableSeedData.
     */
    suspend fun fetchVariables(): List<TaalVariable> = withContext(Dispatchers.IO) {
        try {
            val response = executeGetRequest("/rest/v1/tabla_variables?select=*")
            if (response != null) {
                val array = JSONArray(response)
                val list = mutableListOf<TaalVariable>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val typeStr = obj.getString("type")
                    val type = VariableType.values().firstOrNull { it.name.equals(typeStr, ignoreCase = true) } ?: VariableType.Kayda
                    list.add(
                        TaalVariable(
                            taalId = obj.getString("taal_id"),
                            type = type,
                            bols = obj.getString("bol_text"),
                            bolsHi = obj.optString("bol_text_hi", obj.getString("bol_text")),
                            matras = obj.getInt("matras")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (_: Exception) {}
        TaalVariableSeedData.variables
    }

    /**
     * Fetch Players / Maestros from Supabase tabla_players table.
     */
    suspend fun fetchPlayers(): List<Player> = withContext(Dispatchers.IO) {
        try {
            val response = executeGetRequest("/rest/v1/tabla_players?select=*")
            if (response != null) {
                val array = JSONArray(response)
                val list = mutableListOf<Player>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Player(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            dates = obj.optString("dates", ""),
                            gharana = obj.optString("gharana", ""),
                            biography = obj.optString("biography", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (_: Exception) {}
        HistorySeedData.players
    }

    private fun executeGetRequest(endpoint: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$SUPABASE_URL$endpoint")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", publishableKey)
                setRequestProperty("Authorization", "Bearer $publishableKey")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun jsonArrayToIntList(array: JSONArray): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            list.add(array.getInt(i))
        }
        return list
    }

    private fun jsonArrayToStringList(array: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }
}
