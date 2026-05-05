package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.AccountPlanRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link AccountPlanRepository} implementation backed by {@link SSDB}.
 */
public class SSDBAccountPlanRepository implements AccountPlanRepository {

    private final SSDB db;

    public SSDBAccountPlanRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    @Override
    public List<SSAccountPlan> findAll() {
        return db.getAccountPlans();
    }

    @Override
    public Optional<SSAccountPlan> findById(int id) {
        SSAccountPlan probe = new SSAccountPlan();
        probe.setId(id);
        return db.getAccountPlan(probe);
    }

    @Override
    public void add(SSAccountPlan plan) {
        db.addAccountPlan(plan);
    }

    @Override
    public void update(SSAccountPlan plan) {
        db.updateAccountPlan(plan);
    }

    @Override
    public void delete(SSAccountPlan plan) {
        db.deleteAccountPlan(plan);
    }
}

