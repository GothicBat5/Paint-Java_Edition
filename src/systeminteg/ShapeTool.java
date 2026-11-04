package systeminteg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public enum ShapeTool {
    LINE("Line", "╱"),
    RECT("Rectangle", "▭"),
    FILLED_RECT("Filled Rectangle", "▬"),
    ELLIPSE("Ellipse", "◯"),
    FILLED_ELLIPSE("Filled Ellipse", "⬤");

    private final String label;
    private final String icon;

    ShapeTool(String label, String icon) 
    {
        this.label = label;
        this.icon = icon;
    }

    public String label() 
    { 
    	return label; 
    }

    public String icon() 
    { 
    	return icon; 
    }

    public void draw(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
    {
        g.setStroke(c);
        g.setFill(c);
        g.setLineWidth(size);
        g.setLineCap(StrokeLineCap.ROUND);

        double x = Math.min(x1, x2), y = Math.min(y1, y2);
        double w = Math.abs(x2 - x1), h = Math.abs(y2 - y1);

        switch (this) 
        {
            case LINE -> g.strokeLine(x1, y1, x2, y2);
            case RECT -> g.strokeRect(x, y, w, h);
            case FILLED_RECT -> g.fillRect(x, y, w, h);
            case ELLIPSE -> g.strokeOval(x, y, w, h);
            case FILLED_ELLIPSE -> g.fillOval(x, y, w, h);
        }
    }
}
