package ch.so.agi.jenkins.gretldatenportal;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public final class GretlDatenportalManagedJobProperty extends JobProperty<Job<?, ?>> {

    @DataBoundConstructor
    public GretlDatenportalManagedJobProperty() {
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return Job.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "GRETL Datenportal managed job";
        }
    }
}
