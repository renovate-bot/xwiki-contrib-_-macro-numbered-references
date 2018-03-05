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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for {@link NumberedHeadingsTransformation}.
 *
 * @version $Id$
 */
@AllComponents
public class NumberedHeadingsTransformationTest
{
    @Rule
    public MockitoComponentMockingRule<Transformation> mocker = new MockitoComponentMockingRule<>(
        NumberedHeadingsTransformation.class, Arrays.asList(ErrorBlockGenerator.class));

    @Test
    public void transform() throws Exception
    {
        String content = "See section {{reference section='C'/}}. Invalid {{reference section='invalid'/}}.\n\n"
            + "= heading A =\n"
            + "== heading B ==\n"
            + "== {{id name='C'/}}heading C ==\n"
            + "=== heading D ===\n";

        Parser parser = this.mocker.getInstance(Parser.class, "xwiki/2.1");
        XDOM xdom = parser.parse(new StringReader(content));
        // Execute the Macro transformation
        MacroTransformation macroTransformation = this.mocker.getInstance(Transformation.class, "macro");
        macroTransformation.transform(xdom, new TransformationContext());

        this.mocker.getComponentUnderTest().transform(xdom, new TransformationContext());
        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = this.mocker.getInstance(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());
        renderer.render(xdom, printer);

        String expectedContent = "See section [[1.2.1>>doc:||anchor=\"C\"]]. "
                + "Invalid (% class=\"xwikirenderingerror\" %)No section id named [invalid] was found"
                    + "(% class=\"xwikirenderingerrordescription hidden\" %){{{Verify the section id used.}}}(%%).\n\n"
            + "= (% class=\"numbered-reference\" %)1(%%) heading A =\n\n"
            + "== (% class=\"numbered-reference\" %)1.1(%%) heading B ==\n\n"
            + "== (% class=\"numbered-reference\" %)1.2(%%) {{id name=\"C\"/}}heading C ==\n\n"
            + "=== (% class=\"numbered-reference\" %)1.2.1(%%) heading D ===";

        assertEquals(expectedContent, printer.toString());
    }

    @Test
    public void transformIgnoresProtectedContent() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [code] []\n"
            + "beginSection\n"
            + "beginHeader [1, null]\n"
            + "onWord [heading]\n"
            + "endHeader [1, null]\n"
            + "endSection\n"
            + "endMacroMarkerStandalone [code] []\n"
            + "endDocument";

        List<Block> sectionBlock = Arrays.asList(new SectionBlock(Arrays.asList(new HeaderBlock(Arrays.asList(
            new WordBlock("heading")), HeaderLevel.LEVEL1))));
        XDOM xdom = new XDOM(Arrays.asList((Block) new MacroMarkerBlock("code", Collections.emptyMap(),
            sectionBlock, false)));
        this.mocker.getComponentUnderTest().transform(xdom, new TransformationContext());

        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = this.mocker.getInstance(BlockRenderer.class, Syntax.EVENT_1_0.toIdString());
        renderer.render(xdom, printer);

        assertEquals(expected, printer.toString());
    }
}
