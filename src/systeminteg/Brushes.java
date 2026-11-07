package systeminteg;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class Brushes {

    private Brushes() {}
    // standalone toolbar button, not part of the dropdown
    public static final Brush ERASER = new Eraser();

    // the brush dropdown is built from this list
    public static final List<Brush> ALL = List.of(new Pencil(), new Ink(), new Paintbrush(), new Calligraphy(), new Spray(), new Pixel());

    private static final class Pencil extends Brush {
        Pencil() 
        { 
        	super("Pencil", "✎"); 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
        {
            round(g, x1, y1, x2, y2, c, Math.max(1, size * 0.4));
        }
    }

    private static final class Ink extends Brush {
        Ink() 
        { 
        	super("Ink", "✒"); 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size)
        {
            round(g, x1, y1, x2, y2, c, size);
        }
    }

    // a bundle of bristles at fixed offsets, so moving drags streaks of slightly different tone
    private static final class Paintbrush extends Brush {
        private static final int BRISTLES = 28;

        private final double[] ox = new double[BRISTLES];
        private final double[] oy = new double[BRISTLES];
        private final double[] shade = new double[BRISTLES];
        private final Color[] tones = new Color[BRISTLES];
        private Color source;

        Paintbrush() 
        {
            super("Paintbrush", "≋");
            Random rnd = new Random();
            
            for (int i = 0; i < BRISTLES; i++) 
            {
                double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble());
                ox[i] = Math.cos(a) * r;
                oy[i] = Math.sin(a) * r;
                shade[i] = (rnd.nextDouble() - 0.5) * 0.3;
            }
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
        {
        	
            if (!c.equals(source)) 
            {
                source = c;
                for (int i = 0; i < BRISTLES; i++)
                    tones[i] = shade[i] >= 0 ? c.interpolate(Color.WHITE, shade[i]) : c.interpolate(Color.BLACK, -shade[i]);
            }
            double spread = size * 0.5, width = Math.max(1.5, size * 0.25);
            
            for (int i = 0; i < BRISTLES; i++) 
            {
                double dx = ox[i] * spread, dy = oy[i] * spread;
                round(g, x1 + dx, y1 + dy, x2 + dx, y2 + dy, tones[i], width);
            }
        }
    }

    // flat nib held at a fixed angle: thick across the nib, hairline along it
    private static final class Calligraphy extends Brush {
        private static final double NX = Math.cos(Math.toRadians(45)), NY = -Math.sin(Math.toRadians(45));

        Calligraphy() 
        { 
        	super("Calligraphy", "✑");
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) {
            double half = Math.max(1, size * 0.7);
            double dx = NX * half, dy = NY * half;

            g.setFill(c);
            g.setStroke(c);
            g.setLineWidth(1);
            g.setLineCap(StrokeLineCap.BUTT);
            g.strokeLine(x1 - dx, y1 - dy, x1 + dx, y1 + dy);
            if (x1 == x2 && y1 == y2) return;

            double[] xs = { x1 - dx, x1 + dx, x2 + dx, x2 - dx };
            double[] ys = { y1 - dy, y1 + dy, y2 + dy, y2 - dy };
            g.fillPolygon(xs, ys, 4);
            g.strokePolygon(xs, ys, 4);
        }
    }

    // scatter pattern is precomputed once; a stroke just samples it, which keeps spraying cheap
    private static final class Spray extends Brush {
        private static final int N = 2048, MASK = N - 1;
        private static final float[] PX = new float[N], PY = new float[N];

        static {
            Random rnd = new Random(1);
            
            for (int i = 0; i < N; i++) 
            {
                double d;
                do d = Math.abs(rnd.nextGaussian()) / 2.2; while (d > 1);
                double a = rnd.nextDouble() * Math.PI * 2;
                PX[i] = (float) (Math.cos(a) * d);
                PY[i] = (float) (Math.sin(a) * d);
            }
        }

        Spray() { super("Spray", "∷"); }

        @Override
        public boolean buildsUp() 
        { 
        	return true; 
        }

        @Override
        public double reach(double size) 
        { 
        	return size * 3 + 4; 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size)
        {
            double radius = size * 3;
            int steps = Math.max(1, (int) (Math.hypot(x2 - x1, y2 - y1) / Math.max(2, radius / 4)));
            int dots = (int) Math.min(160, 12 + radius * 1.2);
            int dot = size < 20 ? 1 : size < 40 ? 2 : 3;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            g.setFill(c);
            g.setGlobalAlpha(0.6);
            
            for (int s = 1; s <= steps; s++) 
            {
                double t = s / (double) steps;
                double cx = x1 + (x2 - x1) * t, cy = y1 + (y2 - y1) * t;
                int k = rnd.nextInt(N);
                for (int i = 0; i < dots; i++) {
                    int j = (k + i) & MASK;
                    g.fillRect(Math.floor(cx + PX[j] * radius), Math.floor(cy + PY[j] * radius), dot, dot);
                }
            }
            g.setGlobalAlpha(1);
        }
    }

    // squares snapped to a grid, joined with Bresenham so fast moves leave no gaps
    private static final class Pixel extends Brush {
        Pixel() 
        { 
        	super("Pixel", "▦"); 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
        {
            int cell = Math.max(1, (int) Math.round(size / 2));
            int cx = (int) Math.floor(x1 / cell), cy = (int) Math.floor(y1 / cell);
            int ex = (int) Math.floor(x2 / cell), ey = (int) Math.floor(y2 / cell);
            int dx = Math.abs(ex - cx), dy = -Math.abs(ey - cy);
            int sx = cx < ex ? 1 : -1, sy = cy < ey ? 1 : -1;
            int err = dx + dy;

            g.setFill(c);
            
            while (true) 
            {
                g.fillRect(cx * cell, cy * cell, cell, cell);
                if (cx == ex && cy == ey) break;
                int e2 = 2 * err;
                
                if (e2 >= dy) 
                { 
                	err += dy; 
                	cx += sx; 
                }
                if (e2 <= dx) 
                { 
                	err += dx; 
                	cy += sy; 
                }
            }
        }
    }

    private static final class Eraser extends Brush {
        Eraser() 
        { 
        	super("Eraser", "◻"); 
        }

        @Override
        public boolean isEraser() 
        { 
        	return true; 
        }

        @Override
        public void stroke(GraphicsContext g, double x1, double y1, double x2, double y2, Color c, double size) 
        {
            round(g, x1, y1, x2, y2, c, size * 2);
        }
    }
}
