package se.swedsoft.bookkeeping.gui.invoice.util;


import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.calc.math.SSProductQuantityValidator;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSQuantityPresentationUtil;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSBigDecimalCellEditor;
import se.swedsoft.bookkeeping.gui.util.table.editors.SSBigDecimalCellRenderer;
import se.swedsoft.bookkeeping.gui.util.table.model.SSEditableTableModel;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableColumn;

import java.math.BigDecimal;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:34:35
 */
public class SSInvoiceRowTableModel extends SSEditableTableModel<SSSaleRow> {

    private static SSCustomer iCustomer;

    /**
     *
     * @return
     */
    @Override
    public SSSaleRow newObject() {
        return  new SSSaleRow();
    }

    /**
     * Returns the type of data in this model.
     *
     * @return The current data type.
     */
    @Override
    public Class<?> getType() {
        return SSSaleRow.class;
    }

    public void setCustomer(SSCustomer pCustomer) {
        iCustomer = pCustomer;
    }

    /**
     * Product column
     */
    public static SSTableColumn<SSSaleRow> COLUMN_PRODUCT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.1")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            SSProduct iProduct = iObject.getProduct(se.swedsoft.bookkeeping.data.system.SSProductContext.getProducts());

            return iProduct != null ? iProduct : iObject.getProductNr();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            if (iValue instanceof SSProduct) {
                iObject.setProduct((SSProduct) iValue);
                if (iCustomer != null && iCustomer.getDiscount() != null) {
                    iObject.setDiscount(
                            iCustomer.getDiscount().doubleValue()
                                    == new BigDecimal(0).doubleValue()
                                            ? null
                                            : iCustomer.getDiscount());
                }
                if (iObject.getProduct() != null
                        && iObject.getProduct().getResultUnitNr() != null) {
                    iObject.setResultUnit(
                            iObject.getProduct().getResultUnit(
                                    iObject.getProduct().getResultUnitNr()).orElse(null));
                }
                if (iObject.getProduct() != null
                        && iObject.getProduct().getProjectNr() != null) {
                    iObject.setProject(
                            iObject.getProduct().getProject(
                                    iObject.getProduct().getProjectNr()).orElse(null));
                }
            } else {
                iObject.setProductNr((String) iValue);

                SSNewCompany iCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                if (iObject.getTaxCode() == null) {
                    iObject.setTaxCode(SSTaxCode.TAXRATE_1);
                }
                if (iObject.getUnit() == null) {
                    iObject.setUnit(iCompany.getStandardUnit());
                }

                if (iCustomer != null && iCustomer.getDiscount() != null) {
                    iObject.setDiscount(
                            iCustomer.getDiscount().doubleValue()
                                    == new BigDecimal(0).doubleValue()
                                            ? null
                                            : iCustomer.getDiscount());
                }
                if (iObject.getProduct() != null
                        && iObject.getProduct().getResultUnit() != null) {
                    iObject.setResultUnit(iObject.getProduct().getResultUnit());
                }
                if (iObject.getProduct() != null
                        && iObject.getProduct().getProject() != null) {
                    iObject.setProject(iObject.getProduct().getProject());
                }

            }
        }

        @Override
        public Class getColumnClass() {
            return SSProduct.class;
        }

        @Override
        public int getDefaultWidth() {
            return 150;
        }
    };

    /**
     * Description column
     */
    public static SSTableColumn<SSSaleRow> COLUMN_DESCRIPTION = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.2")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getDescription();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setDescription((String) iValue);

            SSNewCompany iCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

            if (iObject.getTaxCode() == null) {
                iObject.setTaxCode(SSTaxCode.TAXRATE_1);
            }
            if (iObject.getUnit() == null) {
                iObject.setUnit(iCompany.getStandardUnit());
            }
        }

        @Override
        public Class getColumnClass() {
            return String.class;
        }

        @Override
        public int getDefaultWidth() {
            return 150;
        }
    };

    /**
     * Unit price
     */
    public static SSTableColumn<SSSaleRow> COLUMN_UNITPRICE = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.3")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getUnitprice();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setUnitprice((BigDecimal) iValue);
        }

        @Override
        public Class getColumnClass() {
            return BigDecimal.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Quantity
     */
    public static SSTableColumn<SSSaleRow> COLUMN_QUANTITY = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.4")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return SSQuantityPresentationUtil.toDisplayQuantity(iObject.getQuantity());
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            Integer iQuantityTenths = SSQuantityPresentationUtil.toStoredTenths(iValue);
            SSProduct iProduct = iObject.getProduct(se.swedsoft.bookkeeping.data.system.SSProductContext.getProducts());

            if (!SSProductQuantityValidator.isValidQuantity(iProduct, iQuantityTenths)) {
                return;
            }

            iObject.setQuantity(iQuantityTenths);
        }

        @Override
        public Class getColumnClass() {
            return BigDecimal.class;
        }

        @Override
        public SSBigDecimalCellRenderer getCellRenderer() {
            return new SSBigDecimalCellRenderer(1);
        }

        @Override
        public SSBigDecimalCellEditor getCellEditor() {
            return new SSBigDecimalCellEditor(1);
        }

        @Override
        public int getDefaultWidth() {
            return 60;
        }
    };

    /**
     * Quantity
     */
    public static SSTableColumn<SSSaleRow> COLUMN_UNIT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.5")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getUnit();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setUnit((SSUnit) iValue);
        }

        @Override
        public Class getColumnClass() {
            return SSUnit.class;
        }

        @Override
        public int getDefaultWidth() {
            return 40;
        }
    };

    /**
     * Sum
     */
    public static SSTableColumn<SSSaleRow> COLUMN_DISCOUNT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.6")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getDiscount();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setDiscount((BigDecimal) iValue);
        }

        @Override
        public Class getColumnClass() {
            return BigDecimal.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Sum
     */
    public static SSTableColumn<SSSaleRow> COLUMN_SUM = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.7")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getSum().orElse(null);
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {}

        @Override
        public Class getColumnClass() {
            return BigDecimal.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Moms
     */
    public static SSTableColumn<SSSaleRow> COLUMN_TAX = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.8")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getTaxCode();
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setTaxCode((SSTaxCode) iValue);
        }

        @Override
        public Class getColumnClass() {
            return SSTaxCode.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Account
     */
    public static SSTableColumn<SSSaleRow> COLUMN_ACCOUNT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.9")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getAccount(se.swedsoft.bookkeeping.data.system.SSAccountingContext.getAccounts());
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            if (iValue instanceof SSAccount) {
                iObject.setAccount((SSAccount) iValue);
            }

        }

        @Override
        public Class getColumnClass() {
            return SSAccount.class;
        }

        @Override
        public int getDefaultWidth() {
            return 60;
        }
    };

    /**
     * Account
     */
    public static SSTableColumn<SSSaleRow> COLUMN_RESULTUNIT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.10")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getResultUnit(se.swedsoft.bookkeeping.data.system.SSResultUnitContext.getResultUnits());
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setResultUnit((SSNewResultUnit) iValue);
        }

        @Override
        public Class getColumnClass() {
            return SSNewResultUnit.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

    /**
     * Project
     */
    public static SSTableColumn<SSSaleRow> COLUMN_PROJECT = new SSTableColumn<>(
            SSBundle.getBundle().getString("salerowtable.column.11")) {
        @Override
        public Object getValue(SSSaleRow iObject) {
            return iObject.getProject(se.swedsoft.bookkeeping.data.system.SSProjectContext.getProjects());
        }

        @Override
        public void setValue(SSSaleRow iObject, Object iValue) {
            iObject.setProject((SSNewProject) iValue);
        }

        @Override
        public Class getColumnClass() {
            return SSNewProject.class;
        }

        @Override
        public int getDefaultWidth() {
            return 100;
        }
    };

}
