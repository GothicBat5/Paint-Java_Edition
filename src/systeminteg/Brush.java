package systeminteg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

// Base class for freehand brushes. Add a new subclass to Brushes.ALL to get it in the dropdown.
public abstract class Brush {

    private final String name;
    private final String icon;

    protected Brush(String name, String icon) 
    {
        this.name = name;
        this.icon = icon;
    }

    public String name() 
    { 
    	return name; 
    }

    public String icon()
    { 
    	return icon; 
    }

    public boolean isEraser() 
    { 
    	return false; 
    }

    // true if holding the pointer still should keep depositing paint (spray)
    public boolean buildsUp() 
    { 
    	return false; 
    }

    // how far from the pointer this brush can draw; keeps the dirty rectangle tight
    public double reach(double size) 
    { 
    	return size * 2; 
    }

    public abstract void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color color, double size);

    protected static void round(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double width) 
    {
        g.setStroke(c);
        g.setFill(c);
        g.setLineWidth(width);
        g.setLineCap(StrokeLineCap.ROUND);
        if (x1 == x2 && y1 == y2) g.fillOval(x1 - width / 2, y1 - width / 2, width, width);
        else g.strokeLine(x1, y1, x2, y2);
    }
}
