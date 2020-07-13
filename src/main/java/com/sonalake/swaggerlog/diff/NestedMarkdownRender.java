package com.sonalake.swaggerlog.diff;

import com.deepoove.swagger.diff.output.MarkdownRender;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * Given the generated diffs, produce a markdown string that can be rendered in asciidoc
 */
@RequiredArgsConstructor
@Builder
class NestedMarkdownRender extends MarkdownRender {
  /**
   * What is the base chapter level - this is to allow the rendered docs to be placed into
   * any doc.
   */
  private final int baseChapterLevel;

  /**
   * Render the markdown (the "html" name comes from the class being extended)
   *
   * @param oldVersion       The name of the old version
   * @param newVersion       The name of the new version
   * @param newEndpoints     Endpoints that were added to the new version
   * @param missingEndpoints Endpoints that were removed or deprecated from the old version
   * @param changedEndpoints Endpoints that were changed from the old to new version - any params, post body or response
   * @return the diff as markdown
   */
  @Override
  public String renderHtml(String oldVersion, String newVersion,
                           String newEndpoints, String missingEndpoints, String changedEndpoints) {
    // we override the renderer because we want to push out some different information
    StringBuilder builder = new StringBuilder()
      .append(chapterHeading())
      .append(format("Version %s to %s", oldVersion, newVersion))
      .append("\n");

    section(builder, "New Endpoints", newEndpoints);
    section(builder, "Deprecated Endpoints", missingEndpoints);
    section(builder, "Changed Endpoints", changedEndpoints);

    return builder.toString();
  }

  /**
   * Build a section heading for the chapter
   *
   * @return the chapter heading texrt
   */
  private String chapterHeading() {
    return heading(baseChapterLevel);
  }

  /**
   * Build a section heading for the chapter subsections
   *
   * @return the chapter section heading texrt
   */
  private String sectionHeading() {
    return heading(1 + baseChapterLevel);
  }

  /**
   * Build a generic section
   *
   * @param level the chapter level
   * @return the header text (prefixed with a new line
   */
  private String heading(int level) {
    return format("%n%s ", repeat('=', level));
  }

  /**
   * build a section, but only if the text isn't blank
   *
   * @param builder the target buffer onto which the text is to be appended
   * @param label   the section name
   * @param text    the test to append
   */
  private void section(StringBuilder builder, String label, String text) {
    if (!text.trim().isEmpty()) {
      builder.append(sectionHeading()).append(label).append("\n").append(text).append("\n");
    }
  }
}
