package se.swedsoft.bookkeeping.gui.company.panel;


import org.fribok.bookkeeping.data.util.ConnectionSecurity;
import se.swedsoft.bookkeeping.data.system.SSMail;
import se.swedsoft.bookkeeping.data.system.SSMailTrustStore;
import se.swedsoft.bookkeeping.data.util.SSMailServer;
import se.swedsoft.bookkeeping.data.util.SSMailServerException;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSMailCertificateDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSProgressDialog;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A dialog where the user can enter info about a mail server to send mail with.
 *
 * When opened it takes a SSMailServer and sets the fields of the dialog, and
 * returns a new SSMailServer with the new information.
 *
 * Opened from SSCompanyPageAdditional.
 *
 * @author jensli
 *
 * $Id$
 *
 */
public class SSMailServerDialog extends SSDialog {    private static final Logger LOG = LoggerFactory.getLogger(SSMailServerDialog.class);


    private JPanel contentPane;
    private JTextField addressText;
    private JLabel addressLabel;
    private JTextField bccAddressesText;
    private JLabel bccAddressesLabel;
    private JCheckBox authCheckbox;
    private JLabel connectionSecurityLabel;
    private JComboBox<ConnectionSecurity> connectionSecurityCombobox;
    private JLabel userNameLabel;
    private JTextField usernameText;
    private JPasswordField passwordField;
    private JLabel passwordLabel;
    private JLabel portLabel;
    private JTextField portField;
    private JButton testConnectionButton;
    private JButton testAndImportCertificateButton;
    private SSButtonPanel iButtonPanel;

    private JDialog iParent;

    private SSMailServer iMailServer;

    private boolean shouldSave;

    public SSMailServerDialog(JDialog iParent) {

        super(iParent,
                SSBundle.getBundle().getString("companypanel.basic.server_dialog_title"));

        this.iParent = iParent;

        setLocationRelativeTo(iParent);

        setContentPane(contentPane);
        setModal(true);

        iButtonPanel.getOkButton().addActionListener(e -> onOK());

        iButtonPanel.getCancelButton().addActionListener(e -> onCancel());

        testConnectionButton.addActionListener(e -> onTestConnection());
        testAndImportCertificateButton.addActionListener(e -> onTestAndImportCertificate());

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        authCheckbox.addItemListener(e -> onNewAuthState(e.getStateChange() == ItemEvent.SELECTED));
	for (ConnectionSecurity type : ConnectionSecurity.values()) {
	    connectionSecurityCombobox.addItem(type);
	}

        pack();
    }

    /**
     * Sets the text of the fields in the dialog from the data in server.
     * @param server
     */
    private void loadFieldsFromServer(SSMailServer server) {

        addressText.setText(server.getURI().getHost());
        bccAddressesText.setText(server.getBccAddresses());
        authCheckbox.setSelected(server.isAuth());
	try {
	    connectionSecurityCombobox.setSelectedIndex(server.getConnectionSecurity().getIndex());
	} catch (NullPointerException ex) {
	    LOG.info("Just missing new connection security values of dialogue. Nothing to worry about.");
	}
        usernameText.setText(server.getUsername());
        passwordField.setText(SSMail.crypter.decrypt(server.getPassword()));
        portField.setText(Integer.toString(server.getURI().getPort()));

        onNewAuthState(server.isAuth());
    }

    /**
     * Reads the fields of the dialog and constructs a SSMailServer from
     * that, throwing if there was a format error in the fields.
     * @throws SSMailServerException
     * @return
     */
    private SSMailServer getServerFromFields() throws SSMailServerException {

        int port;

        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            throw new SSMailServerException("parse error for portField",
                    "mailserver.number_error");
        }

