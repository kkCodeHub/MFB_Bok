# Release 3 – Release Notes

## Version
Release 3 RC1

## Sammanfattning
Release 3 fokuserar på enhetliga refresh-flöden, stabilisering av affärskritiska listfönster och tydligare driftberedskap efter release.

## Levererat i Release 3
- Enhetligt uppdateringsmönster i prioriterade listfönster
- Frame-level refresh för affärskritiska vyer
- Konsoliderad hantering av import, delete och cache-synk i relevanta fönster
- Förberedda releaseartefakter för Go/No-Go och hypercare

## Pre-delivered mellan R2 och R3
- Prioriterade refresh-mönster för centrala listfönster
- Cache-synk i SSDB efter add/update/delete
- Frame-level refresh helpers

## Explicit utanför Release 3
- Nya funktionella domänflöden utanför R3-scope
- Större UI-moderniseringar utan datakonsistens-effekt
- Vidare borttagning av V1-kod innan feature parity är verifierad

## Drift och verifiering
- Hypercare-fönster används för tät uppföljning efter release
- Loggar granskas via befintlig Logback-konfiguration
- Kritiska CRUD-flöden ska kontrolleras i skarp drift

