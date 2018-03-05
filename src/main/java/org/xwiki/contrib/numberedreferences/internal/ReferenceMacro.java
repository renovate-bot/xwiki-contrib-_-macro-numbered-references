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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Create a link to a section id, displaying the section number as the link label.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("reference")
@Singleton
public class ReferenceMacro extends AbstractMacro<ReferenceMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION =
        "Create a link to a section id, displaying the section number as the link label.";

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ReferenceMacro()
    {
        super("Id", DESCRIPTION, ReferenceMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_NAVIGATION);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(ReferenceMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // We pass the section id inside a custo ReferenceBlock.
        // It'll be the "numberedheadings" transformation's goal to modify the XDOM for the macro when it executes,
        // thus computing the section number and replacing the ReferenceBlock with a LinkBlock.
        ReferenceBlock block = new ReferenceBlock(parameters.getSection());
        return Collections.singletonList(block);
    }
}
