package com.neura.keys.service

import android.view.View
import android.widget.FrameLayout
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
        val root = FrameLayout(this)
        val cv = ComposeView(this)
        cv.setContent {
            MaterialTheme {
                NeuraKeysKeyboard(controller = controller)
            }
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        root.addView(cv, lp)
        
        attachComposeOwners(root)
        composeView = cv
        return root
    }

    override fun onFinishInput() {
        super.onFinishInput()
    }

    override fun onDestroy() {
        composeView = null
        super.onDestroy()
    }
}
