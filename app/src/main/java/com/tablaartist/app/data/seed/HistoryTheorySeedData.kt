package com.tablaartist.app.data.seed

import com.tablaartist.app.data.Gharana
import com.tablaartist.app.data.Player
import com.tablaartist.app.data.TheoryTopic

object HistorySeedData {
    const val historyOfTabla = """The origin of the tabla is steeped in the rich confluence of ancient Indian temple percussion and Mughal court music. 

Ancient Indian sculptures dating back to the 2nd century BCE at Bhaja Caves and 6th century at Badami depict dual barrel drums played vertically. However, popular historical tradition attributes the modern bifurcation of the two-headed Pakhawaj (or Mridangam) into two separate drums to the 13th-century polymath and Sufi poet-musician Hazrat Amir Khusru.

In the early 18th century in Delhi, Ustad Siddhar Khan Dhadi revolutionized Indian percussion by modifying the instruments, creating the distinctive layered black iron-paste syahi (tuning spot) on goat skin, and establishing the foundational two-finger playing technique of the Delhi Gharana. 

From Delhi, the tradition branched across Northern India into six legendary schools of rhythm (Gharanas): Delhi, Ajrara, Lucknow, Farrukhabad, Benares, and Punjab."""

    val players: List<Player> = listOf(
        Player(
            id = "siddhar",
            name = "Ustad Siddhar Khan Dhadi",
            dates = "Early 18th Century",
            gharana = "Delhi",
            biography = "Father of the modern tabla and founder of the Delhi Gharana. He codified the initial bols (Dha, Dhin, Ge, Na, Tin) and established the two-finger (do ungli) technique."
        ),
        Player(
            id = "ahmadjan",
            name = "Pandit Ahmad Jan Thirakwa",
            dates = "1892–1976",
            gharana = "Farrukhabad",
            biography = "Revered as the 'Emperor of Tabla' (Aftab-e-Mousiqui). Known for his matchless clarity, poetic peshkaar, and expansive mastery across all gharanas."
        ),
        Player(
            id = "allarakha",
            name = "Ustad Alla Rakha",
            dates = "1919–2000",
            gharana = "Punjab",
            biography = "Legendary Punjab gharana doyen. He popularized the intricate mathematical cross-rhythms (chand) and took Indian percussion to global prominence alongside Pandit Ravi Shankar."
        ),
        Player(
            id = "kishan",
            name = "Pandit Kishan Maharaj",
            dates = "1923–2007",
            gharana = "Benares",
            biography = "Benares gharana giant known for his immense power, majestic open-hand (khula haath) bols, and incomparable stage charisma in solo and accompaniment."
        ),
        Player(
            id = "samtaprasad",
            name = "Pandit Samta Prasad (Gudai Maharaj)",
            dates = "1921–1994",
            gharana = "Benares",
            biography = "Benares maestro celebrated for his lightning-fast clarity, divine bayan resonance, and soulful, intricate tihayis."
        ),
        Player(
            id = "zakir",
            name = "Ustad Zakir Hussain",
            dates = "1951–Present",
            gharana = "Punjab",
            biography = "Grammy Award-winning global phenomenon and son of Ustad Alla Rakha. Elevated the tabla to an internationally acclaimed solo concert instrument through unparalleled speed, tone, and innovative cross-genre collaborations."
        )
    )

    val gharanas: List<Gharana> = listOf(
        Gharana(
            id = "delhi",
            name = "Delhi Gharana",
            founder = "Ustad Siddhar Khan",
            players = listOf("Ustad Siddhar Khan", "Ustad Gamay Khan", "Ustad Inam Ali Khan", "Pandit Chaturlal")
        ),
        Gharana(
            id = "ajrara",
            name = "Ajrara Gharana",
            founder = "Kallu Khan & Miru Khan",
            players = listOf("Ustad Habibuddin Khan", "Ustad Niazu Khan", "Ustad Hashmat Ali Khan")
        ),
        Gharana(
            id = "lucknow",
            name = "Lucknow Gharana",
            founder = "Mian Bakshu & Modu Khan",
            players = listOf("Ustad Afaq Hussain Khan", "Pandit Swapan Chaudhuri", "Ustad Ilmas Hussain Khan")
        ),
        Gharana(
            id = "farrukhabad",
            name = "Farrukhabad Gharana",
            founder = "Ustad Haji Vilayat Ali Khan",
            players = listOf("Pandit Ahmad Jan Thirakwa", "Pandit Jnan Prakash Ghosh", "Pandit Anindo Chatterjee", "Pandit Subhankar Banerjee")
        ),
        Gharana(
            id = "banaras",
            name = "Benares Gharana",
            founder = "Pandit Ram Sahai",
            players = listOf("Pandit Kishan Maharaj", "Pandit Samta Prasad", "Pandit Kumar Bose", "Pandit Sharda Sahai")
        ),
        Gharana(
            id = "punjab",
            name = "Punjab Gharana",
            founder = "Lala Bhavanidas & Mian Qadir Baksh",
            players = listOf("Ustad Alla Rakha", "Ustad Zakir Hussain", "Ustad Tari Khan", "Pandit Yogesh Samsi")
        )
    )
}

