package popoll.util

import okio.Okio
import popoll.model.Config
import popoll.moshi
import java.io.File

object ConfigParser {

    fun parse(): Config? {
        val configFile = File("config.json")

        if (!configFile.exists()) {
            println("please add a config.json - you can find a template at config.json.template")
        } else {
            val configBuffer = Okio.buffer(Okio.source(configFile))

            try {
                val config = moshi.adapter<Config>(Config::class.java).fromJson(configBuffer)
                if (config.username == null) {
                    println("please specify a username in the config.json")
                    return null
                }

                if (config.password == null) {
                    println("please specify a password in the config.json")
                    return null
                }

                if (config.apikey == null) {
                    println("please specify a apikey in the config.json")
                    return null
                }

                return config // YAY we got a valid config

            } catch (e: Exception) {
                println("Invalid config file - could not parse it")
            }

        }
        return null
    }

}