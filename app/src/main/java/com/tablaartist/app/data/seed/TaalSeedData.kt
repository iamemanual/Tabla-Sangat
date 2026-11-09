package com.tablaartist.app.data.seed

import com.tablaartist.app.data.Taal

object TaalSeedData {
    val taals: List<Taal> = listOf(
        Taal(
            id = "teentaal",
            name = "Teentaal",
            nameHi = "तीनताल",
            matras = 16,
            vibhagSplit = listOf(4, 4, 4, 4),
            markers = listOf("x", "2", "0", "3"),
            thekaBols = listOf("Dha", "Dhin", "Dhin", "Dha", "Dha", "Dhin", "Dhin", "Dha", "Na", "Tin", "Tin", "Na", "TeTe", "Dhin", "Dhin", "Dha"),
            thekaBolsHi = listOf("धा", "धिन", "धिन", "धा", "धा", "धिन", "धिन", "धा", "ना", "तिन", "तिन", "ना", "तेटे", "धिन", "धिन", "धा")
        ),
        Taal(
            id = "jhaptal",
            name = "Jhaptal",
            nameHi = "झपताल",
            matras = 10,
            vibhagSplit = listOf(2, 3, 2, 3),
            markers = listOf("x", "2", "0", "3"),
            thekaBols = listOf("Dhi", "Na", "Dhi", "Dhi", "Na", "Ti", "Na", "Dhi", "Dhi", "Na"),
            thekaBolsHi = listOf("धी", "ना", "धी", "धी", "ना", "ति", "ना", "धी", "धी", "ना")
        ),
        Taal(
            id = "rupak",
            name = "Rupak",
            nameHi = "रूपक",
            matras = 7,
            vibhagSplit = listOf(3, 2, 2),
            markers = listOf("0", "2", "3"),
            thekaBols = listOf("Tin", "Tin", "Na", "Dhi", "Na", "Dhi", "Na"),
            thekaBolsHi = listOf("तिन", "तिन", "ना", "धी", "ना", "धी", "ना")
        ),
        Taal(
            id = "ektaal",
            name = "Ektaal",
            nameHi = "एकताल",
            matras = 12,
            vibhagSplit = listOf(2, 2, 2, 2, 2, 2),
            markers = listOf("x", "2", "0", "2", "0", "2"),
            thekaBols = listOf("Dhin", "Dhin", "DhaGe", "Tirakita", "Dhin", "Dhin", "DhaGe", "Tirakita", "Dhin", "Na", "Dhin", "Dha"),
            thekaBolsHi = listOf("धिन", "धिन", "धागे", "तिरकिटा", "धिन", "धिन", "धागे", "तिरकिटा", "धिन", "ना", "धिन", "धा")
        ),
        Taal(
            id = "keherwa",
            name = "Keherwa",
            nameHi = "केहरवा",
            matras = 8,
            vibhagSplit = listOf(4, 4),
            markers = listOf("x", "0"),
            thekaBols = listOf("Dha", "Ge", "Na", "Ti", "Na", "Ka", "Dhi", "Na"),
            thekaBolsHi = listOf("धा", "गे", "ना", "ति", "ना", "का", "धी", "ना")
        ),
        Taal(
            id = "dadra",
            name = "Dadra",
            nameHi = "दादरा",
            matras = 6,
            vibhagSplit = listOf(3, 3),
            markers = listOf("x", "0"),
            thekaBols = listOf("Dha", "Dhi", "Na", "Dha", "Tu", "Na"),
            thekaBolsHi = listOf("धा", "धी", "ना", "धा", "तु", "ना")
        ),
        Taal(
            id = "pancham_savari",
            name = "Pancham Savari",
            nameHi = "पंचम सवारी",
            matras = 14,
            vibhagSplit = listOf(4, 4, 3, 3),
            markers = listOf("x", "2", "0", "3"),
            thekaBols = listOf("Dha", "Dhin", "Dhin", "Dha", "Dha", "Dhin", "Dha", "Tin", "Na", "Dhin", "Dhin", "Dha", "Tin", "Na"),
            thekaBolsHi = listOf("धा", "धिन", "धिन", "धा", "धा", "धिन", "धा", "तिन", "ना", "धिन", "धिन", "धा", "तिन", "ना")
        ),
        Taal(
            id = "deepchandi",
            name = "Deepchandi",
            nameHi = "दीपचन्दी",
            matras = 14,
            vibhagSplit = listOf(3, 4, 3, 4),
            markers = listOf("x", "2", "0", "3"),
            thekaBols = listOf("Dha", "Dhin", "Dha", "Dha", "Dhin", "Dhin", "Dha", "Dha", "Tin", "Na", "Dhin", "Dhin", "Dha", "Tin"),
            thekaBolsHi = listOf("धा", "धिन", "धा", "धा", "धिन", "धिन", "धा", "धा", "तिन", "ना", "धिन", "धिन", "धा", "तिन")
        )
    )
}
