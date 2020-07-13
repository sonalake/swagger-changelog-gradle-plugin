package com.sonalake.swaggerlog.gradle;

import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.diff.LogGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;

import static java.util.Optional.ofNullable;


/**
 * Defines the changelog task for the gradle plugin
 */
@Slf4j
@Getter
public class ChangelogTask extends DefaultTask {

  @Input
  private final Project project;

  @Inject
  public ChangelogTask(Project project) {
    this.project = project;
  }

  /**
   * Generates the actual changelog
   */
  @TaskAction
  public void generateChangelog() {
    ChangelogExtension taskConfig =
      ofNullable(getProject().getExtensions().findByType(ChangelogExtension.class))
        .orElseThrow(() -> new IllegalArgumentException("No config specified for task"));

    log.debug("Using config: {}", taskConfig);
    try {
      buildLogGenerator(taskConfig.buildConfig()).generateChangeLog();
    } catch (IOException e) {
      log.error("Failed to generate log from config {}", taskConfig, e);
      throw new IllegalArgumentException("Failed to generate log from config", e);
    }
  }

  protected LogGenerator buildLogGenerator(Config config) {
    return LogGenerator.builder().config(config).build();
  }
}
