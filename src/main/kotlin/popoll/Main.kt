package popoll

import com.squareup.moshi.Moshi
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import popoll.model.Config
import popoll.model.postbank.Token
import popoll.model.postbank.iban.Iban
import popoll.model.postbank.transaction.Transaction
import popoll.util.ConfigParser

val moshi = Moshi.Builder().build()

fun main(args: Array<String>) {

    val config = ConfigParser.parse() ?: return

    val client = OkHttpClient().newBuilder().build()

    val tokenRequest = getBaseRequest("token?username=${config.username}&password=${config.password}", config)
            .post(FormBody.Builder().build())
            .build()

    val tokenResponse = client.newCall(tokenRequest).execute()

    val tokenObject = moshi.adapter<Token>(Token::class.java).fromJson(tokenResponse.body().string())

    if (tokenObject.token == null) {
        println("Got no token - please check your config.json")
        return
    }

    val ibanRequest = getBaseRequest("accounts/giro", config)
            .header("x-auth", tokenObject.token)
            .build()

    val ibanResponse = client.newCall(ibanRequest).execute()

    val ibanObject = moshi.adapter<Iban>(Iban::class.java).fromJson(ibanResponse.body().string())

    ibanObject.content.forEach {
        println("processing IBAN ${it.iban}")

        val transactionRequest = getBaseRequest("accounts/giro/${it.iban}/transactions", config)
                .header("x-auth", tokenObject.token)
                .build()

        val execute = client.newCall(transactionRequest).execute()

        val transactionObject = moshi.adapter<Transaction>(Transaction::class.java).fromJson(execute.body().string())

        transactionObject.content.forEach {
            println(it)
        }

    }
}

private fun getBaseRequest(path: String, config: Config): Request.Builder {
    return Request.Builder()
            .url("https://hackathon.postbank.de/bank-api/gold/postbankid/$path")
            .header("Accept", "application/json")
            .header("api-key", config.apikey)
            .header("device-signature", config.apikey)
}

