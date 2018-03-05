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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.listener.Listener;

/**
 * Carries information for the {@link NumberedHeadingsTransformation} transformation which will replace this block
 * by a Link block.
 *
 * @version $Id$
 * @since 1.0
 */
public class ReferenceBlock extends AbstractBlock
{
    /**
     * The unique id for the reference/location.
     */
    private String id;

    /**
     * @param id the unique id for the reference/location.
     */
    public ReferenceBlock(String id)
    {
        this.id = id;
    }

    /**
     * @return the reference/location id
     */
    public String getId()
    {
        return this.id;
    }

    @Override
    public void traverse(Listener listener)
    {
        // Don't do anything, this block is not supposed to be rendered anyway
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ReferenceBlock) {
            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getId(), ((ReferenceBlock) obj).getId());

            return builder.isEquals();
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getId());

        return builder.toHashCode();
    }
}
