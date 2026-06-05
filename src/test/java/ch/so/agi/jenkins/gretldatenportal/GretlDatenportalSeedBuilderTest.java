package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class GretlDatenportalSeedBuilderTest {
    @TempDir
    Path tempDir;

    @Test
    @WithJenkins
    void generatesAndUpdatesJobsFromManagedGitCheckout(JenkinsRule jenkinsRule) throws Exception {
        Path sourceRepository = tempDir.resolve("source-repo");
        GitTestSupport.initRepository(sourceRepository);
        GitTestSupport.writeSharedJenkinsfile(sourceRepository);
        GitTestSupport.addOrganization(sourceRepository, "afu", "ch.so.abfall.deponien");
        Files.createDirectories(sourceRepository.resolve("gradle/wrapper"));
        GitTestSupport.commitAll(sourceRepository, "initial topics");

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("seed");
        project.getBuildersList().add(new GretlDatenportalSeedBuilder(
                "",
                GitTestSupport.fileUrl(sourceRepository),
                "main"));

        jenkinsRule.configRoundtrip(project);
        jenkinsRule.buildAndAssertSuccess(project);
        assertNotNull(jenkinsRule.jenkins.getItemByFullName("gretl-datenportal-afu", WorkflowJob.class));

        GitTestSupport.addOrganization(sourceRepository, "statistikdienst", "ch.so.statistik.bevoelkerung");
        Files.createDirectories(sourceRepository.resolve("statistikdienst/ch.so.statistik.bevoelkerung/examples"));
        GitTestSupport.commitAll(sourceRepository, "add statistikdienst");

        jenkinsRule.buildAndAssertSuccess(project);
        assertNotNull(jenkinsRule.jenkins.getItemByFullName("gretl-datenportal-statistikdienst", WorkflowJob.class));
    }

    @Test
    @WithJenkins
    void ignoresBrokenTopLevelFolderWithoutFailingSeedRun(JenkinsRule jenkinsRule) throws Exception {
        Path sourceRepository = tempDir.resolve("source-repo");
        GitTestSupport.initRepository(sourceRepository);
        GitTestSupport.writeSharedJenkinsfile(sourceRepository);
        GitTestSupport.addOrganization(sourceRepository, "afu", "ch.so.abfall.deponien");
        Path brokenDatasetPath = Files.createDirectories(
                sourceRepository.resolve("statistikdienst/ch.so.statistik.bevoelkerung"));
        Files.writeString(
                brokenDatasetPath.resolve("dataset.xtf"),
                GitTestSupport.datasetXml(
                        "ch.so.statistik.bevoelkerung",
                        "Bevoelkerung",
                        "Broken top-level folder fixture",
                        false));
        GitTestSupport.commitAll(sourceRepository, "topics with broken top-level folder");

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("seed-with-warning");
        project.getBuildersList().add(new GretlDatenportalSeedBuilder(
                "",
                GitTestSupport.fileUrl(sourceRepository),
                "main"));

        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        jenkinsRule.assertLogContains("WARNING: Skipping folder because it has dataset-like child directories but no gretl-datenportal-job.yaml.", build);
        assertNotNull(jenkinsRule.jenkins.getItemByFullName("gretl-datenportal-afu", WorkflowJob.class));
    }

    @Test
    @WithJenkins
    void failsWithEnglishSummaryWhenTopicRepositoryValidationFails(JenkinsRule jenkinsRule) throws Exception {
        Path sourceRepository = tempDir.resolve("source-repo");
        GitTestSupport.initRepository(sourceRepository);
        GitTestSupport.writeSharedJenkinsfile(sourceRepository);
        Path organizationPath = Files.createDirectories(sourceRepository.resolve("afu"));
        Files.writeString(
                organizationPath.resolve("gretl-datenportal-job.yaml"),
                """
                permissions:
                  read:
                    - GA_Gretl_Datenportal_Read
                """);
        GitTestSupport.commitAll(sourceRepository, "invalid permissions");

        FreeStyleProject project = jenkinsRule.createFreeStyleProject("seed-invalid");
        project.getBuildersList().add(new GretlDatenportalSeedBuilder(
                "",
                GitTestSupport.fileUrl(sourceRepository),
                "main"));

        FreeStyleBuild build = jenkinsRule.buildAndAssertStatus(Result.FAILURE, project);
        jenkinsRule.assertLogContains("ERROR: permissions.build must contain at least one group.", build);
        jenkinsRule.assertLogContains("ERROR: Topic repository validation failed.", build);
    }

    @Test
    @WithJenkins
    void failsWithEnglishMessageWhenTopicRepositoryPathIsMissing(JenkinsRule jenkinsRule) throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("seed-missing-config");
        project.getBuildersList().add(new GretlDatenportalSeedBuilder("", "", ""));

        FreeStyleBuild build = jenkinsRule.buildAndAssertStatus(Result.FAILURE, project);
        jenkinsRule.assertLogContains("ERROR: Topic repository path is not configured.", build);
    }
}
