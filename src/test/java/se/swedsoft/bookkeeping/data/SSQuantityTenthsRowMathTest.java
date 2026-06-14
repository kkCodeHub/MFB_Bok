package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SSQuantityTenthsRowMathTest {

    @Test
    void saleRowSumUsesTenthsQuantity() {
        SSSaleRow iRow = new SSSaleRow();
        iRow.setUnitprice(new BigDecimal("100.00"));
        iRow.setQuantity(25); // 2.5 units

        assertThat(iRow.getSum()).isPresent();
        assertThat(iRow.getSum().orElseThrow()).isEqualByComparingTo("250.00");
    }

    @Test
    void purchaseOrderRowSumUsesTenthsQuantity() {
        SSPurchaseOrderRow iRow = new SSPurchaseOrderRow();
        iRow.setUnitPrice(new BigDecimal("80.00"));
        iRow.setQuantity(25); // 2.5 units

        assertThat(iRow.getSum()).isPresent();
        assertThat(iRow.getSum().orElseThrow()).isEqualByComparingTo("200.00");
    }

    @Test
    void supplierInvoiceRowSumUsesTenthsQuantity() {
        SSSupplierInvoiceRow iRow = new SSSupplierInvoiceRow();
        iRow.setUnitprice(new BigDecimal("120.00"));
        iRow.setQuantity(15); // 1.5 units

        assertThat(iRow.getSum()).isPresent();
        assertThat(iRow.getSum().orElseThrow()).isEqualByComparingTo("180.00");
    }

    @Test
    void productSelectionDefaultsToOneWholeUnitInTenths() {
        SSProduct iProduct = new SSProduct();
        iProduct.setNumber("P-1");
        iProduct.setDescription("Test product");

        SSSaleRow iSaleRow = new SSSaleRow();
        iSaleRow.setProduct(iProduct);
        assertThat(iSaleRow.getQuantity()).isEqualTo(10);

        SSPurchaseOrderRow iPurchaseRow = new SSPurchaseOrderRow();
        iPurchaseRow.setProduct(iProduct);
        assertThat(iPurchaseRow.getQuantity()).isEqualTo(10);

        SSSupplierInvoiceRow iSupplierRow = new SSSupplierInvoiceRow();
        iSupplierRow.setProduct(iProduct);
        assertThat(iSupplierRow.getQuantity()).isEqualTo(10);

        SSProductRow iProductRow = new SSProductRow();
        iProductRow.setProduct(iProduct);
        assertThat(iProductRow.getQuantity()).isEqualTo(10);
    }
}

