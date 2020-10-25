package ch.nliechti.solaredge

import ch.nliechti.solaredge.ShellyState.OFF
import ch.nliechti.solaredge.ShellyState.ON
import ch.nliechti.solaredge.powerDetails.PowerDetailsResponse
import ch.nliechti.solaredge.powerDetails.SolarEdgeParsedResponse
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.MINUTES

var shellyIp: String = ""
var powerReserve: Int = 0
var heaterPowerUsage = 5000
var updateCycleInitial = 10000L
val siteId = System.getenv("SOLAR_EDGE_SITE_ID")
val apiKey = System.getenv("SOLAR_EDGE_API_KEY")

// 10min
var updateCycleAfterPowerOn = 600000L
var activeUpdateCycle = updateCycleAfterPowerOn

fun main(args: Array<String>) = runBlocking<Unit> {
    shellyIp = System.getenv("SHELLY_IP")
    updateCycleInitial = (System.getenv("UPDATE_CYCLE") ?: "10000").toLong()
    updateCycleAfterPowerOn = (System.getenv("UPDATE_CYCLE_AFTER_POWER_ON") ?: "15000").toLong()
    powerReserve = (System.getenv("AVAILABLE_POWER") ?: "500").toInt()
    heaterPowerUsage = (System.getenv("HEATER_POWER_USAGE") ?: "5000").toInt()

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
        activeUpdateCycle = it.newUpdateCycle
    }
}

fun parseResponse(powerDetailsResponse: PowerDetailsResponse): SolarEdgeParsedResponse {
    return SolarEdgeParsedResponse(
            production = powerDetailsResponse.powerDetails.meters.filter { it.type == "Production" }[0].values[0].value,
            selfConsumption = powerDetailsResponse.powerDetails.meters.filter { it.type == "SelfConsumption" }[0].values[0].value,
            isShellyOn = isShellyOn()
    )
}

fun triggerShellyIfEnoughPower(solarEdgeResponse: SolarEdgeParsedResponse): TriggerShellyResponse? {
    var production = solarEdgeResponse.production
    val selfConsumption = solarEdgeResponse.selfConsumption
    if (null == production || null == selfConsumption) {
        logWithDate("***** Production or SelfConsumption is not set *****")
        Thread.sleep(10000)
        return null
    }
    logWithDate("Production: $production")
    logWithDate("selfConsumption: $selfConsumption")
    logWithDate("Power available: ${(production - selfConsumption)}")

    val isShellyOn = isShellyOn()
    logWithDate("Shelly isOn: $isShellyOn")
    if (isShellyOn) {
        production -= heaterPowerUsage
        logWithDate("Production without heater is $production")
    }

    val spareEnergy = (production - selfConsumption)
    return if (spareEnergy > powerReserve) {
        turnShelly(ON)
        TriggerShellyResponse(ON, updateCycleAfterPowerOn, spareEnergy)
    } else {
        turnShelly(OFF)
        TriggerShellyResponse(OFF, updateCycleInitial, spareEnergy)
    }
}

fun isShellyOn(): Boolean {
    var shellyState = false
    val call = "http://$shellyIp/relay/0".httpGet().responseString { shellyResponse ->
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
    "http://$shellyIp/relay/0?turn=${shellyState.state}".httpGet().response()
}

fun get15minInFuture(): String {
    val date = LocalDateTime.now().plus(15, MINUTES)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return date.format(formatter).replace(" ", "%20")
}

fun getNow(): String {
    val date = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return date.format(formatter).replace(" ", "%20")
}

enum class ShellyState constructor(val state: String) {
    ON("on"), OFF("off")
}

fun logWithDate(message: String) {
    val date = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    println(date.format(formatter) + ": " + message)
}
