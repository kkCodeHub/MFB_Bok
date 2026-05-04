package se.swedsoft.bookkeeping.gui.autodist;


import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSAutoDistRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.autodist.panel.SSAutoDistPanel;
import se.swedsoft.bookkeeping.gui.autodist.util.SSAutoDistTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;


/**
 * User: Andreas Lago
 * Date: 2006-sep-21
 * Time: 11:56:22
 */
public class SSAutoDistDialog {

    private static ResourceBundle bundle = SSBundle.getBundle();

    private SSAutoDistDialog() {}

    /**
     *
     * @param iMainFrame
     * @param iAutoDist
     * @param pModel
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSAutoDist iAutoDist, final SSAutoDistTableModel pModel) {
        final SSDialog      iDialog = new SSDialog(iMainFrame,
                bundle.getString("autodistframe.copy.title"));
        final SSAutoDistPanel iPanel = new SSAutoDistPanel(iDialog, false);
        SSAutoDist iNew = new SSAutoDist(iAutoDist);

        iPanel.setAutoDist(iNew);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSAutoDist iAutoDist1 = iPanel.getAutoDist();

                for (SSAutoDist iCurrentAutoDist : SSDB.getInstance().getAutoDists()) {
                    if (iCurrentAutoDist.getNumber().equals(iAutoDist1.getNumber())) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistaccexists");
                        return;
                    }
                }
                for (SSAutoDistRow iRow : iAutoDist1.getRows()) {
                    if (iRow.getAccount() == null) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistrownoacc");
                        return;
                    }
                }
                SSDB.getInstance().addAutoDist(iAutoDist1);

                iDialog.closeDialog();

            };

        iPanel.addOkActionListener(iSaveAction);

        iPanel.addCancelActionListener(e -> iDialog.closeDialog());

        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "autodistframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     *
     * @param iMainFrame
     * @param iAutoDist
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSAutoDist iAutoDist, final SSAutoDistTableModel pModel) {
        final SSDialog      iDialog = new SSDialog(iMainFrame,
                bundle.getString("autodistframe.edit.title"));
        final SSAutoDistPanel iPanel = new SSAutoDistPanel(iDialog, true);

        final SSAutoDist iOriginal = iAutoDist;

        iPanel.setAutoDist(new SSAutoDist(iAutoDist));

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSAutoDist iAutoDist1 = iPanel.getAutoDist();

                for (SSAutoDist iCurrentAutoDist : SSDB.getInstance().getAutoDists()) {
                    if (iCurrentAutoDist.getNumber().equals(iAutoDist1.getNumber())
                            && !iAutoDist1.getNumber().equals(iOriginal.getNumber())) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistaccexists");
                        return;
                    }
                }
                for (SSAutoDistRow iRow : iAutoDist1.getRows()) {
                    if (iRow.getAccount() == null) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistrownoacc");
                        return;
                    }
                }
                SSDB.getInstance().updateAutoDist(iAutoDist1, iOriginal);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }
                iDialog.closeDialog();

            };

        iPanel.addOkActionListener(iSaveAction);

        iPanel.addCancelActionListener(e -> {

                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "autodistframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSAutoDistTableModel pModel) {
        final SSDialog      iDialog = new SSDialog(iMainFrame,
                bundle.getString("autodistframe.new.title"));
        final SSAutoDistPanel iPanel = new SSAutoDistPanel(iDialog, false);

        iPanel.setAutoDist(new SSAutoDist());

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSAutoDist iAutoDist = iPanel.getAutoDist();

                for (SSAutoDist iCurrentAutoDist : SSDB.getInstance().getAutoDists()) {
                    if (iCurrentAutoDist.getNumber().equals(iAutoDist.getNumber())) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistaccexists");
                        return;
                    }
                }
                for (SSAutoDistRow iRow : iAutoDist.getRows()) {
                    if (iRow.getAccount() == null) {
                        new SSErrorDialog(iMainFrame, "autodistframe.autodistrownoacc");
                        return;
                    }
                }
                SSDB.getInstance().addAutoDist(iAutoDist);

                iDialog.closeDialog();

            };

        iPanel.addOkActionListener(iSaveAction);

        iPanel.addCancelActionListener(e -> iDialog.closeDialog());
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "autodistframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }
}
