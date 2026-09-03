/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.accountingyear.dialog;


import org.fribok.bookkeeping.app.SSDBUiInitializer;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSDBConfig;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.accountingyear.panel.SSAccountingYearPanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;


public class SSNewAccountingYearDialog {

    private static ResourceBundle bundle = SSBundle.getBundle();

    private SSNewAccountingYearDialog() {}

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void showDialog(final SSMainFrame iMainFrame, final SSDefaultTableModel<SSNewAccountingYear> pModel) {

        final SSDialog              iDialog = new SSDialog(iMainFrame,
                bundle.getString("accountingyearframe.new.title"));
        final SSAccountingYearPanel iPanel = new SSAccountingYearPanel();

        iPanel.setAccountingYear(new SSNewAccountingYear());
        iPanel.setYearFromAndTo();

        iPanel.addOkAction(
                e -> {
                        if (!iPanel.validatePreviousYearSelection(iDialog)) {
                            return;
                        }

                        SSNewAccountingYear iAccountingYear = iPanel.getAccountingYear();

                        if (iAccountingYear.getAccountPlan() != null) {
                            // Year creation copies the selected template into year-owned account rows.
                            // Do not create a new template plan row when creating a year.
                            SSAccountPlan iDetachedPlan = new SSAccountPlan(iAccountingYear.getAccountPlan());
                            iDetachedPlan.setId(null);
                            iAccountingYear.setAccountPlan(iDetachedPlan);
                        }

                        SSAccountingContext.addAccountingYear(iAccountingYear);

                        int iResponce = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                                "accountingyearframe.replaceyear",
                                iAccountingYear.toRenderString());

                        if (iResponce == JOptionPane.YES_OPTION) {

                            SSCompanyYearContext.openYear(iAccountingYear);
                            SSDBUiInitializer.initYear(true);
                            SSDBConfig.setYearId(SSAccountingContext.getCurrentCompany().getId(),
                                    iAccountingYear.getId());
                            // Close all year related frames
                            SSFrameManager.getInstance().close();
                        }

                        iDialog.closeDialog();


                    });

        iPanel.addCancelAction(e -> iDialog.closeDialog());
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);
        iDialog.pack();
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

}


