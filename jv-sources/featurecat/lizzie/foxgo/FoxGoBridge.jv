package featurecat.lizzie.foxgo;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.MoveData;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * Main bridge class that coordinates Fox Go screen capture, stone detection, board sync, AI
 * analysis, and overlay display.
 *
 * <p>Polling loop (every 500ms): 1. Check Fox Go window is active 2. Capture board screenshot 3.
 * Detect new stones -> sync to Lizzie.board 4. Read AI engine analysis results 5. Draw
 * winrate/suggestions on overlay window
 */
public class FoxGoBridge {
  private static final int POLL_INTERVAL_MS = 500;

  private final FoxGoDetector detector;
  private final StoneDetector stoneDetector;
  private final OverlayWindow overlayWindow;

  private ScheduledExecutorService scheduler;
  private boolean running = false;
  private boolean autoDetectWindow = true;
  private List<MovePosition> lastKnownStones = new ArrayList<>();
  private int lastBoardHash = 0;

  public interface FoxGoListener {
    void onStatusChanged(String status);

    void onBoardSync(int stoneCount);
  }

  private FoxGoListener listener;

  public FoxGoBridge() {
    this.detector = new FoxGoDetector();
    this.stoneDetector = new StoneDetector();
    this.overlayWindow = new OverlayWindow();
  }

  public void setListener(FoxGoListener listener) {
    this.listener = listener;
  }

  public FoxGoDetector getDetector() {
    return detector;
  }

  public StoneDetector getStoneDetector() {
    return stoneDetector;
  }

  public OverlayWindow getOverlayWindow() {
    return overlayWindow;
  }

  public synchronized void start() {
    if (running) return;
    if (!stoneDetector.isCalibrated()) {
      JOptionPane.showMessageDialog(
          Lizzie.frame,
          "Please calibrate the board position first.\nSettings -> Fox Go AI Helper",
          "Info",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    running = true;
    lastKnownStones.clear();
    lastBoardHash = 0;

    overlayWindow.alignToBoard(
        stoneDetector.getX0(),
        stoneDetector.getY0(),
        stoneDetector.getX0() + (int) (18 * stoneDetector.getXGap()),
        stoneDetector.getY0() + (int) (18 * stoneDetector.getYGap()));
    overlayWindow.showOverlay();

    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::pollCycle, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

    System.out.println("[FoxGoBridge] Started sync.");
    if (listener != null) listener.onStatusChanged("Running...");
  }

  public synchronized void stop() {
    if (!running) return;
    running = false;
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
    overlayWindow.hideOverlay();
    System.out.println("[FoxGoBridge] Stopped sync.");
    if (listener != null) listener.onStatusChanged("Stopped");
  }

  public boolean isRunning() {
    return running;
  }

  public void setAutoDetectWindow(boolean auto) {
    this.autoDetectWindow = auto;
  }

  public void applyCalibration(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
    stoneDetector.calibrate(topLeftX, topLeftY, bottomRightX, bottomRightY);
  }

  public void shutdown() {
    stop();
  }

  private void pollCycle() {
    try {
      if (autoDetectWindow && !detector.isFoxGoActive()) return;

      Robot robot = new Robot();
      Rectangle boardRect =
          new Rectangle(
              stoneDetector.getX0() - 10,
              stoneDetector.getY0() - 10,
              (int) (19 * stoneDetector.getXGap()) + 20,
              (int) (19 * stoneDetector.getYGap()) + 20);
      BufferedImage screenshot = robot.createScreenCapture(boardRect);

      List<MovePosition> currentStones = stoneDetector.detectStones(screenshot);
      int currentHash = computeHash(currentStones);

      if (currentHash != lastBoardHash) {
        syncToBoard(currentStones, lastKnownStones);
        lastKnownStones = new ArrayList<>(currentStones);
        lastBoardHash = currentHash;
        if (listener != null) listener.onBoardSync(currentStones.size());
      }

      List<MoveData> bestMoves = (Lizzie.leelaz != null) ? Lizzie.leelaz.getBestMoves() : null;
      if (bestMoves != null && !bestMoves.isEmpty()) {
        overlayWindow.updateAnalysis(bestMoves);
      }
    } catch (AWTException e) {
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("[FoxGoBridge] Error: " + e.getMessage());
    }
  }

  private void syncToBoard(List<MovePosition> current, List<MovePosition> previous) {
    // Find newly placed stones
    List<MovePosition> newStones = new ArrayList<>(current);
    newStones.removeAll(previous);

    // Find removed stones (captured)
    List<MovePosition> removedStones = new ArrayList<>(previous);
    removedStones.removeAll(current);

    if (newStones.isEmpty() && removedStones.isEmpty()) return;

    SwingUtilities.invokeLater(
        () -> {
          if (!removedStones.isEmpty()) {
            // Rebuild full board state
            Lizzie.board.clear();
            Lizzie.board.clear();
            for (MovePosition mp : current) {
              Lizzie.board.place(mp.getCol(), mp.getRow(), mp.getStone());
            }
            System.out.println("[FoxGoBridge] Full rebuild: " + current.size() + " stones");
          } else {
            for (MovePosition mp : newStones) {
              Lizzie.board.place(mp.getCol(), mp.getRow(), mp.getStone());
              System.out.println("[FoxGoBridge] New: " + mp.getStone() + " " + mp.toCoordName());
            }
          }

          if (Lizzie.leelaz != null && !Lizzie.leelaz.isPondering()) {
            Lizzie.leelaz.togglePonder();
          }
        });
  }

  private int computeHash(List<MovePosition> stones) {
    int hash = 0;
    for (MovePosition mp : stones) hash = 31 * hash + mp.hashCode();
    return hash;
  }
}
