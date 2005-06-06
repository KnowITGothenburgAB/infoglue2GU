/* ===============================================================================
*
* Part of the InfoGlue Content Management Platform (www.infoglue.org)
*
* ===============================================================================
*
*  Copyright (C)
* 
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License version 2, as published by the
* Free Software Foundation. See the file LICENSE.html for more information.
* 
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License along with
* this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
* Place, Suite 330 / Boston, MA 02111-1307 / USA.
*
* ===============================================================================
*/

package org.infoglue.deliver.taglib.content;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.infoglue.deliver.taglib.TemplateControllerTag;
import org.infoglue.deliver.taglib.component.ComponentLogicTag;

public class MatchingContentsTag extends TemplateControllerTag {
	private static final long serialVersionUID = 4050206323348354355L;

	private String contentTypeDefinitionNames;
	private String categoryNames;
	private int numberOfItems = 2;
	
    public MatchingContentsTag()
    {
        super();
    }

	public int doEndTag() throws JspException
    {
	    setResultAttribute(getController().getMatchingContents(contentTypeDefinitionNames, categoryNames, "publishDateTime", "desc", true, numberOfItems));
        return EVAL_PAGE;
    }

    public void setContentTypeDefinitionNames(String contentTypeDefinitionNames)
    {
        this.contentTypeDefinitionNames = contentTypeDefinitionNames;
    }

    public void setCategoryNames(String categoryNames)
    {
        this.categoryNames = categoryNames;
    }
    
    public void setNumberOfItems(int numberOfItems)
    {
        this.numberOfItems = numberOfItems;
    }
    
}
