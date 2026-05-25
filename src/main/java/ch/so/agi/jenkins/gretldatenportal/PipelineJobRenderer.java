package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class PipelineJobRenderer {
    private static final String TEMPLATE_RESOURCE = "/ch/so/agi/jenkins/gretldatenportal/shared/Jenkinsfile";
    private static final String TIMEOUT_PLACEHOLDER = "@@TIMEOUT_MINUTES@@";
    private static final String GRADLE_TASK_PLACEHOLDER = "@@GRADLE_TASK@@";
    private static final String POST_BLOCK_PLACEHOLDER = "@@POST_BLOCK@@";

    private final EmailNotificationService emailNotificationService;

    public PipelineJobRenderer() {
        this(new EmailNotificationService());
    }

    public PipelineJobRenderer(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    public String render(OrganizationUnit organization, NotificationConfiguration notificationConfiguration) {
        return renderBundledDefault(organization, notificationConfiguration);
    }

    public String renderBundledDefault(
            OrganizationUnit organization,
            NotificationConfiguration notificationConfiguration) {
        return renderTemplate(loadBundledTemplate(), organization, notificationConfiguration);
    }

    String loadBundledTemplate() {
        try (InputStream inputStream = PipelineJobRenderer.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Bundled Jenkinsfile template resource not found: " + TEMPLATE_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load bundled Jenkinsfile template.", ex);
        }
    }

    String renderTemplate(
            String template,
            OrganizationUnit organization,
            NotificationConfiguration notificationConfiguration) {
        JobDefinition jobDefinition = organization.getJobDefinition();
        String postBlock = emailNotificationService.renderPostBlock(notificationConfiguration);
        if (postBlock.isBlank()) {
            postBlock = "post { always { echo 'GRETL Datenportal job finished.' } }";
        }

        return template
                .replace(TIMEOUT_PLACEHOLDER, Integer.toString(jobDefinition.getTimeoutMinutes()))
                .replace(GRADLE_TASK_PLACEHOLDER, escapeGroovy(jobDefinition.getGradleTask()))
                .replace(POST_BLOCK_PLACEHOLDER, indent(postBlock, 4));
    }

    private static String indent(String value, int spaces) {
        String prefix = " ".repeat(spaces);
        return value.lines().map(line -> prefix + line).reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private static String escapeGroovy(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
