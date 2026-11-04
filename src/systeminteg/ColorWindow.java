package systeminteg;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.function.Consumer;

// Floating, non-modal color window: hue/saturation wheel, brightness, hex and a palette.
public class ColorWindow extends Stage {

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final int SIZE = 220;
    private static final String[] PALETTE = {
        "#000000", "#800000", "#008000", "#000080", "#808000", "#008080", "#800080",
        "#404040", "#808080", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF",
        "#FF00FF", "#FFFFFF", "#FF8000", "#80FF00", "#00FF80", "#0080FF", "#8000FF",
        "#FF0080", "#C0C0C0", "#8B5A2B", "#FFC896", "#96FFC8", "#C896FF", "#FFFFC8"
    };

    private final Stage owner;
    private final Consumer<Color> onFg, onBg;
    private final Canvas wheel = new Canvas(SIZE, SIZE);
    private final Slider brightness = new Slider(0, 100, 100);
    private final TextField hex = new TextField();
    private final Region fgSwatch = target(true);
    private final Region bgSwatch = target(false);
    private final Label editing = new Label();

    private Color fg, bg;
    private boolean editingFg = true;
    private boolean syncing, placed;
    private double hue, sat, bri = 1;

    public ColorWindow(Stage owner, Color fg, Color bg, Consumer<Color> onFg, Consumer<Color> onBg) 
    {
        this.owner = owner;
        this.fg = fg;
        this.bg = bg;
        this.onFg = onFg;
        this.onBg = onBg;

        initOwner(owner);
        initStyle(StageStyle.UTILITY);
        setTitle("Colors");
        setResizable(false);

        Button swap = new Button("⇄");
        swap.getStyleClass().add("round-btn");
        swap.setTooltip(new Tooltip("Swap colors"));
        swap.setOnAction(e -> swap());

        HBox targets = new HBox(12, fgSwatch, bgSwatch, swap, editing);
        targets.setAlignment(Pos.CENTER_LEFT);

        wheel.setOnMousePressed(this::pickWheel);
        wheel.setOnMouseDragged(this::pickWheel);
        HBox wheelRow = new HBox(wheel);
        wheelRow.setAlignment(Pos.CENTER);

        brightness.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(brightness, Priority.ALWAYS);
        brightness.valueProperty().addListener((o, a, n) -> fromWheel(() -> bri = n.doubleValue() / 100));
        HBox bright = new HBox(10, new Label("Brightness"), brightness);
        bright.setAlignment(Pos.CENTER_LEFT);

        hex.setOnAction(e -> commitHex());
        hex.focusedProperty().addListener((o, a, focused) -> { 
        	if (!focused) commitHex(); });
        
        HBox hexRow = new HBox(10, new Label("Hex"), hex);
        hexRow.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Click: selected color   Right click: secondary");
        hint.getStyleClass().add("hint");

        VBox root = new VBox(targets, wheelRow, bright, hexRow, palette(), hint);
        root.getStyleClass().add("color-window");

        Scene scene = new Scene(root);
        if (Main.CSS != null) scene.getStylesheets().add(Main.CSS);
        setScene(scene);

        select(true);
    }

    public void toggle() 
    {
        if (isShowing()) 
        {
            hide();
            return;
        }
        show();
        
        if (!placed)
        {
            placed = true;
            setX(owner.getX() + owner.getWidth() - getWidth() - 24);
            setY(owner.getY() + 90);
        }
    }

    private Region target(boolean isFg) 
    {
        Region r = new Region();
        r.getStyleClass().add("target");
        r.setOnMouseClicked(e -> select(isFg));
        Tooltip.install(r, new Tooltip(isFg ? "Primary" : "Secondary"));
        return r;
    }

    private GridPane palette() 
    {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        
        for (int i = 0; i < PALETTE.length; i++) 
        {
            String code = PALETTE[i];
            Color c = Color.web(code);
            Region sw = new Region();
            sw.getStyleClass().add("swatch");
            sw.setStyle("-fx-background-color: " + code + ";");
            Tooltip.install(sw, new Tooltip(code));
            sw.setOnMousePressed(e -> {
                boolean toBg = e.isSecondaryButtonDown() || !editingFg;
                store(c, toBg);
                if (toBg == !editingFg) sync();
            });
            grid.add(sw, i % 7, i / 7);
        }
        return grid;
    }

