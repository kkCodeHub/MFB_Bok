package se.swedsoft.bookkeeping.gui.inpayment.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.ImageIcon;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSInpaymentTableModelTest {
    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_inpayment_table_model";

    private static Connection connection;

    @org.junit.jupiter.api.BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Inpayment TableModel Test AB");
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
    void firstColumnShouldShowEnteredIconForEnteredInpayment() {
        SSInpayment inpayment = new SSInpayment();
        inpayment.setEntered(true);

        SSInpaymentTableModel model = new SSInpaymentTableModel(List.of(inpayment));

        assertThat(model.getColumnName(0)).isEmpty();
        assertThat(model.getColumnClass(0)).isEqualTo(ImageIcon.class);
        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void firstColumnShouldBeEmptyForNotEnteredInpayment() {
        SSInpayment inpayment = new SSInpayment();
        inpayment.setEntered(false);

        SSInpaymentTableModel model = new SSInpaymentTableModel(List.of(inpayment));

        assertThat(model.getValueAt(0, 0)).isNull();
    }

    @Test
    void numberShouldRemainInSecondColumn() {
        SSInpayment inpayment = new SSInpayment();
        inpayment.setNumber(1234);

        SSInpaymentTableModel model = new SSInpaymentTableModel(List.of(inpayment));

        assertThat(model.getValueAt(0, 1)).isEqualTo(1234);
    }
}
