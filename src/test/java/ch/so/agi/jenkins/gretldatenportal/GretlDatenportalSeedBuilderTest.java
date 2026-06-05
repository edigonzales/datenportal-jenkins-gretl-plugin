package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.FreeStyleProject;
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
}
