package se.swedsoft.bookkeeping.gui.company;


import org.fribok.bookkeeping.app.SSDBUiInitializer;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.*;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.accountingyear.SSAccountingYearFrame;
import se.swedsoft.bookkeeping.gui.company.panel.SSCompanySettingsDialog;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;

import javax.swing.*;
import java.util.ResourceBundle;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:06:19
 */
public class SSCompanyDialog {

    private static ResourceBundle bundle = SSBundle.getBundle();

    private SSCompanyDialog() {}

    public static void editDialog(SSMainFrame iMainFrame, SSNewCompany pCompany, final SSDefaultTableModel<SSNewCompany> pModel) {
        SSCompanySettingsDialog iDialog = new SSCompanySettingsDialog(iMainFrame,
                bundle.getString("companyframe.edit.title"));

        iDialog.setSize(700, 550);
        iDialog.setLocationRelativeTo(iMainFrame);

        iDialog.setCompany(pCompany);

        final int iModalResult = iDialog.showDialog();

        if (iModalResult != JOptionPane.OK_OPTION) {
            if (iModalResult == JOptionPane.CLOSED_OPTION) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "companyframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
            } else {
                return;
            }
        }

        SSNewCompany iCompany = iDialog.getCompany();

        iCompany.setId(pCompany.getId());

        SSCompanyYearContext.updateCompany(iCompany);

        if (SSCompanyFrame.getInstance() != null) {
            SSCompanyFrame.getInstance().updateFrame();
        }
    }

    /**
     *
     * @param iMainFrame
     * @param pCompany
     * @param pModel
     */
    public static void editCurrentDialog(SSMainFrame iMainFrame, SSNewCompany pCompany, final SSDefaultTableModel<SSNewCompany> pModel) {
        SSCompanySettingsDialog iDialog = new SSCompanySettingsDialog(iMainFrame,
                bundle.getString("companyframe.editcurrent.title"));

        iDialog.setSize(700, 550);
        iDialog.setLocationRelativeTo(iMainFrame);

        iDialog.setCompany(pCompany);
        final int iModalResult = iDialog.showDialog();

        if (iModalResult != JOptionPane.OK_OPTION) {
            if (iModalResult == JOptionPane.CLOSED_OPTION) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "companyframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
            } else {
                return;
            }
        }
        SSNewCompany iCompany = iDialog.getCompany();

        iCompany.setId(pCompany.getId());

        SSCompanyYearContext.updateCompany(iCompany);

        if (SSCompanyFrame.getInstance() != null) {
            SSCompanyFrame.getInstance().updateFrame();
        }
    }

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSDefaultTableModel<SSNewCompany> pModel) {
        SSCompanySettingsDialog iDialog = new SSCompanySettingsDialog(iMainFrame,
                bundle.getString("companyframe.new.title"));

        iDialog.setSize(700, 550);
        iDialog.setLocationRelativeTo(iMainFrame);

        iDialog.setCompany(new SSNewCompany());

        final int iModalResult = iDialog.showDialog();

        if (iModalResult != JOptionPane.OK_OPTION) {
            if (iModalResult == JOptionPane.CLOSED_OPTION) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "companyframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
            } else {
                return;
            }
        }
        SSNewCompany iCompany = iDialog.getCompany();

        SSCompanyYearContext.addCompany(iCompany);
        SSCompanyFrame.getInstance().updateFrame();

        SSQueryDialog iQDialog = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                "companyframe.replacecompany", iCompany.getName());

        if (iQDialog.getResponce() == JOptionPane.YES_OPTION) {
            // Sätt det valda företaget som nuvarande företag
            SSCompanyYearContext.setCurrentCompany(iCompany);
            SSDBUiInitializer.init(true);
            SSDBConfig.setCompanyId(iCompany.getId());

            SSCompanyYearContext.setCurrentYear(null);
            if (SSCompanyFrame.getInstance() != null) {
                SSCompanyFrame.getInstance().updateFrame();
            }

            // Stäng alla fönster
            SSFrameManager.getInstance().close();

            SSAccountingYearFrame.showFrame(iMainFrame, 500, 300, true);

        } else {
            if (SSCompanyFrame.getInstance() != null) {
                SSCompanyFrame.getInstance().updateFrame();
            }
        }
        iDialog.closeDialog();
    }
}
