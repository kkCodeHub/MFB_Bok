package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSBigDecimalTextField;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:14:40
 */
public class SSCompanyPageTax extends SSCompanyPage {

    private SSNewCompany iCompany;

    private JPanel iPanel;

    private SSBigDecimalTextField iTaxRate1;
    private SSBigDecimalTextField iTaxRate2;
    private SSBigDecimalTextField iTaxRate3;
    private JComponent iFirstInvalidComponent;

    /**
     * @param iDialog
     */
    public SSCompanyPageTax(JDialog iDialog) {
        super(iDialog);
        addKeyListeners();
    }

    /**
     *
     * @return the name and title
     */
    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.tax");
    }

    /**
     *
     * @return the panel
     */
    @Override
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     * Set the company to edit
     *
     * @param iCompany
     */
    @Override
    public void setCompany(SSNewCompany iCompany) {
        this.iCompany = iCompany;

        iTaxRate1.setValue(iCompany.getTaxRate1());
        iTaxRate2.setValue(iCompany.getTaxRate2());
        iTaxRate3.setValue(iCompany.getTaxRate3());
    }

    /**
     * Get the edited company
     *
     * @return the company
     */
    @Override
    public SSNewCompany getCompany() {
        iCompany.setTaxrate1(iTaxRate1.getValue());
        iCompany.setTaxrate2(iTaxRate2.getValue());
        iCompany.setTaxrate3(iTaxRate3.getValue());

        return iCompany;
    }

    public void addKeyListeners() {
        SwingUtilities.invokeLater(() -> iTaxRate1.requestFocusInWindow());

        iTaxRate1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iTaxRate2.requestFocusInWindow());
                }
            }
        });

        iTaxRate2.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iTaxRate3.requestFocusInWindow());
                }
            }
        });
    }

    @Override
    public List<String> validatePage() {
        List<String> iErrors = new ArrayList<>();
        iFirstInvalidComponent = null;

        validateTaxRate(iErrors, iTaxRate1, "Tax page: Tax rate 1", iTaxRate1.getValue());
        validateTaxRate(iErrors, iTaxRate2, "Tax page: Tax rate 2", iTaxRate2.getValue());
        validateTaxRate(iErrors, iTaxRate3, "Tax page: Tax rate 3", iTaxRate3.getValue());
        return iErrors;
    }

    @Override
    public JComponent getFirstInvalidComponent() {
        return iFirstInvalidComponent;
    }

    private void validateTaxRate(List<String> iErrors, JComponent iField, String iLabel, BigDecimal iRate) {
        if (iRate == null) {
            return;
        }
        if (iRate.compareTo(BigDecimal.ZERO) < 0 || iRate.compareTo(new BigDecimal("100")) > 0) {
            iErrors.add(iLabel + " must be between 0 and 100.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iField;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPageTax");
        sb.append("{iCompany=").append(iCompany);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTaxRate1=").append(iTaxRate1);
        sb.append(", iTaxRate2=").append(iTaxRate2);
        sb.append(", iTaxRate3=").append(iTaxRate3);
        sb.append('}');
        return sb.toString();
    }
}
