package featurecat.lizzie.foxgo;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.rules.Board;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Optional;
import javax.swing.*;

/**
 * 透明覆盖窗口 显示在野狐棋盘上方，绘制 AI 分析结果（胜率、推荐走法）
 *
 * <p>支持： - 窗口透明背景 - 鼠标穿透（点击操作透传到野狐） - 可拖拽标题栏调整位置
 */
public class OverlayWindow extends JWindow {
  private static final int PADDING = 27;
  private static final int STONE_RADIUS = 8;
  private static final int BOARD_SIZE = 19;
  private static final Color OVERLAY_LINE_COLOR = new Color(255, 255, 255, 80);
  private static final Color OVERLAY_BG_COLOR = new Color(255, 255, 255, 10);

  // 棋盘区域屏幕坐标
  private int boardX0, boardY0, boardX1, boardY1;
  private double xGap, yGap;
  private boolean boardConfigured = false;

  // 拖动支持
  private Point mouseDownCompCoords;
  private int windowOffsetX, windowOffsetY;

  // AI 分析数据
  private List<MoveData> currentBestMoves;
  private boolean visible = false;
  private boolean mousePenetrate = true; // 默认穿透

  // 控制面板
  private JPanel titleBar;
  private JLabel titleLabel;

  public OverlayWindow() {
    initUI();
  }

