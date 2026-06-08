package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;
import static java.lang.Math.min;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.util.Utils;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.json.JSONArray;

/** The window used to display the game. */
public class LizzieFrame extends MainFrame {

  private static final String[] commands = {
    resourceBundle.getString("LizzieFrame.commands.keyN"),
    resourceBundle.getString("LizzieFrame.commands.keyEnter"),
    resourceBundle.getString("LizzieFrame.commands.keySpace"),
    resourceBundle.getString("LizzieFrame.commands.keyUpArrow"),
    resourceBundle.getString("LizzieFrame.commands.keyDownArrow"),
    resourceBundle.getString("LizzieFrame.commands.rightClick"),
    resourceBundle.getString("LizzieFrame.commands.mouseWheelScroll"),
    resourceBundle.getString("LizzieFrame.commands.keyC"),
    resourceBundle.getString("LizzieFrame.commands.keyP"),
    // scoreMode has no keyboard binding in 0.7.4.
    // resourceBundle.getString("LizzieFrame.commands.keyPeriod"),
    resourceBundle.getString("LizzieFrame.commands.keyA"),
    resourceBundle.getString("LizzieFrame.commands.keyM"),
    resourceBundle.getString("LizzieFrame.commands.keyI"),
    resourceBundle.getString("LizzieFrame.commands.keyO"),
    resourceBundle.getString("LizzieFrame.commands.keyS"),
    resourceBundle.getString("LizzieFrame.commands.keyAltC"),
    resourceBundle.getString("LizzieFrame.commands.keyAltV"),
    resourceBundle.getString("LizzieFrame.commands.keyF"),
    resourceBundle.getString("LizzieFrame.commands.keyV"),
    resourceBundle.getString("LizzieFrame.commands.keyW"),
    resourceBundle.getString("LizzieFrame.commands.keyCtrlW"),
    resourceBundle.getString("LizzieFrame.commands.keyG"),
    resourceBundle.getString("LizzieFrame.commands.keyR"),
    resourceBundle.getString("LizzieFrame.commands.keyBracket"),
    resourceBundle.getString("LizzieFrame.commands.keyT"),
    resourceBundle.getString("LizzieFrame.commands.keyCtrlT"),
    resourceBundle.getString("LizzieFrame.commands.keyY"),
    resourceBundle.getString("LizzieFrame.commands.keyZ"),
    resourceBundle.getString("LizzieFrame.commands.keyShiftZ"),
    resourceBundle.getString("LizzieFrame.commands.keyHome"),
    resourceBundle.getString("LizzieFrame.commands.keyEnd"),
    resourceBundle.getString("LizzieFrame.commands.keyControl"),
    resourceBundle.getString("LizzieFrame.commands.keyDelete"),
    resourceBundle.getString("LizzieFrame.commands.keyBackspace"),
    // This is keyK actually in 0.7.4.
    // resourceBundle.getString("LizzieFrame.commands.keyE"),
  };
  private static BoardRenderer boardRenderer;
  private static Menu menu;
  private JPanel mainPanel;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;
  private boolean isPonderingBeforeReplayVariation = false;

  // Show the playouts in the title
  private ScheduledExecutorService showPlayouts = Executors.newScheduledThreadPool(1);
  private long lastPlayouts = 0;
  public boolean isDrawVisitsInTitle = true;

  /** Creates a window */
  public LizzieFrame() {
    super();

    boardRenderer = new BoardRenderer();
    menu = new Menu();
    toolBar = new ToolBar();
    toolBar.setVisible(Lizzie.config.showToolBar);
    add(toolBar, Lizzie.config.toolbarPosition);
    setMinimumSize(new Dimension(640, 400));
    boolean persisted = Lizzie.config.persistedUi != null;
    if (persisted
        && Lizzie.config.persistedUi.optJSONArray("main-window-position") != null
        && Lizzie.config.persistedUi.optJSONArray("main-window-position").length() == 4) {
      JSONArray pos = Lizzie.config.persistedUi.getJSONArray("main-window-position");
      this.setBounds(pos.getInt(0), pos.getInt(1), pos.getInt(2), pos.getInt(3));
      this.boardPositionProportion =
          Lizzie.config.persistedUi.optInt(
              "board-position-proportion", this.boardPositionProportion);
    } else {
      setSize(960, 600);
      setLocationRelativeTo(null); // Start centered, needs to be called *after* setSize...
    }
    mainPanel =
        new JPanel(true) {
          @Override
          protected void paintComponent(Graphics g) {
            Utils.mustBeEventDispatchThread();
            super.paintComponent(g);

            paintMainPanel(g);
          }
        };
    getContentPane().add(mainPanel);
    mainPanel.setTransferHandler(Utils.transFile);
    mainPanel.setFocusable(true);
    setJMenuBar(menu);

    // Allow change font in the config
    if (Lizzie.config.uiFontName != null) {
      uiFont = new Font(Lizzie.config.uiFontName, Font.PLAIN, 12);
    }
    if (Lizzie.config.winrateFontName != null) {
      winrateFont = new Font(Lizzie.config.winrateFontName, Font.BOLD, 12);
    }

    if (Lizzie.config.startMaximized && !persisted) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    } else if (persisted && Lizzie.config.persistedUi.getBoolean("window-maximized")) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    try {
      this.setIconImage(ImageIO.read(getClass().getResourceAsStream("/assets/logo.png")));
    } catch (IOException e) {
      e.printStackTrace();
    }

