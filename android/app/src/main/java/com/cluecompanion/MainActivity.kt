package com.cluecompanion

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
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
    if(session.status=="LOBBY") LobbyScreen(session, onSessionChanged={ gameSession=it }, modifier=Modifier.align(Alignment.Center))
    else GameScreen(session, onSessionChanged={ gameSession=it }, modifier=Modifier.align(Alignment.Center))
   } ?: LandingScreen(onGameJoined={ gameSession=it }, modifier=Modifier.align(Alignment.Center))
  }
 }
}

@Composable private fun GameScreen(session: GameSession, onSessionChanged: (GameSession) -> Unit, modifier: Modifier=Modifier) {
 var errorMessage by remember { mutableStateOf<String?>(null) }
 var confirmation by remember { mutableStateOf<String?>(null) }
 var showAccusation by remember { mutableStateOf(false) }
 var notification by remember { mutableStateOf<String?>(null) }
 var observedEvents by remember(session.code,session.playerId) { mutableIntStateOf(session.events.size) }
 var isSubmitting by remember { mutableStateOf(false) }
 val handler=remember { Handler(Looper.getMainLooper()) }
 val me=session.players.first { it.id==session.playerId }
 val current=session.players.firstOrNull { it.id==session.currentPlayerId }
 val isMyTurn=session.status=="PLAYING"&&session.currentPlayerId==session.playerId&&!me.isEliminated

 LaunchedEffect(session.code,session.playerId,session.status) {
  while(session.status=="PLAYING") {
   delay(2_000)
   runCatching { withContext(Dispatchers.IO) { GameApi.getGame(session) } }.onSuccess {
    if(it.events.size>observedEvents) notification=it.events.lastOrNull()
    observedEvents=it.events.size;onSessionChanged(it);errorMessage=null
   }.onFailure { errorMessage="Waiting for the server…" }
  }
 }

 Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally) {
  AppTitle();Spacer(Modifier.height(24.dp))
  Text("YOUR CARDS",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
  Spacer(Modifier.height(10.dp))
  session.hand.groupBy { it.type }.forEach { (type,cards) ->
   Card(colors=CardDefaults.cardColors(containerColor=Panel),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)) {
    Column(Modifier.padding(16.dp)) { Text(type,color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold);cards.forEach { Text(it.name,fontSize=18.sp) } }
   }
  }
  Spacer(Modifier.height(20.dp))
  when {
   session.status=="FINISHED" -> {
    val winner=session.players.firstOrNull { it.id==session.winnerId }?.name ?: "A detective"
    Text("$winner solved the case!",color=Gold,fontSize=24.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    Text(session.solution.joinToString(" • ") { it.name },textAlign=TextAlign.Center,modifier=Modifier.padding(top=8.dp))
   }
   me.isEliminated -> Text("Your accusation was incorrect. You are out of the turn order.",color=Cream.copy(alpha=.7f),textAlign=TextAlign.Center)
   isMyTurn -> {
    Text("IT'S YOUR TURN",color=Gold,fontSize=22.sp,fontWeight=FontWeight.Black)
    Spacer(Modifier.height(12.dp))
    Button({confirmation="finish"},enabled=!isSubmitting,modifier=Modifier.fillMaxWidth().height(52.dp)){Text("FINISHED WITH TURN",fontWeight=FontWeight.Bold)}
    Spacer(Modifier.height(10.dp))
    OutlinedButton({confirmation="accuse"},enabled=!isSubmitting,modifier=Modifier.fillMaxWidth().height(52.dp)){Text("MAKE FINAL ACCUSATION",fontWeight=FontWeight.Bold)}
   }
   else -> Text("Waiting for ${current?.name ?: "the next detective"} to finish their turn…",color=Cream.copy(alpha=.7f),textAlign=TextAlign.Center)
  }
  errorMessage?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=12.dp)) }
  Spacer(Modifier.height(24.dp))
 }

 confirmation?.let { action -> AlertDialog(onDismissRequest={confirmation=null},title={Text("Are you sure?")},text={Text(if(action=="finish") "Your turn will pass to the next player." else "A final accusation cannot be changed. An incorrect accusation removes you from the turn order.")},confirmButton={TextButton(onClick={confirmation=null;if(action=="finish")submit(handler,{GameApi.finishTurn(session)},{isSubmitting=it},{errorMessage=it},onSessionChanged)else showAccusation=true}){Text("YES, I'M SURE")}},dismissButton={TextButton({confirmation=null}){Text("CANCEL")}}) }
 if(showAccusation) AccusationDialog(session.cards,onDismiss={showAccusation=false},onSubmit={suspect,weapon,room->showAccusation=false;submit(handler,{GameApi.accuse(session,suspect,weapon,room)},{isSubmitting=it},{errorMessage=it}){result->notification=result.events.lastOrNull();observedEvents=result.events.size;onSessionChanged(result)}})
 notification?.let { message -> AlertDialog(onDismissRequest={notification=null},title={Text(if(session.status=="FINISHED") "Case closed" else "Final accusation")},text={Text(message)},confirmButton={TextButton({notification=null}){Text("OK")}}) }
}

@Composable private fun AccusationDialog(cards: List<GameCard>,onDismiss:()->Unit,onSubmit:(GameCard,GameCard,GameCard)->Unit) {
 val suspects=cards.filter { it.type=="SUSPECT" };val weapons=cards.filter { it.type=="WEAPON" };val rooms=cards.filter { it.type=="ROOM" }
 var suspect by remember { mutableStateOf<GameCard?>(null) };var weapon by remember { mutableStateOf<GameCard?>(null) };var room by remember { mutableStateOf<GameCard?>(null) }
 AlertDialog(onDismissRequest=onDismiss,title={Text("Make final accusation")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){CardDropdown("Character",suspects,suspect){suspect=it};CardDropdown("Weapon",weapons,weapon){weapon=it};CardDropdown("Room",rooms,room){room=it}}},confirmButton={Button(onClick={onSubmit(suspect!!,weapon!!,room!!)},enabled=suspect!=null&&weapon!=null&&room!=null){Text("SUBMIT ACCUSATION")}},dismissButton={TextButton(onDismiss){Text("CANCEL")}})
}

@Composable private fun CardDropdown(label:String,cards:List<GameCard>,selected:GameCard?,onSelected:(GameCard)->Unit) {
 var expanded by remember { mutableStateOf(false) }
 Box { OutlinedButton({expanded=true},modifier=Modifier.fillMaxWidth()){Text(selected?.name ?: "Select $label",Modifier.weight(1f));Text("▾")};DropdownMenu(expanded,onDismissRequest={expanded=false}){cards.forEach { card->DropdownMenuItem(text={Text(card.name)},onClick={onSelected(card);expanded=false})}} }
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
