package systeminteg;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.Arrays;

public final class FloodFill {

    private static final int TOLERANCE = 40;

    private FloodFill() {}

    public static void fill(Canvas canvas, int sx, int sy, Color color) 
    {
        int w = (int) canvas.getWidth(), h = (int) canvas.getHeight();
        
        if (sx < 0 || sy < 0 || sx >= w || sy >= h) return;

        int[] px = new int[w * h];
        canvas.snapshot(null, null).getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);

        int target = px[sy * w + sx];
        int replacement = argb(color);
        if (target == replacement) return;

        boolean[] seen = new boolean[w * h];
        int[] stack = new int[256];
        int top = 0;
        stack[top++] = sy * w + sx;

        while (top > 0) 
        {
            int i = stack[--top];
            if (!ok(px, seen, i, target)) continue;

            int y = i / w, row = y * w;
            int l = i - row, r = l;
            while (l > 0 && ok(px, seen, row + l - 1, target)) l--;
            while (r < w - 1 && ok(px, seen, row + r + 1, target)) r++;

            for (int x = l; x <= r; x++) 
            {
                seen[row + x] = true;
                px[row + x] = replacement;
            }

            for (int ny = y - 1; ny <= y + 1; ny += 2) 
            {
                if (ny < 0 || ny >= h) continue;
                boolean run = false;
                
                for (int x = l; x <= r; x++) 
                {
                    int j = ny * w + x;
                    boolean match = ok(px, seen, j, target);
                    
                    if (match && !run) 
                    {
                        if (top == stack.length) stack = Arrays.copyOf(stack, top * 2);
                        stack[top++] = j;
                    }
                    run = match;
                }
            }
        }

        WritableImage out = new WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        canvas.getGraphicsContext2D().drawImage(out, 0, 0);
    }

    private static boolean ok(int[] px, boolean[] seen, int i, int target) 
    {
        if (seen[i]) return false;
        int a = px[i];
        
        // small tolerance so anti-aliased edges don't leave halos
        int d = Math.abs(((a >> 16) & 255) - ((target >> 16) & 255))
              + Math.abs(((a >> 8) & 255) - ((target >> 8) & 255))
              + Math.abs((a & 255) - (target & 255));
        return d <= TOLERANCE;
    }

    private static int argb(Color c) 
    {
        return (255 << 24)
             | ((int) Math.round(c.getRed() * 255) << 16)
             | ((int) Math.round(c.getGreen() * 255) << 8)
             | (int) Math.round(c.getBlue() * 255);
    }
}