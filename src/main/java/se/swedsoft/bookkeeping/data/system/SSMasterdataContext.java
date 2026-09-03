package se.swedsoft.bookkeeping.data.system;


import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;


/**
 * Transition service for migrating masterdata callsites away from direct SSDB facade access.
 */
public final class SSMasterdataContext {

    private SSMasterdataContext() {}

    public static List<SSCustomer> getCustomers() {
        return Repositories.customers().findAll();
    }

    public static Optional<SSCustomer> getCustomer(SSCustomer pCustomer) {
        if (pCustomer == null) {
            return Optional.empty();
        }
        return Repositories.customers().findByCustomer(pCustomer);
    }

    public static Optional<SSCustomer> getCustomer(String pCustomerNumber) {
        return Repositories.customers().findByNumber(pCustomerNumber);
    }

    public static List<SSCustomer> getCustomers(List<SSCustomer> pCustomers) {
        return Repositories.customers().findAll(pCustomers);
    }

    public static void addCustomer(SSCustomer pCustomer) {
        Repositories.customers().add(pCustomer);
    }

    public static void updateCustomer(SSCustomer pCustomer) {
        Repositories.customers().update(pCustomer);
    }

    public static void deleteCustomer(SSCustomer pCustomer) {
        Repositories.customers().delete(pCustomer);
    }

    public static List<SSSupplier> getSuppliers() {
        return Repositories.suppliers().findAll();
    }

    public static Optional<SSSupplier> getSupplier(SSSupplier pSupplier) {
        if (pSupplier == null) {
            return Optional.empty();
        }
        return Repositories.suppliers().findBySupplier(pSupplier);
    }

    public static List<SSSupplier> getSuppliers(List<SSSupplier> pSuppliers) {
        return Repositories.suppliers().findAll(pSuppliers);
    }

    public static void addSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().add(pSupplier);
    }

    public static void updateSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().update(pSupplier);
    }

    public static void deleteSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().delete(pSupplier);
    }

    public static List<SSUnit> getUnits() {
        return Repositories.units().findAll();
    }

    public static Optional<SSUnit> getUnit(String pName) {
        return Repositories.units().findByName(pName);
    }

    public static void addUnit(SSUnit pUnit) {
        Repositories.units().add(pUnit);
    }

    public static void updateUnit(SSUnit pUnit) {
        Repositories.units().update(pUnit);
    }

    public static void deleteUnit(SSUnit pUnit) {
        Repositories.units().delete(pUnit);
    }

    public static List<SSCurrency> getCurrencies() {
        return Repositories.currencies().findAll();
    }

    public static Optional<SSCurrency> getCurrency(SSCurrency pCurrency) {
        if (pCurrency == null || pCurrency.getName() == null) {
            return Optional.empty();
        }
        return Repositories.currencies().findByCode(pCurrency.getName());
    }

    public static void addCurrency(SSCurrency pCurrency) {
        Repositories.currencies().add(pCurrency);
    }

    public static void updateCurrency(SSCurrency pCurrency) {
        Repositories.currencies().update(pCurrency);
    }

    public static void deleteCurrency(SSCurrency pCurrency) {
        Repositories.currencies().delete(pCurrency);
    }

    public static List<SSDeliveryWay> getDeliveryWays() {
        return Repositories.deliveryWays().findAll();
    }

    public static Optional<SSDeliveryWay> getDeliveryWay(String pName) {
        return Repositories.deliveryWays().findByName(pName);
    }

    public static void addDeliveryWay(SSDeliveryWay pDeliveryWay) {
        Repositories.deliveryWays().add(pDeliveryWay);
    }

    public static void updateDeliveryWay(SSDeliveryWay pDeliveryWay) {
        Repositories.deliveryWays().update(pDeliveryWay);
    }

    public static void deleteDeliveryWay(SSDeliveryWay pDeliveryWay) {
        Repositories.deliveryWays().delete(pDeliveryWay);
    }

    public static List<SSDeliveryTerm> getDeliveryTerms() {
        return Repositories.deliveryTerms().findAll();
    }

    public static Optional<SSDeliveryTerm> getDeliveryTerm(String pName) {
        return Repositories.deliveryTerms().findByName(pName);
    }

    public static void addDeliveryTerm(SSDeliveryTerm pDeliveryTerm) {
        Repositories.deliveryTerms().add(pDeliveryTerm);
    }

    public static void updateDeliveryTerm(SSDeliveryTerm pDeliveryTerm) {
        Repositories.deliveryTerms().update(pDeliveryTerm);
    }

    public static void deleteDeliveryTerm(SSDeliveryTerm pDeliveryTerm) {
        Repositories.deliveryTerms().delete(pDeliveryTerm);
    }

    public static List<SSPaymentTerm> getPaymentTerms() {
        return Repositories.paymentTerms().findAll();
    }

    public static Optional<SSPaymentTerm> getPaymentTerm(String pName) {
        return Repositories.paymentTerms().findByName(pName);
    }

    public static void addPaymentTerm(SSPaymentTerm pPaymentTerm) {
        Repositories.paymentTerms().add(pPaymentTerm);
    }

    public static void updatePaymentTerm(SSPaymentTerm pPaymentTerm) {
        Repositories.paymentTerms().update(pPaymentTerm);
    }

    public static void deletePaymentTerm(SSPaymentTerm pPaymentTerm) {
        Repositories.paymentTerms().delete(pPaymentTerm);
    }

