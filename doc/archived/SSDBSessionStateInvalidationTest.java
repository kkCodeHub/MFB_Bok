package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;

class SSDBSessionStateInvalidationTest {

    private SSDB iDb;

    @BeforeEach
    void setUp() {
        iDb = SSDB.getInstance();
        primeCaches();
    }

    @AfterEach
    void tearDown() {
        iDb.clearCachedLists();
        iDb.setCurrentYear(null);
    }

    @Test
    void clearAllCachesShouldClearBothCompanyAndYearCaches() {
        iDb.clearCachedLists();
        iDb.setCurrentYear(null);

        assertThat(iDb.iProducts).isNull();
        assertThat(iDb.iOrders).isNull();
        assertThat(iDb.iOutpayments).isNull();
        assertThat(iDb.iVouchers).isNull();
    }

    @Test
    void invalidateCachesForCompanyChangeShouldKeepYearCache() {
        iDb.setCurrentCompany(new SSNewCompany());

        assertThat(iDb.iProducts).isNull();
        assertThat(iDb.iInvoices).isNull();
        assertThat(iDb.iSupplierCreditInvoices).isNull();
        assertThat(iDb.iVouchers).isNotNull();
    }

    @Test
    void invalidateCachesForYearChangeShouldOnlyClearYearCache() {
        iDb.setCurrentYear(null);

        assertThat(iDb.iVouchers).isNull();
        assertThat(iDb.iProducts).isNotNull();
        assertThat(iDb.iOutpayments).isNotNull();
    }

    private void primeCaches() {
        iDb.iProducts = new LinkedList<>();
        iDb.iCustomers = new LinkedList<>();
        iDb.iSuppliers = new LinkedList<>();
        iDb.iAutoDists = new LinkedList<>();

        iDb.iInpayments = new LinkedList<>();
        iDb.iTenders = new LinkedList<>();
        iDb.iOrders = new LinkedList<>();
        iDb.iInvoices = new LinkedList<>();
        iDb.iCreditInvoices = new LinkedList<>();
        iDb.iPeriodicInvoices = new LinkedList<>();

        iDb.iOutpayments = new LinkedList<>();
        iDb.iPurchaseOrders = new LinkedList<>();
        iDb.iSupplierInvoices = new LinkedList<>();
        iDb.iSupplierCreditInvoices = new LinkedList<>();

        iDb.iInventories = new LinkedList<>();
        iDb.iIndeliveries = new LinkedList<>();
        iDb.iOutdeliveries = new LinkedList<>();

        iDb.iOwnReports = new LinkedList<>();
        iDb.iVouchers = new LinkedList<>();
    }
}