object TheorySeedData {
    val topics: List<TheoryTopic> = listOf(
        TheoryTopic(
            id = "pranas",
            title = "The 10 Pranas of Taal (तास के दस प्राण)",
            body = """The ancient Natyashastra and Sangeet Ratnakara treatises define the Ten Lifelines (Das Pranas) that govern Indian rhythm:

1. Kaal (Time): Duration of time units from minute micro-moments (Kshana, Nimisha) to seconds and minutes.
2. Marg (Path): The progression of beats, traditionally categorized into Dhruva, Chitra, Vartika, and Dakshina.
3. Kriya (Action): Physical execution of rhythm—Nishabd (silent wave/Khali) and Sashabd (voiced strike/Taali).
4. Ang (Limb/Subdivision): Metrical components that constitute a rhythmic cycle (Anudrutam, Drutam, Laghu, Guru, Pluta, Kakapadam).
5. Graha (Starting Point): Where the musical phrase begins relative to Sam:
   • Sama Graha (begins exactly on Sam)
   • Ateeta Graha (begins just after Sam)
   • Anagata Graha (begins before Sam)
   • Vishama Graha (begins off-beat)
6. Jati (Metric Class): Classification based on syllable subdivision: Tisra (3), Chatusra (4), Khanda (5), Mishra (7), Sankirna (9).
7. Kala (Tempo Scaling): Speed multiplication of beats (Ek-guna, Do-guna, Chau-guna, Ath-guna).
8. Laya (Tempo/Flow): The continuous tempo of time—Vilambit (slow), Madhya (medium), Drut (fast), and Ati-Drut (ultra-fast).
9. Yati (Rhythmic Motion): The shape/flow of acceleration or deceleration:
   • Sama Yati (uniform tempo)
   • Srotovaha (expanding from narrow to wide, slow to fast)
   • Gopuchha (cow's tail: tapering from wide to narrow)
   • Damaru (narrow at center, wide at ends)
   • Mridanga (wide at center, narrow at ends)
10. Prastar (Permutation): Systematic mathematical variation and permutation of rhythmic phrases (Paltas)."""
        ),
        TheoryTopic(
            id = "jatiyan",
            title = "Jatiyan of Taal (तालों की जातियाँ)",
            body = """In Indian classical rhythm, Jati refers to the fundamental subdivision of a single beat (matra) into micro-pulses:

1. Tisra Jati (3 units / Takita):
   Subdivision of 3 pulses per beat. Creates rolling swing rhythms (Dadra, Khemta).
2. Chatusra Jati (4 units / Takadhimi):
   Subdivision of 4 pulses per beat. The standard framework of North Indian classical taals (Teentaal, Keherwa).
3. Khanda Jati (5 units / Takatkit):
   Subdivision of 5 pulses per beat (2 + 3). Highly syncopated and energetic.
4. Mishra Jati (7 units / Takadhimita kita):
   Subdivision of 7 pulses per beat (4 + 3 or 3 + 4). Complex classical gait (Rupak, Deepchandi).
5. Sankirna Jati (9 units / Takadhimi takatkit):
   Subdivision of 9 pulses per beat (4 + 5 or 2 + 3 + 4). Rare and expansive virtuoso framework."""
        ),
        TheoryTopic(
            id = "samapadi",
            title = "Types of Taal: Samapadi & Vishamapadi (समपदी और विषमपदी ताल)",
            body = """Hindustani taals are categorized into two structural varieties based on the equality of their vibhags (bars/measures):

1. Samapadi Taal (Equi-divided Rhythms):
Taals whose vibhags are all of identical matra length.
• Teentaal: 16 matras divided into 4 + 4 + 4 + 4 (4 vibhags of 4 beats each).
• Ektaal: 12 matras divided into 2 + 2 + 2 + 2 + 2 + 2 (6 vibhags of 2 beats each).
• Keherwa: 8 matras divided into 4 + 4 (2 vibhags of 4 beats each).
• Dadra: 6 matras divided into 3 + 3 (2 vibhags of 3 beats each).

2. Vishamapadi Taal (Asymmetrically-divided Rhythms):
Taals whose vibhags are of unequal lengths, generating rhythmic tension and unique cadence.
• Jhaptal: 10 matras divided into 2 + 3 + 2 + 3.
• Rupak: 7 matras divided into 3 + 2 + 2 (uniquely starts with Khali on Sam).
• Pancham Savari: 15 matras divided into 4 + 4 + 3 + 4.
• Dhamar: 14 matras divided into 5 + 2 + 3 + 4."""
        ),
        TheoryTopic(
            id = "parts",
            title = "Parts of the Tabla (तबले के अंग)",
            body = """A tabla set consists of two drums: the Dayan (right/treble) and Bayan (left/bass/Dagga):

1. Dayan (Right Drum / Treble):
   • Wood Body (Lakdi): Carved from heavy dense hardwoods (Sheesham, Bijasal, Mahogany).
   • Puri (Drumhead): Goat-skin composite membrane.
   • Syahi (Black Tuning Spot): Concentric circular paste of iron filing, soot, and starch; grants the bell-like harmonic overtones.
   • Chanti / Kinar: Outer skin rim where sharp 'Na / Ta' bols are struck.
   • Maidan / Sur: Clear skin region between Syahi and Chanti producing ringing 'Tin / Tun'.
   • Gajra: Braided leather ring tensioning the Puri.
   • Baddi: Continuous camel/buffalo rawhide strap running vertically.
   • Gatta: Wooden cylindrical pegs wedged beneath Baddi for macro pitch tuning.
   • Kundal: Bottom leather ring holding the straps.

2. Bayan (Left Drum / Bass / Dagga):
   • Shell: Hammered copper, brass, or seasoned clay.
   • Off-center Syahi: Positioned towards the top edge to allow the heel of the palm to modulate pitch ('Ge, Gha, Kat').
   • Bira / Gaddi: Soft cloth ring cushions upon which both drums rest."""
        )
    )

    fun findById(id: String): TheoryTopic? = topics.firstOrNull { it.id == id }
}
