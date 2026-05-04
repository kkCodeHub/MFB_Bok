package se.swedsoft.bookkeeping.gui.inventory;


import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.inventory.panel.SSInventoryPanel;
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
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-sep-26
 * Time: 11:50:08
 */
public class SSInventoryDialog {
    private SSInventoryDialog() {}

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final AbstractTableModel pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inventortyframe.new.title"));

        SSInventory iInventory = new SSInventory();

        iInventory.setNumber(null);
        int iResponce = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                "inventortyframe.dialog.addproducts");
        final SSInventoryPanel iPanel = new SSInventoryPanel(iDialog);

        if (iResponce == JOptionPane.OK_OPTION) {
            List<SSProduct> iProducts = SSDB.getInstance().getProducts();

            for (SSProduct iProduct : iProducts) {
                if (iProduct.isStockProduct() && !iProduct.isParcel()) {
                    iInventory.getRows().add(new SSInventoryRow(iProduct, 0));
                }
            }

        }
        iPanel.setInventory(iInventory);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSInventory iInventory1 = iPanel.getInventory();

                SSDB.getInstance().addInventory(iInventory1);
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
                        "inventoryframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                SSInventory iInventory1 = iPanel.getInventory();

                SSDB.getInstance().addInventory(iInventory1);

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
     * @param iInventory
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSInventory iInventory, final AbstractTableModel pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inventortyframe.edit.title"));
        final SSInventoryPanel iPanel = new SSInventoryPanel(iDialog);

        iPanel.setInventory(new SSInventory(iInventory));

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSInventory iInventory1 = iPanel.getInventory();

                SSDB.getInstance().updateInventory(iInventory1);

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
                        "inventoryframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                SSInventory iInventory1 = iPanel.getInventory();

                SSDB.getInstance().updateInventory(iInventory1);

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
     * @param iInventory
     * @param pModel
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSInventory iInventory, final AbstractTableModel pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inventortyframe.copy.title"));
        final SSInventoryPanel iPanel = new SSInventoryPanel(iDialog);

        iPanel.setInventory(iInventory);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                SSInventory iInventory1 = iPanel.getInventory();

                SSDB.getInstance().addInventory(iInventory1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iDialog.closeDialog();

            });

        iPanel.addCancelActionListener(e -> iDialog.closeDialog());

        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.showDialog();
    }
}
