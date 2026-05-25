package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public final class DatasetDefinitionParser {
    public ParseResult parse(Path datasetJson) {
        List<ValidationMessage> messages = new ArrayList<>();
        JSONObject json;

        try {
            json = JSONObject.fromObject(Files.readString(datasetJson, StandardCharsets.UTF_8));
        } catch (IOException | JSONException ex) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Could not read or parse dataset.json: " + ex.getMessage(),
                    datasetJson));
            return new ParseResult(null, messages);
        }

        String id = requiredString(json, "id", datasetJson, messages);
        String title = requiredString(json, "title", datasetJson, messages);
        String description = optionalString(json, "description", datasetJson, messages);
        Boolean series = requiredBoolean(json, "series", datasetJson, messages);

        if (!messages.isEmpty()) {
            return new ParseResult(null, messages);
        }

        return new ParseResult(new DatasetDefinition(id, title, description, series), messages);
    }

    private static String requiredString(
            JSONObject json,
            String key,
            Path path,
            List<ValidationMessage> messages) {
        if (!json.containsKey(key)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "dataset.json is missing required string property '" + key + "'.",
                    path));
            return null;
        }
        Object value = json.get(key);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "dataset.json property '" + key + "' must be a non-empty string.",
                    path));
            return null;
        }
        return ((String) value).trim();
    }

    private static String optionalString(
            JSONObject json,
            String key,
            Path path,
            List<ValidationMessage> messages) {
        if (!json.containsKey(key) || json.get(key) == null) {
            return "";
        }
        Object value = json.get(key);
        if (!(value instanceof String)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "dataset.json property '" + key + "' must be a string when present.",
                    path));
            return "";
        }
        return ((String) value).trim();
    }

    private static Boolean requiredBoolean(
            JSONObject json,
            String key,
            Path path,
            List<ValidationMessage> messages) {
        if (!json.containsKey(key)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "dataset.json is missing required boolean property '" + key + "'.",
                    path));
            return null;
        }
        Object value = json.get(key);
        if (!(value instanceof Boolean)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "dataset.json property '" + key + "' must be true or false.",
                    path));
            return null;
        }
        return (Boolean) value;
    }

    public static final class ParseResult {
        private final DatasetDefinition definition;
        private final List<ValidationMessage> messages;

        private ParseResult(DatasetDefinition definition, List<ValidationMessage> messages) {
            this.definition = definition;
            this.messages = List.copyOf(messages);
        }

        public DatasetDefinition getDefinition() {
            return definition;
        }

        public List<ValidationMessage> getMessages() {
            return messages;
        }

        public boolean isValid() {
            return definition != null && messages.stream().noneMatch(ValidationMessage::isError);
        }
    }
}
