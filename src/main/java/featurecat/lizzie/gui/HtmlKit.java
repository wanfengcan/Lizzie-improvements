package featurecat.lizzie.gui;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class HtmlKit extends HTMLEditorKit {
  private StyleSheet style = new StyleSheet();

  @Override
  public void setStyleSheet(StyleSheet styleSheet) {
    style = styleSheet;
  }

  @Override
  public StyleSheet getStyleSheet() {
    if (style == null) {
      style = super.getStyleSheet();
    }
    return style;
  }
}
