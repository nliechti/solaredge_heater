package ch.nliechti.solaredge.powerDetails

data class SolarEdgeParsedResponse(
        val production: Double?,
        val selfConsumption: Double?,
        val isShellyOn: Boolean
)