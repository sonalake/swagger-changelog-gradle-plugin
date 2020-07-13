package com.sonalake.swaggerlog.gradle;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
/**
 * Defines the changelog gradle plugin
 */
@Slf4j
public class ChangelogPlugin implements Plugin<Project> {

  static final String EXTENSION_NAME = "swaggerChangeLog";
  static final String TASK_NAME = "generateChangeLog";

  /**
   * Applies the {@link ChangelogExtension} and {@link ChangelogTask} to the project.
   *
   * The task will be named {@value #TASK_NAME} and will look for configs in {@value #EXTENSION_NAME}
   * @param project the project for which this plugin is being defined
   */
  @Override
  public void apply(@Nonnull Project project) {
    log.debug("Preparing");

    project.getExtensions().create(EXTENSION_NAME, ChangelogExtension.class);
    project.getTasks().register(TASK_NAME, ChangelogTask.class, project);

    log.debug("Registered");
  }
}
