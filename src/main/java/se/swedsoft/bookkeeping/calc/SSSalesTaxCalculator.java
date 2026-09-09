package se.swedsoft.bookkeeping.calc;


import se.swedsoft.bookkeeping.calc.math.SSAccountMath;
import se.swedsoft.bookkeeping.calc.math.SSVoucherMath;
import se.swedsoft.bookkeeping.calc.util.SSCalculatorException;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-mar-09
 * Time: 10:35:03
 */
public class SSSalesTaxCalculator {    private static final Logger LOG = LoggerFactory.getLogger(SSSalesTaxCalculator.class);


    private SSNewAccountingYear iAccountingYear;

    private LocalDate iDateFrom;

    private LocalDate iDateTo;

    private List<SSVATReportGroup> iReportGroups;

    private List<SSVATControlGroup> iControlGroups;

    /**
     *
     * @param pAccountingYear
     * @param pDateFrom
     * @param pDateTo
     */
    public SSSalesTaxCalculator(SSNewAccountingYear pAccountingYear, LocalDate pDateFrom,
            LocalDate pDateTo) {
        iAccountingYear = pAccountingYear;
        iDateFrom = pDateFrom;
        iDateTo = pDateTo;
    }

    /**
     * @deprecated Use {@link #SSSalesTaxCalculator(SSNewAccountingYear, LocalDate, LocalDate)}.
     */
    @Deprecated
    public SSSalesTaxCalculator(SSNewAccountingYear pAccountingYear, Date pDateFrom, Date pDateTo) {
        this(pAccountingYear, SSDateUtil.toLocalDate(pDateFrom), SSDateUtil.toLocalDate(pDateTo));
    }

    /**
     *
     * @throws SSCalculatorException
     */
    public void calculate() throws SSCalculatorException {
        iReportGroups = createReportGroups();
        iControlGroups = createControlGroups();

        // Get all vouchers
        List<SSVoucher> iVouchers = SSVoucherMath.getVouchers(
                iAccountingYear.getVouchers(), iDateFrom, iDateTo);

        // LOG.info("Momsrapport, verifikationer:");
        // for(SSVoucher iVoucher : iVouchers){
        // LOG.info(iVoucher);
        // }

        Map<SSAccount, BigDecimal> creditMinusDebetSum = SSVoucherMath.getCreditMinusDebetSum(
                iVouchers);
        Map<SSAccount, BigDecimal> debetMinusCreditSum = SSVoucherMath.getDebetMinusCreditSum(
                iVouchers);

        for (SSVATReportGroup iGroup : iReportGroups) {
            BigDecimal iSum = null;

            switch (iGroup.getGroup2()) {

                case 5: //11:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "5_25", "5_12", "5_6").add(
                            SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "21"));
                    break;

                case 6: //12:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "6_25", "6_12", "6_6");
                    break;

                case 7: //13:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "7_25", "7_12", "7_6");
                    break;

                case 8: //14:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "8");
                    break;

                case 10: //15:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "10");
                    break;

                case 11: //21:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "11");
                    break;

                case 12: //22:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "12");
                    break;

                case 20: //23:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "20");
                    break;

                case 21: //24:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "21");
                    break;

                case 22: //25:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "22");
                    break;

                case 23: //26:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "23");
                    break;

                case 24: //30:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "24");
                    break;

                case 30: //31:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "30");
                    break;

                case 31: //32:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "31");
                    break;

                case 32: //33:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "32");
                    break;

                case 35: //34:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "35");
                    break;

                case 36: //35:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "36");
                    break;

                case 37: //36:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "37");
                    break;

                case 38: //37:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "38");
                    break;

                case 39:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "39");
                    break;

