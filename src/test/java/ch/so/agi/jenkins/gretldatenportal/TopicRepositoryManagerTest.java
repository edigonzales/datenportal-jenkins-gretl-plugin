package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TopicRepositoryManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void clonesAndUpdatesManagedCheckoutFromLocalGitRepository() throws IOException, InterruptedException {
        Path sourceRepository = tempDir.resolve("source-repo");
        GitTestSupport.initRepository(sourceRepository);
        GitTestSupport.writeSharedJenkinsfile(sourceRepository);
        GitTestSupport.addOrganization(sourceRepository, "afu", "ch.so.abfall.deponien");
        GitTestSupport.commitAll(sourceRepository, "initial topics");

        Path checkoutPath = tempDir.resolve("managed/topic-repo");
        TopicRepositoryManager manager = new TopicRepositoryManager();

        Path initialCheckout = manager.ensureManagedCheckout(
                GitTestSupport.fileUrl(sourceRepository),
                "main",
                checkoutPath,
                false,
                null);

        ScanResult initialScan = new TopicRepositoryScanner().scan(initialCheckout);
        assertEquals(1, initialScan.getOrganizations().size());
        assertTrue(initialCheckout.resolve("afu/gretl-datenportal-job.yaml").toFile().isFile());

        GitTestSupport.addOrganization(sourceRepository, "statistikdienst", "ch.so.statistik.bevoelkerung");
        GitTestSupport.commitAll(sourceRepository, "add statistikdienst");

        Path updatedCheckout = manager.ensureManagedCheckout(
                GitTestSupport.fileUrl(sourceRepository),
                "main",
                checkoutPath,
                true,
                null);

        ScanResult updatedScan = new TopicRepositoryScanner().scan(updatedCheckout);
        assertEquals(2, updatedScan.getOrganizations().size());
        assertTrue(updatedCheckout.resolve("statistikdienst/gretl-datenportal-job.yaml").toFile().isFile());
    }
}
