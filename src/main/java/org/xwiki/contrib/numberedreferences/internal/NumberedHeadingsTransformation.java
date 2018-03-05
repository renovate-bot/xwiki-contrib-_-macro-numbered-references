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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.block.ProtectedBlockFilter;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.transformation.AbstractTransformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Find all headings, create numbers (and support nested numbering with the dot notation, e.g. {@code 1.1.1.1}) for
 * them and display the number in front of the heading label.
 *
 * @version $Id$
 * @since 1.0
 */
public class NumberedHeadingsTransformation extends AbstractTransformation
{
    private static final String CLASS = "class";

    private static final String CLASS_VALUE = "numbered-reference";

    private static final BlockMatcher HEADINGBLOCK_MATCHER = new ClassBlockMatcher(HeaderBlock.class);

    private static final BlockMatcher SECTIONBLOCK_MATCHER = new ClassBlockMatcher(SectionBlock.class);

    private static final SpecialSymbolBlock DOT_BLOCK = new SpecialSymbolBlock('.');

    /**
     * Used to filter protected blocks (code macro marker block, etc).
     */
    private ProtectedBlockFilter filter = new ProtectedBlockFilter();

    @Override
    public void transform(Block block, TransformationContext context) throws TransformationException
    {
        // Algorithm:
        // - For each HeaderBlock:
        //   - Check if there's already been a number added in a previous sibling HeaderBlock and if so increase the
        //     last digit of the counter by 1.
        //   - If not, then check if there's already been a number added in a parent HeaderBlock and if so add a new
        //     counter digit (e.g. if the parent if "3.5", then the new number if "3.5.1".
        //   - If not, then start the number at 1.
        //   - Insert the new computed number as the first Block in the Heading children.
        // Notes:
        // - In order to recognize the number inserted, we wrap it into a GroupBlock with a specific class parameter.
        List<HeaderBlock> headerBlocks = block.getBlocks(HEADINGBLOCK_MATCHER, Block.Axes.DESCENDANT);
        for (HeaderBlock headerBlock : headerBlocks) {

            if (headerBlock.getChildren().isEmpty() || isInsProtectedBlock(headerBlock)) {
                continue;
            }

            // Step 1: Find number in previous Header blocks and increase by 1
            List<Integer> number =
                extractNumberFromPreviousBlocks(headerBlock.getSection(), Block.Axes.PRECEDING_SIBLING);
            if (number.isEmpty()) {
                // Find ancestor Header blocks
                number =  extractNumberFromPreviousBlocks(headerBlock.getSection(), Block.Axes.ANCESTOR);
                if (number.isEmpty()) {
                    // No previous number, start at 1!
                    number = new ArrayList<>();
                }
                number.add(1);
            } else {
                int newValue = number.get(number.size() - 1) + 1;
                number.set(number.size() - 1, newValue);
            }

            // Step 2: Insert the number in the header
            // Start by adding a space so that we have <number><space><rest of what was there before>
            headerBlock.insertChildBefore(new SpaceBlock(), headerBlock.getChildren().get(0));
            headerBlock.insertChildBefore(serializeNumber(number), headerBlock.getChildren().get(0));
        }
    }

    private List<Integer> extractNumberFromHeading(SectionBlock sectionBlock)
    {
        HeaderBlock block = sectionBlock.getHeaderBlock();
        if (block != null && !block.getChildren().isEmpty() && block.getChildren().get(0) instanceof FormatBlock) {
            FormatBlock formatBlock = (FormatBlock) block.getChildren().get(0);
            if (formatBlock.getParameter(CLASS) != null && formatBlock.getParameter(CLASS).contains(CLASS_VALUE)) {
                return parseNumbers(formatBlock.getChildren());
            }
        }

        return Collections.emptyList();
    }

    private List<Integer> extractNumberFromPreviousBlocks(SectionBlock block, Block.Axes axe)
    {
        List<SectionBlock> previousSectionBlocks = block.getBlocks(SECTIONBLOCK_MATCHER, axe);
        for (SectionBlock sectionBlock : previousSectionBlocks) {
            List<Integer> number = extractNumberFromHeading(sectionBlock);
            if (!number.isEmpty()) {
                return number;
            }
        }

        return Collections.emptyList();
    }

    private List<Integer> parseNumbers(List<Block> blocks)
    {
        List<Integer> result = new ArrayList<>();

        // The blocks are of the format: (WORD_BLOCK)(SPECIAL_SYMBOL_BLOCK + WORD_BLOCK)*
        for (Block block : blocks) {
            if (block instanceof WordBlock) {
                result.add(Integer.parseInt(((WordBlock) block).getWord()));
            }
        }

        return result;
    }

    private Block serializeNumber(List<Integer> number)
    {
        List<Block> valueBlocks = new ArrayList<>();
        Iterator<Integer> iterator = number.iterator();
        while (iterator.hasNext()) {
            valueBlocks.add(new WordBlock(String.valueOf(iterator.next())));
            if (iterator.hasNext()) {
                valueBlocks.add(DOT_BLOCK);
            }
        }
        return new FormatBlock(valueBlocks, Format.NONE, Collections.singletonMap(CLASS, CLASS_VALUE));
    }

    // TODO: Remove this when http://jira.xwiki/org/XXXX is implemented
    private boolean isInsProtectedBlock(Block block)
    {
        Block currentBlock = block;
        while (currentBlock != null) {
            if (isProtectedBlock(currentBlock)) {
                return true;
            }
            currentBlock = currentBlock.getParent();
        }
        return false;
    }

    private boolean isProtectedBlock(Block block)
    {
        return (block instanceof MacroMarkerBlock) && "code".equals(((MacroMarkerBlock) block).getId());
    }
}
