package ch.so.agi.jenkins.gretldatenportal;

import hudson.FilePath;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jenkinsci.Symbol;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

public class GretlDatenportalSeedBuilder extends Builder implements SimpleBuildStep {
    private final String topicRepositoryPath;

    @DataBoundConstructor
    public GretlDatenportalSeedBuilder(String topicRepositoryPath) {
        this.topicRepositoryPath = topicRepositoryPath == null ? "" : topicRepositoryPath;
    }

    public String getTopicRepositoryPath() {
        return topicRepositoryPath;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        if (topicRepositoryPath.isBlank()) {
            listener.error("Themen-Repo-Pfad ist nicht konfiguriert.");
            throw new IOException("Themen-Repo-Pfad ist nicht konfiguriert.");
        }

        ScanResult scanResult = new TopicRepositoryScanner().scan(Path.of(topicRepositoryPath));
        for (ValidationMessage message : scanResult.getMessages()) {
            listener.getLogger().printf("%s: %s%s%n",
                    message.getSeverity(),
                    message.getMessage(),
                    message.getPath() == null ? "" : " (" + message.getPath() + ")");
        }
        if (scanResult.hasErrors()) {
            listener.error("Themen-Repo enthaelt Validierungsfehler.");
            throw new IOException("Themen-Repo enthaelt Validierungsfehler.");
        }

        List<String> generated = new GretlDatenportalJobGenerator().generate(scanResult);
        listener.getLogger().println("Generated GRETL Datenportal jobs: " + generated);
    }

    @Extension
    @Symbol("gretlDatenportalJobGenerator")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Generate GRETL Datenportal Jobs";
        }
    }
}
