# GRETL Datenportal Jobs Jenkins Plugin

Jenkins plugin for GRETL Datenportal job catalogues, start forms, and generated
Pipeline jobs.

The current implementation covers the first vertical slice from the v5
specification:

- Maven HPI plugin skeleton.
- RootAction at `/gretl-datenportal`.
- Global configuration for display name, URL name, and local topic repository
  path.
- Topic repository scanner for organizations, dataset folders, and mandatory
  `dataset.json` files.
- YAML parsing for `gretl-datenportal-job.yaml` and `dataset-gui.yaml`.
- Default GUI model, GUI merge, and server-side start-form validation.
- Seed builder and Pipeline job generator for generated workflow jobs.
- Custom organization `Jenkinsfile` resolution before falling back to the
  repo-owned default template from `shared/Jenkinsfile`.
- YAML permission model for `permissions.read` and `permissions.build`, with
  server-side RootAction checks.
- Default Pipeline script renderer with `stashedFile` uploads and email-ext
  post-block support for the repo-wide shared template.
- The `MODE` build parameter is currently reserved and not rendered or passed
  to Gradle.
- Basic start and run-detail Jelly views.
- Unit tests for scanner, GUI merge, start validation, permissions, custom
  Pipeline resolution, and Pipeline rendering.

## Build

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.10-tem
mvn -ntp package
```

The local HPI is written to:

```text
target/jenkins-gretl-datenportal-plugin.hpi
```

## Shared Jenkinsfile Default

The recommended repo-level default Pipeline lives in the topic repository at:

```text
shared/Jenkinsfile
```

Resolution order for generated jobs:

1. `execution.jenkinsfile` from the organization `gretl-datenportal-job.yaml`
2. `Jenkinsfile` in the organization folder
3. `execution.jenkinsfile` from `shared/gretl-datenportal-defaults.yaml`
4. `shared/Jenkinsfile`

`shared/Jenkinsfile` is the canonical repo-level default and may use these
placeholders, which the plugin resolves from merged shared and organization
defaults:

- `@@TIMEOUT_MINUTES@@`
- `@@GRADLE_TASK@@`
- `@@POST_BLOCK@@`

### How Placeholder Replacement Works

`@@NAME@@` is a plain-text marker, not a template engine feature of Jenkins or
Groovy.

- Only exact known markers are replaced.
- `@@TIMEOUT_MINUTES@@` is replaced from `JobDefinition.timeoutMinutes`.
- `@@GRADLE_TASK@@` is replaced from `JobDefinition.gradleTask`.
- `@@POST_BLOCK@@` is replaced from `NotificationConfiguration` via
  `EmailNotificationService`.
- When notifications are disabled, `@@POST_BLOCK@@` falls back to
  `post { always { echo 'GRETL Datenportal job finished.' } }`.
- Placeholder replacement happens only for the implicit repo-level
  `shared/Jenkinsfile`.
- Organization `Jenkinsfile` files and explicit `execution.jenkinsfile` paths
  are loaded literally and are not interpolated.
- Unknown markers remain unchanged.

If an organization has no own Pipeline override and no explicit
`execution.jenkinsfile` path resolves first, then `shared/Jenkinsfile` must
exist.

## Local Jenkins Dev Setup

The companion local setup in
`/Users/stefan/sources/p-agi_datenportal/jenkins-dev` contains an installer
script and a v5-compatible demo topic repository. Build the HPI first, install
it into `jenkins-dev`, verify the installed JPI, then start Jenkins:

```bash
cd /Users/stefan/sources/jenkins-gretl-datenportal-plugin
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.10-tem
mvn -ntp package

cd /Users/stefan/sources/p-agi_datenportal/jenkins-dev
./bin/install-gretl-datenportal-plugin.sh

stat -f '%Sm %N' -t '%Y-%m-%d %H:%M:%S' \
  /Users/stefan/sources/jenkins-gretl-datenportal-plugin/target/jenkins-gretl-datenportal-plugin.hpi \
  /Users/stefan/sources/p-agi_datenportal/jenkins-dev/jenkins-home/plugins/jenkins-gretl-datenportal-plugin.jpi

./bin/start.sh
```

If Jenkins is already running on `localhost:8080`, fully restart it after the
installer finishes. Overwriting the `.jpi` alone does not reload already loaded
classes or Jelly views.

Then open:

```text
http://localhost:8080/gretl-datenportal
```

For the full local workflow, including the seed job and demo uploads, see
`/Users/stefan/sources/p-agi_datenportal/jenkins-dev/README.md`.

The full working specification and Codex prompt live in
[`spec/gretl_datenportal_jobs_plugin_spec_and_codex_prompt_v5.md`](spec/gretl_datenportal_jobs_plugin_spec_and_codex_prompt_v5.md).
