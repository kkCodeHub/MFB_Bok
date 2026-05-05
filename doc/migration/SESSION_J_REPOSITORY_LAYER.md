# Session J - V2 Repository Layer Implementation (Steg 2.3 Phase 2)

**Status:** READY TO START  
**Date:** 2026-05-05  
**Estimated Effort:** 3-4 sessions (J, K, L)  
**Risk Level:** MEDIUM (first code integration with V2)  

---

## Background

Sessions A through I established:
- ✅ V2 schema created in `src/main/resources/sql/create_tables_v2.sql`
- ✅ Schema syntax validated by `SchemaV2ValidatorTest`
- ✅ Integration tests created for all core entities (Customer, Product, Supplier, Voucher, Invoice, Order, Tender, Credit Invoice, Periodic Invoice)

**What's Missing:** 
- Runtime code still uses V1 (OBJECT columns in HSQLDB).
- No application code paths adapted to read/write V2 tables.
- The V2 schema exists but is never instantiated during app startup.

Session J begins **Phase 2 of Steg 2.3**: Building the SQL-based repository layer for masterdata.

---

## Phase 2 Work: V2 Repository Implementation

### Primary Goal
Implement repository interfaces for masterdata (Customer, Product, Supplier) with SQL-based mapping to V2 tables.

### Why Repositories First?
1. **Low Risk:** Masterdata has no complex transactional dependencies.
2. **Validates V2 Schema:** Proves the V2 DDL works with application code.
3. **Unblocks Later Phases:** Voucher/Invoice/Order/etc. depend on Customer/Product matching IDs.

---

## Session J Tasks

### Task J.1: Wire V2 Schema Activation in `SSDB`

**Objective:** Runtime code can switch between V1 and V2 schemas via system property.

**Implementation:**
```java
// In SSDB.createNewTables()
if ("v2".equals(System.getProperty("fribok.schema.version"))) {
    // Load and execute create_tables_v2.sql
    executeSQLScript("create_tables_v2.sql");
} else {
    // Load and execute existing create_tables.sql (V1)
    executeSQLScript("create_tables.sql");
}
```

**Test:** 
- Unit test: `SSDBSchemaVersionTest`
  - Verify V1 schema loads when property unset.
  - Verify V2 schema loads when property = "v2".
  - Verify both schemas can coexist in different DB connections.

**Files to Update:**
- `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java` (~7700 lines)
- Create `src/test/java/.../SSDBSchemaVersionTest.java`

**Acceptance Criteria:**
- `mvn clean test -Dtest=SSDBSchemaVersionTest` passes.
- `mvn test -Dfribok.schema.version=v2` creates V2 tables.
- No errors when switching between versions.

---

### Task J.2: Implement CustomerRepository (SQL-based V2 read)

**Objective:** Read Customer masterdata from V2 table `tbl_customer` without Java serialization.

**Files:**
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/CustomerRepository.java` (interface)
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/v2/V2CustomerRepository.java` (SQL-based impl)

**Implementation Outline:**
```java
public class V2CustomerRepository implements CustomerRepository {
    private final Connection connection;

    @Override
    public List<SSCustomer> findAll(Integer companyId) throws SQLException {
        String sql = "SELECT * FROM tbl_customer WHERE companyid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<SSCustomer> customers = new ArrayList<>();
                while (rs.next()) {
                    customers.add(mapRowToCustomer(rs));
                }
                return customers;
            }
        }
    }

    private SSCustomer mapRowToCustomer(ResultSet rs) throws SQLException {
        SSCustomer customer = new SSCustomer();
        customer.setCustomerNr(rs.getString("number"));
        customer.setName(rs.getString("name"));
        // ... map all fields from V2 schema
        // including embedded address, currency, payment term, etc.
        return customer;
    }
}
```

**Test:** 
- `SSCustomerV2RepositoryTest` (similar to `SSCustomerV2IntegrationTest` but testing repository layer only).
- Mock or in-memory H2 for unit tests; full HSQLDB for integration.

**Acceptance Criteria:**
- `mvn test -Dtest=SSCustomerV2RepositoryTest` passes.
- All `SSCustomer` fields mapping correctly from V2.
- No dependency on OBJECT columns or Java serialization.

---

### Task J.3: Implement ProductRepository (SQL-based V2 read)

**Objective:** Read Product masterdata from V2 table `tbl_product`.

**Complexity:** Medium  
- Flat fields (number, name, prices, etc.) → straightforward SQL.
- Map `iDefaultAccounts` (legacy Map<SSDefaultAccount, Integer>) → V2 child table `tbl_product_account`.

**Files:**
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/ProductRepository.java` (interface)
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/v2/V2ProductRepository.java` (SQL impl)

**Additional Table:**
```sql
CREATE TABLE tbl_product_account (
    product_id INTEGER NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    account_number INTEGER NOT NULL,
    PRIMARY KEY (product_id, account_type),
    FOREIGN KEY (product_id) REFERENCES tbl_product(id)
);
```

**Test:** 
- `SSProductV2RepositoryTest`

