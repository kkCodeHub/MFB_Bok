package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.gui.resultunit.util.SSResultUnitTableModel;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;


/**
 * $Id$
 *
 */
public class SSResultUnitResultSetupDialog extends SSDialog {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private JRadioButton iRadioAll;

    private JRadioButton iRadioSingle;

    private SSDateChooser iFrom;

    private SSDateChooser iTo;

    private SSTableComboBox<SSNewResultUnit> iResultUnit;

    /**
     *
     * @param iFrame
     * @param title
     */
    public SSResultUnitResultSetupDialog(JFrame iFrame, String title) {
        super(iFrame, title);

        setPanel(iPanel);

        iRadioSingle.addChangeListener(e -> iResultUnit.setEnabled(iRadioSingle.isSelected()));

        iButtonPanel.addCancelActionListener(e -> setModalResult(JOptionPane.CANCEL_OPTION, true));
        iButtonPanel.addOkActionListener(e -> setModalResult(JOptionPane.OK_OPTION, true));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        ButtonGroup iGroup = new ButtonGroup();

        iGroup.add(iRadioAll);
        iGroup.add(iRadioSingle);

        iResultUnit.setModel(SSResultUnitTableModel.getDropDownModel());
        iResultUnit.setSelected(iResultUnit.getFirst());

    }

    /**
     * @param pDate the start date
     */
    public void setLocalFrom(LocalDate pDate) {
        iFrom.setLocalDate(pDate);
    }

    /**
     * @param pDate the end date
     */
    public void setLocalTo(LocalDate pDate) {
        iTo.setLocalDate(pDate);
    }

    /**
     * @return the start date as a {@link LocalDate}
     */
    public LocalDate getLocalFrom() {
        return iFrom.getLocalDate();
    }

    /**
     * @return the end date as a {@link LocalDate}
     */
    public LocalDate getLocalTo() {
        return iTo.getLocalDate();
    }

    /**
     *
     * @return
     */
    public SSNewResultUnit getSelectedResultUnit() {
        if (iRadioSingle.isSelected()) {
            return iResultUnit.getSelected();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSResultUnitResultSetupDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iFrom=").append(iFrom);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iRadioAll=").append(iRadioAll);
        sb.append(", iRadioSingle=").append(iRadioSingle);
        sb.append(", iResultUnit=").append(iResultUnit);
        sb.append(", iTo=").append(iTo);
        sb.append('}');
        return sb.toString();
    }
}
