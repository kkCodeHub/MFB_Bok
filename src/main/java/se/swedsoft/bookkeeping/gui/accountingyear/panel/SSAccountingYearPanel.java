package se.swedsoft.bookkeeping.gui.accountingyear.panel;

import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.gui.accountplans.util.SSAccountPlanTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanLoader;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * New accounting-year panel.
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

    public SSAccountingYearPanel() {
        ButtonGroup group = new ButtonGroup();
        group.add(iRadioUseLast);
        group.add(iRadioAccountPlan);
        iRadioAccountPlan.addChangeListener(e -> iAccountPlan.setEnabled(iRadioAccountPlan.isSelected()));
        iAccountPlan.setModel(SSAccountPlanTableModel.getDropDownModel());
        iAccountPlan.setSelected(iAccountPlan.getFirst());
    }

    public void setAccountingYear(SSNewAccountingYear pAccountingYear) {
        iAccountingYear = pAccountingYear;
        iFrom.setLocalDate(iAccountingYear.getLocalFrom());
        iTo.setLocalDate(iAccountingYear.getLocalTo());
        iAccountPlan.setSelected(iAccountingYear.getAccountPlan());
    }

    public SSNewAccountingYear getAccountingYear() {
        iAccountingYear.setLocalFrom(iFrom.getLocalDate());
        iAccountingYear.setLocalTo(iTo.getLocalDate());

        if (iAccountPlanPanel.isVisible()) {
            SSAccountPlan accountPlan = getAccountPlan();
            if (accountPlan != null) {
                SSNewAccountingYear lastYear = SSCompanyYearContext.getLastYear().orElse(null);
                boolean copyFromPreviousYear = lastYear != null
                        && lastYear.getAccountPlan() != null
                        && (iRadioUseLast.isSelected() || accountPlan == lastYear.getAccountPlan());

                SSAccountPlan yearPlan = new SSAccountPlan(accountPlan);
                int startYear = iAccountingYear.getLocalFrom() != null
                        ? iAccountingYear.getLocalFrom().getYear() : LocalDate.now().getYear();
                String companyName = getCurrentCompanyName();

                String planName = formatPlanName(companyName, startYear);
                yearPlan.setName(planName);
                yearPlan.setExcelPath(null);
                yearPlan.setDefaultPlan(false);
                if (copyFromPreviousYear) {
                    yearPlan.setBaseName(formatPlanName(companyName, startYear - 1));
                } else {
                    String templateName = accountPlan.getName();
                    if (templateName == null || templateName.trim().isEmpty()) {
                        templateName = planName;
                    }
                    yearPlan.setBaseName(templateName);
                }
                iAccountingYear.setAccountPlan(yearPlan);
            }
        }
        return iAccountingYear;
    }

    private String getCurrentCompanyName() {
        if (SSAccountingContext.getCurrentCompany() != null
                && SSAccountingContext.getCurrentCompany().getName() != null) {
            return SSAccountingContext.getCurrentCompany().getName().trim();
        }
        return "";
    }

    private String formatPlanName(String companyName, int year) {
        if (companyName == null || companyName.isEmpty()) {
            return Integer.toString(year);
        }
        return companyName + " " + year;
    }

    public SSAccountPlan getAccountPlan() {
        SSNewAccountingYear last = SSCompanyYearContext.getLastYear().orElse(null);
        if (iRadioUseLast.isSelected() && last != null && last.getAccountPlan() != null) {
            return last.getAccountPlan();
        }

        SSAccountPlan selected = iAccountPlan.getSelected();
        if (selected == null) {
            return null;
        }

        if (!selected.isTemplatePlan()) {
            return selected;
        }

        try {
            return SSAccountPlanLoader.loadPlan(selected);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void setYearFromAndTo() {
        SSNewAccountingYear last = SSCompanyYearContext.getLastYear().orElse(null);

        iRadioUseLast.setEnabled(last != null);
        iRadioAccountPlan.setEnabled(iAccountPlan.getFirst() != null);

        if (last == null && iAccountPlan.getFirst() == null) {
            iButtonPanel.getOkButton().setEnabled(false);
            return;
        }

        if (last != null) {
            iRadioUseLast.setSelected(true);

            LocalDate lastFrom = last.getLocalFrom();
            LocalDate lastTo = last.getLocalTo();
            LocalDate lastExclEnd = lastTo.plusDays(1);
            long diffMonths = ChronoUnit.MONTHS.between(lastFrom, lastExclEnd);

            LocalDate newFrom = lastExclEnd.withDayOfMonth(1);
            LocalDate newTo = newFrom.plusMonths(diffMonths).minusDays(1);

            iFrom.setLocalDate(newFrom);
            iTo.setLocalDate(newTo);
        } else {
            int year = LocalDate.now().getYear();
            iFrom.setLocalDate(LocalDate.of(year, 1, 1));
            iTo.setLocalDate(LocalDate.of(year, 12, 31));
            iAccountPlan.setSelected(iAccountPlan.getFirst());
        }
    }

    public JPanel getPanel() {
        return iPanel;
    }

    public void addOkAction(ActionListener e) {
        iButtonPanel.addOkActionListener(e);
    }

    public void addCancelAction(ActionListener e) {
        iButtonPanel.addCancelActionListener(e);
    }

    /**
     * Validates that a preceding year exists when "use last year's account plan" is selected.
     * If no immediately preceding year is found, shows an information dialog, disables the
     * radio button and selects "account plan" instead.
     *
     * @param pParent parent component for the message dialog
     * @return {@code true} if validation passed, {@code false} if the user must correct the selection
     */
    public boolean validatePreviousYearSelection(Component pParent) {
        if (!iRadioUseLast.isSelected()) {
            return true;
        }
        LocalDate newFrom = iFrom.getLocalDate();
        if (newFrom != null) {
            List<SSNewAccountingYear> years = SSCompanyYearContext.getYears();
            for (SSNewAccountingYear year : years) {
                if (newFrom.equals(year.getLocalTo().plusDays(1))) {
                    return true;
                }
            }
        }
        JOptionPane.showMessageDialog(
                pParent,
                SSBundle.getBundle().getString("accountingyearpanel.nopreviousyear.message"),
                SSBundle.getBundle().getString("accountingyearpanel.nopreviousyear.title"),
                JOptionPane.INFORMATION_MESSAGE);
        iRadioUseLast.setEnabled(false);
        iRadioAccountPlan.setSelected(true);
        return false;
    }

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
