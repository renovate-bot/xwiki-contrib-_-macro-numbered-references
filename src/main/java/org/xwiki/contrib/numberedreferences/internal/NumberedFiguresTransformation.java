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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.block.FigureCaptionBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.marcro.figure.FigureTypeRecognizer;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Find all figures, create numbers in their captions and replace Reference macro blocks with a link block linking to
 * the caption.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Named("numberedfigures")
@Singleton
public class NumberedFiguresTransformation extends AbstractNumberedTransformation
{
    private static final BlockMatcher FIGUREBLOCK_MATCHER = new ClassBlockMatcher(FigureBlock.class);

    private static final String CLASS = "class";

    private static final String FIGURE_CLASS_VALUE = "wikigeneratedfigurenumber";

    private static final String TABLE_CLASS_VALUE = "wikigeneratedtablenumber";

    private static final String FIGURE_TRANSLATION_KEY = "transformation.numberedReferences.figurePrefix";

    private static final String TABLE_TRANSLATION_KEY = "transformation.numberedReferences.tablePrefix";

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private FigureTypeRecognizer figureTypeRecognizer;

    @Override
    public void transform(Block block, TransformationContext context) throws TransformationException
    {
        // Àlgorithm:
        // - Find all FigureBlock (except those in protected data such as inside code macro)
        // - For each FigureBlock, compute the figure number, cache it, and insert it in the associated
        //   FigureCaptionBlock (if any)
        Map<String, List<Block>> figureNumbers = new HashMap<>();
        int figureNumber = 0;
        int tableNumber = 0;
        List<FigureBlock> figureBlocks = block.getBlocks(FIGUREBLOCK_MATCHER, Block.Axes.DESCENDANT);
        for (FigureBlock figureBlock : figureBlocks) {

            if (figureBlock.getChildren().isEmpty() || isInsProtectedBlock(figureBlock)) {
                continue;
            }

            boolean isTable = this.figureTypeRecognizer.isTable(figureBlock);
            int number;
            if (isTable) {
                tableNumber++;
                number = tableNumber;
            } else {
                figureNumber++;
                number = figureNumber;
            }

            // Update the FigureCaptionBlock (if any)
            FigureCaptionBlock figureCaptionBlock = getFigureCaptionBlock(figureBlock);
            if (figureCaptionBlock != null) {
                figureCaptionBlock.insertChildBefore(new SpaceBlock(), figureCaptionBlock.getChildren().get(0));
                figureCaptionBlock.insertChildBefore(serializeAndFormatNumber(number, isTable),
                    figureCaptionBlock.getChildren().get(0));

            }

            // Save in our cache the ids representing this figure by looking for all id macros defined inside the
            // FigureBlock
            List<Block> idBlocks = figureBlock.getBlocks(new ClassBlockMatcher(IdBlock.class), Block.Axes.DESCENDANT);
            for (Block idBlock : idBlocks) {
                figureNumbers.put(((IdBlock) idBlock).getName(), Arrays.asList(serializeNumber(number)));
            }
        }

        // Replace the ReferenceBlock with links
        replaceReferenceBlocks(block, figureNumbers, "figure");
    }

    private Block serializeAndFormatNumber(int number, boolean isTable)
    {
        String key = isTable ? TABLE_TRANSLATION_KEY : FIGURE_TRANSLATION_KEY;
        Translation translation = this.localizationManager.getTranslation(key);
        List<Block> blocks = new ArrayList<>();
        blocks.add(translation.render(number));
        blocks.add(new SpaceBlock());
        String classValue = isTable ? TABLE_CLASS_VALUE : FIGURE_CLASS_VALUE;
        return new FormatBlock(blocks, Format.NONE, Collections.singletonMap(CLASS, classValue));
    }

    private Block serializeNumber(int number)
    {
        return new WordBlock(String.valueOf(number));
    }

    private FigureCaptionBlock getFigureCaptionBlock(FigureBlock block)
    {
        FigureCaptionBlock result = null;
        List<Block> children = block.getChildren();
        if (children.size() > 0) {
            // The FigureCaptionBlock is either the first child block or the last one
            result = extractFigureCaptionBlock(children.get(0));
            if (result == null) {
                result = extractFigureCaptionBlock(children.get(children.size() - 1));
            }
        }
        return result;
    }

    private FigureCaptionBlock extractFigureCaptionBlock(Block block)
    {
        FigureCaptionBlock result = null;
        if (block instanceof MacroMarkerBlock) {
            MacroMarkerBlock macroMarkerBlock = (MacroMarkerBlock) block;
            if (macroMarkerBlock.getChildren().size() > 0
                && macroMarkerBlock.getChildren().get(0) instanceof FigureCaptionBlock)
            {
                result = (FigureCaptionBlock) macroMarkerBlock.getChildren().get(0);
            }
        }
        return result;
    }
}
