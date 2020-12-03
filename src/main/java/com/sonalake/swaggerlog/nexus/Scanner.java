package com.sonalake.swaggerlog.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.formats.VersionFinder;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Queries nexus for the swagger docs for a given artifact, sorts them, and then
 * gets an ordered history for processing.
 */
@Builder
@RequiredArgsConstructor
@Slf4j
public class Scanner {
  private final Config config;

  /**
   * Given the config, get the ordered history of diffable swaggers.
   *
   * @return the list of versions
   */
  public List<VersionStep> getHistory() {
    log.debug("Generating history now");
    List<VersionedArtifact> artifacts = new VersionFinder().findVersions(config);
    appendSnapshotToHistory(artifacts);
    SearchResults versions = SearchResults.builder().versions(artifacts).build();

    return versions.buildHistory();
  }

  /**
   * If there is a snapshot, then add it to the search results
   *
   * @param artifacts the discovered versions
   */
  private void appendSnapshotToHistory( List<VersionedArtifact> artifacts) {
    ofNullable(config.getSnapshotVersionFile()).ifPresent(path -> {
      VersionedArtifact version = VersionedArtifact.builder()
        .group(config.getArtifact().getGroupId())
        .artifact(config.getArtifact().getArtifactId())
        .path(path)
        .build();
      log.debug("Adding local snapshot file to version history");
      artifacts.add(version);
    });
  }

  /**
   * Parse the search restuls, and translate them into the SearchResults type
   *
   * @param search
   * @return
   */
  private SearchResults parseSearchResults(HttpResponse<String> search) {
    log.debug(search.getBody());

    ObjectMapper mapper = new ObjectMapper();
    final SearchResults versions;
    try {
      versions = mapper.readValue(search.getBody(), SearchResults.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse results from nexus: " + config, e);
    }
    return versions;
  }

  /**
   * Build the version UTI for downloading the swagger, for the given version
   *
   * @param version The versioned artifact model
   * @return the URL to download this artifact
   */
  public String getVersionUri(VersionedArtifact version) {
    if (version.isSnapshot()) {
      return version.getPath();
    }

    return version.getDownloadFrom();
  }

}
