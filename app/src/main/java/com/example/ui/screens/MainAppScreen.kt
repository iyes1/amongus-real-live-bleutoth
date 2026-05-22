package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynth
import com.example.data.*
import com.example.ui.components.*
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(viewModel: GameViewModel) {
    val gamePhase by viewModel.gamePhase.collectAsState()
    val players by viewModel.players.collectAsState()
    val myPlayerId by viewModel.myPlayerId.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val totalTaskProgress by viewModel.totalTaskProgress.collectAsState()
    
    val sabotageActive by viewModel.sabotageActive.collectAsState()
    val sabotageTimer by viewModel.sabotageTimer.collectAsState()
    
    val myPlayer = players.find { it.id == myPlayerId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    // Sophisticated dark slate to vertical gradient
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF04080B), Color(0xFF0C1319))
                    )
                )
        ) {
            // Underlay alarm strobe if sabotage is active
            if (sabotageActive) {
                var isRedFlash by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    while (true) {
                        isRedFlash = !isRedFlash
                        delay(600)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isRedFlash) Color(0xFFFF124F).copy(alpha = 0.15f) else Color.Transparent
                        )
                )
            }

            // Primary Content Switcher based on current game states
            when (gamePhase) {
                GamePhase.SETUP -> SetupScreen(viewModel)
                GamePhase.ROLE_REVEAL -> RoleRevealScreen(viewModel, myPlayer)
                GamePhase.ACTIVE_GAME -> ActiveGameScreen(viewModel, myPlayer, tasks, totalTaskProgress, players, sabotageActive, sabotageTimer)
                GamePhase.EMERGENCY_MEETING -> EmergencyMeetingScreen(viewModel, players, myPlayerId)
                GamePhase.GAME_OVER -> GameOverScreen(viewModel)
            }
        }
    }
}

/**
 * SCREEN 1: Game parameters installation, custom names and Bluetooth telemetry scanner.
 */
