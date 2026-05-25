# Spezifikation v5 und Codex-Prompt: Jenkins-Plugin fuer GRETL Datenportal Jobs

Arbeitsname / sichtbarer Name: **GRETL Datenportal Jobs**  
Technischer Name: `jenkins-gretl-datenportal-plugin`  
Jenkins-URL: `/gretl-datenportal`  
Stand: 2026-05-23  
Version: 5  
Zielgruppe: ChatGPT Codex / LLM-Agent / Entwickler

## Verwendung dieser Datei

Diese Datei ist Spezifikation und operative LLM-Anweisung in einem Dokument.

Fuer ChatGPT Codex oder einen anderen LLM-Agenten gilt:

1. Zuerst die gesamte Spezifikation lesen.
2. Danach den Abschnitt **Codex-Auftrag / LLM-Anweisung** als operative Arbeitsanweisung verwenden.
3. Nicht versuchen, alles auf einmal zu implementieren.
4. Iterativ mit einem minimal lauffaehigen Jenkins-Plugin beginnen.
5. Businesslogik nicht ins Jenkins-Plugin verschieben; diese bleibt im Gradle/GRETL-Build.

## 1. Zielbild

Das Plugin stellt eine neue Art von GRETL-Jobs fuer das Befuellen und Aktualisieren des kantonalen Datenportals bereit.

Das Plugin ersetzt nicht die bestehenden klassischen GRETL-Jobs. Es orchestriert fachliche Datenportal-Jobs in Jenkins und erzeugt normale Pipeline Jobs. Console Output, Artefakte, Build History und Jenkins-Berechtigungen bleiben Jenkins-standardkonform.

Die eigentliche Fachlogik bleibt im bestehenden Gradle/GRETL-Build:

- Datenvalidierung;
- Publikation;
- Metadatenverarbeitung;
- DCAT-AP-CH;
- Dateiablage;
- Kommunikation mit Datenportal-Systemen.

Das Jenkins-Plugin ist zustaendig fuer:

- Themen-Repo scannen;
- Job-Katalog und Start-UI bereitstellen;
- Organisationsjobs erzeugen oder aktualisieren;
- Formularfelder aus Standard-GUI und Overrides berechnen;
- Parameter serverseitig validieren;
- Builds starten;
- Run-Details darstellen;
- E-Mail-Benachrichtigungen in Version 1 unterstuetzen;
- Webhook als spaetere generische Schnittstelle im Modell vorbereiten.

## 2. Kanonische Konventionen

Version 5 definiert neue kanonische Konventionen. Bestehende Prototypen oder Demo-Repos mit alten Namen gelten als Altbeispiele und nicht als Referenzformat.

Kanonische Dateinamen:

```text
gretl-datenportal-job.yaml   # Organisationskonfiguration
dataset.json                 # fachliche Datensatzdefinition, Pflicht pro Datensatz
dataset-gui.yaml             # optionaler GUI-Override pro Datensatz
Jenkinsfile                  # optionaler Custom Pipeline Override pro Organisation
```

Kanonische Build-Parameter:

```text
ORGANISATION
DATASET
ENVIRONMENT
METADATA_FILE
DATA_FILE
DRY_RUN
COMMENT
CONFIRM_PRODUCTION
SERIES_ID
```

`SERIES_ID` wird nur fachlich verpflichtend, wenn der gewaehlte Datensatz in `dataset.json` `"series": true` setzt.

`MODE` ist fachlich reserviert, wird aber bis auf Weiteres nicht im Plugin-GUI gerendert, nicht als Jenkins-Parameter erzeugt und nicht an Gradle uebergeben.

## 3. Themen-Repo

### 3.1 Zielstruktur

```text
themen-repo/
  afu/
    gretl-datenportal-job.yaml
    Jenkinsfile
    ch.so.gewaesser.wasserqualitaet/
      dataset.json
      dataset-gui.yaml
    ch.so.abfall.deponien/
      dataset.json

  statistikdienst/
    gretl-datenportal-job.yaml
    ch.so.statistik.bevoelkerung/
      dataset.json

  agi/
    gretl-datenportal-job.yaml
    ch.so.hoheitsgrenzen/
      dataset.json
```

Erste Ebene: Organisationseinheit / Fachstelle, z.B. `afu`, `statistikdienst`, `agi`.

Zweite Ebene: Datensatzordner, z.B. `ch.so.gewaesser.wasserqualitaet`.

Jede Organisationseinheit wird zu einem Jenkins Pipeline Job:

```text
gretl-datenportal-afu
gretl-datenportal-statistikdienst
gretl-datenportal-agi
```

Die Datensatzordner einer Organisationseinheit werden im fachlichen Startformular und im generierten Pipeline Job als `DATASET`-Choice angeboten.

### 3.2 Datensatzdefinition `dataset.json`

Jeder Datensatzordner muss eine `dataset.json` enthalten.

Minimal:

```json
{
  "id": "ch.so.gewaesser.wasserqualitaet",
  "title": "Wasserqualitaet",
  "series": true
}
```

Mit optionaler Beschreibung:

