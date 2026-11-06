package com.neura.keys.service

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base class for NeuraKeys service.
 *
 * InputMethodService does NOT implement LifecycleOwner / ViewModelStoreOwner /
 * SavedStateRegistryOwner. Without these three, ComposeView crashes on popup
 * because Recomposer cannot find an owner in the view tree.
 *
 * This class bridges the gap safely:
 *  - Registry/Store/SavedState are created ONCE and reused across input-view
 *    recreations (system can call onCreateInputView multiple times per process).
 *  - Lifecycle states track IME visibility, not process lifetime.
 *  - DESTROYED fires exactly once (LifecycleRegistry requirement).
 *  - performRestore is guarded — a second call would throw IllegalStateException.
 */
abstract class LifecycleInputMethodService : InputMethodService(),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val ownedViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var restoredOnce = false

    /** Long-lived coroutine scope for keyboard-side work (AI, dictionary, timers). */
    val serviceScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = ownedViewModelStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        restoreSavedStateOnce()
        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        moveLifecycleTo(Lifecycle.State.RESUMED)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        moveLifecycleTo(Lifecycle.State.STARTED)
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        moveLifecycleTo(Lifecycle.State.STARTED)
        super.onWindowHidden()
    }

    override fun onDestroy() {
        try { serviceScope.cancel() } catch (_: Throwable) {}
        try { ownedViewModelStore.clear() } catch (_: Throwable) {}
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        super.onDestroy()
    }

    /**
     * Attach all three ViewTree owners to the root returned from onCreateInputView.
     * MUST be called on every fresh root view (system may recreate it).
     */
    protected fun attachComposeOwners(root: View) {
        restoreSavedStateOnce()
        ViewTreeLifecycleOwner.set(root, this)
        ViewTreeViewModelStoreOwner.set(root, this)
        ViewTreeSavedStateRegistryOwner.set(root, this)
    }

    private fun restoreSavedStateOnce() {
        if (restoredOnce) return
        try {
            savedStateRegistryController.performRestore(null as Bundle?)
        } catch (_: IllegalStateException) {
            // Already restored — safe. Happens if system calls onCreateInputView
            // before onCreate, or vice versa on odd OEMs.
        }
        restoredOnce = true
    }

    private fun moveLifecycleTo(target: Lifecycle.State) {
        val current = lifecycleRegistry.currentState
        if (current == Lifecycle.State.DESTROYED) return
        // LifecycleRegistry automatically walks intermediate states.
        lifecycleRegistry.currentState = target
    }
}
