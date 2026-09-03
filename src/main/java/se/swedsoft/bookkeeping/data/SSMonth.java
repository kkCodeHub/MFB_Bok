package se.swedsoft.bookkeeping.data;


import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

    public SSMonth(LocalDate pFrom) {
        iFrom = pFrom;
        iTo = null;
    }

    public SSMonth(LocalDate pFrom, LocalDate pTo) {
        iFrom = pFrom;
        iTo = pTo;
    }

    public LocalDate getFrom() {
        return iFrom;
    }

    public LocalDate getLocalFrom() {
        return iFrom;
    }

    public LocalDate getTo() {
        return iTo;
    }

    public LocalDate getLocalTo() {
        return iTo;
    }

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
        DateTimeFormatter format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        return format.format(iFrom).substring(0, 7);
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

    public boolean isDateInMonth(LocalDate iDate) {
        return iDate != null
                && iDate.getMonth() == iFrom.getMonth()
                && iDate.getYear() == iFrom.getYear();
    }

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

}
