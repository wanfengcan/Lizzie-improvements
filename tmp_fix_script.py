# -*- coding: utf-8 -*-
import os

content = r'''package featurecat.lizzie.foxgo;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.util.Utils;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Provides a crosshair magnifier overlay for visually calibrating board corner positions. Both
 * "Pick Top-Left" and "Pick Bottom-Right" open a floating magnifier window.
 */
public class BoardAligner extends JDialog {
  private static final int MAG_SIZE = 160; // magnified area size in pixels
  private static final int GRID = 11; // 11x11 pixel grid
  private static final double SCALE = MAG_SIZE / (double) GRID; // ~14.545
  private static final int WIN_W = MAG_SIZE + 40;
  private static final int WIN_H = MAG_SIZE + 60;

  private JTextField topXField;
  private JTextField topYField;
  private JTextField bottomXField;
  private JTextField bottomYField;
  private JLabel statusLabel;
  private JButton startBtn;
  private JButton stopBtn;

  private final FoxGoBridge bridge;

  // ======================== Constructor ========================

  public BoardAligner(FoxGoBridge bridge) {
    super(Lizzie.frame, "\u68cb\u76d8\u6821\u51c6", true);
    this.bridge = bridge;
    initUI();
    loadConfig();
    pack();
    setLocationRelativeTo(Lizzie.frame);
    setResizable(false);
  }

  // ======================== UI Setup ========================

  private void initUI() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

    // Header area
    JPanel header = new JPanel(new BorderLayout());
    JLabel title = new JLabel("Fox Go AI \u6821\u51c6");
    title.setFont(new Font("SansSerif", Font.BOLD, 16));
    header.add(title, BorderLayout.WEST);

    statusLabel = new JLabel("\u7b49\u5f85\u6821\u51c6");
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
    statusLabel.setForeground(Color.GRAY);
    header.add(statusLabel, BorderLayout.EAST);
    mainPanel.add(header, BorderLayout.NORTH);

    // Form area
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Instructions
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    JLabel instructions =
        new JLabel(
            "<html>\u5728 Fox Go \u68cb\u76d8\u4e0a\uff0c\u8bf7\u5148\u6821\u51c6\u4e24\u4e2a\u89d2\u70b9\u3002<br>"
                + "\u70b9\u51fb\u201c\u9009\u53d6\u5de6\u4e0a\u89d2\u201d\u6216\u201c\u9009\u53d6\u53f3\u4e0b\u89d2\u201d\u6309\u94ae\u6253\u5f00\u653e\u5927\u955c\uff0c\u518d\u70b9\u51fb\u9009\u62e9\u5bf9\u5e94\u7684\u5750\u6807\u70b9\u3002<br>"
                + "\u6309 ESC \u53d6\u6d88\u9009\u62e9</html>");
    instructions.setFont(new Font("SansSerif", Font.PLAIN, 12));
    form.add(instructions, gbc);

    gbc.gridwidth = 1;

    // Top-Left row
    gbc.gridy = 1;
    gbc.gridx = 0;
    form.add(new JLabel("\u5de6\u4e0a\u89d2 X:"), gbc);
    gbc.gridx = 1;
    topXField = new JTextField(6);
    form.add(topXField, gbc);
    gbc.gridx = 2;
    form.add(new JLabel("Y:"), gbc);
    gbc.gridx = 3;
    topYField = new JTextField(6);
    form.add(topYField, gbc);

    // Bottom-Right row
    gbc.gridy = 2;
    gbc.gridx = 0;
    form.add(new JLabel("\u53f3\u4e0b\u89d2 X:"), gbc);
    gbc.gridx = 1;
    bottomXField = new JTextField(6);
    form.add(bottomXField, gbc);
    gbc.gridx = 2;
    form.add(new JLabel("Y:"), gbc);
    gbc.gridx = 3;
    bottomYField = new JTextField(6);
    form.add(bottomYField, gbc);

    // Buttons row
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    JButton pickTopBtn = new JButton("\u9009\u53d6\u5de6\u4e0a\u89d2");
    pickTopBtn.addActionListener(e -> pickPosition(true));
    form.add(pickTopBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    JButton pickBottomBtn = new JButton("\u9009\u53d6\u53f3\u4e0b\u89d2");
    pickBottomBtn.addActionListener(e -> pickPosition(false));
    form.add(pickBottomBtn, gbc);

    gbc.gridy = 4;
    gbc.gridx = 0;
    gbc.gridwidth = 4;
    JButton showBtn = new JButton("\u663e\u793a\u68cb\u76d8\u8f6e\u5ed3");
    showBtn.addActionListener(e -> showBoardContour());
    form.add(showBtn, gbc);

    gbc.gridy = 5;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    startBtn = new JButton("\u5f00\u59cb\u540c\u6b65");
    startBtn.addActionListener(e -> startSync());
    form.add(startBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    stopBtn = new JButton("\u505c\u6b62\u540c\u6b65");
    stopBtn.addActionListener(e -> stopSync());
    stopBtn.setEnabled(false);
    form.add(stopBtn, gbc);

    gbc.gridy = 6;
    gbc.gridx = 0;
    gbc.gridwidth = 4;
    JButton hideBtn = new JButton("\u9690\u85cf\u8f6e\u5ed3");
    hideBtn.addActionListener(e -> bridge.getOverlayWindow().hideOverlay());
    form.add(hideBtn, gbc);

    gbc.gridy = 7;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    JButton saveBtn = new JButton("\u4fdd\u5b58\u6821\u51c6");
    saveBtn.addActionListener(e -> saveConfig());
    form.add(saveBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    JButton closeBtn = new JButton("\u5173\u95ed");
    closeBtn.addActionListener(e -> dispose());
    form.add(closeBtn, gbc);

    mainPanel.add(form, BorderLayout.CENTER);
    add(mainPanel);
  }

  // ======================== Magnifier pick ========================

  /**
   * Opens a floating magnifier window that shows an 11x11 zoomed pixel grid around the cursor.
   * Click anywhere on the magnifier to pick the clicked pixel's screen coordinates. Press ESC to
   * cancel.
   *
   * @param isLeft true to set top-left fields; false to set bottom-right fields
   */
  private void pickPosition(boolean isLeft) {
    Robot robot;
    try {
      robot = new Robot();
    } catch (AWTException ex) {
      ex.printStackTrace();
      Utils.showMessageDialog(this, "\u65e0\u6cd5\u521b\u5efa\u5c4f\u5e55\u6293\u53d6\u5bf9\u8c61\u3002");
      return;
    }

    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    JWindow magWindow = new JWindow();
    magWindow.setAlwaysOnTop(true);
    magWindow.setSize(WIN_W, WIN_H);
    magWindow.setBackground(new Color(0, 0, 0, 200)); // translucent background

    // info label (top)
    JLabel infoLabel = new JLabel("RGB(?,?,?) (?,?)", SwingConstants.CENTER);
    infoLabel.setForeground(new Color(200, 255, 200));
    infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
    infoLabel.setBackground(new Color(30, 30, 30, 220));
    infoLabel.setOpaque(true);

    // hint label (bottom)
    JLabel hintLabel = new JLabel("\u70b9\u51fb\u9009\u62e9\u50cf\u7d20 | ESC \u53d6\u6d88", SwingConstants.CENTER);
    hintLabel.setForeground(Color.LIGHT_GRAY);
    hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
    hintLabel.setBackground(new Color(30, 30, 30, 220));
    hintLabel.setOpaque(true);

    // pixel buffer (121 = 11x11)
    int[] pixelBuffer = new int[GRID * GRID];

    // magnifier panel
    JPanel magPanel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw each pixel as a scaled rectangle
            for (int row = 0; row < GRID; row++) {
              for (int col = 0; col < GRID; col++) {
                int idx = row * GRID + col;
                int argb = pixelBuffer[idx];
                if ((argb & 0xFF000000) != 0) {
                  g2.setColor(new Color(argb));
                  g2.fillRect(
                      (int) (col * SCALE),
                      (int) (row * SCALE),
                      (int) Math.ceil(SCALE),
                      (int) Math.ceil(SCALE));
                }
              }
            }

            // Grid lines
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i <= GRID; i++) {
              int pos = (int) (i * SCALE);
              g2.drawLine(pos, 0, pos, MAG_SIZE);
              g2.drawLine(0, pos, MAG_SIZE, pos);
            }

            // Center crosshair
            int cx = MAG_SIZE / 2;
            int cy = MAG_SIZE / 2;
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(cx - 8, cy, cx + 8, cy);
            g2.drawLine(cx, cy - 8, cx, cy + 8);
          }
        };
    magPanel.setPreferredSize(new Dimension(MAG_SIZE, MAG_SIZE));
    magPanel.setBackground(Color.BLACK);
    magPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 200, 100), 2));

    // Layout
    magWindow.setLayout(new BorderLayout(0, 0));
    magWindow.add(infoLabel, BorderLayout.NORTH);
    magWindow.add(magPanel, BorderLayout.CENTER);
    magWindow.add(hintLabel, BorderLayout.SOUTH);

    // Position the window near the mouse
    Point mousePos = MouseInfo.getPointerInfo().getLocation();
    int wx = Math.min(mousePos.x + 30, screenRect.width - magWindow.getWidth());
    int wy = Math.min(mousePos.y + 30, screenRect.height - magWindow.getHeight());
    magWindow.setLocation(wx, wy);

    // Timer: refresh magnifier every 100ms
    Timer refreshTimer =
        new Timer(
            100,
            e -> {
              try {
                Point mp = MouseInfo.getPointerInfo().getLocation();
                int newX = mp.x + 30;
                int newY = mp.y + 30;
                if (newX + magWindow.getWidth() > screenRect.width) {
                  newX = mp.x - magWindow.getWidth() - 10;
                }
                if (newY + magWindow.getHeight() > screenRect.height) {
                  newY = mp.y - magWindow.getHeight() - 10;
                }
                magWindow.setLocation(Math.max(0, newX), Math.max(0, newY));

                // Capture 11x11 region centered on cursor
                int captX = mp.x - 5;
                int captY = mp.y - 5;
                if (captX >= 0
                    && captY >= 0
                    && captX + GRID <= screenRect.width
                    && captY + GRID <= screenRect.height) {
                  BufferedImage capture =
                      robot.createScreenCapture(new Rectangle(captX, captY, GRID, GRID));
                  for (int r = 0; r < GRID; r++) {
                    for (int c = 0; c < GRID; c++) {
                      pixelBuffer[r * GRID + c] = capture.getRGB(c, r);
                    }
                  }
                  int centerArgb = capture.getRGB(5, 5);
                  int r = (centerArgb >> 16) & 0xFF;
                  int g = (centerArgb >> 8) & 0xFF;
                  int b = centerArgb & 0xFF;
                  infoLabel.setText(String.format("RGB(%d,%d,%d) (%d,%d)", r, g, b, mp.x, mp.y));
                }
              } catch (Exception ignored) {
                // ignore capture failures near screen edges
              }
              magPanel.repaint();
            });

    // Mouse listener on magnifier panel
    magPanel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent evt) {
            refreshTimer.stop();
            // Use the event's screen coordinates for precision
            int sx = evt.getXOnScreen();
            int sy = evt.getYOnScreen();
            if (isLeft) {
              topXField.setText(String.valueOf(sx));
              topYField.setText(String.valueOf(sy));
            } else {
              bottomXField.setText(String.valueOf(sx));
              bottomYField.setText(String.valueOf(sy));
            }
            infoLabel.setText(String.format("\u5df2\u9009\u53d6: (%d,%d)", sx, sy));
            magWindow.dispose();
          }
        });

    // Mouse listener on info label (same handler)
    infoLabel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent evt) {
            refreshTimer.stop();
            int sx = evt.getXOnScreen();
            int sy = evt.getYOnScreen();
            if (isLeft) {
              topXField.setText(String.valueOf(sx));
              topYField.setText(String.valueOf(sy));
            } else {
              bottomXField.setText(String.valueOf(sx));
              bottomYField.setText(String.valueOf(sy));
            }
            magWindow.dispose();
          }
        });

    // Key listener: ESC cancels
    magWindow.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
              refreshTimer.stop();
              magWindow.dispose();
            }
          }
        });

    magWindow.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    magWindow.pack();
    magWindow.setVisible(true);
    magWindow.requestFocusInWindow();
    refreshTimer.start();
  }

  // ======================== Board contour ========================

  private void showBoardContour() {
    String tx = topXField.getText().trim();
    String ty = topYField.getText().trim();
    String bx = bottomXField.getText().trim();
    String by = bottomYField.getText().trim();

    if (tx.isEmpty() || ty.isEmpty() || bx.isEmpty() || by.isEmpty()) {
      Utils.showMessageDialog(this, "\u8bf7\u5148\u6821\u51c6\u5750\u6807\u3002");
      return;
    }

    try {
      int topX = Integer.parseInt(tx);
      int topY = Integer.parseInt(ty);
      int botX = Integer.parseInt(bx);
      int botY = Integer.parseInt(by);

      bridge.applyCalibration(topX, topY, botX, botY);
      OverlayWindow overlay = bridge.getOverlayWindow();
      overlay.alignToBoard(topX, topY, botX, botY);
      overlay.showOverlay();
      updateStatus("\u8f6e\u5ed3\u5df2\u663e\u793a", new Color(0, 128, 0));
    } catch (NumberFormatException e) {
      Utils.showMessageDialog(this, "\u5750\u6807\u683c\u5f0f\u65e0\u6548");
    }
  }

  // ======================== Start / Stop sync ========================

  private void startSync() {
    if (!bridge.getStoneDetector().isCalibrated()) {
      Utils.showMessageDialog(this, "\u8bf7\u5148\u6821\u51c6\u597d\u4f4d\u7f6e\u3002");
      return;
    }
    bridge.start();
    startBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    updateStatus("\u540c\u6b65\u4e2d...", new Color(0, 128, 0));
  }

  private void stopSync() {
    bridge.stop();
    startBtn.setEnabled(true);
    stopBtn.setEnabled(false);
    updateStatus("\u5df2\u505c\u6b62", Color.GRAY);
  }

  // ======================== Config persistence ========================

  private void saveConfig() {
    try {
      Lizzie.config.leelazConfig.put("foxgo-top-x", Integer.parseInt(topXField.getText().trim()));
      Lizzie.config.leelazConfig.put("foxgo-top-y", Integer.parseInt(topYField.getText().trim()));
      Lizzie.config.leelazConfig.put(
          "foxgo-bottom-x", Integer.parseInt(bottomXField.getText().trim()));
      Lizzie.config.leelazConfig.put(
          "foxgo-bottom-y", Integer.parseInt(bottomYField.getText().trim()));
      Lizzie.config.save();
      updateStatus("\u914d\u7f6e\u5df2\u4fdd\u5b58", new Color(0, 128, 0));
    } catch (NumberFormatException e) {
      Utils.showMessageDialog(this, "\u5750\u6807\u683c\u5f0f\u65e0\u6548");
    } catch (Exception e) {
      updateStatus("\u4fdd\u5b58\u5931\u8d25", Color.RED);
    }
  }

  private void loadConfig() {
    int tx = Lizzie.config.leelazConfig.optInt("foxgo-top-x", 0);
    int ty = Lizzie.config.leelazConfig.optInt("foxgo-top-y", 0);
    int bx = Lizzie.config.leelazConfig.optInt("foxgo-bottom-x", 0);
    int by = Lizzie.config.leelazConfig.optInt("foxgo-bottom-y", 0);

    if (tx > 0 && ty > 0 && bx > 0 && by > 0) {
      topXField.setText(String.valueOf(tx));
      topYField.setText(String.valueOf(ty));
      bottomXField.setText(String.valueOf(bx));
      bottomYField.setText(String.valueOf(by));
      updateStatus("\u914d\u7f6e\u5df2\u52a0\u8f7d", new Color(0, 128, 0));
    }
  }

  // ======================== Status helper ========================

  private void updateStatus(String text, Color color) {
    statusLabel.setText(text);
    statusLabel.setForeground(color);
  }
}
'''

with open('G:/idea_workspace/Lizzie-improvements/src/main/java/featurecat/lizzie/foxgo/BoardAligner.java', 'w', encoding='utf-8') as f:
    f.write(content)

# Verify
with open('G:/idea_workspace/Lizzie-improvements/src/main/java/featurecat/lizzie/foxgo/BoardAligner.java', 'rb') as f:
    data = f.read()

print(f"Written {len(data)} bytes")
try:
    text = data.decode('utf-8')
    print("UTF-8 decode OK")
except:
    print("UTF-8 decode FAILED")

# Check for known Chinese strings
checks = [
    ("\\u68cb\\u76d8\\u6821\\u51c6", "棋盘校准"),
    ("\\u9009\\u53d6\\u5de6\\u4e0a\\u89d2", "选取左上角"),
    ("\\u9009\\u53d6\\u53f3\\u4e0b\\u89d2", "选取右下角"),
]
for escaped, chars in checks:
    encoded = chars.encode('utf-8')
    if encoded in data:
        print(f"OK: {chars} found")
    else:
        print(f"ERROR: {chars} NOT found, looking for {encoded.hex()}")

import os
os.remove('G:/idea_workspace/Lizzie-improvements/tmp_fix_board_aligner.py')
