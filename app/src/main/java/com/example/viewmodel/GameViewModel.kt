package com.example.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynth
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GameViewModel : ViewModel() {

    // Core Game States
    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _myPlayerId = MutableStateFlow("")
    val myPlayerId: StateFlow<String> = _myPlayerId.asStateFlow()

    private val _tasks = MutableStateFlow<List<GameTask>>(emptyList())
    val tasks: StateFlow<List<GameTask>> = _tasks.asStateFlow()

    private val _totalTaskProgress = MutableStateFlow(0f) // 0.0f to 1.0f
    val totalTaskProgress: StateFlow<Float> = _totalTaskProgress.asStateFlow()

    // Emergencies and Meeting States
    private val _discussionTimeLeft = MutableStateFlow(30)
    val discussionTimeLeft: StateFlow<Int> = _discussionTimeLeft.asStateFlow()

    private val _votingTimeLeft = MutableStateFlow(30)
    val votingTimeLeft: StateFlow<Int> = _votingTimeLeft.asStateFlow()

    private val _hasVoted = MutableStateFlow<Set<String>>(emptySet())
    val hasVoted: StateFlow<Set<String>> = _hasVoted.asStateFlow()

    private val _votesMap = MutableStateFlow<Map<String, String>>(emptyMap()) // Voter ID -> Target ID (or "SKIP")
    val votesMap: StateFlow<Map<String, String>> = _votesMap.asStateFlow()

    private val _meetingResultText = MutableStateFlow("")
    val meetingResultText: StateFlow<String> = _meetingResultText.asStateFlow()

    // Sabotage Alarm States
    private val _sabotageActive = MutableStateFlow(false)
    val sabotageActive: StateFlow<Boolean> = _sabotageActive.asStateFlow()

    private val _sabotageTimer = MutableStateFlow(45)
    val sabotageTimer: StateFlow<Int> = _sabotageTimer.asStateFlow()

    // Bluetooth Local Space Scanning Hub
    private val _bluetoothScanning = MutableStateFlow(false)
    val bluetoothScanning: StateFlow<Boolean> = _bluetoothScanning.asStateFlow()

    private val _simulatedBluetoothPeers = MutableStateFlow<List<Player>>(emptyList())
    val simulatedBluetoothPeers: StateFlow<List<Player>> = _simulatedBluetoothPeers.asStateFlow()

    private val _isBluetoothSynced = MutableStateFlow(false)
    val isBluetoothSynced: StateFlow<Boolean> = _isBluetoothSynced.asStateFlow()

    // Abilities & Custom State tracking
    private val _isShapeshifted = MutableStateFlow(false)
    val isShapeshifted: StateFlow<Boolean> = _isShapeshifted.asStateFlow()

    private val _shapeshiftColor = MutableStateFlow<Color?>(null)
    val shapeshiftColor: StateFlow<Color?> = _shapeshiftColor.asStateFlow()

    private val _shapeshiftTimer = MutableStateFlow(0)
    val shapeshiftTimer: StateFlow<Int> = _shapeshiftTimer.asStateFlow()

    // Jobs for timers
    private var meetingJob: Job? = null
    private var sabotageJob: Job? = null
    private var shapeshiftJob: Job? = null
    private var bluetoothJob: Job? = null

    init {
        loadDefaultPlayers()
        generateTasks()
    }

    private fun loadDefaultPlayers() {
        val defaultNames = listOf("Red_Pilot", "Cyan_Cyborg", "Lime_Lander", "Gold_Gamer", "Pink_Pioneer")
        val initialPlayers = defaultNames.mapIndexed { idx, name ->
            val colorPair = CrewColors.list[idx % CrewColors.list.size]
            Player(
                id = "peer_$idx",
                name = name,
                colorName = colorPair.first,
                colorHex = colorPair.second,
                role = Role.CREWMATE,
                isAlive = true,
                isSimulatedBluetooth = true,
                bluetoothSignalDb = (-55 - idx * 7)
            )
        }
        _players.value = initialPlayers
    }

    fun addPlayer(name: String, colorName: String, colorHex: Color) {
        val existingColors = _players.value.map { it.colorName }
        if (existingColors.contains(colorName)) return // Color is already taken

        val newPlayer = Player(
            id = UUID.randomUUID().toString(),
            name = name,
            colorName = colorName,
            colorHex = colorHex,
            isAlive = true,
            isSimulatedBluetooth = false
        )
        _players.value = _players.value + newPlayer
    }

    fun removePlayer(playerId: String) {
        _players.value = _players.value.filterNot { it.id == playerId }
    }

    fun toggleSimulatedBluetooth(playerId: String) {
        _players.value = _players.value.map {
            if (it.id == playerId) {
                it.copy(isSimulatedBluetooth = !it.isSimulatedBluetooth)
            } else it
        }
    }

    /**
     * Re-assigns roles based on setup parameters.
     */
    fun startRoleReveal() {
        if (_players.value.isEmpty()) return

        val originalList = _players.value.shuffled()
        val totalPlayers = originalList.size

        // Impostor count estimation
        val impostorCount = if (totalPlayers >= 5) 2 else 1
        
        val modifiedList = originalList.mapIndexed { index, player ->
            val assignedRole = when {
                index < impostorCount -> {
                    // Impostors: 1st gets Shapeshifter, rest get standard Impostor
                    if (index == 0) Role.SHAPESHIFTER else Role.IMPOSTOR
                }
                index == impostorCount -> Role.SCIENTIST // Biometrics tracker
                index == impostorCount + 1 -> Role.ENGINEER // Vent CCTV
                index == impostorCount + 2 -> Role.GUARDIAN_ANGEL // Spirit guard
                else -> Role.CREWMATE
            }
            player.copy(role = assignedRole)
        }

        // Set local device player representing this client terminal
        val clientPlayer = modifiedList.firstOrNull { !it.isSimulatedBluetooth } ?: modifiedList.first()
        _myPlayerId.value = clientPlayer.id

        _players.value = modifiedList
        _gamePhase.value = GamePhase.ROLE_REVEAL
        generateTasks()
    }

    /**
     * Start active game loop.
     */
    fun startGame() {
        _gamePhase.value = GamePhase.ACTIVE_GAME
        _sabotageActive.value = false
        _totalTaskProgress.value = 0f
        SoundSynth.playVictory()
    }

    private fun generateTasks() {
        val taskTemplates = listOf(
            Pair("Swipe Admin Access Card", MinigameType.SWIPE_CARD),
            Pair("Re-Route Electrical Wiring", MinigameType.CONNECT_WIRES),
            Pair("Calibrate Main Distributor Rotating Reactor", MinigameType.CALIBRATE_DISTRIBUTOR),
            Pair("Download Security Telemetry stream", MinigameType.HOLD_DOWNLOAD),
            Pair("Seek Sub-ether Comms Signal", MinigameType.SEEK_FREQUENCY)
        )
        val sampleLocations = listOf("Admin Desk", "Electrical Junction", "Upper Engines", "Navigational CCTV", "Comms Room")
        
        _tasks.value = taskTemplates.mapIndexed { i, template ->
            GameTask(
                id = "task_$i",
                title = template.first,
                realLocation = sampleLocations[i % sampleLocations.size],
                minigameType = template.second,
                isCompleted = false
            )
        }
    }

    /**
     * Mark task as completed
     */
    fun completeTask(taskId: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = true) else it
        }
        recalculateProgress()
    }

    private fun recalculateProgress() {
        val total = _tasks.value.size
        if (total == 0) return
        val complete = _tasks.value.count { it.isCompleted }
        _totalTaskProgress.value = complete.toFloat() / total.toFloat()

        if (complete == total) {
            SoundSynth.playVictory()
            checkGameVictory(crewWin = true)
        }
    }

    /**
     * Trigger simulated local Bluetooth scanner radar.
     */
    fun startBluetoothScanning() {
        if (_bluetoothScanning.value) return
        _bluetoothScanning.value = true
        
        bluetoothJob?.cancel()
        bluetoothJob = viewModelScope.launch {
            _simulatedBluetoothPeers.value = emptyList()
            delay(1200) // Radar ping timing

            // Generate active simulated signals nearby
            _players.value.filter { it.isSimulatedBluetooth }.forEach { peer ->
                _simulatedBluetoothPeers.value = _simulatedBluetoothPeers.value + peer.copy(
                    bluetoothSignalDb = (-85..-40).random()
                )
                delay(400)
            }
            _isBluetoothSynced.value = true
            _bluetoothScanning.value = false
        }
    }

    /**
     * Scientist Vitals Flatline: Click to record dead body report
     */
    fun recordBiometricsDead(playerId: String) {
        _players.value = _players.value.map {
            if (it.id == playerId) it.copy(isAlive = false) else it
        }
        checkGameVictory()
    }

    /**
     * Initiate Emergency Meeting Screen & Alarm Siren
     */
    fun triggerEmergencyMeeting(fromDeadReport: Boolean = false) {
        _gamePhase.value = GamePhase.EMERGENCY_MEETING
        _hasVoted.value = emptySet()
        _votesMap.value = emptyMap()
        _meetingResultText.value = "ANALYZING EVIDENCE & BALLOTS"

        if (fromDeadReport) {
            SoundSynth.playBodyReported()
        } else {
            SoundSynth.playEmergencyAlarm()
        }

        meetingJob?.cancel()
        _discussionTimeLeft.value = 15
        _votingTimeLeft.value = 15

        meetingJob = viewModelScope.launch {
            // Discussion Phase
            while (_discussionTimeLeft.value > 0) {
                delay(1000)
                _discussionTimeLeft.value--
            }

            // Voting Phase
            while (_votingTimeLeft.value > 0) {
                delay(1000)
                _votingTimeLeft.value--
                // Simulate automatic voting for other live simulated Bluetooth peers
                if (_votingTimeLeft.value == 10) {
                    simulatePeerVoting()
                }
            }

            // Exclude or Eject evaluation
            evaluateVotingResults()
        }
    }

    private fun simulatePeerVoting() {
        val alivePlayers = _players.value.filter { it.isAlive }
        if (alivePlayers.isEmpty()) return

        alivePlayers.filter { it.isSimulatedBluetooth }.forEach { peer ->
            // Pick random target from alive players, or SKIP
            val target = if ((0..10).random() > 7) {
                "SKIP"
            } else {
                val targets = alivePlayers.filter { it.id != peer.id }
                if (targets.isNotEmpty()) targets.random().id else "SKIP"
            }
            _votesMap.value = _votesMap.value + Pair(peer.id, target)
            _hasVoted.value = _hasVoted.value + peer.id
        }
    }

    /**
     * Cast vote on a specific player or SKIP
     */
    fun castVote(voterId: String, targetId: String) {
        _votesMap.value = _votesMap.value + Pair(voterId, targetId)
        _hasVoted.value = _hasVoted.value + voterId

        val liveVoterCount = _players.value.count { it.isAlive }
        if (_votesMap.value.size >= liveVoterCount) {
            // All casted early, fast-forward evaluation
            _votingTimeLeft.value = 0
        }
    }

    private suspend fun evaluateVotingResults() {
        val tallies = mutableMapOf<String, Int>()
        _votesMap.value.values.forEach { target ->
            if (target != "SKIP") {
                tallies[target] = (tallies[target] ?: 0) + 1
            }
        }

        if (tallies.isEmpty()) {
            _meetingResultText.value = "THE CREW WAS INDECISIVE. NO ONE WAS EJECTED."
        } else {
            val maxEntry = tallies.maxByOrNull { it.value }
            val highestVotes = maxEntry?.value ?: 0
            val duplicateCheck = tallies.filter { it.value == highestVotes }

            if (duplicateCheck.size > 1) {
                _meetingResultText.value = "TIE DETECTED. NO ONE WAS EJECTED."
            } else {
                val ejectedId = maxEntry?.key ?: ""
                val ejectedPlayer = _players.value.find { it.id == ejectedId }
                if (ejectedPlayer != null) {
                    // Check if guardian shield protects or eject them
                    _players.value = _players.value.map {
                        if (it.id == ejectedId) it.copy(isAlive = false) else it
                    }
                    _meetingResultText.value = "${ejectedPlayer.name} (Color: ${ejectedPlayer.colorName}) WAS EJECTED. ROLE: ${ejectedPlayer.role.displayName}"
                    SoundSynth.playDefeat() // Ejection WHOOSH / dread
                } else {
                    _meetingResultText.value = "NO ONE WAS EJECTED."
                }
            }
        }

        delay(4000)

        // Clear Guardian protections for the next cycle
        _players.value = _players.value.map { it.copy(isProtected = false) }

        // Check Victory status
        val isOver = checkGameVictory()
        if (!isOver) {
            _gamePhase.value = GamePhase.ACTIVE_GAME
        }
    }

    /**
     * Guardian Angel ability - protects alive player
     */
    fun castProtectionBarrier(targetPlayerId: String) {
        _players.value = _players.value.map {
            if (it.id == targetPlayerId) it.copy(isProtected = true) else it
        }
        SoundSynth.playVictory() // Barrier chime
    }

    /**
     * Engineer abilities: check vent cam status
     */
    fun playVentCameraSound() {
        SoundSynth.playVent()
    }

    /**
     * Shapeshifter transmutation
     */
    fun performShapeshift(targetColorName: String, color: Color) {
        _isShapeshifted.value = true
        _shapeshiftColor.value = color
        SoundSynth.playVent() // Gurgle shift sound effect

        shapeshiftJob?.cancel()
        _shapeshiftTimer.value = 25
        shapeshiftJob = viewModelScope.launch {
            while (_shapeshiftTimer.value > 0) {
                delay(1000)
                _shapeshiftTimer.value--
            }
            revertShapeshift()
        }
    }

    fun revertShapeshift() {
        _isShapeshifted.value = false
        _shapeshiftColor.value = null
        SoundSynth.playVent()
    }

    /**
     * Impostor trigger local sabotage alarm
     */
    fun triggerSabotage() {
        if (_sabotageActive.value) return
        _sabotageActive.value = true
        SoundSynth.playSabotageSiren()

        sabotageJob?.cancel()
        _sabotageTimer.value = 40
        sabotageJob = viewModelScope.launch {
            while (_sabotageTimer.value > 0) {
                if (!_sabotageActive.value) break
                // Replay the warning klaxon sound every 3 seconds
                if (_sabotageTimer.value % 3 == 0) {
                    SoundSynth.playSabotageSiren()
                }
                delay(1000)
                _sabotageTimer.value--
            }

            if (_sabotageActive.value) {
                // Time elapsed. Impostors win game!
                checkGameVictory(sabotageExplode = true)
            }
        }
    }

    fun resolveSabotageCode(enteredCode: String) {
        if (enteredCode == "1247" || enteredCode == "9988") { // Simulated bypass codes
            _sabotageActive.value = false
            SoundSynth.playTaskChime()
        } else {
            SoundSynth.playDefeat() // Wrong sound
        }
    }

    /**
     * Evaluate Victory conditions
     */
    private fun checkGameVictory(crewWin: Boolean = false, sabotageExplode: Boolean = false): Boolean {
        if (sabotageExplode) {
            _gamePhase.value = GamePhase.GAME_OVER
            _meetingResultText.value = "SABOTAGE DESTROYED LIFE SUPPORT! IMPOSTORS WIN!"
            SoundSynth.playDefeat()
            return true
        }

        if (crewWin) {
            _gamePhase.value = GamePhase.GAME_OVER
            _meetingResultText.value = "ALL SYSTEM TASKS REPLICATED! CREWMATE VICTORY!"
            SoundSynth.playVictory()
            return true
        }

        val alivePlayers = _players.value.filter { it.isAlive }
        val impostorsAlive = alivePlayers.count { it.role.isImpostorTeam }
        val crewmatesAlive = alivePlayers.count { !it.role.isImpostorTeam }

        if (impostorsAlive == 0) {
            _gamePhase.value = GamePhase.GAME_OVER
            _meetingResultText.value = "ALL IMPOSTORS WERE EXILED! CREWMATES WIN!"
            SoundSynth.playVictory()
            return true
        }

        if (impostorsAlive >= crewmatesAlive) {
            _gamePhase.value = GamePhase.GAME_OVER
            _meetingResultText.value = "IMPOSTORS OUTNUMBER THE CREW! IMPOSTORS WIN!"
            SoundSynth.playDefeat()
            return true
        }

        return false
    }

    fun resetGame() {
        meetingJob?.cancel()
        sabotageJob?.cancel()
        shapeshiftJob?.cancel()
        _gamePhase.value = GamePhase.SETUP
        _sabotageActive.value = false
        _isShapeshifted.value = false
        _shapeshiftColor.value = null
        loadDefaultPlayers()
        generateTasks()
    }
}
