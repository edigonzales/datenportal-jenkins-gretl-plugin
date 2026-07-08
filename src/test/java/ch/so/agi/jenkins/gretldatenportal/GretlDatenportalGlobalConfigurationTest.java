package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.FormValidation;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class GretlDatenportalGlobalConfigurationTest {

    @Test
    @WithJenkins
    void defaultsAreReturnedForUnsetSeedJobFields(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        assertTrue(configuration.isSeedJobAutoCreate());
        assertEquals("H/15 * * * *", configuration.getSeedJobCron());
        assertEquals(20, configuration.getSeedJobBuildsToKeep());
    }

    @Test
    @WithJenkins
    void blankCronIsPreservedButDefaultReplacesNull(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        configuration.setSeedJobCron("");
        assertEquals("", configuration.getSeedJobCron());

        configuration.setSeedJobCron("  H H * * *  ");
        assertEquals("H H * * *", configuration.getSeedJobCron());
    }

    @Test
    @WithJenkins
    void buildsToKeepFallsBackToDefaultForNonPositiveValues(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        configuration.setSeedJobBuildsToKeep(0);
        assertEquals(20, configuration.getSeedJobBuildsToKeep());

        configuration.setSeedJobBuildsToKeep(-5);
        assertEquals(20, configuration.getSeedJobBuildsToKeep());

        configuration.setSeedJobBuildsToKeep(7);
        assertEquals(7, configuration.getSeedJobBuildsToKeep());
    }

    @Test
    @WithJenkins
    void topicRepositoryConfiguredReflectsUrlOrPath(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        configuration.setTopicRepositoryUrl("");
        configuration.setTopicRepositoryPath("");
        assertFalse(configuration.isTopicRepositoryConfigured());

        configuration.setTopicRepositoryUrl("file:///tmp/repo");
        assertTrue(configuration.isTopicRepositoryConfigured());

        configuration.setTopicRepositoryUrl("");
        configuration.setTopicRepositoryPath("/tmp/repo");
        assertTrue(configuration.isTopicRepositoryConfigured());
    }

    @Test
    @WithJenkins
    void cronValidationAllowsBlankAndRejectsInvalid(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        assertEquals(FormValidation.Kind.OK, configuration.doCheckSeedJobCron("").kind);
        assertEquals(FormValidation.Kind.OK, configuration.doCheckSeedJobCron("H/15 * * * *").kind);
        assertEquals(FormValidation.Kind.ERROR, configuration.doCheckSeedJobCron("not a cron").kind);
    }

    @Test
    @WithJenkins
    void buildsToKeepValidationRequiresPositiveInteger(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();

        assertEquals(FormValidation.Kind.OK, configuration.doCheckSeedJobBuildsToKeep("20").kind);
        assertEquals(FormValidation.Kind.ERROR, configuration.doCheckSeedJobBuildsToKeep("0").kind);
        assertEquals(FormValidation.Kind.ERROR, configuration.doCheckSeedJobBuildsToKeep("-3").kind);
        assertEquals(FormValidation.Kind.ERROR, configuration.doCheckSeedJobBuildsToKeep("abc").kind);
    }
}
