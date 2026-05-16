# Release 3 – Genomförandeplan

**Status**: Beslutad och aktiv  
**Startdatum**: 2026-05-16  
**Syfte**: Stabilisering av kontoplanflöde, enhetlig data-refresh och driftsäker leverans

---

## Scope för Release 3

### Inkluderat (MÅSTE)
1. **Enhetligt uppdateringsmönster i affärskritiska listfönster**
   - Nyskapat/uppdaterat/raderat objekt syns direkt utan omstart/bolagsbyte
   - Konsistent cache/repository-synk efter commit i SSDB

2. **Data-integritet och versionshantering**
   - Påbörjad mellan R2 och R3, färdigställs i R3:
     - Årsbaserad aktiv-status i kontoplan
     - Write-back och snapshot-hantering
   - Migrering verifierad för dataintegritet (0 förlust, reproducerbar)

3. **Dokumentation och release-beredskap**
   - `CHANGELOG.md` uppdaterad
   - Release notes klara
   - Operativ migreringsguide

### Pre-delivered (redan gjort mellan R2–R3, verifieras i R3)
- Refresh-mönster implementerat för prioriterade listfönster:
  - Autodist, Customer, Supplier, Product
  - PurchaseOrder, Voucher
  - InPayment, OutPayment
  - CreditInvoice, SupplierCreditInvoice, PeriodicInvoice
- Cache-synk i SSDB efter add/update/delete
- Frame-level refresh helpers tillagda

### Undantaget (KAN moves to R3.1)
- Ytterligare moderniseringar utanför kontoplan-kärnor
- Nya rapportfunktioner
- UI-kosmetik utan datakonsistens-effekt

---

## Genomförandeordning (faser)

### **Fas 0: Scope-låsning (styrning)**
- [ ] Bekräfta exakt scope för R3
- [ ] Märk pre-delivered delar som låsta (ej omarbete)
- [ ] Acceptera avgränsningar formellt
- **Gate**: Signerad scope-lista

### **Fas 1: Baslinje och inventering**
- [ ] Frys nuläge i kod/dokument för R3-spåret
- [ ] Inventera kvarvarande avvikelser mot "starkt uppdateringsmönster"
- [ ] Skapa prioriterad åtgärdslista
- [ ] Dokumentera beroenden och risker
- **Gate**: Komplett gap-lista med ansvar + ordning

### **Fas 2: Slutlig implementation av återstående R3-punkter**
- [ ] Åtgärda kvarvarande svaga uppdateringsflöden:
  1. Verifikat/betalning/faktura-nära flöden
  2. Masterdata (kund/leverantör/produkt)
  3. Övriga listfönster i scope
- [ ] Säkerställ enhetligt post-commit cache/refresh-mönster
- [ ] Testa varje ändrings-batch lokalt
- **Gate**: Alla MÅSTE-punkter implementerade

### **Fas 3: Verifiering och regression**
- [ ] Funktionsverifiering: CRUD i prioriterade listfönster
- [ ] Regression: kärnflöden (order, faktura, in-/utbetalning, verifikat)
- [ ] Dataintegritetskontroller: år/kontoplan-beroenden
- [ ] `mvn clean install` och `mvn test` grönt
- **Gate**: Inga öppna blocker/hög-kritiska fel

### **Fas 4: Releasekandidater och dokumentation**
- [ ] SkapaRC1 (Release Candidate 1)
- [ ] Uppdatera `CHANGELOG.md`:
  - Vad som levererats i R3
  - Vad som var pre-delivered mellan R2–R3
  - Vad som explicit inte ingår
- [ ] Uppdatera release notes och migreringssteg
- [ ] Granskning och signering av dokumentation
- **Gate**: RC + release-docs godkänd

### **Fas 5: Go/No-Go och release**
- [ ] Formellt Go/No-Go-möte
- [ ] Checklista färdig (blockers = 0)
- [ ] Släpp R3
- [ ] Hypercare-fönster: verifieringschecklista för drift
- **Gate**: Produktionsacceptans

---

## Acceptanskriterier (Definition of Done)

1. **Bygg & test**
   - `mvn clean install` passerar i CI

2. **Funktionstest**
   - Create/update/delete syns direkt i listfönster samma session
   - Börja med prioriterad lista (autodist, customer, supplier, product, voucher, payment, invoice)
   - Sluta med övriga scope

3. **Regression**
   - Inga kritiska regressionsfel i order/faktura/betalning/verifikat

4. **Data-integritet**
   - Migrering från R2→R3 ger 0 dataförlust
   - Gamla och nya miljöer kan upgraderas säkert

5. **Dokumentation**
   - `CHANGELOG.md` uppdaterad
   - Operativ migreringsguide inlåst

6. **Go/No-Go**
   - Alla MÅSTE-punkter uppfyllda
   - Öppna defects: max normala, inga blockers

---

## Beslutspunkter

- **efter Fas 0**: Scope låst → går vi vidare?
- **efter Fas 1**: Gap-lista & prioritering → är planen realistisk?
- **efter Fas 2**: Implementation klar → räcker tiden för test?
- **efter Fas 3**: Regression godkänd → klara för RC?
- **efter Fas 4**: RC + docs godkänd → Go eller No-Go?
- **Fas 5**: Release och verifiering

---

## Noteringar

- Pre-delivered delar mellan R2–R3 kan krävta final tuning/verifiering; de är inte helt färdiga förrän R3-test.
- Migreringstester måste köras mot realistisk staging-kopia före produktionsrelease.
- Hypercare-fönster (1–2 veckor efter release) för snabb defect-feedback.

---

**Senast uppdaterad**: 2026-05-16  
**Godkänd av**: [väntande]