//    /** @deprecated Use {@link SSResultUnitContext#getResultUnits()}. */
//    @Deprecated
//    public static List<SSNewResultUnit> getResultUnits() {
//        return SSResultUnitContext.getResultUnits();
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#getResultUnits(List)}. */
//    @Deprecated
//    public static List<SSNewResultUnit> getResultUnits(List<SSNewResultUnit> pResultUnits) {
//        return SSResultUnitContext.getResultUnits(pResultUnits);
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#addResultUnit(SSNewResultUnit)}. */
//    @Deprecated
//    public static void addResultUnit(SSNewResultUnit pResultUnit) {
//        SSResultUnitContext.addResultUnit(pResultUnit);
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#updateResultUnit(SSNewResultUnit)}. */
//    @Deprecated
//    public static void updateResultUnit(SSNewResultUnit pResultUnit) {
//        SSResultUnitContext.updateResultUnit(pResultUnit);
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#deleteResultUnit(SSNewResultUnit)}. */
//    @Deprecated
//    public static void deleteResultUnit(SSNewResultUnit pResultUnit) {
//        SSResultUnitContext.deleteResultUnit(pResultUnit);
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#getResultUnit(SSNewResultUnit)}. */
//    @Deprecated
//    public static Optional<SSNewResultUnit> getResultUnit(SSNewResultUnit pResultUnit) {
//        return SSResultUnitContext.getResultUnit(pResultUnit);
//    }
//
//    /** @deprecated Use {@link SSResultUnitContext#getResultUnit(String)}. */
//    @Deprecated
//    public static Optional<SSNewResultUnit> getResultUnit(String pResultUnitNumber) {
//        return SSResultUnitContext.getResultUnit(pResultUnitNumber);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#getProjects()}. */
//    @Deprecated
//    public static List<SSNewProject> getProjects() {
//        return SSProjectContext.getProjects();
//    }
//
//    /** @deprecated Use {@link SSProjectContext#getProjects(List)}. */
//    @Deprecated
//    public static List<SSNewProject> getProjects(List<SSNewProject> pProjects) {
//        return SSProjectContext.getProjects(pProjects);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#addProject(SSNewProject)}. */
//    @Deprecated
//    public static void addProject(SSNewProject pProject) {
//        SSProjectContext.addProject(pProject);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#updateProject(SSNewProject)}. */
//    @Deprecated
//    public static void updateProject(SSNewProject pProject) {
//        SSProjectContext.updateProject(pProject);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#deleteProject(SSNewProject)}. */
//    @Deprecated
//    public static void deleteProject(SSNewProject pProject) {
//        SSProjectContext.deleteProject(pProject);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#getProject(SSNewProject)}. */
//    @Deprecated
//    public static Optional<SSNewProject> getProject(SSNewProject pProject) {
//        return SSProjectContext.getProject(pProject);
//    }
//
//    /** @deprecated Use {@link SSProjectContext#getProject(String)}. */
//    @Deprecated
//    public static Optional<SSNewProject> getProject(String pProjectNumber) {
//        return SSProjectContext.getProject(pProjectNumber);
//    }

}
