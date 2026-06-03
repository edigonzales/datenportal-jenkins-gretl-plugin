package ch.so.agi.jenkins.gretldatenportal;

import hudson.Extension;
import hudson.util.FormValidation;
import java.nio.file.Path;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

@Extension
@Symbol("gretlDatenportalJobs")
public class GretlDatenportalGlobalConfiguration extends GlobalConfiguration {
    private String displayName = "GRETL Datenportal Jobs";
    private String urlName = "gretl-datenportal";
    private String topicRepositoryUrl = "";
    private String topicRepositoryBranch = "main";
    private String topicRepositoryPath = "";

    public GretlDatenportalGlobalConfiguration() {
        load();
    }

    public static GretlDatenportalGlobalConfiguration get() {
        return GlobalConfiguration.all().get(GretlDatenportalGlobalConfiguration.class);
    }

    @Override
    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? "GRETL Datenportal Jobs" : displayName;
    }

    @DataBoundSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        save();
    }

    public String getUrlName() {
        return urlName == null || urlName.isBlank() ? "gretl-datenportal" : urlName;
    }

    @DataBoundSetter
    public void setUrlName(String urlName) {
        this.urlName = urlName;
        save();
    }

    public String getTopicRepositoryPath() {
        return topicRepositoryPath == null ? "" : topicRepositoryPath;
    }

    public String getTopicRepositoryUrl() {
        return topicRepositoryUrl == null ? "" : topicRepositoryUrl;
    }

    @DataBoundSetter
    public void setTopicRepositoryUrl(String topicRepositoryUrl) {
        this.topicRepositoryUrl = topicRepositoryUrl;
        save();
    }

    public String getTopicRepositoryBranch() {
        return topicRepositoryBranch == null || topicRepositoryBranch.isBlank() ? "main" : topicRepositoryBranch;
    }

    @DataBoundSetter
    public void setTopicRepositoryBranch(String topicRepositoryBranch) {
        this.topicRepositoryBranch = topicRepositoryBranch;
        save();
    }

    public Path getTopicRepositoryPathAsPath() {
        String configuredPath = getTopicRepositoryPath();
        if (configuredPath.isBlank()) {
            return null;
        }
        return Path.of(configuredPath);
    }

    @DataBoundSetter
    public void setTopicRepositoryPath(String topicRepositoryPath) {
        this.topicRepositoryPath = topicRepositoryPath;
        save();
    }

    public FormValidation doCheckDisplayName(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.warning("A display name is recommended.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckUrlName(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.error("URL name is required.");
        }
        if (!value.matches("^[a-z0-9][a-z0-9-]*$")) {
            return FormValidation.error("Use lower-case letters, digits, and hyphens.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckTopicRepositoryUrl(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        if (!value.contains("://")) {
            return FormValidation.warning("A Git URL such as file://... or https://... is recommended.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckTopicRepositoryBranch(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        if (value.contains(" ")) {
            return FormValidation.error("Git branch names must not contain spaces.");
        }
        return FormValidation.ok();
    }
}
