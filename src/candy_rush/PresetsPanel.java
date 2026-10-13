package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class PresetsPanel extends JPanel
{
    public record Preset(String name, java.util.function.Function<BufferedImage, BufferedImage> fn) {}

    private static final Color BG = new Color(245, 248, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color ACCENT = new Color(27, 197, 91);
    private static final Color GOLD = new Color(180, 130, 30);
    private static final Color TEXT_DIM = new Color(100, 110, 120);
    private static final Color BORDER_COL = new Color(210, 220, 230);
    private static final Color SEL_RING = new Color(7, 126, 51);

    private final java.util.List<Preset> presets = buildPresets();
    private int selected = -1;
    private final Consumer<BufferedImage> onApply;
    private BufferedImage currentImage;

    public PresetsPanel(Consumer<BufferedImage> onApply)
    {
        this.onApply = onApply;
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);

        JLabel header = new JLabel("  Presets");
        header.setFont(new Font("Georgia", Font.BOLD, 14));
        header.setForeground(ACCENT);
        header.setBorder(new EmptyBorder(10, 6, 6, 6));
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setBackground(BG);
        grid.setBorder(new EmptyBorder(4, 8, 8, 8));

        for (int i = 0; i < presets.size(); i++)
        {
            final int idx = i;
            Preset p = presets.get(i);
            JButton card = buildCard(p.name(), idx);
            card.addActionListener(e -> selectPreset(idx));
            grid.add(card);
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        JButton applyBtn = new JButton("Apply Preset");
        applyBtn.setFont(new Font("Georgia", Font.BOLD, 12));
        applyBtn.setBackground(ACCENT);
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFocusPainted(false);
        applyBtn.setBorderPainted(false);
        applyBtn.setOpaque(true);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.setBorder(new EmptyBorder(9, 16, 9, 16));
        applyBtn.addActionListener(e -> applySelected());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        south.setBackground(BG);
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        south.add(applyBtn);
        add(south, BorderLayout.SOUTH);
    }

    private JButton buildCard(String name, int idx)
    {
        JButton btn = new JButton(name)
        {
            @Override
            protected void paintComponent(Graphics g0)
            {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = selected == idx;
                g.setColor(sel ? new Color(230, 240, 232) : CARD_BG);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                if (sel)
                {
                    g.setColor(SEL_RING);
                    g.setStroke(new BasicStroke(2));
                    g.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
                }
                else {
                    g.setColor(BORDER_COL);
                    g.setStroke(new BasicStroke(1));
                    g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                }
                g.setFont(new Font("Georgia", Font.PLAIN, 11));
                g.setColor(sel ? ACCENT : TEXT_DIM);
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(name)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(name, tx, ty);
            }

            @Override
            public Dimension getPreferredSize()
            {
                return new Dimension(90, 38);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void selectPreset(int idx)
    {
        selected = idx;
        repaint();
    }

    public void setImage(BufferedImage img)
    {
        this.currentImage = img;
    }

    private void applySelected()
    {
        if (selected < 0 || currentImage == null) return;

        BufferedImage result = presets.get(selected).fn().apply(currentImage);
        onApply.accept(result);
    }

    private java.util.List<Preset> buildPresets()
    {
        java.util.List<Preset> list = new java.util.ArrayList<>();

        list.add(new Preset("Vivid",img -> Effex.brightnessContrast(Effex.warmth(img), 10, 40)));
        list.add(new Preset("Fade",img -> {
            BufferedImage b = Effex.brightnessContrast(img, 30, -60);
            return blendWithWhite(b, 0.18f);
        }));
        list.add(new Preset("Noir",img -> Effex.brightnessContrast(Effex.toGrayscale(img), 0, 60)));
        list.add(new Preset("Chrome",img -> Effex.brightnessContrast(Effex.cool(Effex.toGrayscale(img)), 10, 50)));
        list.add(new Preset("Golden Hr.",img -> goldenHour(img)));
        list.add(new Preset("Matte",img -> matte(img)));
        list.add(new Preset("Arctic",img -> arctic(img)));
        list.add(new Preset("Sepia+",img -> Effex.brightnessContrast(Effex.sepia(img), 5, 20)));
        list.add(new Preset("Pop",img -> pop(img)));
        list.add(new Preset("Dream",img -> dream(img)));
        list.add(new Preset("Ink",img -> ink(img)));
        list.add(new Preset("Cinematic",img -> cinematic(img)));
        list.add(new Preset("Sunrise",img -> sunrise(img)));
        list.add(new Preset("Soft Glow",img -> softGlow(img)));

        return list;
    }

    private BufferedImage goldenHour(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp(((rgb >> 16) & 0xff) + 40);
                int g = clamp(((rgb >> 8)  & 0xff) + 15);
                int b = clamp((rgb & 0xff) - 40);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return Effex.brightnessContrast(out, 5, 15);
    }

    private BufferedImage matte(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp((int)(((rgb >> 16) & 0xff) * 0.85 + 30));
                int g = clamp((int)(((rgb >> 8)  & 0xff) * 0.85 + 28));
                int b = clamp((int)((rgb & 0xff) * 0.85 + 35));
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    private BufferedImage arctic(BufferedImage img)
    {
        BufferedImage cold = Effex.cool(img);
        return Effex.brightnessContrast(cold, 20, -20);
    }

    private BufferedImage pop(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8)  & 0xff;
                int b = rgb & 0xff;
                int lum = (int)(0.299*r + 0.587*g + 0.114*b);
                float sat = 1.6f;
                r = clamp((int)(lum + sat * (r - lum)));
                g = clamp((int)(lum + sat * (g - lum)));
                b = clamp((int)(lum + sat * (b - lum)));
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return Effex.brightnessContrast(out, 0, 25);
    }

    private BufferedImage dream(BufferedImage img)
    {
        BufferedImage blurred = Effex.blur(img, 3);
        return blendImages(img, blurred, 0.55f);
    }

    private BufferedImage ink(BufferedImage img)
    {
        BufferedImage gray = Effex.toGrayscale(img);
        return Effex.brightnessContrast(gray, -10, 80);
    }

    private BufferedImage cinematic(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage toned = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp((int)(((rgb >> 16) & 0xff) * 0.95 + 5));
                int g = clamp((int)(((rgb >> 8)  & 0xff) * 0.92));
                int b = clamp((int)((rgb & 0xff) * 0.88 + 10));
                toned.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return Effex.vignette(Effex.brightnessContrast(toned, -5, 20), 0.5f);
    }

    private BufferedImage sunrise(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp(((rgb >> 16) & 0xff) + 35);
                int g = clamp(((rgb >> 8)  & 0xff) + 5);
                int b = clamp((rgb & 0xff) - 20);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return Effex.vignette(out, 0.3f);
    }

    private BufferedImage softGlow(BufferedImage img)
    {
        BufferedImage bright = Effex.brightnessContrast(img, 20, -30);
        BufferedImage blurred = Effex.blur(bright, 4);
        return blendImages(img, blurred, 0.4f);
    }

    private BufferedImage blendWithWhite(BufferedImage img, float alpha)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp((int)(((rgb >> 16) & 0xff) * (1-alpha) + 255 * alpha));
                int g = clamp((int)(((rgb >> 8)  & 0xff) * (1-alpha) + 255 * alpha));
                int b = clamp((int)((rgb & 0xff) * (1-alpha) + 255 * alpha));
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    private BufferedImage blendImages(BufferedImage a, BufferedImage b, float t)
    {
        int w = a.getWidth(), h = a.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgbA = a.getRGB(x, y);
                int rgbB = b.getRGB(x, y);
                int alpha = (rgbA >> 24) & 0xff;
                int r = clamp((int)(((rgbA >> 16) & 0xff) * (1-t) + ((rgbB >> 16) & 0xff) * t));
                int g = clamp((int)(((rgbA >> 8)  & 0xff) * (1-t) + ((rgbB >> 8)  & 0xff) * t));
                int bv = clamp((int)((rgbA & 0xff) * (1-t) + (rgbB & 0xff) * t));
                out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | bv);
            }
        return out;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
