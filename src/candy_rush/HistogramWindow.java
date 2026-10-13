package candy_rush;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class HistogramWindow extends JFrame
{
    private BufferedImage source;
    private final HistPanel panel;

    public HistogramWindow()
    {
        super("Histogram");
        setSize(340, 260);
        setMinimumSize(new Dimension(280, 200));
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setLocationByPlatform(true);
        setResizable(true);

        panel = new HistPanel();
        add(panel);
    }

    public void update(BufferedImage img)
    {
        this.source = img;
        panel.repaint();
    }

    private class HistPanel extends JPanel
    {
        HistPanel() { setBackground(new Color(22, 22, 22)); }

        @Override
        protected void paintComponent(Graphics g0)
        {
            super.paintComponent(g0);
            if (source == null)
            {
                g0.setColor(new Color(80, 80, 80));
                g0.drawString("No image", 10, 20);
                return;
            }

            int[] histR = new int[256], histG = new int[256], histB = new int[256], histL = new int[256];
            int w = source.getWidth(), h = source.getHeight();
            int step = Math.max(1, (w * h) / 60_000);

            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x += step)
                {
                    int rgb = source.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >>  8) & 0xff;
                    int b =  rgb & 0xff;
                    histR[r]++;
                    histG[g]++;
                    histB[b]++;
                    histL[(int)(0.299*r + 0.587*g + 0.114*b)]++;
                }

            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pw = getWidth(), ph = getHeight();
            int pad = 10;
            int cw = pw - pad*2;
            int ch = (ph - pad*3) / 2;

            drawHist(g, histL, histR, histG, histB, pad, pad, cw, ch, false);
            drawHist(g, null,  histR, histG, histB, pad, pad+ch+pad, cw, ch, true);

            g.setColor(new Color(100, 100, 100));
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.drawString("Luminance", pad + 2, pad + 11);
            g.drawString("R / G / B", pad + 2, pad + ch + pad + 11);
        }

        private void drawHist(Graphics2D g, int[] luma, int[] r, int[] gr, int[] b,
                              int ox, int oy, int w, int h, boolean rgb)
        {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(ox, oy, w, h);
            g.setColor(new Color(55, 55, 55));
            for (int i = 1; i < 4; i++)
                g.drawLine(ox + w*i/4, oy, ox + w*i/4, oy + h);

            int peak = 1;
            if (luma != null) for (int v : luma) peak = Math.max(peak, v);
            if (rgb)
            {
                for (int v : r)  peak = Math.max(peak, v);
                for (int v : gr) peak = Math.max(peak, v);
                for (int v : b)  peak = Math.max(peak, v);
            }

            if (luma != null)
                drawBars(g, luma, ox, oy, w, h, peak, new Color(180, 180, 180, 120));
            if (rgb)
            {
                drawBars(g, r,  ox, oy, w, h, peak, new Color(255, 60,  60,  100));
                drawBars(g, gr, ox, oy, w, h, peak, new Color(60,  210, 60,  100));
                drawBars(g, b,  ox, oy, w, h, peak, new Color(60,  120, 255, 100));
            }

            g.setColor(new Color(70, 70, 70));
            g.drawRect(ox, oy, w, h);
        }

        private void drawBars(Graphics2D g, int[] hist, int ox, int oy, int w, int h, int peak, Color col)
        {
            g.setColor(col);
            int barW = Math.max(1, w / 256);
            for (int i = 0; i < 256; i++)
            {
                int bh = (int)((hist[i] / (double) peak) * h);
                g.fillRect(ox + i * w / 256, oy + h - bh, barW, bh);
            }
        }
    }
}
