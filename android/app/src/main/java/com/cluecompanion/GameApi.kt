package com.cluecompanion

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class GamePlayer(
 val id: String,
 val name: String,
 val isHost: Boolean,
)

data class GameSession(
 val code: String,
 val playerId: String,
 val status: String,
 val players: List<GamePlayer>,
)

object GameApi {
 fun createGame(name: String): GameSession = request("api/games", "POST", name = name)

 fun joinGame(code: String, name: String): GameSession =
  request("api/games/${code.uppercase()}/players", "POST", name = name)

 fun getGame(session: GameSession): GameSession =
  request("api/games/${session.code}", "GET", playerId = session.playerId)

 fun startGame(session: GameSession): GameSession =
  request("api/games/${session.code}/start", "POST", playerId = session.playerId)

 private fun request(
  path: String,
  method: String,
  name: String? = null,
  playerId: String? = null,
 ): GameSession {
  val connection = (URL(BuildConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
   requestMethod = method
   connectTimeout = 10_000
   readTimeout = 10_000
   setRequestProperty("Accept", "application/json")
   playerId?.let { setRequestProperty("X-Player-Id", it) }
   if (name != null) {
    doOutput = true
    setRequestProperty("Content-Type", "application/json; charset=utf-8")
   }
  }

  return try {
   name?.let { playerName ->
    connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
     it.write(JSONObject().put("name", playerName.trim()).toString())
    }
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

   parse(responseBody, playerId, name)
  } finally {
   connection.disconnect()
  }
 }

 private fun parse(responseBody: String, knownPlayerId: String?, playerName: String?): GameSession {
  val response = JSONObject(responseBody)
  val jsonPlayers = response.getJSONArray("players")
  val players = (0 until jsonPlayers.length()).map { index ->
   jsonPlayers.getJSONObject(index).let {
    GamePlayer(it.getString("id"), it.getString("name"), it.getBoolean("host"))
   }
  }
  val playerId = knownPlayerId ?: players.first {
   it.name.equals(playerName?.trim(), ignoreCase = true)
  }.id
  return GameSession(response.getString("code"), playerId, response.getString("status"), players)
 }
}
