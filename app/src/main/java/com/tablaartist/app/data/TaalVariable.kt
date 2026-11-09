package com.tablaartist.app.data

enum class VariableType(
    val title: String,
    val titleHi: String,
    val description: String
) {
    Theka("Theka", "थेका", "The base repeating pattern of the taal."),
    Kayda("Kayda", "कायदा", "A structured composition with theme-and-variation (paltas)."),
    Rela("Rela", "रेला", "Fast, intricate composition emphasizing speed and clarity."),
    Tukda1("Tukda 1", "टुकड़ा 1", "Short concluding phrase, first variation."),
    Tukda2("Tukda 2", "टुकड़ा 2", "Short concluding phrase, second variation."),
    Tukda3("Tukda 3", "टुकड़ा 3", "Short concluding phrase, third variation."),
    ChakkardarTihayi("Chakkardar Tihayi", "चक्करदार तिहाई", "Three-fold rhythmic cadence with acceleration."),
    Uthaan("Uthaan", "उठान", "Opening flourish that leads into the theka."),
    Peshkaar("Peshkaar", "पेशकार", "Expansive, exploratory introduction of the taal.");

    companion object {
        fun valuesForTaals(): List<VariableType> = values().toList()
    }
}

data class TaalVariable(
    val taalId: String,
    val type: VariableType,
    val bols: String,
    val bolsHi: String,
    val audioResId: Int? = null,
    val matras: Int
)
