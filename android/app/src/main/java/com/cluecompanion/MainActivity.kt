package com.cluecompanion

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ClueApp() } }
}

private val Ink=Color(0xFF101713); private val Panel=Color(0xFF1D2923); private val Gold=Color(0xFFD7B56D); private val Cream=Color(0xFFF7F0DF)

@Composable fun ClueApp() {
 var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Ink,surface=Panel,onBackground=Cream,onSurface=Cream)) {
  Box(Modifier.fillMaxSize().background(Ink).padding(24.dp)) {
   Column(Modifier.fillMaxWidth().align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
    Text("C L U E",color=Gold,fontSize=46.sp,fontWeight=FontWeight.Black,letterSpacing=5.sp)
    Text("COMPANION",color=Cream,fontSize=13.sp,letterSpacing=4.sp)
    Spacer(Modifier.height(12.dp)); Text("Leave the cards in the box.",color=Cream.copy(alpha=.7f),fontSize=16.sp)
    Spacer(Modifier.height(42.dp))
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
     Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
      Text("WHO'S PLAYING?",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
      OutlinedTextField(name,{name=it.take(30)},label={Text("Your name")},singleLine=true,modifier=Modifier.fillMaxWidth())
      Button(onClick={},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("CREATE A GAME",fontWeight=FontWeight.Bold) }
      Row(verticalAlignment=Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f));Text("  OR  ",color=Cream.copy(alpha=.5f));HorizontalDivider(Modifier.weight(1f)) }
      OutlinedTextField(code,{code=it.uppercase().take(5)},label={Text("5-letter game code")},singleLine=true,modifier=Modifier.fillMaxWidth())
      OutlinedButton(onClick={},enabled=name.isNotBlank()&&code.length==5,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("JOIN GAME",fontWeight=FontWeight.Bold) }
     }
    }
    Spacer(Modifier.height(30.dp));Text("3–6 detectives • One hidden truth",color=Cream.copy(alpha=.55f),textAlign=TextAlign.Center)
   }
  }
 }
}
@Preview(showBackground=true,widthDp=390,heightDp=844) @Composable private fun Preview(){ClueApp()}
