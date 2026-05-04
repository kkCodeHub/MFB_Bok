package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Date: 2006-jan-27
 * Time: 11:48:46
 */
public class SSMonth  implements Serializable {

    // Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    private LocalDate iFrom;

    private LocalDate iTo;

    /**
     * @deprecated Use {@link #SSMonth(LocalDate)} instead.
     */
    @Deprecated
    public SSMonth(Date pFrom) {
        iFrom = SSDateUtil.toLocalDate(pFrom);
        iTo = null;
    }

    /**
     * @param pFrom the start date as a LocalDate
     */
    public SSMonth(LocalDate pFrom) {
        iFrom = pFrom;
        iTo = null;
    }

    /**
     * @deprecated Use {@link #SSMonth(LocalDate, LocalDate)} instead.
     */
    @Deprecated
    public SSMonth(Date pFrom, Date pTo) {
        iFrom = SSDateUtil.toLocalDate(pFrom);
        iTo = SSDateUtil.toLocalDate(pTo);
    }

    /**
     * @param pFrom the start date as a LocalDate
     * @param pTo the end date as a LocalDate
     */
    public SSMonth(LocalDate pFrom, LocalDate pTo) {
        iFrom = pFrom;
        iTo = pTo;
    }

    /**
     *
     * @return The date
     */
    @Deprecated
    public Date getDate() {
        return SSDateUtil.toDate(iFrom);
    }

    /**
     *
     * @return The date
     */
    @Deprecated
    public Date getFrom() {
        return SSDateUtil.toDate(iFrom);
    }

    /**
     * @return the from date as a LocalDate
     */
    public LocalDate getLocalFrom() {
        return iFrom;
    }

    /**
     *
     * @return The date
     */
    @Deprecated
    public Date getTo() {
        return SSDateUtil.toDate(iTo);
    }

    /**
     * @return the to date as a LocalDate
     */
    public LocalDate getLocalTo() {
        return iTo;
    }

    /**
     *
     * @param pFrom
     * @param pTo
     * @return boolean
     */
    @Deprecated
    public boolean isBetween(Date pFrom, Date pTo) {
        LocalDate localFrom = SSDateUtil.toLocalDate(pFrom);
        LocalDate localTo = SSDateUtil.toLocalDate(pTo);
        return !iFrom.isBefore(localFrom) && !iFrom.isAfter(localTo);
    }

    /**
     * @param pFrom the start date as a LocalDate
     * @param pTo the end date as a LocalDate
     * @return true if this month's from date is between pFrom and pTo (inclusive)
     */
    public boolean isBetween(LocalDate pFrom, LocalDate pTo) {
        return !iFrom.isBefore(pFrom) && !iFrom.isAfter(pTo);
    }

    public int hashCode() {
        if (iFrom == null) {
            return super.hashCode();
        }

        return iFrom.getYear() * 12 + iFrom.getMonthValue();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SSMonth) {
            return obj.hashCode() == hashCode();
        }
        return super.equals(obj);
    }

    public String toString() {
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);

        return format.format(SSDateUtil.toDate(iFrom)).substring(0, 7);
    }

    /**
     *
     * @return
     */
    public String getName() {
        int iMonth = iFrom.getMonthValue() - 1;
        int iYear = iFrom.getYear();

        DateFormatSymbols iSymbols = new DateFormatSymbols();

        return iSymbols.getMonths()[iMonth] + ", " + iYear;
    }

    /**
     *  Breaks a year into it's months
     * @param pYearData
     * @return
     */
    public static List<SSMonth> splitYearIntoMonths(SSNewAccountingYear pYearData) {
        return splitYearIntoMonths(pYearData.getLocalFrom(), pYearData.getLocalTo());
    }

    public boolean isDateInMonth(Date iDate) {
        LocalDate checkDate = SSDateUtil.toLocalDate(iDate);
        return checkDate != null
                && checkDate.getMonth() == iFrom.getMonth()
                && checkDate.getYear() == iFrom.getYear();
    }

    public boolean isDateInMonth(LocalDate iDate) {
        return iDate != null
                && iDate.getMonth() == iFrom.getMonth()
                && iDate.getYear() == iFrom.getYear();
    }

    /**
     * Breaks a year into its months.
     *
     * @param iFrom the start date
     * @param iTo the end date
     * @return List of months
     * @deprecated Use {@link #splitYearIntoMonths(LocalDate, LocalDate)} instead
     */
    @Deprecated
    public static List<SSMonth> splitYearIntoMonths(Date iFrom, Date iTo) {
        return splitYearIntoMonths(SSDateUtil.toLocalDate(iFrom), SSDateUtil.toLocalDate(iTo));
    }

    /**
     * Breaks a year into its months.
     *
     * @param from the start date
     * @param to the end date
     * @return List of months covering each calendar month in the range
     */
    public static List<SSMonth> splitYearIntoMonths(LocalDate from, LocalDate to) {
        List<SSMonth> iMonths = new LinkedList<>();

        // Start at the first day of the month containing 'from'
        LocalDate current = from.withDayOfMonth(1);

        // Loop through all months between the from date and the to date
        while (current.compareTo(to) < 0) {
            LocalDate monthStart = current;
            LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());

            iMonths.add(new SSMonth(monthStart, monthEnd));

            current = current.plusMonths(1);
        }
        return iMonths;
    }

    /**
     * Custom deserialization to handle backward compatibility.
     * Pre-migration serialized streams stored {@code iFrom} and {@code iTo} as
     * {@code java.util.Date}.  This method reads them as raw objects and converts
     * via {@link SSDateUtil#readLocalDate(Object)}.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = in.readFields();
        iFrom = SSDateUtil.readLocalDate(fields.get("iFrom", null));
        iTo = SSDateUtil.readLocalDate(fields.get("iTo", null));
    }

}
