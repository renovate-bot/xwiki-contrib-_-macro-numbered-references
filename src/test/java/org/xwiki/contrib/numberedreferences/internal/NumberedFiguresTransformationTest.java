/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.numberedreferences.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.block.FigureCaptionBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link NumberedFiguresTransformation}.
 *
 * @version $Id$
 */
@AllComponents
public class NumberedFiguresTransformationTest
{
    @Rule
    public MockitoComponentMockingRule<Transformation> mocker = new MockitoComponentMockingRule<>(
        NumberedFiguresTransformation.class);

    @AfterComponent
    public void setUp() throws Exception
    {
        ContextualLocalizationManager localizationManager =
            this.mocker.registerMockComponent(ContextualLocalizationManager.class);
        Translation translation = mock(Translation.class);
        when(translation.render()).thenReturn(new CompositeBlock(Arrays.asList(
            new WordBlock("Figure"), new SpecialSymbolBlock(':'))));
        when(localizationManager.getTranslation("transformation.numberedReferences.figurePrefix")).thenReturn(
            translation);
    }

    @Test
    public void transform() throws Exception
    {
        String content = "See figure {{reference figure='F1'/}}. Invalid {{reference figure='invalid'/}}.\n\n"
            + "{{figure}}\n"
            + "[[image:whatever]]\n\n"
            + "{{figureCaption}}\n"
            + "{{id name='F1'/}}Nice image\n"
            + "{{/figureCaption}}\n"
            + "{{/figure}}";

        Parser parser = this.mocker.getInstance(Parser.class, "xwiki/2.1");
        XDOM xdom = parser.parse(new StringReader(content));
        // Execute the Macro transformation
        MacroTransformation macroTransformation = this.mocker.getInstance(Transformation.class, "macro");
        macroTransformation.transform(xdom, new TransformationContext());

        this.mocker.getComponentUnderTest().transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = this.mocker.getInstance(BlockRenderer.class, Syntax.EVENT_1_0.toIdString());
        renderer.render(xdom, printer);

        String expectedContent = "beginDocument [[syntax]=[XWiki 2.1]]\n"
                + "beginParagraph\n"
                + "onWord [See]\n"
                + "onSpace\n"
                + "onWord [figure]\n"
                + "onSpace\n"
                + "beginMacroMarkerInline [reference] [figure=F1]\n"
                + "beginLink [Typed = [true] Type = [doc] Reference = [] Parameters = [[anchor] = [F1]]] [false]\n"
                + "onWord [1]\n"
                + "endLink [Typed = [true] Type = [doc] Reference = [] Parameters = [[anchor] = [F1]]] [false]\n"
                + "endMacroMarkerInline [reference] [figure=F1]\n"
                + "onSpecialSymbol [.]\n"
                + "onSpace\n"
                + "onWord [Invalid]\n"
                + "onSpace\n"
                + "beginMacroMarkerInline [reference] [figure=invalid]\n"
                + "beginFormat [NONE] [[class]=[xwikirenderingerror]]\n"
                + "onWord [No figure id named [invalid] was found]\n"
                + "endFormat [NONE] [[class]=[xwikirenderingerror]]\n"
                + "beginFormat [NONE] [[class]=[xwikirenderingerrordescription hidden]]\n"
                + "onVerbatim [Verify the figure id used.] [true]\n"
                + "endFormat [NONE] [[class]=[xwikirenderingerrordescription hidden]]\n"
                + "endMacroMarkerInline [reference] [figure=invalid]\n"
                + "onSpecialSymbol [.]\n"
                + "endParagraph\n"
                + "beginMacroMarkerStandalone [figure] [] [[[image:whatever]]\n"
                + "\n"
                + "{{figureCaption}}\n"
                + "{{id name='F1'/}}Nice image\n"
                + "{{/figureCaption}}]\n"
                + "beginParagraph\n"
                + "onImage [Typed = [false] Type = [url] Reference = [whatever]] [false]\n"
                + "endParagraph\n"
                + "beginMacroMarkerStandalone [figureCaption] [] [{{id name='F1'/}}Nice image]\n"
                + "beginFormat [NONE] [[class]=[numbered-figure-reference]]\n"
                + "onWord [Figure]\n"
                + "onSpecialSymbol [:]\n"
                + "onSpace\n"
                + "onWord [1]\n"
                + "endFormat [NONE] [[class]=[numbered-figure-reference]]\n"
                + "onSpace\n"
                + "beginMacroMarkerInline [id] [name=F1]\n"
                + "onId [F1]\n"
                + "endMacroMarkerInline [id] [name=F1]\n"
                + "onWord [Nice]\n"
                + "onSpace\n"
                + "onWord [image]\n"
                + "endMacroMarkerStandalone [figureCaption] [] [{{id name='F1'/}}Nice image]\n"
                + "endMacroMarkerStandalone [figure] [] [[[image:whatever]]\n"
                + "\n"
                + "{{figureCaption}}\n"
                + "{{id name='F1'/}}Nice image\n"
                + "{{/figureCaption}}]\n"
                + "endDocument [[syntax]=[XWiki 2.1]]";

        assertEquals(expectedContent, printer.toString());
    }

    @Test
    public void transformIgnoresProtectedContent() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [code] [] []\n"
            + "beginMacroMarkerStandalone [figure] [] []\n"
            + "beginMacroMarkerStandalone [figureCaption] [] []\n"
            + "onWord [caption]\n"
            + "endMacroMarkerStandalone [figureCaption] [] []\n"
            + "endMacroMarkerStandalone [figure] [] []\n"
            + "endMacroMarkerStandalone [code] [] []\n"
            + "endDocument";

        List<Block> figureBlocks = blocks(createMacroMarkerBlock("figure",
            blocks(new FigureBlock(blocks(createMacroMarkerBlock("figureCaption",
                blocks(new FigureCaptionBlock(blocks(new WordBlock("caption"))))))))));
        XDOM xdom = new XDOM(blocks(createMacroMarkerBlock("code", figureBlocks)));
        this.mocker.getComponentUnderTest().transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = this.mocker.getInstance(BlockRenderer.class, Syntax.EVENT_1_0.toIdString());
        renderer.render(xdom, printer);

        assertEquals(expected, printer.toString());
    }

    private List<Block> blocks(Block... blocks)
    {
        return Arrays.asList(blocks);
    }

    private MacroMarkerBlock createMacroMarkerBlock(String macroId, List<Block> blocks)
    {
        return new MacroMarkerBlock(macroId, Collections.emptyMap(), "", blocks, false);
    }
}
