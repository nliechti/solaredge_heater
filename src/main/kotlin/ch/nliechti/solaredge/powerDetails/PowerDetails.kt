package ch.nliechti.solaredge.powerDetails


data class PowerDetails(
    val timeUnit: String,
    val unit: String,
    val meters: List<Meters>
)