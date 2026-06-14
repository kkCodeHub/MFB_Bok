package se.swedsoft.bookkeeping.gui.util.dialogs;

import se.swedsoft.bookkeeping.data.system.SSMailTrustStore;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;

/**
 * Dialog for reviewing and approving an SMTP certificate.
 */
public final class SSMailCertificateDialog {

    private SSMailCertificateDialog() {
    }

    /**
     * Shows a certificate approval dialog.
     *
     * @param owner the parent frame
     * @param info the certificate information
     * @return {@code true} if the user approved the certificate
     */
    public static boolean showDialog(JFrame owner, SSMailTrustStore.MailCertificateInfo info) {
        if (owner == null || info == null) {
            return false;
        }

        String title = SSBundle.getBundle().getString("mail.certapprove.title");
        String message = SSBundle.getBundle().getString("mail.certapprove.message");
        String approve = SSBundle.getBundle().getString("mail.certapprove.import");
        String cancel = SSBundle.getBundle().getString("mail.certapprove.cancel");

        JTextArea textArea = new JTextArea(message + "\n\n" + SSMailTrustStore.buildDescription(info));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 360));
        scrollPane.setBorder(null);

        Object[] options = new Object[] { approve, cancel };
        Icon icon = SSIcon.getIcon("ICON_DIALOG_QUESTION");
        JOptionPane optionPane = new JOptionPane(scrollPane, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, icon, options, cancel);

        SSDialog dialog = new SSDialog(owner, title);
        dialog.setOptionPane(optionPane);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible();

        return approve.equals(optionPane.getValue());
    }

    /**
     * Shows a certificate approval dialog.
     *
     * @param owner the parent dialog
     * @param info the certificate information
     * @return {@code true} if the user approved the certificate
     */
    public static boolean showDialog(JDialog owner, SSMailTrustStore.MailCertificateInfo info) {
        if (owner == null || info == null) {
            return false;
        }

        String title = SSBundle.getBundle().getString("mail.certapprove.title");
        String message = SSBundle.getBundle().getString("mail.certapprove.message");
        String approve = SSBundle.getBundle().getString("mail.certapprove.import");
        String cancel = SSBundle.getBundle().getString("mail.certapprove.cancel");

        JTextArea textArea = new JTextArea(message + "\n\n" + SSMailTrustStore.buildDescription(info));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 360));
        scrollPane.setBorder(null);

        Object[] options = new Object[] { approve, cancel };
        Icon icon = SSIcon.getIcon("ICON_DIALOG_QUESTION");
        JOptionPane optionPane = new JOptionPane(scrollPane, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, icon, options, cancel);

        SSDialog dialog = new SSDialog(owner, title);
        dialog.setOptionPane(optionPane);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible();

        return approve.equals(optionPane.getValue());
    }
}

