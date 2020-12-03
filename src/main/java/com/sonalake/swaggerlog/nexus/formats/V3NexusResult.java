package com.sonalake.swaggerlog.nexus.formats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.VersionedArtifact;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class V3NexusResult implements NexusResult {

  @JsonProperty("items")
  private List<Item> items;


  /**
   * ${@inheritDoc}
   */
  @Override
  public List<VersionedArtifact> buildVersions(Config config) {
    // if there is a classifier, then use it, otherwise we use the first json
    String filenameSuffix = isNotEmpty(config.getArtifact().getClassifier())
      ? String.format("-%s.json", config.getArtifact().getClassifier())
      : ".json";

    // this is _much_ easier than V2, because this result contains the asset download URL
    return items.stream()
      .map(i -> {
        String downloadFrom = i.assets.stream().filter(a -> a.getPath().endsWith(filenameSuffix))
          .findFirst()
          .map(Asset::getDownloadUrl)
          .orElse(null);
        return VersionedArtifact.builder()
          .group(i.getGroup())
          .version(i.getVersion())
          .artifact(i.getName())
          .downloadFrom(downloadFrom)
          .build();
      })
      .collect(Collectors.toList());
  }

  @Override
  public void validate() throws AssertionError {
    if (isEmpty(this.items)) {
      throw new AssertionError("There are no items details in the response");
    }
  }


  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Item {
    private String group;
    private String name;
    private String version;
    @JsonProperty("assets")
    private List<Asset> assets;


  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Asset {
    private String path;
    private String downloadUrl;

  }
}
