package com.cluecompanion

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class CreatedGame(val code: String, val playerId: String)

object GameApi {
 fun createGame(name: String): CreatedGame = post("api/games", name)

 fun joinGame(code: String, name: String): CreatedGame = post("api/games/${code.uppercase()}/players", name)

 private fun post(path: String, name: String): CreatedGame {
  val connection = (URL(BuildConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
   requestMethod = "POST"
   connectTimeout = 10_000
   readTimeout = 10_000
   doOutput = true
   setRequestProperty("Content-Type", "application/json; charset=utf-8")
   setRequestProperty("Accept", "application/json")
  }

  return try {
   connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
    it.write(JSONObject().put("name", name.trim()).toString())
   }

   val status = connection.responseCode
   val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
    ?.bufferedReader()
    ?.use { it.readText() }
    .orEmpty()

   if (status !in 200..299) {
    val message = runCatching { JSONObject(responseBody).optString("error") }.getOrNull()
     ?.takeIf { it.isNotBlank() }
     ?: "The server returned HTTP $status"
    error(message)
   }

   val response = JSONObject(responseBody)
   val players = response.getJSONArray("players")
   val player = (0 until players.length())
    .map { players.getJSONObject(it) }
    .first { it.getString("name").equals(name.trim(), ignoreCase = true) }
   CreatedGame(response.getString("code"), player.getString("id"))
  } finally {
   connection.disconnect()
  }
 }
}
