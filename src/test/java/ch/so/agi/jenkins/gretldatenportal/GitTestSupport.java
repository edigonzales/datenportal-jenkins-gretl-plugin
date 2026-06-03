package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class GitTestSupport {
    private GitTestSupport() {
    }

    static void initRepository(Path repositoryPath) throws IOException, InterruptedException {
        Files.createDirectories(repositoryPath);
        run(repositoryPath.getParent(), "git", "init", "-b", "main", repositoryPath.toString());
        run(repositoryPath, "git", "config", "user.email", "local@example.invalid");
        run(repositoryPath, "git", "config", "user.name", "Local Test");
    }

    static void commitAll(Path repositoryPath, String message) throws IOException, InterruptedException {
        run(repositoryPath, "git", "add", ".");
        run(repositoryPath, "git", "commit", "-m", message);
    }

    static void addOrganization(Path repositoryPath, String organizationId, String datasetId)
            throws IOException {
        Path organizationPath = Files.createDirectories(repositoryPath.resolve(organizationId));
        Files.writeString(
                organizationPath.resolve("gretl-datenportal-job.yaml"),
                """
                id: %s
                title: %s Datenportal publizieren
                description: Lokaler Test fuer %s.
                permissions:
                  read:
                    - GA_Gretl_Datenportal_Read
                  build:
                    - GA_Gretl_Datenportal_%s
                """.formatted(organizationId, organizationId.toUpperCase(), organizationId, organizationId.toUpperCase()),
                StandardCharsets.UTF_8);

        Path datasetPath = Files.createDirectories(organizationPath.resolve(datasetId));
        Files.writeString(
                datasetPath.resolve("dataset.json"),
                """
                {
                  "id": "%s",
                  "title": "%s",
                  "description": "",
                  "series": false
                }
                """.formatted(datasetId, datasetId),
                StandardCharsets.UTF_8);
    }

    static void writeSharedJenkinsfile(Path repositoryPath) throws IOException {
        Path sharedPath = Files.createDirectories(repositoryPath.resolve("shared"));
        Files.writeString(
                sharedPath.resolve("Jenkinsfile"),
                """
                pipeline {
                    agent any
                    options {
                        timeout(time: @@TIMEOUT_MINUTES@@, unit: 'MINUTES')
                    }
                    stages {
                        stage('Run') {
                            steps {
                                echo '@@GRADLE_TASK@@'
                            }
                        }
                    }
                @@POST_BLOCK@@
                }
                """,
                StandardCharsets.UTF_8);
    }

    static String fileUrl(Path path) {
        return path.toUri().toString();
    }

    private static void run(Path workdir, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workdir.toFile())
                .redirectErrorStream(true)
                .start();

        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (" + String.join(" ", List.of(command)) + "): " + output);
        }
    }
}
