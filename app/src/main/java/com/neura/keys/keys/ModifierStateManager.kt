package com.neura.keys.keys

import android.os.SystemClock
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ModifierStatus { INACTIVE, ONE_SHOT, LOCKED }

data class ModifierSnapshot(
    val ctrl: ModifierStatus = ModifierStatus.INACTIVE,
    val alt: ModifierStatus = ModifierStatus.INACTIVE,
    val shift: ModifierStatus = ModifierStatus.INACTIVE
) {
    fun get(kind: ModifierKind): ModifierStatus = when (kind) {
        ModifierKind.CTRL -> ctrl
        ModifierKind.ALT -> alt
        ModifierKind.SHIFT -> shift
    }

    fun with(kind: ModifierKind, status: ModifierStatus): ModifierSnapshot = when (kind) {
        ModifierKind.CTRL -> copy(ctrl = status)
        ModifierKind.ALT -> copy(alt = status)
        ModifierKind.SHIFT -> copy(shift = status)
    }

    fun isActive(kind: ModifierKind): Boolean = get(kind) != ModifierStatus.INACTIVE

    fun isAnyActive(): Boolean =
        ctrl != ModifierStatus.INACTIVE ||
            alt != ModifierStatus.INACTIVE ||
            shift != ModifierStatus.INACTIVE

    fun toMetaState(): Int {
        var meta = 0
        if (ctrl != ModifierStatus.INACTIVE) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt != ModifierStatus.INACTIVE) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shift != ModifierStatus.INACTIVE) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        return meta
    }
}

/**
 * Tri-state modifier logic.
 * - Tap: INACTIVE -> ONE_SHOT; ONE_SHOT + fast tap -> LOCKED; ONE_SHOT + slow tap -> INACTIVE; LOCKED + tap -> INACTIVE.
 * - Long press: force LOCKED from any state.
 * - consumeOneShot(): called after any key press consumes one-shot modifiers.
 */
class ModifierStateManager {
    private val _state = MutableStateFlow(ModifierSnapshot())
    val state: StateFlow<ModifierSnapshot> = _state.asStateFlow()

    private var lastTapAt: Long = 0L

    fun tap(kind: ModifierKind) {
        val now = SystemClock.uptimeMillis()
        val delta = now - lastTapAt
        lastTapAt = now
        _state.update { snap ->
            val current = snap.get(kind)
            val next = when (current) {
                ModifierStatus.INACTIVE -> ModifierStatus.ONE_SHOT
                ModifierStatus.ONE_SHOT ->
                    if (delta in 1..DOUBLE_TAP_WINDOW_MS) ModifierStatus.LOCKED else ModifierStatus.INACTIVE
                ModifierStatus.LOCKED -> ModifierStatus.INACTIVE
            }
            snap.with(kind, next)
        }
    }

    fun longPress(kind: ModifierKind) {
        _state.update { snap -> snap.with(kind, ModifierStatus.LOCKED) }
    }

    fun consumeOneShot() {
        _state.update { snap ->
            var next = snap
            if (next.ctrl == ModifierStatus.ONE_SHOT) next = next.with(ModifierKind.CTRL, ModifierStatus.INACTIVE)
            if (next.alt == ModifierStatus.ONE_SHOT) next = next.with(ModifierKind.ALT, ModifierStatus.INACTIVE)
            if (next.shift == ModifierStatus.ONE_SHOT) next = next.with(ModifierKind.SHIFT, ModifierStatus.INACTIVE)
            next
        }
    }

    fun clear() {
        _state.value = ModifierSnapshot()
    }

    companion object {
        const val DOUBLE_TAP_WINDOW_MS = 350L
    }
}


fun ModifierStatus.isActive(): Boolean = this != ModifierStatus.INACTIVE
