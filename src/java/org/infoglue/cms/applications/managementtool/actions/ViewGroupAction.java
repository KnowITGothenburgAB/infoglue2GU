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

package org.infoglue.cms.applications.managementtool.actions;

import org.infoglue.cms.security.InfoGlueGroup;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.GroupControllerProxy;
//import org.infoglue.cms.controllers.kernel.impl.simple.GroupPropertiesController;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.applications.common.actions.WebworkAbstractAction;

import java.util.*;

public class ViewGroupAction extends WebworkAbstractAction
{
	private String groupName;
	private boolean supportsUpdate = true;
	private InfoGlueGroup infoGlueGroup;
	private List infoGluePrincipals;
	private List assignedInfoGluePrincipals;
	private List contentTypeDefinitionVOList;
	private List assignedContentTypeDefinitionVOList;    
	
    protected void initialize(String groupName) throws Exception
    {
		this.supportsUpdate				= GroupControllerProxy.getController().getSupportUpdate();
		this.infoGlueGroup				= GroupControllerProxy.getController().getGroup(groupName);
		this.assignedInfoGluePrincipals	= GroupControllerProxy.getController().getInfoGluePrincipals(groupName);
		this.infoGluePrincipals			= UserControllerProxy.getController().getAllUsers();
		
		this.contentTypeDefinitionVOList 			= ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList(ContentTypeDefinitionVO.EXTRANET_ROLE_PROPERTIES);
		//this.assignedContentTypeDefinitionVOList 	= GroupPropertiesController.getController().getContentTypeDefinitionVOList(groupName);  
    } 

    public String doExecute() throws Exception
    {
        this.initialize(getGroupName());
        
        return "success";
    }
        
    public String getGroupName()
    {
        return groupName;
    }

	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}
            
    public java.lang.String getDescription()
    {
        return this.infoGlueGroup.getDescription();
    }
        
  	public List getAllInfoGluePrincipals() throws Exception
	{
		return this.infoGluePrincipals;
	}	
	
	public List getAssignedInfoGluePrincipals() throws Exception
	{
		return this.assignedInfoGluePrincipals;
	}

	public List getAssignedContentTypeDefinitionVOList()
	{
		return assignedContentTypeDefinitionVOList;
	}

	public List getContentTypeDefinitionVOList()
	{
		return contentTypeDefinitionVOList;
	}

	public void setAssignedContentTypeDefinitionVOList(List list)
	{
		assignedContentTypeDefinitionVOList = list;
	}

	public void setContentTypeDefinitionVOList(List list)
	{
		contentTypeDefinitionVOList = list;
	}

	public boolean getSupportsUpdate()
	{
		return this.supportsUpdate;
	}
}
