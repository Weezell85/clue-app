package com.cluecompanion

import android.content.Context

data class SavedGameSession(val gameCode: String, val playerToken: String, val playerId: String, val playerName: String)

class SessionStore(context: Context) {
 private val preferences = context.getSharedPreferences("game_session", Context.MODE_PRIVATE)

 fun save(session: GameSession) {
  preferences.edit().putString("gameCode", session.code).putString("playerToken", session.playerToken)
   .putString("playerId", session.playerId).putString("playerName", session.playerName).apply()
 }

 fun load(): SavedGameSession? {
  val code = preferences.getString("gameCode", null) ?: return null
  val token = preferences.getString("playerToken", null) ?: return null
  val id = preferences.getString("playerId", null) ?: return null
  return SavedGameSession(code, token, id, preferences.getString("playerName", "").orEmpty())
 }

 fun clear() = preferences.edit().clear().apply()
}
