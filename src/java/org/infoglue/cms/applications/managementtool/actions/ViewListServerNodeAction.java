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

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.ServerNodeController;

import java.util.List;


/**
 * 	Action class for usecase ViewListServerNodeUCC 
 *
 *  @author Mattias Bogeblad
 */

public class ViewListServerNodeAction extends InfoGlueAbstractAction 
{
	private List serverNodes;
	private List allowedAdminIPList;
	private String allowedAdminIP;
	
	protected String doExecute() throws Exception 
	{
		this.serverNodes = ServerNodeController.getController().getServerNodeVOList();
		this.allowedAdminIPList = ServerNodeController.getController().getAllowedAdminIPList();
    	this.allowedAdminIP = ServerNodeController.getController().getAllowedAdminIP();
    	return "success";
	}
	

	public List getServerNodes()
	{
		return this.serverNodes;		
	}

	public String doSave() throws Exception 
	{
		ServerNodeController.getController().setAllowedAdminIP(allowedAdminIP);
    	return "success";
	}


	public List getAllowedAdminIPList()
	{
		return this.allowedAdminIPList;		
	}
	
    public String getAllowedAdminIP()
    {
        return allowedAdminIP;
    }
    
    public void setAllowedAdminIP(String allowedAdminIP)
    {
        this.allowedAdminIP = allowedAdminIP;
    }
}
