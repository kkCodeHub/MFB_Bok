# Ny trad: status for 8->12 domaner och SSDB-delning

Datum: 2026-06-02
Syfte: snabb overlamning for att kunna starta en ny trad och fortsatta arbetet utan att tappa kontext.

## 1) Malsattning

- Gaa fran 8 till 12 domaner enligt dokumenterad malarkitektur.
- Dela upp ansvar i dagens `SSDB` till tydliga domaner/tjanster.
- Langa sikten: `SSDB` ska vara overgangsadapter, inte plats for ny affarslogik.

Planerade 12 domaner (malbild):
1. Company/Year
2. Accounting Core
3. Masterdata
4. Sales
5. Purchase
6. Payments
7. Product
8. OwnReport
9. Project & ResultUnit
10. Inventory & Deliveries
11. Event/Trigger Sync
12. System/Config

## 2) Vad som ar klart (viktigast)

### Partition 8 B & C (senaste stora delsteget)

- Konverteringslager borttaget i centrala read/write-vagar (7 identifierade anrop ersatta).
- Direkt tenths-semantik etablerad i relevanta floden.
- Byggstatus i rapporterna: kompilering utan fel i den verifierade korningen.
- Grundtesterna i delsteget passerade i rapporterad korning.

Praktisk effekt:
- Mindre dubbelkonvertering och mindre risk for semantisk drift mellan lager.
- Baserad grund for fortsatt uppdelning av `SSDB` och vidare domanrenodling.

### Release 3-sparet (stabilisering av uppdateringsfloden)

- Scope, gap-analys och faser 0-5 genomforda.
- Hogprioriterade dialog/frame-floden migrerade till enhetligt uppdateringsmonster.
- Go/No-Go beslutat som Go, med hypercare-plan efter release.

## 3) Pagande arbete

- UI-lagret behover fortsatt anpassning dar visning fortsatt kraver division med 10 i presentationsgransen.
- `SSInvoiceMath` och narliggande berakningslogik behover verifieras for tenths-semantik.
- Delar av testsviten behover justerade for nya forvantade varden.
- Date-migrering (mot `LocalDate`) ar pa gang men inte helt klar i alla granssnytor.

## 4) Kvarvarande risker/blockerare

- Precision/avrundning i affarskritiska floden (faktura, verifikat, summeringar).
- Granssnytor mot externa format/integrationer som forvantar tidigare decimalbeteende.
- Stor teknisk skuld i `SSDB` (ansvarstung och stor klass), vilket okar regressionsrisk vid flytt av ansvar.
- Sakerhets- och beroenderisker dokumenterade i moderniseringssparet (inkl. rapportering kring Jasper/iText-sparet).
- Persistenssparet: V2-schema finns dokumenterat men runtime-bootstrap ar inte fullt kopplat i huvudflodet.

## 5) Rekommenderad nasta arbetsordning i ny trad

1. **Lasa in och bekrafta nulage**
   - Verifiera att senaste branch innehaller Partition 8 B & C-andringar enligt rapport.
2. **Sluta quantiteskedjan end-to-end**
   - Sakerstall tenths-semantik i UI -> berakning -> persistens -> rapport/export.
3. **Mala bort SSDB-ansvar stegvis**
   - Flytta en doman i taget till tydligt ansvarigt lager, med kontraktstester runt flytten.
4. **Harda regressionstester**
   - Fokus pa faktura, kreditfaktura, verifikat, kontoplan/arsbyte.
5. **Parallellt: moderniserings- och sakerhetsspår**
   - Fortsatt Date-migrering och planerad beroendehardning.

## 6) Konkret "startpaket" for nasta trad

Anvand denna checklista direkt i nasta trad:

- [ ] Bekrafta scope: "8->12 domaner + SSDB-delning, stegvis utan funktionsregression".
- [ ] Lista vilka domaner som redan ar tekniskt separerade vs kvar i `SSDB`.
- [ ] Verifiera tenths-floden i minst 3 kritiska affarsfall (faktura, kreditfaktura, verifikat).
- [ ] Identifiera nasta 1-2 konkreta flyttar av ansvar ut ur `SSDB`.
- [ ] Definiera minsta testpaket som maste vara gront efter varje flytt.

## 7) Kallor (for snabb aterlasning)

- `SSDB_12_DOMAIN_ARCHITECTURE_PLAN.md`
- `PARTITION_8_B_C_FINAL_REPORT.md`
- `PARTITION_8_B_C_STATUS_REPORT.md`
- `RELEASE_3_PLAN.md`
- `RELEASE_3_GO_NO_GO.md`
- `RELEASE_3_RELEASE_NOTES.md`
- `V2_DOMAIN_MODEL_TARGET.md`
- `V2_PROJECT_REVIEW_REPORT.md`
- `DETAILED_MODERNIZATION_PLAN.md`
- `MODERNIZATION.md`

---

Kort slutsats:
Projektet har passerat en viktig milstolpe dar konverteringslagret i centrala floden ar borttaget och grunden for 12-domansmodellen ar lagd. Nasta steg i ny trad bor fokusera pa att stanga tenths-kedjan end-to-end, minska `SSDB`-ansvar doman for doman, och halla hog regressionstackning i affarskritiska floden.

