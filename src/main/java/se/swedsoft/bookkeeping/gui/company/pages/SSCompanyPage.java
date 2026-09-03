package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;

import javax.swing.*;
import java.util.Collections;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:10:40
 */
public abstract class SSCompanyPage {

    protected JDialog iDialog;

    /**
     *
     * @param iDialog
     */
    public SSCompanyPage(JDialog iDialog) {
        this.iDialog = iDialog;
    }

    /**
     *
     * @return the name and title
     */
    public abstract String getName();

    /**
     *
     * @return the panel
     */
    public abstract JPanel getPanel();

    /**
     * Set the company to edit
     *
     * @param iCompany
     */
    public abstract void setCompany(SSNewCompany iCompany);

    /**
     * Get the edited company
     *
     * @return the company
     */
    public abstract SSNewCompany getCompany();

    /**
     * Page-level validation executed before OK closes the dialog.
     *
     * @return user-facing validation messages, empty when valid.
     */
    public List<String> validatePage() {
        return Collections.emptyList();
    }

    /**
     * Optional first component to focus when this page fails validation.
     *
     * @return component to focus, or null when not applicable.
     */
    public JComponent getFirstInvalidComponent() {
        return null;
    }

    /**
     *
     * @param iDialog
     */
    public void setDialog(JDialog iDialog) {
        this.iDialog = iDialog;
    }

    // public static SSNewCompanyPage getInstance();

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPage");
        sb.append("{iDialog=").append(iDialog);
        sb.append('}');
        return sb.toString();
    }
}
