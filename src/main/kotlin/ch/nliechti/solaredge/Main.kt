package ch.nliechti.solaredge

import ch.nliechti.solaredge.powerDetails.PowerDetailsResponse
import ch.nliechti.solaredge.powerDetails.ShellyDecisionParams
import ch.nliechti.solaredge.services.ShellyService
import ch.nliechti.solaredge.services.ShellyState.OFF
import ch.nliechti.solaredge.services.ShellyState.ON
import ch.nliechti.solaredge.services.UtilService.Companion.get15minInFuture
import ch.nliechti.solaredge.services.UtilService.Companion.getNow
import ch.nliechti.solaredge.services.UtilService.Companion.logWithDate
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking

private var shellyIp: String = ""
private var powerReserve: Int = 0
private var heaterPowerUsage = 5000
private var updateCycleInitial = 10000L
private val siteId = System.getenv("SOLAR_EDGE_SITE_ID")
private val apiKey = System.getenv("SOLAR_EDGE_API_KEY")
private lateinit var shellyService: ShellyService

// 10min
var updateCycleAfterPowerOn = 600000L
var activeUpdateCycle = updateCycleAfterPowerOn

fun main(args: Array<String>) = runBlocking<Unit> {
    shellyIp = System.getenv("SHELLY_IP")
    updateCycleInitial = (System.getenv("UPDATE_CYCLE") ?: "10000").toLong()
    updateCycleAfterPowerOn = (System.getenv("UPDATE_CYCLE_AFTER_POWER_ON") ?: "15000").toLong()
    powerReserve = (System.getenv("AVAILABLE_POWER") ?: "500").toInt()
    heaterPowerUsage = (System.getenv("HEATER_POWER_USAGE") ?: "5000").toInt()
    shellyService = ShellyService(shellyIp)

    activeUpdateCycle = updateCycleInitial

    while (true) {
        getValues()
        Thread.sleep(activeUpdateCycle)
    }
}

private fun getValues() {
    val getString = "https://monitoringapi.solaredge.com/site/$siteId/powerDetails" +
            "?meters=PRODUCTION,SELFCONSUMPTION" +
            "&startTime=${getNow()}&endTime=${get15minInFuture()}" +
            "&api_key=$apiKey"
    val call = getString.httpGet()
            .responseString { result ->
                when (result) {
                    is Result.Success -> {
                        val data = result.get()
                        Klaxon().parse<PowerDetailsResponse>(data)?.let { triggerShelly(it) }
                    }
                    is Result.Failure -> {
                        logWithDate("Call to $getString failed")
                    }
                }
            }
    call.join()
}

fun triggerShelly(powerDetailsResponse: PowerDetailsResponse) {
    val response = triggerShellyIfEnoughPower(parseResponse(powerDetailsResponse))
    response?.let {
        activeUpdateCycle = if(it.shellyState == ON) {
            updateCycleAfterPowerOn
        } else {
            updateCycleInitial
        }
    }
}

fun parseResponse(powerDetailsResponse: PowerDetailsResponse): ShellyDecisionParams {
    return ShellyDecisionParams(
            production = powerDetailsResponse.powerDetails.meters.filter { it.type == "Production" }[0].values[0].value,
            selfConsumption = powerDetailsResponse.powerDetails.meters.filter { it.type == "SelfConsumption" }[0].values[0].value,
            isShellyOn = shellyService.isShellyOn(),
            heaterPowerUsage = heaterPowerUsage,
            powerReserve = powerReserve
    )
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
        shellyService.turnShelly(ON)
        TriggerShellyResponse(ON, spareEnergy)
    } else {
        shellyService.turnShelly(OFF)
        TriggerShellyResponse(OFF, spareEnergy)
    }
}
