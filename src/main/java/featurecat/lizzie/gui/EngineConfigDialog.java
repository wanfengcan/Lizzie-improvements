package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class EngineConfigDialog extends LizzieDialog {

  private JTextField txtEnginePath;
  private JTextField txtModelPath;
  private JTextField txtExtraParams;
  private JLabel lblPreview;

  public EngineConfigDialog(java.awt.Frame owner) {
    super(owner, "引擎配置", true);
    setBounds(100, 100, 560, 420);
    setResizable(false);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 8, 4, 8);
    c.fill = GridBagConstraints.HORIZONTAL;

    // Row 0: 引擎程序
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    panel.add(new JLabel("KataGo 程序:", SwingConstants.RIGHT), c);

    c.gridx = 1;
    c.weightx = 1.0;
    txtEnginePath = new JTextField(30);
    panel.add(txtEnginePath, c);

    c.gridx = 2;
    c.weightx = 0;
    JButton btnBrowseEngine = new JButton("浏览...");
    panel.add(btnBrowseEngine, c);

    // Row 1: 模型文件
    c.gridx = 0;
    c.gridy = 1;
    panel.add(new JLabel("模型文件:", SwingConstants.RIGHT), c);

    c.gridx = 1;
    c.weightx = 1.0;
    txtModelPath = new JTextField(30);
    panel.add(txtModelPath, c);

    c.gridx = 2;
    c.weightx = 0;
    JButton btnBrowseModel = new JButton("浏览...");
    panel.add(btnBrowseModel, c);

    // Row 2: 追加参数
    c.gridx = 0;
    c.gridy = 2;
    panel.add(new JLabel("追加参数:", SwingConstants.RIGHT), c);

    c.gridx = 1;
    c.weightx = 1.0;
    txtExtraParams = new JTextField(30);
    panel.add(txtExtraParams, c);

    c.gridx = 2;
    c.weightx = 0;
    panel.add(new JLabel(""), c);

    // Row 3: 参数说明
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 3;
    c.insets = new Insets(8, 12, 2, 12);
    JTextArea txtHelp = new JTextArea(
        "参数示例:\n"
            + "-config xxx.cfg    指定 GTP 配置文件\n"
            + "-override-version 15    覆盖版本号\n"
            + "-analysis-threads 4    限制分析线程数\n"
            + "参数用空格分隔");
    txtHelp.setEditable(false);
    txtHelp.setOpaque(false);
    txtHelp.setFont(txtHelp.getFont().deriveFont(11.0f));
    panel.add(txtHelp, c);

    // Row 4: 预览标签
    c.gridy = 4;
    c.insets = new Insets(8, 8, 2, 8);
    lblPreview = new JLabel("预览: ");
    lblPreview.setFont(lblPreview.getFont().deriveFont(11.0f));
    panel.add(lblPreview, c);

    // Row 5: 分隔线
    c.gridy = 5;
    c.insets = new Insets(4, 8, 4, 8);
    panel.add(new JSeparator(), c);

    // Row 6: 按钮
    c.gridy = 6;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.CENTER;
    JPanel btnPanel = new JPanel();
    JButton btnSave = new JButton("保存");
    JButton btnCancel = new JButton("取消");
    btnPanel.add(btnSave);
    btnPanel.add(btnCancel);
    panel.add(btnPanel, c);

    getContentPane().add(panel);

    // == 事件处理 ==

    // DocumentListener: 实时更新预览
    DocumentListener previewUpdater = new DocumentListener() {
      public void changedUpdate(DocumentEvent e) { updatePreview(); }
      public void insertUpdate(DocumentEvent e) { updatePreview(); }
      public void removeUpdate(DocumentEvent e) { updatePreview(); }
    };
    txtEnginePath.getDocument().addDocumentListener(previewUpdater);
    txtModelPath.getDocument().addDocumentListener(previewUpdater);
    txtExtraParams.getDocument().addDocumentListener(previewUpdater);

    // 浏览引擎程序
    btnBrowseEngine.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("可执行文件 (*.exe)", "exe"));
        chooser.setDialogTitle("选择 KataGo 引擎程序");
        if (chooser.showOpenDialog(EngineConfigDialog.this) == JFileChooser.APPROVE_OPTION) {
          txtEnginePath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
      }
    });

    // 浏览模型文件
    btnBrowseModel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "模型文件 (*.gz, *.bin.gz, *.txt, *.txt.gz)",
            "gz", "txt");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("选择模型文件");
        if (chooser.showOpenDialog(EngineConfigDialog.this) == JFileChooser.APPROVE_OPTION) {
          txtModelPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
      }
    });

    // 保存
    btnSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String enginePath = txtEnginePath.getText().trim();
        String modelPath = txtModelPath.getText().trim();

        if (enginePath.isEmpty()) {
          JOptionPane.showMessageDialog(EngineConfigDialog.this,
              "请指定 KataGo 引擎程序路径。", "输入校验", JOptionPane.WARNING_MESSAGE);
          return;
        }
        if (modelPath.isEmpty()) {
          JOptionPane.showMessageDialog(EngineConfigDialog.this,
              "请指定模型文件路径。", "输入校验", JOptionPane.WARNING_MESSAGE);
          return;
        }

        StringBuilder cmd = new StringBuilder(enginePath);
        cmd.append(" -model ").append(modelPath);
        String extra = txtExtraParams.getText().trim();
        if (!extra.isEmpty()) {
          cmd.append(" ").append(extra);
        }

        org.json.JSONObject leelazCfg = Lizzie.config.leelazConfig;
        if (leelazCfg == null) {
          JOptionPane.showMessageDialog(EngineConfigDialog.this,
              "配置对象未初始化，无法保存。", "错误", JOptionPane.ERROR_MESSAGE);
          return;
        }
        leelazCfg.put("engine-command", cmd.toString());
        try {
          Lizzie.config.save();
        } catch (java.io.IOException ex) {
          JOptionPane.showMessageDialog(EngineConfigDialog.this,
              "保存配置失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
          return;
        }

        JOptionPane.showMessageDialog(EngineConfigDialog.this,
            "配置已保存，请重启程序生效。");
        dispose();
      }
    });

    // 取消
    btnCancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    // 初始化字段
    loadCurrentConfig();
    updatePreview();
    setLocationRelativeTo(owner);
  }

  private void loadCurrentConfig() {
    try {
      org.json.JSONObject leelazCfg = Lizzie.config.leelazConfig;
      if (leelazCfg == null) {
        return;
      }
      String command = leelazCfg.optString("engine-command", "");
      if (command.isEmpty()) {
        return;
      }

      // 解析: 按 " -model " 分割
      String modelFlag = " -model ";
      int modelIdx = command.indexOf(modelFlag);
      if (modelIdx < 0) {
        // 没有 -model 参数，整个作为引擎路径
        txtEnginePath.setText(command);
        return;
      }

      String beforeModel = command.substring(0, modelIdx);
      String afterModel = command.substring(modelIdx + modelFlag.length());

      txtEnginePath.setText(beforeModel.trim());

      // 分离模型路径和追加参数
      int firstSpace = afterModel.indexOf(' ');
      if (firstSpace > 0) {
        txtModelPath.setText(afterModel.substring(0, firstSpace).trim());
        txtExtraParams.setText(afterModel.substring(firstSpace).trim());
      } else {
        txtModelPath.setText(afterModel.trim());
      }
    } catch (Exception ex) {
      // 配置读取失败时静默忽略，用户可手动输入
    }
  }

  private void updatePreview() {
    String enginePath = txtEnginePath.getText().trim();
    String modelPath = txtModelPath.getText().trim();
    String extra = txtExtraParams.getText().trim();

    StringBuilder preview = new StringBuilder("预览: ");
    if (!enginePath.isEmpty()) {
      preview.append(enginePath);
    }
    if (!modelPath.isEmpty()) {
      preview.append(" -model ").append(modelPath);
    }
    if (!extra.isEmpty()) {
      preview.append(" ").append(extra);
    }
    lblPreview.setText(preview.toString());
  }
}
