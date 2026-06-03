package ch.so.agi.jenkins.gretldatenportal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import jenkins.model.Jenkins;

final class TopicRepositoryManager {
    static final String MANAGED_REPOSITORY_RELATIVE_PATH = "gretl-datenportal/topic-repo";

    Path resolveRepositoryPath(GretlDatenportalGlobalConfiguration configuration, boolean update)
            throws IOException, InterruptedException {
        return resolveRepositoryPath(
                ConfiguredTopicRepository.fromGlobalConfiguration(configuration),
                update,
                null);
    }

    Path resolveRepositoryPath(
            ConfiguredTopicRepository repository,
            boolean update,
            Consumer<String> logger) throws IOException, InterruptedException {
        Objects.requireNonNull(repository, "repository");

        if (repository.hasGitRepository()) {
            return ensureManagedCheckout(repository.getUrl(), repository.getBranch(), managedRepositoryPath(), update, logger);
        }
        if (repository.hasLegacyPath()) {
            return repository.legacyPathAsPath();
        }
        return null;
    }

    Path ensureManagedCheckout(
            String repositoryUrl,
            String branch,
            Path checkoutPath,
            boolean update,
            Consumer<String> logger) throws IOException, InterruptedException {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return null;
        }

        Path normalizedCheckoutPath = checkoutPath.toAbsolutePath().normalize();
        Files.createDirectories(normalizedCheckoutPath.getParent());

        if (!isGitCheckout(normalizedCheckoutPath)) {
            recreateCheckout(normalizedCheckoutPath, repositoryUrl, branch, logger);
            return normalizedCheckoutPath;
        }

        if (!update) {
            return normalizedCheckoutPath;
        }

        log(logger, "Updating managed topic repository checkout: " + normalizedCheckoutPath);
        runGit(normalizedCheckoutPath, logger, "remote", "set-url", "origin", repositoryUrl);
        runGit(normalizedCheckoutPath, logger, "fetch", "--prune", "origin");
        String normalizedBranch = normalizeBranch(branch);
        runGit(normalizedCheckoutPath, logger, "checkout", "-B", normalizedBranch, "origin/" + normalizedBranch);
        runGit(normalizedCheckoutPath, logger, "reset", "--hard", "origin/" + normalizedBranch);
        runGit(normalizedCheckoutPath, logger, "clean", "-fd");
        return normalizedCheckoutPath;
    }

    private Path managedRepositoryPath() throws IOException {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            throw new IOException("Jenkins is not running; managed topic repository path is unavailable.");
        }
        return jenkins.getRootDir().toPath().resolve(MANAGED_REPOSITORY_RELATIVE_PATH);
    }

    private void recreateCheckout(Path checkoutPath, String repositoryUrl, String branch, Consumer<String> logger)
            throws IOException, InterruptedException {
        deleteRecursively(checkoutPath);
        Files.createDirectories(checkoutPath.getParent());
        log(logger, "Cloning managed topic repository checkout: " + repositoryUrl);
        runGit(
                checkoutPath.getParent(),
                logger,
                "clone",
                "--branch",
                normalizeBranch(branch),
                "--single-branch",
                repositoryUrl,
                checkoutPath.toString());
    }

    private boolean isGitCheckout(Path checkoutPath) {
        if (!Files.isDirectory(checkoutPath)) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("git", "-C", checkoutPath.toString(), "rev-parse", "--is-inside-work-tree")
                    .redirectErrorStream(true)
                    .start();
            try (InputStream inputStream = process.getInputStream()) {
                String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
                return process.waitFor() == 0 && "true".equalsIgnoreCase(output);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private void runGit(Path workdir, Consumer<String> logger, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .directory(workdir.toFile())
                .redirectErrorStream(true)
                .start();

        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        int exitCode = process.waitFor();
        if (!output.isBlank()) {
            log(logger, output);
        }
        if (exitCode != 0) {
            throw new IOException("Git command failed (" + String.join(" ", command) + "): " + output);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(current -> {
                try {
                    Files.deleteIfExists(current);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private String normalizeBranch(String branch) {
        return branch == null || branch.isBlank() ? "main" : branch.trim();
    }

    private void log(Consumer<String> logger, String message) {
        if (logger != null && message != null && !message.isBlank()) {
            logger.accept(message);
        }
    }
}
