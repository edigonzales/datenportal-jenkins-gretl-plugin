package ch.so.agi.jenkins.gretldatenportal;

import java.nio.file.Path;

final class ConfiguredTopicRepository {
    private final String legacyPath;
    private final String url;
    private final String branch;

    private ConfiguredTopicRepository(String legacyPath, String url, String branch) {
        this.legacyPath = blankToEmpty(legacyPath);
        this.url = blankToEmpty(url);
        this.branch = blankToEmpty(branch);
    }

    static ConfiguredTopicRepository resolve(
            String builderPath,
            String builderUrl,
            String builderBranch,
            GretlDatenportalGlobalConfiguration configuration) {
        String globalPath = configuration == null ? "" : configuration.getTopicRepositoryPath();
        String globalUrl = configuration == null ? "" : configuration.getTopicRepositoryUrl();
        String globalBranch = configuration == null ? "main" : configuration.getTopicRepositoryBranch();

        if (!isBlank(builderPath)) {
            return new ConfiguredTopicRepository(builderPath, "", "");
        }
        if (!isBlank(builderUrl)) {
            return new ConfiguredTopicRepository("", builderUrl, isBlank(builderBranch) ? globalBranch : builderBranch);
        }
        if (!isBlank(globalUrl)) {
            return new ConfiguredTopicRepository("", globalUrl, globalBranch);
        }
        return new ConfiguredTopicRepository(globalPath, "", globalBranch);
    }

    static ConfiguredTopicRepository fromGlobalConfiguration(GretlDatenportalGlobalConfiguration configuration) {
        return resolve("", "", "", configuration);
    }

    Path legacyPathAsPath() {
        return hasLegacyPath() ? Path.of(legacyPath) : null;
    }

    boolean hasLegacyPath() {
        return !legacyPath.isBlank();
    }

    boolean hasGitRepository() {
        return !url.isBlank();
    }

    String getUrl() {
        return url;
    }

    String getBranch() {
        return branch.isBlank() ? "main" : branch;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