```json
{
  "id": "ch.so.abfall.deponien",
  "title": "Deponien",
  "description": "Standorte und Attribute der Deponien.",
  "series": false
}
```

Regeln:

- `id` ist Pflicht und muss dem Namen des Datensatzordners entsprechen.
- `title` ist Pflicht und wird in der Plugin-UI angezeigt.
- `description` ist optional.
- `series` ist Pflicht und muss `true` oder `false` sein.
- `dataset.json` enthaelt keine GUI-Overrides, keine Permissions und keine Execution-Konfiguration.

Fehlt `dataset.json`, ist das ein Validierungsfehler beim Scannen oder Generieren.

### 3.3 Datensatz-GUI-Override `dataset-gui.yaml`

`dataset-gui.yaml` ist optional. Fehlt die Datei, gelten nur das globale Standard-GUI und der Organisations-Override.

Beispiel:

```yaml
gui:
  fields:
    - id: COMMENT
      required: true
      label: Begruendung
      description: Begruendung fuer die Publikation.
```

`dataset-gui.yaml` darf nur GUI-Felder ueberschreiben oder ergaenzen. Fachliche Metadaten bleiben in `dataset.json`.

## 4. Organisationskonfiguration

Jede Organisationseinheit soll eine `gretl-datenportal-job.yaml` enthalten.

Beispiel:

```yaml
id: afu
title: AFU Datenportal publizieren
description: Publiziert AFU-Datensaetze ins kantonale Datenportal.

department:
  id: afu
  label: Amt fuer Umwelt

datasets:
  source:
    type: subdirectories
    path: "."

permissions:
  read:
    - GA_Gretl_Datenportal_Read
  build:
    - GA_Gretl_Datenportal_AFU

execution:
  type: jenkinsPipeline
  jobName: gretl-datenportal-afu
  jenkinsfile: Jenkinsfile
  gradleTask: publishToDatenportal
  timeoutMinutes: 60

notifications:
  email:
    mode: append
    recipients:
      - datenportal-afu@bd.so.ch
    onFailure: true
    onUnstable: true
    onSuccess: false
    onAborted: false
    includeBuildStarter: true

  webhook:
    enabled: false
    credentialId: datenportal-afu-webhook
    events:
      - failure
      - success

gui:
  fields:
    - id: DRY_RUN
      defaultValue: true
```

Regeln:

- `id` ist Pflicht und muss dem Organisationsordner entsprechen.
- `execution.jobName` ist optional; Default ist `gretl-datenportal-${org}`.
- `execution.gradleTask` ist optional; Default ist `publishToDatenportal`.
- `execution.jenkinsfile` ist optional; der kanonische gemeinsame Repo-Default liegt in `shared/Jenkinsfile`, und ein Organisations-`Jenkinsfile` darf ihn ueberschreiben.
- `gui.fields` ist optional und wird per Feld-ID mit dem globalen Standard-GUI gemergt.

## 5. GUI-Modell

### 5.1 GUI-Kaskade

Das finale Startformular entsteht aus drei Ebenen:

```text
globales Standard-GUI
  ↓
Organisations-Override aus gretl-datenportal-job.yaml
  ↓
Themen-Override aus dataset-gui.yaml
```

Merge-Regel:

- Felder werden ueber `id` identifiziert.
- Ein Override mit gleicher `id` ueberschreibt die Eigenschaften dieses Feldes.
- Nicht erwaehnte Default-Felder bleiben erhalten.
- Neue Felder duerfen ergaenzt werden.
- Die Reihenfolge kommt vom Standard-GUI; neue Felder werden am Ende der jeweiligen Override-Ebene angehaengt, falls keine Positionierung implementiert ist.

### 5.2 Standard-GUI

Das Standard-GUI enthaelt:

```yaml
gui:
  fields:
    - id: ORGANISATION
      label: Organisation
      type: string
      required: true
      readOnly: true

    - id: DATASET
      label: Datensatz
      type: choice
      required: true
      source:
        type: datasets

    - id: ENVIRONMENT
      label: Umgebung
      type: choice
      values: [test, integration, production]
      defaultValue: test
      required: true

    - id: METADATA_FILE
      label: Metadaten hochladen
      type: file
      required: false
      uploadMode: stashedFile
      allowedExtensions: [json, yaml, yml, xml, ttl, zip]
      maxSizeMb: 10

    - id: DATA_FILE
      label: Daten hochladen
      type: file
      required: false
      uploadMode: stashedFile
      allowedExtensions: [csv, xlsx, gpkg, geojson, zip]
      maxSizeMb: 100

    - id: DRY_RUN
      label: Dry Run
      type: boolean
      defaultValue: true

    - id: COMMENT
      label: Kommentar
      type: text
      required: false

    - id: CONFIRM_PRODUCTION
      label: Production bestaetigen
      type: boolean
      defaultValue: false
      requiredIf:
        parameter: ENVIRONMENT
        equals: production
      visibleIf:
        parameter: ENVIRONMENT
        equals: production
```

Wenn `dataset.json` fuer den gewaehlten Datensatz `"series": true` enthaelt, wird zusaetzlich folgendes Feld gerendert und validiert:

