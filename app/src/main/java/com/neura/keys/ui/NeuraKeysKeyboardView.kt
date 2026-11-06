package com.neura.keys.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neura.keys.keys.Key
import com.neura.keys.keys.KeyboardController
import com.neura.keys.keys.KeyboardLayoutMode
import com.neura.keys.keys.KeyboardLayouts
import com.neura.keys.keys.ModifierKind
import com.neura.keys.keys.ModifierSnapshot

object KeyboardColors {
    val Background = Color(0xFF1A1C20)
    val TopBarBg = Color(0xFF23262B)
    val KeyBg = Color(0xFF3A3D42)
    val KeyActionBg = Color(0xFF2E3136)
    val ModifierBg = Color(0xFF4A4D52)
    val ModifierActive = Color(0xFF4A90E2)
    val Accent = Color(0xFFF38229)
    val Text = Color(0xFFF5F5F5)
    val TextMuted = Color(0xFFB0B3B8)
}

@Composable
fun NeuraKeysKeyboard(
    controller: KeyboardController,
    modifier: Modifier = Modifier
) {
    val mode by controller.mode.collectAsState()
    val mods by controller.modifierState.collectAsState()
    val layout = KeyboardLayouts.get(mode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KeyboardColors.Background)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        SuggestionStrip()
        Spacer(Modifier.height(6.dp))
        ModeSwitcherBar(current = mode, onSelect = controller::setMode)
        Spacer(Modifier.height(6.dp))
        layout.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                row.keys.forEach { key ->
                    KeyView(
                        key = key,
                        mods = mods,
                        onPress = controller::onKeyPress,
                        onModifierLongPress = controller::onModifierLongPress
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KeyboardColors.TopBarBg)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SuggestionChip("AI", accent = true)
        SuggestionChip("...", accent = false)
        SuggestionChip("OK", accent = false)
        Spacer(Modifier.fillMaxWidth())
    }
}

@Composable
private fun SuggestionChip(text: String, accent: Boolean) {
    Text(
        text = text,
        color = if (accent) KeyboardColors.Accent else KeyboardColors.Text,
        fontSize = 15.sp,
        fontWeight = if (accent) FontWeight.Bold else FontWeight.Medium
    )
}

@Composable
private fun ModeSwitcherBar(
    current: KeyboardLayoutMode,
    onSelect: (KeyboardLayoutMode) -> Unit
) {
    val modes = listOf(
        KeyboardLayoutMode.STANDARD,
        KeyboardLayoutMode.PC_DEVELOPER,
        KeyboardLayoutMode.SYMBOLS,
        KeyboardLayoutMode.AI_PANEL
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { m ->
            val active = m == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) KeyboardColors.Accent else KeyboardColors.KeyActionBg)
                    .combinedClickable(onClick = { onSelect(m) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = m.displayName,
                    color = if (active) Color.Black else KeyboardColors.TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.KeyView(
    key: Key,
    mods: ModifierSnapshot,
    onPress: (Key) -> Unit,
    onModifierLongPress: (ModifierKind) -> Unit
) {
    val modifierKind = (key as? Key.Modifier)?.kind
    val isActiveModifier = modifierKind != null && mods.isActive(modifierKind)

    val bg = when {
        isActiveModifier -> KeyboardColors.ModifierActive
        key is Key.ModeSwitch -> KeyboardColors.Accent
        key is Key.Backspace || key is Key.Enter -> KeyboardColors.KeyActionBg
        key is Key.Modifier -> KeyboardColors.ModifierBg
        key is Key.Space -> KeyboardColors.KeyActionBg
        else -> KeyboardColors.KeyBg
    }

    val labelColor = when {
        key is Key.ModeSwitch -> Color.Black
        isActiveModifier -> Color.White
        else -> KeyboardColors.Text
    }

    val onClick: () -> Unit = { onPress(key) }
    val onLongClick: (() -> Unit)? = modifierKind?.let { kind ->
        { onModifierLongPress(kind) }
    }

    Box(
        modifier = Modifier
            .weight(key.weight)
            .padding(horizontal = 3.dp, vertical = 3.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            color = labelColor,
            fontSize = if (key is Key.Char) 20.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
