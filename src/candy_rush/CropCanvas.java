package candy_rush;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class CropCanvas extends JPanel
{
    private BufferedImage image;
    private double zoom = 1.0;

    private int selX1, selY1, selX2, selY2;
    private boolean hasSel = false;
    private boolean dragging = false;

    private final Consumer<Rectangle> onCrop;

    public CropCanvas(Consumer<Rectangle> onCrop)
    {
        this.onCrop = onCrop;
        setBackground(new Color(40, 40, 40));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        MouseAdapter ma = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                selX1 = selX2 = e.getX();
                selY1 = selY2 = e.getY();
                hasSel = false;
                dragging = true;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                selX2 = e.getX();
                selY2 = e.getY();
                hasSel = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                selX2 = e.getX();
                selY2 = e.getY();
                dragging = false;
                hasSel  = Math.abs(selX2 - selX1) > 4 && Math.abs(selY2 - selY1) > 4;
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setImage(BufferedImage img, double z)
    {
        this.image = img;
        this.zoom = z;
        hasSel = false;
        repaint();
    }

    public void applyCrop()
    {
        if (!hasSel || image == null) return;

        int offX = imageOffsetX();
        int offY = imageOffsetY();

        int ix1 = (int)((Math.min(selX1, selX2) - offX) / zoom);
        int iy1 = (int)((Math.min(selY1, selY2) - offY) / zoom);
        int ix2 = (int)((Math.max(selX1, selX2) - offX) / zoom);
        int iy2 = (int)((Math.max(selY1, selY2) - offY) / zoom);

        ix1 = Math.max(0, Math.min(ix1, image.getWidth()));
        iy1 = Math.max(0, Math.min(iy1, image.getHeight()));
        ix2 = Math.max(0, Math.min(ix2, image.getWidth()));
        iy2 = Math.max(0, Math.min(iy2, image.getHeight()));

        if (ix2 - ix1 < 1 || iy2 - iy1 < 1) return;

        onCrop.accept(new Rectangle(ix1, iy1, ix2 - ix1, iy2 - iy1));
        hasSel = false;
        repaint();
    }

    public void cancelSel()
    {
        hasSel = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0)
    {
        super.paintComponent(g0);
        if (image == null) return;

        Graphics2D g = (Graphics2D) g0;
        int offX = imageOffsetX();
        int offY = imageOffsetY();
        int sw = (int)(image.getWidth() * zoom);
        int sh = (int)(image.getHeight() * zoom);

        g.drawImage(image, offX, offY, sw, sh, null);

        if (hasSel || dragging)
        {
            int rx = Math.min(selX1, selX2);
            int ry = Math.min(selY1, selY2);
            int rw = Math.abs(selX2 - selX1);
            int rh = Math.abs(selY2 - selY1);

            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect(offX, offY, sw, ry - offY);
            g.fillRect(offX, ry + rh, sw, (offY + sh) - (ry + rh));
            g.fillRect(offX, ry, rx - offX, rh);
            g.fillRect(rx + rw, ry, (offX + sw) - (rx + rw), rh);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1, new float[]{6, 3}, 0));
            g.drawRect(rx, ry, rw, rh);
            g.setStroke(new BasicStroke(1));

            int imgW = (int)(rw / zoom);
            int imgH = (int)(rh / zoom);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(Color.WHITE);
            g.drawString(imgW + " × " + imgH, rx + 4, ry - 4 > offY + 4 ? ry - 4 : ry + 14);
        }
    }

    private int imageOffsetX()
    {
        return Math.max(0, (getWidth() - (int)(image.getWidth()  * zoom)) / 2);
    }

    private int imageOffsetY()
    {
        return Math.max(0, (getHeight() - (int)(image.getHeight() * zoom)) / 2);
    }
}