```yaml
- id: SERIES_ID
  label: Serie
  type: string
  required: true
```

Bei direktem Jenkins-Start darf `SERIES_ID` im generierten Job optional sichtbar sein. Die Pflichtregel wird serverseitig in der Plugin-UI und zusaetzlich im Default-Jenkinsfile validiert.

### 5.3 Feldtypen

Version 1 unterstuetzt:

```text
string
text
choice
boolean
file
date
url
```

Alle Feldtypen unterstuetzen:

```yaml
id: ...
label: ...
description: ...
required: true|false
defaultValue: ...
visibleIf:
  parameter: ...
  equals: ...
requiredIf:
  parameter: ...
  equals: ...
```

`choice` unterstuetzt:

```yaml
values:
  - test
  - integration
  - production
```

oder:

```yaml
source:
  type: datasets
```

`file` unterstuetzt:

```yaml
uploadMode: stashedFile
allowedExtensions: [csv, xlsx, gpkg]
maxSizeMb: 100
```

Version 1 implementiert keine komplexe Expression Language. Nur einfache Bedingungen `parameter equals value` sind vorgesehen.

### 5.4 Formularvalidierung

Serverseitig zu validieren:

- `ORGANISATION` ist gesetzt und entspricht dem Organisationsjob.
- `DATASET` ist gesetzt und gehoert zur Organisation.
- Pflichtfelder sind gesetzt.
- Bei `series: true` ist `SERIES_ID` gesetzt.
- Mindestens eines von `METADATA_FILE` oder `DATA_FILE` ist gesetzt.
- Beide Uploads zusammen sind erlaubt.
- Dateiendungen entsprechen den erlaubten Extensions.
- Dateigroessen liegen innerhalb der Limits.
- Bei `ENVIRONMENT=production` muss `CONFIRM_PRODUCTION=true` sein.
- Benutzer hat READ/BUILD-Recht fuer die Organisation.

Die gleichen fachlich kritischen Regeln muessen im Default-Jenkinsfile erneut geprueft werden, damit direkte Starts ueber Jenkins Build With Parameters nicht an der Fachvalidierung vorbeigehen.

## 6. Build- und Upload-Vertrag

### 6.1 Primaere Startoberflaeche

Die primaere fachliche Startoberflaeche ist die Plugin-UI unter `/gretl-datenportal`.

Generierte Pipeline Jobs bleiben normale Jenkins Jobs und koennen direkt gestartet werden. Bei direktem Jenkins-Start kann die Parameterseite weniger komfortabel sein, muss aber mit dem dokumentierten Parametervertrag kompatibel bleiben.

### 6.2 Uploads

`METADATA_FILE` und `DATA_FILE` werden als zwei Pipeline-kompatible `stashedFile`-Parameter ueber das Jenkins File Parameter Plugin modelliert.

Empfohlene Jenkinsfile-Nutzung:

```groovy
withFileParameter('METADATA_FILE') {
    // env.METADATA_FILE zeigt auf die temporaer verfuegbare Datei,
    // env.METADATA_FILE_FILENAME enthaelt den Originaldateinamen.
}

withFileParameter('DATA_FILE') {
    // env.DATA_FILE zeigt auf die temporaer verfuegbare Datei,
    // env.DATA_FILE_FILENAME enthaelt den Originaldateinamen.
}
```

Alternativ kann `unstash` verwendet werden, wenn dies fuer den generierten Pipeline Code einfacher ist.

### 6.3 Gradle/GRETL-Aufruf

Default:

```bash
./gradlew publishToDatenportal \
  -Porganisation="${ORGANISATION}" \
  -Pdataset="${DATASET}" \
  -Penvironment="${ENVIRONMENT}" \
  -PdryRun="${DRY_RUN}" \
  -PseriesId="${SERIES_ID}" \
  -PmetadataFile="${METADATA_FILE_PATH}" \
  -PmetadataFileName="${METADATA_FILE_NAME}" \
  -PdataFile="${DATA_FILE_PATH}" \
  -PdataFileName="${DATA_FILE_NAME}"
```

Nur vorhandene Upload-Properties muessen uebergeben werden. `SERIES_ID` wird nur bei Series-Datensaetzen fachlich benoetigt.

Der Gradle Task ist global und pro Organisation ueberschreibbar. Default ist `publishToDatenportal`.

### 6.4 Default-Jenkinsfile

Das Default-Jenkinsfile soll:

- Parameter validieren;
- Uploads materialisieren;
- Gradle/GRETL ausfuehren;
- Build Display Name und Description sinnvoll setzen;
- Artefakte archivieren, falls vorhanden;
- E-Mail-Post-Block fuer `email-ext` enthalten.

Grobe Struktur:

```groovy
pipeline {
    agent any

    options {
        timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
        string(name: 'ORGANISATION', defaultValue: '')
        choice(name: 'DATASET', choices: [])
        choice(name: 'ENVIRONMENT', choices: ['test', 'integration', 'production'])
        booleanParam(name: 'DRY_RUN', defaultValue: true)
        text(name: 'COMMENT', defaultValue: '')
        booleanParam(name: 'CONFIRM_PRODUCTION', defaultValue: false)
        string(name: 'SERIES_ID', defaultValue: '')
        stashedFile(name: 'METADATA_FILE')
        stashedFile(name: 'DATA_FILE')
    }

    stages {
        stage('Validate parameters') {
            steps {
                script {
                    // generated validation
                }
            }
        }

        stage('Run GRETL Datenportal Job') {
            steps {
                script {
                    // materialize files and call ./gradlew publishToDatenportal
                }
            }
        }
    }

    post {
        success {
            // optional emailext when configured
        }
        unstable {
            // optional emailext when configured
        }
        failure {
            // optional emailext when configured
        }
        aborted {
            // optional emailext when configured
        }
    }
}
```

Die konkrete Syntax fuer `stashedFile` haengt von der Pipeline-Definition ab und muss gegen das File Parameter Plugin getestet werden.

## 7. Custom Jenkinsfile

Eine Organisation darf ein eigenes `Jenkinsfile` enthalten und damit den Default-Pipeline-Code vollstaendig ueberschreiben.

Der kanonische gemeinsame Repo-Default liegt unter `shared/Jenkinsfile`.
Dieses Repo-Template darf folgende Platzhalter enthalten:

- `@@TIMEOUT_MINUTES@@`
- `@@GRADLE_TASK@@`
- `@@POST_BLOCK@@`

### 7.1 Platzhalter-Semantik

Syntax:

- Ein Platzhalter ist ein exakter Plain-Text-Marker der Form `@@NAME@@`.
- Es gibt keine eigentliche Template-Engine, keine Schleifen, keine Bedingungen und keine verschachtelten Templates.
- Unbekannte Platzhalter bleiben unveraendert im resultierenden Jenkinsfile stehen.

Ersetzungszeitpunkt:

- Die Ersetzung passiert nur dann, wenn das Plugin den impliziten Repo-Default `shared/Jenkinsfile` verwendet.
- Organisations-`Jenkinsfile` und explizite `execution.jenkinsfile`-Dateien werden literal geladen und nicht interpoliert.
- Technisch liest der Resolver den Text aus `shared/Jenkinsfile` und uebergibt ihn an den Renderer, der die bekannten Marker per einfacher String-Ersetzung ersetzt.

Bedeutung:

- `@@TIMEOUT_MINUTES@@` wird aus dem gemergten `JobDefinition.timeoutMinutes` ersetzt.
- `@@GRADLE_TASK@@` wird aus dem gemergten `JobDefinition.gradleTask` ersetzt.
- `@@POST_BLOCK@@` wird aus der gemergten `NotificationConfiguration` via `EmailNotificationService` erzeugt.
- Wenn Notifications deaktiviert sind, wird `@@POST_BLOCK@@` durch `post { always { echo 'GRETL Datenportal job finished.' } }` ersetzt.

Beispiel:

```text
timeout(time: @@TIMEOUT_MINUTES@@, unit: 'MINUTES')
sh "./gradlew @@GRADLE_TASK@@ ${gradleArgs.join(' ')}"
@@POST_BLOCK@@
```

Reihenfolge:

1. Wenn `execution.jenkinsfile` gesetzt ist, wird dieser Pfad verwendet.
2. Sonst, wenn im Organisationsordner ein `Jenkinsfile` liegt, wird dieses verwendet.
3. Sonst, wenn `shared/gretl-datenportal-defaults.yaml` ein `execution.jenkinsfile` setzt, wird dieser Pfad relativ zu `shared/` verwendet.
4. Sonst wird `shared/Jenkinsfile` aus dem Themen-Repo verwendet und nur dort mit den gemergten Defaults interpoliert.

Fehlt `shared/Jenkinsfile`, ist das ein Validierungsfehler, aber nur dann, wenn mindestens eine Organisation weder eigenes `Jenkinsfile` noch einen expliziten `execution.jenkinsfile`-Pfad verwendet.

Auch ein Custom Jenkinsfile soll den dokumentierten Build-Parametervertrag respektieren.

## 8. Seeder / Generator

### 8.1 Zielarchitektur

Der Seed Job ruft einen Plugin-BuildStep auf:

```text
Seed Job
  ↓
Generate GRETL Datenportal Jobs
  ↓
Plugin scannt Themen-Repo
  ↓
Plugin erzeugt oder aktualisiert Pipeline Jobs pro Organisation
```

Der Java-Generator im Plugin ist Zielarchitektur. Ein bestehender Groovy Job-DSL-Prototyp ist nur Altbeispiel.

### 8.2 Generator-Input

Der Generator liest:

- globale Plugin-Konfiguration;
- Themen-Repo;
- `gretl-datenportal-job.yaml` pro Organisation;
- `dataset.json` pro Datensatz;
- optional `dataset-gui.yaml` pro Datensatz;
- optional Custom `Jenkinsfile` pro Organisation.

### 8.3 Generator-Output

Pro Organisationseinheit:

- ein Jenkins Pipeline Job;
- `DATASET`-Choice aus den Datensaetzen der Organisation;
- feste oder nicht editierbare `ORGANISATION` als Build-Parameter;
- Standardparameter gemaess GUI-Modell;
- `METADATA_FILE` und `DATA_FILE` als `stashedFile`-Parameter;
- optionale Notification-Environment-Variablen;
- View `GRETL Datenportal Jobs`.

## 9. Plugin-UI

Der normale Jenkins-Header bleibt sichtbar. Das Plugin baut keine separate SPA und ersetzt Jenkins nicht.

### 9.1 Uebersicht

```text
GRETL Datenportal Jobs
[Filter Organisation] [Filter Umgebung] [Suche]

Organisationseinheit | Datensaetze | letzter Lauf | Status | Starten
```

### 9.2 Startformular

Beispiel:

```text
AFU Datenportal publizieren

Organisation: afu
Datensatz: Wasserqualitaet (ch.so.gewaesser.wasserqualitaet)
Serie: [________________]              # nur wenn series=true
Umgebung: [test ▼]
Metadaten hochladen: [Datei auswaehlen]
Daten hochladen: [Datei auswaehlen]
Dry Run: [x]
Kommentar: [...]

[Job starten]
```

Wenn `production` gewaehlt wird:

```text
Production bestaetigen: [ ]
```

### 9.3 Run Details

```text
AFU Datenportal publizieren #123
Status: Success

Parameter:
- Organisation: afu
- Datensatz: Wasserqualitaet (ch.so.gewaesser.wasserqualitaet)
- Umgebung: test
- Dry Run: true

Artefakte:
- validation-report.html
- publish-report.json
- dcat-ap-ch.ttl

Console Output
Re-Run
```

## 10. Berechtigungen

Berechtigungen werden fachlich ueber AD-Gruppen modelliert.

Beispiel:

```yaml
permissions:
  read:
    - GA_Gretl_Datenportal_Read
  build:
    - GA_Gretl_Datenportal_AFU
```

Version 1:

- Plugin-UI prueft READ serverseitig.
- Plugin-UI prueft BUILD serverseitig vor Buildstart.
- YAML enthaelt AD-Gruppen als Modell.
- Generierte Job-Matrix-Berechtigungen sind nicht Pflicht fuer Version 1.

Spaetere Version:

- generierte Jobs koennen passende Matrix Permissions erhalten;
- globale Admin-/Operator-Gruppen koennen separat modelliert werden.

## 11. Notifications

### 11.1 E-Mail in Version 1

E-Mail ist Version-1-Bestandteil. Das Jenkins Plugin `email-ext` ist benoetigte Abhaengigkeit.

Konfigurations-Ebenen:

```text
globale Defaults
  ↓
Organisationseinheit
  ↓
Datensatz / einzelner Job
  ↓
optional Build-Starter
```

Standardmodus ist `append`.

Modi:

```text
append
override
disabled
```

Events:

```text
failure
unstable
success
aborted
```

Default:

```yaml
onFailure: true
onUnstable: true
onSuccess: false
onAborted: false
```

Empfaenger werden normalisiert, dedupliziert und validiert.

### 11.2 E-Mail-Inhalt

Mindestinhalt:

```text
Status
Organisationseinheit
Datensatz
Series ID, falls vorhanden
Umgebung
Dry Run
Build-Nummer
Build-URL
Console-URL
Artefakt-Links
Startzeit / Dauer
ausloesender Benutzer
kurzer Fehlerauszug, falls vorhanden
```

Beispiel Betreff:

```text
[GRETL Datenportal] FEHLER: afu / ch.so.gewaesser.wasserqualitaet (#123)
```

### 11.3 Webhook

Webhook wird als generische Zukunftsschnittstelle im Modell vorbereitet, aber nicht als Pflichtbestandteil von Version 1 vollstaendig implementiert.

Nicht als direkte Teams-/Slack-/Mattermost-Integration verstehen.

Beispiel Payload fuer spaetere Versionen:

```json
{
  "event": "failure",
  "plugin": "gretl-datenportal-jobs",
  "job": {
    "name": "gretl-datenportal-afu",
    "organization": "afu",
    "dataset": "ch.so.gewaesser.wasserqualitaet",
    "environment": "test"
  },
  "build": {
    "number": 123,
    "status": "FAILURE",
    "url": "https://jenkins.example/job/gretl-datenportal-afu/123/",
    "startedBy": "martin.weber",
    "durationMillis": 162000
  },
  "parameters": {
    "dryRun": false,
    "seriesId": "2026"
  }
}
```

## 12. Globale Plugin-Konfiguration

Konfigurierbar:

- Anzeigename;
- URL Name;
- Themen-Repo URL;
- Branch;
- Credentials;
- Pfad im Repo;
- Organisationsordner Pattern;
- Datensatzordner Pattern;
- Default Jenkinsfile;
- Default Gradle Task;
- Umgebungen;
- Modi;
- Upload-Limits;
- E-Mail Defaults;
- AD-Gruppen-Mapping;
- Job-Namensschema.

Beispiel JCasC:

