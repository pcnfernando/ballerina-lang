package io.ballerina.projects.internal.environment;

import io.ballerina.projects.ProjectException;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.PackageRepository;
import io.ballerina.projects.internal.repositories.FileSystemRepository;
import io.ballerina.projects.internal.repositories.RemotePackageRepository;
import io.ballerina.projects.util.ProjectConstants;
import org.ballerinalang.toml.exceptions.SettingsTomlException;
import org.ballerinalang.toml.model.Settings;
import org.ballerinalang.toml.parser.SettingsProcessor;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.ballerina.runtime.api.constants.RuntimeConstants.USER_HOME;

/**
 * Represents the Ballerina user home and responsible for resolving cached packages.
 *
 * @since 2.0.0
 */
public final class BallerinaUserHome {

    private final Path ballerinaUserHomeDirPath;
    private final RemotePackageRepository remotePackageRepository;
    private final Map<String, PackageRepository> customRepositories;

    private BallerinaUserHome(Environment environment, Path ballerinaUserHomeDirPath) {
        this.ballerinaUserHomeDirPath = ballerinaUserHomeDirPath;
        Path remotePackageRepositoryPath = ballerinaUserHomeDirPath.resolve(ProjectConstants.REPOSITORIES_DIR)
                .resolve(ProjectConstants.CENTRAL_REPOSITORY_CACHE_NAME);
        try {
            Files.createDirectories(remotePackageRepositoryPath);
        } catch (IOException exception) {
            throw new ProjectException("unable to create the file system cache of Ballerina Central repository");
        }

        this.remotePackageRepository = RemotePackageRepository
                .from(environment, remotePackageRepositoryPath, readSettings());
        this.customRepositories = Map.of(ProjectConstants.LOCAL_REPOSITORY_NAME, createLocalRepository(environment));
    }

    public static BallerinaUserHome from(Environment environment, Path ballerinaUserHomeDirPath) {
        validateBallerinaUserHomeDir(ballerinaUserHomeDirPath);
        return new BallerinaUserHome(environment, ballerinaUserHomeDirPath);
    }

    public static BallerinaUserHome from(Environment environment) {
        String userHomeDir = System.getProperty(USER_HOME);
        if (userHomeDir == null || userHomeDir.isEmpty()) {
            throw new ProjectException("unable to get user home directory");
        }

        Path homeRepoPath = Paths.get(userHomeDir, ProjectConstants.HOME_REPO_DEFAULT_DIRNAME);
        return from(environment, homeRepoPath);
    }

    public RemotePackageRepository remotePackageRepository() {
        return this.remotePackageRepository;
    }

    public Map<String, PackageRepository> customRepositories() {
        return this.customRepositories;
    }

    /**
     * Read Settings.toml to populate the configurations.
     *
     * @return {@link Settings} settings object
     */
    private Settings readSettings() {
        Path settingsFilePath = this.ballerinaUserHomeDirPath.resolve(ProjectConstants.SETTINGS_FILE_NAME);
        if (Files.notExists(settingsFilePath)) {
            try {
                Files.createFile(settingsFilePath);
            } catch (IOException e) {
                throw new ProjectException(
                        ProjectConstants.SETTINGS_FILE_NAME + " does not exists in '" + ballerinaUserHomeDirPath
                                + "', File creation also failed");
            }
        }
        try {
            return SettingsProcessor.parseTomlContentFromFile(settingsFilePath);
        } catch (IOException | SettingsTomlException e) {
            // Ignore 'Settings.toml' reading and parsing errors
            return new Settings();
        }
    }

    private static void validateBallerinaUserHomeDir(Path ballerinaUserHomeDirPath) {
        // If directory does not exists, create it
        if (Files.notExists(ballerinaUserHomeDirPath) || !Files.isDirectory(ballerinaUserHomeDirPath)) {
            try {
                Files.createDirectories(ballerinaUserHomeDirPath);
            } catch (IOException e) {
                throw new ProjectException(
                        "Ballerina user home directory does not exists in '" + ballerinaUserHomeDirPath
                                + "', Directory creation also failed");
            }
        }
    }

    private FileSystemRepository createLocalRepository(Environment environment) {
        Path repositoryPath = ballerinaUserHomeDirPath.resolve(ProjectConstants.REPOSITORIES_DIR)
                .resolve(ProjectConstants.LOCAL_REPOSITORY_NAME);
        try {
            Files.createDirectories(repositoryPath);
        } catch (IOException exception) {
            throw new ProjectException("unable to create repository: " + ProjectConstants.LOCAL_REPOSITORY_NAME);
        }
        String ballerinaShortVersion = RepoUtils.getBallerinaShortVersion();
        return new FileSystemRepository(environment, repositoryPath, ballerinaShortVersion);
    }
}
