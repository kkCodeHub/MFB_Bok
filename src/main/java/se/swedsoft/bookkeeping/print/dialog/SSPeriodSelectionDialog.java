package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;


/**
 * $Id$
 *
 */
public class SSPeriodSelectionDialog extends SSDialog {

    private SSDateChooser iTo;

    private SSDateChooser iFrom;

    private SSButtonPanel iButtonPanel;

    private JPanel iPanel;

    /**
     *
     * @param iMainFrame
     * @param iTitle
     */
    public SSPeriodSelectionDialog(SSMainFrame iMainFrame, String iTitle) {
        super(iMainFrame, iTitle);

        setPanel(iPanel);

        iButtonPanel.addCancelActionListener(e -> setModalResult(JOptionPane.CANCEL_OPTION, true));
        iButtonPanel.addOkActionListener(e -> setModalResult(JOptionPane.OK_OPTION, true));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());
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


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSPeriodSelectionDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iFrom=").append(iFrom);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTo=").append(iTo);
        sb.append('}');
        return sb.toString();
    }
}
