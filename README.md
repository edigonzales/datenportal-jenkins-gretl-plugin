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

## Teams und Jenkins-Autorisierung

Die Benutzer werden im Themenrepo Teams zugeordnet. Die Datei
`shared/gretl-datenportal-teams.yaml` ist die einzige Quelle fuer diese
Zuordnung:

```yaml
teams:
  gretl-datenportal-seed-operators:
    users:
      - sziegler
      - mmuster

  datenportal-read:
    users:
      - bbeispiel

  statistikdienst-build:
    users:
      - sziegler
```

Eine Organisationsdatei referenziert nur noch Team-IDs:

```yaml
permissions:
  read:
    - team: datenportal-read
  build:
    - team: statistikdienst-build
```

Unter `Manage Jenkins -> System -> GRETL Datenportal Jobs` wird bei
`Seed-Job Operators Team` die Team-ID des Seeder-Teams eingetragen. Das Feld
ist keine Organisationsberechtigung. Das Seeder-Team darf den verwalteten Job
sehen und starten, aber nicht konfigurieren, loeschen, abbrechen oder seinen
Builder ersetzen. Der erste Seed-Lauf muss deshalb durch einen Jenkins-
Administrator erfolgen; danach synchronisiert ein erfolgreicher Seed-Lauf auch
die Seeder-ACL.

Fuer die Jenkins-Sicherheit muss das Plugin `matrix-auth` installiert sein und
Jenkins muss die **Project-based Matrix Authorization Strategy** verwenden.
LDAP oder Entra ID authentisiert die Benutzer; die Teamdatei liefert die
projektbezogenen Berechtigungen. Global sollten normale Benutzer nur
`Overall/Read` erhalten. Pauschale globale `Item/Read`- oder `Item/Build`-
Rechte fuer `authenticated` duerfen nicht gesetzt werden, weil globale Rechte
additiv zu den Projekt-ACLs wirken. Jenkins-Administratoren werden separat mit
`Overall/Administer` abgesichert. Die Themenrepo-YAML vergibt niemals
globale Administrationsrechte und insbesondere kein `Item/Configure`.

Pluginverwaltete Organisationsjobs erhalten eine nicht-erbende Matrix-ACL. Bei
einem erfolgreichen Seed-Lauf werden Team-Aenderungen und entfernte
Organisationen synchronisiert. Eine ungueltige Teams- oder
Berechtigungskonfiguration bricht den Lauf ab; die letzte gueltige Seeder-ACL
bleibt dabei erhalten.

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
