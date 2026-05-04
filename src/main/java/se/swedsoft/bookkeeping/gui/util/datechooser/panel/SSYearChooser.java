package se.swedsoft.bookkeeping.gui.util.datechooser.panel;


import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Year picker panel using a {@link JSpinner} with a number model.
 *
 * <p>Internally uses {@link LocalDate} for all date arithmetic.
 */
public class SSYearChooser extends JPanel implements ChangeListener, CaretListener, ActionListener {    private static final Logger LOG = LoggerFactory.getLogger(SSYearChooser.class);

    private static final int MIN_YEAR = 1;
    private static final int MAX_YEAR = 9999;

    private JPanel iPanel;

    private JSpinner iSpinner;

    private JTextField iTextField;

    private SpinnerNumberModel iModel;

    // The selected date (stored as LocalDate internally)
    private LocalDate iLocalDate;
    // the change listeners
    private List<ActionListener> iChangeListeners;

    public SSYearChooser() {
        setLayout(new BorderLayout());

        add(iPanel, BorderLayout.CENTER);

        iChangeListeners = new LinkedList<>();

        iTextField = new JTextField();
        iTextField.setBorder(BorderFactory.createEmptyBorder());
        iTextField.setHorizontalAlignment(SwingConstants.RIGHT);
        iTextField.addCaretListener(this);
        iTextField.addActionListener(this);

        iModel = new SpinnerNumberModel();
        iModel.setMinimum(MIN_YEAR);
        iModel.setMaximum(MAX_YEAR);
        iModel.addChangeListener(this);

        iSpinner.setEditor(iTextField);
        iSpinner.setModel(iModel);

        setLocalDate(LocalDate.now());
    }

    /**
     *
     * @return the panel
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     * @return the selected date as a legacy Date
     * @deprecated Use {@link #getLocalDate()} instead.
     */
    @Deprecated
    public Date getDate() {
        return SSDateUtil.toDate(iLocalDate);
    }

    /**
     * @return the selected date
     */
    public LocalDate getLocalDate() {
        return iLocalDate;
    }

    /**
     * @param iDate the date
     * @deprecated Use {@link #setLocalDate(LocalDate)} instead.
     */
    @Deprecated
    public void setDate(Date iDate) {
        setLocalDate(SSDateUtil.toLocalDate(iDate));
    }

    /**
     * Set the selected date, updating the spinner to show the correct year.
     *
     * @param date the date
     */
    public void setLocalDate(LocalDate date) {
        this.iLocalDate = date;

        iSpinner.setValue(date.getYear());
    }

    /**
     * Invoked when the date changes
     *
     * @param iActionListener the listener
     */

    public void addChangeListener(ActionListener iActionListener) {
        iChangeListeners.add(iActionListener);
    }

    /**
     *
     */
    private void notifyChangeListeners() {
        ActionEvent iEvent = new ActionEvent(this, 0, "year");

        for (ActionListener iActionListener : iChangeListeners) {
            iActionListener.actionPerformed(iEvent);
        }
    }

    /**
     * Invoked when the target of the listener has changed its state.
     *
     * @param e a ChangeEvent object
     */
    public void stateChanged(ChangeEvent e) {
        Number iNumber = iModel.getNumber();

        iTextField.setText(iNumber.toString());

        iLocalDate = iLocalDate.withYear(iNumber.intValue());

        notifyChangeListeners();
    }

    /**
     * After any user input, the value of the textfield is proofed. Depending on
     * being an integer, the value is colored green or red.
     *
     * @param e Description of the Parameter
     */
    public void caretUpdate(CaretEvent e) {
        int iValue;

        try {
            iValue = Integer.decode(iTextField.getText());
        } catch (NumberFormatException e1) {
            iTextField.setForeground(Color.RED);

            return;
        }

        if (iValue < MIN_YEAR || iValue > MAX_YEAR) {
            iTextField.setForeground(Color.RED);
        } else {
            iTextField.setForeground(Color.BLACK);
        }

        iTextField.repaint();
    }

    /**
     * After any user input, the value of the textfield is proofed. Depending on
     * being an integer, the value is colored green or red. If the textfield is
     * green, the enter key is accepted and the new value is set.
     *
     * @param e
     *            Description of the Parameter
     */
    public void actionPerformed(ActionEvent e) {
        try {
            int iValue = Integer.decode(iTextField.getText());

            iModel.setValue(iValue);

        } catch (NumberFormatException e1) {
            LOG.error("Unexpected error", e1);
        }
    }

    /**
     * Used to clean up references making sure the garbage collector
     * is able to clean up the object.
     */
    public void dispose() {

        iPanel.removeAll();
        iPanel = null;
        iSpinner.removeAll();
        iSpinner = null;

        ActionListener[] iActionListeners = iTextField.getActionListeners();
        CaretListener[] iCaretListeners = iTextField.getCaretListeners();

        for (ActionListener iActionListener : iActionListeners) {
            iTextField.removeActionListener(iActionListener);
        }

        for (CaretListener iCaretListener : iCaretListeners) {
            iTextField.removeCaretListener(iCaretListener);
        }

        iTextField.removeAll();
        iTextField = null;

        ChangeListener[] iChangeListenerss = iModel.getChangeListeners();

        for (ChangeListener iChangeListener : iChangeListenerss) {
            iModel.removeChangeListener(iChangeListener);
        }
        iModel = null;

        iLocalDate = null;

        iChangeListeners.removeAll(iChangeListeners);
        iChangeListeners = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.datechooser.panel.SSYearChooser");
        sb.append("{iChangeListeners=").append(iChangeListeners);
        sb.append(", iLocalDate=").append(iLocalDate);
        sb.append(", iModel=").append(iModel);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iSpinner=").append(iSpinner);
        sb.append(", iTextField=").append(iTextField);
        sb.append('}');
        return sb.toString();
    }
}
