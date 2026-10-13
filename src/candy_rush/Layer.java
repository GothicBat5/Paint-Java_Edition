package candy_rush;

import java.awt.image.BufferedImage;

public class Layer
{
    public enum BlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, SOFT_LIGHT, DIFFERENCE }

    public String name;
    public BufferedImage image;
    public float opacity;
    public boolean visible;
    public BlendMode blendMode;

    public Layer(String name, BufferedImage image)
    {
        this.name = name;
        this.image = image;
        this.opacity = 1.0f;
        this.visible = true;
        this.blendMode = BlendMode.NORMAL;
    }

    public Layer deepCopy()
    {
        int w = image.getWidth(), h = image.getHeight();
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copy.createGraphics().drawImage(image, 0, 0, null);
        Layer l = new Layer(name, copy);
        l.opacity = opacity;
        l.visible = visible;
        l.blendMode = blendMode;
        return l;
    }
}
