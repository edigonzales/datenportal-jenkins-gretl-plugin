package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

class ConfiguredTopicRepositoryTest {
    @Test
    @WithJenkins
    void prefersGitConfigurationOverLegacyPath(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();
        configuration.setTopicRepositoryPath("/tmp/legacy-repo");
        configuration.setTopicRepositoryUrl("file:///tmp/git-repo");
        configuration.setTopicRepositoryBranch("");

        ConfiguredTopicRepository repository = ConfiguredTopicRepository.fromGlobalConfiguration(configuration);

        assertTrue(repository.hasGitRepository());
        assertFalse(repository.hasLegacyPath());
        assertEquals("file:///tmp/git-repo", repository.getUrl());
        assertEquals("main", repository.getBranch());
    }

    @Test
    @WithJenkins
    void fallsBackToLegacyPathWhenGitUrlIsMissing(JenkinsRule jenkinsRule) {
        GretlDatenportalGlobalConfiguration configuration = GretlDatenportalGlobalConfiguration.get();
        configuration.setTopicRepositoryPath("/tmp/legacy-repo");
        configuration.setTopicRepositoryUrl("");
        configuration.setTopicRepositoryBranch("develop");

        ConfiguredTopicRepository repository = ConfiguredTopicRepository.fromGlobalConfiguration(configuration);

        assertFalse(repository.hasGitRepository());
        assertTrue(repository.hasLegacyPath());
        assertEquals(Path.of("/tmp/legacy-repo"), repository.legacyPathAsPath());
        assertEquals("develop", repository.getBranch());
    }
}
