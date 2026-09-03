package se.swedsoft.bookkeeping.data.system;

import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSStandardText;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.util.SSMailMessage;
import se.swedsoft.bookkeeping.data.util.SSMailServer;
import se.swedsoft.bookkeeping.data.util.SymetricCrypter;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSMailCertificateDialog;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.util.SSUtil;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.security.GeneralSecurityException;

import java.awt.GraphicsEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for sending mail
 *
 * Date: 2007-mar-26
 * Time: 16:35:15
 * @version $Id$
 */
public class SSMail {

    private static final Logger LOG = LoggerFactory.getLogger(SSMail.class);
    private static final String MAIL_TRUSTSTORE_PASSWORD = "changeit";
    private static final int SMTP_TIMEOUT_MS = 20_000;

    // Used to encrypt password
    public static SymetricCrypter crypter = new SymetricCrypter(
            new byte[] { 0x4f, 0x53, -0x71, -0x28, 0x0d, 0x21, 0x1c, -0x1c});

    // Change this to get detailed debug info form JavaMail
    private static final boolean SHOULD_DEBUG_PRINT = false;

    // This dir is where to look for pdf to send as attachments.
    private static final File PDF_FILE_DIR = new File(Path.get(Path.APP_DATA), "pdftoemail");

    private SSMail() {}

    /**
     * Asks if the user really wants to send a mail, gets data from db, and calls
     * doSendMail to send it
     * @param pTo
     * @param pSubject
     * @param pFileName
     * @return
     * @throws AddressException
     * @throws MessagingException
     */
    public static boolean sendMail(String pTo, String pSubject, String pFileName)
        throws AddressException, MessagingException {

        SSUtil.verifyNotNull("Arguments to sendMail can not be null", pTo, pSubject,
                pFileName);

        if (SSQueryDialog.showDialog(SSMainFrame.getInstance(), SSBundle.getBundle(),
                "mail.send", pTo)
                != JOptionPane.OK_OPTION) {
            return false;
        }

        SSNewCompany company = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

        SSMailMessage message = new SSMailMessage(company.getEMail(), pTo, company.getMailServer().getBccAddresses(), pSubject,
                company.getStandardText(SSStandardText.Email).orElse(null), new File(PDF_FILE_DIR, pFileName).getPath());

        // Send message
        MimeMessage mimeMessage = makeMessage(company.getMailServer(), message);

        try {
            sendMimeMessage(mimeMessage);
            return true;
        } catch (MessagingException e) {
            if (isTrustFailure(e) && promptAndImportMailCertificate(company.getMailServer())) {
                try {
                    sendMimeMessage(makeMessage(company.getMailServer(), message));
                    return true;
                } catch (MessagingException retryException) {
                    logMailFailure(company.getMailServer(), message, retryException);
                    throw retryException;
                }
            }

            logMailFailure(company.getMailServer(), message, e);
            throw e;
        }
    }

    /**
     * Makes a MimeMessage ready to be send from the arguments
     * @param server
     * @param mail
     * @return
     * @throws MessagingException
     */
    public static MimeMessage makeMessage(SSMailServer server, SSMailMessage mail)
        throws MessagingException {

        SSUtil.verifyNotNull("server", server);
        SSUtil.verifyNotNull("Email message fields", mail.getFrom(), mail.getTo(),
                mail.getSubject());

        Session session = makeSession(server);
        MimeMessage message = makeMime(mail, session);
        Multipart multipart = makeMultipart(mail);

        // Put parts in message
        message.setContent(multipart);

        return message;
    }

