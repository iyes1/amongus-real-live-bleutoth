package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.audio.SoundSynth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Swipe Card Interactive Game
 */
@Composable
fun SwipeCardGame(onComplete: () -> Unit) {
    var swipeState by remember { mutableStateOf("READY") } // READY, SWIPING, TOO_FAST, TOO_SLOW, ACCEPTED
    var dragPositionX by remember { mutableFloatStateOf(0f) }
    var startTime by remember { mutableLongStateOf(0L) }
    var widthTrack by remember { mutableFloatStateOf(1f) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1B24))
            .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("swipe_card_minigame")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SECURITY CHECKPOINT",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )
            Text(
                text = "SWIPE ADMIN IDENTIFICATION CARD",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Screen/Terminal readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF070B0E), RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        when (swipeState) {
                            "ACCEPTED" -> Color(0xFF04FF88)
                            "TOO_FAST", "TOO_SLOW" -> Color(0xFFFF124F)
                            else -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (swipeState) {
                        "READY" -> "> INSERT CARD & SWIPE"
                        "SWIPING" -> "--- CALIBRATING VELOCITY ---"
                        "TOO_FAST" -> "❌ BADGE READ FAILURE: TOO FAST"
                        "TOO_SLOW" -> "❌ BADGE READ FAILURE: TOO SLOW"
                        "ACCEPTED" -> "✅ SYSTEM GRANTED ACCESS"
                        else -> "> UNKNOWN FAULT"
                    },
                    color = when (swipeState) {
                        "ACCEPTED" -> Color(0xFF04FF88)
                        "TOO_FAST", "TOO_SLOW" -> Color(0xFFFF124F)
                        else -> Color(0xFF00E5FF)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Swipe Reader Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .drawBehind {
                        widthTrack = size.width
                        // Draw horizontal reader slot lines
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF1B313E), Color(0xFF33576C), Color(0xFF1B313E))
                            ),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 14f
                        )
                        drawLine(
                            color = Color(0xFF070B0E),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 4f
                        )
                    }
            ) {
                // The draggable card
                val cardWidth = 90.dp
                Box(
                    modifier = Modifier
                        .offset(x = dragPositionX.dp)
                        .width(cardWidth)
                        .height(70.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD600), Color(0xFFFF8F00))
                            )
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    startTime = System.currentTimeMillis()
                                    swipeState = "SWIPING"
                                },
                                onDragEnd = {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val fraction = dragPositionX / (widthTrack - 120f)
                                    
                                    if (fraction < 0.85f) {
                                        // Didn't swipe all the way
                                        swipeState = "READY"
                                        dragPositionX = 0f
                                    } else {
                                        // Complete swipe, evaluate duration
                                        if (elapsed < 180) {
                                            swipeState = "TOO_FAST"
                                            SoundSynth.playDefeat()
                                        } else if (elapsed > 800) {
                                            swipeState = "TOO_SLOW"
                                            SoundSynth.playDefeat()
                                        } else {
                                            swipeState = "ACCEPTED"
                                            SoundSynth.playTaskChime()
                                            coroutineScope.launch {
                                                delay(1000)
                                                onComplete()
                                            }
                                        }
                                        // Reset swipe position slightly after evaluation
                                        coroutineScope.launch {
                                            delay(1500)
                                            if (swipeState != "ACCEPTED") {
                                                dragPositionX = 0f
                                                swipeState = "READY"
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = (dragPositionX + dragAmount.x).coerceIn(0f, widthTrack - 240f)
                                    dragPositionX = newX
                                }
                            )
                        }
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFF212121))
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CORE-ID",
                                color = Color(0xFF212121),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calibration helper stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OPTIMAL SPEED: 180ms - 800ms", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("STATUS: $swipeState", color = Color(0xFF00E5FF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * Connect Wires Interactive game
 */
