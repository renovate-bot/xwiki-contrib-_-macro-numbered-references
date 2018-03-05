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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.transformation.AbstractTransformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.util.ErrorBlockGenerator;

/**
 * Find all headings, create numbers (and support nested numbering with the dot notation, e.g. {@code 1.1.1.1}) for
 * them and display the number in front of the heading label.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("numberedheadings")
@Singleton
public class NumberedHeadingsTransformation extends AbstractTransformation
{
    private static final String CLASS = "class";

    private static final String CLASS_VALUE = "numbered-reference";

    private static final BlockMatcher HEADINGBLOCK_MATCHER = new ClassBlockMatcher(HeaderBlock.class);

    private static final BlockMatcher SECTIONBLOCK_MATCHER = new ClassBlockMatcher(SectionBlock.class);

    private static final SpecialSymbolBlock DOT_BLOCK = new SpecialSymbolBlock('.');

    @Inject
    private ErrorBlockGenerator errorBlockGenerator;

    @Override
    public int getPriority()
    {
        // Use a high value so that it's executed last so that the Macro transformation and any other transformations
        // can contribute headings to the XDOM and thus they can be numbered too.
        return 2000;
    }

    @Override
    public void transform(Block block, TransformationContext context) throws TransformationException
    {
        // Algorithm - Part 1:
        // - For each HeaderBlock:
        //   - Check if there's already been a number added in a previous sibling HeaderBlock and if so increase the
        //     last digit of the counter by 1.
        //   - If not, then check if there's already been a number added in a parent HeaderBlock and if so add a new
        //     counter digit (e.g. if the parent if "3.5", then the new number if "3.5.1".
        //   - If not, then start the number at 1.
        //   - Insert the new computed number as the first Block in the Heading children.
        // Notes:
        // - In order to recognize the number inserted, we wrap it into a GroupBlock with a specific class parameter.
        //
        // Algorithm - Part 2:
        // - For each MacroMarkerBlock for the "reference" macro:
        //   - Insert a LinkBlock with a label being the matching number

        Map<String, List<Integer>> headingNumbers = new HashMap<>();

        // Part 1
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
            headerBlock.insertChildBefore(serializeAndFormatNumber(number), headerBlock.getChildren().get(0));

            // Step 3: Save in our cache the ids representing this section. We save the following keys in the cache:
            // - the header block id
            // - all the IdBlock found as children Blocks of the header block
            if (headerBlock.getId() != null) {
                headingNumbers.put(headerBlock.getId(), number);
            }
            List<Block> idBlocks = block.getBlocks(new ClassBlockMatcher(IdBlock.class), Block.Axes.DESCENDANT);
            for (Block idBlock : idBlocks) {
                // We must pay attention to not save the IdBLock generated by the Reference macro!
                headingNumbers.put(((IdBlock) idBlock).getName(), number);
            }
        }

        // Part 2
        replaceReferenceBlocks(block, headingNumbers);
    }

    private void replaceReferenceBlocks(Block block, Map<String, List<Integer>> headingNumbers)
    {
        List<Block> referenceBlocks =
            block.getBlocks(new ClassBlockMatcher(ReferenceBlock.class), Block.Axes.DESCENDANT);
        for (Block untypedReferenceBlock : referenceBlocks) {
            // Replace the ReferenceBlock blocks with a LinkBlock, if we can find a matching reference.
            // Otherwise return some error blocks.
            ReferenceBlock referenceBlock = (ReferenceBlock) untypedReferenceBlock;
            String id = referenceBlock.getId();
            List<Integer> number = headingNumbers.get(id);
            MacroMarkerBlock referenceParentBlock = (MacroMarkerBlock) referenceBlock.getParent();
            if (number == null) {
                // Generate error blocks
                String message = String.format("No section id named [%s] was found", id);
                String description = "Verify the section id used.";
                List<Block> errorBlocks =
                    this.errorBlockGenerator.generateErrorBlocks(message, description, referenceParentBlock.isInline());
                referenceParentBlock.getParent().replaceChild(errorBlocks, referenceParentBlock);
            } else {
                // Add the LinkBlock
                DocumentResourceReference resourceReference = new DocumentResourceReference("");
                resourceReference.setAnchor(id);
                LinkBlock linkBlock = new LinkBlock(serializeNumber(number), resourceReference, false);
                referenceParentBlock.getParent().replaceChild(linkBlock, referenceParentBlock);
            }
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

    private List<Block> serializeNumber(List<Integer> number)
    {
        List<Block> valueBlocks = new ArrayList<>();
        Iterator<Integer> iterator = number.iterator();
        while (iterator.hasNext()) {
            valueBlocks.add(new WordBlock(String.valueOf(iterator.next())));
            if (iterator.hasNext()) {
                valueBlocks.add(DOT_BLOCK);
            }
        }
        return valueBlocks;
    }

    private Block serializeAndFormatNumber(List<Integer> number)
    {
        return new FormatBlock(serializeNumber(number), Format.NONE, Collections.singletonMap(CLASS, CLASS_VALUE));
    }

    // TODO: Remove this when https://jira.xwiki.org/browse/XWIKI-15093 is implemented
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
