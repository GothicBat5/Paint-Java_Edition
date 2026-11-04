package systeminteg;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFiles {

    private final Stage stage;
    private File current;

    public ImageFiles(Stage stage) 
    {
        this.stage = stage;
    }

    public Image open() 
    {
        File f = chooser("Open", true).showOpenDialog(stage);
        if (f == null) return null;

        Image img = new Image(f.toURI().toString());
        if (img.isError()) {
            error("Could not open file:\n" + img.getException().getMessage());
            return null;
        }
        current = f;
        updateTitle(f);
        return img;
    }

    public boolean save(Image img)
    {
        return current != null ? write(img, current) : saveAs(img);
    }

    public boolean saveAs(Image img)
    {
        FileChooser fc = chooser("Save As", false);
        File f = fc.showSaveDialog(stage);
        if (f == null) return false;

        if (!hasKnownExtension(f.getName())) 
        {
            ExtensionFilter sel = fc.getSelectedExtensionFilter();
            String ext = sel == null ? ".png" : sel.getExtensions().get(0).substring(1);
            f = new File(f.getPath() + ext);
        }

        if (!write(img, f)) return false;
        current = f;
        updateTitle(f);
        return true;
    }

    public void reset() {
        current = null;
        stage.setTitle("untitled - Paint");
    }

    private boolean write(Image img, File f)
    {
        String fmt = format(f.getName());
        
        try {
            BufferedImage out = SwingFXUtils.fromFXImage(img, null);
            
            if (!fmt.equals("png")) 
            {
                // jpg/bmp writers reject an alpha channel
                BufferedImage rgb = new BufferedImage(out.getWidth(), out.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(out, 0, 0, null);
                g.dispose();
                out = rgb;
            }
            if (!ImageIO.write(out, fmt, f)) throw new IOException("No writer for " + fmt);
            return true;
        } 
        catch (IOException e) 
        {
            error("Could not save file:\n" + e.getMessage());
            return false;
        }
    }

    private boolean hasKnownExtension(String name) 
    {
        name = name.toLowerCase();
        return name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private String format(String name) 
    {
        name = name.toLowerCase();
        if (name.endsWith(".bmp")) return "bmp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        return "png";
    }

    private FileChooser chooser(String title, boolean open) 
    {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        if (open) fc.getExtensionFilters().add(new ExtensionFilter("Images", "*.png", "*.bmp", "*.jpg", "*.jpeg", "*.gif"));
        fc.getExtensionFilters().addAll(
            new ExtensionFilter("PNG Image", "*.png"),
            new ExtensionFilter("BMP Image", "*.bmp"),
            new ExtensionFilter("JPEG Image", "*.jpg", "*.jpeg"));
        return fc;
    }

    private void updateTitle(File f) 
    {
        stage.setTitle(f.getName() + " - Paint");
    }

    private void error(String msg) 
    {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.initOwner(stage);
        a.showAndWait();
    }
}