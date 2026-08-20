package com.cluecompanion

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class CreatedGame(val code: String, val playerId: String)

object GameApi {
 fun createGame(name: String): CreatedGame {
  val connection = (URL(BuildConfig.BASE_URL + "api/games").openConnection() as HttpURLConnection).apply {
   requestMethod = "POST"
   connectTimeout = 10_000
   readTimeout = 10_000
   doOutput = true
   setRequestProperty("Content-Type", "application/json; charset=utf-8")
   setRequestProperty("Accept", "application/json")
  }

  try {
   connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
    it.write(JSONObject().put("name", name.trim()).toString())
   }

   val responseBody = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
    ?.bufferedReader()
    ?.use { it.readText() }
    .orEmpty()

   if (connection.responseCode !in 200..299) {
    val message = runCatching { JSONObject(responseBody).optString("error") }.getOrNull()
     ?.takeIf { it.isNotBlank() }
     ?: "The server returned HTTP ${connection.responseCode}"
    error(message)
   }

   val response = JSONObject(responseBody)
   CreatedGame(
    code = response.getString("code"),
    playerId = response.getJSONArray("players").getJSONObject(0).getString("id")
   )
  } finally {
   connection.disconnect()
  }
 }
}
