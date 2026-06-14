# Dokumentöversikt - Decimalantal (heltal*10)

Detta dokument beskriver syftet med de framtagna planerings- och releasefilerna för införande av decimalantal med intern modell `heltal*10`.

## Rekommenderad läsordning

1. `DECIMAL_QUANTITY_PLAN.md`
2. `DECIMAL_QUANTITY_SPRINT_CHANGE_LIST.md`
3. `DECIMAL_QUANTITY_RISKS_AND_DEPENDENCIES.md`
4. `DECIMAL_QUANTITY_GO_NO_GO_TEMPLATE.md`
5. `DECIMAL_QUANTITY_GO_NO_GO_EXAMPLE.md`

---

## 1. `DECIMAL_QUANTITY_PLAN.md`

### Syfte
Huvuddokumentet för målbilden.

### Innehåll
- Beslutade regler för antal som `int*10`
- Visning alltid med 1 decimal
- Prisregler (`HALF_UP`, 2 decimaler)
- Produktregel **hela antal endast**
- Lager-, frakt-, vikt- och volymregler
- Övergripande datamodell, migreringsprincip och testmål

### Används av
- Produktägare
- Arkitekt/tekniskt ansvarig
- Utveckling
- QA

### När används dokumentet
- När scope och målbild ska förstås eller bekräftas
- Som referens när implementationen påbörjas

---

## 2. `DECIMAL_QUANTITY_SPRINT_CHANGE_LIST.md`

### Syfte
Konkret genomförandeplan sprint-för-sprint.

### Innehåll
- Implementeringsordning
- Berörda tabeller
- Berörda domänklasser
- Berörda persistensklasser
- Berörda GUI-skärmar/paneler
- Berörda rapporter/integrationer

### Används av
- Utvecklingsteam
- Tech lead
- Projektledning

### När används dokumentet
- Vid sprintplanering
- Vid uppdelning i stories/tasks
- Vid prioritering av beroenden

---

## 3. `DECIMAL_QUANTITY_RISKS_AND_DEPENDENCIES.md`

### Syfte
Identifiera risker, beroenden och exit-kriterier per sprint.

### Innehåll
- Beroenden per sprint
- Huvudrisker per sprint
- Motåtgärder
- Exit-kriterier
- Tvärgående riskindikatorer

### Används av
- Projektledning
- Tech lead
- QA
- Releaseansvarig

### När används dokumentet
- Inför sprintstart
- Vid riskgenomgångar
- Som underlag i statusmöten och releasebeslut

---

## 4. `DECIMAL_QUANTITY_GO_NO_GO_TEMPLATE.md`

### Syfte
Tom beslutsmall för release-/driftsättningsmöte.

### Innehåll
- Blockerande krav
- Risk- och undantagstabell
- Testsammanfattning
- Migreringssammanfattning
- Beslutspunkt GO/NO-GO
- Kort exekveringsplan och omplaneringsdel

### Används av
- Releaseansvarig
- QA
- Teknisk ansvarig
- Produktägare

### När används dokumentet
- Inför stage- eller produktionsrelease
- På själva Go/No-Go-mötet

---

## 5. `DECIMAL_QUANTITY_GO_NO_GO_EXAMPLE.md`

### Syfte
Visa hur den tomma Go/No-Go-mallen kan fyllas i.

### Innehåll
- Realistiska exempelvärden
- Exempel på risker
- Exempel på test- och migreringsstatus
- Exempel på beslutskommentar och smoke-testpaket

### Viktigt
Detta dokument är ett **exempel**, inte verifierad produktionsstatus.

### Används av
- Releaseansvarig
- Projektledning
- QA

### När används dokumentet
- Som referens innan första riktiga Go/No-Go-mötet
- För att kalibrera vilken detaljnivå som förväntas i beslutsunderlaget

---

## Rekommenderat praktiskt arbetssätt

### Före implementation
- Läs och godkänn `DECIMAL_QUANTITY_PLAN.md`
- Bryt ned arbetet med `DECIMAL_QUANTITY_SPRINT_CHANGE_LIST.md`
- Bedöm genomföranderisk med `DECIMAL_QUANTITY_RISKS_AND_DEPENDENCIES.md`

### Under implementation
- Följ sprintordningen i ändringslistan
- Uppdatera riskdokumentet när nya beroenden eller risker upptäcks

### Inför release
- Kopiera `DECIMAL_QUANTITY_GO_NO_GO_TEMPLATE.md`
- Fyll i aktuell status för test, migrering och risker
- Använd `DECIMAL_QUANTITY_GO_NO_GO_EXAMPLE.md` som referens för detaljnivå

---

## Kort sammanfattning

- `DECIMAL_QUANTITY_PLAN.md` = **vad** som ska uppnås
- `DECIMAL_QUANTITY_SPRINT_CHANGE_LIST.md` = **hur och i vilken ordning** det genomförs
- `DECIMAL_QUANTITY_RISKS_AND_DEPENDENCIES.md` = **vad som kan gå fel och vad som måste vara klart**
- `DECIMAL_QUANTITY_GO_NO_GO_TEMPLATE.md` = **beslutsmall inför release**
- `DECIMAL_QUANTITY_GO_NO_GO_EXAMPLE.md` = **exempel på ifylld beslutsmall**

