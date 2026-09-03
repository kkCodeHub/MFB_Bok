package se.swedsoft.bookkeeping.gui.resultunit;


import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.system.SSResultUnitContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.resultunit.panel.SSResultUnitPanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-okt-11
 * Time: 09:56:33
 */
public class SSResultUnitDialog {
    private SSResultUnitDialog() {}

    /**
     *
     * @param iMainFrame
     * @param model
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSTableModel<SSNewResultUnit> model) {
        final SSDialog          iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("resultunitframe.new.title"));
        final SSResultUnitPanel iPanel = new SSResultUnitPanel(false);

        iPanel.setResultUnit(new SSNewResultUnit());

        final ActionListener iSaveAction = e -> {

                SSNewResultUnit iResultUnit = iPanel.getResultUnit();

                List<SSNewResultUnit> iResultUnits = SSResultUnitContext.getResultUnits();

                for (SSNewResultUnit pResultUnit : iResultUnits) {
                    if (iResultUnit.getNumber().equals(pResultUnit.getNumber())) {
                        new SSErrorDialog(iMainFrame, "resultunitframe.duplicate",
                                iResultUnit.getNumber());
                        return;
                    }
                }

                SSResultUnitContext.addResultUnit(iResultUnit);

                if (model != null) {
                    model.setObjects(SSResultUnitContext.getResultUnits());
                }
                if (SSResultUnitFrame.getInstance() != null) {
                    SSResultUnitFrame.getInstance().updateFrame();
                }

                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> iDialog.closeDialog());

        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "resultunitframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);
        iDialog.pack();
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     *
     * @param iMainFrame
     * @param pResultUnit
     * @param model
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSNewResultUnit pResultUnit, final SSTableModel<SSNewResultUnit> model) {
        final SSDialog          iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("resultunitframe.edit.title"));
        final SSResultUnitPanel iPanel = new SSResultUnitPanel(true);

        iPanel.setResultUnit(pResultUnit);

        final ActionListener iSaveAction = e -> {


                SSNewResultUnit iResultUnit = iPanel.getResultUnit();

                SSResultUnitContext.updateResultUnit(iResultUnit);

                if (model != null) {
                    model.setObjects(SSResultUnitContext.getResultUnits());
                }
                if (SSResultUnitFrame.getInstance() != null) {
                    SSResultUnitFrame.getInstance().updateFrame();
                }
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "resultunitframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);
        iDialog.pack();
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

}
