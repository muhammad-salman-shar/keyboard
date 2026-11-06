package com.neura.keys.keys

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * Central router. Chooses between commitText, deleteSurroundingText, and full
 * sendKeyEvent(ACTION_DOWN + ACTION_UP) with meta-state bitmask.
 *
 * Rules:
 *  - Char keys: commitText unless Ctrl/Alt active, then KeyEvent shortcut.
 *  - Shift alone: commitText the shifted variant (safest on all text fields).
 *  - Function/Shortcut: KeyEvent with current meta.
 *  - Backspace: surrogate-pair safe delete, unless Ctrl/Alt active.
 */
class KeyDispatcher(
    private val modifierState: ModifierStateManager,
    private val getInputConnection: () -> InputConnection?
) {

    fun dispatch(key: Key) {
        when (key) {
            is Key.Char -> dispatchChar(key)
            is Key.Backspace -> dispatchBackspace()
            is Key.Enter -> dispatchEnter()
            is Key.Space -> dispatchSpace()
            is Key.Modifier -> modifierState.tap(key.kind)
            is Key.Function -> dispatchFunction(key)
            is Key.Shortcut -> dispatchShortcut(key)
            is Key.ModeSwitch -> Unit
        }
    }

    fun longPressModifier(kind: ModifierKind) {
        modifierState.longPress(kind)
    }

    private fun dispatchChar(key: Key.Char) {
        val ic = getInputConnection() ?: return
        val mods = modifierState.state.value

        if (mods.ctrl.isActive() || mods.alt.isActive()) {
            val keyCode = charToKeyCode(key.output)
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                sendKeyEvent(ic, keyCode, mods.toMetaState())
            } else {
                val text = if (mods.shift.isActive()) key.shiftedOutput ?: key.output else key.output
                ic.commitText(text, 1)
            }
            modifierState.consumeOneShot()
        } else if (mods.shift.isActive()) {
            val text = key.shiftedOutput ?: key.output
            ic.commitText(text, 1)
            modifierState.consumeOneShot()
        } else {
            ic.commitText(key.output, 1)
        }
    }

    private fun dispatchSpace() {
        val ic = getInputConnection() ?: return
        ic.commitText(" ", 1)
    }

    private fun dispatchEnter() {
        val ic = getInputConnection() ?: return
        val meta = modifierState.state.value.toMetaState()
        sendKeyEvent(ic, KeyEvent.KEYCODE_ENTER, meta)
        modifierState.consumeOneShot()
    }

    private fun dispatchBackspace() {
        val ic = getInputConnection() ?: return
        val mods = modifierState.state.value

        if (mods.ctrl.isActive() || mods.alt.isActive()) {
            sendKeyEvent(ic, KeyEvent.KEYCODE_DEL, mods.toMetaState())
            modifierState.consumeOneShot()
            return
        }

        val before = ic.getTextBeforeCursor(2, 0)
        val count = if (before != null && before.length >= 2 &&
            Character.isHighSurrogate(before[0]) &&
            Character.isLowSurrogate(before[1])
        ) 2 else 1
        ic.deleteSurroundingText(count, 0)
    }

    private fun dispatchFunction(key: Key.Function) {
        val ic = getInputConnection() ?: return
        val meta = modifierState.state.value.toMetaState()
        sendKeyEvent(ic, key.androidKeyCode, meta)
        modifierState.consumeOneShot()
    }

    private fun dispatchShortcut(key: Key.Shortcut) {
        val ic = getInputConnection() ?: return
        val meta = modifierState.state.value.toMetaState() or key.extraMeta
        sendKeyEvent(ic, key.androidKeyCode, meta)
        modifierState.consumeOneShot()
    }

    private fun sendKeyEvent(ic: InputConnection, keyCode: Int, meta: Int) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta)
        val up = KeyEvent(now, now + 1, KeyEvent.ACTION_UP, keyCode, 0, meta)
        ic.sendKeyEvent(down)
        ic.sendKeyEvent(up)
    }

    private fun charToKeyCode(text: String): Int {
        if (text.isEmpty()) return KeyEvent.KEYCODE_UNKNOWN
        val ch = text[0]
        return when {
            ch in 'a'..'z' -> KeyEvent.KEYCODE_A + (ch - 'a')
            ch in 'A'..'Z' -> KeyEvent.KEYCODE_A + (ch - 'A')
            ch in '0'..'9' -> KeyEvent.KEYCODE_0 + (ch - '0')
            ch == ' ' -> KeyEvent.KEYCODE_SPACE
            ch == '\n' -> KeyEvent.KEYCODE_ENTER
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
}
