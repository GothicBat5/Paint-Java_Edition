package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ColorGradeWindow extends JFrame
{
    static class Curve
    {
        final List<Point2D> pts = new ArrayList<>();

        Curve()
        {
            pts.add(new Point2D(0.0, 0.0));
            pts.add(new Point2D(1.0, 1.0));
        }

        int[] buildLUT()
        {
            int[] lut = new int[256];
            for (int i = 0; i < 256; i++)
            {
                double x = i / 255.0;
                lut[i] = (int) Math.round(clamp01(sample(x)) * 255);
            }
            return lut;
        }

        private double sample(double x)
        {
            if (pts.size() == 1) return pts.get(0).y;
            if (x <= pts.get(0).x) return pts.get(0).y;
            if (x >= pts.get(pts.size()-1).x) return pts.get(pts.size()-1).y;

            for (int i = 0; i < pts.size() - 1; i++)
            {
                Point2D p0 = pts.get(i);
                Point2D p1 = pts.get(i + 1);
                if (x >= p0.x && x <= p1.x)
                {
                    Point2D pm = i > 0 ? pts.get(i - 1) : new Point2D(p0.x - (p1.x - p0.x), p0.y - (p1.y - p0.y));
                    Point2D p2 = i < pts.size() - 2  ? pts.get(i + 2) : new Point2D(p1.x + (p1.x - p0.x), p1.y + (p1.y - p0.y));

                    double t = (x - p0.x) / (p1.x - p0.x);
                    return catmullRom(pm.y, p0.y, p1.y, p2.y, t);
                }
            }
            return x;
        }

        private static double catmullRom(double p0, double p1, double p2, double p3, double t)
        {
            return 0.5 * ((2*p1) + (-p0+p2)*t + (2*p0-5*p1+4*p2-p3)*t*t + (-p0+3*p1-3*p2+p3)*t*t*t);
        }

        private static double clamp01(double v)
        {
            return Math.max(0, Math.min(1, v));
        }

        void reset()
        {
            pts.clear();
            pts.add(new Point2D(0.0, 0.0));
            pts.add(new Point2D(1.0, 1.0));
        }
    }

    static class Point2D
    {
        double x, y;
        Point2D(double x, double y) { this.x = x; this.y = y; }
    }

    private enum Channel { RGB, R, G, B }
    private final Curve curveRGB = new Curve();
    private final Curve curveR = new Curve();
    private final Curve curveG = new Curve();
    private final Curve curveB = new Curve();
    private Channel activeChannel = Channel.RGB;

    private final JSlider slExposure = slider(-100, 100, 0);
    private final JSlider slSaturation = slider(-100, 100, 0);
    private final JSlider slTemperature = slider(-100, 100, 0);
    private final JSlider slTint = slider(-100, 100, 0);
    private final JSlider slHighlights = slider(-100, 100, 0);
    private final JSlider slShadows = slider(-100, 100, 0);

    private final BufferedImage sourceImage;
    private final Consumer<BufferedImage> previewFn;
    private final Runnable commitFn;

    private BufferedImage lastPreview;

    private CurveCanvas curveCanvas;

    public ColorGradeWindow(BufferedImage source, Consumer<BufferedImage> previewFn, Runnable commitFn)
    {
        super("Color Grading — sRGB Curves");
        this.sourceImage = source;
        this.previewFn = previewFn;
        this.commitFn = commitFn;

        setSize(780, 600);
        setMinimumSize(new Dimension(700, 520));
        setLocationByPlatform(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildUI();
        attachSliderListeners();

        setVisible(true);
        schedulePreview();
    }
    private void buildUI()
    {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(30, 30, 30));
        setContentPane(root);

        JPanel leftPanel = new JPanel(new BorderLayout(4, 6));
        leftPanel.setOpaque(false);

        curveCanvas = new CurveCanvas();
        curveCanvas.setPreferredSize(new Dimension(340, 340));

        JPanel channelTabs = buildChannelTabs();
        JButton resetCurveBtn = darkButton("Reset Curve");

        resetCurveBtn.addActionListener(e -> {
            activeCurve().reset(); curveCanvas.repaint(); schedulePreview();
        });

        JPanel curveTop = new JPanel(new BorderLayout());
        curveTop.setOpaque(false);
        curveTop.add(channelTabs, BorderLayout.CENTER);
        curveTop.add(resetCurveBtn, BorderLayout.EAST);

        leftPanel.add(curveTop, BorderLayout.NORTH);
        leftPanel.add(curveCanvas, BorderLayout.CENTER);


        JPanel rightPanel = buildSliderPanel();

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomBar.setOpaque(false);

        JButton cancelBtn = darkButton("Cancel");
        cancelBtn.addActionListener(e -> {
            previewFn.accept(sourceImage);
            dispose();
        });

        JButton applyBtn = darkButton("Apply");
        applyBtn.setBackground(new Color(0, 120, 215));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.addActionListener(e -> {
            schedulePreview();
            commitFn.run();
            dispose();
        });

        bottomBar.add(cancelBtn);
        bottomBar.add(applyBtn);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);

        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(6);
        split.setDividerLocation(370);
        split.setResizeWeight(0.55);

        root.add(split, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildChannelTabs()
    {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setOpaque(false);

        for (Channel ch : Channel.values())
        {
            JToggleButton btn = new JToggleButton(ch.name());
            btn.setSelected(ch == Channel.RGB);
            styleChannelBtn(btn, ch);
            btn.addActionListener(e -> {
                activeChannel = ch;
                curveCanvas.repaint();
            });
            p.add(btn);
        }
        ButtonGroup bg = new ButtonGroup();
        for (Component c : p.getComponents())
            if (c instanceof JToggleButton)
                bg.add((JToggleButton) c);

        return p;
    }

    private void styleChannelBtn(JToggleButton btn, Channel ch)
    {
        btn.setFocusable(false);
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(52, 24));

        Color fg = switch (ch) {
            case R -> new Color(255, 80,  80);
            case G -> new Color(80,  210, 80);
            case B -> new Color(80,  140, 255);
            default -> Color.WHITE;
        };
        btn.setForeground(fg);
        btn.setBackground(new Color(50, 50, 50));
        btn.setBorderPainted(true);
    }

    private JPanel buildSliderPanel()
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(38, 38, 38));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)),
                new EmptyBorder(10, 12, 10, 12)));

        p.add(sectionLabel("Global Adjustments"));
        p.add(Box.createVerticalStrut(6));
        p.add(sliderRow("Exposure",    slExposure));
        p.add(sliderRow("Saturation",  slSaturation));
        p.add(Box.createVerticalStrut(8));
        p.add(sectionLabel("Color Balance"));
        p.add(Box.createVerticalStrut(6));
        p.add(sliderRow("Temperature", slTemperature));
        p.add(sliderRow("Tint",        slTint));
        p.add(Box.createVerticalStrut(8));
        p.add(sectionLabel("Tone"));
        p.add(Box.createVerticalStrut(6));
        p.add(sliderRow("Highlights",  slHighlights));
        p.add(sliderRow("Shadows",     slShadows));
        p.add(Box.createVerticalGlue());

        JButton resetAllBtn = darkButton("Reset All");
        resetAllBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetAllBtn.addActionListener(e -> resetAll());
        p.add(Box.createVerticalStrut(10));
        p.add(resetAllBtn);

        return p;
    }

    private JComponent sliderRow(String label, JSlider sl)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(190, 190, 190));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setPreferredSize(new Dimension(82, 20));

        JLabel val = new JLabel(String.valueOf(sl.getValue()));
        val.setForeground(new Color(160, 160, 160));
        val.setFont(new Font("Monospaced", Font.PLAIN, 11));
        val.setPreferredSize(new Dimension(32, 20));
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        sl.addChangeListener(e -> val.setText(String.valueOf(sl.getValue())));

        styleSlider(sl);
        row.add(lbl, BorderLayout.WEST);
        row.add(sl, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JLabel sectionLabel(String text)
    {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(new Color(120, 120, 120));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
    private void attachSliderListeners()
    {
        for (JSlider sl : new JSlider[]{ slExposure, slSaturation, slTemperature, slTint, slHighlights, slShadows })
            sl.addChangeListener(e -> schedulePreview());
    }

    private volatile boolean previewPending = false;

    private void schedulePreview()
    {
        if (previewPending) return;
        previewPending = true;
        SwingUtilities.invokeLater(() -> {
            previewPending = false;
            lastPreview = applyAll(sourceImage);
            previewFn.accept(lastPreview);
        });
    }

    BufferedImage applyAll(BufferedImage src)
    {

        int[] lutRGB = curveRGB.buildLUT();
        int[] lutR = curveR.buildLUT();
        int[] lutG = curveG.buildLUT();
        int[] lutB = curveB.buildLUT();
        int[] finalR = new int[256];
        int[] finalG = new int[256];
        int[] finalB = new int[256];

        for (int i = 0; i < 256; i++)
        {
            finalR[i] = lutR[lutRGB[i]];
            finalG[i] = lutG[lutRGB[i]];
            finalB[i] = lutB[lutRGB[i]];
        }

        float exposure = slExposure.getValue() / 100.0f;
        float saturation = slSaturation.getValue() / 100.0f;
        float temperature = slTemperature.getValue() / 100.0f;
        float tint = slTint.getValue() / 100.0f;
        float highlights = slHighlights.getValue() / 100.0f;
        float shadows = slShadows.getValue() / 100.0f;

        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = src.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb  & 0xff;

                r = finalR[r];
                g = finalG[g];
                b = finalB[b];

                float expFactor = (float)Math.pow(2.0, exposure * 2.0);
                r = clamp((int)(r * expFactor));
                g = clamp((int)(g * expFactor));
                b = clamp((int)(b * expFactor));

                r = clamp(r + (int)(temperature * 40));
                b = clamp(b - (int)(temperature * 40));

                g = clamp(g + (int)(tint * 30));
                r = clamp(r - (int)(tint * 15));
                b = clamp(b - (int)(tint * 15));

                int lum = (int)(0.299*r + 0.587*g + 0.114*b);
                float satScale = 1.0f + saturation;
                r = clamp((int)(lum + satScale * (r - lum)));
                g = clamp((int)(lum + satScale * (g - lum)));
                b = clamp((int)(lum + satScale * (b - lum)));

                float norm = (r + g + b) / (3.0f * 255);
                if (highlights != 0)
                {
                    float hMask = norm * norm;

                    int delta = (int)(highlights * 60 * hMask);
                    r = clamp(r + delta);
                    g = clamp(g + delta);
                    b = clamp(b + delta);
                }
                if (shadows != 0)
                {
                    float sMask = (1.0f - norm) * (1.0f - norm);
                    int delta = (int)(shadows * 60 * sMask);
                    r = clamp(r + delta);
                    g = clamp(g + delta);
                    b = clamp(b + delta);
                }

                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }

        return out;
    }

    private static int clamp(int v)
    {
        return Math.max(0, Math.min(255, v));
    }

    private Curve activeCurve()
    {
        return switch (activeChannel) {
            case R -> curveR;
            case G -> curveG;
            case B -> curveB;
            default -> curveRGB;
        };
    }

    private Color activeColor()
    {
        return switch (activeChannel)
        {
            case R -> new Color(255, 80, 80);
            case G -> new Color(80, 210, 80);
            case B -> new Color(80, 140, 255);
            default -> new Color(220, 220, 220);
        };
    }

    private void resetAll()
    {
        curveRGB.reset(); curveR.reset(); curveG.reset(); curveB.reset();
        slExposure.setValue(0);  slSaturation.setValue(0);
        slTemperature.setValue(0); slTint.setValue(0);
        slHighlights.setValue(0); slShadows.setValue(0);
        curveCanvas.repaint();
        schedulePreview();
    }

    private static JSlider slider(int min, int max, int value)
    {
        JSlider sl = new JSlider(min, max, value);
        sl.setOpaque(false);
        return sl;
    }

    private static void styleSlider(JSlider sl)
    {
        sl.setOpaque(false);
        sl.setForeground(new Color(150, 150, 150));
    }

    private static JButton darkButton(String label)
    {
        JButton btn = new JButton(label);
        btn.setBackground(new Color(60, 60, 60));
        btn.setForeground(new Color(220, 220, 220));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        return btn;
    }

    private class CurveCanvas extends JPanel
    {
        private static final int PAD = 20;
        private static final int HIT = 8;

        private Point2D dragging = null;

        CurveCanvas()
        {
            setBackground(new Color(22, 22, 22));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));

            addMouseListener(new MouseAdapter()
            {
                @Override public void mousePressed(MouseEvent e)
                {
                    Curve curve = activeCurve();
                    Point2D hit = hitTest(e.getPoint(), curve);

                    if (SwingUtilities.isRightMouseButton(e))
                    {

                        if (hit != null && curve.pts.size() > 2)
                        {
                            curve.pts.remove(hit);
                            repaint();
                            schedulePreview();
                        }
                        return;
                    }

                    if (hit != null)
                        dragging = hit;
                    else
                    {
                        Point2D np = toNorm(e.getPoint());
                        curve.pts.add(np);
                        curve.pts.sort((a, b) -> Double.compare(a.x, b.x));
                        dragging = np;
                        repaint();
                        schedulePreview();
                    }
                }

                @Override public void mouseReleased(MouseEvent e)
                {
                    dragging = null;
                    schedulePreview();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override public void mouseDragged(MouseEvent e)
                {
                    if (dragging == null) return;
                    Curve curve = activeCurve();
                    Point2D norm = toNorm(e.getPoint());

                    int idx = curve.pts.indexOf(dragging);
                    double minX = idx > 0 ? curve.pts.get(idx-1).x + 0.01 : 0.0;
                    double maxX = idx < curve.pts.size()-1 ? curve.pts.get(idx+1).x - 0.01 : 1.0;
                    dragging.x = Math.max(minX, Math.min(maxX, norm.x));
                    dragging.y = Math.max(0, Math.min(1, norm.y));

                    repaint();
                    schedulePreview();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0)
        {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - PAD * 2;
            int h = getHeight() - PAD * 2;

            g.setColor(new Color(45, 45, 45));

            for (int i = 1; i < 4; i++)
            {
                int gx = PAD + i * w / 4;
                int gy = PAD + i * h / 4;
                g.drawLine(gx, PAD, gx, PAD + h);
                g.drawLine(PAD, gy, PAD + w, gy);
            }

            g.setColor(new Color(60, 60, 60));
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1, new float[]{4, 4}, 0));
            g.drawLine(PAD, PAD + h, PAD + w, PAD);
            g.setStroke(new BasicStroke(1));

            drawHistogram(g, w, h);
            Curve curve = activeCurve();
            Color lineCol = activeColor();
            drawSpline(g, curve, lineCol, w, h);

            for (Point2D pt : curve.pts)
            {
                int px = PAD + (int)(pt.x * w);
                int py = PAD + (int)((1 - pt.y) * h);

                g.setColor(lineCol);
                g.fillOval(px - 5, py - 5, 10, 10);
                g.setColor(Color.WHITE);
                g.drawOval(px - 5, py - 5, 10, 10);
            }
            g.setColor(new Color(100, 100, 100));
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.drawString(activeChannel.name(), PAD + 4, PAD + 16);
        }

        private void drawSpline(Graphics2D g, Curve curve, Color col, int w, int h)
        {
            Path2D path = new Path2D.Double();
            boolean first = true;

            for (int px = 0; px <= w; px++)
            {
                double nx = px / (double) w;
                double ny = 1 - curve.buildLUT()[(int)(nx * 255)] / 255.0;
                int sx = PAD + px;
                int sy = PAD + (int)(ny * h);
                if (first) { path.moveTo(sx, sy); first = false; }
                else path.lineTo(sx, sy);
            }
            g.setColor(col);
            g.setStroke(new BasicStroke(2));
            g.draw(path);
            g.setStroke(new BasicStroke(1));
        }

        private void drawHistogram(Graphics2D g, int w, int h)
        {


            int[] hist = new int[256];
            int sw = sourceImage.getWidth(), sh = sourceImage.getHeight();
            int step = Math.max(1, (sw * sh) / 50_000);

            int count = 0;
            for (int y = 0; y < sh; y++)
                for (int x = 0; x < sw; x += step)
                {
                    int rgb = sourceImage.getRGB(x, y);
                    int lum = (int)(0.299*((rgb>>16)&0xff) + 0.587*((rgb>>8)&0xff) + 0.114*(rgb&0xff));
                    hist[lum]++;
                    count++;
                }
            int peak = 0;
            for (int v : hist) if (v > peak) peak = v;
            if (peak == 0) return;

            g.setColor(new Color(60, 60, 60, 180));
            for (int i = 0; i < 256; i++)
            {
                int barH = (int)((hist[i] / (double) peak) * h);
                int bx = PAD + (int)(i / 255.0 * w);
                g.fillRect(bx, PAD + h - barH, Math.max(1, w/256), barH);
            }
        }

        private Point2D toNorm(java.awt.Point p)
        {
            int w = getWidth() - PAD * 2;
            int h = getHeight() - PAD * 2;
            double nx = (p.x - PAD) / (double) w;
            double ny = 1.0 - (p.y - PAD) / (double) h;
            return new Point2D(Math.max(0,Math.min(1,nx)), Math.max(0,Math.min(1,ny)));
        }

        private Point2D hitTest(java.awt.Point p, Curve curve)
        {
            int w = getWidth() - PAD * 2;
            int h = getHeight() - PAD * 2;
          
            for (Point2D pt : curve.pts)
            {
                int px = PAD + (int)(pt.x * w);
                int py = PAD + (int)((1 - pt.y) * h);
                if (Math.abs(p.x - px) <= HIT && Math.abs(p.y - py) <= HIT)
                    return pt;
            }
            return null;
        }
    }

    public BufferedImage getLastPreview()
    {
        return lastPreview;
    }
}
