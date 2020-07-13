package com.sonalake.swaggerlog.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
class SearchResults {
  @Singular
  @JsonProperty("data")
  private List<VersionedArtifact> versions;

  public List<VersionStep> buildHistory() {

    // first, sort the versions by semver
    List<VersionedArtifact> sortedVersions = versions.stream()
      // remove any versions that aren't diffable
      .filter(VersionedArtifact::isDiffable)
      .sorted()
      .collect(Collectors.toList());

    // need at least two versions to have a history
    if (sortedVersions.size() < 2) {
      return Collections.emptyList();
    }

    // now build up a result from the remaining elements
    VersionedArtifact current = sortedVersions.remove(0);

    List<VersionStep> steps = new ArrayList<>();
    for (VersionedArtifact nextVersion : sortedVersions) {
      steps.add(
        VersionStep.builder()
          .from(current)
          .to(nextVersion)
          .build()
      );
      current = nextVersion;
    }
    return steps;
  }

  public void addSnapshot(VersionedArtifact snapshot) {
    versions.add(snapshot);
  }
}
