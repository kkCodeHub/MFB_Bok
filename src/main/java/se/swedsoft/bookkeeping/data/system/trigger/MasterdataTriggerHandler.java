package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.calc.math.SSCustomerMath;
import se.swedsoft.bookkeeping.calc.math.SSSupplierMath;
import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.autodist.SSAutoDistFrame;
import se.swedsoft.bookkeeping.gui.customer.SSCustomerFrame;
import se.swedsoft.bookkeeping.gui.product.SSProductFrame;
import se.swedsoft.bookkeeping.gui.project.SSProjectFrame;
import se.swedsoft.bookkeeping.gui.resultunit.SSResultUnitFrame;
import se.swedsoft.bookkeeping.gui.supplier.SSSupplierFrame;
import se.swedsoft.bookkeeping.gui.vouchertemplate.SSVoucherTemplateFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class MasterdataTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MasterdataTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    MasterdataTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (iTriggerName.contains("PROJECT")) {
            if (SSProjectFrame.getInstance() != null) {
                SSProjectFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.contains("RESULTUNIT")) {
            if (SSResultUnitFrame.getInstance() != null) {
                SSResultUnitFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.contains("VOUCHERTEMPLATE")) {
            if (SSVoucherTemplateFrame.getInstance() != null) {
                SSVoucherTemplateFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWPRODUCT", "EDITPRODUCT", "DELETEPRODUCT")) {
            List<SSProduct> iProducts = iRuntime.getProducts();
            if (iProducts == null) {
                return true;
            }
            SSProduct iProduct = new SSProduct();
            iProduct.setNumber(iNumber);

            if (iTriggerName.equals("NEWPRODUCT")) {
                Optional<SSProduct> optProduct = Repositories.products().findByProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("NEWPRODUCT trigger: product not found for number {}", iNumber);
                    return true;
                }
                iProduct = optProduct.get();
                if (!iProducts.contains(iProduct)) {
                    iProducts.add(iProduct);
                }
            } else if (iTriggerName.equals("EDITPRODUCT")) {
                Optional<SSProduct> optProduct = Repositories.products().findByProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("EDITPRODUCT trigger: product not found for number {}", iNumber);
                    return true;
                }
                iProduct = optProduct.get();
                int iIndex = iProducts.lastIndexOf(iProduct);
                if (iIndex == -1) {
                    return true;
                }
                iProducts.remove(iIndex);
                iProducts.add(iIndex, iProduct);
            } else {
                iProducts.remove(iProduct);
            }

            if (SSProductFrame.getInstance() != null) {
                SSProductFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWCUSTOMER", "EDITCUSTOMER", "DELETECUSTOMER")) {
            List<SSCustomer> iCustomers = iRuntime.getCustomers();
            if (iCustomers == null) {
                return true;
            }
            SSCustomer iCustomer = new SSCustomer();
            iCustomer.setNumber(iNumber);

            if (iTriggerName.equals("NEWCUSTOMER")) {
                Optional<SSCustomer> optCustomer = Repositories.customers().findByCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("NEWCUSTOMER trigger: customer not found for number {}", iNumber);
                    return true;
                }
                iCustomer = optCustomer.get();
                if (!iCustomers.contains(iCustomer)) {
                    iCustomers.add(iCustomer);
                }
                if (SSCustomerMath.iInvoicesForCustomers == null) {
                    SSCustomerMath.iInvoicesForCustomers = new HashMap<>();
                }
                SSCustomerMath.iInvoicesForCustomers.put(iCustomer.getNumber(), new LinkedList<>());
            } else if (iTriggerName.equals("EDITCUSTOMER")) {
                Optional<SSCustomer> optCustomer = Repositories.customers().findByCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("EDITCUSTOMER trigger: customer not found for number {}", iNumber);
                    return true;
                }
                iCustomer = optCustomer.get();
                int iIndex = iCustomers.lastIndexOf(iCustomer);
                if (iIndex == -1) {
                    return true;
                }
                iCustomers.remove(iIndex);
                iCustomers.add(iIndex, iCustomer);
            } else {
                iCustomers.remove(iCustomer);
            }

            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWSUPPLIER", "EDITSUPPLIER", "DELETESUPPLIER")) {
            List<SSSupplier> iSuppliers = iRuntime.getSuppliers();
            if (iSuppliers == null) {
                return true;
            }
            SSSupplier iSupplier = new SSSupplier();
            iSupplier.setNumber(iNumber);

            if (iTriggerName.equals("NEWSUPPLIER")) {
                Optional<SSSupplier> optSupplier = Repositories.suppliers().findBySupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("NEWSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return true;
                }
                iSupplier = optSupplier.get();
                if (!iSuppliers.contains(iSupplier)) {
                    iSuppliers.add(iSupplier);
                }
                if (SSSupplierMath.iInvoicesForSuppliers == null) {
                    SSSupplierMath.iInvoicesForSuppliers = new HashMap<>();
                }
                SSSupplierMath.iInvoicesForSuppliers.put(iSupplier.getNumber(), new LinkedList<>());
            } else if (iTriggerName.equals("EDITSUPPLIER")) {
                Optional<SSSupplier> optSupplier = Repositories.suppliers().findBySupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("EDITSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return true;
                }
                iSupplier = optSupplier.get();
                int iIndex = iSuppliers.lastIndexOf(iSupplier);
                if (iIndex == -1) {
                    return true;
                }
                iSuppliers.remove(iIndex);
                iSuppliers.add(iIndex, iSupplier);
            } else {
                iSuppliers.remove(iSupplier);
            }

            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWAUTODIST", "EDITAUTODIST", "DELETEAUTODIST")) {
            List<SSAutoDist> iAutoDists = iRuntime.getAutoDists();
            if (iAutoDists == null) {
                return true;
            }
            Integer iAccount = Integer.parseInt(iNumber);
            SSAutoDist iAutoDist = new SSAutoDist();
            iAutoDist.setAccountNumber(iAccount);

            if (iTriggerName.equals("NEWAUTODIST")) {
                Optional<SSAutoDist> optAutoDist = Repositories.autoDists().findByAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("NEWAUTODIST trigger: autodist not found for number {}", iNumber);
                    return true;
                }
                iAutoDist = optAutoDist.get();
                if (!iAutoDists.contains(iAutoDist)) {
                    iAutoDists.add(iAutoDist);
                }
            } else if (iTriggerName.equals("EDITAUTODIST")) {
                Optional<SSAutoDist> optAutoDist = Repositories.autoDists().findByAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("EDITAUTODIST trigger: autodist not found for number {}", iNumber);
                    return true;
                }
                iAutoDist = optAutoDist.get();
                int iIndex = iAutoDists.lastIndexOf(iAutoDist);
                if (iIndex == -1) {
                    return true;
                }
                iAutoDists.remove(iIndex);
                iAutoDists.add(iIndex, iAutoDist);
            } else {
                iAutoDists.remove(iAutoDist);
            }

            if (SSAutoDistFrame.getInstance() != null) {
                SSAutoDistFrame.getInstance().updateFrame();
            }
            return true;
        }

        return false;
    }
}