```yaml
unclassified:
  gretlDatenportalJobs:
    displayName: "GRETL Datenportal Jobs"
    urlName: "gretl-datenportal"
    topicRepository:
      url: "https://github.com/sogis/datenportal-themen.git"
      branch: "main"
      credentialsId: "github-access-token"
      basePath: "."
    generation:
      jobNamePattern: "gretl-datenportal-${org}"
      defaultJenkinsfile: "Jenkinsfile"
      defaultGradleTask: "publishToDatenportal"
      deleteRemovedJobs: true
    defaults:
      environments: [test, integration, production]
      defaultEnvironment: test
      dryRun: true
      metadataFile:
        allowedExtensions: [json, yaml, yml, xml, ttl, zip]
        maxSizeMb: 10
      dataFile:
        allowedExtensions: [csv, xlsx, gpkg, geojson, zip]
        maxSizeMb: 100
    permissions:
      groupPattern: "GA_Gretl_Datenportal_${ORG}"
    notifications:
      email:
        enabled: true
        onFailure: true
        onUnstable: true
        onSuccess: false
        onAborted: false
```

## 13. Technische Architektur

Sprache:

- Java

Build:

- Maven HPI Plugin

UI:

- Jenkins Jelly;
- Jenkins Form Controls;
- Jenkins Symbols;
- wenig JavaScript fuer einfache UI-Interaktionen wie `visibleIf` / `requiredIf`;
- eigenes CSS nur ergaenzend;
- keine React-/Vue-SPA.

Vorgeschlagene Paketstruktur:

```text
src/main/java/ch/so/agi/jenkins/gretldatenportal/
  GretlDatenportalRootAction.java
  GretlDatenportalGlobalConfiguration.java
  GretlDatenportalSeedBuilder.java
  GretlDatenportalJobGenerator.java
  TopicRepositoryScanner.java
  OrganizationUnit.java
  DatasetEntry.java
  DatasetDefinition.java
  JobDefinition.java
  GuiDefinition.java
  GuiFieldDefinition.java
  ParameterType.java
  ChoiceSource.java
  Condition.java
  JobDefinitionParser.java
  DatasetDefinitionParser.java
  DatasetGuiParser.java
  JobDefinitionValidator.java
  GuiDefinitionMerger.java
  PipelineJobRenderer.java
  NotificationConfiguration.java
  EmailNotificationService.java

src/main/resources/ch/so/agi/jenkins/gretldatenportal/GretlDatenportalRootAction/
  index.jelly
  organization.jelly
  start.jelly
  run.jelly

src/main/resources/ch/so/agi/jenkins/gretldatenportal/GretlDatenportalSeedBuilder/
  config.jelly

src/main/webapp/css/
  gretl-datenportal.css

src/main/webapp/js/
  gretl-datenportal.js
```

## 14. Tests

### 14.1 Scanner

Testfaelle:

- gueltige Themenstruktur mit mehreren Organisationen;
- leere Organisation;
- fehlende `gretl-datenportal-job.yaml`;
- fehlende `dataset.json`;
- `series: true`;
- `series: false`;
- `dataset.json.id` passt nicht zum Ordnernamen;
- versteckte Ordner werden ignoriert;
- ungueltige Datensatzordnernamen werden gemeldet.

### 14.2 Parser / Validator

Testfaelle:

- `dataset.json` minimal gueltig;
- `dataset.json` ohne `series`;
- `dataset.json` mit falschem Typ fuer `series`;
- `gretl-datenportal-job.yaml` gueltig;
- `dataset-gui.yaml` gueltig;
- ungueltige Feldtypen;
- ungueltige ChoiceSource;
- ungueltige `visibleIf` / `requiredIf`;
- GUI-Merge per Feld-ID.

### 14.3 Formularvalidierung

Testfaelle:

- alle Standardpflichtfelder gesetzt;
- kein Upload gesetzt;
- nur `METADATA_FILE` gesetzt;
- nur `DATA_FILE` gesetzt;
- beide Uploads gesetzt;
- ungueltige Dateiendung;
- zu grosse Datei;
- `series: true` ohne `SERIES_ID`;
- `series: false` ohne `SERIES_ID`;
- `production` ohne `CONFIRM_PRODUCTION`;
- fehlendes BUILD-Recht.

### 14.4 Generator

Testfaelle:

- Pipeline Job pro Organisation;
- `DATASET`-Choice aus `dataset.json`;
- Anzeige Titel + ID in Plugin-UI, technische ID als Build-Wert;
- `stashedFile`-Parameter fuer `METADATA_FILE` und `DATA_FILE`;
- `shared/Jenkinsfile` als gemeinsamer Default ohne Custom Jenkinsfile;
- Custom Jenkinsfile pro Organisation;
- Notification Env Vars / Post-Block-Vertrag;
- Build Description / Display Name.

### 14.5 Jenkins-Integration

Testfaelle mit Jenkins Test Harness:

- RootAction unter `/gretl-datenportal`;
- Uebersichtsseite rendert;
- Startformular rendert;
- Build wird mit Parametern gequeued;
- READ/BUILD-Pruefung greift;
- Run-Details zeigen Parameter, Artefakte und Links.

## 15. Nicht-Ziele Version 1

Nicht in Version 1:

