package systeminteg;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;

public class DrawingCanvas extends StackPane {

    private enum Mode { BRUSH, SHAPE, FILL }

    private static final int MAX_HISTORY = 30;

    private final Canvas base = new Canvas();
    // live stroke, merged into base on release so opacity doesn't stack
    private final Canvas stroke = new Canvas();
    private final Deque<WritableImage> undo = new ArrayDeque<>();
    private final Deque<WritableImage> redo = new ArrayDeque<>();

    private Mode mode = Mode.BRUSH;
    private Brush brush = Brushes.ALL.get(0);
    private ShapeTool shape = ShapeTool.values()[0];

    private Color fg = Color.BLACK, bg = Color.WHITE, active = Color.BLACK;
    private double size = 8, flow = 1;
    private double startX, startY, lastX, lastY;
    private double minX, minY, maxX, maxY;
    private boolean drawing;
    private BiConsumer<Integer, Integer> onCursor;

    public DrawingCanvas(int w, int h) 
    {
        getChildren().addAll(base, stroke);
        getStyleClass().add("paper");
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setCursor(Cursor.CROSSHAIR);
        allocate(w, h);

        stroke.setOnMousePressed(this::pressed);
        stroke.setOnMouseDragged(this::dragged);
        stroke.setOnMouseReleased(this::released);
        stroke.setOnMouseMoved(e -> report(e));
    }

    public void useBrush(Brush b) 
    { 
    	brush = b; 
    	mode = Mode.BRUSH; 
    }

    public void useShape(ShapeTool s) 
    { 
    	shape = s; 
    	mode = Mode.SHAPE; 
    }

    public void useFill() 
    { 
    	mode = Mode.FILL; 
    }

    public void setFgColor(Color c) 
    { 
    	fg = c; 
    }

    public void setBgColor(Color c) 
    { 
    	bg = c; 
    }

    public void setBrushSize(double s) 
    { 
    	size = s; 
    }

    public void setBrushOpacity(double o) 
    { 
    	flow = o; 
    }

    public void setOnCursor(BiConsumer<Integer, Integer> l) 
    { 
    	onCursor = l; 
    }

    private void report(MouseEvent e) 
    {
        if (onCursor != null) onCursor.accept((int) e.getX(), (int) e.getY());
    }

    private void pressed(MouseEvent e) 
    {
        if (e.getButton() != MouseButton.PRIMARY && e.getButton() != MouseButton.SECONDARY) return;
        active = e.getButton() == MouseButton.SECONDARY ? bg : fg;

        if (mode == Mode.FILL) 
        {
            pushHistory();
            FloodFill.fill(base, (int) e.getX(), (int) e.getY(), active);
            return;
        }

        drawing = true;
        startX = lastX = e.getX();
        startY = lastY = e.getY();
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = -Double.MAX_VALUE;
        stroke.setOpacity(flow);
        if (mode == Mode.BRUSH) paintSegment(startX, startY);
    }

    private void dragged(MouseEvent e) 
    {
        if (!drawing) return;
        report(e);

        if (mode == Mode.BRUSH) {
            paintSegment(e.getX(), e.getY());
            return;
        }

        clear(dirty());
        touch(startX, startY);
        touch(e.getX(), e.getY());
        shape.draw(stroke.getGraphicsContext2D(), startX, startY, e.getX(), e.getY(), active, size);
    }

    private void released(MouseEvent e) 
    {
        if (!drawing) return;
        drawing = false;
        stroke.setOpacity(1);

        Rectangle2D r = dirty();
        if (r == null) return;

        pushHistory();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        sp.setViewport(r);
        WritableImage layer = stroke.snapshot(sp, null);

        GraphicsContext g = base.getGraphicsContext2D();
        g.setGlobalAlpha(flow);
        g.drawImage(layer, r.getMinX(), r.getMinY());
        g.setGlobalAlpha(1);
        clear(r);
    }

    private void paintSegment(double x, double y) 
    {
        Color c = brush.isEraser() ? Color.WHITE : active;
        brush.stroke(stroke.getGraphicsContext2D(), lastX, lastY, x, y, c, size);
        touch(lastX, lastY);
        touch(x, y);
        lastX = x;
        lastY = y;
    }

    // only the part of the stroke layer that was touched gets snapshotted and cleared
    private void touch(double x, double y) 
    {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }

    private Rectangle2D dirty() 
    {
        if (minX > maxX) return null;
        double pad = size * 2 + 4;
        double x = Math.max(0, Math.floor(minX - pad));
        double y = Math.max(0, Math.floor(minY - pad));
        double r = Math.min(stroke.getWidth(), Math.ceil(maxX + pad));
        double b = Math.min(stroke.getHeight(), Math.ceil(maxY + pad));
        return r > x && b > y ? new Rectangle2D(x, y, r - x, b - y) : null;
    }

    private void clear(Rectangle2D r) 
    {
        if (r != null) 
        {
        	stroke.getGraphicsContext2D().clearRect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
        }
    }

    private WritableImage grab() 
    { 
    	return base.snapshot(null, null); 
    }

    private void pushHistory() 
    {
        undo.addLast(grab());
        if (undo.size() > MAX_HISTORY) undo.removeFirst();
        redo.clear();
    }

    private void resetHistory() 
    {
        undo.clear();
        redo.clear();
    }

    public void undo() 
    { 
    	restore(undo, redo); 
    }

    public void redo() 
    { 
    	restore(redo, undo); 
    }

    private void restore(Deque<WritableImage> from, Deque<WritableImage> to) 
    {
        if (from.isEmpty()) return;
        to.addLast(grab());
        base.getGraphicsContext2D().drawImage(from.removeLast(), 0, 0);
    }

    private void allocate(int w, int h) 
    {
        base.setWidth(w);
        base.setHeight(h);
        stroke.setWidth(w);
        stroke.setHeight(h);
        GraphicsContext g = base.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
    }

    public int pixelWidth() 
    { 
    	return (int) base.getWidth(); 
    }

    public int pixelHeight() 
    { 
    	return (int) base.getHeight(); 
    }

    public void newPage() 
    {
        allocate(pixelWidth(), pixelHeight());
        resetHistory();
    }

    public void resizeCanvas(int w, int h) 
    {
        WritableImage old = grab();
        
        allocate(w, h);
        base.getGraphicsContext2D().drawImage(old, 0, 0);
        resetHistory();
    }

    public void load(Image img) 
    {
        allocate((int) img.getWidth(), (int) img.getHeight());
        base.getGraphicsContext2D().drawImage(img, 0, 0);
        resetHistory();
    }

    public Image getImage() 
    { 
    	return grab(); 
    }
}