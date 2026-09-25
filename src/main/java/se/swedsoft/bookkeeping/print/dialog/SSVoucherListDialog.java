package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.calc.math.SSVoucherMath;
import se.swedsoft.bookkeeping.calc.util.SSFilter;
import se.swedsoft.bookkeeping.calc.util.SSFilterFactory;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.SQLException;


/**
 * $Id$
 *
 */
public class SSVoucherListDialog extends SSDialog {
    static final String SERIES_SYSTEM = "System";
    static final String SERIES_CUSTOM = "Egna";

    public enum SSVoucherRange {
        ALL_VOUCHERS, BETWEEN_NUMBER, BETWEEN_DATE
    }

    private JPanel iPanel;

    private SSDateChooser iFromDate;

    private SSDateChooser iToDate;

    private JComboBox<Integer> iToVoucher;

    private JComboBox<Integer> iFromVoucher;

    private JComboBox<String> iSeriesFilter;

    private SSButtonPanel iButtonPanel;
    private final List<SSVoucher> iYearVouchers;

    /**
     *
     * @param iMainFrame
     */
    public SSVoucherListDialog(SSMainFrame iMainFrame) {
        super(iMainFrame, SSBundle.getBundle().getString("voucherlistreport.dialog.title"));

        List<SSVoucher> iVouchers = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getVouchers();
        iYearVouchers = new ArrayList<>(iVouchers);

        setPanel(iPanel);

        iButtonPanel.addOkActionListener(e -> {
            if (!validateDateInterval()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Från-datum måste vara tidigare än eller samma som till-datum.",
                        "Validering",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            closeDialog(JOptionPane.OK_OPTION);
        });

        iButtonPanel.addCancelActionListener(e -> closeDialog(JOptionPane.CANCEL_OPTION));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        initializeSeriesFilter(iVouchers);
        updateNumberRangeDefaults();
        iSeriesFilter.addActionListener(e -> updateNumberRangeDefaults());
        initializeDateDefaults();

        iFromVoucher.setEnabled(true);
        iToVoucher.setEnabled(true);
        iFromDate.setEnabled(true);
        iToDate.setEnabled(true);
    }

    private void initializeSeriesFilter(List<SSVoucher> vouchers) {
        List<String> options = buildSeriesOptions(vouchers,
                SSBundle.getBundle().getString("voucherlistreport.dialog.filter.all"));

        iSeriesFilter.setModel(new DefaultComboBoxModel<>(options.toArray(new String[0])));
        iSeriesFilter.setSelectedIndex(0);
    }

    private void updateNumberRangeDefaults() {
        String selectedSeriesFilter = (String) iSeriesFilter.getSelectedItem();
        List<SSVoucher> seriesVouchers = applySeriesFilter(new ArrayList<>(iYearVouchers), selectedSeriesFilter);

        int maxNumber = resolveMaxNumber(seriesVouchers);
        Integer[] values = new Integer[maxNumber];
        for (int i = 1; i <= maxNumber; i++) {
            values[i - 1] = i;
        }

        iFromVoucher.setModel(new DefaultComboBoxModel<>(values));
        iToVoucher.setModel(new DefaultComboBoxModel<>(values));
        iFromVoucher.setSelectedItem(1);
        iToVoucher.setSelectedItem(maxNumber);
    }

    private void initializeDateDefaults() {
        SSNewAccountingYear currentYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
        if (currentYear == null) {
            return;
        }
        if (currentYear.getLocalFrom() != null) {
            iFromDate.setLocalDate(currentYear.getLocalFrom());
        }
        if (currentYear.getLocalTo() != null) {
            iToDate.setLocalDate(currentYear.getLocalTo());
        }
    }

    private boolean validateDateInterval() {
        return isValidDateInterval(iFromDate.getLocalDate(), iToDate.getLocalDate());
    }

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @return
     */
    public SSVoucherRange getVoucherRange() {
        return SSVoucherRange.ALL_VOUCHERS;
    }

    /**
     *
     * @param pDateFrom
     */
    public void setDateFrom(Date pDateFrom) {
        iFromDate.setLocalDate(SSDateUtil.toLocalDate(pDateFrom));
    }

    /**
     *
     * @param pDateTo
     */
    public void setDateTo(Date pDateTo) {
        iToDate.setLocalDate(SSDateUtil.toLocalDate(pDateTo));
    }

    /**
     * @param pDateFrom the start date
     */
    public void setLocalDateFrom(LocalDate pDateFrom) {
        iFromDate.setLocalDate(pDateFrom);
    }

    /**
     * @param pDateTo the end date
     */
    public void setLocalDateTo(LocalDate pDateTo) {
        iToDate.setLocalDate(pDateTo);
    }

    /**
     * @return the start date as a {@link LocalDate}
     */
    public LocalDate getLocalDateFrom() {
        return iFromDate.getLocalDate();
    }

    /**
     * @return the end date as a {@link LocalDate}
     */
    public LocalDate getLocalDateTo() {
        return iToDate.getLocalDate();
    }

    /**
     *
     * @return
     */
    public Integer getNumberFrom() {
        Integer iSelected = (Integer) iFromVoucher.getSelectedItem();
        return iSelected == null ? 1 : iSelected;
    }

    /**
     *
     * @return
     */
    public Integer getNumberTo() {
        Integer iSelected = (Integer) iToVoucher.getSelectedItem();
        return iSelected == null ? 1 : iSelected;
    }

    /**
     *
     * @return
     */
    public Date getDateFrom() {
        return SSDateUtil.toDate(iFromDate.getLocalDate());
    }

    /**
     *
     * @return
     */
    public Date getDateTo() {
        return SSDateUtil.toDate(iToDate.getLocalDate());
    }

    /**
     *
     * @return
     */
    public boolean isNumberSelected() {
        return true;
    }

    /**
     *
     * @return
     */
    public boolean isDateSelected() {
        return true;
    }

    /**
     * Returns the invoices to print depending on the user selections
     *
     * @return
     */
    public List<SSVoucher> getElementsToPrint() {

        List<SSVoucher> iVouchers = se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers();
        final String selectedSeriesFilter = (String) iSeriesFilter.getSelectedItem();
        iVouchers = applySeriesFilter(iVouchers, selectedSeriesFilter);

        // Filter by number
        final Integer iNumberFrom = getNumberFrom();
        final Integer iNumberTo = getNumberTo();

        iVouchers = SSFilterFactory.doFilter(iVouchers, new SSFilter<>() {
            public boolean applyFilter(SSVoucher iObject) {
                Integer iNumber = iObject.getNumber();

                return iNumber >= iNumberFrom && iNumber <= iNumberTo;
            }
        });

        // Filter by date
        final LocalDate iDateFrom = iFromDate.getLocalDate();
        final LocalDate iDateTo = iToDate.getLocalDate();

        iVouchers = SSFilterFactory.doFilter(iVouchers, new SSFilter<>() {
            public boolean applyFilter(SSVoucher iInvoice) {
                return SSVoucherMath.inPeriod(iInvoice, iDateFrom, iDateTo);
            }
        });

        return iVouchers;
    }

    private List<SSVoucher> applySeriesFilter(List<SSVoucher> vouchers, String selectedSeriesFilter) {
        final String allOption = SSBundle.getBundle().getString("voucherlistreport.dialog.filter.all");
        if (allOption.equals(selectedSeriesFilter)) {
            return vouchers;
        }

        if (SERIES_SYSTEM.equals(selectedSeriesFilter) || SERIES_CUSTOM.equals(selectedSeriesFilter)) {
            final Set<String> systemSeries = new HashSet<>();
            final Set<String> customSeries = new HashSet<>();
            loadSeriesClassification(systemSeries, customSeries);
            return filterBySeriesSelection(vouchers, selectedSeriesFilter, allOption, systemSeries, customSeries);
        }

        final String selectedSeries = normalizeSeries(selectedSeriesFilter);
        return SSFilterFactory.doFilter(vouchers, new SSFilter<>() {
            public boolean applyFilter(SSVoucher voucher) {
                return normalizeSeries(voucher == null ? null : voucher.getSeries()).equals(selectedSeries);
            }
        });
    }

    static List<String> buildSeriesOptions(List<SSVoucher> vouchers, String allOptionLabel) {
        List<String> options = new ArrayList<>();
        options.add(allOptionLabel);
        options.add(SERIES_SYSTEM);
        options.add(SERIES_CUSTOM);

        Set<String> usedSeries = new LinkedHashSet<>();
        for (SSVoucher voucher : vouchers) {
            if (voucher == null || voucher.getSeries() == null) {
                continue;
            }
            String series = voucher.getSeries().trim().toUpperCase();
            if (!series.isEmpty()) {
                usedSeries.add(series);
            }
        }

        List<String> sortedSeries = new ArrayList<>(usedSeries);
        sortedSeries.sort(Comparator.naturalOrder());
        options.addAll(sortedSeries);
        return options;
    }

    static List<SSVoucher> filterBySeriesSelection(List<SSVoucher> vouchers,
                                                   String selectedSeriesFilter,
                                                   String allOptionLabel,
                                                   Set<String> systemSeries,
                                                   Set<String> customSeries) {
        if (allOptionLabel.equals(selectedSeriesFilter)) {
            return vouchers;
        }
        if (SERIES_SYSTEM.equals(selectedSeriesFilter) || SERIES_CUSTOM.equals(selectedSeriesFilter)) {
            final boolean wantsSystem = SERIES_SYSTEM.equals(selectedSeriesFilter);
            return SSFilterFactory.doFilter(vouchers, new SSFilter<>() {
                public boolean applyFilter(SSVoucher voucher) {
                    String series = normalizeSeriesStatic(voucher == null ? null : voucher.getSeries());
                    return wantsSystem ? systemSeries.contains(series) : customSeries.contains(series);
                }
            });
        }
        final String selectedSeries = normalizeSeriesStatic(selectedSeriesFilter);
        return SSFilterFactory.doFilter(vouchers, new SSFilter<>() {
            public boolean applyFilter(SSVoucher voucher) {
                return normalizeSeriesStatic(voucher == null ? null : voucher.getSeries()).equals(selectedSeries);
            }
        });
    }

    static int resolveMaxNumber(List<SSVoucher> seriesVouchers) {
        int maxNumber = 1;
        for (SSVoucher voucher : seriesVouchers) {
            if (voucher == null) {
                continue;
            }
            maxNumber = Math.max(maxNumber, voucher.getNumber());
        }
        return maxNumber;
    }

    static boolean isValidDateInterval(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return true;
        }
        return !from.isAfter(to);
    }

