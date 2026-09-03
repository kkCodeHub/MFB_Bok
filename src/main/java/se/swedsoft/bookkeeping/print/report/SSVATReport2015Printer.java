package se.swedsoft.bookkeeping.print.report;


import se.swedsoft.bookkeeping.calc.math.SSVoucherMath;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.SSPrinter;
import se.swedsoft.bookkeeping.print.util.SSDefaultJasperDataSource;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * $Id$
 *
 * Med importmoms gällande från 1 januari 2015
 */
public class SSVATReport2015Printer extends SSPrinter {    private static final Logger LOG = LoggerFactory.getLogger(SSVATReport2015Printer.class);


    private SSNewAccountingYear iAccountingYear;

    private LocalDate iDateFrom;

    private LocalDate iDateTo;

    private int iStartVoucher;

    private SSVATReportRowPrinter iPrinter;

    private SSDefaultJasperDataSource iDataSource;

    private List<SSAccount> iAccounts;

    private Map<String, List<SSAccount>> iAccountsByVatCode;

    private Map<SSAccount, BigDecimal> iCreditMinusDebetSum;

    private Map<SSAccount, BigDecimal> iDebetMinusCreditSum;

    /**
     *
     * @param iAccountingYear
     * @param iDateFrom
     * @param iDateTo
     * @param iStartVoucher
     */
    public SSVATReport2015Printer(SSNewAccountingYear iAccountingYear, LocalDate iDateFrom,
            LocalDate iDateTo, int iStartVoucher) {
        this.iAccountingYear = iAccountingYear;
        this.iDateFrom = iDateFrom;
        this.iDateTo = iDateTo;
        this.iStartVoucher = iStartVoucher;
        iAccounts = iAccountingYear.getAccounts();

        setPageHeader("header_period.jrxml");
        setColumnHeader("vatreport2015.jrxml");
        setDetail("vatreport2015.jrxml");

        calculate();
    }

    /**
     * Gets the title file for this repport
     *
     * @return
     */
    @Override
    public String getTitle() {
        return SSBundle.getBundle().getString("vatreport2015.title");
    }

    /**
     *
     */
    private void calculate() {
        // Get all vouchers
        List<SSVoucher> iVouchers = SSVoucherMath.getVouchers(
                iAccountingYear.getVouchers(), iDateFrom, iDateTo);
	final int iStartVoucherIndex = iStartVoucher - 1;
	List<SSVoucher> iVouchers2 = iVouchers;
	if (iStartVoucherIndex >= 0 && iStartVoucherIndex < iVouchers.size()) {
	    iVouchers2 = iVouchers.subList(iStartVoucherIndex, iVouchers.size());
	} else {
	    LOG.error("Använder hela periodens verifikat då börja-med-verifikat ligger utanför giltigt intervall.");
	}
        iCreditMinusDebetSum = SSVoucherMath.getCreditMinusDebetSum(iVouchers2);
        iDebetMinusCreditSum = SSVoucherMath.getDebetMinusCreditSum(iVouchers2);

        iAccountsByVatCode = new HashMap<>();

        for (SSAccount iAccount : iAccounts) {
            String iVATCode = iAccount.getVATCode();

            List<SSAccount> iAccountsForVatCode = iAccountsByVatCode.get(iVATCode);

            if (iAccountsForVatCode == null) {
                iAccountsForVatCode = new LinkedList<>();

                iAccountsByVatCode.put(iVATCode, iAccountsForVatCode);
            }
            iAccountsForVatCode.add(iAccount);
        }
    }

    /**
     *
     * @param group
     * @return
     */
    private String getVATCodesForGroup(Integer group) {
        // Returnerar text som skrivs på momsrapport och visar momskod.
        switch (group) {
        // A. Momspliktig försäljning eller uttag exklusive moms
        case 5:
//            return  "5MP1, 5MP2, 5MP3, 5PTOG";
            return  "5_25, 5_12, 5_6";

        case 6:
            return "6_25, 6_12, 6_6";

        case 7:
            return "7_25. 7_12, 7_6";

        case 8:
            return "8";

        // B. Utgående moms på försäljning eller uttag i ruta 5-8
        case 10:
            return "10";

        case 11:
            return "11";

        case 12:
            return "12";

        // C. Momspliktiga inköp där köparen är skattskyldig.
        case 20:
            return "20";

        case 21:
            return "21";

        case 22:
            return "22";

        case 23:
            return "23";

        case 24:
            return "24";

        // D. Utgående moms på inköp i ruta 20 - 24
        case 30:
            return "30";

        case 31:
            return "31";

        case 32:
            return "32";

        // E. Försäljning m.m. som är undantagen från moms.
        case 35:
            return "35";

        case 36:
            return "36";

        case 37:
            return "37";

        case 38:
            return "38";

        case 39:
            return "39";

        case 40:
            return "40";

        case 41:
            return "41";

        case 42:
            return "42";

        // F. Ingående moms
        case 48:
            return "48";

        // G. Moms att betala eller få tillbaka.
        case 49:
            return "R1, R2";

	// H. Importmoms
        case 50:
            return "50_25, 50_12, 50_6";

	// I. Utgående moms på import i ruta 50
        case 60:
            return "60";

        case 61:
            return "61";

        case 62:
            return "62";
        }
        return null;
    }

