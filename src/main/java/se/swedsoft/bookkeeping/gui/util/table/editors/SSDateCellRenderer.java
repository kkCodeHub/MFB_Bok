/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.util.table.editors;


import javax.swing.table.DefaultTableCellRenderer;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;


// Trade Extensions specific imports

// Java specific imports

/**
 * This class implements a cell renderer that renders a Date using a DateFormat
 * with format DateFormat.SHORT. This will only display year-month-day.
 *
 * @author Roger Björnstedt
 */
public class SSDateCellRenderer extends DefaultTableCellRenderer {

    // The formatter to use for java.util.Date values.
    private DateFormat iFormat;

    // The formatter to use for java.time.LocalDate values.
    private DateTimeFormatter iLocalDateFormat;

    /**
     * Default constructor.
     */
    public SSDateCellRenderer() {
        setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }

    /**
     * Sets the value for the cell. Accepts both {@link java.util.Date} and
     * {@link java.time.LocalDate} values.
     *
     * @param value The value to format.
     */
    @Override
    public void setValue(Object value) {
        if (value == null) {
            setText("");
        } else if (value instanceof LocalDate) {
            if (iLocalDateFormat == null) {
                iLocalDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            }
            setText(iLocalDateFormat.format((LocalDate) value));
        } else {
            if (iFormat == null) {
                iFormat = DateFormat.getDateInstance(DateFormat.SHORT);
            }
            setText(iFormat.format(value));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.table.editors.SSDateCellRenderer");
        sb.append("{iFormat=").append(iFormat);
        sb.append('}');
        return sb.toString();
    }
}
