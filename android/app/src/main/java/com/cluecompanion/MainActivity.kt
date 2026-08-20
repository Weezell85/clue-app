package com.cluecompanion

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ClueApp() } }
}

private val Ink=Color(0xFF101713); private val Panel=Color(0xFF1D2923); private val Gold=Color(0xFFD7B56D); private val Cream=Color(0xFFF7F0DF)

@Composable fun ClueApp() {
 var gameSession by remember { mutableStateOf<GameSession?>(null) }
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Panel,onBackground=Cream,onSurface=Cream)) {
  Box(Modifier.fillMaxSize().background(Ink).padding(24.dp)) {
   gameSession?.let { session ->
    LobbyScreen(session, onSessionChanged={ gameSession=it }, modifier=Modifier.align(Alignment.Center))
   } ?: LandingScreen(onGameJoined={ gameSession=it }, modifier=Modifier.align(Alignment.Center))
  }
 }
}

@Composable private fun LandingScreen(onGameJoined: (GameSession) -> Unit, modifier: Modifier=Modifier) {
 var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
 var isSubmitting by remember { mutableStateOf(false) }; var errorMessage by remember { mutableStateOf<String?>(null) }
 val mainHandler = remember { Handler(Looper.getMainLooper()) }
 Column(modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
  AppTitle()
  Spacer(Modifier.height(42.dp))
  Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
   Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text("WHO'S PLAYING?",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
    OutlinedTextField(name,{name=it.take(30)},label={Text("Your name")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Button(onClick={
     submit(mainHandler, { GameApi.createGame(name) }, { isSubmitting=it }, { errorMessage=it }, onGameJoined)
    },enabled=name.isNotBlank()&&!isSubmitting,modifier=Modifier.fillMaxWidth().height(52.dp)) {
     if(isSubmitting) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Text("CREATE A GAME",fontWeight=FontWeight.Bold)
    }
    errorMessage?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    Row(verticalAlignment=Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f));Text("  OR  ",color=Cream.copy(alpha=.5f));HorizontalDivider(Modifier.weight(1f)) }
    OutlinedTextField(code,{code=it.uppercase().take(5)},label={Text("5-letter game code")},singleLine=true,modifier=Modifier.fillMaxWidth())
    OutlinedButton(onClick={
     submit(mainHandler, { GameApi.joinGame(code,name) }, { isSubmitting=it }, { errorMessage=it }, onGameJoined)
    },enabled=name.isNotBlank()&&code.length==5&&!isSubmitting,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("JOIN GAME",fontWeight=FontWeight.Bold) }
   }
  }
  Spacer(Modifier.height(30.dp));Text("3–6 detectives • One hidden truth",color=Cream.copy(alpha=.55f),textAlign=TextAlign.Center)
 }
}

private fun submit(handler: Handler, request: () -> GameSession, loading: (Boolean) -> Unit, error: (String?) -> Unit, success: (GameSession) -> Unit) {
 loading(true); error(null)
 Thread {
  val result=runCatching(request)
  handler.post { result.onSuccess(success).onFailure { error(it.message ?: "Could not reach the server") }; loading(false) }
 }.start()
}

@Composable private fun LobbyScreen(session: GameSession, onSessionChanged: (GameSession) -> Unit, modifier: Modifier=Modifier) {
 var errorMessage by remember { mutableStateOf<String?>(null) }; var isStarting by remember { mutableStateOf(false) }
 val mainHandler = remember { Handler(Looper.getMainLooper()) }
 val me = session.players.first { it.id == session.playerId }

 LaunchedEffect(session.code, session.playerId, session.status) {
  while (session.status == "LOBBY") {
   delay(2_000)
   runCatching { withContext(Dispatchers.IO) { GameApi.getGame(session) } }
    .onSuccess { onSessionChanged(it); errorMessage=null }
    .onFailure { errorMessage="Waiting for the server…" }
  }
 }

 Column(modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
  AppTitle()
  Spacer(Modifier.height(36.dp))
  Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
   Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text(if(session.status=="LOBBY") "GAME LOBBY" else "GAME STARTED",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
    Text(session.code,color=Cream,fontSize=36.sp,fontWeight=FontWeight.Black,letterSpacing=7.sp)
    Text("Share this code with the other detectives",color=Cream.copy(alpha=.7f))
    HorizontalDivider()
    Text("PLAYERS (${session.players.size}/6)",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
    session.players.forEach { player ->
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      Text("•",color=Gold,fontSize=22.sp); Spacer(Modifier.width(10.dp)); Text(player.name,Modifier.weight(1f),fontSize=18.sp)
      if(player.isHost) Text("HOST",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
     }
    }
    if(session.status=="LOBBY" && me.isHost) {
     Button(onClick={
      submit(mainHandler, { GameApi.startGame(session) }, { isStarting=it }, { errorMessage=it }, onSessionChanged)
     },enabled=session.players.size>=3&&!isStarting,modifier=Modifier.fillMaxWidth().height(52.dp)) {
      if(isStarting) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Text("START GAME",fontWeight=FontWeight.Bold)
     }
     if(session.players.size<3) Text("Waiting for ${3-session.players.size} more player${if(3-session.players.size==1) "" else "s"}",color=Cream.copy(alpha=.65f))
    } else if(session.status=="LOBBY") Text("Waiting for ${session.players.first { it.isHost }.name} to start the game…",color=Cream.copy(alpha=.65f))
    else Text("The cards have been dealt.",color=Gold,fontWeight=FontWeight.Bold)
    errorMessage?.let { Text(it,color=MaterialTheme.colorScheme.error) }
   }
  }
 }
}

@Composable private fun AppTitle() {
 Text("C L U E",color=Gold,fontSize=46.sp,fontWeight=FontWeight.Black,letterSpacing=5.sp)
 Text("COMPANION",color=Cream,fontSize=13.sp,letterSpacing=4.sp)
 Spacer(Modifier.height(12.dp));Text("Leave the cards in the box.",color=Cream.copy(alpha=.7f),fontSize=16.sp)
}

@Preview(showBackground=true,widthDp=390,heightDp=844) @Composable private fun Preview(){ClueApp()}
