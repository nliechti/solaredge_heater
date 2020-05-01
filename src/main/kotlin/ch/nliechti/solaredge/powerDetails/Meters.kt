package ch.nliechti.solaredge.powerDetails


data class Meters(
    val type: String,
    val values: List<Values>
)