package se.swedsoft.bookkeeping.gui.util.datechooser.panel;


import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Quarter picker panel.
 *
 * <p>Internally uses {@link LocalDate} for all date arithmetic.
 */
public class SSQuarterChooser extends JPanel implements ItemListener {

    private JComboBox iComboBox;

    private JPanel iPanel;

    // The selected date (stored as LocalDate internally)
    private LocalDate iLocalDate;
    // the change listeners
    private List<ActionListener> iChangeListeners;

    /**
     *
     */
    public SSQuarterChooser() {
        iChangeListeners = new LinkedList<>();

        setLayout(new BorderLayout());

        add(iPanel, BorderLayout.CENTER);

        updateQuarterNames();

        iComboBox.addItemListener(this);
        setLocalDate(LocalDate.now());
    }

    /**
     *
     */
    private void updateQuarterNames() {
        String[] iMonths = new DateFormatSymbols().getMonths();

        iComboBox.removeAllItems();
        iComboBox.addItem("1");
        iComboBox.addItem("2");
        iComboBox.addItem("3");
        iComboBox.addItem("4");
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
        ActionEvent iEvent = new ActionEvent(this, 0, "quarter");

        for (ActionListener iActionListener : iChangeListeners) {
            iActionListener.actionPerformed(iEvent);
        }
    }

    /**
     * Invoked when an item has been selected or deselected by the user.
     */
    public void itemStateChanged(ItemEvent e) {
        notifyChangeListeners();
    }

    /**
     * Returns the start date of the selected quarter as a legacy Date.
     *
     * @return the start date of the selected quarter
     * @deprecated Use {@link #getLocalDate()} instead.
     */
    @Deprecated
    public Date getDate() {
        return SSDateUtil.toDate(getLocalDate());
    }

    /**
     * Returns the start date of the selected quarter.
     *
     * @return the first day of the selected quarter
     */
    public LocalDate getLocalDate() {
        int iIndex = iComboBox.getSelectedIndex();
        int quarterStartMonth = iIndex * 3 + 1; // 1-based month

        return LocalDate.of(iLocalDate.getYear(), quarterStartMonth, 1);
    }

    /**
     * Returns the end date of the selected quarter as a legacy Date.
     *
     * @return the end date of the selected quarter
     * @deprecated Use {@link #getLocalEndDate()} instead.
     */
    @Deprecated
    public Date getEndDate() {
        return SSDateUtil.toDate(getLocalEndDate().atTime(23, 59, 59));
    }

    /**
     * Returns the last day of the selected quarter.
     *
     * @return the last day of the selected quarter
     */
    public LocalDate getLocalEndDate() {
        int iIndex = iComboBox.getSelectedIndex();
        int quarterEndMonth = (iIndex + 1) * 3; // 1-based month

        LocalDate lastDay = LocalDate.of(iLocalDate.getYear(), quarterEndMonth, 1);
        return lastDay.withDayOfMonth(lastDay.lengthOfMonth());
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
     * Set the selected date, updating the combo box to show the correct quarter.
     *
     * @param date the date
     */
    public void setLocalDate(LocalDate date) {
        this.iLocalDate = date;

        int iIndex = (date.getMonthValue() - 1) / 3;

        iComboBox.setSelectedIndex(iIndex);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.datechooser.panel.SSQuarterChooser");
        sb.append("{iChangeListeners=").append(iChangeListeners);
        sb.append(", iComboBox=").append(iComboBox);
        sb.append(", iLocalDate=").append(iLocalDate);
        sb.append(", iPanel=").append(iPanel);
        sb.append('}');
        return sb.toString();
    }
}
