package com.sonalake.swaggerlog.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import se.sawano.java.text.AlphanumericComparator;

import javax.validation.constraints.NotNull;
import java.util.Locale;

import static org.apache.commons.lang3.math.NumberUtils.isDigits;

/**
 * An version of the artifact that can be compared to another for swagger diffs
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "comparator")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(exclude = "comparator")
@Getter
public class VersionedArtifact implements Comparable<VersionedArtifact> {
  private final AlphanumericComparator comparator = new AlphanumericComparator(Locale.ENGLISH);

  @JsonProperty("groupId")
  private String group;

  @JsonProperty("artifactId")
  private String artifact;

  @JsonProperty("version")
  private String version;

  @JsonProperty("classifier")
  private String classifier;

  private String path;

  /**
   * Sorted by semver version order
   *
   * @param o the other entity
   * @return the comparison identity
   */
  @Override
  public int compareTo(@NotNull VersionedArtifact o) {
    // snapshots come last
    if (isSnapshot()) {
      return 1;
    } else if (o.isSnapshot()) {
      return -1;
    } else {
      return comparator.compare(getVersion(), o.getVersion());
    }
  }

  boolean isSnapshot() {
    return null == version;
  }

  /**
   * A version is diffable if it's a snapshot, or if it's version only contains digit values
   *
   * @return
   */
  boolean isDiffable() {
    return isSnapshot() || isDigits(getVersion().replace(".", ""));
  }
}
