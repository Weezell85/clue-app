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
 val playerToken: String,
 val playerId: String,
 val playerName: String,
 val status: String,
 val phase: String,
 val players: List<GamePlayer>,
 val currentPlayerId: String?,
 val responderId: String?,
 val winnerId: String?,
 val hand: List<GameCard>,
 val cards: List<GameCard>,
 val turnRoom: GameCard?,
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
  request("api/games/${session.code}", "GET", session = session)

 fun restoreGame(saved: SavedGameSession): GameSession = getGame(GameSession(
  saved.gameCode, saved.playerToken, saved.playerId, saved.playerName, "", "", emptyList(),
  null, null, null, emptyList(), emptyList(), null, emptyList(), emptyList(), emptyList()
 ))

 fun startGame(session: GameSession): GameSession =
  request("api/games/${session.code}/start", "POST", session = session)

 fun finishTurn(session: GameSession): GameSession =
  request("api/games/${session.code}/pass", "POST", session = session)

 fun enterRoom(session: GameSession, room: GameCard): GameSession =
  request("api/games/${session.code}/room", "POST", session = session, body = room.json())

 fun noRoom(session: GameSession): GameSession =
  request("api/games/${session.code}/no-room", "POST", session = session)

 fun suggest(session: GameSession, suspect: GameCard, weapon: GameCard, room: GameCard): GameSession =
  request("api/games/${session.code}/suggestions", "POST", session = session,
   body = selection(suspect, weapon, room))

 fun respond(session: GameSession, card: GameCard? = null): GameSession =
  request("api/games/${session.code}/suggestions/respond", "POST", session = session,
   body = JSONObject().put("card", card?.json()))

 fun accuse(session: GameSession, suspect: GameCard, weapon: GameCard, room: GameCard): GameSession =
  request("api/games/${session.code}/accusations", "POST", session = session,
   body = selection(suspect, weapon, room))

 private fun request(
  path: String,
  method: String,
  name: String? = null,
  session: GameSession? = null,
  body: JSONObject? = null,
 ): GameSession {
  val connection = (URL(BuildConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
   requestMethod = method
   connectTimeout = 10_000
   readTimeout = 10_000
   setRequestProperty("Accept", "application/json")
   session?.let { setRequestProperty("Authorization", "Bearer ${it.playerToken}") }
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
    throw GameApiException(status, message)
   }

   parse(responseBody, session, name)
  } finally {
   connection.disconnect()
  }
 }

 private fun parse(responseBody: String, knownSession: GameSession?, playerName: String?): GameSession {
  val response = JSONObject(responseBody)
  val jsonPlayers = response.getJSONArray("players")
  val players = (0 until jsonPlayers.length()).map { index ->
   jsonPlayers.getJSONObject(index).let {
    GamePlayer(it.getString("id"), it.getString("name"), it.getBoolean("host"), it.getBoolean("eliminated"))
   }
  }
  val playerId = knownSession?.playerId ?: players.first {
   it.name.equals(playerName?.trim(), ignoreCase = true)
  }.id
  fun cards(key: String) = response.getJSONArray(key).let { array -> (0 until array.length()).map { index ->
   array.getJSONObject(index).let { GameCard(it.getString("type"), it.getString("name")) }
  } }
  val events = response.getJSONArray("events").let { array -> (0 until array.length()).map { array.getJSONObject(it).getString("message") } }
  val me = players.first { it.id == playerId }
  val playerToken = response.optString("playerToken").takeIf { !response.isNull("playerToken") && it.isNotBlank() } ?: knownSession?.playerToken
   ?: error("The server did not return a player token")
  return GameSession(response.getString("code"), playerToken, playerId, me.name, response.getString("status"), response.getString("phase"), players,
   response.optString("currentPlayerId").takeIf { it.isNotBlank() }, response.optString("responderId").takeIf { it.isNotBlank() }, response.optString("winnerId").takeIf { it.isNotBlank() },
   cards("hand"), cards("cards"), response.optJSONObject("turnRoom")?.let { GameCard(it.getString("type"),it.getString("name")) }, cards("suggestedCards"), cards("solution"), events)
 }

 private fun selection(suspect: GameCard, weapon: GameCard, room: GameCard) =
  JSONObject().put("suspect", suspect.json()).put("weapon", weapon.json()).put("room", room.json())
 private fun GameCard.json() = JSONObject().put("type", type).put("name", name)
}

class GameApiException(val statusCode: Int, message: String) : Exception(message)
