package com.sonalake.swaggerlog.config;

/**
 * Defines config settings
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class Config {
  /**
   * the root nexus path, e.g. http://atlanta.sonalake.corp:8081/nexus
   */
  private String nexusHome;

  /**
   * The name of the repo in which to find the files (e.g. "releases")
   */
  private String repositoryId;

  /**
   * The artifact in jira from which the versions will be selected for diffing
   */
  private Artifact artifact;

  /**
   * Where and how should the files be written
   */
  private Target target;

  // if this is set, it will be considered the _last_ version in the history
  private String snapshotVersionFile;

  public String getNexusSearchPath() {
    return nexusHome + "/service/local/lucene/search";
  }

  public String getNexusDownloadPath(String repository) {
    return nexusHome + "/service/local/repositories/" + repository + "/content";
  }
}
