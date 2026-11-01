import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class Gelo extends JFrame implements ActionListener
{
    JButton bt, bl, updateButton;
    JLabel jl;
    JTextField tf;

    Gelo()
    {
        this.setTitle("Nexuxpunk");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout(10, 10));

        // Text display
        jl = new JLabel("Nexuxpunk", SwingConstants.CENTER);
        jl.setBackground(Color.DARK_GRAY);
        jl.setForeground(Color.WHITE);
        jl.setOpaque(true);
        jl.setFont(new Font("MV Boli", Font.PLAIN, 50));

        // Text input
        tf = new JTextField(15);

        // Buttons
        bt = new JButton("Background");
        bl = new JButton("Text Color");
        updateButton = new JButton("Update Text");

        bt.addActionListener(this);
        bl.addActionListener(this);
        updateButton.addActionListener(this);

        // Input and update button
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Enter text:"));
        inputPanel.add(tf);
        inputPanel.add(updateButton);

        // Color buttons
        JPanel colorPanel = new JPanel(new FlowLayout());
        colorPanel.add(bt);
        colorPanel.add(bl);

        // Organize the window
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(colorPanel, BorderLayout.SOUTH);

        this.add(jl, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ex)
    {
        if (ex.getSource() == bt)
        {
            Color cl = JColorChooser.showDialog(this, "Pick Background Color", jl.getBackground());

            if (cl != null)
            {
                jl.setBackground(cl);
            }
        }
        else if (ex.getSource() == bl)
        {
            Color cl = JColorChooser.showDialog(this, "Pick Text Color", jl.getForeground());

            if (cl != null)
            {
                jl.setForeground(cl);
            }
        }
        else if (ex.getSource() == updateButton)
        {
            jl.setText(tf.getText());
        }
    }
}
