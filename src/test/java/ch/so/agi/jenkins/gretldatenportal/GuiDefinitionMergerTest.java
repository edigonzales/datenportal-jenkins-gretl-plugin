package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiDefinitionMergerTest {
    @TempDir
    Path tempDir;

    @Test
    void mergesGuiOverridesByFieldId() throws IOException {
        GuiDefinition base = new DefaultGuiDefinitionFactory().create(false);
        GuiDefinition override = parseGui(
                """
                gui:
                  fields:
                    - id: ENVIRONMENT
                      defaultValue: production
                    - id: COMMENT
                      required: true
                    - id: EXTRA
                      label: Extra
                      type: string
                      required: true
                """);

        GuiDefinition merged = new GuiDefinitionMerger().merge(base, override);

        assertEquals("production", field(merged, "ENVIRONMENT").getDefaultValue());
        assertEquals(ParameterType.CHOICE, field(merged, "ENVIRONMENT").getType());
        assertTrue(field(merged, "ENVIRONMENT").isRequired());
        assertTrue(field(merged, "COMMENT").isRequired());
        assertEquals("Extra", field(merged, "EXTRA").getLabel());
        assertEquals("ORGANISATION", merged.getFields().get(0).getId());
        assertEquals("EXTRA", merged.getFields().get(merged.getFields().size() - 1).getId());
    }

    private GuiDefinition parseGui(String yaml) throws IOException {
        Path file = tempDir.resolve("dataset-gui.yaml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return new DatasetGuiParser().parse(file);
    }

    private GuiFieldDefinition field(GuiDefinition definition, String id) {
        return definition.getFields().stream()
                .filter(field -> field.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
