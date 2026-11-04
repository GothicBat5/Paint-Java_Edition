package systeminteg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Random;

public final class Brushes {

    private Brushes() {}

    // standalone toolbar button, not part of the dropdown
    public static final Brush ERASER = new Eraser();

    // the brush dropdown is built from this list
    public static final List<Brush> ALL = List.of(new Pencil(), new Ink(), new Airbrush());

    private static final class Pencil extends Brush {
        Pencil() { super("Pencil", "✎"); }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
        {
            round(g, x1, y1, x2, y2, c, Math.max(1, size * 0.4));
        }
    }

    private static final class Ink extends Brush {
        Ink() { super("Ink", "✒"); }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) {
            round(g, x1, y1, x2, y2, c, size);
        }
    }

    private static final class Airbrush extends Brush {
        private final Random rnd = new Random();

        Airbrush() 
        { 
        	super("Airbrush", "☁"); 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) {
            double radius = size * 1.5;
            int steps = Math.max(1, (int) (Math.hypot(x2 - x1, y2 - y1) / Math.max(1, radius / 4)));
            int dots = (int) Math.max(4, radius * 2);

            g.setFill(c);
            g.setGlobalAlpha(0.25);
            
            for (int i = 0; i < steps; i++) 
            {
                double t = i / (double) steps;
                double cx = x1 + (x2 - x1) * t, cy = y1 + (y2 - y1) * t;
                
                for (int d = 0; d < dots; d++) 
                {
                    double angle = rnd.nextDouble() * Math.PI * 2;
                    double r = Math.min(radius, Math.abs(rnd.nextGaussian()) * radius / 2.5);
                    g.fillRect(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r, 1.5, 1.5);
                }
            }
            g.setGlobalAlpha(1);
        }
    }

    private static final class Eraser extends Brush 
    {
        Eraser() { super("Eraser", "◻"); }

        @Override
        public boolean isEraser() { return true; }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) {
            round(g, x1, y1, x2, y2, c, size * 2);
        }
    }
}