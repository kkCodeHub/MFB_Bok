package se.swedsoft.bookkeeping.print.report;


import se.swedsoft.bookkeeping.calc.math.SSCreditInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSDateMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSMonth;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.SSPrinter;
import se.swedsoft.bookkeeping.print.util.SSDefaultJasperDataSource;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


/**
 * Date: 2006-mar-03
 * Time: 15:32:42
 */
public class SSCustomerRevenuePrinter extends SSPrinter {

    private SSMonthlyDistributionPrinter iPrinter;

    private SSDefaultJasperDataSource iDataSource;

    private List<SSCustomer> iCustomers;

    private Date  iDateFrom;

    private Date  iDateTo;

    Map<String, Map<SSMonth, BigDecimal>> iCustomerRevenue;

    /**
     *
     * @param pCustomers
     * @param pFrom
     * @param pTo
     */
    public SSCustomerRevenuePrinter(List<SSCustomer> pCustomers, Date pFrom, Date pTo) {
        iCustomers = pCustomers;
        iDateFrom = pFrom;
        iDateTo = pTo;
        calculate();
        setPageHeader("header_period.jrxml");
        setColumnHeader("customerrevenue.jrxml");
        setDetail("customerrevenue.jrxml");
    }

    /**
     * Gets the title file for this repport
     *
     * @return
     */
    @Override
    public String getTitle() {
        return SSBundle.getBundle().getString("customerrevenue.title");
    }

    /**
     * @return SSDefaultTableModel
     */
    @Override
    protected SSDefaultTableModel getModel() {
        addParameter("dateFrom", iDateFrom);
        addParameter("dateTo", iDateTo);

        iPrinter = new SSMonthlyDistributionPrinter(iDateFrom, iDateTo);
        iPrinter.generateReport();

        addParameter("Report", iPrinter.getReport());
        addParameter("Parameters", iPrinter.getParameters());

        iDataSource = new SSDefaultJasperDataSource(iPrinter.getModel());

        SSDefaultTableModel<SSCustomer> iModel = new SSDefaultTableModel<>() {
            @Override
            public Class<?> getType() {
                return SSCustomer.class;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                Object value = null;

                SSCustomer iCustomer = getObject(rowIndex);

                switch (columnIndex) {
                case 0:
                    value = iCustomer.getNumber();
                    break;

                case 1:
                    value = iCustomer.getName();
                    break;

                case 2:
                    iPrinter.setCustomer(iCustomer,
                            iCustomerRevenue.get(iCustomer.getNumber()));
                    iDataSource.reset();
                    value = iDataSource;
                    break;
                }

                return value;
            }
        };

        iModel.addColumn("customer.number");
        iModel.addColumn("customer.description");
        iModel.addColumn("month.data");

        iModel.setObjects(iCustomers);

        return iModel;
    }

