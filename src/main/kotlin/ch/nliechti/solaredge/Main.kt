package ch.nliechti.solaredge

import ch.nliechti.solaredge.ShellyState.OFF
import ch.nliechti.solaredge.ShellyState.ON
import ch.nliechti.solaredge.powerDetails.PowerDetailsResponse
import com.beust.klaxon.Klaxon
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.MINUTES

var shellyIp: String = ""
var availablePower: Long = 3000
var updateCycleInitial = 10000L

// 10min
var updateCycleAfterPowerOn = 600000L
var activeUpdateCycle = updateCycleAfterPowerOn

fun main(args: Array<String>) = runBlocking<Unit> {
    val apiKey = System.getenv("SOLAR_EDGE_API_KEY")
    val siteId = System.getenv("SOLAR_EDGE_SITE_ID")
    shellyIp = System.getenv("SHELLY_IP")
    updateCycleInitial = System.getenv("UPDATE_CYCLE").toLong()
    updateCycleAfterPowerOn = System.getenv("UPDATE_CYCLE_AFTER_POWER_ON").toLong()
    availablePower = System.getenv("AVAILABLE_POWER").toLong()

    activeUpdateCycle = updateCycleInitial

    while (true) {
        val getString = "https://monitoringapi.solaredge.com/site/$siteId/powerDetails" +
            "?meters=PRODUCTION,SELFCONSUMPTION" +
            "&startTime=${getNow()}&endTime=${get15minInFuture()}" +
            "&api_key=$apiKey"
        getString.httpGet()
            .responseString { result ->
                when (result) {
                    is Result.Success -> {
                        val data = result.get()
                        Klaxon().parse<PowerDetailsResponse>(data)?.let { triggerShellyIfEnoughPower(it) }
                    }
                    is Result.Failure -> {
                        logWithDate("Call to $getString failed")
                    }
                }
            }
        Thread.sleep(activeUpdateCycle)
    }
}


fun triggerShellyIfEnoughPower(powerDetailsResponse: PowerDetailsResponse) {
    val production = powerDetailsResponse.powerDetails.meters.filter { it.type == "Production" }[0].values[0].value
    val selfConsumption = powerDetailsResponse.powerDetails.meters.filter { it.type == "SelfConsumption" }[0].values[0].value
    if (null == production || null == selfConsumption) {
        logWithDate("***** Production or SelfConsumption is not set *****")
        triggerShellyIfEnoughPower(powerDetailsResponse)
        return
    }
    logWithDate("Production: $production")
    logWithDate("selfConsumption: $selfConsumption")
    logWithDate("Power available: ${(production - selfConsumption)}")

    activeUpdateCycle = if ((production - selfConsumption) > availablePower) {
        turnShelly(ON)
        updateCycleAfterPowerOn
    } else {
        turnShelly(OFF)
        updateCycleInitial
    }
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
