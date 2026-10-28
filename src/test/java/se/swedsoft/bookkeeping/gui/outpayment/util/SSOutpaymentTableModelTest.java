package se.swedsoft.bookkeeping.gui.outpayment.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.ImageIcon;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSOutpaymentTableModelTest {
    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_outpayment_table_model";

    private static Connection connection;

    @org.junit.jupiter.api.BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Outpayment TableModel Test AB");
        SSCompanyYearContext.addCompany(company);
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @org.junit.jupiter.api.AfterAll
    static void teardown() {
        System.clearProperty("fribok.schema.version");
    }

    @Test
    void numberShouldRemainInFirstColumn() {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setNumber(1234);

        SSOutpaymentTableModel model = new SSOutpaymentTableModel();
        model.addColumn(SSOutpaymentTableModel.COLUMN_ENTERED);
        model.addColumn(SSOutpaymentTableModel.COLUMN_NUMBER);
        model.addColumn(SSOutpaymentTableModel.COLUMN_DATE);
        model.addColumn(SSOutpaymentTableModel.COLUMN_TEXT);
        model.addColumn(SSOutpaymentTableModel.COLUMN_SUM);
        model.setObjects(List.of(outpayment));

        assertThat(model.getValueAt(0, 1)).isEqualTo(1234);
    }

    @Test
    void firstColumnShouldShowEnteredIconForEnteredOutpayment() {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setEntered(true);

        SSOutpaymentTableModel model = new SSOutpaymentTableModel();
        model.addColumn(SSOutpaymentTableModel.COLUMN_ENTERED);
        model.addColumn(SSOutpaymentTableModel.COLUMN_NUMBER);
        model.addColumn(SSOutpaymentTableModel.COLUMN_DATE);
        model.addColumn(SSOutpaymentTableModel.COLUMN_TEXT);
        model.addColumn(SSOutpaymentTableModel.COLUMN_SUM);
        model.setObjects(List.of(outpayment));

        assertThat(model.getColumnName(0)).isEmpty();
        assertThat(model.getColumnClass(0)).isEqualTo(ImageIcon.class);
        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void firstColumnShouldBeEmptyForNotEnteredOutpayment() {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setEntered(false);

        SSOutpaymentTableModel model = new SSOutpaymentTableModel();
        model.addColumn(SSOutpaymentTableModel.COLUMN_ENTERED);
        model.addColumn(SSOutpaymentTableModel.COLUMN_NUMBER);
        model.addColumn(SSOutpaymentTableModel.COLUMN_DATE);
        model.addColumn(SSOutpaymentTableModel.COLUMN_TEXT);
        model.addColumn(SSOutpaymentTableModel.COLUMN_SUM);
        model.setObjects(List.of(outpayment));

        assertThat(model.getValueAt(0, 0)).isNull();
    }

    @Test
    void sumColumnShouldUsePaymentAmountSum() {
        SSOutpayment outpayment = new SSOutpayment();
        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setValue(new BigDecimal("100.00"));
        row.setCurrencyRate(new BigDecimal("2.000000"));
        outpayment.getRows().add(row);

        SSOutpaymentTableModel model = new SSOutpaymentTableModel();
        model.addColumn(SSOutpaymentTableModel.COLUMN_ENTERED);
        model.addColumn(SSOutpaymentTableModel.COLUMN_NUMBER);
        model.addColumn(SSOutpaymentTableModel.COLUMN_DATE);
        model.addColumn(SSOutpaymentTableModel.COLUMN_TEXT);
        model.addColumn(SSOutpaymentTableModel.COLUMN_SUM);
        model.setObjects(List.of(outpayment));

        assertThat((BigDecimal) model.getValueAt(0, 4)).isEqualByComparingTo("100.00");
    }
}
