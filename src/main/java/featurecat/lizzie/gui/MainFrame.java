package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.util.Utils;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONObject;

public abstract class MainFrame extends JFrame {
  public static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");

  private static final String DEFAULT_TITLE = resourceBundle.getString("LizzieFrame.title");
  public boolean isPlayingAgainstLeelaz = false;
  public boolean playerIsBlack = true;
  public boolean isNewGame = false;
  public int boardPositionProportion = Lizzie.config.boardPositionProportion;
  public int winRateGridLines = 3;
  public boolean showControls = false;
  public static Font uiFont;
  public static Font winrateFont;
  public boolean isEstimating = false;
  public boolean isFirstCount = true;
  public boolean isAutoEstimating = false;
  public boolean isShowingRightMenu = false;
  public ToolBar toolBar;
  public boolean isShowingPolicy = false;

  static {
    // load fonts
    try {
      uiFont = new Font("SansSerif", Font.TRUETYPE_FONT, 12);
      winrateFont =
          Font.createFont(
              Font.TRUETYPE_FONT,
              Thread.currentThread()
                  .getContextClassLoader()
                  .getResourceAsStream("fonts/OpenSans-Semibold.ttf"));
    } catch (IOException | FontFormatException e) {
      e.printStackTrace();
    }
  }

  // Save the player title
  private String playerTitle = "";
  protected String visitsString = "";
  // Force refresh board
  private boolean forceRefresh;
  public boolean isMouseOver = false;

  public MainFrame() throws HeadlessException {
    super(DEFAULT_TITLE);
    Utils.mustBeEventDispatchThread();
    ToolTipManager.sharedInstance().setInitialDelay(0);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }

  public boolean isDesignMode() {
    return false;
  }

  public void toggleDesignMode() {}

  public void updateBasicInfo() {}

  public void updateBasicInfo(String bTime, String wTime) {}

