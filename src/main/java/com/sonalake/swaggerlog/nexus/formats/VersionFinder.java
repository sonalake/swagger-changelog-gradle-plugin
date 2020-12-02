package com.sonalake.swaggerlog.nexus.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.sonalake.swaggerlog.config.Config;
import com.sonalake.swaggerlog.nexus.VersionedArtifact;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class VersionFinder {

  public List<VersionedArtifact> findVersions(Config config) {
    try {
      if (config.isVersion3()) {
        return findNexus3Versions(config);
      } else {
        return findNexus2Versions(config);
      }

    } catch (UnirestException | IOException e) {
      throw new IllegalArgumentException("Failed to parse results from nexus: " + config, e);
    }

  }


  private List<VersionedArtifact> findNexus3Versions(Config config) throws UnirestException, IOException {
    HttpRequest builder = Unirest.get(config.getNexusHome() + "/service/local/lucene/search")
      .header("accept", "application/json")
      .queryString("repository", config.getRepositoryId())
      .queryString("group", config.getArtifact().getGroupId())
      .queryString("maven.artifactId", config.getArtifact().getArtifactId())
      .queryString("maven.extension", "json");

    if (isNotBlank(config.getArtifact().getClassifier())) {
      builder.queryString("maven.classifier", config.getArtifact().getClassifier());
    }

    return parseNexusResults(builder.asString(), V3NexusResult.class, config);
  }

  private List<VersionedArtifact> findNexus2Versions(Config config) throws UnirestException, IOException {
    HttpRequest builder = Unirest.get(config.getNexusHome() + "/service/rest/v1/search")
      .header("accept", "application/json")
      .queryString("g", config.getArtifact().getGroupId())
      .queryString("a", config.getArtifact().getArtifactId())
      .queryString("p", "json")
      .queryString("repositoryId", config.getRepositoryId());

    if (isNotBlank(config.getArtifact().getClassifier())) {
      builder.queryString("c", config.getArtifact().getClassifier());
    }


    return parseNexusResults(builder.asString(), V2NexusResult.class, config);
  }


  private <T extends NexusResult> List<VersionedArtifact> parseNexusResults(HttpResponse<String> result, Class<T> format, Config config) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    T resultObject = mapper.readValue(result.getBody(), format);
    return resultObject.buildVersions(config);
  }
}