  private void initUI() {
    setSize(400, 400);
    setAlwaysOnTop(true);

    // 设置窗口透明背景
    setBackground(new Color(0, 0, 0, 0));

    // ===== 标题栏（拖拽用） =====
    titleBar =
        new JPanel(new BorderLayout()) {
          @Override
          public Insets getInsets() {
            return new Insets(0, 15, 0, 0);
          }
        };
    titleBar.setPreferredSize(new Dimension(100, 28));
    titleBar.setBackground(new Color(50, 50, 50, 180));

    JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
    westPanel.setOpaque(false);
    titleBar.add(westPanel, BorderLayout.WEST);

    titleLabel = new JLabel("Lizzie AI助手");
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
    westPanel.add(titleLabel);

    JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 4));
    eastPanel.setOpaque(false);

    // 穿透切换按钮
    JCheckBox penetrateCheck = new JCheckBox("穿透");
    penetrateCheck.setSelected(true);
    penetrateCheck.setForeground(Color.WHITE);
    penetrateCheck.setBackground(new Color(0, 0, 0, 0));
    penetrateCheck.setFont(new Font("SansSerif", Font.PLAIN, 11));
    penetrateCheck.addActionListener(
        e -> {
          mousePenetrate = penetrateCheck.isSelected();
          updateMousePenetrate();
        });
    eastPanel.add(penetrateCheck);

    // 关闭按钮
    JButton closeBtn = new JButton("×");
    closeBtn.setContentAreaFilled(false);
    closeBtn.setBorderPainted(false);
    closeBtn.setForeground(Color.WHITE);
    closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
    closeBtn.addActionListener(e -> hideOverlay());
    eastPanel.add(closeBtn);

    titleBar.add(eastPanel, BorderLayout.EAST);

    // 标题栏拖拽
    titleBar.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            mouseDownCompCoords = e.getPoint();
          }
        });
    titleBar.addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
            Point currCoords = e.getLocationOnScreen();
            setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
            // 同步窗口偏移
            windowOffsetX = getX() - boardX0;
            windowOffsetY = getY() - boardY0;
          }
        });

    // ===== 内容面板（透明，绘制棋盘和AI数据） =====
    JPanel contentPanel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gc = (Graphics2D) g.create();
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 透明背景
            gc.setColor(OVERLAY_BG_COLOR);
            gc.fillRect(0, 0, getWidth(), getHeight());

            // 绘制棋盘线框（辅助对齐）
            if (boardConfigured) {
              drawBoardLines(gc);
            }

            // 绘制 AI 推荐走法
            if (currentBestMoves != null && boardConfigured) {
              drawSuggestions(gc);
            }

            gc.dispose();
          }

          // 鼠标穿透：让点击穿透到野狐
          @Override
          public boolean contains(int x, int y) {
            // 如果启用了穿透，且不在标题栏区域，则点击穿透
            if (mousePenetrate && y > titleBar.getHeight()) {
              return false;
            }
            return super.contains(x, y);
          }
        };
    contentPanel.setOpaque(false);

    // 布局
    setLayout(new BorderLayout());
    add(titleBar, BorderLayout.NORTH);
    add(contentPanel, BorderLayout.CENTER);
  }

  /** 配置覆盖窗口与野狐棋盘对齐 */
  public void alignToBoard(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
    this.boardX0 = topLeftX;
    this.boardY0 = topLeftY;
    this.boardX1 = bottomRightX;
    this.boardY1 = bottomRightY;
    this.xGap = (double) (bottomRightX - topLeftX) / (BOARD_SIZE - 1);
    this.yGap = (double) (bottomRightY - topLeftY) / (BOARD_SIZE - 1);
    this.boardConfigured = true;

    // 窗口位置：覆盖在棋盘上方（加上标题栏高度）
    int overlayWidth = (bottomRightX - topLeftX) + 2 * PADDING;
    int overlayHeight =
        (bottomRightY - topLeftY) + 2 * PADDING + titleBar.getPreferredSize().height;

    // 如果有偏移记忆，应用偏移
    if (windowOffsetX != 0 || windowOffsetY != 0) {
      setLocation(
          topLeftX - PADDING + windowOffsetX, topLeftY - PADDING + windowOffsetY);
    } else {
      setLocation(topLeftX - PADDING, topLeftY - PADDING);
    }
    setSize(overlayWidth, overlayHeight);
  }

  /** 更新 AI 分析结果 */
  public void updateAnalysis(List<MoveData> bestMoves) {
    this.currentBestMoves = bestMoves;
    repaint();
  }

  /** 显示覆盖窗口 */
  public void showOverlay() {
    setVisible(true);
    visible = true;
    applyWindowTransparent();
  }

  /** 隐藏覆盖窗口 */
  public void hideOverlay() {
    setVisible(false);
    visible = false;
  }

  public boolean isOverlayVisible() {
    return visible;
  }

  // ===== 绘制方法 =====

  private void drawBoardLines(Graphics2D gc) {
    int drawWidth = getWidth();
    int drawHeight = getHeight() - titleBar.getHeight();

    gc.setColor(OVERLAY_LINE_COLOR);
    gc.setStroke(new BasicStroke(1));

    // 绘制网格线
    for (int i = 0; i < BOARD_SIZE; i++) {
      int posY = PADDING + (int) (i * yGap);
      int posX = PADDING + (int) (i * xGap);

      // 水平线
      gc.drawLine(PADDING, posY, PADDING + (int) (18 * xGap), posY);
      // 垂直线
      gc.drawLine(posX, PADDING, posX, PADDING + (int) (18 * yGap));
    }

    // 绘制星位
    gc.setColor(OVERLAY_LINE_COLOR);
    int[] stars = {3, 9, 15};
    for (int x : stars) {
      for (int y : stars) {
        int cx = PADDING + (int) (x * xGap);
        int cy = PADDING + (int) (y * yGap);
        gc.fillOval(cx - STONE_RADIUS / 2, cy - STONE_RADIUS / 2, STONE_RADIUS, STONE_RADIUS);
      }
    }
  }

  private void drawSuggestions(Graphics2D gc) {
    if (currentBestMoves == null) return;

    int maxToShow = Math.min(currentBestMoves.size(), 5); // 最多显示5个候选

    for (int i = 0; i < maxToShow; i++) {
      MoveData move = currentBestMoves.get(i);
      if (move == null || move.coordinate == null) continue;

      Optional<int[]> coords = Board.asCoordinates(move.coordinate);
      if (!coords.isPresent()) continue;

      int[] pos = coords.get();
      // 棋盘坐标 (pos[0]=col, pos[1]=row) → 屏幕坐标
      double screenX = PADDING + pos[0] * xGap;
      double screenY = PADDING + pos[1] * yGap;

      // 根据胜率决定颜色
      float winrate = (float) move.winrate;
      Color circleColor;
      if (winrate >= 50) {
        // 高胜率 → 暖色
        float intensity = Math.min(1.0f, (winrate - 50) / 40);
        circleColor = new Color(1.0f, 1.0f - intensity * 0.7f, 0.2f, 0.9f);
      } else {
        // 低胜率 → 冷色
        float intensity = Math.min(1.0f, (50 - winrate) / 40);
        circleColor = new Color(0.3f + intensity * 0.3f, 0.3f, 1.0f, 0.9f);
      }

      // 绘制圆圈标记
      gc.setColor(circleColor);
      gc.setStroke(new BasicStroke(2.5f));
      gc.drawOval((int) (screenX - xGap / 2), (int) (screenY - yGap / 2), (int) xGap, (int) yGap);

      // 绘制胜率文字
      String label = String.format("%.1f%%", winrate);
      if (move.scoreMean != 0 && Lizzie.config.showScoremeanInSuggestion) {
        label = String.format("%.1f", move.scoreMean);
      }

      gc.setFont(new Font("SansSerif", Font.BOLD, 11));
      FontMetrics fm = gc.getFontMetrics();
      int textX = (int) (screenX - fm.stringWidth(label) / 2);
      int textY = (int) (screenY + fm.getAscent() / 2);

      // 文字背景
      gc.setColor(new Color(0, 0, 0, 150));
      gc.fillRect(textX - 2, textY - fm.getAscent(), fm.stringWidth(label) + 4, fm.getHeight());

      gc.setColor(Color.WHITE);
      gc.drawString(label, textX, textY);
    }
  }

  // ===== Windows 窗口特效 =====

  /** 应用窗口透明 + 鼠标穿透（WS_EX_LAYERED | WS_EX_TRANSPARENT） */
  private void applyWindowTransparent() {
    try {
      WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(this));
      int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
      wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
      User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
    } catch (Exception e) {
      // 非 Windows 系统或不支持，忽略
      e.printStackTrace();
    }
  }

  /** 更新鼠标穿透状态 */
  private void updateMousePenetrate() {
    try {
      WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(this));
      int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
      if (mousePenetrate) {
        wl = wl | WinUser.WS_EX_TRANSPARENT;
      } else {
        wl = wl & ~WinUser.WS_EX_TRANSPARENT;
      }
      User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
