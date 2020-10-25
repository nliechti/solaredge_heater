package ch.nliechti.solaredge.services

import ch.nliechti.solaredge.ShellyResponse
import ch.nliechti.solaredge.logWithDate
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

class ShellyService(private val shellyIp: String) {

     fun isShellyOn(): Boolean {
        var shellyState = false
        val call = "http://${shellyIp}/relay/0".httpGet().responseString { shellyResponse ->
            when (shellyResponse) {
                is Result.Success -> {
                    Klaxon().parse<ShellyResponse>(shellyResponse.get())?.let { shellyState = it.isOn }
                }
            }
        }
        call.join()
        return shellyState
    }

    fun turnShelly(shellyState: ShellyState) {
        logWithDate("Turn shelly $shellyState")
        "http://${shellyIp}/relay/0?turn=${shellyState.state}".httpGet().response()
    }

}

enum class ShellyState constructor(val state: String) {
    ON("on"), OFF("off")
}