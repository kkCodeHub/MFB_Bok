package se.swedsoft.bookkeeping.gui.inpayment.util;


import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSBigDecimalCellRenderer;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSDateCellRenderer;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:34:35
 */
public class SSInpaymentTableModel extends SSDefaultTableModel<SSInpayment> {

    /**
     * Default constructor.
     */
    public SSInpaymentTableModel() {
        this(Repositories.inpayments().findAll());
    }

    /**
     * Constructor.
     *
     * @param pObjects The data for the table model.
     */
    public SSInpaymentTableModel(List<SSInpayment> pObjects) {
        super(pObjects);
        addColumn("");
        addColumn(SSBundle.getBundle().getString("inpaymenttable.column.1"));
        addColumn(SSBundle.getBundle().getString("inpaymenttable.column.2"));
        addColumn(SSBundle.getBundle().getString("inpaymenttable.column.3"));
        addColumn(SSBundle.getBundle().getString("inpaymenttable.column.4"));
    }

    @Override
    public Class<?> getType() {
        return SSProduct.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SSInpayment iInpayment = getObject(rowIndex);

        Object value = null;

        switch (columnIndex) {
        case 0:
            value = iInpayment.isEntered()
                    ? SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL)
                    : null;
            break;

        case 1:
            value = iInpayment.getNumber();
            break;

        case 2:
            value = SSDateUtil.toDate(iInpayment.getLocalDate());
            break;

        case 3:
            value = iInpayment.getText();
            break;

        case 4:
            value = SSInpaymentMath.getSum(iInpayment);
            break;
        }

        return value;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return ImageIcon.class;

        case 1:
            return String.class;

        case 2:
            return Date.class;

        case 3:
            return String.class;

        case 4:
            return BigDecimal.class;
        }
        return super.getColumnClass(columnIndex);
    }

    /**
     *
     * @param iTable
     */
    public static void setupTable(SSTable iTable) {
        iTable.getColumnModel().getColumn(0).setPreferredWidth(20);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        iTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        iTable.getColumnModel().getColumn(3).setPreferredWidth(470);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(95);

        iTable.setDefaultRenderer(Date.class, new SSDateCellRenderer());

        iTable.setDefaultRenderer(BigDecimal.class, new SSBigDecimalCellRenderer(2));
    }
}
