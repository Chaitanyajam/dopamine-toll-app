package com.example.dopaminetoll

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class BlockerService : AccessibilityService(), androidx.lifecycle.LifecycleOwner, SavedStateRegistryOwner {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var overlayView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // Packages to IGNORE (System UI, etc)
    private val ignoredPackages = listOf(
        "com.android.systemui",
        "android",
        "com.google.android.inputmethod.latin", // GBoard (don't block keyboard)
        "com.samsung.android.honeyboard"        // Samsung Keyboard
    )

    private var currentBlockedPackage: String? = null
    private var isSessionUnlocked = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val eventPackageName = event.packageName?.toString() ?: return

            // 1. Ignore self and System UI
            if (eventPackageName == packageName) return
            if (ignoredPackages.contains(eventPackageName)) return

            // 2. Get Blocked List from Prefs
            val blockedPackages = getBlockedPackages()

            // 3. Blocking Logic
            if (blockedPackages.contains(eventPackageName)) {
                if (overlayView != null) return
                if (isSessionUnlocked && currentBlockedPackage == eventPackageName) return

                currentBlockedPackage = eventPackageName
                isSessionUnlocked = false
                showOverlay()
            } else {
                currentBlockedPackage = null
                isSessionUnlocked = false
                removeOverlay()
            }
        }
    }

    // --- UPDATED: Read from SharedPreferences ---
    private fun getBlockedPackages(): Set<String> {
        val prefs = getSharedPreferences("dopamine_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    }

    private fun getBlockedTasks(): List<String> {
        val prefs = getSharedPreferences("dopamine_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("tasks", emptySet())?.toList() ?: emptyList()
    }

    private fun getTimerDuration(): Int {
        val prefs = getSharedPreferences("dopamine_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("duration", 60)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BlockerService)
            setViewTreeSavedStateRegistryOwner(this@BlockerService)

            setContent {
                val currentTasks = getBlockedTasks()
                val currentDuration = getTimerDuration()

                DopamineOverlay(
                    tasks = currentTasks,
                    duration = currentDuration,
                    onUnlock = {
                        isSessionUnlocked = true
                        removeOverlay()
                    }
                )
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}