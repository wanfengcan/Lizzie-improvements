package featurecat.lizzie.foxgo;

import featurecat.lizzie.rules.Stone;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects Go stones from a screenshot of the Fox Go board. Uses RGB pixel threshold detection: -
 * Black stones: RGB all dark, red < 10 - White stones: RGB all bright, red > 180 - Board
 * background: intermediate values
 */
public class StoneDetector {
  public static final int BOARD_SIZE = 19;

  private int x0, y0; // top-left screen coordinate
  private int x1, y1; // bottom-right screen coordinate
  private double xGap, yGap; // cell spacing
  private boolean calibrated = false;

  public void calibrate(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
    this.x0 = topLeftX;
    this.y0 = topLeftY;
    this.x1 = bottomRightX;
    this.y1 = bottomRightY;
    this.xGap = (double) (x1 - x0) / (BOARD_SIZE - 1);
    this.yGap = (double) (y1 - y0) / (BOARD_SIZE - 1);
    this.calibrated = true;
  }

  public boolean isCalibrated() {
    return calibrated;
  }

  public double getXGap() {
    return xGap;
  }

  public double getYGap() {
    return yGap;
  }

  public int getX0() {
    return x0;
  }

  public int getY0() {
    return y0;
  }

  /**
   * Detect all stones on the board using multi-point voting. More robust than single-pixel
   * detection.
   */
  public List<MovePosition> detectStones(BufferedImage screenshot) {
    if (!calibrated) throw new IllegalStateException("StoneDetector not calibrated");
    List<MovePosition> stones = new ArrayList<>();
    for (int row = 0; row < BOARD_SIZE; row++) {
      for (int col = 0; col < BOARD_SIZE; col++) {
        Stone s = sampleStone(screenshot, row, col);
        if (s != Stone.EMPTY) stones.add(new MovePosition(row, col, s));
      }
    }
    return stones;
  }

  /** Sample multiple pixels around an intersection and vote on stone color */
  private Stone sampleStone(BufferedImage screenshot, int row, int col) {
    // Center + 8 offset positions for voting
    int[][] offsets = {
      {0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}
    };
    int blackVotes = 0, whiteVotes = 0;
    for (int[] off : offsets) {
      int px = (int) Math.round(x0 + col * xGap + off[0]);
      int py = (int) Math.round(y0 + row * yGap + off[1]);
      if (px < 0 || px >= screenshot.getWidth() || py < 0 || py >= screenshot.getHeight()) continue;
      int pixel = screenshot.getRGB(px, py);
      int r = (pixel >> 16) & 0xFF;
      int g = (pixel >> 8) & 0xFF;
      int b = pixel & 0xFF;
      if (r < 10 && g < 10 && b < 10) blackVotes++;
      else if (r > 180 && g > 180 && b > 180) whiteVotes++;
    }
    int total = blackVotes + whiteVotes;
    if (total < 5) return Stone.EMPTY;
    if (blackVotes > total * 0.6) return Stone.BLACK;
    if (whiteVotes > total * 0.6) return Stone.WHITE;
    return Stone.EMPTY;
  }
}
