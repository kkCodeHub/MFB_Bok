package se.swedsoft.bookkeeping.gui.outdelivery;


import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.outdelivery.panel.SSOutdeliveryPanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * User: Andreas Lago
 * Date: 2006-sep-26
 * Time: 11:50:08
 */
public class SSOutdeliveryDialog {
    private SSOutdeliveryDialog() {}

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final AbstractTableModel pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outdeliveryframe.new.title"));
        final SSOutdeliveryPanel iPanel = new SSOutdeliveryPanel(iDialog);

        SSOutdelivery iOutdelivery = new SSOutdelivery();

        iOutdelivery.setNumber(null);
        iPanel.setOutdelivery(iOutdelivery);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().addOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iDialog.closeDialog();

            });

        iPanel.addCancelActionListener(e -> iDialog.closeDialog());

        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outdeliveryframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().addOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iDialog.closeDialog();
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.showDialog();
    }

    /**
     *
     * @param iMainFrame
     * @param iOutdelivery
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSOutdelivery iOutdelivery, final AbstractTableModel pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outdeliveryframe.edit.title"));
        final SSOutdeliveryPanel iPanel = new SSOutdeliveryPanel(iDialog);

        iPanel.setOutdelivery(new SSOutdelivery(iOutdelivery));

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().updateOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }
                iDialog.closeDialog();

            });

        iPanel.addCancelActionListener(e -> {

                iDialog.closeDialog();

            });

        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outdeliveryframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().updateOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }
                iDialog.closeDialog();
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.showDialog();
    }

    /**
     *
     * @param iMainFrame
     * @param iCopyFrom
     * @param pModel
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSOutdelivery iCopyFrom, final AbstractTableModel pModel) {
        final SSDialog          iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outdeliveryframe.copy.title"));
        final SSOutdeliveryPanel iPanel = new SSOutdeliveryPanel(iDialog);

        SSOutdelivery iOutdelivery = new SSOutdelivery(iCopyFrom);

        iOutdelivery.setNumber(null);

        iPanel.setOutdelivery(iOutdelivery);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().addOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iDialog.closeDialog();

            });

        iPanel.addCancelActionListener(e -> iDialog.closeDialog());

        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outdeliveryframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                SSOutdelivery iOutdelivery1 = iPanel.getoutdelivery();

                SSDB.getInstance().addOutdelivery(iOutdelivery1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iDialog.closeDialog();
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.showDialog();
    }
}
