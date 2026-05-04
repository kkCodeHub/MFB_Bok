/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.util.table.editors;


import javax.swing.table.DefaultTableCellRenderer;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;


/**
 * This class implements a cell renderer that renders a Date and time
 *
 */
public class SSDateTimeCellRenderer extends DefaultTableCellRenderer {

    // The formatter to use.
    private DateFormat iDateFormat;
    private DateFormat iTimeFormat;

    /**
     * Default constructor.
     */
    public SSDateTimeCellRenderer() {
        iDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        iTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    }

    /**
     * Sets the value for the cell. Accepts {@link java.util.Date},
     * {@link java.time.LocalDate}, and {@link java.time.LocalDateTime} values.
     *
     * @param value The value to format.
     */
    @Override
    public void setValue(Object value) {
        if (value == null) {
            setText("");
        } else if (value instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
            setText(dtf.format((LocalDateTime) value));
        } else if (value instanceof LocalDate) {
            DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            setText(df.format((LocalDate) value));
        } else {
            setText(iDateFormat.format(value) + ' ' + iTimeFormat.format(value));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.table.editors.SSDateTimeCellRenderer");
        sb.append("{iDateFormat=").append(iDateFormat);
        sb.append(", iTimeFormat=").append(iTimeFormat);
        sb.append('}');
        return sb.toString();
    }
}
