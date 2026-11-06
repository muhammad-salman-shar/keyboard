package com.neura.keys.keys

enum class KeyboardLayoutMode(val displayName: String) {
    STANDARD("ABC"),
    PC_DEVELOPER("PC"),
    SYMBOLS("?123"),
    AI_PANEL("AI")
}

enum class ModifierKind(val label: String) {
    CTRL("Ctrl"),
    ALT("Alt"),
    SHIFT("Shift")
}

/**
 * Sealed key hierarchy — data-driven, JSON-ready.
 * Add rows to [KeyboardLayouts] to extend; dispatcher picks the right path per type.
 */
sealed interface Key {
    val id: String
    val label: String
    val weight: Float

    data class Char(
        override val id: String,
        override val label: String,
        override val weight: Float = 1f,
        val output: String = label,
        val shiftedOutput: String? = null
    ) : Key

    data class Backspace(
        override val id: String = "backspace",
        override val label: String = "\u232B",
        override val weight: Float = 1.5f
    ) : Key

    data class Enter(
        override val id: String = "enter",
        override val label: String = "\u21B5",
        override val weight: Float = 1.5f
    ) : Key

    data class Space(
        override val id: String = "space",
        override val label: String = "space",
        override val weight: Float = 4f
    ) : Key

    data class Modifier(
        override val id: String,
        override val label: String,
        override val weight: Float = 1.2f,
        val kind: ModifierKind
    ) : Key

    data class Function(
        override val id: String,
        override val label: String,
        override val weight: Float = 1f,
        val androidKeyCode: Int
    ) : Key

    data class ModeSwitch(
        override val id: String,
        override val label: String,
        override val weight: Float = 1.5f,
        val target: KeyboardLayoutMode
    ) : Key

    /** Hardcoded shortcut (e.g. Ctrl+C). Combines extraMeta with live modifier state. */
    data class Shortcut(
        override val id: String,
        override val label: String,
        override val weight: Float = 1f,
        val androidKeyCode: Int,
        val extraMeta: Int
    ) : Key
}

data class KeyRow(val keys: List<Key>)

data class KeyboardLayout(
    val mode: KeyboardLayoutMode,
    val rows: List<KeyRow>
)

/**
 * Starter layouts. Add more keys by extending the rows below.
 * Test pipeline first, then expand.
 */
object KeyboardLayouts {

    val STANDARD = KeyboardLayout(
        mode = KeyboardLayoutMode.STANDARD,
        rows = listOf(
            KeyRow(listOf(
                Key.Char(id = "q", label = "q", shiftedOutput = "Q"),
                Key.Char(id = "w", label = "w", shiftedOutput = "W"),
                Key.Char(id = "e", label = "e", shiftedOutput = "E"),
                Key.Char(id = "r", label = "r", shiftedOutput = "R"),
                Key.Char(id = "t", label = "t", shiftedOutput = "T")
            )),
            KeyRow(listOf(
                Key.Modifier(id = "shift", label = "\u21E7", kind = ModifierKind.SHIFT),
                Key.Char(id = "a", label = "a", shiftedOutput = "A"),
                Key.Char(id = "s", label = "s", shiftedOutput = "S"),
                Key.Char(id = "d", label = "d", shiftedOutput = "D"),
                Key.Backspace()
            )),
            KeyRow(listOf(
                Key.ModeSwitch(id = "to_pc", label = "PC", target = KeyboardLayoutMode.PC_DEVELOPER),
                Key.Space(),
                Key.Enter()
            ))
        )
    )

    val PC_DEVELOPER = KeyboardLayout(
        mode = KeyboardLayoutMode.PC_DEVELOPER,
        rows = listOf(
            KeyRow(listOf(
                Key.Function(id = "esc", label = "Esc", androidKeyCode = android.view.KeyEvent.KEYCODE_ESCAPE),
                Key.Function(id = "tab", label = "Tab", androidKeyCode = android.view.KeyEvent.KEYCODE_TAB),
                Key.Function(id = "f1", label = "F1", androidKeyCode = android.view.KeyEvent.KEYCODE_F1),
                Key.Function(id = "f2", label = "F2", androidKeyCode = android.view.KeyEvent.KEYCODE_F2),
                Key.Backspace()
            )),
            KeyRow(listOf(
                Key.Modifier(id = "ctrl", label = "Ctrl", weight = 1.4f, kind = ModifierKind.CTRL),
                Key.Modifier(id = "alt", label = "Alt", weight = 1.4f, kind = ModifierKind.ALT),
                Key.Modifier(id = "shift", label = "\u21E7", weight = 1.2f, kind = ModifierKind.SHIFT),
                Key.Char(id = "a", label = "a", shiftedOutput = "A"),
                Key.Char(id = "c", label = "c", shiftedOutput = "C"),
                Key.Char(id = "v", label = "v", shiftedOutput = "V"),
                Key.Char(id = "x", label = "x", shiftedOutput = "X"),
                Key.Char(id = "z", label = "z", shiftedOutput = "Z")
            )),
            KeyRow(listOf(
                Key.ModeSwitch(id = "to_std", label = "ABC", target = KeyboardLayoutMode.STANDARD),
                Key.Space(),
                Key.Enter()
            ))
        )
    )

    fun get(mode: KeyboardLayoutMode): KeyboardLayout = when (mode) {
        KeyboardLayoutMode.STANDARD -> STANDARD
        KeyboardLayoutMode.PC_DEVELOPER -> PC_DEVELOPER
        KeyboardLayoutMode.SYMBOLS -> STANDARD
        KeyboardLayoutMode.AI_PANEL -> STANDARD
    }
}