- Ersatz der klassischen GRETL-Jobs;
- eigenes Workflow-System;
- direkte Ausfuehrung ausserhalb Jenkins;
- komplexe Metadatenpflege fuer DCAT-AP-CH im Plugin;
- vollstaendiges Datenportal-Frontend;
- eigenes Scheduling;
- eigene Benutzerverwaltung;
- komplexe Formular-Expression-Language;
- React/Vue-SPA;
- direkter Teams-/Slack-/Mattermost-Kanal;
- vollstaendige Webhook-Retry-Queue;
- Job-Matrix-Berechtigungen als Pflichtbestandteil.

## 16. Codex-Auftrag / LLM-Anweisung

Der folgende Abschnitt ist die operative Anweisung fuer ChatGPT Codex oder einen vergleichbaren LLM-Agenten.

```text
Du bist ein erfahrener Jenkins-Plugin-Entwickler, Java-Architekt und Jenkins Job-Generator-Experte.

Projekt:
Wir entwickeln ein Jenkins-Plugin mit dem sichtbaren Namen "GRETL Datenportal Jobs".

Zweck:
Das Plugin ist fuer eine neue Art von GRETL-Jobs gedacht. Diese Jobs dienen dem Befuellen und Aktualisieren eines kantonalen Datenportals. Es ersetzt nicht die bestehenden klassischen GRETL-Jobs.

Wichtig:
Die Businesslogik fuer Datenportal, Datenvalidierung, Publikation, Metadaten, DCAT-AP-CH, Dateiablage usw. liegt NICHT im Jenkins-Plugin. Diese Logik liegt weiterhin im Gradle Build, konkret im bestehenden speziellen GRETL-Gradle-Plugin. Das Jenkins-Plugin orchestriert, generiert Jobs, stellt UI bereit, validiert Formularparameter und startet Jenkins Builds.

Architekturentscheidungen:
- Java-basiertes Jenkins-Plugin.
- Maven HPI Plugin.
- Jelly-first UI.
- Normalen Jenkins-Header und Jenkins-Rahmen nicht ersetzen.
- Keine React/Vue-SPA in Version 1.
- Wenig JavaScript nur fuer einfache UI-Interaktionen wie visibleIf/requiredIf.
- Weiterhin Seeder-/Job-Generator-Ansatz.
- Zielarchitektur: Seed Job ruft einen Plugin-BuildStep auf.
- Generiere pro Organisationseinheit einen Jenkins Pipeline Job.
- Die Datensatz-Unterordner einer Organisationseinheit werden als DATASET-Choice angezeigt.
- Jeder Datensatzordner muss eine dataset.json enthalten.
- dataset.json enthaelt mindestens id, title und series.
- dataset.json enthaelt keine GUI-Overrides.
- dataset-gui.yaml kann pro Datensatz GUI-Felder ueberschreiben.
- GUI-Kaskade: globales Standard-GUI, Organisations-Override, Datensatz-Override.
- GUI-Merge erfolgt per Feld-ID.
- `shared/Jenkinsfile` ist der kanonische gemeinsame Default fuer die Pipeline-Definition.
- Jeder generierte Job muss weiterhin per Custom Jenkinsfile ueberschreibbar sein.
- Berechtigungen werden ueber AD-Gruppen modelliert.
- Version 1 prueft READ/BUILD serverseitig im Plugin.
- Builds sollen normale Jenkins Builds sein.
- Console Output, Artefakte und Build History sollen Jenkins-standardkonform bleiben.
- E-Mail ist der erste vollstaendig unterstuetzte Notification-Kanal.
- email-ext ist eine benoetigte Jenkins-Abhaengigkeit.
- Webhook wird als generische Zukunftsschnittstelle im Modell vorbereitet, aber nicht als Pflichtbestandteil der ersten Iteration behandelt.
- Teams, Slack, Mattermost oder andere Kanaele werden in Version 1 nicht direkt implementiert.

Kanonische Themen-Repo-Struktur:
themen-repo/
  afu/
    gretl-datenportal-job.yaml
    Jenkinsfile
    ch.so.gewaesser.wasserqualitaet/
      dataset.json
      dataset-gui.yaml
    ch.so.abfall.deponien/
      dataset.json
  statistikdienst/
    gretl-datenportal-job.yaml
    ch.so.statistik.bevoelkerung/
      dataset.json

Daraus sollen Jobs entstehen:
- gretl-datenportal-afu
- gretl-datenportal-statistikdienst

Kanonische Build-Parameter:
- ORGANISATION
- DATASET
- ENVIRONMENT
- METADATA_FILE
- DATA_FILE
- DRY_RUN
- COMMENT
- CONFIRM_PRODUCTION
- SERIES_ID

Standard-GUI:
- ORGANISATION: string, required, readOnly
- DATASET: choice, required, source datasets
- ENVIRONMENT: choice test/integration/production, default test, required
- METADATA_FILE: stashedFile, optional, json/yaml/yml/xml/ttl/zip, max 10 MB
- DATA_FILE: stashedFile, optional, csv/xlsx/gpkg/geojson/zip, max 100 MB
- DRY_RUN: boolean, default true
- COMMENT: text, optional
- CONFIRM_PRODUCTION: boolean, visible/required for production
- SERIES_ID: string, required only if selected dataset has series=true

Upload-Regel:
METADATA_FILE und DATA_FILE sind einzeln optional, aber mindestens eines muss vorhanden sein. Beide zusammen sind erlaubt.

Default-Ausfuehrung:
./gradlew publishToDatenportal mit:
-Porganisation
-Pdataset
-Penvironment
-PdryRun
und, falls vorhanden:
-PseriesId
-PmetadataFile
-PmetadataFileName
-PdataFile
-PdataFileName

Minimaler Funktionsumfang Iteration 1:
1. Jenkins-Plugin-Projektstruktur erstellen.
2. pom.xml fuer Jenkins Plugin erstellen.
3. RootAction unter /gretl-datenportal bereitstellen.
4. Einfache Uebersichtsseite mit Jenkins/Jelly anzeigen.
5. Statisches Demo-Themen-Repo oder Test-Filesystem scannen.
6. Organisationseinheiten erkennen.
7. Datensatz-Unterordner erkennen.
8. dataset.json lesen und validieren.
9. Java-Modellklassen fuer Organisationseinheiten, Datensaetze, Jobdefinitionen und GUI-Felder anlegen.
10. Unit Tests fuer den Scanner schreiben.
11. Noch keine echten Jenkins Jobs generieren, falls das zu viel auf einmal ist; zuerst Scanner und UI stabilisieren.

Minimaler Funktionsumfang Iteration 2:
1. gretl-datenportal-job.yaml pro Organisation lesen.
2. dataset-gui.yaml pro Datensatz lesen, falls vorhanden.
3. YAML-/JSON-Parser und Validator implementieren.
4. Parameter-/GUI-Feldtypen unterstuetzen:
   - string
   - text
   - choice
   - boolean
   - file
   - date
   - url
5. required true/false unterstuetzen.
6. ChoiceSource datasets unterstuetzen.
7. Einfache Bedingungen unterstuetzen:
   - visibleIf parameter equals value
   - requiredIf parameter equals value
8. GUI-Merge per Feld-ID implementieren.
9. Jelly-Startformular fuer eine Organisationseinheit rendern.
10. Servervalidierung fuer Pflichtfelder, Upload-Regel, series und production implementieren.

Minimaler Funktionsumfang Iteration 3:
1. Seeder-/Generator-Mechanismus als Java BuildStep implementieren.
2. Pro Organisationseinheit einen Jenkins Pipeline Job erzeugen oder aktualisieren.
3. DATASET-Choice-Parameter aus Datensatzordnern und dataset.json erzeugen.
4. ORGANISATION als Build-Parameter mit fixem Wert erzeugen.
5. METADATA_FILE und DATA_FILE als stashedFile-Parameter erzeugen.
6. SERIES_ID fuer direkte Jenkins-Starts optional sichtbar machen.
7. `shared/Jenkinsfile` als Repo-Default verwenden und fehlende Repo-Defaults frueh validieren.
8. Custom Jenkinsfile pro Organisationseinheit respektieren.
9. Pipeline Job so generieren, dass ./gradlew publishToDatenportal ausgefuehrt wird.
10. Upload-Dateipfade und Originaldateinamen an Gradle uebergeben.

Minimaler Funktionsumfang Iteration 4:
1. E-Mail-Notification-Modell implementieren.
2. Konfiguration auf mehreren Ebenen unterstuetzen:
   - global
   - Organisationseinheit
   - Datensatz / Job
   - optional Build-Starter
3. Modi unterstuetzen:
   - append
   - override
   - disabled
4. Empfaenger normalisieren und deduplizieren.
5. onFailure, onUnstable, onSuccess, onAborted unterstuetzen.
6. Version 1 darf E-Mail ueber generierten Jenkinsfile post block und emailext loesen.
7. Webhook nur als Modell/Konfiguration vorbereiten, nicht zwingend vollstaendig ausimplementieren.

Minimaler Funktionsumfang Iteration 5:
1. Run-Details-Seite erstellen.
2. Status, Parameter, Console-Link, Artefakte und Re-Run-Link anzeigen.
3. Jenkins Build Description und Display Name sinnvoll setzen.
4. Optional Console Output Preview anzeigen.

Wichtige Regeln:
- Normaler Jenkins-Header bleibt sichtbar.
- Kein eigenes vollstaendiges Web-Frontend bauen.
- Keine Businesslogik aus Gradle/GRETL ins Plugin verschieben.
- Keine Secrets loggen.
- File Uploads validieren.
- Jenkins Permissions serverseitig pruefen.
- Ungueltige YAML-/JSON-Dateien sollen gute Fehlermeldungen erzeugen.
- Kein komplexer Expression-Evaluator fuer Version 1.
- Kein React/Vue.
- E-Mail zuerst.
- Webhook nur als generische Zukunftsschnittstelle vorbereiten.
- Teams/Slack/Mattermost nicht direkt implementieren.
- Immer zuerst kleine, lauffaehige Schritte liefern.
```
