package com.sonalake.swaggerlog.diff;

import com.deepoove.swagger.diff.SwaggerDiff;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import nl.jworks.markdown_to_asciidoc.Converter;

import java.util.Map;

/**
 * Renders the diff - in actuality, it uses two tools to dump the diff as markdown, and then translate it to asciidoc.
 * <p>
 * It also makes a few changes to the file to make it less hokey.
 */
@Slf4j
class Renderer {

  String render(int baseChapterLevel, SwaggerDiff diff) {
    String renderedDiff = NestedMarkdownRender.builder().baseChapterLevel(baseChapterLevel).build().render(diff);
    return diffToAsciidoc(renderedDiff);
  }

  /**
   *  The provided "convertMarkdownToAsciiDoc" doesn't quite work, so we do some small
   *  changes to the output file so it will render pretty.
   * @param renderedDiff
   * @return
   */
  private String diffToAsciidoc(String renderedDiff) {

    Swapper swapper = Swapper.builder()
      // clean up some of the rendering that breaks our asciidoc
      .replacement("\\n\\s*Parameters", "\n\nParameters\n")
      .replacement("\\n\\s*Return Type", "\n\nReturn Type\n")
      .replacement("—", "")
      // the markdown renderer throws this in and it mucks up asciidoc
      .replacement("\\n----", "\n")
      .build();

    return swapper.swap(Converter.convertMarkdownToAsciiDoc(renderedDiff));
  }

  /**
   * Simple class to allow for a configurable way to do bulk string replacements.
   */
  @Builder
  @RequiredArgsConstructor
  private static class Swapper {
    @Singular
    private final Map<String, String> replacements;

    /**
     * Given a string, apply all the given replacements
     * @param rendered
     * @return
     */
    String swap(String rendered) {
      String updated = rendered;
      for (Map.Entry<String, String> replacement : replacements.entrySet()) {
        String find = replacement.getKey();
        String replace = replacement.getValue();
        updated = updated.replaceAll(find, replace);
      }
      return updated;
    }
  }

}
