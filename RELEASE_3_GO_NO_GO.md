# Release 3 – Go/No-Go

## Syfte
Dokumentera beslutspunkten för Fas 5 och säkerställa att release kan tas i drift på kontrollerat sätt.

## Go/No-Go-kriterier
- `mvn clean install` passerar
- Inga blockerande regressionsfel i order, faktura, betalning eller verifikat
- CRUD-flöden uppdaterar listor direkt i samma session
- Data-integritet är verifierad för R2 → R3
- Release notes och migreringssteg är uppdaterade
- Hypercare-plan finns för uppföljning efter release

## Beslutsmall
- **Go**: Alla MÅSTE-punkter uppfyllda och inga blockers kvar
- **No-Go**: Minst en blockerande avvikelse eller oacceptabel regressionsrisk

## Sign-off
- Teknisk verifiering: godkänd (`mvn clean install` passerade)
- Verksamhetsverifiering: godkänd på dokumentationsnivå
- Driftacceptans: godkänd för hypercare-uppföljning

## Nästa steg efter beslut
- Vid Go: Release 3 är klar för nästa steg och hypercare-fönstret används för driftuppföljning
- Vid No-Go: åtgärda blockers och kör om verifiering


