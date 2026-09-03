package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * Transition service for migrating inventory and delivery callsites away from
 * direct SSDB facade access.
 *
 * <p>Provides access to inventory documents, indeliveries, and outdeliveries.</p>
 *
 * <p>This is domain #10 in the 12-domain target architecture.</p>
 */
public final class SSInventoryDeliveriesContext {

    private SSInventoryDeliveriesContext() {}

    // -------------------------------------------------------------------------
    // Inventory CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all inventory documents for the current company.
     *
     * @return list of inventories; never {@code null}
     */
    public static List<SSInventory> getInventories() {
        return Repositories.inventories().findAll();
    }

    /**
     * Looks up an inventory document by reference.
     *
     * @param pInventory reference inventory; must not be {@code null}
     * @return an {@link Optional} containing the stored inventory, or empty if not found
     */
    public static Optional<SSInventory> getInventory(SSInventory pInventory) {
        return Repositories.inventories().findByInventory(pInventory);
    }

    /**
     * Creates a new inventory document with auto-generated number.
     *
     * @return a new {@link SSInventory} with auto-assigned number; never {@code null}
     */
    public static SSInventory createInventory() {
        return Repositories.inventories().createNew();
    }

    /**
     * Persists a new inventory document.
     *
     * @param pInventory the inventory to add; must not be {@code null}
     */
    public static void addInventory(SSInventory pInventory) {
        Repositories.inventories().add(pInventory);
    }

    /**
     * Updates an existing inventory document.
     *
     * @param pInventory the inventory with updated values; must not be {@code null}
     */
    public static void updateInventory(SSInventory pInventory) {
        Repositories.inventories().update(pInventory);
    }

    /**
     * Deletes an inventory document.
     *
     * @param pInventory the inventory to delete; must not be {@code null}
     */
    public static void deleteInventory(SSInventory pInventory) {
        Repositories.inventories().delete(pInventory);
    }

    // -------------------------------------------------------------------------
    // Indelivery CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all indeliveries for the current company.
     *
     * @return list of indeliveries; never {@code null}
     */
    public static List<SSIndelivery> getIndeliveries() {
        return Repositories.indeliveries().findAll();
    }

    /**
     * Looks up an indelivery by reference.
     *
     * @param pIndelivery reference indelivery; must not be {@code null}
     * @return an {@link Optional} containing the stored indelivery, or empty if not found
     */
    public static Optional<SSIndelivery> getIndelivery(SSIndelivery pIndelivery) {
        return Repositories.indeliveries().findByIndelivery(pIndelivery);
    }

    /**
     * Creates a new indelivery document with auto-generated number.
     *
     * @return a new {@link SSIndelivery} with auto-assigned number; never {@code null}
     */
    public static SSIndelivery createIndelivery() {
        return Repositories.indeliveries().createNew();
    }

    /**
     * Persists a new indelivery.
     *
     * @param pIndelivery the indelivery to add; must not be {@code null}
     */
    public static void addIndelivery(SSIndelivery pIndelivery) {
        Repositories.indeliveries().add(pIndelivery);
    }

    /**
     * Updates an existing indelivery.
     *
     * @param pIndelivery the indelivery with updated values; must not be {@code null}
     */
    public static void updateIndelivery(SSIndelivery pIndelivery) {
        Repositories.indeliveries().update(pIndelivery);
    }

    /**
     * Deletes an indelivery.
     *
     * @param pIndelivery the indelivery to delete; must not be {@code null}
     */
    public static void deleteIndelivery(SSIndelivery pIndelivery) {
        Repositories.indeliveries().delete(pIndelivery);
    }

    // -------------------------------------------------------------------------
    // Outdelivery CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all outdeliveries for the current company.
     *
     * @return list of outdeliveries; never {@code null}
     */
    public static List<SSOutdelivery> getOutdeliveries() {
        return Repositories.outdeliveries().findAll();
    }

    /**
     * Looks up an outdelivery by reference.
     *
     * @param pOutdelivery reference outdelivery; must not be {@code null}
     * @return an {@link Optional} containing the stored outdelivery, or empty if not found
     */
    public static Optional<SSOutdelivery> getOutdelivery(SSOutdelivery pOutdelivery) {
        return Repositories.outdeliveries().findByOutdelivery(pOutdelivery);
    }

    /**
     * Creates a new outdelivery document with auto-generated number.
     *
     * @return a new {@link SSOutdelivery} with auto-assigned number; never {@code null}
     */
    public static SSOutdelivery createOutdelivery() {
        return Repositories.outdeliveries().createNew();
    }

    /**
     * Persists a new outdelivery.
     *
     * @param pOutdelivery the outdelivery to add; must not be {@code null}
     */
    public static void addOutdelivery(SSOutdelivery pOutdelivery) {
        Repositories.outdeliveries().add(pOutdelivery);
    }

    /**
     * Updates an existing outdelivery.
     *
     * @param pOutdelivery the outdelivery with updated values; must not be {@code null}
     */
    public static void updateOutdelivery(SSOutdelivery pOutdelivery) {
        Repositories.outdeliveries().update(pOutdelivery);
    }

    /**
     * Deletes an outdelivery.
     *
     * @param pOutdelivery the outdelivery to delete; must not be {@code null}
     */
    public static void deleteOutdelivery(SSOutdelivery pOutdelivery) {
        Repositories.outdeliveries().delete(pOutdelivery);
    }
}
