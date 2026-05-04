package se.swedsoft.bookkeeping.gui.util.datechooser.panel;


import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Composite calendar panel that orchestrates {@link SSDayChooser},
 * {@link SSMonthChooser}, and {@link SSYearChooser}.
 *
 * <p>Internally uses {@link LocalDate} for all date arithmetic.
 */
public class SSCalendar implements ActionListener {

    private JPanel iPanel;

    private JPanel iYearPanel;

    private JPanel iMonthPanel;

    private JPanel iDayPanel;

    private SSDayChooser iDayChooser;

    private SSMonthChooser iMonthChooser;

    private SSYearChooser iYearChooser;
    // The selected date (stored as LocalDate internally)
    private LocalDate iLocalDate;
    // the change listeners
    private List<ActionListener> iChangeListeners;

    /**
     *
     */
    public SSCalendar() {
        iChangeListeners = new LinkedList<>();

        iYearPanel.setLayout(new BorderLayout());
        iMonthPanel.setLayout(new BorderLayout());
        iDayPanel.setLayout(new BorderLayout());

        iDayChooser = new SSDayChooser();
        iMonthChooser = new SSMonthChooser();
        iYearChooser = new SSYearChooser();

        iDayChooser.addChangeListener(this);
        iMonthChooser.addChangeListener(this);
        iYearChooser.addChangeListener(this);

        iYearPanel.add(iYearChooser.getPanel(), BorderLayout.CENTER);
        iMonthPanel.add(iMonthChooser.getPanel(), BorderLayout.CENTER);
        iDayPanel.add(iDayChooser.getPanel(), BorderLayout.CENTER);
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
     * Set the selected date, updating all sub-choosers.
     *
     * @param date the date
     */
    public void setLocalDate(LocalDate date) {
        this.iLocalDate = date;

        iDayChooser.setLocalDate(date);
        iMonthChooser.setLocalDate(date);
        iYearChooser.setLocalDate(date);
    }

    /**
     *
     * @return the year chooser
     */
    public SSYearChooser getYearChooser() {
        return iYearChooser;
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
     * @return the month chooser
     */
    public SSMonthChooser getMonthChooser() {
        return iMonthChooser;
    }

    /**
     *
     * @return the day chooser
     */
    public SSDayChooser getDayChooser() {
        return iDayChooser;
    }

    /**
     *
     * @param iEvent the event
     */
    private void notifyChangeListeners(ActionEvent iEvent) {

        for (ActionListener iActionListener : iChangeListeners) {
            iActionListener.actionPerformed(iEvent);
        }
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        ActionEvent iEvent = new ActionEvent(this, 0, e.getActionCommand());

        if (e.getSource() == iDayChooser) {
            iLocalDate = iDayChooser.getLocalDate();

            notifyChangeListeners(iEvent);
        }
        if (e.getSource() == iYearChooser) {
            iLocalDate = iYearChooser.getLocalDate();
        }
        if (e.getSource() == iMonthChooser) {
            iLocalDate = iMonthChooser.getLocalDate();
        }

        iDayChooser.setLocalDate(iLocalDate);
        iMonthChooser.setLocalDate(iLocalDate);
        iYearChooser.setLocalDate(iLocalDate);

    }

    /**
     * Used to clean up references making sure the garbage collector
     * is able to clean up the object.
     */
    public void dispose() {
        iPanel.removeAll();
        iPanel = null;

        iDayPanel.removeAll();
        iDayPanel = null;

        iYearPanel.removeAll();
        iYearPanel = null;

        iMonthPanel.removeAll();
        iMonthPanel = null;

        iDayChooser.dispose();
        iDayChooser = null;

        iMonthChooser.dispose();
        iMonthChooser = null;

        iYearChooser.dispose();
        iYearChooser = null;

        iLocalDate = null;

        iChangeListeners.removeAll(iChangeListeners);
        iChangeListeners = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.datechooser.panel.SSCalendar");
        sb.append("{iChangeListeners=").append(iChangeListeners);
        sb.append(", iLocalDate=").append(iLocalDate);
        sb.append(", iDayChooser=").append(iDayChooser);
        sb.append(", iDayPanel=").append(iDayPanel);
        sb.append(", iMonthChooser=").append(iMonthChooser);
        sb.append(", iMonthPanel=").append(iMonthPanel);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iYearChooser=").append(iYearChooser);
        sb.append(", iYearPanel=").append(iYearPanel);
        sb.append('}');
        return sb.toString();
    }
}
