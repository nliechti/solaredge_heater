package ch.nliechti.solaredge.services

import ch.nliechti.solaredge.ShellyResponse
import ch.nliechti.solaredge.TriggerShellyResponse
import ch.nliechti.solaredge.powerDetails.ShellyDecisionParams
import ch.nliechti.solaredge.services.UtilService.Companion.logWithDate
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

    fun triggerShellyIfEnoughPower(params: ShellyDecisionParams): TriggerShellyResponse? {
        var production = params.production
        val selfConsumption = params.selfConsumption
        if (null == production || null == selfConsumption) {
            logWithDate("***** Production or SelfConsumption is not set *****")
            Thread.sleep(10000)
            return null
        }
        logWithDate("Production: $production")
        logWithDate("selfConsumption: $selfConsumption")
        logWithDate("Power available: ${(production - selfConsumption)}")


        logWithDate("Shelly isOn: ${params.isShellyOn}")
        if (params.isShellyOn) {
            production -= params.heaterPowerUsage
            logWithDate("Production without heater is $production")
        }

        val spareEnergy = (production - selfConsumption)
        return if (spareEnergy > params.powerReserve) {
            turnShelly(ShellyState.ON)
            TriggerShellyResponse(ShellyState.ON, spareEnergy)
        } else {
            turnShelly(ShellyState.OFF)
            TriggerShellyResponse(ShellyState.OFF, spareEnergy)
        }
    }

}

enum class ShellyState constructor(val state: String) {
    ON("on"), OFF("off")
}