    private void select(boolean isFg) 
    {
        editingFg = isFg;
        editing.setText(isFg ? "Editing primary" : "Editing secondary");
        sync();
    }

    private void swap() 
    {
        Color f = fg, b = bg;
        store(b, false);
        store(f, true);
        sync();
    }

    private void pickWheel(MouseEvent e) 
    {
        double r = SIZE / 2.0, dx = e.getX() - r, dy = e.getY() - r;
        fromWheel(() -> {
            hue = (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
            sat = Math.min(1, Math.hypot(dx, dy) / r);
        });
    }

    // wheel and slider edits keep their own hue/sat/brightness so a gray or black doesn't lose the hue
    private void fromWheel(Runnable update) 
    {
        if (syncing) return;
        
        update.run();
        Color c = Color.hsb(hue, sat, bri);
        store(c, !editingFg);
        hex.setText(toHex(c));
        drawWheel();
    }

    private void commitHex() 
    {
        String t = hex.getText() == null ? "" : hex.getText().trim();
        if (!t.startsWith("#")) t = "#" + t;
        
        try {
            store(Color.web(t), !editingFg);
        } 
        
        catch (IllegalArgumentException | NullPointerException e) {
            // fall through to resync, which restores the last valid value
        }
        sync();
    }

    private void store(Color c, boolean toBg) 
    {
        if (c == null) return;
        Color opaque = Color.color(c.getRed(), c.getGreen(), c.getBlue());
        if (toBg) bg = opaque;
        else fg = opaque;

        fgSwatch.setStyle("-fx-background-color: " + toHex(fg) + ";");
        bgSwatch.setStyle("-fx-background-color: " + toHex(bg) + ";");
        
        try {
            (toBg ? onBg : onFg).accept(opaque);
        } 
        
        catch (RuntimeException ex) 
        {
            ex.printStackTrace();
        }
    }

    // load the selected color into the wheel, slider and hex field
    private void sync() {
        Color c = editingFg ? fg : bg;
        syncing = true;
        try {
            hue = c.getHue();
            sat = c.getSaturation();
            bri = c.getBrightness();
            brightness.setValue(bri * 100);
            hex.setText(toHex(c));
        } 
        finally {
            syncing = false;
        }
        fgSwatch.setStyle("-fx-background-color: " + toHex(fg) + ";");
        bgSwatch.setStyle("-fx-background-color: " + toHex(bg) + ";");
        fgSwatch.pseudoClassStateChanged(ACTIVE, editingFg);
        bgSwatch.pseudoClassStateChanged(ACTIVE, !editingFg);
        drawWheel();
    }

    private void drawWheel() 
    {
        WritableImage img = new WritableImage(SIZE, SIZE);
        PixelWriter pw = img.getPixelWriter();
        double r = SIZE / 2.0;
        
        for (int y = 0; y < SIZE; y++) 
        {
            for (int x = 0; x < SIZE; x++) 
            {
                double dx = x + 0.5 - r, dy = y + 0.5 - r;
                double d = Math.hypot(dx, dy);
                if (d > r) continue;
                double h = (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
                pw.setColor(x, y, Color.hsb(h, d / r, bri));
            }
        }

        GraphicsContext g = wheel.getGraphicsContext2D();
        g.clearRect(0, 0, SIZE, SIZE);
        g.drawImage(img, 0, 0);

        double a = Math.toRadians(hue), mx = r + Math.cos(a) * sat * r, my = r + Math.sin(a) * sat * r;
        g.setLineWidth(2);
        g.setStroke(Color.WHITE);
        g.strokeOval(mx - 7, my - 7, 14, 14);
        g.setLineWidth(1);
        g.setStroke(Color.BLACK);
        g.strokeOval(mx - 8, my - 8, 16, 16);
    }

    private static String toHex(Color c) 
    {
        return String.format("#%02X%02X%02X",
            Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}