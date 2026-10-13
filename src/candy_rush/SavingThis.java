package candy_rush;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.image.BufferedImage;
import java.io.File;


public class SavingThis
{
    public static void save(BufferedImage img, File file)
    {
        try {
            String name = file.getName().toLowerCase();
            String format = "png";

            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) format = "jpg";
            else if (name.endsWith(".bmp")) format = "bmp";
            else if (name.endsWith(".gif")) format = "gif";
            if (!name.contains(".")) file = new File(file.getAbsolutePath() + ".png");

            boolean ok = ImageIO.write(img, format, file);

            if (ok) System.out.println("Saved: " + file.getAbsolutePath());
            else
                JOptionPane.showMessageDialog(null,
                        "Could not save in that format.\nTry saving as .png instead.",
                        "Save Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, "Save failed:\n" + e.getMessage(),
             "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
