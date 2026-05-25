package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultGuiDefinitionFactoryTest {
    private final DefaultGuiDefinitionFactory factory = new DefaultGuiDefinitionFactory();

    @Test
    void rendersNoModeAndOptionalCommentByDefault() {
        GuiDefinition gui = factory.create(false);

        assertFalse(hasField(gui, "MODE"));
        assertFalse(field(gui, "COMMENT").isRequired());
    }

    @Test
    void rendersSeriesIdOnlyForSeriesDatasets() {
        assertTrue(hasField(factory.create(true), "SERIES_ID"));
        assertFalse(hasField(factory.create(false), "SERIES_ID"));
    }

    @Test
    void restrictsDefaultUploadsToJsonAndCsv() {
        GuiDefinition gui = factory.create(false);

        assertTrue(field(gui, "METADATA_FILE").getAllowedExtensions().contains("json"));
        assertFalse(field(gui, "METADATA_FILE").getAllowedExtensions().contains("yaml"));
        assertTrue(field(gui, "DATA_FILE").getAllowedExtensions().contains("csv"));
        assertFalse(field(gui, "DATA_FILE").getAllowedExtensions().contains("xlsx"));
        assertTrue(field(gui, "CONFIRM_PRODUCTION").getLabel().contains("bestätigen"));
    }

    private boolean hasField(GuiDefinition guiDefinition, String id) {
        return guiDefinition.getFields().stream().anyMatch(field -> field.getId().equals(id));
    }

    private GuiFieldDefinition field(GuiDefinition guiDefinition, String id) {
        return guiDefinition.getFields().stream()
                .filter(field -> field.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
