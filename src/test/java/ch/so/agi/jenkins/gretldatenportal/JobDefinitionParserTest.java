package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobDefinitionParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesPermissionsAndExecution() throws IOException {
        Path yaml = tempDir.resolve("gretl-datenportal-job.yaml");
        Files.writeString(
                yaml,
                """
                title: AFU Datenportal publizieren
                execution:
                  jobName: gretl-datenportal-afu
                  jenkinsfile: Jenkinsfile
                  gradleTask: publishToDatenportal
                  timeoutMinutes: 45
                permissions:
                  read:
                    - GA_Gretl_Datenportal_Read
                  build:
                    - GA_Gretl_Datenportal_AFU
                """,
                StandardCharsets.UTF_8);

        OrganizationJobConfiguration configuration = new JobDefinitionParser().parse(yaml, "afu");

        assertEquals("afu", configuration.getJobDefinition().getId());
        assertEquals("gretl-datenportal-afu", configuration.getJobDefinition().getJobName());
        assertEquals("Jenkinsfile", configuration.getJobDefinition().getJenkinsfile());
        assertEquals(45, configuration.getJobDefinition().getTimeoutMinutes());
        assertTrue(configuration.getPermissionConfiguration().hasReadRestrictions());
        assertTrue(configuration.getPermissionConfiguration().hasBuildRestrictions());
    }

    @Test
    void usesFolderIdWhenYamlIdIsMissing() throws IOException {
        Path yaml = tempDir.resolve("gretl-datenportal-job.yaml");
        Files.writeString(
                yaml,
                """
                permissions:
                  read:
                    - GA_Gretl_Datenportal_Read
                  build:
                    - GA_Gretl_Datenportal_AFU
                """,
                StandardCharsets.UTF_8);

        OrganizationJobConfiguration configuration = new JobDefinitionParser().parse(yaml, "afu");

        assertEquals("afu", configuration.getJobDefinition().getId());
    }

    @Test
    void ignoresYamlIdWhenItDiffersFromFolderName() throws IOException {
        Path yaml = tempDir.resolve("gretl-datenportal-job.yaml");
        Files.writeString(
                yaml,
                """
                id: something-else
                permissions:
                  read:
                    - GA_Gretl_Datenportal_Read
                  build:
                    - GA_Gretl_Datenportal_AFU
                """,
                StandardCharsets.UTF_8);

        OrganizationJobConfiguration configuration = new JobDefinitionParser().parse(yaml, "afu");

        assertEquals("afu", configuration.getJobDefinition().getId());
    }
}
