package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class LayerPanel extends JPanel
{
    private final LayerStack stack;
    private final JPanel listPanel;
    private final JLabel blendLabel;
    private final JComboBox<Layer.BlendMode> blendCombo;
    private final JSlider opacitySlider;

    public LayerPanel(LayerStack stack)
    {
        this.stack = stack;
        setPreferredSize(new Dimension(170, 0));
        setBackground(new Color(38, 38, 38));
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(60, 60, 60)));

        JLabel title = new JLabel("  Layers");
        title.setForeground(new Color(200, 200, 200));
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setOpaque(true);
        title.setBackground(new Color(45, 45, 45));
        title.setPreferredSize(new Dimension(0, 28));
        add(title, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(38, 38, 38));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(38, 38, 38));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(45, 45, 45));
        bottom.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel opLbl = new JLabel("Opacity");
        opLbl.setForeground(new Color(170, 170, 170));
        opLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        opLbl.setAlignmentX(LEFT_ALIGNMENT);

        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setOpaque(false);
        opacitySlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        opacitySlider.setAlignmentX(LEFT_ALIGNMENT);
        opacitySlider.addChangeListener(e -> {
            Layer active = stack.getActive();
            if (active != null)
            {
                active.opacity = opacitySlider.getValue() / 100f;
                if (stack.getOnChange() != null) stack.getOnChange().run();
                rebuildList();
            }
        });

        blendLabel = new JLabel("Blend");
        blendLabel.setForeground(new Color(170, 170, 170));
        blendLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        blendLabel.setAlignmentX(LEFT_ALIGNMENT);

        blendCombo = new JComboBox<>(Layer.BlendMode.values());
        blendCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        blendCombo.setAlignmentX(LEFT_ALIGNMENT);
        blendCombo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        blendCombo.addActionListener(e -> {
            Layer active = stack.getActive();
            if (active != null)
            {
                active.blendMode = (Layer.BlendMode) blendCombo.getSelectedItem();
                if (stack.getOnChange() != null) stack.getOnChange().run();
            }
        });

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 3, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        btnRow.setAlignmentX(LEFT_ALIGNMENT);

        JButton addBtn = layerBtn("+");
        JButton delBtn = layerBtn("−");
        JButton dupBtn = layerBtn("⧉");

        addBtn.setToolTipText("New layer");
        delBtn.setToolTipText("Delete layer");
        dupBtn.setToolTipText("Duplicate layer");

        addBtn.addActionListener(e -> addBlankLayer());
        delBtn.addActionListener(e -> {
            stack.removeLayer(stack.getActiveIndex());
            rebuildList();
        });
        dupBtn.addActionListener(e -> {
            Layer active = stack.getActive();
            if (active != null)
            {
                stack.insertAboveActive(active.deepCopy());
                rebuildList();
            }
        });

        btnRow.add(addBtn);
        btnRow.add(delBtn);
        btnRow.add(dupBtn);

        bottom.add(opLbl);
        bottom.add(opacitySlider);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(blendLabel);
        bottom.add(blendCombo);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(btnRow);

        add(bottom, BorderLayout.SOUTH);
    }

    public void addBlankLayerPublic()
    {
        addBlankLayer();
    }

    private void addBlankLayer()
    {
        int w = stack.canvasWidth();
        int h = stack.canvasHeight();
        if (w == 0 || h == 0) return;
        BufferedImage blank = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Layer l = new Layer("Layer " + (stack.getLayers().size() + 1), blank);
        stack.insertAboveActive(l);
        rebuildList();
    }

    public void rebuildList()
    {
        listPanel.removeAll();
        List<Layer> layers = stack.getLayers();
        for (int i = layers.size() - 1; i >= 0; i--)
        {
            int idx = i;
            Layer layer = layers.get(i);
            boolean isActive = (i == stack.getActiveIndex());

            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            row.setBackground(isActive ? new Color(60, 90, 130) : new Color(50, 50, 50));
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 30, 30)),
                    new EmptyBorder(4, 6, 4, 4)));

            JLabel thumb = new JLabel(makeThumbnail(layer.image));
            thumb.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);

            JLabel nameLbl = new JLabel(layer.name);
            nameLbl.setForeground(Color.WHITE);
            nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

            JLabel metaLbl = new JLabel(Math.round(layer.opacity * 100) + "%  " + layer.blendMode.name().toLowerCase());
            metaLbl.setForeground(new Color(160, 160, 160));
            metaLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));

            JToggleButton visTgl = new JToggleButton(layer.visible ? "●" : "○");
            visTgl.setSelected(layer.visible);
            visTgl.setFocusable(false);
            visTgl.setMargin(new Insets(0, 0, 0, 0));
            visTgl.setPreferredSize(new Dimension(18, 18));
            visTgl.setFont(new Font("SansSerif", Font.PLAIN, 8));
            visTgl.setForeground(new Color(180, 180, 180));
            visTgl.setOpaque(false);
            visTgl.setBorderPainted(false);
            visTgl.addActionListener(e -> {
                layer.visible = visTgl.isSelected();
                visTgl.setText(layer.visible ? "●" : "○");
                if (stack.getOnChange() != null) stack.getOnChange().run();
            });

            info.add(nameLbl);
            info.add(metaLbl);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            right.setOpaque(false);
            right.add(visTgl);

            row.add(thumb, BorderLayout.WEST);
            row.add(info,  BorderLayout.CENTER);
            row.add(right, BorderLayout.EAST);

            row.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e)
                {
                    stack.setActiveIndex(idx);
                    syncControls();
                    rebuildList();
                }
            });

            listPanel.add(row);
        }

        listPanel.add(Box.createVerticalGlue());
        listPanel.revalidate();
        listPanel.repaint();
        syncControls();
    }

    private void syncControls()
    {
        Layer active = stack.getActive();
        if (active == null) return;
        opacitySlider.setValue(Math.round(active.opacity * 100));
        blendCombo.setSelectedItem(active.blendMode);
    }

    private static ImageIcon makeThumbnail(BufferedImage img)
    {
        int tw = 36, th = 36;
        Image scaled = img.getScaledInstance(tw, th, Image.SCALE_FAST);
        BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thumb.createGraphics();
        g.setColor(new Color(80, 80, 80));
        g.fillRect(0, 0, tw, th);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return new ImageIcon(thumb);
    }

    private static JButton layerBtn(String label)
    {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(new Color(60, 60, 60));
        b.setForeground(Color.WHITE);
        return b;
    }
}
