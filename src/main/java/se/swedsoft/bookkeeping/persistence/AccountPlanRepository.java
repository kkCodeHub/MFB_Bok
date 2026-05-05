package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSAccountPlan;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSAccountPlan} persistence operations.
 */
public interface AccountPlanRepository {

    /**
     * Returns all account plans.
     *
     * @return mutable list of account plans; never {@code null}
     */
    List<SSAccountPlan> findAll();

    /**
     * Looks up an account plan by id.
     *
     * @param id account-plan id
     * @return matching plan, or empty when not found
     */
    Optional<SSAccountPlan> findById(int id);

    /**
     * Persists a new account plan.
     *
     * @param plan account plan to add
     */
    void add(SSAccountPlan plan);

    /**
     * Updates an existing account plan.
     *
     * @param plan account plan to update
     */
    void update(SSAccountPlan plan);

    /**
     * Deletes an account plan.
     *
     * @param plan account plan to delete
     */
    void delete(SSAccountPlan plan);
}

