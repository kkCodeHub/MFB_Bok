# V2 Domain Model Target

Datum: 2026-05-29

## Syfte

Detta dokument förtydligar målmodellen för V2 efter cleanup i 3.1 där
`SSNew*`-modellerna är den enda aktiva modellvägen i applikationskod.

## Målmodell i V2

Följande klasser är de föredragna V2-modellerna i ny och migrerad kod:

- `SSNewCompany`
- `SSNewAccountingYear`
- `SSNewProject`
- `SSNewResultUnit`

## Legacy-modeller som fasas ut

Följande klasser betraktas som legacy och ska inte vara förstahandsval i ny kod:

- `SSCompany`
- `SSAccountingYear`
- `SSProject`
- `SSResultUnit`

De behålls tills vidare endast som kompatibilitetsskal för:

- kompatibilitet med äldre serialiserad data
- begränsad bakåtkompatibilitet under avvecklingsperioden

### Status efter 3.2 batch 3

- `SSProject` är reducerad till ett tunt kompatibilitetsskal ovanpå V2-beteende.
- `SSResultUnit` är reducerad till ett tunt kompatibilitetsskal ovanpå V2-beteende.
- `SSAccountingYear` är reducerad till en minimal serialiseringsstub (legacy-kompatibilitet).
- `SSCompany` är reducerad till en minimal serialiseringsstub (legacy-kompatibilitet).
- Samtliga fyra legacy-modeller är nu isolerade till kompatibilitetsgränsen och ska inte användas i aktiv V2-kod.

## Praktiska regler

1. Ny och befintlig aktiv kod ska använda `SSNew*`-modellerna.
2. Legacy-klasser får bara finnas kvar som isolerade kompatibilitetsklasser.
3. Nya API:er, repository-metoder och GUI-flöden ska enbart exponera `SSNew*`-typer.
4. Legacy -> V2-bryggkonstruktörer ska inte återinföras.

## Nästa steg

1. Håll legacy-referenser innanför kompatibilitetsgränsen via test/CI-kontroller.
2. Planera slutlig borttagning av legacy-klasser när extern bakåtkompatibilitet inte längre krävs.
3. Fortsätt V2-utveckling utan nya beroenden till legacy-modeller.
