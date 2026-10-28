package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierInvoiceMathTest {

    @AfterEach
    void clearSaldoMap() {
        SSSupplierInvoiceMath.iSaldoMap = null;
    }

    @Test
    void getInvoicesWithPositiveSaldoShouldExcludeZeroAndNegativeSaldo() {
        SSSupplierInvoice positive = new SSSupplierInvoice();
        positive.setNumber(1001);
        SSSupplierInvoice zero = new SSSupplierInvoice();
        zero.setNumber(1002);
        SSSupplierInvoice negative = new SSSupplierInvoice();
        negative.setNumber(1003);

        SSSupplierInvoiceMath.iSaldoMap = new HashMap<>();
        SSSupplierInvoiceMath.iSaldoMap.put(1001, new BigDecimal("50.00"));
        SSSupplierInvoiceMath.iSaldoMap.put(1002, BigDecimal.ZERO);
        SSSupplierInvoiceMath.iSaldoMap.put(1003, new BigDecimal("-10.00"));

        List<SSSupplierInvoice> result = SSSupplierInvoiceMath.getInvoicesWithPositiveSaldo(
                List.of(positive, zero, negative));

        assertThat(result).extracting(SSSupplierInvoice::getNumber).containsExactly(1001);
    }
}
