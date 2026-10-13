package candy_rush;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;

public class Effex
{

    private static int clamp(int v)
    {
        return Math.max(0, Math.min(255, v));
    }

    private static BufferedImage copyARGB(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    public static BufferedImage toGrayscale(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b =  rgb & 0xff;
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                out.setRGB(x, y, (a << 24) | (gray << 16) | (gray << 8) | gray);
            }
        return out;
    }

    public static BufferedImage invert(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)

            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = 255 - ((rgb >> 16) & 0xff);
                int g = 255 - ((rgb >> 8) & 0xff);
                int b = 255 -  (rgb & 0xff);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    public static BufferedImage sepia(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >>  8) & 0xff;
                int b = rgb & 0xff;
                int nr = clamp((int)(r * 0.393 + g * 0.769 + b * 0.189));
                int ng = clamp((int)(r * 0.349 + g * 0.686 + b * 0.168));
                int nb = clamp((int)(r * 0.272 + g * 0.534 + b * 0.131));
                out.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        return out;
    }

    public static BufferedImage warmth(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp(((rgb >> 16) & 0xff) + 30);
                int g = clamp(((rgb >> 8) & 0xff) + 10);
                int b = clamp((rgb & 0xff) - 30);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    public static BufferedImage cool(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = clamp(((rgb >> 16) & 0xff) - 30);
                int g = clamp(((rgb >>  8) & 0xff) + 10);
                int b = clamp((rgb & 0xff) + 30);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    public static BufferedImage brightnessContrast(BufferedImage img, int brightness, int contrast)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        double factor = (259.0 * (contrast + 255)) / (255.0 * (259 - contrast));

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                r = clamp((int)(factor * (r - 128) + 128) + brightness);
                g = clamp((int)(factor * (g - 128) + 128) + brightness);
                b = clamp((int)(factor * (b - 128) + 128) + brightness);

                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }

    public static BufferedImage blur(BufferedImage img, int radius)
    {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        float val = 1.0f / (size * size);
        Arrays.fill(data, val);

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(copyARGB(img), null);
    }


    public static BufferedImage sharpen(BufferedImage img)
    {
        float[] kernel = {
                0f, -1f,  0f,
                -1f,  5f, -1f,
                0f, -1f,  0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(copyARGB(img), null);
    }

    public static BufferedImage edgeDetect(BufferedImage img)
    {
        BufferedImage gray = toGrayscale(img);
        int w = gray.getWidth(), h = gray.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int[][] gx = { {-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1} };
        int[][] gy = { {-1,-2,-1}, { 0, 0, 0}, { 1, 2, 1} };

        for (int y = 1; y < h - 1; y++)
            for (int x = 1; x < w - 1; x++)
            {
                int sx = 0, sy = 0;
                for (int ky = -1; ky <= 1; ky++)
                    for (int kx = -1; kx <= 1; kx++)
                    {
                        int lum = gray.getRGB(x + kx, y + ky) & 0xff;
                        sx += lum * gx[ky + 1][kx + 1];
                        sy += lum * gy[ky + 1][kx + 1];
                    }
                int mag = clamp((int)Math.sqrt(sx * sx + sy * sy));
                int a = (gray.getRGB(x, y) >> 24) & 0xff;
                out.setRGB(x, y, (a << 24) | (mag << 16) | (mag << 8) | mag);
            }
        return out;
    }

    public static BufferedImage emboss(BufferedImage img)
    {
        float[] kernel = {
                -2f, -1f, 0f,
                -1f,  1f, 1f,
                0f,  1f, 2f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage out = op.filter(copyARGB(img), null);

        return brightnessContrast(out, 30, 0);
    }

    public static BufferedImage flipHorizontal(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(w - 1 - x, y, img.getRGB(x, y));
        return out;
    }

    public static BufferedImage flipVertical(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, h - 1 - y, img.getRGB(x, y));
        return out;
    }

    public static BufferedImage rotate90(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(h - 1 - y, x, img.getRGB(x, y));
        return out;
    }

    public static BufferedImage rotate90CCW(BufferedImage img)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(y, w - 1 - x, img.getRGB(x, y));
        return out;
    }

    public static BufferedImage pixelate(BufferedImage img, int blockSize)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = copyARGB(img);

        for (int y = 0; y < h; y += blockSize)
            for (int x = 0; x < w; x += blockSize)
            {
                long sumA = 0, sumR = 0, sumG = 0, sumB = 0;
                int count = 0;
              
                for (int dy = 0; dy < blockSize && y + dy < h; dy++)
                    for (int dx = 0; dx < blockSize && x + dx < w; dx++)
                    {
                        int rgb = img.getRGB(x + dx, y + dy);
                        sumA += (rgb >> 24) & 0xff;
                        sumR += (rgb >> 16) & 0xff;
                        sumG += (rgb >> 8) & 0xff;
                        sumB +=  rgb & 0xff;
                        count++;
                    }
                int avg = ((int)(sumA / count) << 24)
                        | ((int)(sumR / count) << 16)
                        | ((int)(sumG / count) << 8)
                        |  (int)(sumB / count);
                for (int dy = 0; dy < blockSize && y + dy < h; dy++)
                    for (int dx = 0; dx < blockSize && x + dx < w; dx++)
                        out.setRGB(x + dx, y + dy, avg);
            }
        return out;
    }

    public static BufferedImage vignette(BufferedImage img, float strength)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = copyARGB(img);
        double cx = w / 2.0, cy = h / 2.0;
        double maxDist = Math.sqrt(cx * cx + cy * cy);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b =  rgb & 0xff;

                double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                double factor = 1.0 - strength * Math.pow(dist / maxDist, 2);
                factor = Math.max(0, factor);

                out.setRGB(x, y, (a << 24)
                        | (clamp((int)(r * factor)) << 16)
                        | (clamp((int)(g * factor)) << 8)
                        |  clamp((int)(b * factor)));
            }
        return out;
    }

    public static BufferedImage posterize(BufferedImage img, int levels)
    {
        if (levels < 2) levels = 2;
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int step = 255 / (levels - 1);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = Math.round(((rgb >> 16) & 0xff) / (float)step) * step;
                int g = Math.round(((rgb >> 8) & 0xff) / (float)step) * step;
                int b = Math.round( (rgb & 0xff) / (float)step) * step;
                out.setRGB(x, y, (a << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        return out;
    }

    public static BufferedImage solarize(BufferedImage img, int threshold)
    {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int rgb = img.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b =  rgb & 0xff;
              
                if (r > threshold) r = 255 - r;
                if (g > threshold) g = 255 - g;
                if (b > threshold) b = 255 - b;
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        return out;
    }
}
