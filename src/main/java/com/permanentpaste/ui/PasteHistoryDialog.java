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

public class PasteHistoryDialog extends JDialog {

    private final ClipboardManager clipboardManager;
    private final JList<String> historyList;
    private final DefaultListModel<String> listModel;

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
                    }
                }
            }
        });
    }

    //method is to show you dropdown list of copied available to paste
    public void showDialog(int x, int y) {
        listModel.clear();
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
        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        clipboardManager.updateLastSeenClipboardContent(text);

        setVisible(false);

        new Thread(() -> {
//                Thread.sleep(100);

            Robot robot = null;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }

            robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_SHIFT);

//                Thread.sleep(100);

                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);

        }).start();
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