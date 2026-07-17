package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StartFormViewTest {
    @Test
    void reloadsFormWhenDatasetChangesThroughJavascriptHook() throws IOException {
        String jelly = Files.readString(
                Path.of("src/main/resources/ch/so/agi/jenkins/gretldatenportal/GretlDatenportalRootAction/start.jelly"),
                StandardCharsets.UTF_8);

        assertTrue(jelly.contains("gretl-datenportal.js"));
        assertTrue(jelly.contains("data-gdp-dataset-selector=\"true\""));
        assertTrue(jelly.contains("data-gdp-dataset-start-url=\"${rootURL}/${it.urlName}/start?organization=${organization.id}&amp;dataset="));
        assertTrue(jelly.contains("Bitte Datensatz auswählen"));
    }

    @Test
    void rendersStaticFieldsWithoutConditionHooks() throws IOException {
        String jelly = Files.readString(
                Path.of("src/main/resources/ch/so/agi/jenkins/gretldatenportal/GretlDatenportalRootAction/start.jelly"),
                StandardCharsets.UTF_8);

        assertTrue(jelly.contains("data-gdp-file-input=\"true\""));
        assertTrue(jelly.contains("data-gdp-file-name=\"${field.id}\""));
        assertTrue(jelly.contains("data-gdp-field=\"${field.id}\""));
        assertTrue(jelly.contains("Keine Datei ausgewählt."));
        assertTrue(jelly.contains("Erlaubt: .json"));
        assertTrue(jelly.contains("Erlaubt: .csv"));
        assertFalse(jelly.contains("data-gdp-visible-param"));
        assertFalse(jelly.contains("data-gdp-required-param"));
        assertFalse(jelly.contains("CONFIRM_PRODUCTION"));
        assertFalse(jelly.contains("gdp-context-strip"));
        assertFalse(jelly.contains("Build Preview"));
        assertFalse(jelly.contains("Parameter-Hilfe"));
        assertFalse(jelly.contains("selected=\"${"));
        assertFalse(jelly.contains("checked=\"${"));
    }
}
