package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TopicRepositoryScanner {
    public static final String ORGANIZATION_JOB_FILE = "gretl-datenportal-job.yaml";
    public static final String DATASET_DEFINITION_FILE = "dataset.json";
    public static final String DATASET_GUI_FILE = "dataset-gui.yaml";
    public static final String SHARED_DIR = "shared";
    public static final String SHARED_JENKINSFILE = "Jenkinsfile";
    public static final String REPOSITORY_DEFAULTS_FILE = "gretl-datenportal-defaults.yaml";

    private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]*$");

    private final DatasetDefinitionParser datasetDefinitionParser;
    private final DatasetGuiParser datasetGuiParser;
    private final JobDefinitionParser jobDefinitionParser;
    private final RepositoryDefaultsParser repositoryDefaultsParser;
    private final JobDefinitionValidator jobDefinitionValidator;

    public TopicRepositoryScanner() {
        this(
                new DatasetDefinitionParser(),
                new DatasetGuiParser(),
                new JobDefinitionParser(),
                new RepositoryDefaultsParser(),
                new JobDefinitionValidator());
    }

    public TopicRepositoryScanner(
            DatasetDefinitionParser datasetDefinitionParser,
            DatasetGuiParser datasetGuiParser,
            JobDefinitionParser jobDefinitionParser,
            RepositoryDefaultsParser repositoryDefaultsParser,
            JobDefinitionValidator jobDefinitionValidator) {
        this.datasetDefinitionParser = datasetDefinitionParser;
        this.datasetGuiParser = datasetGuiParser;
        this.jobDefinitionParser = jobDefinitionParser;
        this.repositoryDefaultsParser = repositoryDefaultsParser;
        this.jobDefinitionValidator = jobDefinitionValidator;
    }

    public ScanResult scan(Path repositoryPath) {
        List<ValidationMessage> messages = new ArrayList<>();
        List<OrganizationUnit> organizations = new ArrayList<>();

        if (repositoryPath == null) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Topic repository path is not configured.",
                    null));
            return new ScanResult(null, organizations, messages);
        }

        if (!Files.isDirectory(repositoryPath)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Topic repository path is not a directory.",
                    repositoryPath));
            return new ScanResult(repositoryPath, organizations, messages);
        }

        RepositoryDefaults repositoryDefaults = scanRepositoryDefaults(repositoryPath, messages);

        try (Stream<Path> orgDirs = Files.list(repositoryPath)) {
            orgDirs.filter(Files::isDirectory)
                    .filter(path -> !isHidden(path))
                    .filter(path -> !SHARED_DIR.equals(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        OrganizationUnit organization = scanOrganization(path, repositoryDefaults, messages);
                        if (organization != null) {
                            organizations.add(organization);
                        }
                    });
        } catch (IOException ex) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Could not list topic repository: " + ex.getMessage(),
                    repositoryPath));
        }

        validateSharedDefaultPipeline(repositoryPath, repositoryDefaults, organizations, messages);
        return new ScanResult(repositoryPath, organizations, messages);
    }

    private RepositoryDefaults scanRepositoryDefaults(Path repositoryPath, List<ValidationMessage> messages) {
        Path sharedPath = repositoryPath.resolve(SHARED_DIR);
        if (!Files.isDirectory(sharedPath)) {
            return RepositoryDefaults.empty();
        }

        Path defaultsFile = sharedPath.resolve(REPOSITORY_DEFAULTS_FILE);
        if (!Files.isRegularFile(defaultsFile)) {
            return RepositoryDefaults.forSharedPath(sharedPath);
        }

        try {
            RepositoryDefaults defaults = repositoryDefaultsParser.parse(defaultsFile, sharedPath);
            messages.addAll(jobDefinitionValidator.validateGui(defaults.getGuiDefinition()));
            return defaults;
        } catch (Exception ex) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Could not parse " + REPOSITORY_DEFAULTS_FILE + ": " + ex.getMessage(),
                    defaultsFile));
            return RepositoryDefaults.forSharedPath(sharedPath);
        }
    }

    private OrganizationUnit scanOrganization(
            Path orgDir,
            RepositoryDefaults repositoryDefaults,
            List<ValidationMessage> messages) {
        String orgId = orgDir.getFileName().toString();
        if (!isValidRepositoryName(orgId)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Organization folder name is invalid: " + orgId,
                    orgDir));
            return null;
        }

        Path organizationJobFile = orgDir.resolve(ORGANIZATION_JOB_FILE);
        boolean jobDefinitionPresent = Files.isRegularFile(organizationJobFile);
        if (!jobDefinitionPresent) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Organization is missing " + ORGANIZATION_JOB_FILE + " and will be ignored.",
                    orgDir));
            return null;
        }

        OrganizationJobConfiguration configuration;
        try {
            configuration = jobDefinitionParser.parse(organizationJobFile, orgId);
        } catch (Exception ex) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Could not parse " + ORGANIZATION_JOB_FILE + ": " + ex.getMessage(),
                    organizationJobFile));
            return null;
        }

        JobDefinition jobDefinition = configuration.getJobDefinition();
        GuiDefinition organizationGui = configuration.getGuiDefinition();
        NotificationConfiguration notificationConfiguration = configuration.getNotificationConfiguration();
        PermissionConfiguration permissionConfiguration = configuration.getPermissionConfiguration();

        List<ValidationMessage> organizationMessages = new ArrayList<>();
        if (!orgId.equals(jobDefinition.getId())) {
            organizationMessages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    ORGANIZATION_JOB_FILE + " id must match organization folder name.",
                    organizationJobFile));
        }
        organizationMessages.addAll(jobDefinitionValidator.validatePermissions(permissionConfiguration, organizationJobFile));
        organizationMessages.addAll(jobDefinitionValidator.validateGui(organizationGui));
        messages.addAll(organizationMessages);
        if (organizationMessages.stream().anyMatch(message -> message.getSeverity() == ValidationMessage.Severity.ERROR)) {
            return null;
        }

        jobDefinition = mergeJobDefinition(orgId, jobDefinition, repositoryDefaults);
        notificationConfiguration = repositoryDefaults.getNotificationConfiguration().merge(notificationConfiguration);

        List<DatasetEntry> datasets = new ArrayList<>();
        try (Stream<Path> datasetDirs = Files.list(orgDir)) {
            datasetDirs.filter(Files::isDirectory)
                    .filter(path -> !isHidden(path))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> datasets.add(scanDataset(path, messages)));
        } catch (IOException ex) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Could not list organization folder: " + ex.getMessage(),
                    orgDir));
        }

        if (datasets.isEmpty()) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.INFO,
                    "Organization contains no dataset folders.",
                    orgDir));
        }

        OrganizationUnit organization = new OrganizationUnit(
                orgId,
                orgDir,
                jobDefinitionPresent,
                jobDefinition,
                organizationGui,
                notificationConfiguration,
                permissionConfiguration,
                repositoryDefaults,
                datasets);
        messages.addAll(jobDefinitionValidator.validateOrganization(organization));
        return organization;
    }

    private JobDefinition mergeJobDefinition(
            String orgId,
            JobDefinition organizationDefinition,
            RepositoryDefaults repositoryDefaults) {
        return new JobDefinition(
                organizationDefinition.getId(),
                organizationDefinition.getTitle(),
                organizationDefinition.getDescription(),
                organizationDefinition.getJobName(),
                organizationDefinition.isGradleTaskSpecified()
                        ? organizationDefinition.getGradleTask()
                        : sharedOrDefaultGradleTask(repositoryDefaults, orgId),
                organizationDefinition.getJenkinsfile(),
                organizationDefinition.isTimeoutMinutesSpecified()
                        ? organizationDefinition.getTimeoutMinutes()
                        : sharedOrDefaultTimeout(repositoryDefaults),
                organizationDefinition.isJobNameSpecified(),
                organizationDefinition.isGradleTaskSpecified() || repositoryDefaults.hasGradleTask(),
                organizationDefinition.isJenkinsfileSpecified(),
                organizationDefinition.isTimeoutMinutesSpecified() || repositoryDefaults.hasTimeoutMinutes());
    }

    private String sharedOrDefaultGradleTask(RepositoryDefaults repositoryDefaults, String orgId) {
        if (repositoryDefaults.hasGradleTask()) {
            return repositoryDefaults.getGradleTask();
        }
        return JobDefinition.defaultFor(orgId).getGradleTask();
    }

    private int sharedOrDefaultTimeout(RepositoryDefaults repositoryDefaults) {
        if (repositoryDefaults.hasTimeoutMinutes()) {
            return repositoryDefaults.getTimeoutMinutes();
        }
        return 60;
    }

    private void validateSharedDefaultPipeline(
            Path repositoryPath,
            RepositoryDefaults repositoryDefaults,
            List<OrganizationUnit> organizations,
            List<ValidationMessage> messages) {
        boolean sharedDefaultRequired = organizations.stream()
                .anyMatch(organization -> requiresImplicitSharedDefault(organization, repositoryDefaults));
        if (!sharedDefaultRequired) {
            return;
        }

        Path sharedJenkinsfile = sharedJenkinsfilePath(repositoryPath, repositoryDefaults);
        if (!Files.isRegularFile(sharedJenkinsfile)) {
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Topic repository is missing shared/Jenkinsfile for organizations without a custom Jenkinsfile.",
                    sharedJenkinsfile));
        }
    }

    private boolean requiresImplicitSharedDefault(
            OrganizationUnit organization,
            RepositoryDefaults repositoryDefaults) {
        if (organization.getJobDefinition().isJenkinsfileSpecified()) {
            return false;
        }
        if (Files.isRegularFile(organization.getPath().resolve(SHARED_JENKINSFILE))) {
            return false;
        }
        return repositoryDefaults == null || !repositoryDefaults.hasJenkinsfile();
    }

    private Path sharedJenkinsfilePath(Path repositoryPath, RepositoryDefaults repositoryDefaults) {
        if (repositoryDefaults != null && repositoryDefaults.hasSharedPath()) {
            return repositoryDefaults.getSharedPath().resolve(SHARED_JENKINSFILE);
        }
        return repositoryPath.resolve(SHARED_DIR).resolve(SHARED_JENKINSFILE);
    }

    private DatasetEntry scanDataset(Path datasetDir, List<ValidationMessage> messages) {
        String folderId = datasetDir.getFileName().toString();
        boolean valid = true;

        if (!isValidRepositoryName(folderId)) {
            valid = false;
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Dataset folder name is invalid: " + folderId,
                    datasetDir));
        }

        DatasetDefinition definition = new DatasetDefinition(folderId, folderId, "", false);
        Path datasetJson = datasetDir.resolve(DATASET_DEFINITION_FILE);

        if (!Files.isRegularFile(datasetJson)) {
            valid = false;
            messages.add(new ValidationMessage(
                    ValidationMessage.Severity.ERROR,
                    "Dataset folder is missing " + DATASET_DEFINITION_FILE + ".",
                    datasetDir));
        } else {
            DatasetDefinitionParser.ParseResult parseResult = datasetDefinitionParser.parse(datasetJson);
            messages.addAll(parseResult.getMessages());
            if (parseResult.isValid()) {
                definition = parseResult.getDefinition();
                if (!folderId.equals(definition.getId())) {
                    valid = false;
                    messages.add(new ValidationMessage(
                            ValidationMessage.Severity.ERROR,
                            "dataset.json id must match dataset folder name.",
                            datasetJson));
                }
            } else {
                valid = false;
            }
        }

        Path datasetGuiFile = datasetDir.resolve(DATASET_GUI_FILE);
        boolean datasetGuiPresent = Files.isRegularFile(datasetGuiFile);
        GuiDefinition datasetGui = GuiDefinition.empty();
        if (datasetGuiPresent) {
            try {
                datasetGui = datasetGuiParser.parse(datasetGuiFile);
                messages.addAll(jobDefinitionValidator.validateGui(datasetGui));
            } catch (Exception ex) {
                valid = false;
                messages.add(new ValidationMessage(
                        ValidationMessage.Severity.ERROR,
                        "Could not parse " + DATASET_GUI_FILE + ": " + ex.getMessage(),
                        datasetGuiFile));
            }
        }
        return new DatasetEntry(folderId, datasetDir, definition, valid, datasetGuiPresent, datasetGui);
    }

    private static boolean isHidden(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(".");
    }

    private static boolean isValidRepositoryName(String value) {
        return value != null && REPOSITORY_NAME_PATTERN.matcher(value).matches()
                && value.equals(value.toLowerCase(Locale.ROOT));
    }
}
