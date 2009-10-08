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

package org.infoglue.cms.applications.common.actions;

import java.util.List;

import org.infoglue.cms.applications.common.ToolbarButton;
import org.infoglue.cms.controllers.kernel.impl.simple.ToolbarController;
import org.infoglue.cms.exception.ConfigurationError;

import webwork.util.ServletValueStack;


/**
 * @author Mattias Bogeblad
 *
 *	This action returns a set of toolbar buttons suitable for the toolbar key sent in.
 */

public class ViewToolbarButtonsAction extends InfoGlueAbstractAction 
{
	private static final long serialVersionUID = 8512298644737456785L;

	private String toolbarKey;
	private List<ToolbarButton> buttons;

	public List<ToolbarButton> getButtons() 
	{
		return buttons;
	}

	public String getToolbarKey() 
	{
		return toolbarKey;
	}

	public void setToolbarKey(String toolbarKey) 
	{
		this.toolbarKey = toolbarKey;
	}

	public String doExecute() throws Exception 
	{
		try
		{
			//System.out.println("In execute with toolbarKey:" + toolbarKey);
			this.buttons = this.getToolbarButtons(toolbarKey, getRequest());			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return SUCCESS;
	}

}
