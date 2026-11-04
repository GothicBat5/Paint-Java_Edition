package systeminteg;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class Main extends Application {

    public static final String CSS = findCss();

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private static String findCss() 
    {
        var url = Main.class.getResource("procreate.css");
        
        if (url == null) 
        {
            System.err.println("procreate.css not found next to Main.class - running unstyled");
            return null;
        }
        return url.toExternalForm();
    }

    private Stage stage;
    private DrawingCanvas canvas;
    private ImageFiles files;
    private ColorWindow colors;

    private final List<Node> toolSlots = new ArrayList<>();
    private final Circle fgDot = new Circle(12, Color.BLACK);
    private final Circle bgDot = new Circle(12, Color.WHITE);
    private final Label status = new Label("Ready");
    private final Label coords = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) 
    {
        this.stage = stage;
        canvas = new DrawingCanvas(900, 650);
        files = new ImageFiles(stage);
        canvas.setOnCursor((x, y) -> coords.setText(x + ", " + y));

        colors = new ColorWindow(stage, Color.BLACK, Color.WHITE, this::setFg, this::setBg);

        BorderPane root = new BorderPane();
        root.setTop(topBar());
        root.setLeft(sidePanel());
        root.setCenter(workspace());
        root.setBottom(statusBar());

        Scene scene = new Scene(root, 1100, 760);
        if (CSS != null) scene.getStylesheets().add(CSS);
        shortcuts(scene);

        stage.setTitle("untitled - Paint");
        stage.setScene(scene);
        stage.show();
    }

    private HBox topBar() 
    {
        MenuButton file = new MenuButton("File");
        file.getStyleClass().add("menu-btn");
        file.getItems().addAll(
            item("New            Ctrl+N", this::newFile),
            item("Open…        Ctrl+O", this::openFile),
            new SeparatorMenuItem(),
            item("Save           Ctrl+S", () -> save(false)),
            item("Save As…", () -> save(true)),
            new SeparatorMenuItem(),
            item("Exit", Platform::exit));

        MenuButton image = new MenuButton("Image");
        image.getStyleClass().add("menu-btn");
        image.getItems().add(item("Canvas Size…", this::resizeDialog));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, file, image, spacer, tools(), colorSwatch());
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox tools() 
    {
        SplitMenuButton brushes = dropdown("Brush", Brushes.ALL, Brush::icon, Brush::name, canvas::useBrush);
        SplitMenuButton shapes = dropdown("Shape", List.of(ShapeTool.values()), ShapeTool::icon, ShapeTool::label, canvas::useShape);
        Button eraser = toolButton(Brushes.ERASER.icon(), Brushes.ERASER.name(), () -> canvas.useBrush(Brushes.ERASER));
        Button fill = toolButton("◉", "Fill", canvas::useFill);

        activate(brushes);

        HBox box = new HBox(4, brushes, eraser, new Separator(Orientation.VERTICAL), shapes, new Separator(Orientation.VERTICAL), fill);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // one-off tool, e.g. eraser or fill
    private Button toolButton(String icon, String tip, Runnable action) 
    {
        Button b = new Button(icon);
        b.getStyleClass().add("tool-btn");
        b.setTooltip(new Tooltip(tip));
        b.setOnAction(e -> {
            activate(b);
            action.run();
        });
        toolSlots.add(b);
        return b;
    }

    // family of tools: the arrow opens the list, the main part re-selects the last pick
    private <T> SplitMenuButton dropdown(String tip, List<T> options, Function<T, String> icon, Function<T, String> name, Consumer<T> action) 
    {
        SplitMenuButton menu = new SplitMenuButton();
        menu.getStyleClass().add("tool-menu");

        T first = options.get(0);
        AtomicReference<T> current = new AtomicReference<>(first);
        Tooltip tooltip = new Tooltip(tip + ": " + name.apply(first));
        menu.setTooltip(tooltip);
        menu.setText(icon.apply(first));

        Consumer<T> pick = o -> {
            current.set(o);
            menu.setText(icon.apply(o));
            tooltip.setText(tip + ": " + name.apply(o));
            activate(menu);
            action.accept(o);
        };

        for (T o : options) 
        {
            MenuItem mi = new MenuItem(icon.apply(o) + "  " + name.apply(o));
            mi.setOnAction(e -> pick.accept(o));
            menu.getItems().add(mi);
        }
        menu.setOnAction(e -> pick.accept(current.get()));

        toolSlots.add(menu);
        return menu;
    }

    private void activate(Node slot) 
    {
        for (Node s : toolSlots) s.pseudoClassStateChanged(ACTIVE, s == slot);
    }

    private Node colorSwatch()
    {
        bgDot.setLayoutX(30);
        bgDot.setLayoutY(24);
        fgDot.setLayoutX(16);
        fgDot.setLayoutY(14);
        bgDot.getStyleClass().add("color-dot");
        fgDot.getStyleClass().add("color-dot");

        Pane swatch = new Pane(bgDot, fgDot);
        swatch.setPrefSize(46, 38);
        swatch.setMinSize(46, 38);
        swatch.setStyle("-fx-cursor: hand;");
        swatch.setOnMouseClicked(e -> colors.toggle());
        return swatch;
    }

    private void setFg(Color c) 
    {
        if (c == null) return;
        canvas.setFgColor(c);
        fgDot.setFill(c);
    }

    private void setBg(Color c) 
    {
        if (c == null) return;
        canvas.setBgColor(c);
        bgDot.setFill(c);
    }

    private VBox sidePanel() 
    {
        Slider size = vSlider(1, 60, 8);
        Slider opacity = vSlider(5, 100, 100);
        size.valueProperty().addListener((o, a, n) -> canvas.setBrushSize(n.doubleValue()));
        opacity.valueProperty().addListener((o, a, n) -> canvas.setBrushOpacity(n.doubleValue() / 100));

        Label sizeVal = new Label();
        sizeVal.textProperty().bind(size.valueProperty().asString("%.0f"));
        Label opVal = new Label();
        opVal.textProperty().bind(opacity.valueProperty().asString("%.0f%%"));

        VBox panel = new VBox(10, size, sizeVal, roundButton("↶", "Undo", canvas::undo), roundButton("↷", "Redo", canvas::redo), opacity, opVal);
        panel.getStyleClass().add("side-panel");
        panel.setAlignment(Pos.CENTER);

        VBox wrap = new VBox(panel);
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(0, 0, 0, 14));
        return wrap;
    }

    private Slider vSlider(double min, double max, double value) 
    {
        Slider s = new Slider(min, max, value);
        s.setOrientation(Orientation.VERTICAL);
        s.setPrefHeight(170);
        return s;
    }

    private Button roundButton(String icon, String tip, Runnable action) 
    {
        Button b = new Button(icon);
        b.getStyleClass().add("round-btn");
        b.setTooltip(new Tooltip(tip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private ScrollPane workspace() 
    {
        StackPane holder = new StackPane(canvas);
        holder.setPadding(new Insets(30));

        ScrollPane sp = new ScrollPane(holder);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("workspace");
        return sp;
    }

    private HBox statusBar() 
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(status, spacer, coords);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private MenuItem item(String text, Runnable action) 
    {
        MenuItem m = new MenuItem(text);
        m.setOnAction(e -> action.run());
        return m;
    }

    private void shortcuts(Scene scene) 
    {
        var ctrl = KeyCombination.SHORTCUT_DOWN;
        var shift = KeyCombination.SHIFT_DOWN;
        var acc = scene.getAccelerators();
        acc.put(new KeyCodeCombination(KeyCode.N, ctrl), this::newFile);
        acc.put(new KeyCodeCombination(KeyCode.O, ctrl), this::openFile);
        acc.put(new KeyCodeCombination(KeyCode.S, ctrl), () -> save(false));
        acc.put(new KeyCodeCombination(KeyCode.S, ctrl, shift), () -> save(true));
        acc.put(new KeyCodeCombination(KeyCode.Z, ctrl), canvas::undo);
        acc.put(new KeyCodeCombination(KeyCode.Z, ctrl, shift), canvas::redo);
        acc.put(new KeyCodeCombination(KeyCode.Y, ctrl), canvas::redo);
    }

    private void save(boolean as) 
    {
        Image img = canvas.getImage();
        if (as ? files.saveAs(img) : files.save(img)) status.setText("Saved");
    }

    private void newFile() 
    {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Discard current drawing?", ButtonType.YES, ButtonType.NO);
        a.setTitle("New");
        a.initOwner(stage);
        
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) 
        {
            canvas.newPage();
            files.reset();
            status.setText("Ready");
        }
    }

    private void openFile() 
    {
        Image img = files.open();
        
        if (img != null) {
            canvas.load(img);
            status.setText("Opened");
        }
    }

    private void resizeDialog() 
    {
        TextField w = new TextField(String.valueOf(canvas.pixelWidth()));
        TextField h = new TextField(String.valueOf(canvas.pixelHeight()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Width"), w);
        grid.addRow(1, new Label("Height"), h);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Canvas Size");
        d.initOwner(stage);
        d.getDialogPane().setContent(grid);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
 
        d.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                int nw = Integer.parseInt(w.getText().trim());
                int nh = Integer.parseInt(h.getText().trim());
                if (nw > 0 && nh > 0 && nw <= 8192 && nh <= 8192) canvas.resizeCanvas(nw, nh);
            } 
            catch (NumberFormatException ignored) 
            {
            	
            }
        });
    }
}