    /**
     * Opens and closes an SMTP connection using the configured connection settings without sending a message.
     *
     * @param server the SMTP server configuration
     * @throws MessagingException if the connection cannot be established
     */
    public static void testConnection(SSMailServer server) throws MessagingException {
        SSUtil.verifyNotNull("server", server);

        MailTrustStoreState trustStoreState = useAppSpecificTrustStore();
        Transport transport = null;
        try {
            Session session = makeSession(server);
            transport = session.getTransport("smtp");
            transport.connect();
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException ignored) {
                    // Ignore close errors; the actual connection result is what the caller cares about.
                }
            }
            restoreTrustStore(trustStoreState);
        }
    }

    private static MimeMessage makeMime(SSMailMessage mail, Session session) throws MessagingException {
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(mail.getFrom()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail.getTo()));

        if (mail.getBcc() != null && !"".equals(mail.getBcc())) {
            for (String bcc : mail.getBcc().split(",[ ]*")) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
            }
        }
        message.setSubject(mail.getSubject());
        return message;
    }

    /**
     * Makes a Multipart from the data in the argument. If getFileName returns non-null,
     * that file is send as an attachment.
     * @param mail
     * @return
     * @throws MessagingException
     */
    private static Multipart makeMultipart(SSMailMessage mail) throws MessagingException {
        // Create the multi-part
        Multipart multipart = new MimeMultipart();

        // Create part one
        BodyPart messageBodyPart = new MimeBodyPart();

        // Fill the message
        messageBodyPart.setText(SSUtil.convertNullToEmpty(mail.getBodyText()));

        // Add the first part
        multipart.addBodyPart(messageBodyPart);

        // Part two is attachment
        if (mail.getFileName() != null) {

            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(mail.getFileName());

            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(mail.getFileName().substring(mail.getFileName().lastIndexOf(File.separator) + 1));

            // Add the second part
            multipart.addBodyPart(messageBodyPart);
        }

        return multipart;
    }

    /**
     * Makes a Session from the data in a SSMailServer
     * @param server
     * @return
     */
    private static Session makeSession(final SSMailServer server) {

        // Get system properties
        Properties props = new Properties();

        // Setup mail server
        props.put("mail.smtp.host", server.getURI().getHost());
        props.put("mail.smtp.port", Integer.toString(server.getURI().getPort()));
        props.put("mail.smtp.auth", Boolean.toString(server.isAuth()));
        props.put("mail.smtp.ssl.enable", Boolean.toString(server.isSSL()));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.starttls.enable", Boolean.toString(server.isStartTLS()));
        props.put("mail.smtp.starttls.required", Boolean.toString(server.isStartTLS()));
        props.put("mail.smtp.connectiontimeout", Integer.toString(SMTP_TIMEOUT_MS));
        props.put("mail.smtp.timeout", Integer.toString(SMTP_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", Integer.toString(SMTP_TIMEOUT_MS));

        Authenticator auth = null;

        // Create Authenticator if it should be used
        if (server.isAuth()) {
            auth = new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(server.getUsername(),
                            SSMail.crypter.decrypt(server.getPassword()));
                }
            };
        }

        // Get session
        Session session = Session.getInstance(props, auth);

        session.setDebug(SHOULD_DEBUG_PRINT);

        return session;
    }

    private static void sendMimeMessage(MimeMessage mimeMessage) throws MessagingException {
        MailTrustStoreState trustStoreState = useAppSpecificTrustStore();
        try {
            Transport.send(mimeMessage);
        } finally {
            restoreTrustStore(trustStoreState);
        }
    }

    private static boolean promptAndImportMailCertificate(SSMailServer server) {
        if (server == null || server.getConnectionSecurity() == null
                || server.getConnectionSecurity() == org.fribok.bookkeeping.data.util.ConnectionSecurity.NONE) {
            return false;
        }

        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }

        try {
            SSMailTrustStore.MailCertificateInfo certificateInfo = SSMailTrustStore.inspect(server);
            if (certificateInfo == null) {
                return false;
            }

            final boolean[] approved = new boolean[] { false };
            Runnable showDialog = () -> approved[0] = SSMailCertificateDialog.showDialog(
                    SSMainFrame.getInstance(), certificateInfo);

            if (SwingUtilities.isEventDispatchThread()) {
                showDialog.run();
            } else {
                SwingUtilities.invokeAndWait(showDialog);
            }

            if (!approved[0]) {
                return false;
            }

            SSMailTrustStore.importCertificates(certificateInfo);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Certificate approval dialog was interrupted", e);
        } catch (InvocationTargetException e) {
            LOG.warn("Certificate approval dialog could not be shown", e);
        } catch (IOException | GeneralSecurityException e) {
            LOG.error("Unable to import SMTP certificate", e);
            SSErrorDialog.showDialog(SSMainFrame.getInstance(),
                    SSBundle.getBundle().getString("mail.certapprove.import_failed.title"),
                    SSBundle.getBundle().getString("mail.certapprove.import_failed.message"));
        }

        return false;
    }

    private static boolean isTrustFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();

            if (className.endsWith("SSLHandshakeException")
                    || className.endsWith("ValidatorException")
                    || className.endsWith("SunCertPathBuilderException")) {
                return true;
            }

            if (message != null
                    && (message.contains("PKIX path building failed")
                    || message.contains("unable to find valid certification path")
                    || message.contains("certificate_unknown"))) {
                return true;
            }

            if (current instanceof MessagingException) {
                current = ((MessagingException) current).getNextException();
            } else {
                current = current.getCause();
            }
        }

        return false;
    }

    private static synchronized MailTrustStoreState useAppSpecificTrustStore() {
        File trustStore = getMailTrustStoreFile();
        if (!trustStore.isFile()) {
            return null;
        }

        MailTrustStoreState state = new MailTrustStoreState();
        state.trustStore = System.getProperty("javax.net.ssl.trustStore");
        state.trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
        state.trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        System.setProperty("javax.net.ssl.trustStore", trustStore.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        System.setProperty("javax.net.ssl.trustStorePassword", MAIL_TRUSTSTORE_PASSWORD);

        LOG.info("Using app-specific mail truststore at {}", trustStore.getAbsolutePath());
        return state;
    }

    private static synchronized void restoreTrustStore(MailTrustStoreState state) {
        if (state == null) {
            return;
        }

        restoreSystemProperty("javax.net.ssl.trustStore", state.trustStore);
        restoreSystemProperty("javax.net.ssl.trustStoreType", state.trustStoreType);
        restoreSystemProperty("javax.net.ssl.trustStorePassword", state.trustStorePassword);
    }

    private static void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static File getMailTrustStoreFile() {
        return SSMailTrustStore.getTrustStoreFile();
    }

    private static void logMailFailure(SSMailServer server, SSMailMessage mail, MessagingException exception) {
        StringBuilder iMessage = new StringBuilder();
        iMessage.append("SMTP send failed");

        if (getMailTrustStoreFile().isFile()) {
            iMessage.append(" [mailTrustStore=").append(getMailTrustStoreFile().getAbsolutePath()).append("]");
        }

        if (server != null && server.getURI() != null) {
            iMessage.append(" [host=").append(server.getURI().getHost());
            iMessage.append(", port=").append(server.getURI().getPort());
            iMessage.append(", auth=").append(server.isAuth());
            iMessage.append(", ssl=").append(server.isSSL());
            iMessage.append(", starttls=").append(server.isStartTLS());
            iMessage.append(", security=").append(server.getConnectionSecurity());
            iMessage.append("]");
        }

        if (mail != null) {
            iMessage.append(" [from=").append(mail.getFrom());
            iMessage.append(", to=").append(mail.getTo());
            iMessage.append(", subject=").append(mail.getSubject());
            iMessage.append("]");
        }

        iMessage.append(" ").append(describeMessagingException(exception));
        LOG.error(iMessage.toString(), exception);
    }

    private static String describeMessagingException(MessagingException exception) {
        StringBuilder iDescription = new StringBuilder();
        Map<Throwable, Boolean> iSeen = new IdentityHashMap<>();

        Throwable iCurrent = exception;
        while (iCurrent != null && !iSeen.containsKey(iCurrent)) {
            iSeen.put(iCurrent, Boolean.TRUE);

            if (!iDescription.isEmpty()) {
                iDescription.append(" -> ");
            }

            iDescription.append(iCurrent.getClass().getSimpleName());
            if (iCurrent.getMessage() != null && !iCurrent.getMessage().isEmpty()) {
                iDescription.append(": ").append(iCurrent.getMessage());
            }

            Throwable iNext = null;
            if (iCurrent instanceof MessagingException) {
                iNext = ((MessagingException) iCurrent).getNextException();
            }
            if (iNext == null) {
                iNext = iCurrent.getCause();
            }
            iCurrent = iNext;
        }

        return iDescription.toString();
    }

    /**
     * Throws an MailValidationException with an resource name that can be used to
     * get an error message from a resource file.
     * @param message
     * @param resourceName
     * @throws MailValidationException
     */
    public static void onError(String message, String resourceName) throws MailValidationException {
        throw new MailValidationException(message, resourceName);
    }

    /**
     * Checks if company and o has mail addresses and servers. If not, opens a error dialog
     * and returns false.
     * @param company
     * @param o can be either an SSSupplier or an SScustomer
     * @param resourceName
     * @return
     */
    private static boolean isOk(SSNewCompany company, Object o, String resourceName) {

        try {
            // Check company address and server
            if (company.getMailServer() == null) {
                onError("invalid mail server at current company", "mail.nosmtpserver");
            }

            if (SSUtil.isNullOrEmpty(company.getEMail())) {
                onError("invalid mail address at current company",
                        "mail.nocompanyemailaddress");
            }

            // Check supplier or customer address
            if (o == null) {
                onError("Argument object is null", resourceName);
            }

            String s;

            if (o instanceof SSCustomer) {
                s = ((SSCustomer) o).getEMail();
            } else if (o instanceof SSSupplier) {
                s = ((SSSupplier) o).getEMail();
            } else {
                throw new ClassCastException("o is of wrong type");
            }

            if (SSUtil.isNullOrEmpty(s)) {
                onError("No mail address", resourceName);
            }

        } catch (MailValidationException e) {
            // Some nessesery field wasnt set, open an error dialog to notify the user
            new SSErrorDialog(SSMainFrame.getInstance(), e.getResourceName());
            return false;
        }

        return true;
    }

    /**
     * Checks if the current company and iCustomer has mail addresses and servers. If not, opens a error dialog
     * and returns false.
     * @param iCustomer
     * @return
     */
    public static boolean isOk(SSCustomer iCustomer) {
        return isOk(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany(), iCustomer,
                "mail.nocustomeremailaddress");
    }

    /**
     * Checks if the current company and iSupplier has mail addresses and servers. If not, opens a error dialog
     * and returns false.
     * @param iSupplier
     * @return
     */
    public static boolean isOk(SSSupplier iSupplier) {
        return isOk(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany(), iSupplier,
                "mail.nosupplieremailaddress");
    }

    private static class MailValidationException extends Exception {
        private final String resourceName;

        public MailValidationException(String message, String resourceName) {
            super(message);
            this.resourceName = resourceName;
        }

        public String getResourceName() {
            return resourceName;
        }
    }

    private static class MailTrustStoreState {
        private String trustStore;
        private String trustStoreType;
        private String trustStorePassword;
    }
}
