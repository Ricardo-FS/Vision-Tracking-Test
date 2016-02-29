import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * This class creates a scrollable text area meant for console output.
 * Built for FRC Team 865 Warp 7 2016 Driver Station GUI
 * Created 26 Feb, 2016
 */

public class Console extends JScrollPane {

    private static ArrayList<Console> allConsoles = new ArrayList<>();
    private JTextArea text;
    private String buffer = "";
    private static Calendar cal = Calendar.getInstance();
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public Console(int width, int height) {
        text = new JTextArea();
        text.setLineWrap(true);
        text.setEditable(false);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setPreferredSize(new Dimension(width, height));
        add(text);
        allConsoles.add(this);
    }

    public void addText(String s) {
        buffer += "\n[" + sdf.format(cal.getTime()) + "] " + s;
        text.setText(buffer);
        getVerticalScrollBar().setValue(getVerticalScrollBar().getMaximum());
    }

    public void clearText() {
        buffer = "";
        text.setText(buffer);
        getVerticalScrollBar().setValue(getVerticalScrollBar().getMaximum());
    }

    public static void clearAllConsoles() {
        for(Console console : allConsoles) console.clearText();
    }

    public static void writeToAllConsoles(String s) {
        for(Console console : allConsoles) console.addText(s);
    }
}