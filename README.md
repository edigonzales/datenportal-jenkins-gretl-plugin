# jenkins-gretl-datenportal-plugin

Jenkins-Plugin fuer GRETL-Datenportal-Katalog, Startformular, Seed-Job und
generierte Pipeline-Jobs.

## Rolle im Gesamtsystem

Dieses Repo enthaelt die eigentliche Plugin-Implementierung.

Hierher gehoeren:

- Scanner und Validierungslogik fuer das Themenrepo
- RootAction unter `/gretl-datenportal`
- fixes Startformular und serverseitige Validierung
- Rechtepruefung ueber `permissions.read` und `permissions.build`
- Seed-Builder, verwalteter Themenrepo-Checkout und Job-Generierung
- Renderlogik fuer `shared/Jenkinsfile` und Placeholder-Ersatz

Nicht hierher gehoeren:

- der fachliche Vertrag des Themenrepos:
  [datenportal-themenrepo](https://codeberg.org/edigonzales/datenportal-themenrepo)
- die lokale Jenkins-Betriebsumgebung:
  [datenportal-jenkins-dev](https://codeberg.org/edigonzales/datenportal-jenkins-dev)

## Zentrale Workflows

### Plugin bauen

```bash
cd ../jenkins-gretl-datenportal-plugin
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.10-tem"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -ntp package
```

Das lokale HPI landet unter
`target/jenkins-gretl-datenportal-plugin.hpi`.

### Plugin in die lokale Jenkins-Umgebung installieren

```bash
cd ../datenportal-jenkins-dev
./bin/install-gretl-datenportal-plugin.sh
```

Wenn Jenkins bereits laeuft, ist anschliessend ein voller Restart noetig.

### Lokalen End-to-End-Loop fahren

```bash
cd ../jenkins-gretl-datenportal-plugin
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.10-tem"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -ntp package

cd ../datenportal-jenkins-dev
./bin/install-gretl-datenportal-plugin.sh
./bin/start.sh
```

Danach den automatisch angelegten Job `gretl-datenportal-seed` manuell starten
oder auf den Cron-Lauf warten und das Datenportal unter `/gretl-datenportal`
pruefen.

## Seed-Job

Neben dem Seed-Builder provisioniert das Plugin auch den Seed-Job
`gretl-datenportal-seed` selbst. Der Job:

- wird automatisch erstellt und aktualisiert, wenn `seedJobAutoCreate=true`;
- bleibt deaktiviert, solange kein Themenrepo konfiguriert ist;
- wird per `seedJobCron` (Default `H/15 * * * *`) getriggert;
- kann jederzeit manuell gestartet werden;
- verwendet die globale Themenrepo-Konfiguration, nicht duplizierte Repo-Werte
  im Job.

Ein leerer `seedJobCron` deaktiviert nur den Timer, nicht den Job. Bestehende,
nicht vom Plugin verwaltete Jobs mit demselben Namen werden nicht ueberschrieben.

Die generierten Datenportal-Jobs sind weiterhin keine Live-Sicht auf das
Themenrepo. Repo-Aenderungen werden erst nach dem naechsten Seed-Lauf
materialisiert.

## Langform-Doku

Die technische Langform-Doku liegt unter
[docs/biblios/entwicklung/index.adoc](docs/biblios/entwicklung/index.adoc).

## Schwester-Repositories

- [datenportal-themenrepo](https://codeberg.org/edigonzales/datenportal-themenrepo)
  ist die kanonische Doku fuer Organisationsstruktur, Datensaetze,
  `gretl-datenportal-job.yaml`, `shared/Jenkinsfile` und Shared Defaults.
- [datenportal-jenkins-dev](https://codeberg.org/edigonzales/datenportal-jenkins-dev)
  ist die kanonische Doku fuer lokalen Jenkins, Offline-Bundle, JCasC und
  Docker-/Airgap-Tests.
