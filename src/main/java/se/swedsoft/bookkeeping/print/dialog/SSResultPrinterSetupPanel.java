package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;


/**
 * $Id$
 *
 */
public class SSResultPrinterSetupPanel {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private SSDateChooser iTo;

    private SSDateChooser iFrom;

    private JCheckBox iBudgetCheckbox;

    private JCheckBox iLastyearCheckBox;

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     * @return the end date as a {@link LocalDate}
     */
    public LocalDate getLocalTo() {
        return iTo.getLocalDate();
    }

    /**
     * @param to the end date
     */
    public void setLocalTo(LocalDate to) {
        iTo.setLocalDate(to);
    }

    /**
     * @return the start date as a {@link LocalDate}
     */
    public LocalDate getLocalFrom() {
        return iFrom.getLocalDate();
    }

    /**
     * @param from the start date
     */
    public void setLocalFrom(LocalDate from) {
        iFrom.setLocalDate(from);
    }

    /**
     *
     * @param pPrintBudget
     */
    public void setPrintBudget(boolean pPrintBudget) {
        iBudgetCheckbox.setSelected(pPrintBudget);
    }

    /**
     *
     * @return
     */
    public boolean getPrintBudget() {
        return iBudgetCheckbox.isSelected();
    }

    /**
     *
     * @param pLastyear
     */
    public void setPrintLastyear(boolean pLastyear) {
        iLastyearCheckBox.setSelected(pLastyear);
    }

    /**
     *
     * @return
     */
    public boolean getPrintLastyear() {
        return iLastyearCheckBox.isSelected();
    }

    /**
     *
     * @param pLastyearEnabled
     */
    public void setPrintLastyearEnabled(boolean pLastyearEnabled) {
        iLastyearCheckBox.setEnabled(pLastyearEnabled);
    }

    /**
     *
     * @param iListener
     */
    public void addCancelActionListener(ActionListener iListener) {
        iButtonPanel.addCancelActionListener(iListener);
    }

    /**
     *
     * @param iListener
     */
    public void addOkActionListener(ActionListener iListener) {
        iButtonPanel.addOkActionListener(iListener);
    }

    public SSButtonPanel getButtonPanel() {
	return iButtonPanel;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSResultPrinterSetupPanel");
        sb.append("{iBudgetCheckbox=").append(iBudgetCheckbox);
        sb.append(", iButtonPanel=").append(iButtonPanel);
        sb.append(", iFrom=").append(iFrom);
        sb.append(", iLastyearCheckBox=").append(iLastyearCheckBox);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTo=").append(iTo);
        sb.append('}');
        return sb.toString();
    }
}
