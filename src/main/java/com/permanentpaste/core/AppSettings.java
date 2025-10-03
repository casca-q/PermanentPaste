package com.permanentpaste.core;

/**
 * This class holds all user-configurable settings for the application.
 * It follows the Single Responsibility Principle, as its only job is to manage
 * the application's configuration.
 *
 * In later phases, this class will also be responsible for saving these settings
 * to a file and loading them on startup.
 */
public class AppSettings {

    /**
     * If true, the clipboard history will be monitored from the moment user saves the configuration
     * If false, the history will not be monitored
     * Default is true.
     */
    private boolean isMonitoringEnabled = true;
    /**
     * The maximum number of items to store in the clipboard history.
     * Default is 5.
     */
    private int historySize = 5;

    /**
     * If true, the clipboard history will be saved when the app closes and reloaded
     * when it starts. If false, the history will be cleared on exit.
     * Default is true.
     */
    private boolean isPersistenceEnabled = true;

    /**
     * If true, the application will save items that are "cut" (Ctrl+X) in addition
     * to items that are "copied" (Ctrl+C).
     * Default is true.
     */
    private boolean includeCutItems = true;

    private boolean useInLinuxTerminal = false;

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    public boolean isPersistenceEnabled() {
        return isPersistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        isPersistenceEnabled = persistenceEnabled;
    }

    public boolean isIncludeCutItemsEnabled() {
        return includeCutItems;
    }

    public void setIncludeCutItems(boolean includeCutItems) {
        this.includeCutItems = includeCutItems;
    }


    public boolean isUseInLinuxTerminal() {
        return useInLinuxTerminal;
    }

    public void setUseInLinuxTerminal(boolean useInLinuxTerminal) {
        this.useInLinuxTerminal = useInLinuxTerminal;
    }


    public boolean isMonitoringEnabled() {
        return isMonitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        isMonitoringEnabled = monitoringEnabled;
    }

    /**
     * This method will eventually save the current settings to a file (e.g., JSON).
     * We will implement this in a later phase.
     */
    public void saveSettingsToFile() {
        System.out.println("Saving settings to file... (Not implemented yet)");
    }

    /**
     * This method will eventually load settings from a file when the app starts.
     * We will implement this in a later phase.
     */
    public void loadSettingsFromFile() {
        System.out.println("Loading settings from file... (Not implemented yet)");
    }

}
