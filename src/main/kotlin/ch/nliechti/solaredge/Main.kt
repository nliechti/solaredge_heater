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

fun main(args: Array<String>) = runBlocking<Unit> {
    val apiKey = System.getenv("SOLAR_EDGE_API_KEY") ?: "DQRY5QQJEJU4B9IJ02RLYQZBX99AOI7I"
    val siteId = System.getenv("SOLAR_EDGE_SITE_ID") ?: "1570357"
    shellyIp = System.getenv("SHELLY_IP") ?: "10.1.1.46"

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
                        val ex = result.getException()
                        println(ex)
                    }
                }
            }
        Thread.sleep(10000)
    }
}


fun triggerShellyIfEnoughPower(powerDetailsResponse: PowerDetailsResponse) {
    val production = powerDetailsResponse.powerDetails.meters.filter { it.type == "Production" }[0].values[0].value
    val selfConsumption = powerDetailsResponse.powerDetails.meters.filter { it.type == "SelfConsumption" }[0].values[0].value
    if (null == production || null == selfConsumption)
        return
    println("Production: $production")
    println("selfConsumption: $selfConsumption")
    println("Power available: ${(production - selfConsumption)}")

    if ((production - selfConsumption) > 3000) {
        turnShelly(ON)
    } else {
        turnShelly(OFF)
    }
}

fun turnShelly(shellyState: ShellyState) {
    println("Turn shelly $shellyState")
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
