package com.sonalake.swaggerlog.nexus;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.sonalake.swaggerlog.config.Artifact;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.config.Target;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Unirest.class)
public class ScannerTest {

  public static final String GROUP_ID = "com.sonalake";
  public static final String ARTIFACT_ID = "order-state-service";
  public static final String CLASSIFIER_ID = "openapi";

  @Test
  public void testV2() throws Exception {

    GetRequest request = givenNexusResponse(fromResource("results.v2.json"));

    // given this config
    List<VersionStep> history = Scanner.builder()
      .config(Config.builder()
        .artifact(Artifact.builder()
          .groupId(GROUP_ID)
          .artifactId(ARTIFACT_ID)
          .build())
        .nexusHome("http://nexus/there")
        .target(Target.builder().targetdir("/tmp/here").build())
        .snapshotVersionFile("snapshot-file")
        .build())
      .build()
      .getHistory();

    // then the query was valid
    verify(request).queryString(eq("g"), eq(GROUP_ID));
    verify(request).queryString(eq("a"), eq(ARTIFACT_ID));

    // and the results parse ok
    assertEquals(2, history.size());
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID)
          .version("1.0.1")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.1/order-state-service-1.0.1.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2.json")
          .build())
        .build(),
      history.get(0)
    );
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).path("snapshot-file").build())
        .build(),
      history.get(1)
    );
  }


  @Test
  public void testWithClassifierV2() throws Exception {
    GetRequest request = givenNexusResponse(fromResource("results.v2.json"));

    // given this config
    List<VersionStep> history = Scanner.builder()
      .config(Config.builder()
        .artifact(Artifact.builder()
          .groupId(GROUP_ID)
          .artifactId(ARTIFACT_ID)
          .classifier(CLASSIFIER_ID)
          .build())
        .nexusHome("http://nexus/there")
        .repositoryId("releases")
        .target(Target.builder().targetdir("/tmp/here").build())
        .snapshotVersionFile("snapshot-file")
        .build())
      .build()
      .getHistory();

    // then the query was valid
    verify(request).queryString(eq("g"), eq(GROUP_ID));
    verify(request).queryString(eq("a"), eq(ARTIFACT_ID));
    verify(request).queryString(eq("c"), eq(CLASSIFIER_ID));

    // and the results parse ok
    assertEquals(2, history.size());
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.1")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.1/order-state-service-1.0.1-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .build(),
      history.get(0)
    );
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/service/local/repositories/releases/content/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).path("snapshot-file").build())
        .build(),
      history.get(1)
    );
  }

  private String fromResource(String name) throws IOException {
    return IOUtils.toString(currentThread().getContextClassLoader().getResource(name), StandardCharsets.UTF_8);
  }

  @Test
  public void testBadJson() throws Exception {
    // given a bad response
    givenNexusResponse("this be no json");

    // given this config
    Config config = Config.builder()
      .artifact(Artifact.builder()
        .groupId(GROUP_ID)
        .artifactId(ARTIFACT_ID)
        .build())
      .nexusHome("http://nexus/there")
      .target(Target.builder().targetdir("/tmp/here").build())
      .snapshotVersionFile("snapshot-file")
      .build();

    // when we look for date we get the expected error
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Scanner.builder()
      .config(config)
      .build()
      .getHistory());

    assertEquals(format("Failed to parse results from nexus: %s", config), error.getMessage());
  }




  private GetRequest givenNexusResponse(String responseData) throws Exception {
    mockStatic(Unirest.class);
    HttpResponse response = mock(HttpResponse.class);
    when(response.getBody()).thenReturn(responseData);
    GetRequest request = spy(new GetRequest(HttpMethod.GET, "here"));
    doReturn(response).when(request).asString();
    when(Unirest.class, "get", anyString()).thenReturn(request);
    return request;
  }

  private String artifact(String groupId, String artifactId, String version) {

    return format(
      "{\"groupId\":\"%s\", \"artifactId\":\"%s\", \"version\":\"%s\"}",
      groupId, artifactId, version

    );
  }


  @Test
  public void testV3() throws Exception {

    GetRequest request = givenNexusResponse(fromResource("results.v3.json"));

    // given this config
    List<VersionStep> history = Scanner.builder()
      .config(Config.builder()
        .isVersion3(true)
        .artifact(Artifact.builder()
          .groupId(GROUP_ID)
          .artifactId(ARTIFACT_ID)
          .build())
        .nexusHome("http://nexus/there")
        .target(Target.builder().targetdir("/tmp/here").build())
        .snapshotVersionFile("snapshot-file")
        .build())
      .build()
      .getHistory();

    // then the query was valid
    verify(request).queryString(eq("group"), eq(GROUP_ID));
    verify(request).queryString(eq("maven.artifactId"), eq(ARTIFACT_ID));

    // and the results parse ok
    assertEquals(2, history.size());
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID)
          .version("1.0.1")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.1/order-state-service-1.0.1-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .build(),
      history.get(0)
    );
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).path("snapshot-file").build())
        .build(),
      history.get(1)
    );
  }

  @Test
  public void testV3WithClassifier() throws Exception {

    GetRequest request = givenNexusResponse(fromResource("results.v3.json"));

    // given this config
    List<VersionStep> history = Scanner.builder()
      .config(Config.builder()
        .isVersion3(true)
        .artifact(Artifact.builder()
          .groupId(GROUP_ID)
          .artifactId(ARTIFACT_ID)
          .classifier(CLASSIFIER_ID)
          .build())
        .nexusHome("http://nexus/there")
        .target(Target.builder().targetdir("/tmp/here").build())
        .snapshotVersionFile("snapshot-file")
        .build())
      .build()
      .getHistory();

    // then the query was valid
    verify(request).queryString(eq("group"), eq(GROUP_ID));
    verify(request).queryString(eq("maven.artifactId"), eq(ARTIFACT_ID));

    // and the results parse ok
    assertEquals(2, history.size());
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID)
          .version("1.0.1")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.1/order-state-service-1.0.1-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .build(),
      history.get(0)
    );
    assertEquals(
      VersionStep.builder()
        .from(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).version("1.0.2")
          .downloadFrom("http://nexus/repository/sample-repo/com/sonalake/order-state-service/1.0.2/order-state-service-1.0.2-openapi.json")
          .build())
        .to(VersionedArtifact.builder().group(GROUP_ID).artifact(ARTIFACT_ID).path("snapshot-file").build())
        .build(),
      history.get(1)
    );
  }

}
