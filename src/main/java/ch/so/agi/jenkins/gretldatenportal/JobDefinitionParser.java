package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class JobDefinitionParser {
    private final GuiDefinitionParser guiDefinitionParser;

    public JobDefinitionParser() {
        this(new GuiDefinitionParser());
    }

    public JobDefinitionParser(GuiDefinitionParser guiDefinitionParser) {
        this.guiDefinitionParser = guiDefinitionParser;
    }

    public OrganizationJobConfiguration parse(Path yamlFile, String fallbackId) throws IOException {
        Map<String, Object> root = YamlSupport.loadMap(yamlFile);
        Map<String, Object> execution = YamlSupport.asMap(root.get("execution"));
        Integer configuredTimeout = YamlSupport.integerValue(execution, "timeoutMinutes");
        int timeout = valueOrDefault(configuredTimeout, 60);

        String id = YamlSupport.stringValue(root, "id");
        if (id.isBlank()) {
            id = fallbackId;
        }

        JobDefinition jobDefinition = new JobDefinition(
                id,
                YamlSupport.stringValue(root, "title"),
                YamlSupport.stringValue(root, "description"),
                YamlSupport.stringValue(execution, "jobName"),
                YamlSupport.stringValue(execution, "gradleTask"),
                YamlSupport.stringValue(execution, "jenkinsfile"),
                timeout,
                hasNonBlankValue(execution, "jobName"),
                hasNonBlankValue(execution, "gradleTask"),
                hasNonBlankValue(execution, "jenkinsfile"),
                configuredTimeout != null);

        NotificationConfiguration notificationConfiguration =
                NotificationConfiguration.fromYaml(YamlSupport.asMap(root.get("notifications")));
        PermissionConfiguration permissionConfiguration =
                PermissionConfiguration.fromYaml(YamlSupport.asMap(root.get("permissions")));

        return new OrganizationJobConfiguration(
                jobDefinition,
                guiDefinitionParser.parse(root),
                notificationConfiguration,
                permissionConfiguration);
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean hasNonBlankValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null && !value.toString().isBlank();
    }
}
