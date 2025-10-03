package com.permanentpaste.core;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;


/**
 * This class is responsible for monitoring the system clipboard in the background
 * and storing a history of copied text.
 */
public class ClipboardManager {

    private final List<String> clipboardHistory = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future<?> monitoringTask;
    private final AppSettings appSettings;
    private String lastSeenClipboardContent = "";

    public ClipboardManager(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    /**
     * Starts the background process of monitoring the clipboard.
     */
    public void startMonitoring() {
        //ukoliko nije aktivna ili je zavrsena ili je otkazana
        //tada mozes da palis
        if (monitoringTask == null || monitoringTask.isDone() || monitoringTask.isCancelled()) {
            //pokreni odmah i ranuj svaki sekund
            monitoringTask = scheduler.scheduleAtFixedRate(this::checkClipboard, 0, 1, TimeUnit.SECONDS);
            System.out.println("Clipboard monitoring started.");
        } else {
            System.out.println("Clipboard monitoring is already active.");
        }
    }
    /**
     * Stops the background process of monitoring the clipboard.
     */
    public void stopMonitoring() {
        //ukoliko je active
        if (monitoringTask != null) {
            //ugasi
            monitoringTask.cancel(false);
            //setuj na null
            monitoringTask = null;
            System.out.println("Clipboard monitoring stopped.");
        } else {
            System.out.println("Clipboard monitoring is not active.");
        }
    }

    public boolean isMonitoringActive() {
        //true if there is task, is ongoing and is not cancelled
        return monitoringTask != null && !monitoringTask.isDone() && !monitoringTask.isCancelled();
    }

    /**
     * This is the core method that runs every second to check the clipboard.
     */
    private void checkClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentContent = (String) clipboard.getData(DataFlavor.stringFlavor);

                if (currentContent != null && !currentContent.equals(lastSeenClipboardContent)) {
                    lastSeenClipboardContent = currentContent;
                    clipboardHistory.add(currentContent);
                    System.out.println("Item saved: " + currentContent);

                    while (clipboardHistory.size() > appSettings.getHistorySize()) {
                        clipboardHistory.remove(0); // Remove the oldest item from the front.
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Returns the current clipboard history as an array.
     */
    public String[] getClipboardHistory() {
        return clipboardHistory.toArray(new String[0]);
    }

    public void updateLastSeenClipboardContent(String content) {
        this.lastSeenClipboardContent = content;
    }
}