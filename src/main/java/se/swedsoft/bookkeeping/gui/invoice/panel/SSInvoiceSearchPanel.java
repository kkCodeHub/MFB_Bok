package se.swedsoft.bookkeeping.gui.invoice.panel;


import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInvoiceTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-nov-15
 * Time: 14:57:36
 */
public class SSInvoiceSearchPanel extends JPanel {
    private JPanel iPanel;

    private JTextField iTextField;

    private SSInvoiceTableModel iModel;

    public SSInvoiceSearchPanel(SSInvoiceTableModel iModel) {
        this.iModel = iModel;

        setLayout(new BorderLayout());
        setVisible(true);
        add(iPanel, BorderLayout.CENTER);

        iTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                ApplyFilter(SSSalesContext.getInvoices());
            }
        });
    }

    public void ApplyFilter(List<SSInvoice> iList) {
        List<SSInvoice> iFiltered = filterInvoices(iList, iTextField.getText());
        SSInvoiceFrame.getInstance().setFilterIndex(
                SSInvoiceFrame.getInstance().getTabbedPane().getSelectedIndex(), iFiltered);
        // iModel.setObjects(iFiltered);

    }

    static List<SSInvoice> filterInvoices(List<SSInvoice> iList, String iFilterText) {
        List<SSInvoice> iFiltered = new LinkedList<>();

        String iText = iFilterText == null ? "" : iFilterText.toLowerCase();

        for (SSInvoice iInvoice : iList) {
            String iNumber = iInvoice.getNumber() == null ? null : iInvoice.getNumber().toString();
            String iDescription = iInvoice.getCustomerName();
            String iCustomerNumber = iInvoice.getCustomerNr();
            String iTypeDescription = iInvoice.getType() == null ? null : iInvoice.getType().getDescription();
            String iTypeName = iInvoice.getType() == null ? null : iInvoice.getType().name();
            String iOriginInvoiceNumber = iInvoice.getOriginInvoiceNumber() == null
                    ? null : iInvoice.getOriginInvoiceNumber().toString();

            if ((iText.length() == 0)
                    || (iNumber != null && iNumber.toLowerCase().startsWith(iText))
                    || (iDescription != null && iDescription.toLowerCase().startsWith(iText))
                    || (iCustomerNumber != null && iCustomerNumber.toLowerCase().startsWith(iText))
                    || (iTypeDescription != null && iTypeDescription.toLowerCase().startsWith(iText))
                    || (iTypeName != null && iTypeName.toLowerCase().startsWith(iText))
                    || (iOriginInvoiceNumber != null && iOriginInvoiceNumber.startsWith(iText))) {
                iFiltered.add(iInvoice);
            }
        }
        return iFiltered;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.invoice.panel.SSInvoiceSearchPanel");
        sb.append("{iModel=").append(iModel);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTextField=").append(iTextField);
        sb.append('}');
        return sb.toString();
    }
}