  public void repaint() {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            repaintInEDT();
          }
        });
  }

  private void repaintInEDT() {
    Utils.mustBeEventDispatchThread();
    super.repaint();
  }

  public void refresh() {
    repaint();
  }

  public void refresh(int type) {
    repaint();
  }

  public boolean isForceRefresh() {
    return forceRefresh;
  }

  public void setForceRefresh(boolean forceRefresh) {
    this.forceRefresh = forceRefresh;
  }

  public boolean processCommentMouseWheelMoved(MouseWheelEvent e) {
    return false;
  }

  public void removeEstimateRect() {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            removeEstimateRectInEDT();
          }
        });
  }

  public void removeEstimateRectInEDT() {}

  public void drawEstimateRectKata(ArrayList<Double> estimateArray) {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            drawEstimateRectKataInEDT(estimateArray);
          }
        });
  }

  public void drawEstimateRectKataInEDT(ArrayList<Double> estimateArray) {}

  public void drawControls() {}

  public void replayBranch(boolean generateGif) {}

  public void refreshBackground() {}

  public void resetImages() {}

  public void clear() {}

  public boolean isMouseOver(int x, int y) {
    return false;
  }

  public void onClicked(int x, int y) {}

  public void onDoubleClicked(int x, int y) {}

  public void checkRightClick(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON3) {
      onRightClickedOutsideBoard();
    }
  }

  public void onRightClicked(int x, int y) {
    if (!openRightClickMenu(x, y)) onRightClickedOutsideBoard();
  }

  private void onRightClickedOutsideBoard() {
    Input.undo();
  }

  public void onCenterClicked(int x, int y) {}

  public void onMouseDragged(int x, int y) {}

  public void onMouseExited(int x, int y) {}

  public void onMouseMoved(int x, int y) {}

  public void startRawBoard() {}

  public void stopRawBoard() {}

  public boolean incrementDisplayedBranchLength(int n) {
    return false;
  }

  public void doBranch(int moveTo) {}

  public void addSuggestionAsBranch() {}

  public void increaseMaxAlpha(int k) {}

  public abstract void copySgf();

  public abstract void pasteSgf();

  public void editComment() {}

  public void copyCommentToClipboard() {}

  public void setPlayers(String whitePlayer, String blackPlayer) {
    playerTitle = String.format("(%s [W] vs %s [B])", whitePlayer, blackPlayer);
    updateTitle();
  }

  public void updateTitle() {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            updateTitleInEDT();
          }
        });
  }

  private void updateTitleInEDT() {
    Utils.mustBeEventDispatchThread();
    StringBuilder sb = new StringBuilder(DEFAULT_TITLE);
    sb.append(playerTitle);
    sb.append(" [" + Lizzie.leelaz.nicknameOrEngineCommand() + "]");
    sb.append(visitsString);
    setTitle(sb.toString());
  }

  public void resetTitle() {
    playerTitle = "";
    updateTitle();
  }

  public void openConfigDialog() {}

  public void openConfigDialog(int index) {}

  public void openChangeMoveDialog() {}

  public void openAvoidMoveDialog() {
    AvoidMoveDialog avoidMoveDialog = new AvoidMoveDialog();
    avoidMoveDialog.setVisible(true);
  }

  public void toggleGtpConsole() {
    Lizzie.leelaz.toggleGtpConsole();
    if (Lizzie.gtpConsole != null) {
      Lizzie.gtpConsole.setVisible(!Lizzie.gtpConsole.isVisible());
    } else {
      Lizzie.gtpConsole = new GtpConsolePane(this);
      Lizzie.gtpConsole.setVisible(true);
    }
  }

  public void toggleToolBar() {
    Lizzie.config.showToolBar = !Lizzie.config.showToolBar;
    toolBar.setVisible(Lizzie.config.showToolBar);
    Lizzie.config.uiConfig.put("show-toolbar", Lizzie.config.showToolBar);
    try {
      Lizzie.config.save();
    } catch (IOException es) {
    }
  }

  public String getToolBarPosition() {
    return Lizzie.config.toolbarPosition;
  }

  public boolean getFocus() {
    return requestFocusInWindow();
  }

  public void openOnlineDialog() {}

  public void startGame() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    NewGameDialog gameDialog = new NewGameDialog();
    gameDialog.setGameInfo(gameInfo);
    gameDialog.setVisible(true);
    boolean playerIsBlack = gameDialog.playerIsBlack();
    boolean isNewGame = gameDialog.isNewGame();
    if (gameDialog.isCancelled()) return;

    if (isNewGame) {
      Lizzie.board.clear();
    }
    Lizzie.leelaz.komi(gameInfo.getKomi());

    Lizzie.leelaz.time_settings();
    Lizzie.frame.playerIsBlack = playerIsBlack;
    Lizzie.frame.isNewGame = isNewGame;
    Lizzie.frame.isPlayingAgainstLeelaz = true;

    boolean isHandicapGame = gameInfo.getHandicap() != 0;
    if (isNewGame) {
      Lizzie.board.getHistory().setGameInfo(gameInfo);
      if (isHandicapGame) {
        Lizzie.board.getHistory().getData().blackToPlay = false;
        Lizzie.leelaz.handicap(gameInfo.getHandicap());
        if (playerIsBlack) Lizzie.leelaz.genmove("W");
      } else if (!playerIsBlack) {
        Lizzie.leelaz.genmove("B");
      }
    } else {
      Lizzie.board.getHistory().setGameInfo(gameInfo);
      if (Lizzie.frame.playerIsBlack != Lizzie.board.getData().blackToPlay) {
        if (!Lizzie.leelaz.isThinking) {
          Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "B" : "W"));
        }
      }
    }
  }

  public void editGameInfo() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    GameInfoDialog gameInfoDialog = new GameInfoDialog();
    gameInfoDialog.setGameInfo(gameInfo);
    gameInfoDialog.setVisible(true);

    gameInfoDialog.dispose();
  }

  public void saveFile() {
    FileNameExtensionFilter filter =
        new FileNameExtensionFilter("Smart Game Format (*.sgf *.SGF)", "sgf");
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-folder"));
    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (!(file.getPath().endsWith(".sgf") || file.getPath().endsWith(".SGF"))) {
        file = new File(file.getPath() + ".sgf");
      }
      if (file.exists()) {
        int ret =
            JOptionPane.showConfirmDialog(
                this,
                resourceBundle.getString("LizzieFrame.prompt.sgfExists"),
                "Warning",
                JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      try {
        SGFParser.save(Lizzie.board, file.getPath());
        if (file.getParent() != null) {
          filesystem.put("last-folder", file.getParent());
        }
      } catch (IOException err) {
        JOptionPane.showConfirmDialog(
            this,
            resourceBundle.getString("LizzieFrame.prompt.failedTosaveFile"),
            "Error",
            JOptionPane.ERROR);
      }
    }
  }

  public void openFile() {
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    FileDialog fileDialog = new FileDialog(this, resourceBundle.getString("LizzieFrame.openFile"));
    fileDialog.setLocationRelativeTo(this);
    fileDialog.setDirectory(filesystem.getString("last-folder"));
    fileDialog.setFile("*.sgf;*.SGF");
    fileDialog.setMultipleMode(false);
    fileDialog.setMode(0);
    fileDialog.setVisible(true);
    File[] file = fileDialog.getFiles();
    if (file.length > 0) loadFile(file[0]);
  }

  public void loadFile(File file) {
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    if (!(file.getPath().endsWith(".sgf") || file.getPath().endsWith(".SGF"))) {
      file = new File(file.getPath() + ".sgf");
    }
    try {
      System.out.println(file.getPath());
      SGFParser.load(file.getPath());
      if (file.getParent() != null) {
        filesystem.put("last-folder", file.getParent());
      }
    } catch (IOException err) {
      JOptionPane.showConfirmDialog(
          this,
          resourceBundle.getString("LizzieFrame.prompt.failedToOpenFile"),
          "Error",
          JOptionPane.ERROR);
    }
  }

  protected String loadingText() {
    return (Lizzie.leelaz != null) && Lizzie.leelaz.isDown()
        ? "Engine is down."
        : resourceBundle.getString("LizzieFrame.display.loading");
  }

  public void toggleEstimateByZen() {}

  public boolean playCurrentVariation() {
    return false;
  }

  public void playBestMove() {}

  public void estimateByZen() {}

  public void noAutoEstimateByZen() {}

  public void noEstimateByZen(boolean byToolBar) {}

  public void drawEstimateRectZen(ArrayList<Double> estimateArray) {}

  public void saveImage() {}

  public void updateEngineMenu(List<Leelaz> engineList) {}

  public void updateEngineIcon(List<Leelaz> engineList, int currentEngineNo) {}

  public abstract Optional<int[]> convertScreenToCoordinates(int x, int y);

  public abstract void updateScoreMenu(boolean on);

  public abstract boolean openRightClickMenu(int x, int y);

  public abstract void clearBeforeMove();
}
