package featurecat.lizzie.foxgo;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.util.Utils;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Provides a crosshair magnifier overlay for visually calibrating board corner positions. Both
 * "Pick Top-Left" and "Pick Bottom-Right" open a floating magnifier window.
 */
public class BoardAligner extends JDialog {
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
    super(Lizzie.frame, "棋盘校准", false);
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
    JLabel title = new JLabel("Fox Go AI 校准");
    title.setFont(new Font("SansSerif", Font.BOLD, 16));
    header.add(title, BorderLayout.WEST);

    statusLabel = new JLabel("等待校准");
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
            "<html>在 Fox Go 棋盘上，请先校准两个角点。<br>"
                + "点击\u201C选取左上角\u201D或\u201C选取右下角\u201D按钮打开放大镜，再点击选择对应的坐标点。<br>"
                + "按 ESC 取消选择</html>");
    instructions.setFont(new Font("SansSerif", Font.PLAIN, 12));
    form.add(instructions, gbc);

    gbc.gridwidth = 1;

    // Top-Left row
    gbc.gridy = 1;
    gbc.gridx = 0;
    form.add(new JLabel("左上角 X:"), gbc);
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
    form.add(new JLabel("右下角 X:"), gbc);
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
    JButton pickTopBtn = new JButton("选取左上角");
    pickTopBtn.addActionListener(e -> pickPosition(true));
    form.add(pickTopBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    JButton pickBottomBtn = new JButton("选取右下角");
    pickBottomBtn.addActionListener(e -> pickPosition(false));
    form.add(pickBottomBtn, gbc);

    gbc.gridy = 4;
    gbc.gridx = 0;
    gbc.gridwidth = 4;
    JButton showBtn = new JButton("显示棋盘轮廓");
    showBtn.addActionListener(e -> showBoardContour());
    form.add(showBtn, gbc);

    gbc.gridy = 5;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    startBtn = new JButton("开始同步");
    startBtn.addActionListener(e -> startSync());
    form.add(startBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    stopBtn = new JButton("停止同步");
    stopBtn.addActionListener(e -> stopSync());
    stopBtn.setEnabled(false);
    form.add(stopBtn, gbc);

    gbc.gridy = 6;
    gbc.gridx = 0;
    gbc.gridwidth = 4;
    JButton hideBtn = new JButton("隐藏轮廓");
    hideBtn.addActionListener(e -> bridge.getOverlayWindow().hideOverlay());
    form.add(hideBtn, gbc);

    gbc.gridy = 7;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    JButton saveBtn = new JButton("保存校准");
    saveBtn.addActionListener(e -> saveConfig());
    form.add(saveBtn, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    JButton closeBtn = new JButton("关闭");
    closeBtn.addActionListener(e -> dispose());
    form.add(closeBtn, gbc);

    mainPanel.add(form, BorderLayout.CENTER);
    add(mainPanel);
  }

  // ======================== Magnifier pick ========================

  private static final int MAG_RADIUS = 60;
  private static final int CAP_SIZE = 11;

  private void pickPosition(boolean isLeft) {
    Robot robot;
    try {
      robot = new Robot();
    } catch (AWTException ex) {
      ex.printStackTrace();
      Utils.showMessageDialog(this, "无法创建屏幕抓取对象。");
      return;
    }

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int screenW = screenSize.width;
    int screenH = screenSize.height;

    final int[] capturedX = {0};
    final int[] capturedY = {0};
    final int[] mouseX = {0};
    final int[] mouseY = {0};
    final int[] pixelBuffer = new int[CAP_SIZE * CAP_SIZE];

    JFrame overlay = new JFrame();
    overlay.setSize(screenW, screenH);
    overlay.setLocation(0, 0);
    overlay.setUndecorated(true);
    overlay.setAlwaysOnTop(true);
    overlay.setBackground(new Color(0, 0, 0, 30));
    overlay.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    JPanel overlayPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRect(0, 0, screenW, screenH);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, mouseY[0], screenW, mouseY[0]);
        g2.drawLine(mouseX[0], 0, mouseX[0], screenH);
      }
    };
    overlayPanel.setOpaque(false);
    overlay.add(overlayPanel, BorderLayout.CENTER);

    JPanel magPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        g2.setClip(new java.awt.geom.Ellipse2D.Double(0, 0, MAG_RADIUS * 2, MAG_RADIUS * 2));

        for (int row = 0; row < CAP_SIZE; row++) {
          for (int col = 0; col < CAP_SIZE; col++) {
            int argb = pixelBuffer[row * CAP_SIZE + col];
            if ((argb >>> 24) != 0) {
              g2.setColor(new Color(argb, true));
              double px = col * MAG_RADIUS * 2.0 / CAP_SIZE;
              double py = row * MAG_RADIUS * 2.0 / CAP_SIZE;
              double pw = MAG_RADIUS * 2.0 / CAP_SIZE;
              double ph = MAG_RADIUS * 2.0 / CAP_SIZE;
              g2.fillRect((int) px, (int) py, (int) Math.ceil(pw), (int) Math.ceil(ph));
            }
          }
        }

        g2.setClip(null);
        g2.setColor(new Color(100, 200, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(0, 0, MAG_RADIUS * 2 - 1, MAG_RADIUS * 2 - 1);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));
        int gridCount = 5;
        for (int i = 1; i < gridCount; i++) {
          int pos = i * MAG_RADIUS * 2 / gridCount;
          g2.drawLine(pos, 0, pos, MAG_RADIUS * 2);
          g2.drawLine(0, pos, MAG_RADIUS * 2, pos);
        }

        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(MAG_RADIUS - 8, MAG_RADIUS, MAG_RADIUS + 8, MAG_RADIUS);
        g2.drawLine(MAG_RADIUS, MAG_RADIUS - 8, MAG_RADIUS, MAG_RADIUS + 8);
      }
    };
    magPanel.setBounds(0, 0, MAG_RADIUS * 2, MAG_RADIUS * 2);
    magPanel.setOpaque(false);
    magPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    overlay.add(magPanel);

    JLabel coordLabel = new JLabel("", SwingConstants.CENTER);
    coordLabel.setBounds(0, MAG_RADIUS * 2 - 30, MAG_RADIUS * 2, 30);
    coordLabel.setForeground(Color.WHITE);
    coordLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
    overlay.add(coordLabel);

    Timer timer = new Timer(50, null);
    timer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Point p = MouseInfo.getPointerInfo().getLocation();
        mouseX[0] = p.x;
        mouseY[0] = p.y;

        int magX = p.x - MAG_RADIUS;
        int magY = p.y - MAG_RADIUS - 30;
        if (magX < 0) magX = 0;
        if (magY < 0) magY = 0;
        if (magX + MAG_RADIUS * 2 > screenW) magX = screenW - MAG_RADIUS * 2;
        if (magY + MAG_RADIUS * 2 + 30 > screenH) magY = screenH - MAG_RADIUS * 2 - 30;
        magPanel.setLocation(magX, magY);
        coordLabel.setLocation(magX, magY);

        int captX = p.x - CAP_SIZE / 2;
        int captY = p.y - CAP_SIZE / 2;
        if (captX >= 0 && captY >= 0 && captX + CAP_SIZE <= screenW && captY + CAP_SIZE <= screenH) {
          try {
            BufferedImage cap = robot.createScreenCapture(new Rectangle(captX, captY, CAP_SIZE, CAP_SIZE));
            for (int r = 0; r < CAP_SIZE; r++) {
              for (int c = 0; c < CAP_SIZE; c++) {
                pixelBuffer[r * CAP_SIZE + c] = cap.getRGB(c, r);
              }
            }
            int argb = cap.getRGB(CAP_SIZE / 2, CAP_SIZE / 2);
            int rv = (argb >> 16) & 0xFF;
            int gv = (argb >> 8) & 0xFF;
            int bv = argb & 0xFF;
            coordLabel.setText(String.format("(%d,%d) RGB(%d,%d,%d)", p.x, p.y, rv, gv, bv));
          } catch (Exception ex) {
            coordLabel.setText(String.format("(%d,%d)", p.x, p.y));
          }
        } else {
          coordLabel.setText(String.format("(%d,%d)", p.x, p.y));
        }

        overlayPanel.repaint();
        magPanel.repaint();
      }
    });

    MouseAdapter clickHandler = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
          Point p = MouseInfo.getPointerInfo().getLocation();
          capturedX[0] = p.x;
          capturedY[0] = p.y;
          finish();
        }
      }
    };

    KeyAdapter escHandler = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          finish();
        }
      }
    };

    Runnable finish = new Runnable() {
      public void run() {
        timer.stop();
        overlay.dispose();
        setEnabled(true);
        toFront();
        requestFocus();
        if (capturedX[0] > 0 || capturedY[0] > 0) {
          if (isLeft) {
            topXField.setText(String.valueOf(capturedX[0]));
            topYField.setText(String.valueOf(capturedY[0]));
          } else {
            bottomXField.setText(String.valueOf(capturedX[0]));
            bottomYField.setText(String.valueOf(capturedY[0]));
          }
        }
      }
    };

    overlay.addMouseListener(clickHandler);
    overlay.addKeyListener(escHandler);

    setEnabled(false);
    overlay.setVisible(true);
    overlay.toFront();
    overlay.requestFocus();
    timer.start();
  }

  // ======================== Board contour ========================

  private void showBoardContour() {
    String tx = topXField.getText().trim();
    String ty = topYField.getText().trim();
    String bx = bottomXField.getText().trim();
    String by = bottomYField.getText().trim();

    if (tx.isEmpty() || ty.isEmpty() || bx.isEmpty() || by.isEmpty()) {
      Utils.showMessageDialog(this, "请先校准坐标。");
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
      updateStatus("轮廓已显示", new Color(0, 128, 0));
    } catch (NumberFormatException e) {
      Utils.showMessageDialog(this, "坐标格式无效");
    }
  }

  // ======================== Start / Stop sync ========================

  private void startSync() {
    if (!bridge.getStoneDetector().isCalibrated()) {
      Utils.showMessageDialog(this, "请先校准好位置。");
      return;
    }
    bridge.start();
    startBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    updateStatus("同步中...", new Color(0, 128, 0));
  }

  private void stopSync() {
    bridge.stop();
    startBtn.setEnabled(true);
    stopBtn.setEnabled(false);
    updateStatus("已停止", Color.GRAY);
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
      updateStatus("配置已保存", new Color(0, 128, 0));
    } catch (NumberFormatException e) {
      Utils.showMessageDialog(this, "坐标格式无效");
    } catch (Exception e) {
      updateStatus("保存失败", Color.RED);
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
      updateStatus("配置已加载", new Color(0, 128, 0));
    }
  }

  // ======================== Status helper ========================

  private void updateStatus(String text, Color color) {
    statusLabel.setText(text);
    statusLabel.setForeground(color);
  }
}
