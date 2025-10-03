package com.permanentpaste.ui;

import com.permanentpaste.core.AppSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import com.permanentpaste.core.ClipboardManager;

public class ControlCubeFrame extends JFrame {

    private final AppSettings settings;
    private final ClipboardManager clipboardManager;

    public ControlCubeFrame(AppSettings settings, ClipboardManager clipboardManager) {
        this.settings = settings;
        this.clipboardManager = clipboardManager;

        setTitle("Permanent Paste");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        Font mainFont = loadAppFont("/AlienSpace.ttf", 14f);

        Color alienCyan = new Color(0xD709CCC1, true);
        Color retroBlack = new Color(0x0C1212);
        Color alienGray = new Color(0x192129);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(retroBlack);
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel imageLabel = new JLabel();
        try {
            URL imageUrl = getClass().getResource("/mainImage.png");
            if (imageUrl != null) {
                BufferedImage img = javax.imageio.ImageIO.read(imageUrl);

                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int rgba = img.getRGB(x, y);
                        Color col = new Color(rgba, true);

                        if ((col.getRed() > 200 && col.getGreen() > 200 && col.getBlue() > 200) ||
                                (Math.abs(col.getRed() - col.getGreen()) < 15 &&
                                        Math.abs(col.getGreen() - col.getBlue()) < 15 &&
                                        col.getRed() > 180)) {
                            img.setRGB(x, y, 0x00FFFFFF);
                        }
                    }
                }

                Image scaledImage = img.getScaledInstance(380, -1, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                imageLabel.setText("Image not found");
                imageLabel.setForeground(Color.CYAN.darker());
            }
        } catch (Exception e) {
            imageLabel.setText("Error loading image");
        }
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridy = 0;
        mainPanel.add(imageLabel, gbc);

        JToggleButton onOffToggle = new JToggleButton("Monitoring: ON");
        onOffToggle.setFont(mainFont);
        onOffToggle.setBackground(alienGray);
        onOffToggle.setForeground(alienCyan);
        onOffToggle.setSelected(true);
        gbc.gridy = 1;
        mainPanel.add(onOffToggle, gbc);

        JLabel historyLabel = new JLabel("History Size:", SwingConstants.CENTER);
        historyLabel.setFont(mainFont);
        historyLabel.setBackground(alienGray);
        historyLabel.setForeground(alienCyan);
        gbc.gridy = 2;
        mainPanel.add(historyLabel, gbc);

        Integer[] historySizes = {5, 7, 10, 15};
        JComboBox<Integer> historySizeDropdown = new JComboBox<>(historySizes);
        historySizeDropdown.setFont(mainFont);
        historySizeDropdown.setSelectedItem(settings.getHistorySize());

        DefaultListCellRenderer centerRenderer = new DefaultListCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        historySizeDropdown.setRenderer(centerRenderer);

        gbc.gridy = 3;
        mainPanel.add(historySizeDropdown, gbc);




        JCheckBox persistenceCheckbox = new JCheckBox("Remember history after restart");
        persistenceCheckbox.setFont(mainFont);
        persistenceCheckbox.setForeground(alienCyan);
        persistenceCheckbox.setOpaque(false);
        persistenceCheckbox.setSelected(settings.isPersistenceEnabled());
        gbc.gridy = 4;
        mainPanel.add(persistenceCheckbox, gbc);

        JCheckBox includeCutCheckbox = new JCheckBox("Include cut items (Ctrl+X)");
        includeCutCheckbox.setFont(mainFont);
        includeCutCheckbox.setForeground(alienCyan);
        includeCutCheckbox.setOpaque(false);
        includeCutCheckbox.setSelected(settings.isIncludeCutItemsEnabled());
        gbc.gridy = 5;
        mainPanel.add(includeCutCheckbox, gbc);

        JToggleButton saveButton = new JToggleButton("SAVE");
        saveButton.setFont(mainFont);
        saveButton.setBackground(alienGray);
        saveButton.setForeground(alienCyan);
        saveButton.setSelected(true);
        gbc.gridy = 6;
        mainPanel.add(saveButton, gbc);



        onOffToggle.addActionListener(e -> {
                    boolean isSelected = onOffToggle.isSelected();
                    onOffToggle.setText(isSelected ? "Monitoring: ON" : "Monitoring: OFF");
                    settings.setMonitoringEnabled(isSelected);
                    System.out.println("Monitoring toggled: " + isSelected);

                    if (isSelected) {
                        clipboardManager.startMonitoring();
                    } else {
                        clipboardManager.stopMonitoring();
                    }
                });

        historySizeDropdown.addActionListener(e -> {
            int selectedSize = (Integer) historySizeDropdown.getSelectedItem();
            settings.setHistorySize(selectedSize);
            System.out.println("History size set to: " + settings.getHistorySize());
        });

        persistenceCheckbox.addActionListener(e -> {
            settings.setPersistenceEnabled(persistenceCheckbox.isSelected());
            System.out.println("Persistence enabled: " + settings.isPersistenceEnabled());
        });

        includeCutCheckbox.addActionListener(e -> {
            settings.setIncludeCutItems(includeCutCheckbox.isSelected());
            System.out.println("Include cut items enabled: " + settings.isIncludeCutItemsEnabled());
        });
    }

    private Font loadAppFont(String resourcePath, float size) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) throw new Exception("Font file not found at: " + resourcePath);
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            return new Font("Monospaced", Font.PLAIN, (int) size);
        }
    }
}