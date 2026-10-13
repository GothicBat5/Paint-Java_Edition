package candy_rush;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ViewportController
{
    private final JScrollPane pane;
    private final JLabel imageLabel;
    private final Consumer<Double> onZoomChanged;

    private double zoom;
    private static final double ZOOM_MIN = 0.05;
    private static final double ZOOM_MAX = 20.0;
    private static final double ZOOM_STEP = 0.12;

    private int panStartX, panStartY;
    private int panOriginH, panOriginV;
    private boolean panning = false;

    private final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor MOVE = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    private final Cursor DEFAULT = Cursor.getDefaultCursor();

    private boolean spaceDown = false;

    public ViewportController(JScrollPane pane, JLabel imageLabel,
                              double initialZoom, Consumer<Double> onZoomChanged)
    {
        this.pane = pane;
        this.imageLabel = imageLabel;
        this.zoom = initialZoom;
        this.onZoomChanged = onZoomChanged;

        pane.setWheelScrollingEnabled(false);

        attachMouseListeners();
        attachKeyListeners();
    }

    public void setZoom(double z)
    {
        this.zoom = z;
    }

    private void attachMouseListeners()
    {
        MouseAdapter ma = new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                Point viewPos = pane.getViewport().getViewPosition();
                Point mousePos = e.getPoint();

                double imgX = (viewPos.x + mousePos.x) / zoom;
                double imgY = (viewPos.y + mousePos.y) / zoom;

                double rotation = e.getPreciseWheelRotation();
                double factor = 1.0 - rotation * ZOOM_STEP;
                double newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * factor));

                onZoomChanged.accept(newZoom);

                SwingUtilities.invokeLater(() -> {
                    int newX = (int)(imgX * newZoom - mousePos.x);
                    int newY = (int)(imgY * newZoom - mousePos.y);
                    Dimension viewSize = pane.getViewport().getViewSize();
                    newX = Math.max(0, Math.min(newX, viewSize.width - pane.getViewport().getWidth()));
                    newY = Math.max(0, Math.min(newY, viewSize.height - pane.getViewport().getHeight()));
                    pane.getViewport().setViewPosition(new Point(newX, newY));
                });
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (isMiddleButton(e) || (spaceDown && SwingUtilities.isLeftMouseButton(e)))
                {
                    panning = true;
                    panStartX = e.getXOnScreen();
                    panStartY = e.getYOnScreen();
                    panOriginH = pane.getHorizontalScrollBar().getValue();
                    panOriginV = pane.getVerticalScrollBar().getValue();
                    pane.setCursor(MOVE);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (panning)
                {
                    panning = false;
                    pane.setCursor(spaceDown ? HAND : DEFAULT);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (!panning) return;
                int dx = e.getXOnScreen() - panStartX;
                int dy = e.getYOnScreen() - panStartY;
                pane.getHorizontalScrollBar().setValue(panOriginH - dx);
                pane.getVerticalScrollBar().setValue(panOriginV - dy);
            }

            private boolean isMiddleButton(MouseEvent e)
            {
                return SwingUtilities.isMiddleMouseButton(e);
            }
        };

        pane.addMouseWheelListener(ma);
        pane.addMouseListener(ma);
        pane.addMouseMotionListener(ma);

        imageLabel.addMouseWheelListener(ma);
        imageLabel.addMouseListener(ma);
        imageLabel.addMouseMotionListener(ma);
    }

    private void attachKeyListeners()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getKeyCode() != KeyEvent.VK_SPACE) return false;
                    if (e.getID() == KeyEvent.KEY_PRESSED && !spaceDown)
                    {
                        spaceDown = true;
                        pane.setCursor(HAND);
                    }
                    else if (e.getID() == KeyEvent.KEY_RELEASED)
                    {
                        spaceDown = false;
                        if (!panning) pane.setCursor(DEFAULT);
                    }
                    return false;
                });
    }
}
