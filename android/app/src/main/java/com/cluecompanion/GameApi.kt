package com.cluecompanion

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class GamePlayer(
 val id: String,
 val name: String,
 val isHost: Boolean,
 val isEliminated: Boolean,
)

data class GameCard(val type: String, val name: String)

data class GameSession(
 val code: String,
 val playerId: String,
 val status: String,
 val phase: String,
 val players: List<GamePlayer>,
 val currentPlayerId: String?,
 val responderId: String?,
 val winnerId: String?,
 val hand: List<GameCard>,
 val cards: List<GameCard>,
 val suggestedCards: List<GameCard>,
 val solution: List<GameCard>,
 val events: List<String>,
)

object GameApi {
 fun listGames(): List<String> {
  val connection = (URL(BuildConfig.BASE_URL + "api/games").openConnection() as HttpURLConnection).apply {
   requestMethod = "GET"
   connectTimeout = 10_000
   readTimeout = 10_000
   setRequestProperty("Accept", "application/json")
  }
  return try {
   val status = connection.responseCode
   val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
    ?.bufferedReader()?.use { it.readText() }.orEmpty()
   if (status !in 200..299) error("Could not load available games")
   JSONArray(responseBody).let { games -> (0 until games.length()).map { games.getString(it) } }
  } finally {
   connection.disconnect()
  }
 }

 fun createGame(name: String): GameSession = request("api/games", "POST", name = name)

 fun joinGame(code: String, name: String): GameSession =
  request("api/games/${code.uppercase()}/players", "POST", name = name)

 fun getGame(session: GameSession): GameSession =
  request("api/games/${session.code}", "GET", playerId = session.playerId)

 fun startGame(session: GameSession): GameSession =
  request("api/games/${session.code}/start", "POST", playerId = session.playerId)

 fun finishTurn(session: GameSession): GameSession =
  request("api/games/${session.code}/pass", "POST", playerId = session.playerId)

 fun suggest(session: GameSession, suspect: GameCard, weapon: GameCard, room: GameCard): GameSession =
  request("api/games/${session.code}/suggestions", "POST", playerId = session.playerId,
   body = selection(suspect, weapon, room))

 fun respond(session: GameSession, card: GameCard? = null): GameSession =
  request("api/games/${session.code}/suggestions/respond", "POST", playerId = session.playerId,
   body = JSONObject().put("card", card?.json()))

 fun accuse(session: GameSession, suspect: GameCard, weapon: GameCard, room: GameCard): GameSession =
  request("api/games/${session.code}/accusations", "POST", playerId = session.playerId,
   body = selection(suspect, weapon, room))

 private fun request(
  path: String,
  method: String,
  name: String? = null,
  playerId: String? = null,
  body: JSONObject? = null,
 ): GameSession {
  val connection = (URL(BuildConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
   requestMethod = method
   connectTimeout = 10_000
   readTimeout = 10_000
   setRequestProperty("Accept", "application/json")
   playerId?.let { setRequestProperty("X-Player-Id", it) }
   if (name != null || body != null) {
    doOutput = true
    setRequestProperty("Content-Type", "application/json; charset=utf-8")
   }
  }

  return try {
   val requestBody = body ?: name?.let { JSONObject().put("name", it.trim()) }
   requestBody?.let {
    connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
     writer -> writer.write(it.toString())
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
    GamePlayer(it.getString("id"), it.getString("name"), it.getBoolean("host"), it.getBoolean("eliminated"))
   }
  }
  val playerId = knownPlayerId ?: players.first {
   it.name.equals(playerName?.trim(), ignoreCase = true)
  }.id
  fun cards(key: String) = response.getJSONArray(key).let { array -> (0 until array.length()).map { index ->
   array.getJSONObject(index).let { GameCard(it.getString("type"), it.getString("name")) }
  } }
  val events = response.getJSONArray("events").let { array -> (0 until array.length()).map { array.getJSONObject(it).getString("message") } }
  return GameSession(response.getString("code"), playerId, response.getString("status"), response.getString("phase"), players,
   response.optString("currentPlayerId").takeIf { it.isNotBlank() }, response.optString("responderId").takeIf { it.isNotBlank() }, response.optString("winnerId").takeIf { it.isNotBlank() },
   cards("hand"), cards("cards"), cards("suggestedCards"), cards("solution"), events)
 }

 private fun selection(suspect: GameCard, weapon: GameCard, room: GameCard) =
  JSONObject().put("suspect", suspect.json()).put("weapon", weapon.json()).put("room", room.json())
 private fun GameCard.json() = JSONObject().put("type", type).put("name", name)
}
