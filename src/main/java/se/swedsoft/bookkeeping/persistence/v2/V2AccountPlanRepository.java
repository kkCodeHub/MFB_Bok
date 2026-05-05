package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.AccountPlanRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link AccountPlanRepository} implementation backed by {@link SSDB}.
 */
public class V2AccountPlanRepository implements AccountPlanRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    public V2AccountPlanRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2AccountPlanRepository requires fribok.schema.version=v2");
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

