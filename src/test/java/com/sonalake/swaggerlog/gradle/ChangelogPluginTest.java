package com.sonalake.swaggerlog.gradle;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.diff.LogGenerator;
import com.sonalake.swaggerlog.nexus.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableObject;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Collections;

import static com.sonalake.swaggerlog.gradle.ChangelogPlugin.EXTENSION_NAME;
import static com.sonalake.swaggerlog.gradle.ChangelogPlugin.TASK_NAME;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@Slf4j
public class ChangelogPluginTest {

  @InjectMocks
  private ChangelogPlugin plugin;

  @Mock
  private Scanner scanner;

  private Project project;

  @Before
  public void setup() {
    project = ProjectBuilder.builder()
      .withName("test")
      .build();
    log.debug("plugin {}", plugin);
    plugin.apply(project);

  }

  @Test
  public void testPluginConfig()  {
    // then the extension should be ok, but we'll configure it in the tests
    ChangelogExtension extension = (ChangelogExtension) project.getExtensions().findByName(EXTENSION_NAME);
    assertNotNull("No extension found", extension);

    // configure the extension here
    extension.setGroupId("com.sonalake");
    extension.setArtifactId("apidoc");
    extension.setNexusHome("http://server.nexus");
    extension.setTargetdir("/tmp/over/here");
    extension.setSnapshotVersionFile("/tmp/a/version.json");

    log.debug("Testing config: " + extension);

    // get the task
    ChangelogTask task = spy((ChangelogTask) project.getTasks().findByName(TASK_NAME));
    assertNotNull("No task found", task);

    // update the scanner to return nothing
    when(scanner.getHistory()).thenReturn(Collections.emptyList());

    MutableObject<LogGenerator> generatorStore = new MutableObject<>();

    // now set the scanner to return nothing, while at the same time
    // store the generator in an object we can check
    doAnswer(a -> {
      generatorStore.setValue(new LogGenerator(a.getArgument(0)) {
        @Override
        protected Scanner buildScanner() {
          return scanner;
        }
      });
      return generatorStore.getValue();
    }).when(task)
      .buildLogGenerator(any(Config.class));

    task.generateChangelog();

    // now check the generator was created properly
    LogGenerator generator = generatorStore.getValue();
    assertEquals("wrong groupId", "com.sonalake", generator.getConfig().getArtifact().getGroupId());
    assertEquals("wrong artifactId", "apidoc", generator.getConfig().getArtifact().getArtifactId());
    assertEquals("wrong nexus home", "http://server.nexus", generator.getConfig().getNexusHome());
    assertEquals("wrong target", "/tmp/over/here", generator.getConfig().getTarget().getTargetdir());

  }


  @Test
  public void testErrorHandling()  {
    // then the extension should be ok, but we'll configure it in the tests
    ChangelogExtension extension = (ChangelogExtension) project.getExtensions().findByName(EXTENSION_NAME);
    assertNotNull("No extension found", extension);

    // configure the extension here
    extension.setGroupId("com.sonalake");
    extension.setArtifactId("apidoc");
    extension.setNexusHome("http://server.nexus");
    extension.setTargetdir("/tmp/over/here");
    extension.setSnapshotVersionFile("/tmp/a/version.json");

    log.debug("Testing config: " + extension);

    // get the task
    ChangelogTask task = spy((ChangelogTask) project.getTasks().findByName(TASK_NAME));
    assertNotNull("No task found", task);

    // update the scanner to return nothing
    when(scanner.getHistory()).thenReturn(Collections.emptyList());

    MutableObject<LogGenerator> generatorStore = new MutableObject<>();

    // now set the scanner to return nothing, while at the same time
    // store the generator in an object we can check
    doAnswer(a -> {
      generatorStore.setValue(new LogGenerator(a.getArgument(0)) {
        @Override
        protected Scanner buildScanner() {
          return scanner;
        }

        @Override
        public void generateChangeLog() throws IOException {
          throw new IOException("Oh noes!");
        }
      });
      return generatorStore.getValue();
    }).when(task)
      .buildLogGenerator(any(Config.class));

    // check we got the error we expected
    IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, task::generateChangelog);
    assertTrue("Wrong message", expected.getMessage().startsWith("Failed to generate log from config") );
    assertEquals(IOException.class, expected.getCause().getClass());
    assertEquals("Oh noes!", expected.getCause().getMessage());


  }
}
