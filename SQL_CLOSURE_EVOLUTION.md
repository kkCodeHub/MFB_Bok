# SSDB.java SQL-stängning Evolution

## Timeline

```
V1 CODEBASE (2000-2020s)
  ↓
  × × ×  InitialSnapshot (2026-05-04)
  │      └─ Manual close-calls already present
  │         (result of V1 → Git import)
  │
  V2 MIGRATION BEGINS (May 2026)
  │
  ├─ 2026-05-04~20:00 — 577732a: Wire schema selection
  │  └─ First V2 methods with try-with-resources
  │
  ├─ 2026-05-05 — 118b4de: Customer CRUD V2
  │  ├─ NEW methods: try-with-resources ✓
  │  └─ OLD methods: still manual close ✗
  │
  ├─ 2026-05-05 — 59a72ff: Product/Supplier V2
  │  ├─ NEW methods: try-with-resources ✓
  │  └─ OLD methods: still manual close ✗
  │
  ├─ 2026-05-06 — Multiple domain migrations
  │  ├─ NEW: try-with-resources ✓
  │  └─ OLD: manual close ✗
  │
  └─ 2026-05-07~20:47 — 5d8d9c8: Invoice cutover (LATEST)
     ├─ MIXED CODEBASE (both styles coexist)
     ├─ NEW V2 methods: try-with-resources ✓
     └─ OLD V1 methods: manual close ✗


CURRENT STATE (2026-05-27)
┌─────────────────────────────────────┐
│ SSDB.java — Blandade Stål           │
├─────────────────────────────────────┤
│ V1-arv-metoder (~70%):              │
│  • startupLocal()         ✗ manual  │
│  • checkCreateExampleCompany() ✗    │
│  • hasAccountRowsForYear() ✗ manual │
│  • getCompanies()         ✗ manual  │
│  • getCompany()           ✗ manual  │
│  • ... flera dussintals ... ✗       │
│                                     │
│ V2 nya metoder (~30%):              │
│  • shouldSeedV2DemoDataV2() ✓ try   │
│  • getCompanyByNameV2()    ✓ try    │
│  • getAccountingYearByRangeV2() ✓   │
│  • executeSqlScriptResource() ✓     │
│  • logV2DemoSeedSummary()  ✓ try    │
│  • loadAccountPlanSnapshotV2() ✓    │
│  ... (några V2-metoder) ... ✓       │
└─────────────────────────────────────┘
```

---

## Mönster-jämförelse

### V1-arv (Manual close)
```java
// Old V1 pattern — RISK: If exception between next() and close():
// ResultSet may leak

try {
    if (iLastCompany != null) {
        iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_company WHERE id=?");
        iStatement.setObject(1, iLastCompany);
        iResultSet = iStatement.executeQuery();
        
        if (iResultSet.next()) {
            SSNewCompany iCompany = (SSNewCompany) iResultSet.getObject("company");
            setCurrentCompany(iCompany);
        }
        
        iResultSet.close();    // ← May not execute if exception above
        iStatement.close();    // ← May not execute if exception above
    }
} catch (SQLException e) {
    // Resources leaked if exception thrown before close calls!
}
```

### V2-modern (Try-with-resources)
```java
// New V2 pattern — SAFE: Resources guaranteed closed

try (PreparedStatement iStatement = iConnection.prepareStatement(
        "SELECT * FROM tbl_company WHERE id=?")) {
    
    iStatement.setObject(1, iLastCompany);
    
    try (ResultSet iResultSet = iStatement.executeQuery()) {
        if (iResultSet.next()) {
            SSNewCompany iCompany = mapCompanyV2(iResultSet);
            setCurrentCompany(iCompany);
        }
    }
} catch (SQLException e) {
    // Resources GUARANTEED closed, even if exception thrown
    LOG.error("Unexpected error", e);
}
```

### Key Differences

| Aspekt | V1-manuell | V2 try-with |
|--------|-----------|-------------|
| **Exception safety** | ❌ Leak risk | ✅ Guaranteed close |
| **Code lines** | ~5-6 per statement | ~2-4 per statement |
| **Readability** | Boilerplate-heavy | Clean, declarative |
| **Java requirement** | 1.0+ | 7.0+ (try-with-resources) |
| **Compiler support** | All | Java 7+ |

---

## Förnamn

### Anledning till V1-mönster
1. **Java-version:** Kod skrevs för J2SE 1.4–1.6 (innan Java 7, 2011)
2. **Best practice då:** Manual resource management var standard
3. **Migration-omöjlig:** V1-systemet ran on äldrä JVM-versioner

### Varför V2 använder try-with-resources
1. **Java 21 target** — modernt, well-supported
2. **AGENTS.md guidance** — explicit rekommendation
3. **Resource leak prevention** — exception-safe design
4. **Codestylestandard** — established pattern for new code

---

## Rekommendation

**Migrera systematiskt:**
- Fokusera på ofta-ändrade eller exception-känsliga metoder först
- Verktyg tillgängliga: IDE refactorings, grep-patterns, eller Copilot-assistans
- Risknivå: **LAG** — try-with-resources är stabil sedan 2011
- Fördelar: Resurshantering, läsbarhet, kodstandard-compliance

Se `SQL_CLOSURE_ANALYSIS.md` för detaljerad ursprung och strategi.