**Acceptance Criteria:**
- `mvn test -Dtest=SSProductV2RepositoryTest` passes.
- Default accounts correctly loaded via child table join.

---

### Task J.4: Implement SupplierRepository (SQL-based V2 read)

**Objective:** Read Supplier masterdata from V2 table `tbl_supplier`.

**Complexity:** Medium (similar to product; address embedding, payment terms, etc.)

**Files:**
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/SupplierRepository.java` (interface)
- `src/main/java/se/swedsoft/bookkeeping/data/persistence/v2/V2SupplierRepository.java` (SQL impl)

**Test:** 
- `SSSupplierV2RepositoryTest`

**Acceptance Criteria:**
- `mvn test -Dtest=SSSupplierV2RepositoryTest` passes.

---

### Task J.5: Test Runtime Integration

**Objective:** Prove V2 repositories work alongside existing V1 `SSDB` code without conflicts.

**Test Class:** `SSMasterdataV2IntegrationTest`
```java
@Test
void readMasterdataFromV2WithRepositories() {
    // Setup: Create company, customers, products, suppliers in V2.
    // Act: Use V2CustomerRepository to fetch customer list.
    // Assert: Fetched data matches what was inserted.
    // Ensure V1 SSDB still works side-by-side for backward compatibility.
}
```

**Acceptance Criteria:**
- `mvn clean test -Dtest=SSMasterdataV2IntegrationTest -Dfribok.schema.version=v2` passes.
- V2 repositories can be instantiated and used without changes to GUI or other legacy code.

---

## Session J Checklist

- [ ] **J.1 Schema Activation**
  - [ ] `SSDBSchemaVersionTest` created and passing
  - [ ] V2 schema loads when `fribok.schema.version=v2`
  - [ ] V1 schema loads by default
  
- [ ] **J.2 CustomerRepository**
  - [ ] `V2CustomerRepository` implements `CustomerRepository` with pure SQL
  - [ ] All `SSCustomer` fields mapped (including addresses)
  - [ ] `SSCustomerV2RepositoryTest` passing
  - [ ] No dependency on serialization
  
- [ ] **J.3 ProductRepository**
  - [ ] `V2ProductRepository` with `tbl_product_account` child table
  - [ ] Default accounts loaded correctly
  - [ ] `SSProductV2RepositoryTest` passing
  
- [ ] **J.4 SupplierRepository**
  - [ ] `V2SupplierRepository` with address and term mappings
  - [ ] `SSSupplierV2RepositoryTest` passing
  
- [ ] **J.5 Integration Test**
  - [ ] `SSMasterdataV2IntegrationTest` verifies V2 and V1 coexist
  - [ ] `mvn clean test -Dfribok.schema.version=v2` passes all of above
  
- [ ] **Documentation**
  - [ ] UPDATE `SESSION_RESUME_CHECKLIST.md` line 54 with: "Session J klar - V2 repositories for masterdata implemented and tested."
  - [ ] UPDATE `OBJECT_COLUMN_MAPPING.md` with notes on which tables have working V2 implementations
  - [ ] UPDATE this file when Session J is complete

---

## Files to Create/Modify

### New Files
```
src/main/java/se/swedsoft/bookkeeping/data/persistence/
├── CustomerRepository.java (interface)
├── ProductRepository.java (interface)
├── SupplierRepository.java (interface)
└── v2/
    ├── V2CustomerRepository.java
    ├── V2ProductRepository.java
    └── V2SupplierRepository.java

src/test/java/se/swedsoft/bookkeeping/data/persistence/
├── SSCustomerV2RepositoryTest.java
├── SSProductV2RepositoryTest.java
├── SSSupplierV2RepositoryTest.java
└── SSMasterdataV2IntegrationTest.java

src/test/java/se/swedsoft/bookkeeping/data/system/
└── SSDBSchemaVersionTest.java

sql/
└── create_tables_v2_extended.sql (includes tbl_product_account child table)
```

### Modified Files
```
src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java
  - Add schema version check in createNewTables()
  - Load appropriate SQL script based on property
```

---

## Session J Success Criteria

✅ **BUILD:** `mvn clean compile` passes  
✅ **UNIT TESTS:** `mvn test` includes 5 new repository tests, all passing  
✅ **INTEGRATION:** `mvn test -Dfribok.schema.version=v2 -Dtest=SSMasterdataV2IntegrationTest` passing  
✅ **NO REGRESSIONS:** Existing V1 tests still pass (`SSDBCustomerRepositoryTest`, etc.)  
✅ **DOCUMENTATION:** Session checklist and next-steps updated  

---

## Notes

- **No GUI Changes Yet:** The repositories are wired but not yet called by GUI code. That's Session K.
- **Backward Compatibility:** V1 schema and code remain unchanged and functional until explicit cutover.
- **Risk Mitigation:** All new code is in `v2/` package. Easy to disable if needed.
- **Next Session (K):** Wire Repositories into `Bookkeeping.main()` and update one GUI panel to use V2 instead of V1 SSDB calls.

---

**Session J Start Date:** Ready  
**Estimated Completion:** 2-3 hours of focused work  
**Blocker Dependencies:** None (Sessions A-I complete)


