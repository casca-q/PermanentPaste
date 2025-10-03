package com.permanentpaste.ui;

import com.permanentpaste.core.ClipboardManager;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Robot;

public class PasteHistoryDialog extends JDialog {

    private final ClipboardManager clipboardManager;
    private final JList<String> historyList;
    private final DefaultListModel<String> listModel;
    private Runnable onDialogClosed;

    public PasteHistoryDialog(ClipboardManager clipboardManager) {
        this.clipboardManager = clipboardManager;
        this.listModel = new DefaultListModel<>();
        //list for display
        this.historyList = new JList<>(listModel);

        //removes standard formatting
        setUndecorated(true);
        //on top horizontally
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        historyList.setBackground(new Color(0x192129));
        historyList.setForeground(new Color(0xD709CCC1, true));
        historyList.setCellRenderer(new CustomCellRenderer());

        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xD709CCC1, true), 2));
        add(scrollPane, BorderLayout.CENTER);

        //listens to when we clicked somewhere else AFTER PASTING to hide, simulating real life paste
        //window
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                setVisible(false);
                if (onDialogClosed != null) {
                    onDialogClosed.run();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (onDialogClosed != null) {
                    onDialogClosed.run();
                }
            }
        });

        //listens to the clicks IN THE LIST to know when and what to paste
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    //get index of clicked item
                    int index = historyList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        //ako index je 0 ili jednako
                        //get it and paste it
                        String selectedText = listModel.getElementAt(index);
                        pasteText(selectedText);
                        setVisible(false);
                        if (onDialogClosed != null) {
                            onDialogClosed.run();
                        }
                    }
                }
            }
        });
    }

    public void showDialogWithClipboardInterception(int x, int y, String originalClipboardContent, Runnable onCloseCallback) {
        this.onDialogClosed = onCloseCallback;
        listModel.clear();

        // Add the original clipboard content at the top if it's not empty
        if (originalClipboardContent != null && !originalClipboardContent.trim().isEmpty()) {
            // Check if it's already in history to avoid duplicates
            boolean found = false;
            for (String item : clipboardManager.getClipboardHistory()) {
                if (item.equals(originalClipboardContent)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                listModel.addElement(originalClipboardContent + " (current)");
            }
        }

        //starts fresh with latest
        String[] history = clipboardManager.getClipboardHistory();
        //oldest is at 0
        for (int i = history.length - 1; i >= 0; i--) {
            listModel.addElement(history[i]);
        }

        if (listModel.isEmpty()) {
            listModel.addElement("History is empty.");
        }

        //resizes
        pack();
        //sets above mouse
        setLocation(x, y);
        setVisible(true);
        requestFocusInWindow();
    }

    private void pasteText(String text) {
        // Clean the text - remove " (current)" suffix if present
        String cleanText = text;
        if (text.endsWith(" (current)")) {
            cleanText = text.substring(0, text.length() - 11);
        }

        // Make cleanText final for lambda access
        final String finalCleanText = cleanText;

        // Place the selected text in clipboard
        clipboardManager.restoreClipboard(cleanText);
        clipboardManager.updateLastSeenClipboardContent(cleanText);

        setVisible(false);

        // Use Robot to simulate the actual paste action
        new Thread(() -> {
            try {
                Robot robot = new Robot();

                // Small delay to ensure clipboard is ready
                Thread.sleep(50);

                // Release any currently held modifier keys
                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_SHIFT);
                robot.keyRelease(KeyEvent.VK_ALT);
                robot.keyRelease(KeyEvent.VK_META);

                // Small delay before paste
                Thread.sleep(50);

                // Simulate Ctrl+V to paste the selected content
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);

                System.out.println("Pasted via clipboard interception: " +
                    (finalCleanText.length() > 50 ? finalCleanText.substring(0, 50) + "..." : finalCleanText));

            } catch (Exception e) {
                System.err.println("Error during paste simulation: " + e.getMessage());
                // Fallback: just put text in clipboard and let user paste manually
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(finalCleanText), null);
            }
        }).start();

        if (onDialogClosed != null) {
            onDialogClosed.run();
        }
    }

    private static class CustomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            if (isSelected) {
                label.setBackground(new Color(0xD709CCC1, true));
                label.setForeground(new Color(0x192129));
            }
            return label;
        }
    }
}