    setVisible(true);

    // avoid IME issue
    // https://github.com/featurecat/lizzie/pull/880#issuecomment-800804632
    // https://github.com/yzyray/lizzie_adv/commit/e5e8be01b4e072615250e4c4cc8462938b7c0332
    mainPanel.enableInputMethods(false);

    Input input = new Input();

    mainPanel.addMouseListener(input);
    mainPanel.addKeyListener(input);
    mainPanel.addMouseWheelListener(input);
    mainPanel.addMouseMotionListener(input);

    // necessary for Windows users - otherwise Lizzie shows a blank white screen on startup until
    // updates occur.
    repaint();

    // When the window is closed: save the SGF file, then run shutdown()
    this.addWindowListener(
        new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            Lizzie.shutdown();
          }
        });

    // Show the playouts in the title
    showPlayouts.scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            if (!isDrawVisitsInTitle) {
              visitsString = "";
              return;
            }
            if (Lizzie.leelaz == null) return;
            try {
              int totalPlayouts = MoveData.getPlayouts(Lizzie.leelaz.getBestMoves());
              int recorderTotalPlayouts = Lizzie.board.getData().getPlayouts();
              int displayedTotalPlayouts = Math.max(totalPlayouts, recorderTotalPlayouts);
              if (displayedTotalPlayouts <= 0) return;
              boolean showingRecorded =
                  (totalPlayouts < displayedTotalPlayouts) && (totalPlayouts > 0);
              String backgroundTotalPlayoutsString =
                  showingRecorded ? String.format("%d/", totalPlayouts) : "";
              visitsString =
                  String.format(
                      " %s%d %s, %d %s",
                      backgroundTotalPlayoutsString,
                      displayedTotalPlayouts,
                      resourceBundle.getString("LizzieFrame.playouts"),
                      (totalPlayouts > lastPlayouts) ? totalPlayouts - lastPlayouts : 0,
                      resourceBundle.getString("LizzieFrame.visits"));
              updateTitle();
              lastPlayouts = totalPlayouts;
            } catch (Exception e) {
            }
          }
        },
        1,
        1,
        TimeUnit.SECONDS);
  }

  public void addNotify() {
    // needless?
    // https://stackoverflow.com/questions/3435994/buffers-have-not-been-created-whilst-creating-buffers
    super.addNotify();
    createBufferStrategy(2);
  }

  /** Clears related status from empty board. */
  public void clear() {}

  private BufferedImage cachedImage;

  /**
   * Draws the game board and interface
   *
   * @param g0 not used
   */
  public void paintMainPanel(Graphics g0) {
    autosaveMaybe();

    int width = mainPanel.getWidth();
    int height = mainPanel.getHeight();

    int topInset = mainPanel.getInsets().top;
    int leftInset = mainPanel.getInsets().left;
    int rightInset = mainPanel.getInsets().right;
    int bottomInset = mainPanel.getInsets().bottom;

    int maxSize = (int) (min(width - leftInset - rightInset, height - topInset - bottomInset));
    maxSize =
        max(
            maxSize,
            max(Board.boardWidth, Board.boardHeight) + 5);
    int boardX = (width - maxSize) / 8 * boardPositionProportion;
    int boardY = topInset + (height - topInset - bottomInset - maxSize) / 2;

    cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) cachedImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    boardRenderer.setLocation(boardX, boardY);
    boardRenderer.setBoardLength(maxSize, maxSize);
    boardRenderer.setupSizeParameters();
    boardRenderer.draw(g);

    g.dispose();
    g0.drawImage(cachedImage, 0, 0, null);
    g0.dispose();
  }

  public void refreshBackground() {}

  public void resetImages() {
    boardRenderer.resetImages();
  }

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
      if (!isPlayingAgainstLeelaz || (playerIsBlack == Lizzie.board.getData().blackToPlay))
        Lizzie.board.place(coords[0], coords[1]);
    }
    repaint();
  }

  public void onDoubleClicked(int x, int y) {
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (!isPlayingAgainstLeelaz) {
        int moveNumber = Lizzie.board.moveNumberByCoord(coords);
        if (moveNumber > 0) {
          Lizzie.board.goToMoveNumberBeyondBranch(moveNumber);
        }
      }
    }
  }

  public void onCenterClicked(int x, int y) {}

  public void clearMoved() {
    isReplayVariation = false;
    if (Lizzie.frame != null && Lizzie.frame.isMouseOver) {
      Lizzie.frame.isMouseOver = false;
      boardRenderer.startNormalBoard();
    }
  }

  public void onMouseExited(int x, int y) {
    if (Lizzie.frame != null && Lizzie.frame.isShowingRightMenu) return;
    mouseOverCoordinate = outOfBoundCoordinate;
    clearMoved();
  }

  public void onMouseMoved(int x, int y) {
    mouseOverCoordinate = outOfBoundCoordinate;
    Optional<int[]> coords = boardRenderer.convertScreenToCoordinates(x, y);
    coords
        .filter(c -> !isMouseOver(c[0], c[1]))
        .ifPresent(
            c -> {
              clearMoved();
              repaint();
            });
    coords.ifPresent(
        c -> {
          mouseOverCoordinate = c;
          isReplayVariation = false;
          if (Lizzie.frame != null) {
            Lizzie.frame.isMouseOver = boardRenderer.isShowingBranch();
          }
        });
    if (!coords.isPresent() && boardRenderer.isShowingBranch()) {
      clearMoved();
      repaint();
    }
  }

  public boolean isMouseOver(int x, int y) {
    return mouseOverCoordinate[0] == x && mouseOverCoordinate[1] == y;
  }

  public void onMouseDragged(int x, int y) {}

  /**
   * Process Comment Mouse Wheel Moved
   *
   * @return true when the scroll event was processed by this method
   */
  @Override
  public boolean processCommentMouseWheelMoved(MouseWheelEvent e) {
    return false;
  }

  public void createCommentImage(boolean forceRefresh, int w, int h) {}

  private void autosaveMaybe() {
    int interval =
        Lizzie.config.config.getJSONObject("ui").getInt("autosave-interval-seconds") * 1000;
    long currentTime = System.currentTimeMillis();
    if (interval > 0 && currentTime - lastAutosaveTime >= interval) {
      Lizzie.board.autosave();
      lastAutosaveTime = currentTime;
    }
  }

  public void startRawBoard() {
    boolean onBranch = boardRenderer.isShowingBranch();
    int n = (onBranch ? 1 : BoardRenderer.SHOW_RAW_BOARD);
    Utils.setDisplayedBranchLength(boardRenderer, n);
  }

  public void stopRawBoard() {
    Utils.setDisplayedBranchLength(boardRenderer, BoardRenderer.SHOW_NORMAL_BOARD);
  }

  public boolean incrementDisplayedBranchLength(int n) {
    return boardRenderer.incrementDisplayedBranchLength(n);
  }

  @Override
  public void doBranch(int moveTo) {
    boardRenderer.doBranch(moveTo);
  }

  public void addSuggestionAsBranch() {
    boardRenderer.addSuggestionAsBranch();
  }

  public void copySgf() {
    try {
      // Get sgf content from game
      String sgfContent = SGFParser.saveToString();

      // Save to clipboard
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable transferableString = new StringSelection(sgfContent);
      clipboard.setContents(transferableString, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void pasteSgf() {
    // Get string from clipboard
    String sgfContent =
        Optional.ofNullable(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null))
            .filter(cc -> cc.isDataFlavorSupported(DataFlavor.stringFlavor))
            .flatMap(
                cc -> {
                  try {
                    return Optional.of((String) cc.getTransferData(DataFlavor.stringFlavor));
                  } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                  return Optional.empty();
                })
            .orElse("");

    // Load game contents from sgf string
    if (!sgfContent.isEmpty()) {
      SGFParser.loadFromString(sgfContent);
    }
  }

  public void increaseMaxAlpha(int k) {
    boardRenderer.increaseMaxAlpha(k);
  }

  private void drawComment(Graphics2D g, int x, int y, int w, int h) {}

  public void replayBranch(boolean generateGif) {}

  public void removeEstimateRectInEDT() {}

  public void drawEstimateRectKataInEDT(ArrayList<Double> estimateArray) {}

  public void estimateByZen() {}

  public void drawEstimateRectZen(ArrayList<Double> estimateArray) {}

  public void noAutoEstimateByZen() {}

  public void noEstimateByZen(boolean byToolBar) {}

  @Override
  public void updateEngineMenu(List<Leelaz> engineList) {
    javax.swing.SwingUtilities.invokeLater(() -> menu.updateEngineMenu(engineList));
  }

  protected void updateEngineMenuInEDT(List<Leelaz> engineList) {
    Utils.mustBeEventDispatchThread();
    menu.updateEngineMenu(engineList);
  }

  @Override
  public void updateEngineIcon(List<Leelaz> engineList, int currentEngineNo) {
    javax.swing.SwingUtilities.invokeLater(
        () -> menu.updateEngineIcon(engineList, currentEngineNo));
  }

  protected void updateEngineIconInEDT(List<Leelaz> engineList, int currentEngineNo) {
    Utils.mustBeEventDispatchThread();
    menu.updateEngineIcon(engineList, currentEngineNo);
  }

  public Optional<int[]> convertScreenToCoordinates(int x, int y) {
    return boardRenderer.convertScreenToCoordinates(x, y);
  }

  public void updateScoreMenu(boolean on) {
    menu.updateScoreMenu(on);
  }

  public boolean openRightClickMenu(int x, int y) {
    return false;
  }

  private void showMenu(int x, int y) {}

  @Override
  public void clearBeforeMove() {}
}
