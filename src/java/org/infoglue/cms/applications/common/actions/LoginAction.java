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

import org.infoglue.cms.controllers.usecases.common.LoginUCC;
import org.infoglue.cms.controllers.usecases.common.LoginUCCFactory;
import org.infoglue.cms.security.AuthenticationModule;

public class LoginAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 35668814570153876L;

	private String userName     = null;
	private String password     = null;
	private String errorMessage = "";
	private String referringUrl = null;
	
	public void setUserName(String userName)
	{
		this.userName = userName;
	}
	
	public String getUserName()
	{
		return this.userName;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}
	
	public String getPassword()
	{
		return this.password;
	}

	public String getErrorMessage()
	{
		return this.errorMessage;
	}
	
	public String doExecute() throws Exception 
	{
		if(this.getRequest().getRemoteUser() != null)
			return "redirect";
		else
			return "success";
	}	

	public String doInvalidLogin() throws Exception 
	{
		if(this.getRequest().getRemoteUser() != null)
		{
			return "redirect";
		}
		else
		{
			return "invalidLogin";
		}
	}	

	public String doLogonUser() throws Exception 
	{
		LoginUCC loginController = LoginUCCFactory.newLoginUCC();
		boolean isAccepted = loginController.authorizeSystemUser(this.userName, this.password);
		
		if(isAccepted)
		{
			return "userAccepted";
		}
		else
		{
			errorMessage = "The logon information given was incorrect, please verify and try again.";
			return "invalidLogin";
		}
	}	
	
	/**
	 * This command invalidates the current session and then calls the authentication module logout method so it can
	 * do it's stuff. Sometimes it involves redirecting the user somewhere and then we returns nothing in this method.
	 */

	public String doLogout() throws Exception
	{
		getHttpSession().invalidate();
		
		AuthenticationModule authenticationModule = AuthenticationModule.getAuthenticationModule(null);
		boolean redirected = authenticationModule.logoutUser(getRequest(), getResponse());
		
		if(redirected)
			return NONE;
		else
			return "logout";
	}
	
	public String getPrincipal()
	{ 
		java.security.Principal principal = getRequest().getUserPrincipal();
		return "Principal:" + principal.getName();
	}

	public String getReferringUrl()
	{
		return referringUrl;
	}

	public void setReferringUrl(String string)
	{
		referringUrl = string;
	}

}