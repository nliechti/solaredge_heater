package ch.nliechti.solaredge.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class UtilService {
    companion object {
        fun get15minInFuture(): String {
            val date = LocalDateTime.now().plus(15, ChronoUnit.MINUTES)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return date.format(formatter).replace(" ", "%20")
        }

        fun getNow(): String {
            val date = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return date.format(formatter).replace(" ", "%20")
        }

        fun logWithDate(message: String) {
            val date = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            println(date.format(formatter) + ": " + message)
        }
    }
}