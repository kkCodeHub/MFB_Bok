package se.swedsoft.bookkeeping.gui.accountingyear.panel;


import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.accountplans.util.SSAccountPlanTableModel;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;


/**
 * Date: 2006-feb-15
 * Time: 12:20:25
 */
public class SSAccountingYearPanel {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    protected SSDateChooser iFrom;

    protected SSDateChooser iTo;

    protected JPanel iAccountPlanPanel;

    protected SSTableComboBox<SSAccountPlan> iAccountPlan;

    protected JRadioButton iRadioUseLast;

    protected JRadioButton iRadioAccountPlan;

    private SSNewAccountingYear iAccountingYear;

    /**
     *
     */
    public SSAccountingYearPanel() {
        ButtonGroup iGroup = new ButtonGroup();

        iGroup.add(iRadioUseLast);
        iGroup.add(iRadioAccountPlan);

        iRadioAccountPlan.addChangeListener(e -> iAccountPlan.setEnabled(iRadioAccountPlan.isSelected()));

        iAccountPlan.setModel(SSAccountPlanTableModel.getDropDownModel());
        iAccountPlan.setSelected(iAccountPlan.getFirst());
    }

    /**
     *
     * @param pAccountingYear
     */
    public void setAccountingYear(SSNewAccountingYear pAccountingYear) {
        iAccountingYear = pAccountingYear;

        iFrom.setLocalDate(iAccountingYear.getLocalFrom());
        iTo.setLocalDate(iAccountingYear.getLocalTo());
        iAccountPlan.setSelected(iAccountingYear.getAccountPlan());
    }

    /**
     *
     * @return
     */
    public SSNewAccountingYear getAccountingYear() {
        iAccountingYear.setLocalFrom(iFrom.getLocalDate());
        iAccountingYear.setLocalTo(iTo.getLocalDate());

        if (iAccountPlanPanel.isVisible()) {
            SSAccountPlan iAccountPlan = getAccountPlan();

            iAccountingYear.setAccountPlan(new SSAccountPlan(iAccountPlan, true));
        }
        return iAccountingYear;
    }

    /**
     *
     * @return
     */
    public SSAccountPlan getAccountPlan() {
        SSNewAccountingYear iLast = SSDB.getInstance().getLastYear().orElse(null);

        if (iRadioUseLast.isSelected() && iLast != null && iLast.getAccountPlan() != null) {
            return iLast.getAccountPlan();
        } else {
            return iAccountPlan.getSelected();
        }

    }

    /**
     * Computes the next accounting year's from and to dates based on the last year,
     * or defaults to the current calendar year if no previous year exists.
     */
    public void setYearFromAndTo() {
        SSNewAccountingYear iLast = SSDB.getInstance().getLastYear().orElse(null);

        iRadioUseLast.setEnabled(iLast != null);
        iRadioAccountPlan.setEnabled(iAccountPlan.getFirst() != null);

        if (iLast == null && iAccountPlan.getFirst() == null) {
            iButtonPanel.getOkButton().setEnabled(false);
            return;
        }

        if (iLast != null) {
            iRadioUseLast.setSelected(true);

            LocalDate lastFrom = iLast.getLocalFrom();
            LocalDate lastTo = iLast.getLocalTo();

            // Compute the length of the last accounting year in months
            // (lastTo + 1 day) - lastFrom gives the exclusive end
            LocalDate lastExclEnd = lastTo.plusDays(1);
            long diffMonths = ChronoUnit.MONTHS.between(lastFrom, lastExclEnd);

            // New year starts on the day after the last year ended
            LocalDate newFrom = lastExclEnd;
            // Set to first day of the month for the start
            newFrom = newFrom.withDayOfMonth(1);

            // New year ends after the same number of months
            LocalDate newTo = newFrom.plusMonths(diffMonths).minusDays(1);

            this.iFrom.setLocalDate(newFrom);
            this.iTo.setLocalDate(newTo);
        } else {
            int year = LocalDate.now().getYear();

            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            iFrom.setLocalDate(yearStart);
            iTo.setLocalDate(yearEnd);

            iAccountPlan.setSelected(iAccountPlan.getFirst());
        }
    }

    /**
     *
     * @return  The main panel
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @param e
     */
    public void addOkAction(ActionListener e) {
        iButtonPanel.addOkActionListener(e);
    }

    /**
     *
     * @param e
     */
    public void addCancelAction(ActionListener e) {
        iButtonPanel.addCancelActionListener(e);
    }

    /**
     *
     * @param iShow
     */
    public void setShowAccountPlanPanel(boolean iShow) {
        iAccountPlanPanel.setVisible(iShow);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.accountingyear.panel.SSAccountingYearPanel");
        sb.append("{iAccountingYear=").append(iAccountingYear);
        sb.append(", iAccountPlan=").append(iAccountPlan);
        sb.append(", iAccountPlanPanel=").append(iAccountPlanPanel);
        sb.append(", iButtonPanel=").append(iButtonPanel);
        sb.append(", iFrom=").append(iFrom);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iRadioAccountPlan=").append(iRadioAccountPlan);
        sb.append(", iRadioUseLast=").append(iRadioUseLast);
        sb.append(", iTo=").append(iTo);
        sb.append('}');
        return sb.toString();
    }
}
