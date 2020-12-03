package com.sonalake.swaggerlog.diff;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.Endpoint;
import com.sonalake.swaggerlog.config.Artifact;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.config.Target;
import com.sonalake.swaggerlog.nexus.Scanner;
import com.sonalake.swaggerlog.nexus.VersionStep;
import com.sonalake.swaggerlog.nexus.VersionedArtifact;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SwaggerDiff.class)
@Slf4j
public class LogGeneratorTest {
  @Test
  public void test() throws Exception {
    mockStatic(SwaggerDiff.class);
    Config config = Config.builder()
      .artifact(Artifact.builder()
        .groupId("groupy")
        .artifactId("covenant")
        .build())
      .nexusHome("http://nexus/there")
      .target(Target.builder().targetdir(Files.createTempDirectory("LogGeneratorTest").toString()).build())
      .build();

    Scanner scanner = spy(Scanner.builder()
      .config(config)
      .build());

    // given this history
    List<VersionStep> history = asList(
      VersionStep.builder().from(artifact("1.0")).to(artifact("1.1")).build(),
      VersionStep.builder().from(artifact("1.1")).to(artifact("1.2")).build(),
      VersionStep.builder().from(artifact("1.2")).to(artifact("1.3")).build(),
      VersionStep.builder().from(artifact("1.3")).to(artifact("1.4")).build(),
      VersionStep.builder().from(artifact("1.4")).to(artifact(null)).build()
    );

    // given these diffs

    when(SwaggerDiff.class, "compareV2", anyString(), any()).then(a -> {
      String from = a.getArgument(0);
      from = from.split("/")[from.split("/").length - 2];

      // the target version could be null, if this is a snapshot
      String to = a.getArgument(1);
      if (null == to) {
        to = "1.5-SNAPSHOT";
      } else {
        to = to.split("/")[to.split("/").length - 2];
      }

      SwaggerDiff diff = mock(SwaggerDiff.class);

      when(diff.getOldVersion()).thenReturn(from);
      when(diff.getNewVersion()).thenReturn(to);

      switch (from + " -> " + to) {
        case "1.0 -> 1.1":
          when(diff.getNewEndpoints()).thenReturn(asList(get("/a/b/c", "Some details")));
          break;
        case "1.1 -> 1.2":
          when(diff.getMissingEndpoints()).thenReturn(asList(get("/d/e/f", "Some other details")));
          break;
        case "1.3 -> 1.4":
          when(diff.getMissingEndpoints()).thenReturn(asList(get("/elvis", "Has left the building")));
          break;
        case "1.4 -> 1.5-SNAPSHOT":
          when(diff.getMissingEndpoints()).thenReturn(asList(get("/whereDoWeGo", "Where do we go now")));
          break;
      }

      return diff;
    });

    doReturn(history).when(scanner).getHistory();

    LogGenerator generator = spy(LogGenerator.builder().config(config).build());
    doReturn(scanner).when(generator).buildScanner();

    generator.generateChangeLog();

    log.debug(config.getTarget().getTargetdir());

    List<String> files = Files.readAllLines(Paths.get(config.getTarget().getTargetdir(), LogGenerator.CHANGE_LOG_ADOC));
    assertEquals(
      // every line is followed by a blank line
      asList(
        "include::change-log-1.0-1.1.adoc[]", "",
        "include::change-log-1.1-1.2.adoc[]", "",
        "include::change-log-1.3-1.4.adoc[]", "",
        "include::change-log-1.4-1.5-SNAPSHOT.adoc[]", ""
      ),
      files
    );

  }

  @Test
  public void testWithNoVersionData() throws Exception {
    mockStatic(SwaggerDiff.class);
    Config config = Config.builder()
      .artifact(Artifact.builder()
        .groupId("groupy")
        .artifactId("covenant")
        .build())
      .nexusHome("http://nexus/there")
      .target(Target.builder().targetdir(Files.createTempDirectory("LogGeneratorTest").toString()).build())
      .build();

    Scanner scanner = spy(Scanner.builder()
      .config(config)
      .build());

    // given no history
    doReturn(emptyList()).when(scanner).getHistory();

    LogGenerator generator = spy(LogGenerator.builder().config(config).build());
    doReturn(scanner).when(generator).buildScanner();

    generator.generateChangeLog();

    log.debug(config.getTarget().getTargetdir());

    List<String> files = Files.readAllLines(Paths.get(config.getTarget().getTargetdir(), LogGenerator.CHANGE_LOG_ADOC));
    assertEquals(
      asList(
        "No change history available"
      ),
      files
    );
  }

  private Endpoint get(String path, String summary) {
    Endpoint end = new Endpoint();
    end.setMethod(HttpMethod.GET);
    end.setPathUrl(path);
    end.setPath(new Path().get(new Operation().summary(summary)));
    return end;
  }

  private VersionedArtifact artifact(String version) {
    return VersionedArtifact.builder()
      .group("groupA")
      .artifact("thisIsId")
      .version(version)
      .downloadFrom(String.format("http://nexus/there/groupA/thisIsId/%s/thisIsId-%s.json", version, version))
      .build();
  }
}
