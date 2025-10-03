package com.permanentpaste;

import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.permanentpaste.core.AppSettings;
import com.permanentpaste.core.ClipboardManager;
import com.permanentpaste.ui.ControlCubeFrame;
import com.permanentpaste.ui.PasteHistoryDialog;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.Robot;
import java.awt.AWTException;

public class Main implements NativeKeyListener {

    private AppSettings appSettings;
    private ClipboardManager clipboardManager;
    private PasteHistoryDialog pasteHistoryDialog;

    // Performance-optimized double Shift detection state
    private long lastShiftPressTime = 0L;
    private int consecutiveShiftPresses = 0;
    private boolean doubleShiftTriggered = false;

    // Timing constants (using static final for compiler optimization)
    private static final long DOUBLE_SHIFT_THRESHOLD = 400L; // 400ms window
    private static final long DEBOUNCE_THRESHOLD = 50L;     // 50ms debounce
    private static final long RESET_TIMEOUT = 1000L;        // 1s reset timeout
    private static final int MAX_SHIFT_PRESSES = 3;          // Prevent excessive detection

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        Main app = new Main();
        app.start();
    }

    public void start() {
        appSettings = new AppSettings();
        clipboardManager = new ClipboardManager(appSettings);
        pasteHistoryDialog = new PasteHistoryDialog(clipboardManager);

        //load SAVED settings
        appSettings.loadSettingsFromFile();
        //if monitoring is ON start monitoring
        if (appSettings.isMonitoringEnabled()) {
            clipboardManager.startMonitoring();
        }else {
            System.out.println("Monitoring is initially disabled by settings.");
        }

        //register, turn on
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        //listen to its actions by class in which it is implemented(main)
        GlobalScreen.addNativeKeyListener(this);

        SwingUtilities.invokeLater(() -> {
            ControlCubeFrame controlCubeFrame = new ControlCubeFrame(appSettings, clipboardManager);
            //show the UI
            controlCubeFrame.setVisible(true);

            controlCubeFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                //when window closing
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    try {
                        //unregister, turn off
                        GlobalScreen.unregisterNativeHook();
                    } catch (NativeHookException e) {
                        System.err.println("Failed to unregister native hook: " + e.getMessage());
                    }
                    //saving if changes in UI when the app closes, if there was changes in the session
                    appSettings.saveSettingsToFile();
                    clipboardManager.stopMonitoring();

                    System.exit(0);
                }
            });
        });
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Performance-optimized double Shift detection
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            handleShiftKeyPress();
        } else {
            // Reset state on any other key press for cleaner behavior
            resetDoubleShiftState();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Key release logic is now handled in nativeKeyPressed for better timing
        // This method is kept empty but required for interface implementation
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }

    /**
     * LAYER 1: Immediately block the Shift key at hardware level
     * to prevent double Shift from affecting other applications
     */
    private void blockKeyCombinationImmediately() {
        try {
            Robot robot = new Robot();

            // Immediately release Shift key to prevent the double press from propagating
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.keyRelease(KeyEvent.VK_META);

            System.out.println("Hardware-level Shift key blocking applied");
        } catch (AWTException e) {
            System.err.println("Failed to create robot for key blocking: " + e.getMessage());
        }
    }

    /**
     * LAYER 2: Perform multi-pass clipboard clearing to ensure complete interception
     */
    private String performMultiPassClipboardLock() {
        String originalContent = "";

        try {
            // Use enhanced force lock with built-in verification
            originalContent = clipboardManager.forceLockClipboard();

            // Additional manual passes for ultra-fast applications
            Robot robot = new Robot();

            for (int i = 0; i < 2; i++) {
                robot.delay(2); // 2ms delay - shorter than before for speed
                clipboardManager.lockClipboard(); // Clear again
            }

            System.out.println("Enhanced multi-pass clipboard clearing completed");
        } catch (Exception e) {
            System.err.println("Error in enhanced multi-pass clipboard lock: " + e.getMessage());
        }

        return originalContent;
    }

    /**
     * LAYER 3: Show dialog with atomic operations to prevent timing gaps
     */
    private void performAtomicDialogDisplay(String originalClipboardContent) {
        // Use SwingUtilities.invokeLater to ensure UI operations are on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Get the current coordinates of the mouse
                Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

                // Call the method to display our paste history dialog at the mouse's location
                // with the original clipboard content as backup
                pasteHistoryDialog.showDialogWithClipboardInterception(
                    mouseLocation.x, mouseLocation.y, originalClipboardContent, () -> {
                        System.out.println("Dialog closed - enhanced clipboard interception complete");
                    });
            } catch (Exception e) {
                System.err.println("Error showing dialog: " + e.getMessage());
                // Fallback: restore clipboard if dialog fails
                clipboardManager.restoreClipboard(originalClipboardContent);
            }
        });
    }

    /**
     * Performance-optimized double Shift detection handler
     */
    private void handleShiftKeyPress() {
        long currentTime = System.currentTimeMillis();

        // Early exit for maximum performance - check conditions in order of likelihood
        if (doubleShiftTriggered) {
            return; // Already triggered, ignore additional Shift presses
        }

        // Debounce: ignore very rapid successive presses (key bounce)
        if (currentTime - lastShiftPressTime < DEBOUNCE_THRESHOLD) {
            return;
        }

        // Check if this is within the double press window
        if (currentTime - lastShiftPressTime <= DOUBLE_SHIFT_THRESHOLD) {
            consecutiveShiftPresses++;

            // Trigger on exactly 2 presses (double Shift)
            if (consecutiveShiftPresses == 2) {
                triggerDoubleShiftInterception();
                return; // Early exit to prevent additional processing
            }

            // Safety: prevent excessive consecutive presses
            if (consecutiveShiftPresses >= MAX_SHIFT_PRESSES) {
                resetDoubleShiftState();
                return;
            }
        } else {
            // Outside window, start new sequence
            consecutiveShiftPresses = 1;
        }

        lastShiftPressTime = currentTime;

        // Schedule automatic reset for cleanup (performance-optimized)
        scheduleResetTimeout();
    }

    /**
     * Trigger the enhanced multi-layer interception on double Shift detection
     */
    private void triggerDoubleShiftInterception() {
        if (!appSettings.isMonitoringEnabled() || !clipboardManager.isMonitoringActive()) {
            resetDoubleShiftState();
            return;
        }

        System.out.println("Double Shift detected - using enhanced multi-layer interception!");
        doubleShiftTriggered = true;

        // Reuse existing enhanced multi-layer interception
        blockKeyCombinationImmediately();
        String originalClipboardContent = performMultiPassClipboardLock();
        performAtomicDialogDisplay(originalClipboardContent);

        // Reset state after successful trigger
        resetDoubleShiftState();
    }

    /**
     * Performance-optimized state reset
     */
    private void resetDoubleShiftState() {
        lastShiftPressTime = 0L;
        consecutiveShiftPresses = 0;
        doubleShiftTriggered = false;
    }

    /**
     * Schedule automatic reset with minimal overhead
     */
    private void scheduleResetTimeout() {
        // Only schedule if not already scheduled to prevent multiple timers
        if (consecutiveShiftPresses == 1) {
            javax.swing.Timer resetTimer = new javax.swing.Timer((int) RESET_TIMEOUT, e -> resetDoubleShiftState());
            resetTimer.setRepeats(false);
            resetTimer.start();
        }
    }
}