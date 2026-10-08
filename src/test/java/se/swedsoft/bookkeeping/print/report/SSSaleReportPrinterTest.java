package se.swedsoft.bookkeeping.print.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.util.SSQuantityPrintUtil;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SSSaleReportPrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:sale-report-" + System.nanoTime() + ";sql.syntax_mys=true");
        Integer companyId = SSV2DatabaseFixture.createCompany(connection, "Testbolaget");
        SSV2DatabaseFixture.setCurrentCompany(companyId, "Testbolaget");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterEach
    void tearDown() throws Exception {
        SSV2DatabaseFixture.clearState();
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @Test
    void cancelledInvoicesAndCreditInvoicesAreExcludedFromSaleReportCounts() {
        SSProduct product = product("P-SALE-1");
        SSProductContext.addProduct(product);

        SSInvoice activeInvoice = invoice(false, product.getNumber(), 10, LocalDate.of(2025, 7, 10));
        SSInvoice cancelledInvoice = invoice(true, product.getNumber(), 100, LocalDate.of(2025, 7, 11));

        SSSalesContext.addInvoice(activeInvoice);
        SSSalesContext.addInvoice(cancelledInvoice);

        SSCreditInvoice activeCreditInvoice = creditInvoice(false, activeInvoice.getNumber(), product.getNumber(), 3,
                LocalDate.of(2025, 7, 12));
        SSCreditInvoice cancelledCreditInvoice = creditInvoice(true, activeInvoice.getNumber(), product.getNumber(), 50,
                LocalDate.of(2025, 7, 13));

        SSSalesContext.addCreditInvoice(activeCreditInvoice);
        SSSalesContext.addCreditInvoice(cancelledCreditInvoice);

        SSDefaultTableModel<?> model = new TestSaleReportPrinter(
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 31),
                SSSaleReportPrinter.SortingMode.Product,
                true).exposeModel();

        int productRow = rowByProductNumber(model, product.getNumber());
        assertThat((BigDecimal) model.getValueAt(productRow, 2))
                .isEqualByComparingTo(SSQuantityPrintUtil.toDisplay(7));
    }

    private static int rowByProductNumber(SSDefaultTableModel<?> model, String productNumber) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (productNumber.equals(model.getValueAt(i, 0))) {
                return i;
            }
        }
        throw new IllegalStateException("Product row not found: " + productNumber);
    }

    private static SSProduct product(String number) {
        SSProduct product = new SSProduct();
        product.setNumber(number);
        product.setDescription("Sale report product");
        product.setSellingPrice(new BigDecimal("10.00"));
        product.setStockPrice(new BigDecimal("5.00"));
        return product;
    }

    private static SSInvoice invoice(boolean cancelled, String productNr, int quantity, LocalDate date) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr("CUST-SALE");
        invoice.setCustomerName("Customer Sale");
        invoice.setLocalDate(date);
        invoice.setLocalDueDate(date.plusDays(30));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(productNr, quantity, "10.00"));
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(boolean cancelled, Integer creditingNr, String productNr, int quantity,
                                                 LocalDate date) {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setCustomerNr("CUST-SALE");
        invoice.setCustomerName("Customer Sale");
        invoice.setCreditingNr(creditingNr);
        invoice.setLocalDate(date);
        invoice.setLocalDueDate(date.plusDays(30));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(productNr, quantity, "10.00"));
        return invoice;
    }

    private static SSSaleRow row(String productNr, int quantity, String unitPrice) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription("Sale report row");
        row.setQuantity(quantity);
        row.setUnitprice(new BigDecimal(unitPrice));
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(3010);
        return row;
    }

    private static class TestSaleReportPrinter extends SSSaleReportPrinter {
        private TestSaleReportPrinter(LocalDate from, LocalDate to, SortingMode sortingMode, boolean ascending) {
            super(from, to, sortingMode, ascending);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
