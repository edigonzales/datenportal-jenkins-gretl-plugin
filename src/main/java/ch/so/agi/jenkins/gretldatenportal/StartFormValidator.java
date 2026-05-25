package ch.so.agi.jenkins.gretldatenportal;

import java.util.ArrayList;
import java.util.List;

public final class StartFormValidator {
    public List<ValidationMessage> validate(ResolvedDatenportalJob job, StartFormSubmission submission) {
        List<ValidationMessage> messages = new ArrayList<>();
        OrganizationUnit organization = job.getOrganization();
        DatasetEntry dataset = job.getDataset();

        if (!organization.getId().equals(submission.value("ORGANISATION"))) {
            messages.add(error("ORGANISATION must match the selected organization."));
        }
        if (!dataset.getId().equals(submission.value("DATASET"))) {
            messages.add(error("DATASET must belong to the selected organization."));
        }

        for (GuiFieldDefinition field : job.getGuiDefinition().getFields()) {
            if (isRequired(field, submission) && isBlank(submission.value(field.getId()))
                    && field.getType() != ParameterType.FILE) {
                messages.add(error(field.getId() + " is required."));
            }
            if (field.getType() == ParameterType.FILE && submission.hasFile(field.getId())) {
                validateFile(field, submission.file(field.getId()), messages);
            }
        }

        if (!submission.hasFile("METADATA_FILE") && !submission.hasFile("DATA_FILE")) {
            messages.add(error("At least one of METADATA_FILE or DATA_FILE is required."));
        }
        if (dataset.getDefinition().isSeries() && isBlank(submission.value("SERIES_ID"))) {
            messages.add(error("SERIES_ID is required for series datasets."));
        }
        if ("production".equals(submission.value("ENVIRONMENT"))
                && !"true".equalsIgnoreCase(submission.value("CONFIRM_PRODUCTION"))) {
            messages.add(error("CONFIRM_PRODUCTION must be true for production builds."));
        }
        return messages;
    }

    private boolean isRequired(GuiFieldDefinition field, StartFormSubmission submission) {
        if (field.isRequired()) {
            return true;
        }
        Condition requiredIf = field.getRequiredIf();
        return requiredIf != null && requiredIf.getEquals().equals(submission.value(requiredIf.getParameter()));
    }

    private void validateFile(GuiFieldDefinition field, UploadedFileInfo file, List<ValidationMessage> messages) {
        List<String> allowedExtensions = allowedExtensions(field);
        if (!allowedExtensions.isEmpty()
                && !allowedExtensions.contains(file.getExtension())) {
            messages.add(error(field.getId() + " has an invalid file extension."));
        }
        if (field.getMaxSizeMb() != null) {
            long maxBytes = field.getMaxSizeMb() * 1024L * 1024L;
            if (file.getSizeBytes() > maxBytes) {
                messages.add(error(field.getId() + " exceeds the maximum file size."));
            }
        }
    }

    private List<String> allowedExtensions(GuiFieldDefinition field) {
        return switch (field.getId()) {
            case "METADATA_FILE" -> List.of("json");
            case "DATA_FILE" -> List.of("csv");
            default -> field.getAllowedExtensions();
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ValidationMessage error(String message) {
        return new ValidationMessage(ValidationMessage.Severity.ERROR, message, null);
    }
}
