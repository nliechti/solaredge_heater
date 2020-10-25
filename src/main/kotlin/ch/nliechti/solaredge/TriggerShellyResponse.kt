package ch.nliechti.solaredge

import ch.nliechti.solaredge.services.ShellyState

data class TriggerShellyResponse(
        val shellyState: ShellyState,
        val spareEnergy: Double
)

