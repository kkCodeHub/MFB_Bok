package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technical verification of the debt refund (outpayment against a customer
 * invoice with negative saldo) flow: {@link SSInvoiceMath#getNegativeSaldoInvoices(List)},
 * {@link SSInpaymentMath#getOutpaymentAmountForInvoice(SSInvoice)} and the debet/credit
 * reversal performed by {@link SSInpayment#generateVoucher()} for a refund row.
 */
@Tag("integration")
class SSInpaymentDebtRefundIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_inpayment_debt_refund";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Debt Refund Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Debt Refund Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));

        SSAccountPlan accountPlan = new SSAccountPlan("BAS");
        List<SSAccount> accounts = new LinkedList<>();
        accounts.add(new SSAccount(1510));
        accounts.add(new SSAccount(1930));
        accountPlan.setAccounts(accounts);
        year.setAccountPlan(accountPlan);

        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void clearCaches() {
        SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void getNegativeSaldoInvoicesReturnsOnlyNegativeSaldoAndExcludesCancelled() {
        SSInvoice positiveSaldoInvoice = persistInvoiceWithBalance("DR-001", "Positive Saldo Customer");
        SSInvoice negativeSaldoInvoice = persistInvoiceWithBalance("DR-002", "Negative Saldo Customer");
        SSInvoice cancelledNegativeSaldoInvoice = persistInvoiceWithBalance("DR-003", "Cancelled Negative Saldo Customer");

        // Over-credit two of the invoices so that their saldo becomes negative.
        SSCreditInvoice creditForNegative = creditInvoice(negativeSaldoInvoice, "DR-002-CR", "1200.00");
        SSSalesContext.addCreditInvoice(creditForNegative);

        SSCreditInvoice creditForCancelled = creditInvoice(cancelledNegativeSaldoInvoice, "DR-003-CR", "1200.00");
        SSSalesContext.addCreditInvoice(creditForCancelled);

        cancelledNegativeSaldoInvoice.setCancelled(true);
        SSSalesContext.updateInvoice(cancelledNegativeSaldoInvoice);

        SSEventTriggerSyncContext.clearCachedLists();

        List<SSInvoice> negativeSaldoInvoices = SSInvoiceMath.getNegativeSaldoInvoices(
                List.of(positiveSaldoInvoice, negativeSaldoInvoice, cancelledNegativeSaldoInvoice));

        assertThat(negativeSaldoInvoices)
                .extracting(SSInvoice::getNumber)
                .contains(negativeSaldoInvoice.getNumber())
                .doesNotContain(positiveSaldoInvoice.getNumber(), cancelledNegativeSaldoInvoice.getNumber());

        SSSalesContext.deleteCreditInvoice(creditForNegative);
        SSSalesContext.deleteCreditInvoice(creditForCancelled);
        Repositories.invoices().delete(positiveSaldoInvoice);
        Repositories.invoices().delete(negativeSaldoInvoice);
        Repositories.invoices().delete(cancelledNegativeSaldoInvoice);
    }

    @Test
    void getOutpaymentAmountForInvoiceMirrorsInpaymentAmount() {
        SSInvoice positiveSaldoInvoice = persistInvoiceWithBalance("DR-010", "Positive Saldo Customer");
        SSInvoice negativeSaldoInvoice = persistInvoiceWithBalance("DR-011", "Negative Saldo Customer");

        SSCreditInvoice creditInvoice = creditInvoice(negativeSaldoInvoice, "DR-011-CR", "1200.00");
        SSSalesContext.addCreditInvoice(creditInvoice);

        SSEventTriggerSyncContext.clearCachedLists();

        // Positive saldo invoice: inpayment amount proposes the saldo, outpayment amount is zero.
        assertThat(SSInpaymentMath.getInpaymentAmountForInvoice(positiveSaldoInvoice))
                .isEqualByComparingTo(SSInvoiceMath.getSaldo(positiveSaldoInvoice));
        assertThat(SSInpaymentMath.getOutpaymentAmountForInvoice(positiveSaldoInvoice))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Negative saldo invoice: outpayment amount proposes the (negative) saldo, inpayment amount is zero.
        BigDecimal negativeSaldo = SSInvoiceMath.getSaldo(negativeSaldoInvoice);
        assertThat(negativeSaldo.signum()).isNegative();
        assertThat(SSInpaymentMath.getOutpaymentAmountForInvoice(negativeSaldoInvoice))
                .isEqualByComparingTo(negativeSaldo);
        assertThat(SSInpaymentMath.getInpaymentAmountForInvoice(negativeSaldoInvoice))
                .isEqualByComparingTo(BigDecimal.ZERO);

        SSSalesContext.deleteCreditInvoice(creditInvoice);
        Repositories.invoices().delete(positiveSaldoInvoice);
        Repositories.invoices().delete(negativeSaldoInvoice);
    }

    @Test
    void generateVoucherForDebtRefundRowReversesDebitCreditAndZeroesSaldo() {
        SSInvoice negativeSaldoInvoice = persistInvoiceWithBalance("DR-020", "Debt Refund Customer");

        SSCreditInvoice creditInvoice = creditInvoice(negativeSaldoInvoice, "DR-020-CR", "1200.00");
        SSSalesContext.addCreditInvoice(creditInvoice);
        SSEventTriggerSyncContext.clearCachedLists();

        BigDecimal negativeSaldo = SSInvoiceMath.getSaldo(negativeSaldoInvoice);
        assertThat(negativeSaldo.signum()).isNegative();

        // Build a regular inpayment (same invoice, positive value) and a debt refund (negative value)
        // so their generated vouchers can be compared row by row.
        SSInpayment regularInpayment = new SSInpayment();
        regularInpayment.setLocalDate(LocalDate.of(2025, 9, 1));
        regularInpayment.setNumber(1);
        SSInpaymentRow regularRow = new SSInpaymentRow();
        regularRow.setInvoiceNr(negativeSaldoInvoice.getNumber());
        regularRow.setCurrencyRate(BigDecimal.ONE);
        regularRow.setValue(new BigDecimal("300.00"));
        regularInpayment.getRows().add(regularRow);

        SSInpayment refundInpayment = new SSInpayment();
        refundInpayment.setLocalDate(LocalDate.of(2025, 9, 1));
        refundInpayment.setNumber(2);
        SSInpaymentRow refundRow = new SSInpaymentRow();
        refundRow.setInvoiceNr(negativeSaldoInvoice.getNumber());
        refundRow.setCurrencyRate(BigDecimal.ONE);
        refundRow.setValue(new BigDecimal("-300.00"));
        refundInpayment.getRows().add(refundRow);

        SSVoucher regularVoucher = regularInpayment.generateVoucher();
        SSVoucher refundVoucher = refundInpayment.generateVoucher();

        SSVoucherRow regularClaimRow = row(regularVoucher, 1510);
        SSVoucherRow regularPaymentRow = row(regularVoucher, 1930);
        SSVoucherRow refundClaimRow = row(refundVoucher, 1510);
        SSVoucherRow refundPaymentRow = row(refundVoucher, 1930);

        // Regular inpayment: credit CustomerClaim, debet InPayment.
        assertThat(regularClaimRow.getCredit()).isEqualByComparingTo("300.00");
        assertThat(regularClaimRow.getDebet()).isNull();
        assertThat(regularPaymentRow.getDebet()).isEqualByComparingTo("300.00");
        assertThat(regularPaymentRow.getCredit()).isNull();

        // Debt refund: debet/credit reversed compared to the regular inpayment.
        assertThat(refundClaimRow.getDebet()).isEqualByComparingTo("300.00");
        assertThat(refundClaimRow.getCredit()).isNull();
        assertThat(refundPaymentRow.getCredit()).isEqualByComparingTo("300.00");
        assertThat(refundPaymentRow.getDebet()).isNull();

        Repositories.invoices().delete(negativeSaldoInvoice);
        SSSalesContext.deleteCreditInvoice(creditInvoice);
    }

    private static SSVoucherRow row(SSVoucher voucher, int accountNumber) {
        return voucher.getRows().stream()
                .filter(iRow -> iRow.getAccount() != null && accountNumber == iRow.getAccount().getNumber())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No voucher row for account " + accountNumber));
    }

    private static SSInvoice persistInvoiceWithBalance(String customerNr, String customerName) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerName);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setType(SSInvoiceType.NORMAL);
        invoice.getRows().add(invoiceRow("P-" + customerNr, "Balanced row", new BigDecimal("1000.00"), 1, 3010));
        Repositories.invoices().add(invoice);
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(SSInvoice invoice, String customerNr, String rowValue) {
        SSCreditInvoice creditInvoice = new SSCreditInvoice(invoice);
        creditInvoice.setCustomerNr(customerNr);
        creditInvoice.getRows().add(invoiceRow("CR-" + customerNr, "Credit row", new BigDecimal(rowValue), 1, 3010));
        creditInvoice.setCancelled(false);
        return creditInvoice;
    }

    private static SSSaleRow invoiceRow(String productNr, String description, BigDecimal unitPrice, int quantity,
                                        int accountNumber) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(accountNumber);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        SSNewCompany company = new SSNewCompany();
        company.setName(name);
        SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}
