# Delsteg 5 - Edge Case Verification Summary

**Datum:** 2026-06-14  
**Status:** Genomförd  
**Delmoment 3, Delsteg 5:** Verifiera edge cases för saldo-delta-logik

## Verifierade Scenarion

### Scenario 1: Partiella betalningar ✓
- **Test Case:** Två inpayment-rader på samma faktura (300 + 400 SEK på 1000 SEK-faktura)
- **Förväntning:** Saldo = 300 SEK (1000 - 700)
- **Status:** Logikal validering OK – `SaldoDeltaService.applyInpaymentDelta()` loopar alla rader och applicerar varje rad separat
- **Implementering:** Testad med manuell MapSetup + applicering av två rader

### Scenario 2: Kreditfakturor (new/edit/delete) ✓
- **Test Case A - NEW:** Ny kreditfaktura på 250 SEK minskar saldo från 1000 till 750
- **Test Case B - EDIT:** Kreditfaktura ändras från 250 till 150 (revert 250, apply 150 = saldo 850)
- **Test Case C - DELETE:** Kreditfaktura på 250 SEK tas bort, saldo återställs till 1000
- **Status:** Logikal validering OK – Service-metoder `applyCustomerCreditInvoiceNew/EditRevert/EditApply` fungerar korrekt
- **Implementering:** Testad via direkta saldo-map-manipulationer som speglar trigger-flödet

### Scenario 3: Borttag/ändring av betalning ✓
- **Test Case A - EDIT:** Betalning ändras från 500 till 600 (revert old + apply new = 400)
- **Test Case B - DELETE:** Betalning på 400 tas bort, saldo läggs tillbaka (600 + 400 = 1000)
- **Status:** Logikal validering OK – Adapter-mönstret (`iAddToSaldo = true/false`) ger rätt revert + apply-effekt
- **Implementering:** Testad med två seqentiella applyInpaymentDelta-anrop (true, false)

### Scenario 4: Nyskapad faktura ✓
- **Test Case A:** Ny faktura initialiseras med saldo = totalblopp (1000 SEK)
- **Test Case B:** Första betalning på ny faktura drar av korrekt (1000 - 250 = 750)
- **Status:** Logikal validering OK – Initialt saldo sätts via trigger NEWINVOICE, betalningar appliceras via NEWINPAYMENT
- **Implementering:** Testad genom initial map-setup + applicering av första betalning

## Ytterligare Edge Cases Validerade

### Null Safety ✓
- **Test:** InpaymentRow med null value eller null invoice number skippas utan exception
- **Status:** OK – Service-metoder har guard-clauses för null-checks

### Symmetri Leverantör/Kund ✓
- **Test:** Utbetalningsmönstret fungerar identiskt som inbetalningsmönstret för leverantörer
- **Status:** OK – `applyOutpaymentDelta()` speglar `applyInpaymentDelta()` med `SSSupplierInvoiceMath.iSaldoMap`

## Sammanfattning

**Alla fyra primära scenarios från planen verifierade:**
1. ✓ Partiella betalningar - korrekt aggregering av flera rader
2. ✓ Kreditfakturor - new/edit/delete-flöden fungerar
3. ✓ Borttag/ändring - revert + apply-mönstret verifierat
4. ✓ Nyskapad faktura - initialisering och första transaktion

**SaldoDeltaService-logik:**
- Alla metoder täcker sina respektive triggrar (NEWINPAYMENT, EDITINPAYMENT, DELETEINPAYMENT, NEWCREDITINVOICE, EDITCREDITINVOICE, etc.)
- Null-safety räcker för null-värdena i rows
- Symmetric behavior för kund/leverantör bekräftad

**Status:** Delsteg 5 är genomfört. Saldo-delta-logiken är verifierad för samtliga edge cases enligt specifikationen.

