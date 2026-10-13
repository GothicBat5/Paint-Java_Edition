package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;

public class ScreenX extends JFrame
{
    private final LayerStack stack = new LayerStack();

    private static class HistoryEntry
    {
        List<Layer> layers;
        int activeIndex;
        HistoryEntry(List<Layer> l, int i)
        {
            layers = l;
            activeIndex = i;
        }
    }

    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 30;

    private double  zoomFactor = 1.0;
    private boolean cropMode = false;
    private boolean textMode = false;

    private final JLabel statusLabel = new JLabel("No image loaded");
    private JMenuItem undoItem, redoItem;
    private JToggleButton cropBtn;
    private JToggleButton textBtn;
    private JScrollPane scrollPane;
    private CropCanvas cropCanvas;
    private JPanel cropBar;
    private JPanel centerStack;
    private LayerPanel layerPanel;
    private TextFontControl textControl;

    private final JLabel imageLabel = new JLabel(placeholderIcon());

    private ColorGradeWindow colorGradeWindow    = null;
    private HistogramWindow histogramWindow     = null;
    private ViewportController viewportController  = null;

    public ScreenX()
    {
        setTitle("ScreenX");
        setSize(1100, 680);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scrollPane = new JScrollPane(imageLabel);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));

        viewportController = new ViewportController(scrollPane, imageLabel, zoomFactor, newZoom -> {
            zoomFactor = newZoom;

            if (textMode) textControl.syncZoom(zoomFactor);
            onStackChanged();
        });

        cropCanvas = new CropCanvas(this::commitCrop);
        textControl = new TextFontControl(this::commitText);
        textControl.setOnCancel(this::exitTextMode);

        centerStack = new JPanel(new CardLayout());
        centerStack.add(scrollPane, "view");
        centerStack.add(cropCanvas, "crop");
        centerStack.add(textControl.getCanvas(), "text");

        layerPanel = new LayerPanel(stack);

        cropBar = buildCropBar();
        cropBar.setVisible(false);

        add(centerStack, BorderLayout.CENTER);
        add(layerPanel,  BorderLayout.EAST);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(180, 180, 180)));
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        stack.setOnChange(this::onStackChanged);

        createMenu();
        createToolBar();

        setVisible(true);
    }

    private void onStackChanged()
    {
        BufferedImage composite = stack.composite();
        if (composite == null) return;
        int sw = (int)(composite.getWidth() * zoomFactor);
        int sh = (int)(composite.getHeight() * zoomFactor);
        imageLabel.setIcon(new ImageIcon(composite.getScaledInstance(sw, sh, Image.SCALE_FAST)));
        layerPanel.rebuildList();

        if (histogramWindow != null && histogramWindow.isVisible())
            histogramWindow.update(composite);
        status(composite.getWidth() + " × " + composite.getHeight() + " px  |  Zoom: "
                + String.format("%.0f%%", zoomFactor * 100)
                + "  |  Layers: " + stack.getLayers().size()
                + "  |  Undo: " + undoStack.size());
    }

    private JPanel buildCropBar()
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(45, 45, 45));
        p.setBorder(new EmptyBorder(10, 8, 10, 8));
        p.setPreferredSize(new Dimension(110, 0));

        JLabel title = new JLabel("Crop");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton applyBtn = sideBtn("Apply Crop");
        JButton cancelBtn = sideBtn("Cancel");

        applyBtn.addActionListener(e -> cropCanvas.applyCrop());
        cancelBtn.addActionListener(e -> exitCropMode());

        JLabel hint = new JLabel("<html><center>Drag to<br>select area</center></html>");
        hint.setForeground(new Color(140, 140, 140));
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(12));
        p.add(applyBtn);
        p.add(Box.createVerticalStrut(6));
        p.add(cancelBtn);
        p.add(Box.createVerticalGlue());
        p.add(hint);
        return p;
    }

    private JButton sideBtn(String label)
    {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(95, 28));
        b.setFocusPainted(false);
        return b;
    }

    private void createMenu()
    {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        addItem(fileMenu, "Open…",  KeyEvent.VK_O, e -> openImage());
        addItem(fileMenu, "Save Composite…", KeyEvent.VK_S, e -> saveImage());
        fileMenu.addSeparator();
        addItem(fileMenu, "Import as New Layer…", -1, e -> importAsLayer());

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        undoItem = addItem(editMenu, "Undo", KeyEvent.VK_Z, e -> undo());
        redoItem = addItem(editMenu, "Redo", KeyEvent.VK_Y, e -> redo());
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);

        JMenu viewMenu = new JMenu("View");
        addItem(viewMenu, "Zoom In", KeyEvent.VK_EQUALS, e -> zoom(1.25));
        addItem(viewMenu, "Zoom Out", KeyEvent.VK_MINUS,  e -> zoom(0.8));
        addItem(viewMenu, "Fit to Window", -1, e -> fitToWindow());
        addItem(viewMenu, "Reset Zoom", KeyEvent.VK_0,  e -> { zoomFactor = 1.0; onStackChanged(); });
        viewMenu.addSeparator();
        addItem(viewMenu, "Histogram", -1, e -> toggleHistogram());

        JMenu layerMenu = new JMenu("Layer");
        addItem(layerMenu, "New Blank Layer", -1, e -> layerPanel.addBlankLayerPublic());
        addItem(layerMenu, "Duplicate Layer", -1, e -> duplicateActiveLayer());
        addItem(layerMenu, "Delete Layer", -1, e -> deleteActiveLayer());
        layerMenu.addSeparator();
        addItem(layerMenu, "Move Layer Up", -1, e -> { stack.moveUp(stack.getActiveIndex()); layerPanel.rebuildList(); });
        addItem(layerMenu, "Move Layer Down", -1, e -> { stack.moveDown(stack.getActiveIndex()); layerPanel.rebuildList(); });
        layerMenu.addSeparator();
        addItem(layerMenu, "Flatten to Single Layer", -1, e -> flattenLayers());

        JMenu fx = new JMenu("Effects");
        fx.setMnemonic('X');

        JMenu colorMenu = new JMenu("Color");
        addEffectItem(colorMenu, "Grayscale", () -> pushEffect(Effex.toGrayscale(activeImage())));
        addEffectItem(colorMenu, "Invert", () -> pushEffect(Effex.invert(activeImage())));
        addEffectItem(colorMenu, "Sepia", () -> pushEffect(Effex.sepia(activeImage())));
        addEffectItem(colorMenu, "Warm Tone", () -> pushEffect(Effex.warmth(activeImage())));
        addEffectItem(colorMenu, "Cool Tone", () -> pushEffect(Effex.cool(activeImage())));
        addEffectItem(colorMenu, "Posterize…", this::doPosterize);
        addEffectItem(colorMenu, "Solarize…", this::doSolarize);
        fx.add(colorMenu);

        JMenu lightMenu = new JMenu("Light & Tone");
        addEffectItem(lightMenu, "Brightness / Contrast…", this::doBrightnessContrast);
        addEffectItem(lightMenu, "Vignette…", this::doVignette);
        lightMenu.addSeparator();
        addEffectItem(lightMenu, "Color Grading (Curves)…", this::openColorGradeWindow);
        fx.add(lightMenu);

        JMenu blurMenu = new JMenu("Blur & Sharpen");
        addEffectItem(blurMenu, "Blur…", this::doBlur);
        addEffectItem(blurMenu, "Sharpen", () -> pushEffect(Effex.sharpen(activeImage())));
        fx.add(blurMenu);

        JMenu edgeMenu = new JMenu("Edge & Texture");
        addEffectItem(edgeMenu, "Edge Detect", () -> pushEffect(Effex.edgeDetect(activeImage())));
        addEffectItem(edgeMenu, "Emboss", () -> pushEffect(Effex.emboss(activeImage())));
        addEffectItem(edgeMenu, "Pixelate…", this::doPixelate);
        fx.add(edgeMenu);

        JMenu geoMenu = new JMenu("Geometry");
        addEffectItem(geoMenu, "Crop", this::enterCropMode);
        addEffectItem(geoMenu, "Flip Horizontal", () -> pushEffect(Effex.flipHorizontal(activeImage())));
        addEffectItem(geoMenu, "Flip Vertical", () -> pushEffect(Effex.flipVertical(activeImage())));
        addEffectItem(geoMenu, "Rotate 90° CW", () -> pushEffect(Effex.rotate90(activeImage())));
        addEffectItem(geoMenu, "Rotate 90° CCW", () -> pushEffect(Effex.rotate90CCW(activeImage())));
        fx.add(geoMenu);

        JMenu textMenu = new JMenu("Text");
        addEffectItem(textMenu, "Add Text…", this::enterTextMode);
        fx.add(textMenu);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(viewMenu);
        bar.add(layerMenu);
        bar.add(fx);
        setJMenuBar(bar);
    }

    private void createToolBar()
    {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        tb.add(toolButton("Open", e -> openImage()));
        tb.add(toolButton("Save", e -> saveImage()));
        tb.addSeparator();
        tb.add(toolButton("Undo", e -> undo()));
        tb.add(toolButton("Redo", e -> redo()));
        tb.addSeparator();
        tb.add(toolButton("Zoom +", e -> zoom(1.25)));
        tb.add(toolButton("Zoom −", e -> zoom(0.8)));
        tb.add(toolButton("Fit", e -> fitToWindow()));
        tb.add(toolButton("Hist", e -> toggleHistogram()));
        tb.addSeparator();

        cropBtn = new JToggleButton("Crop");
        cropBtn.setFocusable(false);
        cropBtn.addActionListener(e -> {
            if (cropBtn.isSelected()) enterCropMode();
            else exitCropMode();
        });
        tb.add(cropBtn);

        textBtn = new JToggleButton("Text");
        textBtn.setFocusable(false);
        textBtn.addActionListener(e -> {
            if (textBtn.isSelected()) enterTextMode();
            else exitTextMode();
        });
        tb.add(textBtn);
        tb.addSeparator();

        tb.add(toolButton("Grayscale", e -> applyIfLoaded(() -> pushEffect(Effex.toGrayscale(activeImage())))));
        tb.add(toolButton("Invert", e -> applyIfLoaded(() -> pushEffect(Effex.invert(activeImage())))));
        tb.add(toolButton("Sepia", e -> applyIfLoaded(() -> pushEffect(Effex.sepia(activeImage())))));
        tb.add(toolButton("B/C…",  e -> applyIfLoaded(this::doBrightnessContrast)));
        tb.add(toolButton("Blur…", e -> applyIfLoaded(this::doBlur)));
        tb.add(toolButton("Sharpen", e -> applyIfLoaded(() -> pushEffect(Effex.sharpen(activeImage())))));
        tb.add(toolButton("Edge", e -> applyIfLoaded(() -> pushEffect(Effex.edgeDetect(activeImage())))));
        tb.add(toolButton("Vignette…", e -> applyIfLoaded(this::doVignette)));

        add(tb, BorderLayout.NORTH);
    }

    private BufferedImage activeImage()
    {
        Layer l = stack.getActive();
        return l == null ? null : l.image;
    }

    private void pushEffect(BufferedImage result)
    {
        if (result == null) return;
        saveUndoSnapshot();
        stack.getActive().image = result;
        stack.setOnChange(stack.getOnChange());
        onStackChanged();
    }

    private void saveUndoSnapshot()
    {
        undoStack.push(new HistoryEntry(stack.snapshotLayers(), stack.getActiveIndex()));
        if (undoStack.size() > MAX_HISTORY) undoStack.pollLast();
        redoStack.clear();
        updateUndoRedo();
    }

    private void undo()
    {
        if (undoStack.isEmpty()) return;
        redoStack.push(new HistoryEntry(stack.snapshotLayers(), stack.getActiveIndex()));
        HistoryEntry e = undoStack.pop();
        stack.restoreLayers(e.layers, e.activeIndex);
        updateUndoRedo();
        status("Undo");
    }

    private void redo()
    {
        if (redoStack.isEmpty()) return;
        undoStack.push(new HistoryEntry(stack.snapshotLayers(), stack.getActiveIndex()));
        HistoryEntry e = redoStack.pop();
        stack.restoreLayers(e.layers, e.activeIndex);
        updateUndoRedo();
        status("Redo");
    }

    private void updateUndoRedo()
    {
        undoItem.setEnabled(!undoStack.isEmpty());
        redoItem.setEnabled(!redoStack.isEmpty());
    }

    private static JFileChooser imageChooser()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files (png, jpg, gif, bmp, tiff)",
                "png", "jpg", "jpeg", "gif", "bmp", "tiff", "tif"));
        chooser.setAcceptAllFileFilterUsed(false);
        return chooser;
    }

    private void openImage()
    {
        JFileChooser chooser = imageChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            try {
                BufferedImage loaded = ImageIO.read(chooser.getSelectedFile());
                if (loaded == null) throw new Exception("Unsupported format");
                undoStack.clear(); redoStack.clear();
                exitCropMode();
                stack.getLayers().clear();
                BufferedImage argb = toARGB(loaded);
                stack.addLayer(new Layer(chooser.getSelectedFile().getName(), argb));
                zoomFactor = 1.0;
                layerPanel.rebuildList();
                onStackChanged();
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(this, "Could not open:\n" + ex.getMessage(),
                        "Open Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importAsLayer()
    {
        JFileChooser chooser = imageChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            try {
                BufferedImage loaded = ImageIO.read(chooser.getSelectedFile());
                if (loaded == null) throw new Exception("Unsupported format");
                saveUndoSnapshot();
                stack.insertAboveActive(new Layer(chooser.getSelectedFile().getName(), toARGB(loaded)));
                layerPanel.rebuildList();
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(this, "Could not import:\n" + ex.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveImage()
    {
        if (stack.isEmpty())
        {
            warnNoImage();
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files (png, jpg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            SavingThis.save(stack.composite(), chooser.getSelectedFile());
            status("Saved → " + chooser.getSelectedFile().getName());
        }
    }

    private void duplicateActiveLayer()
    {
        Layer active = stack.getActive();
        if (active == null)
        {
            warnNoImage();
            return;
        }
        saveUndoSnapshot();
        stack.insertAboveActive(active.deepCopy());
        layerPanel.rebuildList();
    }

    private void deleteActiveLayer()
    {
        if (stack.getLayers().size() <= 1) return;
        saveUndoSnapshot();
        stack.removeLayer(stack.getActiveIndex());
        layerPanel.rebuildList();
    }

    private void flattenLayers()
    {
        if (stack.isEmpty()) return;
        saveUndoSnapshot();
        BufferedImage flat = stack.composite();
        stack.getLayers().clear();
        stack.addLayer(new Layer("Background", flat));
        layerPanel.rebuildList();
    }

    private void zoom(double factor)
    {
        zoomFactor *= factor;
        zoomFactor = Math.max(0.05, Math.min(zoomFactor, 20.0));
        if (viewportController != null) viewportController.setZoom(zoomFactor);
        onStackChanged();
    }

    private void fitToWindow()
    {
        if (stack.isEmpty()) return;
        Dimension pane = scrollPane.getViewport().getSize();
        zoomFactor = Math.min(pane.getWidth() / (double) stack.canvasWidth(),
                pane.getHeight() / (double) stack.canvasHeight());
        if (viewportController != null) viewportController.setZoom(zoomFactor);
        onStackChanged();
    }

    private void toggleHistogram()
    {
        if (histogramWindow == null) histogramWindow = new HistogramWindow();
        if (histogramWindow.isVisible())
        {
            histogramWindow.setVisible(false);
        }
        else
        {
            histogramWindow.setVisible(true);
            BufferedImage c = stack.composite();
            if (c != null) histogramWindow.update(c);
        }
    }

    private void enterCropMode()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            if (cropBtn != null) cropBtn.setSelected(false);
            return;
        }

        cropMode = true;
        cropCanvas.setImage(stack.composite(), zoomFactor);
        ((CardLayout) centerStack.getLayout()).show(centerStack, "crop");
        remove(layerPanel);
        add(cropBar, BorderLayout.EAST);
        revalidate();
        if (cropBtn != null) cropBtn.setSelected(true);
        status("Crop — drag to select, then Apply Crop");
    }

    private void exitCropMode()
    {
        cropMode = false;
        cropCanvas.cancelSel();
        ((CardLayout) centerStack.getLayout()).show(centerStack, "view");
        remove(cropBar);
        add(layerPanel, BorderLayout.EAST);
        revalidate();
        if (cropBtn != null) cropBtn.setSelected(false);
        onStackChanged();
    }

    private void enterTextMode()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            if (textBtn != null) textBtn.setSelected(false);
            return;
        }

        if (cropMode) exitCropMode();
        textMode = true;
        textControl.activate(stack.composite(), zoomFactor);
        ((CardLayout) centerStack.getLayout()).show(centerStack, "text");
        remove(layerPanel);
        add(textControl, BorderLayout.EAST);
        revalidate();
        if (textBtn != null) textBtn.setSelected(true);
        status("Text — move cursor over image to preview, click to place");
    }

    private void exitTextMode()
    {
        textMode = false;
        textControl.deactivate();
        ((CardLayout) centerStack.getLayout()).show(centerStack, "view");
        remove(textControl);
        add(layerPanel, BorderLayout.EAST);
        revalidate();
        if (textBtn != null) textBtn.setSelected(false);
        onStackChanged();
    }

    private void commitText(BufferedImage result, String text)
    {
        saveUndoSnapshot();
        stack.getActive().image = result;
        onStackChanged();
        status("Text placed: \"" + text + "\"  — click again to place more, or Cancel");
        textControl.activate(stack.composite(), zoomFactor);
    }

    private void commitCrop(java.awt.Rectangle r)
    {
        saveUndoSnapshot();
        for (Layer layer : stack.getLayers())
        {
            int lw = layer.image.getWidth(), lh = layer.image.getHeight();
            int cx = Math.min(r.x, lw), cy = Math.min(r.y, lh);
            int cw = Math.min(r.width,  lw - cx);
            int ch = Math.min(r.height, lh - cy);
            if (cw <= 0 || ch <= 0) continue;
            BufferedImage sub  = layer.image.getSubimage(cx, cy, cw, ch);
            BufferedImage copy = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
            copy.createGraphics().drawImage(sub, 0, 0, null);
            layer.image = copy;
        }
        exitCropMode();
        status("Cropped to " + r.width + " × " + r.height);
    }

    private void doBrightnessContrast()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        SliderPreview dlg = new SliderPreview(this, "Brightness / Contrast", activeImage(),
                List.of(new SliderPreview.Param("Brightness", -255, 255, 0),
                        new SliderPreview.Param("Contrast",   -255, 255, 0)),
                v -> Effex.brightnessContrast(activeImage(), v[0], v[1]));
        dlg.setVisible(true);
        if (dlg.isConfirmed()) { int[] v = dlg.values(); pushEffect(Effex.brightnessContrast(activeImage(), v[0], v[1])); }
    }

    private void doBlur()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        SliderPreview dlg = new SliderPreview(this, "Blur", activeImage(),
                List.of(new SliderPreview.Param("Radius", 1, 15, 2)),
                v -> Effex.blur(activeImage(), v[0]));
        dlg.setVisible(true);
        if (dlg.isConfirmed()) pushEffect(Effex.blur(activeImage(), dlg.values()[0]));
    }

    private void doPixelate()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        SliderPreview dlg = new SliderPreview(this, "Pixelate", activeImage(),
                List.of(new SliderPreview.Param("Block Size", 2, 80, 10)),
                v -> Effex.pixelate(activeImage(), v[0]));
        dlg.setVisible(true);
        if (dlg.isConfirmed()) pushEffect(Effex.pixelate(activeImage(), dlg.values()[0]));
    }

    private void doPosterize()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }
        SliderPreview dlg = new SliderPreview(this, "Posterize", activeImage(),
                List.of(new SliderPreview.Param("Levels", 2, 16, 4)),
                v -> Effex.posterize(activeImage(), v[0]));
        dlg.setVisible(true);

        if (dlg.isConfirmed()) pushEffect(Effex.posterize(activeImage(), dlg.values()[0]));
    }

    private void doSolarize()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        SliderPreview dlg = new SliderPreview(this, "Solarize", activeImage(),
                List.of(new SliderPreview.Param("Threshold", 0, 255, 128)),
                v -> Effex.solarize(activeImage(), v[0]));
        dlg.setVisible(true);
        if (dlg.isConfirmed()) pushEffect(Effex.solarize(activeImage(), dlg.values()[0]));
    }

    private void doVignette()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        SliderPreview dlg = new SliderPreview(this, "Vignette", activeImage(),
                List.of(new SliderPreview.Param("Strength", 0, 100, 85)),
                v -> Effex.vignette(activeImage(), v[0] / 100.0f));
        dlg.setVisible(true);
        if (dlg.isConfirmed()) pushEffect(Effex.vignette(activeImage(), dlg.values()[0] / 100.0f));
    }

    private void openColorGradeWindow()
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }

        if (colorGradeWindow != null && colorGradeWindow.isVisible())
        {
            colorGradeWindow.toFront();
            return;
        }

        final BufferedImage snapshot = activeImage();
        colorGradeWindow = new ColorGradeWindow(snapshot,
                preview -> {
                    if (preview == null) return;
                    int sw = (int)(preview.getWidth() * zoomFactor);
                    int sh = (int)(preview.getHeight() * zoomFactor);
                    imageLabel.setIcon(new ImageIcon(preview.getScaledInstance(sw, sh, Image.SCALE_FAST)));
                    status("Color Grade preview — Apply or Cancel");
                },
                () -> {
                    BufferedImage result = colorGradeWindow.getLastPreview();
                    if (result != null) pushEffect(result);
                    else onStackChanged();
                });
    }

    private void applyIfLoaded(Runnable r)
    {
        if (activeImage() == null)
        {
            warnNoImage();
            return;
        }
        r.run();
    }

    private void addEffectItem(JMenu menu, String label, Runnable action)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> applyIfLoaded(action));
        menu.add(item);
    }

    private JMenuItem addItem(JMenu menu, String label, int key, ActionListener al)
    {
        JMenuItem item = new JMenuItem(label);
        if (key != -1)
            item.setAccelerator(KeyStroke.getKeyStroke(key,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        item.addActionListener(al);
        menu.add(item);
        return item;
    }

    private JButton toolButton(String label, ActionListener al)
    {
        JButton btn = new JButton(label);
        btn.setFocusable(false);
        btn.addActionListener(al);
        return btn;
    }

    private void attachScrollZoom()
    {
        scrollPane.addMouseWheelListener(e -> {
            if (stack.isEmpty()) return;
            double factor = e.getWheelRotation() < 0 ? 1.15 : 1.0 / 1.15;

            JViewport vp = scrollPane.getViewport();
            Point mouse = e.getPoint();
            Point vpPos = vp.getViewPosition();

            double oldZoom = zoomFactor;
            zoomFactor *= factor;
            zoomFactor = Math.max(0.05, Math.min(zoomFactor, 20.0));

            double ratio = zoomFactor / oldZoom;
            int newX = (int)((vpPos.x + mouse.x) * ratio - mouse.x);
            int newY = (int)((vpPos.y + mouse.y) * ratio - mouse.y);

            onStackChanged();

            SwingUtilities.invokeLater(() -> {
                Dimension viewSize = vp.getView().getPreferredSize();
                int cx = Math.max(0, Math.min(newX, viewSize.width - vp.getWidth()));
                int cy = Math.max(0, Math.min(newY, viewSize.height - vp.getHeight()));
                vp.setViewPosition(new Point(cx, cy));
            });
        });
    }

    private Point panStart = null;
    private Point panOrigin = null;
    private boolean panning = false;

    private void attachPan()
    {
        MouseAdapter ma = new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e)
            {
                if (isPanTrigger(e))
                {
                    panning = true;
                    panStart = e.getLocationOnScreen();
                    panOrigin = scrollPane.getViewport().getViewPosition();
                    scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override public void mouseDragged(MouseEvent e)
            {
                if (!panning) return;
                Point now  = e.getLocationOnScreen();
                int dx = panStart.x - now.x;
                int dy = panStart.y - now.y;

                JViewport vp = scrollPane.getViewport();
                Dimension viewSize = vp.getView().getPreferredSize();
                int newX = Math.max(0, Math.min(panOrigin.x + dx, viewSize.width  - vp.getWidth()));
                int newY = Math.max(0, Math.min(panOrigin.y + dy, viewSize.height - vp.getHeight()));
                vp.setViewPosition(new Point(newX, newY));
            }

            @Override public void mouseReleased(MouseEvent e)
            {
                if (panning)
                {
                    panning = false;
                    scrollPane.setCursor(Cursor.getDefaultCursor());
                }
            }

            private boolean isPanTrigger(MouseEvent e)
            {
                return SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown());
            }
        };

        scrollPane.getViewport().addMouseListener(ma);
        scrollPane.getViewport().addMouseMotionListener(ma);
        imageLabel.addMouseListener(ma);
        imageLabel.addMouseMotionListener(ma);
    }

    private void warnNoImage()
    {
        JOptionPane.showMessageDialog(this, "No image is open.", "ScreenX", JOptionPane.INFORMATION_MESSAGE);
    }

    private void status(String msg)
    {
        statusLabel.setText(msg);
    }

    private static BufferedImage toARGB(BufferedImage src)
    {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.createGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    private static ImageIcon placeholderIcon()
    {
        BufferedImage ph = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ph.createGraphics();
        g.setColor(new Color(60, 60, 60));
        g.fillRect(0, 0, 400, 300);
        g.setColor(new Color(120, 120, 120));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String msg = "Open an image to get started";
        g.drawString(msg, (400 - fm.stringWidth(msg)) / 2, 158);
        g.dispose();
        return new ImageIcon(ph);
    }
}
