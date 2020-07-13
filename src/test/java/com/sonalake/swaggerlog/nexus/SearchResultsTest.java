package com.sonalake.swaggerlog.nexus;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class SearchResultsTest {

  @Test
  public void testWithNoHistory() {
    assertTrue(
      "Should not error if there are no results",
      SearchResults.builder().build().buildHistory().isEmpty()
    );
  }

  @Test
  public void testWithNotEnoughHistory() {
    assertTrue(
      "Should not error if there arent enough results",
      SearchResults.builder()
        .version(version("1.2.3"))
        .build()
        .buildHistory()
        .isEmpty()
    );
  }

  @Test
  public void testSortedSimple() {
    List<VersionedArtifact> provided = asList(version("1.4"), version("1.2.3"));
    List<VersionStep> expected = asList(versionStep("1.2.3", "1.4"));
    testSorting(provided, expected);
  }

  @Test
  public void testSortedBranching() {
    List<VersionedArtifact> provided = asList(
      version("1.4"),
      version("1.2.3"),
      version("1.2"),
      version("1.2.0.1"));

    List<VersionStep> expected = asList(
      versionStep("1.2", "1.2.0.1"),
      versionStep("1.2.0.1", "1.2.3"),
      versionStep("1.2.3", "1.4")
    );
    testSorting(provided, expected);
  }

  @Test
  public void testSortedStringsShouldBeIgnored() {
    List<VersionedArtifact> provided = asList(
      version("1.4"),
      version("1.2.3"),
      version("1.2.3-RC2"),
      version("1.2.3-RC1"),
      version("1.2"),
      version("1.2.0.1"));

    List<VersionStep> expected = asList(
      versionStep("1.2", "1.2.0.1"),
      versionStep("1.2.0.1", "1.2.3"),
      versionStep("1.2.3", "1.4")
    );
    testSorting(provided, expected);
  }

  @Test
  public void testSortedSnapshot() {
    List<VersionedArtifact> provided = asList(
      version("1.4"),
      version("1.2.3"),
      snapshotVersion("/overHere"),
      version("1.2.3-RC2"),
      version("1.2.3-RC1"),
      version("1.2"),
      version("1.2.0.1"));

    List<VersionStep> expected = asList(
      versionStep("1.2", "1.2.0.1"),
      versionStep("1.2.0.1", "1.2.3"),
      versionStep("1.2.3", "1.4"),
      versionStep("1.4", null)
    );
    testSorting(provided, expected);
  }

  private void testSorting(List<VersionedArtifact> provided, List<VersionStep> expected) {
    log.debug("Provided: {}", provided);
    log.debug("Expected: {}", expected);

    assertEquals(
      "Should match ok",
      expected,
      SearchResults.builder()
        .versions(provided)
        .build()
        .buildHistory()
    );
  }

  private VersionedArtifact version(String version) {
    VersionedArtifact artifact = VersionedArtifact.builder()
      .group("com.sonalake")
      .artifact("test")
      .version(version)
      .build();
    log.debug("Testing version: " + artifact);
    return artifact;
  }

  private VersionedArtifact snapshotVersion(String path) {
    return version(null);

  }

  private VersionStep versionStep(String from, String to) {
    VersionStep step = VersionStep.builder()
      .from(version(from))
      .to(version(to))
      .build();
    log.debug("Testing step: " + step);
    return step;
  }
}
