package candy_rush;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SliderPreview extends JDialog
{
    public static class Param
    {
        final String label;
        final int min, max, initial;

        public Param(String label, int min, int max, int initial)
        {
            this.label = label;
            this.min = min;
            this.max = max;
            this.initial = initial;
        }
    }

    private final List<JSlider> sliders = new ArrayList<>();
    private boolean confirmed = false;

    private static final int PREVIEW_W = 420;
    private static final int PREVIEW_H = 280;

    public SliderPreview(Frame owner, String title,
            BufferedImage source,
            List<Param> params,
            Function<int[], BufferedImage> renderer)
    {
        super(owner, title, true);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
        previewLabel.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        previewLabel.setBackground(new Color(30, 30, 30));
        previewLabel.setOpaque(true);
        root.add(previewLabel, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(new EmptyBorder(0, 0, 6, 0));

        for (Param p : params)
        {
            JSlider sl = new JSlider(p.min, p.max, p.initial);
            sl.setMajorTickSpacing((p.max - p.min) / 4);
            sl.setPaintTicks(true);
            sl.setPaintLabels(true);
            sliders.add(sl);

            JLabel lbl = new JLabel(p.label + "  [" + p.initial + "]");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

            sl.addChangeListener(e -> {
                lbl.setText(p.label + "  [" + sl.getValue() + "]");
                updatePreview(previewLabel, source, renderer);
            });

            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row.add(lbl, BorderLayout.NORTH);
            row.add(sl, BorderLayout.CENTER);
            controls.add(row);
            controls.add(Box.createVerticalStrut(4));
        }

        root.add(controls, BorderLayout.NORTH);

        JButton ok = new JButton("Apply");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.add(cancel);
        btns.add(ok);
        root.add(btns, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);

        updatePreview(previewLabel, source, renderer);
    }

    private void updatePreview(JLabel lbl, BufferedImage src, Function<int[], BufferedImage> renderer)
    {
        int[] vals = values();
        BufferedImage result = renderer.apply(vals);
        Image scaled = result.getScaledInstance(PREVIEW_W, PREVIEW_H, Image.SCALE_FAST);
        lbl.setIcon(new ImageIcon(scaled));
    }

    public boolean isConfirmed()
    {
        return confirmed;
    }

    public int[] values()
    {
        int[] v = new int[sliders.size()];
        for (int i = 0; i < sliders.size(); i++) v[i] = sliders.get(i).getValue();
        return v;
    }
}
