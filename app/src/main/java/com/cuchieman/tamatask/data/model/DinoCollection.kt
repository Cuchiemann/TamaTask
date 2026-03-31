package com.cuchieman.tamatask.data.model

import androidx.compose.ui.graphics.Color
import com.cuchieman.tamatask.ui.theme.TamaCyan
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.theme.TamaYellow

data class DinoSpec(
    val id: String,
    val name: String,
    val species: String,
    val era: String,
    val unlocked: Boolean,
    val accentColor: Color,
    val idleStripRes: String? = null,  // drawable resource name, null if no sprites
    val walkStripRes: String? = null,
    val thumbRes: String? = null       // optional dedicated thumbnail drawable
)

object DinoCollection {
    val all: List<DinoSpec> = listOf(
        DinoSpec("spino", "Spike", "Spinosaurus", "Cretacico", unlocked = true, accentColor = TamaOrange, idleStripRes = "spino_idle_strip", walkStripRes = "spino_walk_strip"),
        DinoSpec("stego", "???", "Stegosaurus", "Jurasico", unlocked = false, accentColor = TamaYellow, idleStripRes = "stego_idle_strip", walkStripRes = "stego_walk_strip", thumbRes = "stego_thumb"),
        DinoSpec("raptor", "???", "Velociraptor", "Cretacico", unlocked = false, accentColor = TamaCyan),
        DinoSpec("trike", "???", "Triceratops", "Cretacico", unlocked = false, accentColor = TamaGreen),
        DinoSpec("ptera", "???", "Pteranodon", "Cretacico", unlocked = false, accentColor = TamaPurple),
        DinoSpec("trex", "???", "Tyrannosaurus Rex", "Cretacico", unlocked = false, accentColor = TamaPink),
    )

    /** Sorted: unlocked first, then locked */
    val sorted: List<DinoSpec> = all.sortedByDescending { it.unlocked }
}
