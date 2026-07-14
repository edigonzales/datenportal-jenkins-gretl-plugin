package ch.so.agi.jenkins.gretldatenportal;

import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.triggers.TimerTrigger.DescriptorImpl;
import hudson.util.FormValidation;
import java.io.IOException;
import java.nio.file.Path;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import net.sf.json.JSONObject;

@Extension
@Symbol("gretlDatenportalJobs")
public class GretlDatenportalGlobalConfiguration extends GlobalConfiguration {
    private String displayName = "GRETL Datenportal Jobs";
    private String urlName = "gretl-datenportal";
    private String topicRepositoryUrl = "";
    private String topicRepositoryBranch = "main";
    private String topicRepositoryPath = "";
    private boolean seedJobAutoCreate = true;
    private String seedJobCron = null;
    private int seedJobBuildsToKeep = -1;
    private String seedJobOperatorsTeam = "gretl-datenportal-seed-operators";

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

    public boolean isSeedJobAutoCreate() {
        return seedJobAutoCreate;
    }

    @DataBoundSetter
    public void setSeedJobAutoCreate(boolean seedJobAutoCreate) {
        this.seedJobAutoCreate = seedJobAutoCreate;
        save();
    }

    public String getSeedJobCron() {
        if (seedJobCron == null) {
            return GretlDatenportalSeedJobProvisioner.DEFAULT_SEED_JOB_CRON;
        }
        return seedJobCron;
    }

    @DataBoundSetter
    public void setSeedJobCron(String seedJobCron) {
        this.seedJobCron = seedJobCron == null ? null : seedJobCron.strip();
        save();
    }

    public int getSeedJobBuildsToKeep() {
        return seedJobBuildsToKeep <= 0 ? 20 : seedJobBuildsToKeep;
    }

    @DataBoundSetter
    public void setSeedJobBuildsToKeep(int seedJobBuildsToKeep) {
        this.seedJobBuildsToKeep = seedJobBuildsToKeep;
        save();
    }

    public String getSeedJobOperatorsTeam() {
        return seedJobOperatorsTeam == null ? "" : seedJobOperatorsTeam;
    }

    @DataBoundSetter
    public void setSeedJobOperatorsTeam(String seedJobOperatorsTeam) {
        this.seedJobOperatorsTeam = seedJobOperatorsTeam == null ? "" : seedJobOperatorsTeam.strip();
        save();
    }

    public boolean isTopicRepositoryConfigured() {
        return !getTopicRepositoryUrl().isBlank() || !getTopicRepositoryPath().isBlank();
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        boolean result = super.configure(req, json);
        try {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null) {
                GretlDatenportalSeedJobProvisioner.provisionFromConfiguration(this);
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw new FormException(e.getCause(), "seedJobAutoCreate");
            }
            throw new FormException(e, "seedJobAutoCreate");
        }
        return result;
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

    public FormValidation doCheckSeedJobCron(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(DescriptorImpl.class);
        if (descriptor != null) {
            return descriptor.doCheckSpec(value, null);
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckSeedJobBuildsToKeep(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        try {
            int intValue = Integer.parseInt(value);
            if (intValue <= 0) {
                return FormValidation.error("Must be a positive integer.");
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Must be a positive integer.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckSeedJobOperatorsTeam(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        if (!value.matches("^[A-Za-z0-9][A-Za-z0-9_.-]*$")) {
            return FormValidation.error("Use a valid team id.");
        }
        return FormValidation.ok();
    }
}
