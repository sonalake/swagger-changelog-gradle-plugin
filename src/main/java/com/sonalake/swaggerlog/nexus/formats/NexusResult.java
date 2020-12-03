package com.sonalake.swaggerlog.nexus.formats;

import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.VersionedArtifact;

import java.util.List;

/**
 * A generic interface for the nexus search results
 */
public interface NexusResult {
  /**
   * Given a config, map the raw json results to a list of versioned artifacts
   *
   * @param config the configuration
   * @return the versioned artifacts
   */
  List<VersionedArtifact> buildVersions(Config config);

  /**
   * Check if there's enough data in here to do anything
   */
  void validate() throws AssertionError;
}
