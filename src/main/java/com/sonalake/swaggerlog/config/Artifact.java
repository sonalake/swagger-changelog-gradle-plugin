package com.sonalake.swaggerlog.config;

import lombok.*;

/**
 * Identifies an artifact in nexus
 */
@ToString
@Builder
@Getter
@Setter
@RequiredArgsConstructor
public class Artifact {
  private final String groupId;
  private final String artifactId;
}
