package com.neura.keys.service

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import com.neura.keys.keys.KeyboardController
import com.neura.keys.ui.NeuraKeysKeyboard

/**
 * Phase 1 IME.
 * Owns a service-scoped [KeyboardController]; renders NeuraKeysKeyboard in a ComposeView.
 * AI + native dictionary hooks connect here in later phases.
 */
class NeuraKeysService : LifecycleInputMethodService() {

    private lateinit var controller: KeyboardController
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        controller = KeyboardController(
            scope = serviceScope,
            getInputConnection = { currentInputConnection }
        )
    }

    override fun onCreateInputView(): View {
        val cv = ComposeView(this)
        attachComposeOwners(cv)
        cv.setContent {
            MaterialTheme {
                NeuraKeysKeyboard(controller = controller)
            }
        }
        composeView = cv
        return cv
    }

    override fun onFinishInput() {
        super.onFinishInput()
    }

    override fun onDestroy() {
        composeView = null
        super.onDestroy()
    }
}