    /**
     *
     * @param group
     * @return
     */
    private BigDecimal getValueForGroup(Integer group) {

        switch (group) {
        // A. Momspliktig försäljning eller uttag exklusive moms
        case 5:
            return getSumForAccounts(iCreditMinusDebetSum, "5_25", "5_12", "5_6");

        case 6:
            return getSumForAccounts(iCreditMinusDebetSum, "6_25", "6_12", "6_6");

        case 7:
            return getSumForAccounts(iCreditMinusDebetSum, "7_25", "7_12", "7_6");

        case 8:
            return getSumForAccounts(iCreditMinusDebetSum, "8");

        // B. Utgående moms på försäljning eller uttag i ruta 5-8

        case 10:
            return getSumForAccounts(iCreditMinusDebetSum, "10");

        case 11:
            return getSumForAccounts(iCreditMinusDebetSum, "11");

        case 12:
            return getSumForAccounts(iCreditMinusDebetSum, "12");

        // C. Momspliktiga inköp där köparen är skatteskyldig.

        case 20:
            return getSumForAccounts(iDebetMinusCreditSum, "20");

        case 21:
            return getSumForAccounts(iDebetMinusCreditSum, "21");

        case 22:
            return getSumForAccounts(iDebetMinusCreditSum, "22");

        case 23:
            return getSumForAccounts(iDebetMinusCreditSum, "23");

        case 24:
            return getSumForAccounts(iDebetMinusCreditSum, "24");

        // D. Utgående moms på inköp i ruta 20 - 24
        case 30:
            return getSumForAccounts(iCreditMinusDebetSum, "30");

        case 31:
            return getSumForAccounts(iCreditMinusDebetSum, "31");

        case 32:
            return getSumForAccounts(iCreditMinusDebetSum, "32");

        // E. Försäljning m.m. som är undantagen från moms.
        case 35:
            return getSumForAccounts(iCreditMinusDebetSum, "35");

        case 36:
            return getSumForAccounts(iCreditMinusDebetSum, "36");

        case 37:
            return getSumForAccounts(iDebetMinusCreditSum, "37");

        case 38:
            return getSumForAccounts(iCreditMinusDebetSum, "38");

        case 39:
            return getSumForAccounts(iCreditMinusDebetSum, "39");

        case 40:
            return getSumForAccounts(iCreditMinusDebetSum, "40");

        case 41:
            return getSumForAccounts(iCreditMinusDebetSum, "41");

        case 42:
            return getSumForAccounts(iCreditMinusDebetSum, "42");

        // F. Ingående moms
        case 48:
            return getSumForAccounts(iDebetMinusCreditSum, "48");

        // G. Moms att betala eller få tillbaka.
        case 49:
            BigDecimal iSum = new  BigDecimal(0);

            iSum = iSum.add(getValueForGroup(10));
            iSum = iSum.add(getValueForGroup(11));
            iSum = iSum.add(getValueForGroup(12));

            iSum = iSum.add(getValueForGroup(30));
            iSum = iSum.add(getValueForGroup(31));
            iSum = iSum.add(getValueForGroup(32));

            iSum = iSum.add(getValueForGroup(60));
            iSum = iSum.add(getValueForGroup(61));
            iSum = iSum.add(getValueForGroup(62));

            iSum = iSum.subtract(getValueForGroup(48));

            return iSum;

        // H. Importmoms
        case 50:
            return getSumForAccounts(iDebetMinusCreditSum, "50_25", "50_12", "50_6");

       // I. Utgående moms på import i ruta 50
        case 60:
            return getSumForAccounts(iCreditMinusDebetSum, "60");

        case 61:
            return getSumForAccounts(iCreditMinusDebetSum, "61");

        case 62:
            return getSumForAccounts(iCreditMinusDebetSum, "62");

        }
        return new BigDecimal(0);
    }

    /**
     * @return SSDefaultTableModel
     */
    @Override
    protected SSDefaultTableModel getModel() {
        addParameter("dateFrom", SSDateUtil.toDate(iDateFrom));
        addParameter("dateTo", SSDateUtil.toDate(iDateTo));

        iPrinter = new SSVATReportRowPrinter();
        iPrinter.generateReport();

        addParameter("Report", iPrinter.getReport());
        addParameter("Parameters", iPrinter.getParameters());

        iDataSource = new SSDefaultJasperDataSource(iPrinter.getModel());

        SSDefaultTableModel<String> iModel = new SSDefaultTableModel<>() {

            @Override
            public Class<?> getType() {
                return String.class;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                String iNumber = getObject(rowIndex);

                Object value = null;

                switch (columnIndex) {
                case 0:
                    value = iNumber;
                    break;

                case 1:
                    value = SSBundle.getBundle().getString(
                            "vatreport2015.group." + iNumber);
                    break;

                case 2:
                    iPrinter.setGroup(iNumber.charAt(0));

                    iDataSource.reset();

                    value = iDataSource;
                    break;

                }
                return value;
            }
        };

        iModel.addColumn("group.number");
        iModel.addColumn("group.description");
        iModel.addColumn("group.rows");

        iModel.add("A");
        iModel.add("B");
        iModel.add("C");
        iModel.add("D");
        iModel.add("H");
        iModel.add("I");
        iModel.add("E");
        iModel.add("F");
        iModel.add("G");

        return iModel;
    }

