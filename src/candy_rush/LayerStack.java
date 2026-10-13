package candy_rush;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LayerStack
{
    private final List<Layer> layers = new ArrayList<>();
    private int activeIndex = 0;
    Runnable onChange;

    public void setOnChange(Runnable r)
    {
        this.onChange = r;
    }

    public Runnable getOnChange()
    {
        return onChange;
    }

    private void fire()
    {
        if (onChange != null) onChange.run();
    }

    public void addLayer(Layer layer)
    {
        layers.add(layer);
        activeIndex = layers.size() - 1;
        fire();
    }

    public void insertAboveActive(Layer layer)
    {
        layers.add(activeIndex + 1, layer);
        activeIndex = activeIndex + 1;
        fire();
    }

    public void removeLayer(int idx)
    {
        if (layers.size() <= 1) return;
        layers.remove(idx);
        activeIndex = Math.min(activeIndex, layers.size() - 1);
        fire();
    }

    public void moveUp(int idx)
    {
        if (idx >= layers.size() - 1) return;
        Layer tmp = layers.get(idx);
        layers.set(idx, layers.get(idx + 1));
        layers.set(idx + 1, tmp);
        if (activeIndex == idx) activeIndex++;
        else if (activeIndex == idx + 1) activeIndex--;
        fire();
    }

    public void moveDown(int idx)
    {
        if (idx <= 0) return;
        Layer tmp = layers.get(idx);
        layers.set(idx, layers.get(idx - 1));
        layers.set(idx - 1, tmp);
        if (activeIndex == idx) activeIndex--;
        else if (activeIndex == idx - 1) activeIndex++;
        fire();
    }

    public Layer getActive()
    {
        return layers.isEmpty() ? null : layers.get(activeIndex);
    }

    public int   getActiveIndex()
    {
        return activeIndex;
    }

    public void  setActiveIndex(int i)
    {
        activeIndex = i; fire();
    }

    public List<Layer> getLayers()
    {
        return layers;
    }

    public boolean isEmpty()
    {
            return layers.isEmpty();
    }

    public int canvasWidth()
    {
        return layers.stream().mapToInt(l -> l.image.getWidth()).max().orElse(0);
    }

    public int canvasHeight()
    {
        return layers.stream().mapToInt(l -> l.image.getHeight()).max().orElse(0);
    }

    public BufferedImage composite()
    {
        int w = canvasWidth(), h = canvasHeight();
        if (w == 0 || h == 0) return null;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (Layer layer : layers)
        {
            if (!layer.visible) continue;
            blendOnto(out, layer);
        }
        return out;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private void blendOnto(BufferedImage dst, Layer layer)
    {
        int w = Math.min(dst.getWidth(),  layer.image.getWidth());
        int h = Math.min(dst.getHeight(), layer.image.getHeight());
        int alphaInt = (int)(layer.opacity * 255);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int src = layer.image.getRGB(x, y);
                int dst0 = dst.getRGB(x, y);

                int sA = ((src >> 24) & 0xff) * alphaInt / 255;
                int sR = (src >> 16) & 0xff;
                int sG = (src >>  8) & 0xff;
                int sB =  src & 0xff;

                int dA = (dst0 >> 24) & 0xff;
                int dR = (dst0 >> 16) & 0xff;
                int dG = (dst0 >>  8) & 0xff;
                int dB =  dst0 & 0xff;

                int bR = sR, bG = sG, bB = sB;

                switch (layer.blendMode)
                {
                    case MULTIPLY -> { bR = sR*dR/255; bG = sG*dG/255; bB = sB*dB/255; }
                    case SCREEN -> { bR = 255-(255-sR)*(255-dR)/255; bG = 255-(255-sG)*(255-dG)/255; bB = 255-(255-sB)*(255-dB)/255; }
                    case OVERLAY -> { bR = dR<128 ? 2*sR*dR/255 : 255-2*(255-sR)*(255-dR)/255;
                        bG = dG<128 ? 2*sG*dG/255 : 255-2*(255-sG)*(255-dG)/255;
                        bB = dB<128 ? 2*sB*dB/255 : 255-2*(255-sB)*(255-dB)/255; }
                    case SOFT_LIGHT -> { bR = softLight(sR,dR); bG = softLight(sG,dG); bB = softLight(sB,dB); }
                    case DIFFERENCE -> { bR = Math.abs(sR-dR); bG = Math.abs(sG-dG); bB = Math.abs(sB-dB); }
                    default -> {}
                }

                if (sA == 0) continue;

                int outA = sA + dA*(255-sA)/255;
                int outR, outG, outB;
                if (outA == 0) 
                { 
                    outR = outG = outB = 0; 
                }
                else
                {
                    outR = clamp((bR*sA + dR*dA*(255-sA)/255) / outA);
                    outG = clamp((bG*sA + dG*dA*(255-sA)/255) / outA);
                    outB = clamp((bB*sA + dB*dA*(255-sA)/255) / outA);
                }

                dst.setRGB(x, y, (clamp(outA)<<24)|(outR<<16)|(outG<<8)|outB);
            }
    }

    private static int softLight(int s, int d)
    {
        float sf = s/255f, df = d/255f;
        float r;
        if (sf <= 0.5f)
            r = df - (1-2*sf)*df*(1-df);
        else
        {
            float gc = df<=0.25f ? ((16*df-12)*df+4)*df : (float)Math.sqrt(df);
            r = df + (2*sf-1)*(gc-df);
        }
        return clamp(Math.round(r*255));
    }

    public List<Layer> snapshotLayers()
    {
        List<Layer> snap = new ArrayList<>();
        for (Layer l : layers) snap.add(l.deepCopy());
        return snap;
    }

    public void restoreLayers(List<Layer> snap, int activeIdx)
    {
        layers.clear();
        layers.addAll(snap);
        activeIndex = activeIdx;
        fire();
    }
}
