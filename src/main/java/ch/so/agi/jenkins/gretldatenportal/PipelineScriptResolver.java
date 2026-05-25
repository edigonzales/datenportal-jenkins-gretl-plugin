package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PipelineScriptResolver {
    private final PipelineJobRenderer pipelineJobRenderer;

    public PipelineScriptResolver() {
        this(new PipelineJobRenderer());
    }

    public PipelineScriptResolver(PipelineJobRenderer pipelineJobRenderer) {
        this.pipelineJobRenderer = pipelineJobRenderer;
    }

    public String resolve(OrganizationUnit organization) throws IOException {
        Path explicit = configuredOrganizationJenkinsfile(organization);
        if (explicit != null) {
            return Files.readString(explicit, StandardCharsets.UTF_8);
        }

        Path implicit = organization.getPath().resolve("Jenkinsfile");
        if (Files.isRegularFile(implicit)) {
            return Files.readString(implicit, StandardCharsets.UTF_8);
        }

        Path sharedExplicit = configuredSharedJenkinsfile(organization.getRepositoryDefaults());
        if (sharedExplicit != null) {
            return Files.readString(sharedExplicit, StandardCharsets.UTF_8);
        }

        Path sharedImplicit = implicitSharedJenkinsfile(organization.getRepositoryDefaults());
        if (sharedImplicit != null) {
            return Files.readString(sharedImplicit, StandardCharsets.UTF_8);
        }

        return pipelineJobRenderer.renderBundledDefault(organization, organization.getNotificationConfiguration());
    }

    private Path configuredOrganizationJenkinsfile(OrganizationUnit organization) throws IOException {
        String configured = organization.getJobDefinition().getJenkinsfile();
        return configuredJenkinsfile(organization.getPath(), configured);
    }

    private Path configuredSharedJenkinsfile(RepositoryDefaults repositoryDefaults) throws IOException {
        if (repositoryDefaults == null || !repositoryDefaults.hasSharedPath()) {
            return null;
        }
        return configuredJenkinsfile(repositoryDefaults.getSharedPath(), repositoryDefaults.getJenkinsfile());
    }

    private Path implicitSharedJenkinsfile(RepositoryDefaults repositoryDefaults) {
        if (repositoryDefaults == null || !repositoryDefaults.hasSharedPath()) {
            return null;
        }
        Path implicit = repositoryDefaults.getSharedPath().resolve("Jenkinsfile");
        return Files.isRegularFile(implicit) ? implicit : null;
    }

    private Path configuredJenkinsfile(Path basePath, String configured) throws IOException {
        if (configured == null || configured.isBlank()) {
            return null;
        }

        Path configuredPath = Path.of(configured);
        if (configuredPath.isAbsolute()) {
            throw new IOException("Configured Jenkinsfile path must be relative: " + configured);
        }

        Path normalizedBasePath = basePath.toAbsolutePath().normalize();
        Path candidate = normalizedBasePath.resolve(configuredPath).normalize();
        if (!candidate.startsWith(normalizedBasePath)) {
            throw new IOException("Configured Jenkinsfile path must stay inside its base folder: " + configured);
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException("Configured Jenkinsfile not found: " + candidate);
        }
        return candidate;
    }
}
