package ch.nliechti.solaredge

data class TriggerShellyResponse(
        val shellyState: ShellyState,
        val newUpdateCycle: Long,
        val spareEnergy: Double
)

