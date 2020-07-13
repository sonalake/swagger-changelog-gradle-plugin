package com.sonalake.swaggerlog.nexus;

import lombok.*;

@RequiredArgsConstructor
@Builder
@ToString
@Getter
@EqualsAndHashCode
public class VersionStep {
  private final VersionedArtifact from;
  private final VersionedArtifact to;
}