    private void calculate() {
        iCustomerRevenue = new HashMap<>();
        List<SSInvoice> iInvoices = SSDB.getInstance().getInvoices();
        LocalDate localFrom = SSDateUtil.toLocalDate(iDateFrom);
        LocalDate localTo = SSDateUtil.toLocalDate(iDateTo);

        for (SSInvoice iInvoice : iInvoices) {
            LocalDate invoiceLocalDate = iInvoice.getLocalDate();
            if (invoiceLocalDate != null && localFrom != null && localTo != null
                    && !invoiceLocalDate.isBefore(localFrom) && !invoiceLocalDate.isAfter(localTo)) {
                LocalDate monthStart = invoiceLocalDate.withDayOfMonth(1);
                LocalDate monthEnd = invoiceLocalDate.withDayOfMonth(invoiceLocalDate.lengthOfMonth());
                SSMonth iMonth = new SSMonth(monthStart, monthEnd);

                if (iInvoice.getCustomerNr() != null
                        && SSInvoiceMath.getNetSum(iInvoice) != null) {
                    BigDecimal iSum = SSInvoiceMath.convertToLocal(iInvoice,
                            SSInvoiceMath.getNetSum(iInvoice));
                    Map<SSMonth, BigDecimal> iRevenueInMonth;

                    if (iCustomerRevenue.containsKey(iInvoice.getCustomerNr())) {
                        iRevenueInMonth = iCustomerRevenue.get(iInvoice.getCustomerNr());
                        if (iRevenueInMonth.containsKey(iMonth)) {
                            iRevenueInMonth.put(iMonth,
                                    iRevenueInMonth.get(iMonth).add(iSum));
                        } else {
                            iRevenueInMonth.put(iMonth, iSum);
                        }
                    } else {
                        iRevenueInMonth = new HashMap<>();
                        iRevenueInMonth.put(iMonth, iSum);
                    }
                    iCustomerRevenue.put(iInvoice.getCustomerNr(), iRevenueInMonth);
                }
            }
        }

        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            LocalDate creditLocalDate = iCreditInvoice.getLocalDate();
            if (creditLocalDate != null && localFrom != null && localTo != null
                    && !creditLocalDate.isBefore(localFrom) && !creditLocalDate.isAfter(localTo)) {
                LocalDate monthStart = creditLocalDate.withDayOfMonth(1);
                LocalDate monthEnd = creditLocalDate.withDayOfMonth(creditLocalDate.lengthOfMonth());
                SSMonth iMonth = new SSMonth(monthStart, monthEnd);

                if (iCreditInvoice.getCustomerNr() != null
                        && SSCreditInvoiceMath.getNetSum(iCreditInvoice) != null) {
                    BigDecimal iSum = SSCreditInvoiceMath.convertToLocal(iCreditInvoice,
                            SSCreditInvoiceMath.getNetSum(iCreditInvoice));
                    Map<SSMonth, BigDecimal> iRevenueInMonth;

                    if (iCustomerRevenue.containsKey(iCreditInvoice.getCustomerNr())) {
                        iRevenueInMonth = iCustomerRevenue.get(
                                iCreditInvoice.getCustomerNr());
                        if (iRevenueInMonth.containsKey(iMonth)) {
                            iRevenueInMonth.put(iMonth,
                                    iRevenueInMonth.get(iMonth).subtract(iSum));
                        } else {
                            iRevenueInMonth.put(iMonth, iSum.negate());
                        }
                    } else {
                        iRevenueInMonth = new HashMap<>();
                        iRevenueInMonth.put(iMonth, iSum.negate());
                    }
                    iCustomerRevenue.put(iCreditInvoice.getCustomerNr(), iRevenueInMonth);
                }
            }
        }
    }

    private class SSMonthlyDistributionPrinter extends SSPrinter {

        private SSDefaultTableModel<SSMonth> iModel;

        private SSCustomer iCustomer;
        private Date  iFrom;

        private Date  iTo;

        private LocalDate iLocalFrom;

        private LocalDate iLocalTo;

        Map<SSMonth, BigDecimal> iRevenue;

        /**
         *
         * @param pFrom
         * @param pTo
         */
        public SSMonthlyDistributionPrinter(Date pFrom, Date pTo) {
            iFrom = pFrom;
            iTo = pTo;
            iLocalFrom = SSDateUtil.toLocalDate(pFrom);
            iLocalTo = SSDateUtil.toLocalDate(pTo);
            setMargins(0, 0, 0, 0);

            setDetail("customerrevenue.monthly.jrxml");
            setSummary("customerrevenue.monthly.jrxml");

            iModel = new SSDefaultTableModel<>(
                    SSMonth.splitYearIntoMonths(iLocalFrom, iLocalTo)) {

                @Override
                public Class<?> getType() {
                    return SSMonth.class;
                }

                public Object getValueAt(int rowIndex, int columnIndex) {
                    Object value = null;

                    SSMonth iMonth = getObject(rowIndex);

                    switch (columnIndex) {
                    case 0:
                        value = iMonth.toString();
                        break;

                    case 1:
                        value = iMonth.getName();
                        break;

                    case 2:
                        if (iCustomer != null && iRevenue.containsKey(iMonth)) {
                            value = iRevenue.get(iMonth);
                        } else {
                            value = new BigDecimal(0);
                        }
                        break;

                    case 3:
                        value = iMonth.isBetween(iLocalFrom, iLocalTo);
                        break;
                    }

                    return value;
                }
            };

            iModel.addColumn("month.date");
            iModel.addColumn("month.description");
            iModel.addColumn("month.value");
            iModel.addColumn("month.visible");
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
         * @param pCustomer
         * @param iMap
         */
        public void setCustomer(SSCustomer pCustomer, Map<SSMonth, BigDecimal> iMap) {
            iCustomer = pCustomer;
            iRevenue = iMap;
            if (iRevenue == null) {
                iRevenue = new HashMap<>();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append(
                    "se.swedsoft.bookkeeping.print.report.SSCustomerRevenuePrinter.SSMonthlyDistributionPrinter");
            sb.append("{iCustomer=").append(iCustomer);
            sb.append(", iFrom=").append(iFrom);
            sb.append(", iModel=").append(iModel);
            sb.append(", iRevenue=").append(iRevenue);
            sb.append(", iTo=").append(iTo);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.report.SSCustomerRevenuePrinter");
        sb.append("{iCustomerRevenue=").append(iCustomerRevenue);
        sb.append(", iCustomers=").append(iCustomers);
        sb.append(", iDataSource=").append(iDataSource);
        sb.append(", iDateFrom=").append(iDateFrom);
        sb.append(", iDateTo=").append(iDateTo);
        sb.append(", iPrinter=").append(iPrinter);
        sb.append('}');
        return sb.toString();
    }
}
