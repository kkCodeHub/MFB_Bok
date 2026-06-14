package se.swedsoft.bookkeeping.gui.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSIndeliveryRow;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
import se.swedsoft.bookkeeping.data.SSOutdeliveryRow;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.gui.indelivery.util.SSIndeliveryRowTableModel;
import se.swedsoft.bookkeeping.gui.inventory.util.SSInventoryRowTableModel;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInvoiceRowTableModel;
import se.swedsoft.bookkeeping.gui.outdelivery.util.SSOutdeliveryRowTableModel;
import se.swedsoft.bookkeeping.gui.supplierinvoice.util.SSSupplierInvoiceRowTableModel;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SSWholeQuantityRowModelValidationTest {

    @Test
    void invoiceRowRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-INVOICE");
        SSSaleRow row = new SSSaleRow();
        row.setProduct(product);
        row.setDescription("Invoice description");
        row.setUnitprice(new BigDecimal("19.90"));
        row.setQuantity(20);

        SSInvoiceRowTableModel.COLUMN_QUANTITY.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getQuantity()).isEqualTo(20);
        assertThat(row.getDescription()).isEqualTo("Invoice description");
        assertThat(row.getUnitprice()).isEqualByComparingTo("19.90");
    }

    @Test
    void supplierInvoiceRowRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-SUPPLIER");
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProduct(product);
        row.setDescription("Supplier description");
        row.setUnitprice(new BigDecimal("24.50"));
        row.setQuantity(20);

        SSSupplierInvoiceRowTableModel.COLUMN_QUANTITY.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getQuantity()).isEqualTo(20);
        assertThat(row.getDescription()).isEqualTo("Supplier description");
        assertThat(row.getUnitprice()).isEqualByComparingTo("24.50");
    }

    @Test
    void indeliveryRowRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-INDELIVERY");
        SSIndeliveryRow row = new SSIndeliveryRow();
        row.setProduct(product);
        row.setChange(20);

        SSIndeliveryRowTableModel.COLUMN_CHANGE.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getChange()).isEqualTo(20);
        assertThat(row.getProduct()).isSameAs(product);
    }

    @Test
    void outdeliveryRowRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-OUTDELIVERY");
        SSOutdeliveryRow row = new SSOutdeliveryRow();
        row.setProduct(product);
        row.setChange(20);

        SSOutdeliveryRowTableModel.COLUMN_CHANGE.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getChange()).isEqualTo(20);
        assertThat(row.getProduct()).isSameAs(product);
    }

    @Test
    void inventoryChangeRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-INVENTORY-CHANGE");
        SSInventoryRow row = new SSInventoryRow();
        row.setProduct(product);
        row.setChange(20);

        SSInventoryRowTableModel.COLUMN_CHANGE.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getChange()).isEqualTo(20);
    }

    @Test
    void inventoryQuantityRejectsDecimalWhenProductOnlyAllowsWhole() {
        SSProduct product = wholeOnlyProduct("P-INVENTORY-QTY");
        SSInventoryRow row = new SSInventoryRow();
        row.setProduct(product);
        row.setStockQuantity(20);
        row.setChange(2);

        SSInventoryRowTableModel.COLUMN_INVENTORYQUANTITY.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getInventoryQuantity()).contains(22);
        assertThat(row.getStockQuantity()).isEqualTo(20);
        assertThat(row.getChange()).isEqualTo(2);
    }

    @Test
    void invoiceRowAcceptsDecimalWhenWholeOnlyIsDisabled() {
        SSProduct product = decimalAllowedProduct("P-INVOICE-DECIMAL");

        SSSaleRow row = new SSSaleRow();
        row.setProduct(product);
        row.setDescription("Invoice decimal");
        row.setQuantity(20);

        SSInvoiceRowTableModel.COLUMN_QUANTITY.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getQuantity()).isEqualTo(15);
        assertThat(row.getDescription()).isEqualTo("Invoice decimal");
    }

    @Test
    void supplierInvoiceRowAcceptsDecimalWhenWholeOnlyIsDisabled() {
        SSProduct product = decimalAllowedProduct("P-SUPPLIER-DECIMAL");

        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProduct(product);
        row.setDescription("Supplier decimal");
        row.setQuantity(20);

        SSSupplierInvoiceRowTableModel.COLUMN_QUANTITY.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getQuantity()).isEqualTo(15);
        assertThat(row.getDescription()).isEqualTo("Supplier decimal");
    }

    @Test
    void inventoryChangeAcceptsDecimalWhenWholeOnlyIsDisabled() {
        SSProduct product = decimalAllowedProduct("P-INVENTORY-CHANGE-DECIMAL");

        SSInventoryRow row = new SSInventoryRow();
        row.setProduct(product);
        row.setStockQuantity(20);
        row.setChange(0);

        SSInventoryRowTableModel.COLUMN_CHANGE.setValue(row, new BigDecimal("1.5"));

        assertThat(row.getChange()).isEqualTo(15);
        assertThat(row.getStockQuantity()).isEqualTo(20);
    }

    @Test
    void invoiceQuantityValidationCanRunOnEdt() throws Exception {
        SSProduct product = wholeOnlyProduct("P-INVOICE-EDT");
        SSSaleRow row = new SSSaleRow();
        row.setProduct(product);
        row.setQuantity(20);

        SwingUtilities.invokeAndWait(() -> {
            assertThat(SwingUtilities.isEventDispatchThread()).isTrue();
            SSInvoiceRowTableModel.COLUMN_QUANTITY.setValue(row, new BigDecimal("1.5"));
        });

        assertThat(row.getQuantity()).isEqualTo(20);
    }

    private static SSProduct wholeOnlyProduct(String number) {
        SSProduct product = new SSProduct();
        product.setNumber(number);
        product.setOnlyWholeQuantity(true);
        return product;
    }

    private static SSProduct decimalAllowedProduct(String number) {
        SSProduct product = new SSProduct();
        product.setNumber(number);
        product.setOnlyWholeQuantity(false);
        return product;
    }
}
