package featurecat.lizzie.foxgo;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import java.awt.Rectangle;

/**
 * Detects Fox Go window using JNA Win32 API. Monitors foreground window title for Fox Go keywords.
 */
public class FoxGoDetector {
  private static final String[] FOX_TITLE_KEYWORDS = {
    "棋谱欣赏", "对局", "对弈中", "升降级", "棋谱", "Fox", "野狐", "fox"
  };

  private Rectangle boardRect;
  private boolean rectReady = false;

  /** Check if Fox Go window is currently active */
  public boolean isFoxGoActive() {
    try {
      WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
      char[] buffer = new char[1024];
      User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
      String title = Native.toString(buffer).trim();
      if (title.isEmpty()) return false;
      for (String keyword : FOX_TITLE_KEYWORDS) {
        if (title.contains(keyword)) return true;
      }
    } catch (Exception e) {
      // JNA not available or non-Windows platform
    }
    return false;
  }

  /** Get foreground window bounding rectangle */
  public Rectangle getWindowRect() {
    try {
      WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
      WinDef.RECT rect = new WinDef.RECT();
      User32.INSTANCE.GetWindowRect(hwnd, rect);
      return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    } catch (Exception e) {
      return null;
    }
  }

  public Rectangle getBoardRect() {
    return boardRect;
  }

  public void setBoardRect(int x0, int y0, int x1, int y1) {
    this.boardRect = new Rectangle(x0, y0, x1 - x0, y1 - y0);
    this.rectReady = true;
  }

  public boolean isRectReady() {
    return rectReady;
  }

  /** Get current foreground window title */
  public static String getForegroundWindowTitle() {
    try {
      WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
      char[] buffer = new char[1024];
      User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
      return Native.toString(buffer).trim();
    } catch (Exception e) {
      return "";
    }
  }
}
