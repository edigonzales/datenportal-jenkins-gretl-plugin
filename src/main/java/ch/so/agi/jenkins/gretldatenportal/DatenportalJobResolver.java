package ch.so.agi.jenkins.gretldatenportal;

public final class DatenportalJobResolver {
    private final DefaultGuiDefinitionFactory defaultGuiDefinitionFactory;
    private final GuiDefinitionMerger guiDefinitionMerger;

    public DatenportalJobResolver() {
        this(new DefaultGuiDefinitionFactory(), new GuiDefinitionMerger());
    }

    public DatenportalJobResolver(
            DefaultGuiDefinitionFactory defaultGuiDefinitionFactory,
            GuiDefinitionMerger guiDefinitionMerger) {
        this.defaultGuiDefinitionFactory = defaultGuiDefinitionFactory;
        this.guiDefinitionMerger = guiDefinitionMerger;
    }

    public ResolvedDatenportalJob resolve(OrganizationUnit organization, DatasetEntry dataset) {
        GuiDefinition gui = guiDefinitionMerger.merge(
                defaultGuiDefinitionFactory.create(dataset.getDefinition().isSeries()),
                organization.getRepositoryDefaults().getGuiDefinition(),
                organization.getOrganizationGui(),
                dataset.getDatasetGui());
        return new ResolvedDatenportalJob(
                organization,
                dataset,
                gui,
                organization.getNotificationConfiguration());
    }
}
