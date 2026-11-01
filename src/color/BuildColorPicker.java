import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class BuildColoPicker extends JFrame
{
    private final ColorWheel wheel = new ColorWheel();
    private final JPanel preview = new JPanel();
    private final JLabel hexLabel = new JLabel();
    private final JLabel rgbLabel = new JLabel();
    private final JLabel hsbLabel = new JLabel();
    private final JSlider brightness = new JSlider(0, 100, 100);

    BuildColoPicker()
    {
        setTitle("Color Picker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));

        ((JComponent) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        add(wheel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);

        wheel.onChange = this::refresh;

        brightness.addChangeListener(e -> {
            wheel.setValue(brightness.getValue() / 100.0);
        });

        refresh();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildSidePanel()
    {
        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setPreferredSize(new Dimension(220, 0));

        preview.setPreferredSize(new Dimension(220, 110));
        preview.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        Font mono = new Font(Font.MONOSPACED, Font.BOLD, 15);
        hexLabel.setFont(mono);
        rgbLabel.setFont(mono);
        hsbLabel.setFont(mono);

        JPanel info = new JPanel(new GridLayout(3, 1, 0, 4));
        info.add(hexLabel);
        info.add(rgbLabel);
        info.add(hsbLabel);

        JPanel brightnessPanel = new JPanel(new BorderLayout());
        brightnessPanel.add(new JLabel("BRIGHTNESS"), BorderLayout.NORTH);
        brightnessPanel.add(brightness, BorderLayout.CENTER);

        JButton copy = new JButton("Copy HEX");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(hex(wheel.getRgb())), null);
        });

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.add(brightnessPanel, BorderLayout.NORTH);
        bottom.add(copy, BorderLayout.SOUTH);

        side.add(preview, BorderLayout.NORTH);
        side.add(info, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);

        return side;
    }

    private void refresh()
    {
        int rgb = wheel.getRgb();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        preview.setBackground(new Color(rgb));
        hexLabel.setText("HEX  " + hex(rgb));
        rgbLabel.setText("RGB  " + r + ", " + g + ", " + b);
        hsbLabel.setText(String.format("HSB  %d, %d%%, %d%%",
            Math.round(wheel.hue),
            Math.round(wheel.saturation * 100),
            Math.round(wheel.value * 100)
        ));
    }

    private static String hex(int rgb)
    {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static int hsvToRgb(double h, double s, double v)
    {
        double c = v * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = v - c;

        double r;
        double g;
        double b;

        switch ((int) (h / 60.0) % 6)
        {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }

        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);

        return (ri << 16) | (gi << 8) | bi;
    }

    private static class ColorWheel extends JPanel
    {
        private static final int SIZE = 300;
        private static final int RADIUS = SIZE / 2;

        double hue = 0;
        double saturation = 1;
        double value = 1;
        Runnable onChange = () -> {
        
        };

        private BufferedImage wheelImage;

        ColorWheel()
        {
            setPreferredSize(new Dimension(SIZE + 10, SIZE + 10));
            buildImage();

            MouseAdapter mouse = new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    pick(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e)
                {
                    pick(e.getX(), e.getY());
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        int getRgb()
        {
            return hsvToRgb(hue, saturation, value);
        }

        void setValue(double newValue)
        {
            value = newValue;
            buildImage();
            repaint();
            onChange.run();
        }

        private void buildImage()
        {
            wheelImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < SIZE; y++)
            {
                for (int x = 0; x < SIZE; x++)
                {
                    double dx = x - RADIUS + 0.5;
                    double dy = y - RADIUS + 0.5;
                    double dist = Math.hypot(dx, dy);

                    if (dist > RADIUS)
                    {
                        continue;
                    }

                    double angle = Math.toDegrees(Math.atan2(-dy, dx));
                    double h = (angle + 360) % 360;
                    double s = dist / RADIUS;

                    int alpha = (int) (Math.min(1.0, RADIUS - dist) * 255);
                    int rgb = hsvToRgb(h, s, value);

                    wheelImage.setRGB(x, y, (alpha << 24) | rgb);
                }
            }
        }

        private void pick(int mx, int my)
        {
            double dx = mx - getWidth() / 2.0;
            double dy = my - getHeight() / 2.0;

            hue = (Math.toDegrees(Math.atan2(-dy, dx)) + 360) % 360;
            saturation = Math.min(1.0, Math.hypot(dx, dy) / RADIUS);

            repaint();
            onChange.run();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            g2.drawImage(wheelImage, cx - RADIUS, cy - RADIUS, null);

            double rad = Math.toRadians(hue);
            int mx = (int) Math.round(cx + Math.cos(rad) * saturation * RADIUS);
            int my = (int) Math.round(cy - Math.sin(rad) * saturation * RADIUS);

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Color.BLACK);
            g2.drawOval(mx - 9, my - 9, 18, 18);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Color.WHITE);
            g2.drawOval(mx - 8, my - 8, 16, 16);
            g2.dispose();
        }
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(BuildColoPicker::new);
    }
}
