package se.swedsoft.bookkeeping.print.report.journals;

import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;

import java.util.Optional;

final class SSCustomerJournalVoucherResolver {

    private SSCustomerJournalVoucherResolver() {
    }

    static SSVoucher resolveVoucher(SSInvoice pInvoice) {
        SSVoucher iVoucher = pInvoice.getVoucher();
        if (hasRows(iVoucher)) {
            return iVoucher;
        }

        Optional<SSVoucher> iPersistedVoucher = resolvePersistedVoucher(pInvoice, iVoucher);
        if (iPersistedVoucher.isPresent() && hasRows(iPersistedVoucher.get())) {
            return iPersistedVoucher.get();
        }

        return generateVoucher(pInvoice);
    }

    private static Optional<SSVoucher> resolvePersistedVoucher(SSInvoice pInvoice, SSVoucher pVoucherReference) {
        if (!hasVoucherReference(pVoucherReference)) {
            return Optional.empty();
        }

        if (pInvoice.getLocalDate() != null) {
            SSNewAccountingYear iYear = SSAccountingContext.resolveAccountingYearForVoucherDate(
                    pInvoice.getLocalDate());
            return SSAccountingContext.getVoucher(iYear, pVoucherReference);
        }

        return SSAccountingContext.getVoucher(pVoucherReference);
    }

    private static SSVoucher generateVoucher(SSInvoice pInvoice) {
        if (pInvoice instanceof SSCreditInvoice) {
            return new SSCreditInvoice((SSCreditInvoice) pInvoice).generateVoucher();
        }

        return new SSInvoice(pInvoice).generateVoucher();
    }

    private static boolean hasRows(SSVoucher pVoucher) {
        return pVoucher != null && pVoucher.getRows() != null && !pVoucher.getRows().isEmpty();
    }

    private static boolean hasVoucherReference(SSVoucher pVoucher) {
        return pVoucher != null && pVoucher.getNumber() > 0;
    }
}
