package se.swedsoft.bookkeeping.gui.outpayment.util;


import se.swedsoft.bookkeeping.calc.math.SSOutpaymentMath;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSBigDecimalCellRenderer;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSDateCellRenderer;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableColumn;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableModel;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.ImageIcon;
import java.math.BigDecimal;
import java.util.Date;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:34:35
 */
public class SSOutpaymentTableModel extends SSTableModel<SSOutpayment> {

    /**
     * Default constructor.
     */
    public SSOutpaymentTableModel() {
        super(Repositories.outpayments().findAll());
    }

    /**
     * Returns the type of data in this model.
     *
     * @return The current data type.
     */
    @Override
    public Class<?> getType() {
        return SSOutpayment.class;
    }

    /**
     * Number column
     */
    public static SSTableColumn<SSOutpayment> COLUMN_NUMBER = new SSTableColumn<>(
            SSBundle.getBundle().getString("outpaymenttable.column.1")) {
        @Override
        public Object getValue(SSOutpayment iObject) {
            return iObject.getNumber();
        }

        @Override
        public void setValue(SSOutpayment iObject, Object iValue) {
            iObject.setNumber((Integer) iValue);

        }

        @Override
        public Class getColumnClass() {
            return Integer.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Entered status column
     */
    public static SSTableColumn<SSOutpayment> COLUMN_ENTERED = new SSTableColumn<>("") {
        @Override
        public Object getValue(SSOutpayment iObject) {
            return iObject.isEntered()
                    ? SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL)
                    : null;
        }

        @Override
        public void setValue(SSOutpayment iObject, Object iValue) {}

        @Override
        public Class getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getDefaultWidth() {
            return 20;
        }
    };

    /**
     * Date column
     */
    public static SSTableColumn<SSOutpayment> COLUMN_DATE = new SSTableColumn<>(
            SSBundle.getBundle().getString("outpaymenttable.column.2")) {
        @Override
        public Object getValue(SSOutpayment iObject) {
            return SSDateUtil.toDate(iObject.getLocalDate());
        }

        @Override
        public void setValue(SSOutpayment iObject, Object iValue) {
            iObject.setLocalDate(SSDateUtil.toLocalDate((Date) iValue));

        }

        @Override
        public Class getColumnClass() {
            return Date.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Date column
     */
    public static SSTableColumn<SSOutpayment> COLUMN_TEXT = new SSTableColumn<>(
            SSBundle.getBundle().getString("outpaymenttable.column.3")) {
        @Override
        public Object getValue(SSOutpayment iObject) {
            return iObject.getText();
        }

        @Override
        public void setValue(SSOutpayment iObject, Object iValue) {
            iObject.setText((String) iValue);

        }

        @Override
        public Class getColumnClass() {
            return String.class;
        }

        @Override
        public int getDefaultWidth() {
            return 450;
        }
    };

    /**
     * Sum column
     */
    public static SSTableColumn<SSOutpayment> COLUMN_SUM = new SSTableColumn<>(
            SSBundle.getBundle().getString("outpaymenttable.column.4")) {
        @Override
        public Object getValue(SSOutpayment iObject) {
            return SSOutpaymentMath.getSum(iObject);
        }

        @Override
        public void setValue(SSOutpayment iObject, Object iValue) {}

        @Override
        public Class getColumnClass() {
            return BigDecimal.class;
        }

        @Override
        public int getDefaultWidth() {
            return 120;
        }
    };

    public static void setupStatusTable(SSTable iTable) {
        iTable.getColumnModel().getColumn(0).setPreferredWidth(20);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        iTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        iTable.getColumnModel().getColumn(3).setPreferredWidth(470);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(95);

        iTable.setDefaultRenderer(Date.class, new SSDateCellRenderer());
        iTable.setDefaultRenderer(BigDecimal.class, new SSBigDecimalCellRenderer(2));
    }

}