    private class SSVATReportRowPrinter extends SSPrinter {

        private SSDefaultTableModel<Integer> iModel;

        /**
         *
         */
        public SSVATReportRowPrinter() {
            setMargins(0, 0, 0, 0);

            setDetail("vatreport2015.rows.jrxml");

            iModel = new SSDefaultTableModel<>() {

                @Override
                public Class<?> getType() {
                    return Integer.class;
                }

                public Object getValueAt(int rowIndex, int columnIndex) {
                    Object value = null;

                    Integer iNumber = getObject(rowIndex);

                    switch (columnIndex) {
                    case 0:
                        value = iNumber;
                        break;

                    case 1:
                        value = SSBundle.getBundle().getString(
                                "vatreport2015.group." + iNumber);
                        break;

                    case 2:
                        value = getVATCodesForGroup(iNumber);
                        break;

                    case 3:
                        BigDecimal groupValue = getValueForGroup(iNumber);
 //                       LOG.info("VAT report group {} value: {}", iNumber, groupValue);
                        value = groupValue.setScale(0, RoundingMode.DOWN);
                        break;
                    }

                    return value;
                }
            };

            iModel.addColumn("group.number");
            iModel.addColumn("group.description");
            iModel.addColumn("group.vatcodes");
            iModel.addColumn("group.value");
        }

        /**
         * Gets the data model for this report
         *
         * @return SSDefaultTableModel
         */
        @Override
        protected SSDefaultTableModel getModel() {
            return iModel;
        }

        /**
         * Gets the title for this report
         *
         * @return The title
         */
        @Override
        public String getTitle() {
            return null;
        }

        /**
         *
         * @param iNumber
         */
        public void setGroup(char iNumber) {
            List<Integer> iObjects = new LinkedList<>();

            switch (iNumber) {
            case 'A':
                iObjects.add(5);
                iObjects.add(6);
                iObjects.add(7);
                iObjects.add(8);
                break;

            case 'B':
                iObjects.add(10);
                iObjects.add(11);
                iObjects.add(12);
                break;

            case 'C':
                iObjects.add(20);
                iObjects.add(21);
                iObjects.add(22);
                iObjects.add(23);
                iObjects.add(24);
                break;

            case 'D':
                iObjects.add(30);
                iObjects.add(31);
                iObjects.add(32);
                break;

            case 'E':
                iObjects.add(35);
                iObjects.add(36);
                iObjects.add(37);
                iObjects.add(38);
                iObjects.add(39);
                iObjects.add(40);
                iObjects.add(41);
                iObjects.add(42);
                break;

            case 'F':
                iObjects.add(48);
                break;

            case 'G':
                iObjects.add(49);
                break;

            case 'H':
                iObjects.add(50);
                break;

            case 'I':
                iObjects.add(60);
                iObjects.add(61);
                iObjects.add(62);
                break;

            }
            iModel.setObjects(iObjects);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append(
                    "se.swedsoft.bookkeeping.print.report.SSVATReport2015Printer.SSVATReportRowPrinter");
            sb.append("{iModel=").append(iModel);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     *
     * @param iSums
     * @param iVATCodes
     * @return
     */
    private BigDecimal getSumForAccounts(Map<SSAccount, BigDecimal> iSums, String... iVATCodes) {
        BigDecimal iSum = new BigDecimal(0);

        for (SSAccount iAccount : iAccounts) {

            if (hasVATCode(iAccount, iVATCodes)) {
                BigDecimal iSumForAccount = iSums.get(iAccount);

                if (iSumForAccount != null) {
                    iSum = iSum.add(iSumForAccount.setScale(0, RoundingMode.DOWN));
                }
            }

        }
        return iSum;
    }

    /**
     *
     * @param iAccount
     * @param iVATCodes
     * @return
     */
    private boolean hasVATCode(SSAccount iAccount, String... iVATCodes) {

        String iVATCodeForAccount = iAccount.getVATCode();

        for (String iVATCode : iVATCodes) {

            if (iVATCode.equals(iVATCodeForAccount)) {

                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.report.SSVATReport2015Printer");
        sb.append("{iAccountingYear=").append(iAccountingYear);
        sb.append(", iAccounts=").append(iAccounts);
        sb.append(", iAccountsByVatCode=").append(iAccountsByVatCode);
        sb.append(", iCreditMinusDebetSum=").append(iCreditMinusDebetSum);
        sb.append(", iDataSource=").append(iDataSource);
        sb.append(", iDateFrom=").append(iDateFrom);
        sb.append(", iDateTo=").append(iDateTo);
        sb.append(", iDebetMinusCreditSum=").append(iDebetMinusCreditSum);
        sb.append(", iPrinter=").append(iPrinter);
        sb.append('}');
        return sb.toString();
    }
}
