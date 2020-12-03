package com.sonalake.swaggerlog.nexus.formats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.VersionedArtifact;
import lombok.Data;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Jackson will map the raw JSON from a V2 query to this class
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class V2NexusResult implements NexusResult {
  @JsonProperty("repoDetails")
  private List<Repo> repoDetails;

  @JsonProperty("data")
  private List<Item> items;

  /**
   * %{@inheritDoc}
   */
  @Override
  public List<VersionedArtifact> buildVersions(Config config) {
    // we derive our download URLs from this part of the result
    Map<String, Repo> repos = emptyIfNull(repoDetails).stream().collect(Collectors.toMap(
      Repo::getRepositoryId,
      v -> v
    ));

    // now map the items to a versioned artifact, each will contain a download URL
    // based on the classifier (or the first json entry) in the underlying "artifact hits"
    return emptyIfNull(items)
      .stream()
      .map(i -> {

        String downloadFrom = buildDownloadUrl(config.getArtifact().getClassifier(), repos, i);
        return VersionedArtifact.builder()
          .artifact(i.getArtifactId())
          .version(i.getVersion())
          .group(i.getGroupId())
          .downloadFrom(downloadFrom)
          .build();
      }).collect(Collectors.toList());

  }


  private String buildDownloadUrl(String classifier, Map<String, Repo> repos, Item i) {
    return i.findLinkForClassifier(classifier).map(link -> {
        List<String> path = new ArrayList<>();
        path.add(repos.get(link.getRepositoryId()).getRepositoryURL());
        path.add("content");
        path.addAll(asList(i.getGroupId().split("\\.")));
        path.add(i.getArtifactId());
        path.add(i.getVersion());

        String classifierAppendage = isNotEmpty(classifier) ?
          "-" + classifier
          : "";

        path.add(format("%s-%s%s.json",
          i.getArtifactId(),
          i.getVersion(),
          classifierAppendage
        ));
        return join("/", path);
      }
    ).orElse(null);
  }


  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Repo {
    @JsonProperty("repositoryId")
    private String repositoryId;
    @JsonProperty("repositoryURL")
    private String repositoryURL;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Item {
    private String groupId;
    private String artifactId;
    private String version;

    @JsonProperty("artifactHits")
    private List<ArtifactHit> artifactHits;

    /**
     * Find the artifact link that matches this classifier. It will find the first json entry
     * if the parameter is null or empty.
     *
     * @param classifier the classifier of interest
     * @return the matching link - this will be enriched with the repo id
     */
    public Optional<ArtifactLink> findLinkForClassifier(String classifier) {

      // if there is a classifier, then use it, otherwise we use the first json
      Predicate<ArtifactLink> checker = isNotEmpty(classifier) ?
        link -> Objects.equals(classifier, link.getClassifier())
        : link -> "json".equals(link.getExtension());

      for (ArtifactHit hit : emptyIfNull(artifactHits)) {
        for (ArtifactLink link : hit.getArtifactLinks()) {
          if (checker.test(link)) {
            link.setRepositoryId(hit.getRepositoryId());
            return Optional.ofNullable(link);
          }
        }
      }

      return Optional.empty();
    }
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ArtifactHit {
    private String repositoryId;
    @JsonProperty("artifactLinks")
    private List<ArtifactLink> artifactLinks;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ArtifactLink {
    private String classifier;
    private String extension;
    private String repositoryId;
  }
}
