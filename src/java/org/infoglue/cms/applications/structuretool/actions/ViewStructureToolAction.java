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

package org.infoglue.cms.applications.structuretool.actions;

import org.infoglue.cms.applications.cmstool.actions.ViewCMSAbstractToolAction;

/**
 * This class implements the action class for the framed page in the siteNode tool.
 * 
 * @author Mattias Bogeblad  
 */

public class ViewStructureToolAction extends ViewCMSAbstractToolAction
{ 
	private static final long serialVersionUID = 1L;

	private String siteNodeId = "-1";

    public String doExecute() throws Exception
    {
        return "success";
    }

	public String getSiteNodeId() 
	{
		return siteNodeId;
	}

	public void setSiteNodeId(String siteNodeId) 
	{
		this.siteNodeId = siteNodeId;
	}

               
}
