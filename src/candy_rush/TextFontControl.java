package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class TextFontControl extends JPanel
{
    private final JComboBox<String> fontCombo;
    private final JSpinner sizeSpinner;
    private final JToggleButton boldBtn, italicBtn;
    private final JButton colorBtn;
    private final JLabel colorSwatch;
    private final JTextField textField;

    private Color  textColor = Color.WHITE;
    private boolean textMode = false;

    private final TextCanvas textCanvas;
    private final BiConsumer<BufferedImage, String> onCommit;
    private Runnable onCancel;

    public TextFontControl(BiConsumer<BufferedImage, String> onCommit)
    {
        this.onCommit = onCommit;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(45, 45, 45));
        setBorder(new EmptyBorder(10, 8, 10, 8));
        setPreferredSize(new Dimension(170, 0));

        JLabel title = new JLabel("Text Tool");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);
        add(gap(10));

        add(sectionLabel("Font"));
        add(gap(3));

        String[] fonts = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        fontCombo = new JComboBox<>(fonts);
        fontCombo.setSelectedItem("Arial");
        fontCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        fontCombo.setAlignmentX(LEFT_ALIGNMENT);
        fontCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        fontCombo.setRenderer(new FontPreviewRenderer());
        add(fontCombo);
        add(gap(6));

        add(sectionLabel("Size"));
        add(gap(3));
        sizeSpinner = new JSpinner(new SpinnerNumberModel(36, 6, 512, 2));
        sizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        sizeSpinner.setAlignmentX(LEFT_ALIGNMENT);
        add(sizeSpinner);
        add(gap(6));

        add(sectionLabel("Style"));
        add(gap(3));
        JPanel styleRow = new JPanel(new GridLayout(1, 2, 4, 0));
        styleRow.setOpaque(false);
        styleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        styleRow.setAlignmentX(LEFT_ALIGNMENT);
        boldBtn = styleToggle("B", Font.BOLD);
        italicBtn = styleToggle("I", Font.ITALIC);
        styleRow.add(boldBtn);
        styleRow.add(italicBtn);
        add(styleRow);
        add(gap(6));

        add(sectionLabel("Color"));
        add(gap(3));
        JPanel colorRow = new JPanel(new BorderLayout(6, 0));
        colorRow.setOpaque(false);
        colorRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        colorRow.setAlignmentX(LEFT_ALIGNMENT);
        colorSwatch = new JLabel();
        colorSwatch.setOpaque(true);
        colorSwatch.setBackground(textColor);
        colorSwatch.setPreferredSize(new Dimension(26, 22));
        colorSwatch.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90)));
        colorBtn = new JButton("Choose…");
        colorBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        colorBtn.setFocusPainted(false);
        colorBtn.addActionListener(e -> pickColor());
        colorRow.add(colorSwatch, BorderLayout.WEST);
        colorRow.add(colorBtn, BorderLayout.CENTER);
        add(colorRow);
        add(gap(10));

        add(sectionLabel("Text"));
        add(gap(3));
        textField = new JTextField("< Lorem Ipsum >");
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        textField.setAlignmentX(LEFT_ALIGNMENT);
        textField.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(textField);
        add(gap(10));

        add(sectionLabel("Click canvas to place"));
        add(gap(8));

        JButton cancelBtn = toolBtn("Cancel");
        cancelBtn.setAlignmentX(LEFT_ALIGNMENT);
        cancelBtn.addActionListener(e -> {
            if (onCancel != null) onCancel.run(); });

        add(cancelBtn);
        add(Box.createVerticalGlue());

        textCanvas = new TextCanvas();
    }

    public void setOnCancel(Runnable r)
    {
        this.onCancel = r;
    }

    public TextCanvas getCanvas()
    {
        return textCanvas;
    }

    public void activate(BufferedImage image, double zoom)
    {
        textMode = true;
        textCanvas.setImage(image, zoom);
    }

    public void deactivate()
    {
        textMode = false;
        textCanvas.setImage(null, 1.0);
    }

    public void syncZoom(double zoom)
    {
        textCanvas.updateZoom(zoom);
    }

    private void pickColor()
    {
        Color chosen = JColorChooser.showDialog(this, "Text Color", textColor);
        if (chosen != null)
        {
            textColor = chosen;
            colorSwatch.setBackground(textColor);
        }
    }

    private Font buildFont()
    {
        String family = (String) fontCombo.getSelectedItem();
        int size = (int) sizeSpinner.getValue();
        int style = Font.PLAIN;
        if (boldBtn.isSelected()) style |= Font.BOLD;
        if (italicBtn.isSelected()) style |= Font.ITALIC;
        return new Font(family, style, size);
    }

    private void burnText(BufferedImage image, int imgX, int imgY)
    {
        String text = textField.getText();
        if (text == null || text.isBlank()) return;

        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setFont(buildFont());
        g.setColor(textColor);
        g.drawString(text, imgX, imgY);
        g.dispose();

        onCommit.accept(result, text);
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel l = new JLabel(text.toUpperCase());
        l.setForeground(new Color(120, 120, 120));
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static Component gap(int h)
    {
        return Box.createRigidArea(new Dimension(0, h));
    }

    private static JButton toolBtn(String label)
    {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return b;
    }

    private static JToggleButton styleToggle(String label, int style)
    {
        JToggleButton b = new JToggleButton(label);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", style, 13));
        return b;
    }

    private class FontPreviewRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String name)
            {
                try {
                    setFont(new Font(name, Font.PLAIN, 13));
                }
                catch (Exception ignored)
                {
                    setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
                setText(name);
            }
            return this;
        }
    }

    public class TextCanvas extends JPanel
    {
        private BufferedImage image;
        private double zoom   = 1.0;
        private Point cursor = null;

        TextCanvas()
        {
            setBackground(new Color(40, 40, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

            addMouseListener(new MouseAdapter()
            {
                @Override public void mousePressed(MouseEvent e)
                {
                    if (image == null) return;
                    int offX = offsetX();
                    int offY = offsetY();
                    int imgX = (int)((e.getX() - offX) / zoom);
                    int imgY = (int)((e.getY() - offY) / zoom);
                    imgX = Math.max(0, Math.min(imgX, image.getWidth()));
                    imgY = Math.max(0, Math.min(imgY, image.getHeight()));
                    burnText(image, imgX, imgY);
                }
            });

            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override public void mouseMoved(MouseEvent e)
                {
                    cursor = e.getPoint();
                    repaint();
                }
            });
        }

        public void setImage(BufferedImage img, double z)
        {
            this.image = img;
            this.zoom = z;
            this.cursor = null;
            repaint();
        }

        public void updateZoom(double z)
        {
            this.zoom = z;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0)
        {
            super.paintComponent(g0);
            if (image == null) return;

            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int offX = offsetX();
            int offY = offsetY();
            int sw = (int)(image.getWidth()  * zoom);
            int sh = (int)(image.getHeight() * zoom);

            g.drawImage(image, offX, offY, sw, sh, null);

            if (cursor != null)
            {
                int imgX = (int)((cursor.x - offX) / zoom);
                int imgY = (int)((cursor.y - offY) / zoom);
                if (imgX >= 0 && imgX <= image.getWidth() && imgY >= 0 && imgY <= image.getHeight())
                {
                    Font  previewFont = buildFont();
                    float scaledSize = (float)(previewFont.getSize() * zoom);
                    Font  screenFont = previewFont.deriveFont(scaledSize);

                    g.setFont(screenFont);
                    g.setColor(new Color(textColor.getRed(), textColor.getGreen(),textColor.getBlue(), 180));
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    String preview = textField.getText();
                    if (preview == null || preview.isBlank()) preview = "Your text here";
                    g.drawString(preview, cursor.x, cursor.y);
                    g.setColor(new Color(255, 255, 100, 160));
                    g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER, 1, new float[]{4, 3}, 0));
                    g.drawLine(cursor.x, offY, cursor.x, offY + sh);
                    g.drawLine(offX, cursor.y, offX + sw, cursor.y);
                    g.setStroke(new BasicStroke(1));
                }
            }
        }

        private int offsetX() { return Math.max(0, (getWidth() - (int)(image.getWidth() * zoom)) / 2); }
        private int offsetY() { return Math.max(0, (getHeight() - (int)(image.getHeight() * zoom)) / 2); }
    }
}
