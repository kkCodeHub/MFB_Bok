package se.swedsoft.bookkeeping.gui.about.panel;


import org.fribok.bookkeeping.app.Version;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.util.BrowserLaunch;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-mar-14
 * Time: 15:15:51
 */
public class SSAboutPanel {    private static final Logger LOG = LoggerFactory.getLogger(SSAboutPanel.class);


    private JPanel iPanel;

    private JButton iCloseButton;

    private JEditorPane iEditorPane;
    private static final Dimension ABOUT_TEXT_SIZE = new Dimension(560, 340);
    /**
     *
     */
    public SSAboutPanel() {
        String iText = SSBundle.getBundle().getString("aboutframe.abouttext");

        iEditorPane.setBackground(iPanel.getBackground());
        iEditorPane.setPreferredSize(ABOUT_TEXT_SIZE);
        iEditorPane.setMinimumSize(ABOUT_TEXT_SIZE);

        iText = iText.replace("{TITLE}", Version.APP_TITLE);
        iText = iText.replace("{VERSION}", Version.APP_VERSION);
        iText = iText.replace("{BUILD}", Version.APP_BUILD);


        iEditorPane.setText(iText);

        iEditorPane.addHyperlinkListener(
                new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                String iEventName = e.getEventType() == null
                        ? ""
                        : e.getEventType().toString();

                if (iEventName.equals("ACTIVATED")) {
                    BrowserLaunch.openURL(e.getURL());
                }
                if (iEventName.equals("ENTERED")) {
                    iEditorPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
                if (iEventName.equals("EXITED")) {
                    iEditorPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }

            }
        });
    }

    /**
     *
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @param iListener
     */
    public void addCloseButtonListener(ActionListener iListener) {
        iCloseButton.addActionListener(iListener);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.about.panel.SSAboutPanel");
        sb.append("{iCloseButton=").append(iCloseButton);
        sb.append(", iEditorPane=").append(iEditorPane);
        sb.append(", iPanel=").append(iPanel);
        sb.append('}');
        return sb.toString();
    }
}
