# Release 3 RC1

## Syfte
Denna RC1 sammanfattar Release 3 efter Fas 4 och fungerar som underlag för Go/No-Go.

## Ingår i RC1
- Enhetliga refresh-flöden i prioriterade listfönster
- Konsoliderad frame-level refresh för affärskritiska vyer
- Dokumenterad migrering för kvarvarande V1/V2-övergångar
- Uppdaterad releaseplan och changelog

## Explicit utanför RC1
- Nya funktionella domänflöden utanför Release 3-scope
- Större UI-moderniseringar utan datakonsistens-effekt
- Vidare borttagning av V1-kod innan feature parity är verifierad

## Migreringssteg
1. Verifiera att ändringarna är gröna med `mvn test`.
2. Kör full release-gate med `mvn clean install`.
3. Granska att prioriterade CRUD-flöden använder V2 där det är avsett.
4. Bekräfta att kvarvarande V1 endast finns som legacy-stöd där planen tillåter det.

## Sign-off-kriterier
- RC1 byggs utan fel
- Dokumentationen är uppdaterad
- Inga blockerande regressionsfel i kärnflöden
- Beslut om Fas 5 kan tas på tydligt underlag

## Nästa steg
Efter RC1 går projektet till Fas 5: Go/No-Go och release.
