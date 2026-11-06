package com.neura.keys.keys

import android.view.inputmethod.InputConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service-scoped state holder. Owns the dispatcher + modifier state + current layout mode.
 * UI just observes the two StateFlows and calls onKeyPress().
 */
class KeyboardController(
    @Suppress("unused") private val scope: CoroutineScope,
    getInputConnection: () -> InputConnection?
) {
    private val modifierStateManager = ModifierStateManager()
    private val dispatcher = KeyDispatcher(modifierStateManager, getInputConnection)

    private val _mode = MutableStateFlow(KeyboardLayoutMode.STANDARD)
    val mode: StateFlow<KeyboardLayoutMode> = _mode.asStateFlow()

    val modifierState: StateFlow<ModifierSnapshot> = modifierStateManager.state

    fun onKeyPress(key: Key) {
        when (key) {
            is Key.ModeSwitch -> _mode.value = key.target
            else -> dispatcher.dispatch(key)
        }
    }

    fun onModifierLongPress(kind: ModifierKind) {
        dispatcher.longPressModifier(kind)
    }

    fun setMode(mode: KeyboardLayoutMode) {
        _mode.value = mode
    }
}
