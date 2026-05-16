package se.swedsoft.bookkeeping.gui.company.util;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Lightweight UI validators for company settings pages.
 */
public final class SSCompanyValidationUtils {

    private SSCompanyValidationUtils() {
    }

    public static void installMaxLengthFilter(JTextField iField, int iMaxLength) {
        if (iField == null || !(iField.getDocument() instanceof AbstractDocument)) {
            return;
        }
        ((AbstractDocument) iField.getDocument()).setDocumentFilter(
                new MaxLengthDocumentFilter(iMaxLength));
    }

    public static void installSwiftFilter(JTextField iField, int iMaxLength) {
        if (iField == null || !(iField.getDocument() instanceof AbstractDocument)) {
            return;
        }
        ((AbstractDocument) iField.getDocument()).setDocumentFilter(
                new SwiftDocumentFilter(iMaxLength));
    }

    private static class MaxLengthDocumentFilter extends DocumentFilter {

        private final int iMaxLength;

        private MaxLengthDocumentFilter(int iMaxLength) {
            this.iMaxLength = iMaxLength;
        }

        @Override
        public void replace(FilterBypass iBypass, int iOffset, int iLength, String iText,
                AttributeSet iAttrs) throws BadLocationException {
            String iSafeText = iText == null ? "" : iText;
            int iCurrentLength = iBypass.getDocument().getLength();
            int iResultLength = iCurrentLength - iLength + iSafeText.length();
            if (iResultLength <= iMaxLength) {
                super.replace(iBypass, iOffset, iLength, iSafeText, iAttrs);
            }
        }

        @Override
        public void insertString(FilterBypass iBypass, int iOffset, String iText,
                AttributeSet iAttr) throws BadLocationException {
            replace(iBypass, iOffset, 0, iText, iAttr);
        }
    }

    private static class SwiftDocumentFilter extends DocumentFilter {

        private final int iMaxLength;

        private SwiftDocumentFilter(int iMaxLength) {
            this.iMaxLength = iMaxLength;
        }

        @Override
        public void replace(FilterBypass iBypass, int iOffset, int iLength, String iText,
                AttributeSet iAttrs) throws BadLocationException {
            String iSanitized = sanitizeSwift(iText);
            int iCurrentLength = iBypass.getDocument().getLength();
            int iResultLength = iCurrentLength - iLength + iSanitized.length();
            if (iResultLength <= iMaxLength) {
                super.replace(iBypass, iOffset, iLength, iSanitized, iAttrs);
            }
        }

        @Override
        public void insertString(FilterBypass iBypass, int iOffset, String iText,
                AttributeSet iAttr) throws BadLocationException {
            replace(iBypass, iOffset, 0, iText, iAttr);
        }

        private String sanitizeSwift(String iText) {
            if (iText == null || iText.isEmpty()) {
                return "";
            }
            return iText.toUpperCase().replaceAll("[^A-Z0-9]", "");
        }
    }
}

