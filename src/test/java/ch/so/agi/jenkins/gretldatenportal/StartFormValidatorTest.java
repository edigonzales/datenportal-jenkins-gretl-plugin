package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StartFormValidatorTest {
    private final StartFormValidator validator = new StartFormValidator();

    @Test
    void acceptsMetadataOnlyUpload() {
        List<ValidationMessage> messages = validator.validate(
                job(true),
                submission(
                        Map.of(
                                "ORGANISATION", "afu",
                                "DATASET", "ch.so.dataset",
                                "SERIES_ID", "2026"),
                        Map.of("METADATA_FILE", new UploadedFileInfo("metadata.json", 100))));

        assertFalse(hasErrors(messages));
    }

    @Test
    void rejectsMissingUploads() {
        List<ValidationMessage> messages = validator.validate(
                job(false),
                submission(Map.of(
                        "ORGANISATION", "afu",
                        "DATASET", "ch.so.dataset"), Map.of()));

        assertTrue(messages.stream().anyMatch(message -> message.getMessage().contains("At least one")));
    }

    @Test
    void rejectsMissingSeriesId() {
        List<ValidationMessage> messages = validator.validate(
                job(true),
                submission(
                        Map.of(
                                "ORGANISATION", "afu",
                                "DATASET", "ch.so.dataset"),
                        Map.of("DATA_FILE", new UploadedFileInfo("data.csv", 100))));

        assertTrue(messages.stream().anyMatch(message -> message.getMessage().contains("SERIES_ID")));
    }

    @Test
    void doesNotRequireProductionConfirmationAnymore() {
        List<ValidationMessage> messages = validator.validate(
                job(false),
                submission(
                        Map.of(
                                "ORGANISATION", "afu",
                                "DATASET", "ch.so.dataset"),
                        Map.of("DATA_FILE", new UploadedFileInfo("data.csv", 100))));

        assertFalse(hasErrors(messages));
    }

    @Test
    void rejectsNonJsonMetadataUpload() {
        List<ValidationMessage> messages = validator.validate(
                job(false),
                submission(
                        Map.of(
                                "ORGANISATION", "afu",
                                "DATASET", "ch.so.dataset"),
                        Map.of("METADATA_FILE", new UploadedFileInfo("metadata.yaml", 100))));

        assertTrue(messages.stream().anyMatch(message -> message.getMessage().contains("METADATA_FILE")));
    }

    @Test
    void rejectsNonCsvDataUpload() {
        List<ValidationMessage> messages = validator.validate(
                job(false),
                submission(
                        Map.of(
                                "ORGANISATION", "afu",
                                "DATASET", "ch.so.dataset"),
                        Map.of("DATA_FILE", new UploadedFileInfo("data.xlsx", 100))));

        assertTrue(messages.stream().anyMatch(message -> message.getMessage().contains("DATA_FILE")));
    }

    private ResolvedDatenportalJob job(boolean series) {
        OrganizationUnit organization = new OrganizationUnit(
                "afu",
                Path.of("afu"),
                true,
                JobDefinition.defaultFor("afu"),
                GuiDefinition.empty(),
                NotificationConfiguration.disabled(),
                List.of());
        DatasetEntry dataset = new DatasetEntry(
                "ch.so.dataset",
                Path.of("afu/ch.so.dataset"),
                new DatasetDefinition("ch.so.dataset", "Dataset", "", series),
                true,
                false,
                GuiDefinition.empty());
        GuiDefinition gui = new DefaultGuiDefinitionFactory().create(series);
        return new ResolvedDatenportalJob(organization, dataset, gui, NotificationConfiguration.disabled());
    }

    private StartFormSubmission submission(Map<String, String> values, Map<String, UploadedFileInfo> files) {
        return new StartFormSubmission(values, files);
    }

    private boolean hasErrors(List<ValidationMessage> messages) {
        return messages.stream().anyMatch(ValidationMessage::isError);
    }
}
