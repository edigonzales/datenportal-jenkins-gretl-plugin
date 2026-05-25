package ch.so.agi.jenkins.gretldatenportal;

import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.TextParameterDefinition;
import hudson.model.Descriptor.FormException;
import io.jenkins.plugins.file_parameters.StashedFileParameterDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

public final class GretlDatenportalJobGenerator {
    private final PipelineScriptResolver pipelineScriptResolver;

    public GretlDatenportalJobGenerator() {
        this(new PipelineScriptResolver());
    }

    public GretlDatenportalJobGenerator(PipelineJobRenderer pipelineJobRenderer) {
        this(new PipelineScriptResolver(pipelineJobRenderer));
    }

    public GretlDatenportalJobGenerator(PipelineScriptResolver pipelineScriptResolver) {
        this.pipelineScriptResolver = pipelineScriptResolver;
    }

    public List<String> generate(ScanResult scanResult) throws IOException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Item.CONFIGURE);

        List<String> generatedJobNames = new ArrayList<>();
        for (OrganizationUnit organization : scanResult.getOrganizations()) {
            if (organization.getDatasets().isEmpty()) {
                continue;
            }
            WorkflowJob job = getOrCreateJob(jenkins, organization.getJobDefinition().getJobName());
            job.setDescription(organization.getJobDefinition().getDescription());
            job.removeProperty(ParametersDefinitionProperty.class);
            job.addProperty(new ParametersDefinitionProperty(parameterDefinitions(organization)));
            try {
                job.setDefinition(new CpsFlowDefinition(
                        pipelineScriptResolver.resolve(organization),
                        true));
            } catch (FormException ex) {
                throw new IOException("Could not render Pipeline job '" + job.getName() + "'.", ex);
            }
            job.save();
            generatedJobNames.add(job.getName());
        }
        return generatedJobNames;
    }

    private WorkflowJob getOrCreateJob(Jenkins jenkins, String name) throws IOException {
        Item existing = jenkins.getItem(name);
        if (existing instanceof WorkflowJob workflowJob) {
            return workflowJob;
        }
        if (existing != null) {
            throw new IOException("Cannot create Pipeline job '" + name + "': item already exists with another type.");
        }
        return jenkins.createProject(WorkflowJob.class, name);
    }

    private List<ParameterDefinition> parameterDefinitions(OrganizationUnit organization) {
        List<ParameterDefinition> parameters = new ArrayList<>();
        parameters.add(new StringParameterDefinition("ORGANISATION", organization.getId(), "Organisationseinheit"));
        parameters.add(new ChoiceParameterDefinition(
                "DATASET",
                organization.getDatasets().stream().map(DatasetEntry::getId).toArray(String[]::new),
                "Datensatz"));
        parameters.add(new ChoiceParameterDefinition(
                "ENVIRONMENT",
                new String[] {"test", "integration", "production"},
                "Umgebung"));
        parameters.add(new StashedFileParameterDefinition("METADATA_FILE"));
        parameters.add(new StashedFileParameterDefinition("DATA_FILE"));
        parameters.add(new BooleanParameterDefinition("DRY_RUN", true, "Dry Run"));
        parameters.add(new TextParameterDefinition("COMMENT", "", "Kommentar"));
        parameters.add(new BooleanParameterDefinition("CONFIRM_PRODUCTION", false, "Production bestaetigen"));
        parameters.add(new StringParameterDefinition("SERIES_ID", "", "Serie"));
        return parameters;
    }
}
