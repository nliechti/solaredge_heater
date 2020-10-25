package ch.nliechti.solaredge

data class ShellyResponse(
    val has_timer: Boolean,
    val isOn: Boolean,
    val timer_remaining: Int
)