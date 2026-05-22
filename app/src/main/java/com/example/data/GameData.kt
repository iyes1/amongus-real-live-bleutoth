package com.example.data

import androidx.compose.ui.graphics.Color

/**
 * Constants representing available game roles.
 */
enum class Role(
    val displayName: String,
    val isImpostorTeam: Boolean,
    val description: String,
    val allianceColor: Color,
    val abilityName: String,
    val abilityDescription: String
) {
    CREWMATE(
        displayName = "Crewmate",
        isImpostorTeam = false,
        description = "Find the Impostor. Complete real-world tasks and resolve critical sabotages.",
        allianceColor = Color(0xFF00E5FF),
        abilityName = "Task Radar",
        abilityDescription = "Scan surrounding terminals to locate tasks."
    ),
    IMPOSTOR(
        displayName = "Impostor",
        isImpostorTeam = true,
        description = "Secretly dispatch crewmates. Trigger critical sabotages and frame innocent crew.",
        allianceColor = Color(0xFFFF124F),
        abilityName = "Sabotage Console",
        abilityDescription = "De-energize lights, spark warnings, and compromise structural vitals."
    ),
    SCIENTIST(
        displayName = "Scientist",
        isImpostorTeam = false,
        description = "Monitor biometric telemetry of all players in real-time.",
        allianceColor = Color(0xFFE040FB),
        abilityName = "Vitals Tracker",
        abilityDescription = "Inspect ECG status of the crew. Detect instantly if anyone flatlines."
    ),
    ENGINEER(
        displayName = "Engineer",
        isImpostorTeam = false,
        description = "Navigate through physical vent paths to travel faster.",
        allianceColor = Color(0xFFFFD600),
        abilityName = "Vent CCTV Network",
        abilityDescription = "Travel virtually through duct chambers and review CCTV camera feeds."
    ),
    SHAPESHIFTER(
        displayName = "Shapeshifter",
        isImpostorTeam = true,
        description = "Change appearances to impersonate any alive crewmate.",
        allianceColor = Color(0xFF04FF88),
        abilityName = "Mimic Matrix",
        abilityDescription = "Shift physical color rendering and signature to confuse onlookers."
    ),
    GUARDIAN_ANGEL(
        displayName = "Guardian Angel",
        isImpostorTeam = false,
        description = "Cast defensive shields from beyond to cover living crewmates.",
        allianceColor = Color(0xFF90CAF9),
        abilityName = "Ethereal Barrier",
        abilityDescription = "Shield an active target crewmate, deflecting the next sabotage/kill intent."
    )
}

/**
 * Game phases representing the state machine.
 */
enum class GamePhase {
    SETUP,
    ROLE_REVEAL,
    ACTIVE_GAME,
    EMERGENCY_MEETING,
    GAME_OVER
}

/**
 * Structural model representing a Crewmate Player.
 */
data class Player(
    val id: String,
    val name: String,
    val colorName: String,
    val colorHex: Color,
    val role: Role = Role.CREWMATE,
    val isAlive: Boolean = true,
    val isProtected: Boolean = false,
    val isSimulatedBluetooth: Boolean = false,
    val bluetoothSignalDb: Int = -60
)

/**
 * Custom Model representing unique real-world tasks.
 */
data class GameTask(
    val id: String,
    val title: String,
    val realLocation: String,
    val minigameType: MinigameType,
    val isCompleted: Boolean = false
)

enum class MinigameType {
    SWIPE_CARD,
    CONNECT_WIRES,
    CALIBRATE_DISTRIBUTOR,
    HOLD_DOWNLOAD,
    SEEK_FREQUENCY
}

/**
 * Colors configuration for players.
 */
object CrewColors {
    val list = listOf(
        Pair("Red", Color(0xFFFF1744)),
        Pair("Cyan", Color(0xFF00E5FF)),
        Pair("Green", Color(0xFF00E676)),
        Pair("Orange", Color(0xFFFF9100)),
        Pair("Purple", Color(0xFFD500F9)),
        Pair("Pink", Color(0xFFF50057)),
        Pair("Yellow", Color(0xFFFFEA00)),
        Pair("Blue", Color(0xFF2979FF)),
        Pair("Lime", Color(0xFFAEEA00)),
        Pair("White", Color(0xFFECEFF1))
    )
}