                case 40:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "40");
                    break;

                case 41:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "41");
                    break;

                case 42:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "42");
                    break;

                case 48:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(debetMinusCreditSum, "48");
                    break;

                case 50:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "50_25", "50_12", "50_6");
                    break;

                case 60:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "60");
                    break;

                case 61:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "61");
                    break;

                case 62:
                    iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "62");
                    break;
            }
            iGroup.setSum(iSum);
        }

        for (SSVATControlGroup iGroup : iControlGroups) {
            BigDecimal iSum = null;

            switch (iGroup.getGroup1()) {
            case 1:
                iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "5_25");
                break;

            case 2:
                iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "5_12");
                break;

            case 3:
                iSum = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "5_6");
                break;
            }

            BigDecimal iReported = null;

            switch (iGroup.getGroup1()) {
            case 1:
                iReported = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "10");
                break;

            case 2:
                iReported = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "11");
                break;

            case 3:
                iReported = SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "12");
                break;
            }

            iGroup.setSum(iSum);
            iGroup.setReported(iReported);
        }

    }

    /**
     *
     * @return
     */
    public List<SSVATReportGroup> getReportGroups() {
        return iReportGroups;
    }

    /**
     *
     * @return
     */
    public List<SSVATControlGroup> getControlGroups() {
        return iControlGroups;
    }

    /**
     *
     * @return
     */
    private List<SSVATReportGroup> createReportGroups() {
        List<SSVATReportGroup> iReportGroups = new LinkedList<>();

        // Add the groups
        iReportGroups.add(new SSVATReportGroup(1, 5)); //(1, 11));
        iReportGroups.add(new SSVATReportGroup(1, 6)); //(1, 12));
        iReportGroups.add(new SSVATReportGroup(1, 7)); //(1, 13));
        iReportGroups.add(new SSVATReportGroup(1, 8)); //(1, 14));

        iReportGroups.add(new SSVATReportGroup(1, 10)); //(1, 15));
        iReportGroups.add(new SSVATReportGroup(1, 11));
        iReportGroups.add(new SSVATReportGroup(1, 12));

        iReportGroups.add(new SSVATReportGroup(2, 20)); //(2, 21));
        iReportGroups.add(new SSVATReportGroup(2, 21)); //(2, 22));
        iReportGroups.add(new SSVATReportGroup(2, 22)); //(2, 23));
        iReportGroups.add(new SSVATReportGroup(2, 23)); //(2, 24));
        iReportGroups.add(new SSVATReportGroup(2, 24)); //(2, 25));

        iReportGroups.add(new SSVATReportGroup(3, 30)); //(3, 30));
        iReportGroups.add(new SSVATReportGroup(3, 31)); //(3, 31));
        iReportGroups.add(new SSVATReportGroup(3, 32)); //(4, 32));

        iReportGroups.add(new SSVATReportGroup(4, 35));
        iReportGroups.add(new SSVATReportGroup(4, 36));
        iReportGroups.add(new SSVATReportGroup(4, 37));
        iReportGroups.add(new SSVATReportGroup(4, 38));
        iReportGroups.add(new SSVATReportGroup(4, 39)); //(4, 33));
        iReportGroups.add(new SSVATReportGroup(4, 40)); //(4, 34));
        iReportGroups.add(new SSVATReportGroup(4, 41));
        iReportGroups.add(new SSVATReportGroup(4, 42));

        iReportGroups.add(new SSVATReportGroup(4, 48));

        iReportGroups.add(new SSVATReportGroup(5, 50));
        iReportGroups.add(new SSVATReportGroup(5, 60));
        iReportGroups.add(new SSVATReportGroup(5, 61));
        iReportGroups.add(new SSVATReportGroup(5, 62));

        return iReportGroups;
    }

    /**
     *
     * @return
     */
    private List<SSVATControlGroup> createControlGroups() {
        List<SSVATControlGroup> iControlGroups = new LinkedList<>();

        // Add the three groups
        iControlGroups.add(new SSVATControlGroup(1));
        iControlGroups.add(new SSVATControlGroup(2));
        iControlGroups.add(new SSVATControlGroup(3));

        return iControlGroups;
    }

    /**
     *
     */
    public static class SSVATReportGroup {

        private int iGroup1;

        private int iGroup2;

        private BigDecimal iSum;

        /**
         * Default constructor.
         * @param pGroup1
         * @param pGroup2
         */
        public SSVATReportGroup(int pGroup1, int pGroup2) {
            iGroup1 = pGroup1;
            iGroup2 = pGroup2;
            iSum = null;
        }

        public int getGroup1() {
            return iGroup1;
        }

        public void setGroup1(int iGroup1) {
            this.iGroup1 = iGroup1;
        }

        public int getGroup2() {
            return iGroup2;
        }

        public void setGroup2(int iGroup2) {
            this.iGroup2 = iGroup2;
        }

        /**
         *
         * @return
         */
        public BigDecimal getSum() {
            return iSum;
        }

        /**
         *
         * @param iSum
         */
        public void setSum(BigDecimal iSum) {
            this.iSum = iSum;
        }

        /**
         * @return String
         */
        public String getGroup1Description() {
            return SSBundle.getBundle().getString("vatreport.group.1." + iGroup1);
        }

        /**
         * @return String
         */
        public String getGroup2Description() {
            return SSBundle.getBundle().getString("vatreport.group.2." + iGroup2);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append("se.swedsoft.bookkeeping.calc.SSSalesTaxCalculator.SSVATReportGroup");
            sb.append("{iGroup1=").append(iGroup1);
            sb.append(", iGroup2=").append(iGroup2);
            sb.append(", iSum=").append(iSum);
            sb.append('}');
            return sb.toString();
        }
    }


    public static class SSVATControlGroup {

        private BigDecimal iSum;

        private BigDecimal iReported;

        private int iGroup1;

        /**
         * Default constructor.
         * @param pGroup1
         */
        public SSVATControlGroup(int pGroup1) {
            iGroup1 = pGroup1;
            iSum = null;
        }

        /**
         * @return
         */
        public int getGroup1() {
            return iGroup1;
        }

        /**
         *
         * @param iGroup1
         */
        public void setGroup1(int iGroup1) {
            this.iGroup1 = iGroup1;
        }

        /**
         *
         * @return
         */
        public BigDecimal getSum() {
            return iSum;
        }

        /**
         *
         * @param iSum
         */
        public void setSum(BigDecimal iSum) {
            this.iSum = iSum;
        }

        public BigDecimal getReported() {
            return iReported;
        }

        public void setReported(BigDecimal iReported) {
            this.iReported = iReported;
        }

        /**
         * @return String
         */
        public String getDescription() {
            return SSBundle.getBundle().getString("vatcontrolreport.group.1." + iGroup1);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append(
                    "se.swedsoft.bookkeeping.calc.SSSalesTaxCalculator.SSVATControlGroup");
            sb.append("{iGroup1=").append(iGroup1);
            sb.append(", iReported=").append(iReported);
            sb.append(", iSum=").append(iSum);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.calc.SSSalesTaxCalculator");
        sb.append("{iAccountingYear=").append(iAccountingYear);
        sb.append(", iControlGroups=").append(iControlGroups);
        sb.append(", iDateFrom=").append(iDateFrom);
        sb.append(", iDateTo=").append(iDateTo);
        sb.append(", iReportGroups=").append(iReportGroups);
        sb.append('}');
        return sb.toString();
    }
}
