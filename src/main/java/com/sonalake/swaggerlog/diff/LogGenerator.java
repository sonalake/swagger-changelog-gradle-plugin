package com.sonalake.swaggerlog.diff;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.Scanner;
import com.sonalake.swaggerlog.nexus.VersionStep;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static com.deepoove.swagger.diff.SwaggerDiff.compareV2;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Given the config, collect and sort the swaggers from nexus, and produce both the diffs and their index file.
 */
@Builder
@RequiredArgsConstructor
@Slf4j
public class LogGenerator {
  protected static final String CHANGE_LOG_ADOC = "change-log.adoc";
  @Getter
  private final Config config;

  /**
   * Given the config, loop through the git history and produce a changelog
   *
   * @throws IOException When the output file can't be created
   */
  public void generateChangeLog() throws IOException {

    log.debug("Generating diff for {}", config);
    Scanner scanner = buildScanner();
    Path index = Paths.get(config.getTarget().getTargetdir(), CHANGE_LOG_ADOC);
    Files.createDirectories(index.getParent());
    Files.deleteIfExists(index);
    log.debug("Writing to index {}", index);

    scanner.getHistory().stream()
      .map(step -> buildSwaggerDiff(scanner, step))
      .filter(this::skipStepsWithNoChanges)
      .forEach(diff -> appendDiffToChangelog(index, diff));

  }

  /**
   * Build a difference-model between the two versions in the give step
   *
   * @param scanner
   * @param step
   * @return
   */
  private SwaggerDiff buildSwaggerDiff(Scanner scanner, VersionStep step) {
    log.debug("Examining step {}", step);
    String fromUri = scanner.getVersionUri(step.getFrom());
    String toUri = scanner.getVersionUri(step.getTo());

    log.debug("Diffing urls {} -> {}", fromUri, toUri);

    return compareV2(fromUri, toUri);
  }

  /**
   * We don't bother producing diffs where nothing has changed between versioons
   *
   * @param diff
   * @return
   */
  private boolean skipStepsWithNoChanges(SwaggerDiff diff) {
    boolean hasNoChanges =
      diff.getChangedEndpoints().isEmpty()
        && diff.getNewEndpoints().isEmpty()
        && diff.getMissingEndpoints().isEmpty();
    if (hasNoChanges) {
      log.debug("No diff for {} -> {}", diff.getOldVersion(), diff.getNewVersion());
    }
    return !hasNoChanges;
  }

  /**
   * Writes the changelog for the diff to the same directory as the path, and also appends a reference in the
   * index file so the diff's change will be included
   *
   * @param index
   * @param diff
   */
  private void appendDiffToChangelog(Path index, SwaggerDiff diff) {
    String filename = "change-log-" + diff.getOldVersion() + "-" + diff.getNewVersion() + ".adoc";
    Path target = Paths.get(config.getTarget().getTargetdir(), filename);
    log.debug("Writing diff for {} -> {} to {}", diff.getOldVersion(), diff.getNewVersion(), target);
    try {
      writeGeneratedFile(diff, target);
      appendGeneratedFileToIndex(index, target);
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't save markdown file: " + filename, e);
    }
  }

  /**
   * Write a file for this diff to the target directory
   *
   * @param diff
   * @param target
   * @throws IOException
   */
  private void writeGeneratedFile(SwaggerDiff diff, Path target) throws IOException {
    Files.copy(
      IOUtils.toInputStream(new Renderer().render(config.getTarget().getBaseChapterLevel(), diff), StandardCharsets.UTF_8),
      target,
      StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Add ta reference to the new diff file to the end of the index file
   *
   * @param index
   * @param target
   * @throws IOException
   */
  private void appendGeneratedFileToIndex(Path index, Path target) throws IOException {
    Files.write(
      index,
      String.format("include::%s[]%n%n", target.getFileName().toString()).getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    );
  }

  /**
   * Build a scanner for the given config
   *
   * @return The nexus scanner
   */
  protected Scanner buildScanner() {
    return Scanner.builder()
      .config(config)
      .build();
  }

}
