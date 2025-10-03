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

public class Main implements NativeKeyListener {

    private AppSettings appSettings;
    private ClipboardManager clipboardManager;
    private PasteHistoryDialog pasteHistoryDialog;

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
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {

        // key number + 34 is non 0 in binary
        boolean isCtrlPressed = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        //same
        boolean isShiftPressed = (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) != 0;
        //calculation for both
        boolean isCtrlShiftV = e.getKeyCode() == NativeKeyEvent.VC_V && isCtrlPressed && isShiftPressed;

        //if its correct combination and if monitoring is enabled and its active
        if (isCtrlShiftV && appSettings.isMonitoringEnabled() && clipboardManager.isMonitoringActive()) {
            System.out.println("Ctrl+Shift+V detected!");

            //runs on EDT
            SwingUtilities.invokeLater(() -> {

                // Get the current coordinates of the mouse
                Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

                // Call the method to display our paste history dialog at the mouse's location
                pasteHistoryDialog.showDialog(mouseLocation.x, mouseLocation.y);
            });
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }
}