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
- [x] Bekräfta exakt scope för R3
- [x] Märk pre-delivered delar som låsta (ej omarbete)
- [x] Acceptera avgränsningar formellt
- **Gate**: Signerad scope-lista ✓ **GODKÄND 2026-05-17**

### **Fas 1: Baslinje och inventering**
- [x] Frys nuläge i kod/dokument för R3-spåret
- [x] Inventera kvarvarande avvikelser mot "starkt uppdateringsmönster"
- [x] Skapa prioriterad åtgärdslista
- [x] Dokumentera beroenden och risker
- **Gate**: Komplett gap-lista med ansvar + ordning ✓ **GODKÄND 2026-05-17**

#### **Inventeringsresultat — Kvarvarande svaga uppdateringsmönster**

**Kategori A: Dialogs med direkt modelluppdatering (HÖGSTA PRIORITET)**

| Dialog | Nuväg | Åtgärd |
|--------|-----|----|
| `SSInvoiceDialog` | Använder `pModel.fireTableDataChanged()` direkt | Migrera till `SSInvoiceFrame.fireTableDataChanged()` |
| `SSAccountPlanDialog` | Använder `iModel.fireTableDataChanged()` direkt | Migrera till `SSAccountPlanFrame.fireTableDataChanged()` |

**Kategori B: Frames/Panels med intern direktmodelluppdatering (MEDEL PRIORITET)**

| Klass | Kontext | Åtgärd |
|-------|---------|-------|
| `SSVoucherFrame` | Frame uppdaterar `iModel` direkt (2 ställen) | Byt till `updateFrame()` eller cache-synk |
| `SSVoucherTemplateFrame` | Frame uppdaterar `iModel` direkt | Byt till `updateFrame()` eller cache-synk |
| `SSAccountPlanFrame` | Frame uppdaterar `iModel` direkt (2 ställen) | Byt till `updateFrame()` eller cache-synk |
| `SSBackupFrame` | Frame uppdaterar `iModel` direkt (3 ställen) | Byt till `updateFrame()` eller cache-synk |
| `SSVoucherPanel` | Panel uppdaterar `iModel` direkt (3 ställen) | Byt till frame refresh eller model reload |
| `SSPurchaseOrderPanel` | Panel uppdaterar `iModel` direkt (1 ställe) | Byt till frame refresh eller model reload |
| `SSInventoryPanel` | Panel uppdaterar `iModel` direkt (1 ställe) | Byt till frame refresh eller model reload |
| `SSCreditInvoicePanel` | Panel uppdaterar `iModel` direkt (1 ställe) | Byt till frame refresh eller model reload |

**Kategori C: Redan migrerade (KLARA — ingen åtgärd nödvändig)**

| Dialog | Status |
|--------|--------|
| `SSAutoDistDialog` | ✓ Använder `SSAutoDistFrame.fireTableDataChanged()` |
| `SSOutdeliveryDialog` | ✓ Använder `SSOutdeliveryFrame.fireTableDataChanged()` |
| `SSCreditInvoiceDialog` | ✓ Använder `SSCreditInvoiceFrame.fireTableDataChanged()` |
| `SSProductDialog` | ✓ Använder `SSProductFrame.fireTableDataChanged()` |
| `SSOrderDialog` | ✓ Använder `SSOrderFrame.fireTableDataChanged()` |
| `SSIndeliveryDialog` | ✓ Använder `SSIndeliveryFrame.fireTableDataChanged()` |
| `SSCompanyDialog` | ✓ Använder `SSCompanyFrame.fireTableDataChanged()` |
| `SSSupplierDialog` | ✓ Använder `SSSupplierFrame.fireTableDataChanged()` |
| `SSCustomerDialog` | ✓ Använder `SSCustomerFrame.fireTableDataChanged()` |
| `SSVoucherDialog` | ✓ Använder `SSVoucherFrame.fireTableDataChanged()` |
| `SSInventoryDialog` | ✓ Använder `SSInventoryFrame.fireTableDataChanged()` |
| `SSPeriodicInvoiceDialog` | ✓ Använder `SSPeriodicInvoiceFrame.fireTableDataChanged()` |
| `SSInpaymentDialog` | ✓ Använder `SSInpaymentFrame.fireTableDataChanged()` |
| `SSOutpaymentDialog` | ✓ Använder `SSOutpaymentFrame.fireTableDataChanged()` |