@Composable
fun SetupScreen(viewModel: GameViewModel) {
    val players by viewModel.players.collectAsState()
    val isScanning by viewModel.bluetoothScanning.collectAsState()
    val scannedPeers by viewModel.simulatedBluetoothPeers.collectAsState()
    val isSynced by viewModel.isBluetoothSynced.collectAsState()

    var playerName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Cyber Logo Header
        Text(
            text = "AMONG US RL",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "REAL-LIFE SPATIAL TACTICAL COMPANION",
            color = Color(0xFF04FF88),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Part 1: Add Local Player Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111F2B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "▶ COMMENCE SPATIAL ID LINK",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("CREWMATE CALLSIGN", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("player_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SELECT SPACE SUIT DESIGN:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Suit Colors list grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CrewColors.list.forEachIndexed { index, pair ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(pair.second)
                                .border(
                                    width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                    color = if (selectedColorIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (playerName.isNotBlank()) {
                            val selectedColor = CrewColors.list[selectedColorIndex]
                            viewModel.addPlayer(playerName, selectedColor.first, selectedColor.second)
                            playerName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_player_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("REGISTER CODESIGN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Part 2: Radar Space Scan simulation HUD (Bluetooth Syncing Interface)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF04FF88).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B161E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛰️ BLUETOOTH SPACE NET RADAR",
                        color = Color(0xFF04FF88),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF04FF88))
                    }
                }

                // Radar scanning circular canvas drawing
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF04080B)),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val radarRadius by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 200f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                    val radarRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        
                        // Radar rings
                        drawCircle(color = Color(0xFF04FF88).copy(alpha = 0.15f), radius = 60f, center = center, style = Stroke(2f))
                        drawCircle(color = Color(0xFF04FF88).copy(alpha = 0.1f), radius = 120f, center = center, style = Stroke(2f))
                        drawCircle(color = Color(0xFF04FF88).copy(alpha = 0.05f), radius = 180f, center = center, style = Stroke(2f))

                        // Active sweep
                        drawCircle(
                            color = Color(0xFF04FF88).copy(alpha = (1f - (radarRadius / 200f)).coerceIn(0f, 1f) * 0.4f),
                            radius = radarRadius,
                            center = center,
                            style = Stroke(4f)
                        )

                        // Sweeper line
                        val angleRad = Math.toRadians(radarRotation.toDouble())
                        val endPoint = Offset(
                            (center.x + 200f * cos(angleRad)).toFloat(),
                            (center.y + 200f * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = Color(0xFF04FF88).copy(alpha = 0.3f),
                            start = center,
                            end = endPoint,
                            strokeWidth = 3f
                        )
                    }

                    if (isScanning) {
                        Text(
                            "SCANNING LOCAL TRANSPONDERS...",
                            color = Color(0xFF04FF88),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isSynced) {
                        Text(
                            "✅ ${scannedPeers.size} PEER SYNC VECTOR STABLE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "SCAN BLUETOOTH TO FIND FRIENDS",
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startBluetoothScanning() },
                    modifier = Modifier.fillMaxWidth().testTag("scan_bluetooth_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF04FF88))
                ) {
                    Text("INITIALIZE BLUETOOTH RADAR SCAN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // Show found peers representation
                if (scannedPeers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ACTIVE CORES LOCATED:", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    
                    scannedPeers.forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(peer.colorHex))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(peer.name, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text("SIGNAL: ${peer.bluetoothSignalDb} dBm", color = Color(0xFF04FF88), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Part 3: Active Manifest Crew list
        Text(
            text = "MAIN BOARDING MANIFESTING",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )

        val filteredPlayers = if (searchQuery.isBlank()) {
            players
        } else {
            players.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.colorName.contains(searchQuery, ignoreCase = true)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("SEEK/SEARCH CREW BY CALLSIGN OR COLOR...", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Seeking filter logo", tint = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("manifest_seek_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
            )
        )

        if (filteredPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (players.isEmpty()) "NO CORES REGISTERED. ADD CORES TO COMMENCE." else "NO CREWMATES MATCH SEARCH CRITERIA.",
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        } else {
            filteredPlayers.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF152331), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(p.colorHex)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Suit Color: ${p.colorName}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (p.isSimulatedBluetooth) "Sim BLE" else "Physical App",
                            color = if (p.isSimulatedBluetooth) Color(0xFF04FF88) else Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { viewModel.toggleSimulatedBluetooth(p.id) }
                        )

                        IconButton(
                            onClick = { viewModel.removePlayer(p.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Erase player from database", tint = Color(0xFFFF124F))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Setup soundboard control HUD element
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFD600).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141F28))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "📢 INTERACTIVE SYNTH SOUNDBOARD",
                    color = Color(0xFFFFD600),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { SoundSynth.playEmergencyAlarm() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF124F))) {
                        Text("ALARM", fontSize = 10.sp, color = Color.White)
                    }
                    Button(onClick = { SoundSynth.playBodyReported() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))) {
                        Text("REPORT", fontSize = 10.sp, color = Color.White)
                    }
                    Button(onClick = { SoundSynth.playKill() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9))) {
                        Text("KILL", fontSize = 10.sp, color = Color.White)
                    }
                    Button(onClick = { SoundSynth.playVent() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))) {
                        Text("VENT WHOOSH", fontSize = 10.sp, color = Color.White)
                    }
                    Button(onClick = { SoundSynth.playTaskChime() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF04FF88))) {
                        Text("TASK DONE", fontSize = 10.sp, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Dispatch button
        Button(
            onClick = { viewModel.startRoleReveal() },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("launch_game_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            enabled = players.size >= 3
        ) {
            Text(
                text = "COMMENCE SYSTEM SHIP DISPATCH",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
        
        if (players.size < 3) {
            Text(
                "DISPATCH REQUIRES MINIMUM OF 3 PLAYERS",
                color = Color(0xFFFF124F),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * SCREEN 2: Swiping decrypt block covers to reveal confidential role assignment cards secretly.
 */
@Composable
fun RoleRevealScreen(viewModel: GameViewModel, player: Player?) {
    var isRevealed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SECURITY INTERLOCK REVELATOR",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "PASS AND LOOK DISPATCH - CONFIDENTIAL MANIFEST",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Security Envelope Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1820))
                    .border(
                        2.dp,
                        if (isRevealed) player?.role?.allianceColor ?: Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { 
                        isRevealed = !isRevealed 
                        if (isRevealed) {
                            SoundSynth.playTaskChime()
                        } else {
                            SoundSynth.playVent()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isRevealed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Hologram cipher shield",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    val timeOffset = (System.currentTimeMillis() % 1000).toFloat() / 1000f
                                    translationY = sin(timeOffset * 2f * Math.PI.toFloat()) * 10f
                                }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "TAP HERE TO UNLOCK BIO-CIPHER",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "KEEP DISPLAY HIDDEN",
                            color = Color(0xFFFF124F),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    // Display details of assigned Role
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .animateContentSize()
                    ) {
                        Text(
                            text = "ASSIGNED ASSIGNMENT PROFILE:",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = player?.role?.displayName?.uppercase() ?: "CREWMATE",
                            color = player?.role?.allianceColor ?: Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = player?.role?.description ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    (player?.role?.allianceColor ?: Color.White).copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, player?.role?.allianceColor ?: Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SPECIAL ABILITY: ${player?.role?.abilityName}",
                                    color = player?.role?.allianceColor ?: Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = player?.role?.abilityDescription ?: "",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dismiss_reveal_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF04FF88))
            ) {
                Text(
                    "CONFIRM DISMISS & RUN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * SCREEN 3: Core spatial console with multi-panels and interactive digital games and specialized roles.
 */
@Composable
fun ActiveGameScreen(
    viewModel: GameViewModel,
    myPlayer: Player?,
    tasks: List<GameTask>,
    totalProgress: Float,
    players: List<Player>,
    sabotageActive: Boolean,
    sabotageTimer: Int
) {
    var activeTaskForMinigame by remember { mutableStateOf<GameTask?>(null) }
    var showActiveAbilityConsole by remember { mutableStateOf<String?>(null) } // "SCIENCE", "VENT", "SHIELD", "MIMIC", null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Neon Progress bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C161F))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛰️ SHIP STABILIZATION METERS",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(totalProgress * 100).toInt()}% COMPLETE",
                            color = Color(0xFF04FF88),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { totalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Color(0xFF04FF88),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            // Sabotage Alert banner
            if (sabotageActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, Color(0xFFFF124F), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0A0E))
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Critical sabotage active", tint = Color(0xFFFF124F), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "CRITICAL SABOTAGE INJECTED",
                            color = Color(0xFFFF124F),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            "CORE COMPROMISING IN $sabotageTimer SECONDS",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        
                        var enteredBypassCode by remember { mutableStateOf("") }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = enteredBypassCode,
                                onValueChange = { enteredBypassCode = it },
                                placeholder = { Text("BYPASS CODE", fontSize = 10.sp) },
                                modifier = Modifier.width(140.dp).height(48.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF124F)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    viewModel.resolveSabotageCode(enteredBypassCode)
                                    enteredBypassCode = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF124F))
                            ) {
                                Text("BYPASS", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Two-column Split panel layout - adapts perfectly on phone or expanded screens
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Block: Assigned tasks cards
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        "🗒️ ACTIVE SYSTEM TASKS:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tasks) { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (task.isCompleted) Color(0xFF04FF88).copy(alpha = 0.3f) else Color(0xFF00E5FF).copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (!task.isCompleted) {
                                            activeTaskForMinigame = task
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (task.isCompleted) Color(0xFF0F1A13) else Color(0xFF101C27)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            task.title,
                                            color = if (task.isCompleted) Color(0xFF04FF88) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "LOCATION: ${task.realLocation}",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                                        contentDescription = "Sync indices status",
                                        tint = if (task.isCompleted) Color(0xFF04FF88) else Color(0xFF00E5FF)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Block: Special abilities interfaces / triggers console
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .background(Color(0xFF0A1116), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "🧬 REALM SYSTEMS HUD",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    myPlayer?.let { p ->
                        textPair("Active Role", p.role.displayName, p.role.allianceColor)
                        
                        var showTransmuteDialog by remember { mutableStateOf(false) }

                        // Displays specialized action panels according to real role
                        when (p.role) {
                            Role.SCIENTIST -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showActiveAbilityConsole = "SCIENCE" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("ECG BIOMETRICS MONITOR", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Role.ENGINEER -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showActiveAbilityConsole = "VENT" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("VENT BLU-PRINTS / CCTV", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Role.GUARDIAN_ANGEL -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showActiveAbilityConsole = "SHIELD" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9)),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("CAST GUARDIAN WARD", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Role.IMPOSTOR -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.triggerSabotage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF124F)),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("SPARK LIGHT SABOTAGE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Role.SHAPESHIFTER -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.triggerSabotage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF124F)),
                                    modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 4.dp)
                                ) {
                                    Text("SPARK SABOTAGE", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { showTransmuteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF04FF88)),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text("MIMIC TRANSMUTE", fontSize = 9.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                            else -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No high-tech specials. Connect shunts with speed & guard yourself.", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Code for MIMIC Morph selection popup
                        if (showTransmuteDialog) {
                            AlertDialog(
                                onDismissRequest = { showTransmuteDialog = false },
                                title = { Text("MIMIC MATRIX TRANSFORMATION", color = Color(0xFF04FF88), fontFamily = FontFamily.Monospace) },
                                text = {
                                    Column {
                                        Text("Select the suit color you wish to replicate:", color = Color.White)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        CrewColors.list.forEach { item ->
                                            Button(
                                                onClick = {
                                                    viewModel.performShapeshift(item.first, item.second)
                                                    showTransmuteDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = item.second),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Text(item.first.uppercase(), color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showTransmuteDialog = false }) {
                                        Text("CANCEL", color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF111F2B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Simulated Biometrics HUD
                    Text(
                        text = "VITAL SYSTEMS SUMMARY:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    players.take(3).forEach { peer ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(peer.colorHex))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(peer.name, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (peer.isAlive) "PULSING" else "FLATLINE",
                                color = if (peer.isAlive) Color(0xFF04FF88) else Color(0xFFFF124F),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BIG PHYSICAL EMERGENCY MEETING RED BUTTON COMPONENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Color(0xFF0D141C))
                    .border(1.dp, Color(0xFFFF124F).copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable { viewModel.triggerEmergencyMeeting() }
                    .testTag("emergency_trigger_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glowing reactor red ring
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .size(34.dp)
                            .background(Color(0xFFFF124F), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "INITIATE EMERGENCY ALARM",
                            color = Color(0xFFFF124F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BROADCAST MEETING ALERTS TO ALL PEERS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // MINIGAME DIGITAL INTERACTIVE DIALOG OVERLAY
        activeTaskForMinigame?.let { task ->
            AlertDialog(
                onDismissRequest = { activeTaskForMinigame = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            task.title.uppercase(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { activeTaskForMinigame = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Terminate digital task interaction", tint = Color.White)
                        }
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (task.minigameType) {
                            MinigameType.SWIPE_CARD -> SwipeCardGame {
                                viewModel.completeTask(task.id)
                                activeTaskForMinigame = null
                            }
                            MinigameType.CONNECT_WIRES -> ConnectWiresGame {
                                viewModel.completeTask(task.id)
                                activeTaskForMinigame = null
                            }
                            MinigameType.CALIBRATE_DISTRIBUTOR -> CalibrateDistributorGame {
                                viewModel.completeTask(task.id)
                                activeTaskForMinigame = null
                            }
                            MinigameType.HOLD_DOWNLOAD -> HoldDownloadGame {
                                viewModel.completeTask(task.id)
                                activeTaskForMinigame = null
                            }
                            MinigameType.SEEK_FREQUENCY -> SeekFrequencyGame {
                                viewModel.completeTask(task.id)
                                activeTaskForMinigame = null
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = Color(0xFF0F1B24),
                modifier = Modifier.padding(12.dp)
            )
        }

        // SPECIAL CORES CONSOLES SCREENS OVERLAY (Science, Vents, Shields, etc.)
        showActiveAbilityConsole?.let { mode ->
            AlertDialog(
                onDismissRequest = { showActiveAbilityConsole = null },
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (mode) {
                                "SCIENCE" -> "ECG BIOMETRICS DESK"
                                "VENT" -> "VENTILATION CCTV PLATFORM"
                                "SHIELD" -> "ETHEREAL BARRIER SECTOR"
                                "MIMIC" -> "MIMIC REPLICATION MATRIX"
                                else -> "SYS CONSOLE"
                            },
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showActiveAbilityConsole = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Exit console view", tint = Color.White)
                        }
                    }
                },
                text = {
                    when (mode) {
                        "SCIENCE" -> ScientistConsoleView(players) { deadPlayerId ->
                            viewModel.recordBiometricsDead(deadPlayerId)
                        }
                        "VENT" -> VentConsoleView {
                            viewModel.playVentCameraSound()
                        }
                        "SHIELD" -> ShieldConsoleView(players) { protectedPlayerId ->
                            viewModel.castProtectionBarrier(protectedPlayerId)
                            showActiveAbilityConsole = null
                        }
                    }
                },
                confirmButton = {},
                containerColor = Color(0xFF0C1319)
            )
        }
    }
}

/**
 * SCIENTIST CONSOLE COMPONENT: pulsate live ECG waveforms turning into flatlines.
 */
@Composable
fun ScientistConsoleView(players: List<Player>, onMarkDead: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(players) { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE040FB).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140F18))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(p.colorHex))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (p.isAlive) "BIOMETRICS OK" else "FLATLINE",
                            color = if (p.isAlive) Color(0xFF04FF88) else Color(0xFFFF124F),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Rotating pulse wave animation or flatline
                    if (p.isAlive) {
                        Row(modifier = Modifier.width(60.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            var tick by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    tick = !tick
                                    delay(400)
                                }
                            }
                            Text("∧", color = Color(0xFF04FF88), fontWeight = if (tick) FontWeight.Bold else FontWeight.Normal)
                            Text("∨", color = Color(0xFF04FF88), fontWeight = if (!tick) FontWeight.Bold else FontWeight.Normal)
                        }

                        Button(
                            onClick = { onMarkDead(p.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF124F)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("FLATLINE", fontSize = 9.sp)
                        }
                    } else {
                        // Flatline indicator
                        Text("----------------", color = Color(0xFFFF124F), fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                    }
                }
            }
        }
    }
}

/**
 * ENGINEER CONSOLE COMPONENT: Traversal map schematic representation.
 */
@Composable
fun VentConsoleView(onVentTraverse: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Duct chamber layout architecture:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Blueprint canvas layout illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFF040B11), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Blueprint grid
                drawRect(color = Color(0xFF00E5FF).copy(alpha = 0.1f))
                drawLine(color = Color(0xFF00E5FF).copy(alpha = 0.2f), start = Offset(100f, 0f), end = Offset(100f, size.height))
                drawLine(color = Color(0xFF00E5FF).copy(alpha = 0.2f), start = Offset(0f, 70f), end = Offset(size.width, 70f))
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VENT ALPHA (ADMIN) ◀───▶ VENT BETA (ELECTRICAL)", color = Color(0xFFFFD600), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("▲", color = Color(0xFF00E5FF), modifier = Modifier.padding(vertical = 4.dp))
                Text("VENT GAMMA (NAVIGATION)", color = Color(0xFFFFD600), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onVentTraverse() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
        ) {
            Text("TRAVERSE CONNECTED SECTORS", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

/**
 * GUARDIAN ANGEL CONSOLE COMPONENT: select target to shelter.
 */
@Composable
fun ShieldConsoleView(players: List<Player>, onProtect: (String) -> Unit) {
    Column {
        Text("Shield barrier available once per emergency countdown. Cast protection bubble on any active crew:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(players.filter { it.isAlive }) { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C1D2A), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(p.colorHex))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onProtect(p.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9))
                    ) {
                        Text("SHIELD CORE", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 4: High-stakes caution debate, custom voting widgets and ejection space panels.
 */
@Composable
fun EmergencyMeetingScreen(viewModel: GameViewModel, players: List<Player>, myPlayerId: String) {
    val discussionTime by viewModel.discussionTimeLeft.collectAsState()
    val votingTime by viewModel.votingTimeLeft.collectAsState()
    val votesMap by viewModel.votesMap.collectAsState()
    val hasVoted by viewModel.hasVoted.collectAsState()
    val resultText by viewModel.meetingResultText.collectAsState()

    var suspectSearchQuery by remember { mutableStateOf("") }
    val alivePlayers = players.filter { it.isAlive }
    val displayedSuspects = if (suspectSearchQuery.isBlank()) {
        alivePlayers
    } else {
        alivePlayers.filter {
            it.name.contains(suspectSearchQuery, ignoreCase = true) ||
            it.colorName.contains(suspectSearchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High voltage caution border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF124F))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "--- SECURE DEBATE CONTEXT ACTIVE ---",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large animated siren panel
        Text(
            text = "EMERGENCY MEETING CONVOCATION",
            color = Color(0xFFFF124F),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Modern glowing retro countdown clocks
        Box(
            modifier = Modifier
                .width(180.dp)
                .background(Color(0xFF13080A), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFFFF124F), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (discussionTime > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISCUSSION TIMER", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Text("${discussionTime}S", fontSize = 28.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            } else if (votingTime > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VOTE NOW LIVE", fontSize = 9.sp, color = Color(0xFF04FF88), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("${votingTime}S", fontSize = 28.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            } else {
                Text("EJECTING CORRUPTED COR-ID...", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grid of players card sheets targeting the candidate to eject
        Text(
            text = "SELECT ACTIVE TRANSMISSION SUSPECT:",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = suspectSearchQuery,
            onValueChange = { suspectSearchQuery = it },
            placeholder = { Text("SEEK SUSPECT...", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Seeking suspect logo", tint = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("suspect_seek_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF124F),
                unfocusedBorderColor = Color(0xFFFF124F).copy(alpha = 0.3f)
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(displayedSuspects) { player ->
                val voterId = myPlayerId
                val playerHasVoted = hasVoted.contains(player.id)
                val isSelected = votesMap[voterId] == player.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (isSelected) Color(0xFFFF124F) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (votingTime > 0 && !hasVoted.contains(voterId) && voterId.isNotBlank()) {
                                viewModel.castVote(voterId, player.id)
                                SoundSynth.playVent() // cast click sound
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF220C10) else Color(0xFF14202B)
                    )
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(player.colorHex)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                player.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            // Displays vote cast indicator icon
                            if (playerHasVoted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("VOTED", color = Color(0xFF04FF88), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Skip Button
        if (votingTime > 0 && !hasVoted.contains(myPlayerId)) {
            Button(
                onClick = { 
                    viewModel.castVote(myPlayerId, "SKIP")
                    SoundSynth.playVent()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Text("SKIP EJECT VOTE", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Evaluation outputs (Space Star background with eject status text results)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF030508), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = resultText.uppercase(),
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * SCREEN 5: Splendid triumph victory or defeat report screen.
 */
@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val resultsText by viewModel.meetingResultText.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "SYSTEM REPLICATED RESULTS:",
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (resultsText.contains("WIN") || resultsText.contains("VICTORY")) "VICTORY" else "DEFEAT",
                color = if (resultsText.contains("WIN") || resultsText.contains("VICTORY")) Color(0xFF04FF88) else Color(0xFFFF124F),
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 44.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1319), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = resultsText,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.resetGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("restart_game_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text(
                    "CONVERT TO MANIFEST REGISTRATION HUD",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Utility textPair
@Composable
fun textPair(label: String, value: String, color: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value.uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
