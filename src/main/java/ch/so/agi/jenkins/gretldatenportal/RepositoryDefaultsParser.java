package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class RepositoryDefaultsParser {
    private final GuiDefinitionParser guiDefinitionParser;

    public RepositoryDefaultsParser() {
        this(new GuiDefinitionParser());
    }

    public RepositoryDefaultsParser(GuiDefinitionParser guiDefinitionParser) {
        this.guiDefinitionParser = guiDefinitionParser;
    }

    public RepositoryDefaults parse(Path yamlFile, Path sharedPath) throws IOException {
        Map<String, Object> root = YamlSupport.loadMap(yamlFile);
        Map<String, Object> execution = YamlSupport.asMap(root.get("execution"));
        return new RepositoryDefaults(
                sharedPath,
                true,
                YamlSupport.stringValue(execution, "gradleTask"),
                YamlSupport.integerValue(execution, "timeoutMinutes"),
                YamlSupport.stringValue(execution, "jenkinsfile"),
                guiDefinitionParser.parse(root),
                NotificationConfiguration.fromYaml(YamlSupport.asMap(root.get("notifications"))));
    }
}
