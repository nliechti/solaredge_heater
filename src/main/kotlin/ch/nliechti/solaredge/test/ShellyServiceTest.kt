package ch.nliechti.solaredge.test

import ch.nliechti.solaredge.powerDetails.ShellyDecisionParams
import ch.nliechti.solaredge.services.ShellyService
import ch.nliechti.solaredge.services.ShellyState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class ShellyServiceTest {

    lateinit var shellyService: ShellyService

    @BeforeEach
    internal fun setUp() {
        shellyService = ShellyService("")
    }

    @Test
    fun turnShellyOnIfEnoughEnergyIsPresent() {
        val params = ShellyDecisionParams(
                production = 6000.0,
                selfConsumption = 1000.0,
                isShellyOn = false,
                heaterPowerUsage = 4000,
                powerReserve = 500
        )

        val result = shellyService.triggerShellyIfEnoughPower(params)
        assertTrue { result!!.shellyState == ShellyState.ON }
    }

    @Test
    fun stayOnIfEnoughEnergyIsPresent() {
        val params = ShellyDecisionParams(
                production = 6000.0,
                selfConsumption = 5000.0,
                isShellyOn = true,
                heaterPowerUsage = 4000,
                powerReserve = 500
        )

        val result = shellyService.triggerShellyIfEnoughPower(params)
        assertTrue { result!!.shellyState == ShellyState.ON }
    }

    @Test
    fun turnShellyOffIfNotEnergyIsPresent() {
        val params = ShellyDecisionParams(
                production = 4000.0,
                selfConsumption = 3600.0,
                isShellyOn = true,
                heaterPowerUsage = 4000,
                powerReserve = 500
        )

        val result = shellyService.triggerShellyIfEnoughPower(params)
        assertTrue { result!!.shellyState == ShellyState.OFF }
    }

    @Test
    fun doNotTurnOnIfNotEnoughFreeEnergyIsPresent() {
        val params = ShellyDecisionParams(
                production = 4000.0,
                selfConsumption = 2000.0,
                isShellyOn = false,
                heaterPowerUsage = 4000,
                powerReserve = 500
        )

        val result = shellyService.triggerShellyIfEnoughPower(params)
        assertTrue { result!!.shellyState == ShellyState.OFF }
    }


}