@Composable
fun ConnectWiresGame(onComplete: () -> Unit) {
    // We have 4 wires. Predefined colors in matching positions.
    val colors = listOf(Color(0xFFFF124F), Color(0xFFFFD600), Color(0xFF00E5FF), Color(0xFF04FF88))
    
    // Wire connection targets: Map of Left Node index -> Right Node index (-1 if none)
    val connections = remember { mutableStateMapOf(0 to -1, 1 to -1, 2 to -1, 3 to -1) }
    
    // Drag tracks
    var activeDraggingNode by remember { mutableStateOf<Int?>(null) }
    var dragEndOffset by remember { mutableStateOf(Offset.Zero) }
    
    val nodePositionsLeft = remember { mutableStateMapOf<Int, Offset>() }
    val nodePositionsRight = remember { mutableStateMapOf<Int, Offset>() }

    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B141C))
            .border(2.dp, Color(0xFFFFD600), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("connect_wires_minigame")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ELECTRICAL JUNCTION PANEL",
                color = Color(0xFFFFD600),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
            Text(
                text = "CONNECT REPLICATED WIRE SHUNTS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .onGloballyPositioned { parentCoordinates = it }
                    .drawBehind {
                        // Draw already connected pathways
                        connections.forEach { (leftIdx, rightIdx) ->
                            if (rightIdx != -1) {
                                val start = nodePositionsLeft[leftIdx] ?: Offset.Zero
                                val end = nodePositionsRight[rightIdx] ?: Offset.Zero
                                drawLine(
                                    color = colors[leftIdx],
                                    start = start,
                                    end = end,
                                    strokeWidth = 14f
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.5f),
                                    start = start,
                                    end = end,
                                    strokeWidth = 3f
                                )
                            }
                        }

                        // Draw actively dragging path
                        activeDraggingNode?.let { leftIdx ->
                            val start = nodePositionsLeft[leftIdx] ?: Offset.Zero
                            drawLine(
                                color = colors[leftIdx],
                                start = start,
                                end = dragEndOffset,
                                strokeWidth = 12f
                            )
                        }
                    }
            ) {
                // Left Column Nodes
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    parentCoordinates?.let { parent ->
                                        if (coordinates.isAttached && parent.isAttached) {
                                            val localOffset = parent.localPositionOf(coordinates, Offset.Zero)
                                            val center = localOffset + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                                            nodePositionsLeft[idx] = center
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .size(24.dp)
                                .background(colors[idx], RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            activeDraggingNode = idx
                                            val startCenter = nodePositionsLeft[idx] ?: Offset.Zero
                                            dragEndOffset = startCenter
                                        },
                                        onDragEnd = {
                                            // Assess if proximity to right node is close
                                            var matchedIdx = -1
                                            nodePositionsRight.forEach { (rightIdx, pos) ->
                                                val distance = (pos - dragEndOffset).getDistance()
                                                if (distance < 50f) {
                                                    matchedIdx = rightIdx
                                                }
                                            }

                                            // Rule: Match left index to right index ONLY if colors are equal
                                            if (matchedIdx == idx) {
                                                connections[idx] = matchedIdx
                                                SoundSynth.playVent() // Click sound effect
                                                
                                                // Check final completion
                                                if (connections.values.all { it != -1 }) {
                                                    SoundSynth.playTaskChime()
                                                    coroutineScope.launch {
                                                        delay(800)
                                                        onComplete()
                                                    }
                                                }
                                            } else {
                                                connections[idx] = -1
                                            }
                                            activeDraggingNode = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragEndOffset += dragAmount
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.Center)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }

                // Center visual label
                Text(
                    text = "◀ DRAG CORRESPONDING SHUNTS ▶",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )

                // Right Column Nodes
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    parentCoordinates?.let { parent ->
                                        if (coordinates.isAttached && parent.isAttached) {
                                            val localOffset = parent.localPositionOf(coordinates, Offset.Zero)
                                            val center = localOffset + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                                            nodePositionsRight[idx] = center
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .size(24.dp)
                                .background(colors[idx], RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        if (connections.values.contains(idx)) Color.White else Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion state
            IconButton(
                onClick = {
                    connections[0] = -1
                    connections[1] = -1
                    connections[2] = -1
                    connections[3] = -1
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset wire circuits", tint = Color.White)
            }
        }
    }
}

/**
 * Calibrate Distributor precision reactor tap alignment minigame
 */
@Composable
fun CalibrateDistributorGame(onComplete: () -> Unit) {
    var activeStage by remember { mutableIntStateOf(1) } // 3 stages to complete
    var currentAngle by remember { mutableFloatStateOf(0f) }
    var calibrationMessage by remember { mutableStateOf("TAP AT PEAK ALIGNMENT") }
    
    val coroutineScope = rememberCoroutineScope()

    // Smoothly rotating dial pointer
    LaunchedEffect(activeStage) {
        if (activeStage <= 3) {
            val rotationSpeed = 4f + (activeStage * 2.5f) // Increase speed per level
            while (true) {
                currentAngle = (currentAngle + rotationSpeed) % 360f
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF140F00))
            .border(2.dp, Color(0xFFFFD600), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("calibrate_distributor_minigame")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = " REACTOR CALIBRATION",
                color = Color(0xFFFFD600),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
            Text(
                text = "ALIGN ROTARY CONNECTIONS WITH STAGE CONTACTS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Stage bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val stageNum = index + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                when {
                                    activeStage > stageNum -> Color(0xFF04FF88)
                                    activeStage == stageNum -> Color(0xFFFFD600)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Dial Canvas Display
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Color(0xFF050505), CircleShape)
                    .border(2.dp, Color(0xFFFFD600).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Target indicator at top (12 o'clock, which is -90 degrees)
                    val targetAngleRad = Math.toRadians(-90.0)
                    val targetOffset = Offset(
                        (center.x + (radius - 20f) * cos(targetAngleRad)).toFloat(),
                        (center.y + (radius - 20f) * sin(targetAngleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFF04FF88),
                        radius = 12f,
                        center = targetOffset
                    )

                    // Active pointer line representing current rotation
                    val activeAngleRad = Math.toRadians(currentAngle.toDouble() - 90.0)
                    val pointerEnd = Offset(
                        (center.x + (radius - 35f) * cos(activeAngleRad)).toFloat(),
                        (center.y + (radius - 35f) * sin(activeAngleRad)).toFloat()
                    )
                    
                    drawLine(
                        color = Color(0xFFFF124F),
                        start = center,
                        end = pointerEnd,
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = pointerEnd
                    )

                    // Center spindle
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 16f,
                        center = center
                    )
                }

                // Active lock signal
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${activeStage}/3",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Calibrate Button
            Button(
                onClick = {
                    // Check if currentAngle is close to 360/0 (within an acceptable target threshold of +/- 30 degrees)
                    // Angle 0-360, where 0 represents 12 o'clock alignment center!
                    val isAligned = currentAngle < 28f || currentAngle > 332f
                    if (isAligned) {
                        SoundSynth.playVent() // Calibrate Stage hit
                        if (activeStage < 3) {
                            activeStage++
                            calibrationMessage = "STAGE $activeStage CONNECTED!"
                        } else {
                            activeStage = 4
                            calibrationMessage = "REACTOR FULLY LOGGED"
                            SoundSynth.playTaskChime()
                            coroutineScope.launch {
                                delay(1000)
                                onComplete()
                            }
                        }
                    } else {
                        // Reset everything
                        SoundSynth.playDefeat() // Failure buzz
                        activeStage = 1
                        calibrationMessage = "❌ CALIBRATION DRIFT! RESET TO 1"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("distributor_action_button")
            ) {
                Text(
                    text = "ENGAGE CALIBRATOR CONNECTOR",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = calibrationMessage,
                color = if (activeStage == 4) Color(0xFF04FF88) else Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * Interactive holographic download progress minigame
 */
@Composable
fun HoldDownloadGame(onComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableIntStateOf(0) }
    
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            while (progress < 100f) {
                delay(120)
                downloadSpeed = (12..48).random()
                progress = (progress + (downloadSpeed / 8f)).coerceAtMost(100f)
            }
            isDownloading = false
            SoundSynth.playTaskChime()
            delay(1000)
            onComplete()
        } else {
            downloadSpeed = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1A15))
            .border(2.dp, Color(0xFF04FF88), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("hold_download_minigame")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ANTENNA LINK TERMINAL",
                color = Color(0xFF04FF88),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
            Text(
                text = "ESTABLISH SPACE VECTOR FILE DOWNLOAD",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic holographic visual
            Box(
                modifier = Modifier
                    .fillMapHeightHex()
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF04FF88).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Simple signal transmit laser effect
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Satellite", tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(16.dp))
                            // Animating dots to simulate transfer beam
                            Text(
                                "▪ ▪ ▪ ▪ ▪ ▪ ▪",
                                color = Color(0xFF04FF88),
                                modifier = Modifier.graphicsLayer {
                                    translationX = (System.currentTimeMillis() % 1000 / 10f) % 40f
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.Refresh, contentDescription = "Console", tint = Color(0xFF04FF88))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SPEED: ${downloadSpeed} MB/S - SYNCING METADATA...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF04FF88)
                        )
                    }
                } else if (progress >= 100f) {
                    Text(
                        text = "100% COMPLETE - CORE LINK RECORDED",
                        color = Color(0xFF04FF88),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "▶ READY FOR INGESTION CONNECTION",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = Color(0xFF04FF88),
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "INGESTED DATA STREAM: ${progress.toInt()}%",
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Hold Button
            Button(
                onClick = {
                    if (progress < 100f) {
                        isDownloading = !isDownloading
                        SoundSynth.playVent() // Switch sound
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDownloading) Color(0xFFFF124F) else Color(0xFF04FF88)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_action_button")
            ) {
                Text(
                    text = if (isDownloading) "HALT STREAM TRANSMISSION" else "INITIATE ANTENNA DOWNLOAD",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Seek Interactive Frequency Alignment minigame
 */
@Composable
fun SeekFrequencyGame(onComplete: () -> Unit) {
    // Communication frequency goes from 88.0 MHz to 108.0 MHz
    var activeFreq by remember { mutableFloatStateOf(88.0f) }
    // A stable target frequency generated once
    val targetFreq = remember { (900..1060).random() / 10f }
    var reachedAndLocked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val distToTarget = kotlin.math.abs(activeFreq - targetFreq)
    val isAligned = distToTarget < 0.6f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF071118))
            .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("seek_frequency_minigame")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "COMMUNICATIONS FREQUENCY SEEKER",
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
            Text(
                text = "SEEK VECTOR ALIGNMENT FOR SHIP BROADCAST",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Target information panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C161F), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET: ${String.format("%.1f", targetFreq)} MHz",
                    color = Color(0xFF04FF88),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "ACTIVE: ${String.format("%.1f", activeFreq)} MHz",
                    color = if (isAligned) Color(0xFF04FF88) else Color(0xFFFF124F),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oscilloscope custom frequency canvas drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF03080C), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sine wave animation timeline
                val infiniteTransition = rememberInfiniteTransition()
                val waveOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val path = androidx.compose.ui.graphics.Path()

                    // Calculate amplitude and noise based on how far we are from target
                    val closenessFactor = (1.0f - (distToTarget / 20.0f)).coerceAtLeast(0.01f) // 0 to 1
                    val baseAmplitude = 30f * closenessFactor
                    val noiseLevel = ((1.0f - closenessFactor) * 25f).coerceAtLeast(0f)

                    path.moveTo(0f, centerY)

                    // Draw a continuous mathematical wave matching closeness
                    for (x in 0..width.toInt() step 4) {
                        val xFloat = x.toFloat()
                        // Base sine wave representing true resonant carrier
                        val carrier = kotlin.math.sin((xFloat / 40f) * closenessFactor * 4f + waveOffset) * baseAmplitude
                        
                        // Chaotic high-frequency noise generated from a pseudo-random function
                        val noise = if (closenessFactor < 0.95f) {
                            kotlin.math.sin(xFloat * 0.8f + waveOffset * 7f) * noiseLevel +
                            kotlin.math.cos(xFloat * 1.5f - waveOffset * 11f) * (noiseLevel * 0.4f)
                        } else 0f

                        val yFloat = centerY + carrier + noise
                        path.lineTo(xFloat, yFloat)
                    }

                    drawPath(
                        path = path,
                        color = if (isAligned) Color(0xFF04FF88) else Color(0xFFFF124F).copy(alpha = 0.85f),
                        style = Stroke(width = 4f)
                    )

                    // Draw grid overlays
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.1f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 2f
                    )
                }

                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF04FF88).copy(alpha = 0.12f))
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF04FF88), RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = "RESONANCE LOCK ACQUIRED",
                        color = Color(0xFF04FF88),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The main Seeking Slider (custom seek bar)
            Slider(
                value = activeFreq,
                onValueChange = { if (!reachedAndLocked) activeFreq = it },
                valueRange = 88.0f..108.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("frequency_slider_seek"),
                colors = SliderDefaults.colors(
                    thumbColor = if (isAligned) Color(0xFF04FF88) else Color(0xFF00E5FF),
                    activeTrackColor = if (isAligned) Color(0xFF04FF88).copy(alpha = 0.5f) else Color(0xFF00E5FF).copy(alpha = 0.5f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Engage connection button
            Button(
                onClick = {
                    if (isAligned && !reachedAndLocked) {
                        reachedAndLocked = true
                        SoundSynth.playTaskChime()
                        coroutineScope.launch {
                            delay(1000)
                            onComplete()
                        }
                    } else {
                        SoundSynth.playDefeat() // Static buzz
                    }
                },
                enabled = isAligned && !reachedAndLocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    disabledContainerColor = Color(0xFF1D2A33)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("comms_seek_lock_button")
            ) {
                Text(
                    text = if (reachedAndLocked) "TRANSMISSION SECURED" else "LOCK TRANSMISSION WAVE",
                    color = if (isAligned) Color.Black else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Utility extension to fix layout padding on heights
fun Modifier.fillMapHeightHex() = this.fillMaxWidth()