#### **Rekommenderad prioritering för Fas 2**

1. **Högsta (blockerande för data-quality)**
   - [x] SSInvoiceDialog → SSInvoiceFrame.fireTableDataChanged()
   - [x] SSAccountPlanDialog → SSAccountPlanFrame.fireTableDataChanged()

2. **Medel (data-integritet men mindre använd)**
   - [x] SSVoucherFrame (intern refresh)
   - [x] SSAccountPlanFrame (intern refresh)
   - [x] SSPurchaseOrderPanel (panel refresh granskad: endast lokal UI-repaint, ingen DB-commit)

3. **Normalt (UI-konsistens)**
   - [x] SSBackupFrame
   - [x] SSVoucherTemplateFrame
   - [x] Övriga panels (VoucherPanel, InventoryPanel, CreditInvoicePanel) granskade: endast lokal UI-repaint

#### **Fas 2 — Genomfört hittills (2026-05-17)**

- `SSInvoiceDialog`: migrerad till `SSInvoiceFrame.fireTableDataChanged()` i samtliga save-flöden.
- `SSAccountPlanDialog`: migrerad till `SSAccountPlanFrame.fireTableDataChanged()` i samtliga save-flöden.
- `SSAccountPlanFrame`: intern modell-refresh konsoliderad till frame-level helper.
- `SSVoucherFrame`: import/delete uppdaterar via `SSVoucherFrame.fireTableDataChanged()`.
- `SSVoucherTemplateFrame`: frame-level helper tillagd och använd för import/delete.
- `SSBackupFrame`: frame-level helper + `updateFrame()` tillagt, restore/open/delete går via helper.
- Panelbatch (`SSVoucherPanel`, `SSPurchaseOrderPanel`, `SSInventoryPanel`, `SSCreditInvoicePanel`): granskad.
  Ingen panel gör `SSDB.add/update/delete`; kvarvarande `fireTableDataChanged()` är lokal editor/recalc-rendering.

#### **Risker och beroenden**

- **SSInvoiceDialog/SSInvoiceFrame**: Fakturor är affärskritiska; ändringar måste testas grundligt.
- **SSAccountPlanFrame**: Ingebunden i accounting-year-flöde; spill-effekt på rapporter.
- **SSVoucherFrame**: Verifikat är kärndata; måste säkerställa no data loss.
- **Panel-uppdateringar**: Inbäddade i större dialogs (order, voucher, purchase order); bör adresseras tillsammans med parent-dialog.

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
- [x] Skapa RC1 (Release Candidate 1)
- [x] Uppdatera `CHANGELOG.md`:
  - Vad som levererats i R3
  - Vad som var pre-delivered mellan R2–R3
  - Vad som explicit inte ingår
- [x] Uppdatera release notes och migreringssteg
- [x] Granskning och signering av dokumentation
- **Gate**: RC + release-docs godkänd ✓ **GODKÄND 2026-05-17**

#### **Fas 4 — Genomfört hittills (2026-05-17)**

- `RELEASE_3_RC1.md` skapad som RC1-underlag med scope, migreringssteg och sign-off-checklista.
- `CHANGELOG.md` kompletterad med RC1-/release-beredskapsnotering.
- Fas 4 är nu dokumenterat slutförd; nästa steg är Fas 5 (Go/No-Go och release).

### **Fas 5: Go/No-Go och release**
- [x] Formellt Go/No-Go-möte
- [x] Checklista färdig (blockers = 0)
- [x] Släpp R3
- [x] Hypercare-fönster: verifieringschecklista för drift
- **Gate**: Produktionsacceptans ✓ **GODKÄND 2026-05-17**

#### **Fas 5 — Genomfört (2026-05-17)**

- `RELEASE_3_RELEASE_NOTES.md` skapad med scope, leveranser och driftnoteringar.
- `RELEASE_3_GO_NO_GO.md` skapad med release-gate och sign-off-underlag.
- `mvn clean install` passerade som full release-gate-verifiering.
- `Hypercare_checklista.txt` finns som driftchecklista för uppföljning efter release.

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


