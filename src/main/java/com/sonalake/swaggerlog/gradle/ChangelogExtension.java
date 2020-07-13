package com.sonalake.swaggerlog.gradle;

import com.sonalake.swaggerlog.config.Artifact;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.config.Target;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static java.util.Optional.ofNullable;

/**
 * Defines the changelog extension for the gradle plugin
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ChangelogExtension {

  /**
   * Identifies the artifact's group in nexus
   */
  private String groupId;

  /**
   * Identifies the artifact, within the group, in nexus
   */
  private String artifactId;

  /**
   * The base URL for the nexus repository
   */
  private String nexusHome;

  /**
   * The repository id - defaults to releases
   */
  private String repositoryId;

  /**
   * The directory into which the diff files and index will be written
   */
  private String targetdir;

  /**
   * The base chapter level for the diff files (e.g. start at chapter 3)
   */
  private Integer baseChapterLevel;

  /**
   * Path to the optional shapshot version file. This is the current, not as-yet released swagger.
   *
   * If this is set, it will be considered the _last_ version in the history
   */
  private String snapshotVersionFile;

  public String getRepositoryId() {
    return repositoryId == null ? "releases" : repositoryId;
  }

  Config buildConfig() {
    return Config.builder()
      .nexusHome(getNexusHome())
      .repositoryId(getRepositoryId())
      .target(buildTarget())
      .snapshotVersionFile(getSnapshotVersionFile())
      .artifact(buildArtifact()
      ).build();
  }

  private Artifact buildArtifact() {
    return Artifact.builder()
      .groupId(getGroupId())
      .artifactId(getArtifactId())
      .build();
  }

  private Target buildTarget() {
    return Target
      .builder()
      .targetdir(getTargetdir())
      .baseChapterLevel(ofNullable(baseChapterLevel).orElse(Target.DEFAULT_CHAPTER_LEVEL))
      .build();
  }
}