        return SSMailServer.makeIfValid("NONAME", addressText.getText(), port,
                bccAddressesText.getText(), authCheckbox.isSelected(), (ConnectionSecurity) connectionSecurityCombobox.getSelectedItem(), usernameText.getText(),
                SSMail.crypter.encrypt(String.valueOf(passwordField.getPassword())));
    }

    private void onTestConnection() {
        SSMailServer server = buildServerForToolAction();
        if (server == null) {
            return;
        }

        runWithProgress("mail.smtp_test_in_progress.title", () -> {
            try {
                SSMail.testConnection(server);
                showInfoDialogOnEdt("mail.smtp_test_success.title", "mail.smtp_test_success.message");
            } catch (MessagingException exc) {
                logSmtpTestFailure(server, exc);
                showFailureDialogOnEdt("mail.smtp_test_failed.title", "mail.smtp_test_failed.message", exc);
            }
        });
    }

    private void onTestAndImportCertificate() {
        SSMailServer server = buildServerForToolAction();
        if (server == null) {
            return;
        }

        runWithProgress("mail.smtp_test_in_progress.title", () -> {
            SSMailTrustStore.MailCertificateInfo certificateInfo;
            try {
                certificateInfo = SSMailTrustStore.inspect(server);
            } catch (IllegalArgumentException exc) {
                showFailureDialogOnEdt("mail.certapprove.no_tls.title", "mail.certapprove.no_tls.message", exc);
                return;
            } catch (IOException | GeneralSecurityException exc) {
                logCertificateOperationFailure(server, "inspect", exc);
                showFailureDialogOnEdt("mail.certapprove.inspect_failed.title", "mail.certapprove.inspect_failed.message", exc);
                return;
            }

            if (!showCertificateDialogOnEdt(certificateInfo)) {
                return;
            }

            try {
                SSMailTrustStore.importCertificates(certificateInfo);
            } catch (IOException | GeneralSecurityException exc) {
                logCertificateOperationFailure(server, "import", exc);
                showFailureDialogOnEdt("mail.certapprove.import_failed.title", "mail.certapprove.import_failed.message", exc);
                return;
            }

            try {
                SSMail.testConnection(server);
            } catch (MessagingException exc) {
                logSmtpTestFailure(server, exc);
                showFailureDialogOnEdt("mail.smtp_test_failed.title", "mail.smtp_test_failed.message", exc);
                return;
            }

            showInfoDialogOnEdt("mail.smtp_import_success.title", "mail.smtp_import_success.message");
        });
    }

    private void runWithProgress(String titleKey, Runnable action) {
        SSProgressDialog.runProgress(this, SSBundle.getBundle().getString(titleKey), action);
    }

    private void showInfoDialogOnEdt(String titleKey, String messageKey) {
        SwingUtilities.invokeLater(() -> showInfoDialog(titleKey, messageKey));
    }

    private void showFailureDialogOnEdt(String titleKey, String messageKey, Throwable throwable) {
        SwingUtilities.invokeLater(() -> showFailureDialog(titleKey, messageKey, throwable));
    }

    private boolean showCertificateDialogOnEdt(SSMailTrustStore.MailCertificateInfo certificateInfo) {
        if (SwingUtilities.isEventDispatchThread()) {
            return SSMailCertificateDialog.showDialog(this, certificateInfo);
        }

        final boolean[] approved = new boolean[] { false };
        try {
            SwingUtilities.invokeAndWait(() -> approved[0] = SSMailCertificateDialog.showDialog(this, certificateInfo));
            return approved[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Certificate approval dialog interrupted", e);
            return false;
        } catch (InvocationTargetException e) {
            LOG.warn("Certificate approval dialog failed", e);
            return false;
        }
    }

    private SSMailServer buildServerForToolAction() {
        try {
            return getServerFromFields();
        } catch (SSMailServerException exc) {
            showFieldError(exc.getResourceName());
            return null;
        }
    }

    private void showFieldError(String resourceName) {
        JOptionPane.showMessageDialog(this,
                SSBundle.getBundle().getString(resourceName),
                SSBundle.getBundle().getString("companypanel.basic.server_error_title"),
                JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoDialog(String titleKey, String messageKey) {
        JOptionPane.showMessageDialog(this,
                SSBundle.getBundle().getString(messageKey),
                SSBundle.getBundle().getString(titleKey),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFailureDialog(String titleKey, String messageKey, Throwable throwable) {
        StringBuilder message = new StringBuilder();
        message.append(SSBundle.getBundle().getString(messageKey));
        String details = describeThrowableChain(throwable);
        if (!details.isEmpty()) {
            message.append("\n\n").append(details);
        }

        JOptionPane.showMessageDialog(this,
                message.toString(),
                SSBundle.getBundle().getString(titleKey),
                JOptionPane.ERROR_MESSAGE);
    }

    private void logSmtpTestFailure(SSMailServer server, MessagingException exception) {
        StringBuilder message = new StringBuilder("SMTP connection test failed");
        message.append(describeServerConnection(server));

        String details = describeThrowableChain(exception);
        if (!details.isEmpty()) {
            message.append(" [details=").append(details.replace('\n', ' ')).append("]");
        }

        LOG.error(message.toString(), exception);
    }

    private void logCertificateOperationFailure(SSMailServer server, String operation, Throwable exception) {
        StringBuilder message = new StringBuilder("SMTP certificate ").append(operation).append(" failed");
        message.append(describeServerConnection(server));
        message.append(" [trustStore=").append(SSMailTrustStore.getTrustStoreFile().getAbsolutePath()).append("]");

        String details = describeThrowableChain(exception);
        if (!details.isEmpty()) {
            message.append(" [details=").append(details.replace('\n', ' ')).append("]");
        }

        LOG.error(message.toString(), exception);
    }

    private String describeServerConnection(SSMailServer server) {
        if (server == null || server.getURI() == null) {
            return "";
        }

        StringBuilder message = new StringBuilder();
        message.append(" [host=").append(server.getURI().getHost());
        message.append(", port=").append(server.getURI().getPort());
        message.append(", auth=").append(server.isAuth());
        message.append(", ssl=").append(server.isSSL());
        message.append(", starttls=").append(server.isStartTLS());
        message.append(", security=").append(server.getConnectionSecurity());
        message.append("]");
        return message.toString();
    }

    private String describeThrowableChain(Throwable throwable) {
        StringBuilder description = new StringBuilder();
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();

        Throwable current = throwable;
        while (current != null && !seen.containsKey(current)) {
            seen.put(current, Boolean.TRUE);

            if (!description.isEmpty()) {
                description.append("\n-> ");
            }

            description.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isEmpty()) {
                description.append(": ").append(current.getMessage());
            }

            Throwable next = current.getCause();
            if (current instanceof javax.mail.MessagingException) {
                Throwable nested = ((javax.mail.MessagingException) current).getNextException();
                if (nested != null) {
                    next = nested;
                }
            }
            current = next;
        }

        return description.toString();
    }

    /**
     * The method for opening the dialog. The data from server will be
     * copied to the text fields of the dialog, a SSMailServer constructed
     * from the edited fields will be returned.
     * @param server
     * @return
     */
    public SSMailServer showServerQuery(SSMailServer server) {

        if (server != null) {
            loadFieldsFromServer(server);
        }

        iMailServer = server;

        if (iParent != null) {
            setLocationRelativeTo(iParent);
        }

        setVisible();

        return iMailServer;
    }

    /**
     * Disables/enables the authorisation components
     * @param isEnabled
     */
    private void onNewAuthState(boolean isEnabled) {
        passwordField.setEnabled(isEnabled);
        passwordLabel.setEnabled(isEnabled);
        usernameText.setEnabled(isEnabled);
        userNameLabel.setEnabled(isEnabled);
    }

    /**
     * Called when the Ok button is clicked. Reads the fields of the dialog, constructs a
     * SSMailServer from them and sets iMailServer with that. Then close the dialog.
     *
     * If there was an error in the fields, open a dialog to inform about that.
     */
    private void onOK() {

        boolean shouldDiscard = true;

        try {
            iMailServer = getServerFromFields();
        } catch (SSMailServerException exc) {
            shouldDiscard = queryShouldDiscard(
                    SSBundle.getBundle().getString(exc.getResourceName()));
        }

        // Close the dialog, else leave dialog open and let user
        // fix the data
        if (shouldDiscard) {
            closeDialog(JOptionPane.OK_OPTION);
        }

    }

    /**
     * Opends a dialog asking if the user wants to discard the faulty info.
     * @param message
     * @return
     */
    private boolean queryShouldDiscard(String message) {

        int res = JOptionPane.showConfirmDialog(this,
                message + "\n\n"
                + SSBundle.getBundle().getString("companypanel.basic.server_error_message"),
                SSBundle.getBundle().getString("companypanel.basic.server_error_title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE);

        return res == JOptionPane.OK_OPTION;
    }

    /**
     * Closes the dialog without saving
     */
    private void onCancel() {
        closeDialog(JOptionPane.CANCEL_OPTION);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.panel.SSMailServerDialog");
        sb.append("{addressLabel=").append(addressLabel);
        sb.append(", addressText=").append(addressText);
        sb.append(", bccAddressesText=").append(bccAddressesText);
        sb.append(", authCheckbox=").append(authCheckbox);
        sb.append(", connectionSecurityLabel=").append(connectionSecurityLabel);
        sb.append(", connectionSecurityCombobox=").append(connectionSecurityCombobox);
        sb.append(", contentPane=").append(contentPane);
        sb.append(", iButtonPanel=").append(iButtonPanel);
        sb.append(", iMailServer=").append(iMailServer);
        sb.append(", iParent=").append(iParent);
        sb.append(", passwordField=").append(passwordField);
        sb.append(", passwordLabel=").append(passwordLabel);
        sb.append(", portField=").append(portField);
        sb.append(", portLabel=").append(portLabel);
        sb.append(", shouldSave=").append(shouldSave);
        sb.append(", userNameLabel=").append(userNameLabel);
        sb.append(", usernameText=").append(usernameText);
        sb.append('}');
        return sb.toString();
    }
}
