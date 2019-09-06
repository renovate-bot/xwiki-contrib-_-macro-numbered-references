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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.transformation.AbstractTransformation;

/**
 * Common code for all numbered transformations.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractNumberedTransformation extends AbstractTransformation
{
    @Override
    public int getPriority()
    {
        // Use a high value so that it's executed last so that the Macro transformation and any other transformations
        // can contribute headings to the XDOM and thus they can be numbered too.
        return 2000;
    }

    protected void replaceReferenceBlocks(Block block, Map<String, List<Block>> numbers)
    {
        List<Block> referenceBlocks =
            block.getBlocks(new ClassBlockMatcher(ReferenceBlock.class), Block.Axes.DESCENDANT);
        for (Block untypedReferenceBlock : referenceBlocks) {
            // Replace the ReferenceBlock blocks with a LinkBlock, if we can find a matching reference.
            // Otherwise don't do anything since another transformation might be a match.
            ReferenceBlock referenceBlock = (ReferenceBlock) untypedReferenceBlock;
            String id = referenceBlock.getId();
            List<Block> numberBlocks = numbers.get(id);
            Block referenceParentBlock = referenceBlock.getParent();
            if (numberBlocks != null) {
                // Add the LinkBlock
                DocumentResourceReference resourceReference = new DocumentResourceReference("");
                resourceReference.setAnchor(id);
                LinkBlock linkBlock = new LinkBlock(numberBlocks, resourceReference, false);
                referenceParentBlock.setChildren(Arrays.asList(linkBlock));
            }
        }
    }

    // TODO: Remove this when https://jira.xwiki.org/browse/XWIKI-15093 is implemented
    protected boolean isInsProtectedBlock(Block block)
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
