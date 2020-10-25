package ch.nliechti.solaredge.powerDetails

data class ShellyDecisionParams(
        val production: Double?,
        val selfConsumption: Double?,
        val isShellyOn: Boolean,
        val heaterPowerUsage: Int,
        val powerReserve: Int
)