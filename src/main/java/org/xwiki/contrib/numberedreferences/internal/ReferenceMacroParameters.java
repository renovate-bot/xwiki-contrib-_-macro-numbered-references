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

import org.xwiki.properties.annotation.PropertyDescription;

import static org.xwiki.contrib.numberedreferences.internal.ReferenceType.FIGURE;
import static org.xwiki.contrib.numberedreferences.internal.ReferenceType.SECTION;

/**
 * Parameters for the {@link ReferenceMacro} Macro.
 *
 * @version $Id$
 * @since 1.0
 */
public class ReferenceMacroParameters
{
    /**
     * The id to link to.
     */
    private String id;

    /**
     * The type of id to link to (section, figure, etc).
     */
    private ReferenceType type;

    /**
     * @param section the id of the section to link to
     */
    @PropertyDescription("Id of the section to link to")
    public void setSection(String section)
    {
        this.id = section;
        this.type = SECTION;
    }

    /**
     * @param figure the id of the figure to link to
     */
    @PropertyDescription("Id of the figure to link to")
    public void setFigure(String figure)
    {
        this.id = figure;
        this.type = FIGURE;
    }

    /**
     * @param id the id to link to
     */
    @PropertyDescription("Id to link to")
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @param type the type of id to link to (section, figure, etc).
     */
    @PropertyDescription("Type of id to link to (section, figure, etc).")
    public void setType(ReferenceType type)
    {
        this.type = type;
    }

    /**
     * @return the id to link to
     */
    public String getSection()
    {
        return getId();
    }

    /**
     * @return the id to link to
     */
    public String getFigure()
    {
        return getId();
    }

    /**
     * @return the id to link to
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the type of id to link to (section, figure, etc).
     */
    public ReferenceType getType()
    {
        return this.type;
    }
}
