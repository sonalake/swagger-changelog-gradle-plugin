package com.sonalake.swaggerlog.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Config information to define where and how to write the diffs
 */
@ToString
@Builder
@Getter
@AllArgsConstructor
public class Target {
  public static final int DEFAULT_CHAPTER_LEVEL = 3;

  /**
   * The main index, and any diff files, will be written into this directory
   */
  private String targetdir;

  /**
   * At what chapter level should the diffs start, defaults to {@value #DEFAULT_CHAPTER_LEVEL}
   */
  @Builder.Default
  private int baseChapterLevel = DEFAULT_CHAPTER_LEVEL;
}
