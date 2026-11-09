package com.tablaartist.app.data

data class Player(
    val id: String,
    val name: String,
    val dates: String,
    val gharana: String,
    val biography: String
)

data class Gharana(
    val id: String,
    val name: String,
    val founder: String,
    val players: List<String>
)

data class TheoryTopic(
    val id: String,
    val title: String,
    val body: String
)
