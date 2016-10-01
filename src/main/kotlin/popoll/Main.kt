package popoll

import com.squareup.moshi.Moshi
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import popoll.model.Config
import popoll.model.postbank.Token
import popoll.model.postbank.iban.Iban
import popoll.model.postbank.transaction.Transaction
import popoll.model.server.Error
import popoll.model.server.Result
import popoll.util.ConfigParser
import spark.Spark.get
import spark.Spark.port

val moshi = Moshi.Builder().build()
val client = OkHttpClient().newBuilder().build()

val resultAdapter = moshi.adapter<Result>(Result::class.java)
val errorAdapter = moshi.adapter<Error>(Error::class.java)

val debug = true

fun main(args: Array<String>) {

    val config = ConfigParser.parse() ?: return

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

    println("Got everything we need - starting server")

    port(4244)

    get("/ping", { request, response ->
        "pong"
    })

    get("/check", { request, response ->

        // check parameters

        if (!request.queryParams().contains("fromname")) {
            return@get errorAdapter.toJson(Error("you need to pass a name from who you expect the transaction ( fromname )"))
        }

        val fromName = request.queryParams("fromname")!!

        if (!request.queryParams().contains("amount")) {
            return@get errorAdapter.toJson(Error("you need to pass one amount"))
        }

        val amount = request.queryParams("amount")!!

        if (!request.queryParams().contains("reference")) {
            return@get errorAdapter.toJson(Error("you need to pass a reference"))
        }

        val reference = request.queryParams("reference")!!

        ibanObject.content.forEach {
            println("processing IBAN ${it.iban}")

            val transactionRequest = getBaseRequest("accounts/giro/${it.iban}/transactions", config)
                    .header("x-auth", tokenObject.token)
                    .build()

            val execute = client.newCall(transactionRequest).execute()

            val transactionObject = moshi.adapter<Transaction>(Transaction::class.java).fromJson(execute.body().string())

            transactionObject.content.forEach {

                if (it.reference.paymentName.toUpperCase().equals(fromName.toUpperCase())
                        && it.amount.equals(amount) && findInPurpose(it.purpose, reference)) {

                    return@get resultAdapter.toJson(Result("found"))
                }

                if (debug) {
                    println(" not matching: $it")
                }
            }
        }

        return@get resultAdapter.toJson(Result("notfound"))
    })


}

fun findInPurpose(purpose: Array<String>, reference: String): Boolean {
    return purpose.any { it.toUpperCase().contains(reference.toUpperCase()) }
}

private fun getBaseRequest(path: String, config: Config): Request.Builder {
    return Request.Builder()
            .url("https://hackathon.postbank.de/bank-api/gold/postbankid/$path")
            .header("Accept", "application/json")
            .header("api-key", config.apikey)
            .header("device-signature", config.apikey)
}

