/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.resultunit.panel;


import se.swedsoft.bookkeeping.data.SSNewResultUnit;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 */
public class SSResultUnitPanel {

    private SSNewResultUnit iResultUnit;

    protected JPanel iPanel;

    private JButton iOkButton;

    private JButton iCancelButton;

    protected JTextField iName;

    protected JFormattedTextField iNumber;

    protected JTextArea iDescription;

    /**
     * Default constructor.
     * @param iEdit
     */
    public SSResultUnitPanel(boolean iEdit) {
        iNumber.setEnabled(!iEdit);

        DecimalFormat iIntegerFormat = (DecimalFormat) NumberFormat.getIntegerInstance();
        iIntegerFormat.setGroupingUsed(false);

        NumberFormatter iNumberFormatter = new NumberFormatter(iIntegerFormat);
        iNumberFormatter.setValueClass(Integer.class);
        iNumberFormatter.setAllowsInvalid(false);
        iNumberFormatter.setCommitsOnValidEdit(true);

        iNumber.setFormatterFactory(new DefaultFormatterFactory(iNumberFormatter));
        iNumber.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

        SwingUtilities.invokeLater(() -> {

                if (iNumber.isEnabled()) {
                    iNumber.requestFocusInWindow();
                } else {
                    iName.requestFocusInWindow();
                }

            });

        iNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iName.requestFocusInWindow());
                }
            }
        });

        iName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iDescription.requestFocusInWindow());
                }
            }
        });

        iDescription.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iOkButton.requestFocusInWindow());
                }
            }
        });

        iOkButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    SwingUtilities.invokeLater(() -> iCancelButton.requestFocusInWindow());
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> {

                            for (ActionListener al : iOkButton.getActionListeners()) {
                                al.actionPerformed(null);
                            }

                        });
                }
            }
        });

        iCancelButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    SwingUtilities.invokeLater(() -> iOkButton.requestFocusInWindow());
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> {

                            for (ActionListener al : iCancelButton.getActionListeners()) {
                                al.actionPerformed(null);
                            }

                        });
                }
            }
        });
    }

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @param pActionListener
     */

    public void addOkAction(ActionListener pActionListener) {
        iOkButton.addActionListener(pActionListener);
    }

    /**
     *
     * @param pActionListener
     */
    public void addCancelAction(ActionListener pActionListener) {
        iCancelButton.addActionListener(pActionListener);
    }

    /**
     *
     * @param iResultUnit
     */
    public void setResultUnit(SSNewResultUnit iResultUnit) {
        this.iResultUnit = iResultUnit;

        iNumber.setValue(parseNumber(iResultUnit.getNumber()));
        iName.setText(iResultUnit.getName());
        iDescription.setText(iResultUnit.getDescription());
    }

    /**
     *
     * @return
     */
    public SSNewResultUnit getResultUnit() {
        iResultUnit.setNumber(iNumber.getText());
        iResultUnit.setName(iName.getText());
        iResultUnit.setDescription(iDescription.getText());
        return iResultUnit;
    }

    private Integer parseNumber(String pNumber) {
        if (pNumber == null || pNumber.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(pNumber.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.resultunit.panel.SSResultUnitPanel");
        sb.append("{iCancelButton=").append(iCancelButton);
        sb.append(", iDescription=").append(iDescription);
        sb.append(", iName=").append(iName);
        sb.append(", iNumber=").append(iNumber);
        sb.append(", iOkButton=").append(iOkButton);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iResultUnit=").append(iResultUnit);
        sb.append('}');
        return sb.toString();
    }
}
