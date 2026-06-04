package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RootActionViewAssetsTest {
    @Test
    void rootViewIncludesOnlyExecutedRunsAndNavigationToJobs() throws IOException {
        String jelly = readView("index.jelly");

        assertTrue(jelly.contains("gretl-datenportal.css"));
        assertTrue(jelly.contains("gretl-datenportal.js"));
        assertTrue(jelly.contains("Ausgeführte Jobs"));
        assertTrue(jelly.contains("data-gdp-filter-list=\"runs\""));
        assertTrue(jelly.contains("data-gdp-filter-control=\"organization\""));
        assertTrue(jelly.contains("data-gdp-filter-control=\"dataset\""));
        assertTrue(jelly.contains("href=\"${rootURL}/${it.urlName}/jobs\""));
        assertFalse(jelly.contains("gdp-job-list"));
    }

    @Test
    void jobsViewIncludesStartOverviewWithoutDatasetParameter() throws IOException {
        String jelly = readView("jobs.jelly");

        assertTrue(jelly.contains("Jobs starten"));
        assertTrue(jelly.contains("gdp-job-list"));
        assertTrue(jelly.contains("start?organization=${job.organization}\""));
        assertFalse(jelly.contains("&amp;dataset="));
        assertFalse(jelly.contains("${job.firstDatasetId}"));
    }

    @Test
    void runViewIncludesLogExcerptAndConsoleLink() throws IOException {
        String jelly = readView("run.jelly");

        assertTrue(jelly.contains("gretl-datenportal.css"));
        assertTrue(jelly.contains("gretl-datenportal.js"));
        assertTrue(jelly.contains("Jobstatus"));
        assertTrue(jelly.contains("data-gdp-status=\"${details.status}\""));
        assertTrue(jelly.contains("data-gdp-run-status-view=\"true\""));
        assertTrue(jelly.contains("data-gdp-run-status-url=\"true\""));
        assertTrue(jelly.contains("Logausgabe"));
        assertTrue(jelly.contains("details.logText"));
        assertFalse(jelly.contains("items=\"${details.logLines}\""));
        assertTrue(jelly.contains("Vollständiges Log öffnen"));
    }

    @Test
    void javascriptEnhancesUploadsAndListFiltering() throws IOException {
        String script = Files.readString(Path.of("src/main/webapp/js/gretl-datenportal.js"), StandardCharsets.UTF_8);

        assertTrue(script.contains("data-gdp-file-input"));
        assertFalse(script.contains("data-gdp-visible-param"));
        assertFalse(script.contains("data-gdp-required-param"));
        assertTrue(script.contains("data-gdp-filter-item"));
        assertTrue(script.contains("data-gdp-run-status-view"));
        assertTrue(script.contains("window.fetch"));
        assertTrue(script.contains("5000"));

        String css = Files.readString(Path.of("src/main/webapp/css/gretl-datenportal.css"), StandardCharsets.UTF_8);
        assertTrue(css.contains("[hidden]"));
        assertTrue(css.contains("display: none !important"));
        assertTrue(css.contains("overflow-y: auto"));
        assertTrue(css.contains("overflow-x: hidden"));
        assertTrue(css.contains("data-gdp-status=\"QUEUED\""));
        assertTrue(css.contains("data-gdp-status=\"RUNNING\""));
    }

    private String readView(String name) throws IOException {
        return Files.readString(
                Path.of("src/main/resources/ch/so/agi/jenkins/gretldatenportal/GretlDatenportalRootAction", name),
                StandardCharsets.UTF_8);
    }
}