    private void loadSeriesClassification(Set<String> systemSeries, Set<String> customSeries) {
        SSNewAccountingYear currentYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
        if (currentYear == null || currentYear.getId() == null) {
            return;
        }
        try {
            List<Map<String, Object>> mappings = Repositories.yearVoucherEventSeriesMaps()
                    .findByYearOrderedById(currentYear.getId());
            for (Map<String, Object> mapping : mappings) {
                String series = normalizeSeries(mapping.get("series_code"));
                if (series.isEmpty()) {
                    continue;
                }
                boolean isCustom = Boolean.TRUE.equals(mapping.getOrDefault("is_custom", false));
                if (isCustom) {
                    customSeries.add(series);
                } else {
                    systemSeries.add(series);
                }
            }
        } catch (SQLException ignored) {
            // If mapping lookup fails, the filter result for System/Egna becomes empty.
        }
    }

    private String normalizeSeries(Object seriesValue) {
        return normalizeSeriesStatic(seriesValue);
    }

    private static String normalizeSeriesStatic(Object seriesValue) {
        if (seriesValue == null) {
            return "";
        }
        String series = String.valueOf(seriesValue).trim().toUpperCase();
        return series.length() == 1 ? series : "";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSVoucherListDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iFromDate=").append(iFromDate);
        sb.append(", iFromVoucher=").append(iFromVoucher);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iSeriesFilter=").append(iSeriesFilter);
        sb.append(", iToDate=").append(iToDate);
        sb.append(", iToVoucher=").append(iToVoucher);
        sb.append('}');
        return sb.toString();
    }
}
