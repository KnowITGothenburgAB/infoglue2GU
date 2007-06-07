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

package org.infoglue.cms.webservices;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
import org.infoglue.cms.controllers.kernel.impl.simple.GroupControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.RoleControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.ServerNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.UserPropertiesController;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.management.GroupVO;
import org.infoglue.cms.entities.management.RoleVO;
import org.infoglue.cms.entities.management.SystemUserVO;
import org.infoglue.cms.entities.management.UserPropertiesVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGlueGroup;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.security.InfoGlueRole;
import org.infoglue.cms.util.dom.DOMBuilder;
import org.infoglue.cms.webservices.elements.RemoteAttachment;
import org.infoglue.common.security.beans.InfoGlueGroupBean;
import org.infoglue.common.security.beans.InfoGluePrincipalBean;
import org.infoglue.common.security.beans.InfoGlueRoleBean;
import org.infoglue.deliver.util.webservices.DynamicWebserviceSerializer;


/**
 * This class is responsible for letting an external application call InfoGlue
 * API:s remotely. It handles api:s to manage user properties.
 * 
 * @author Mattias Bogeblad
 */

public class RemoteUserServiceImpl extends RemoteInfoGlueService
{
    private final static Logger logger = Logger.getLogger(RemoteUserServiceImpl.class.getName());

	/**
	 * The principal executing the workflow.
	 */
	private InfoGluePrincipal principal;

    private static UserControllerProxy userControllerProxy = UserControllerProxy.getController();
    
    /**
     * Registers a new system user.
     */
    
    public Boolean createUser(final String principalName, String firstName, String lastName, String email, String userName, String password, List roleNames, List groupNames) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return new Boolean(false);
        }

        Boolean status = new Boolean(true);
        
        logger.info("***************************************");
        logger.info("Creating user through webservice.......");
        logger.info("***************************************");
        
        try
        {
            initializePrincipal(principalName);
            
            SystemUserVO systemUserVO = new SystemUserVO();
            systemUserVO.setFirstName(firstName);
            systemUserVO.setLastName(lastName);
            systemUserVO.setEmail(email);
            systemUserVO.setUserName(userName);
            systemUserVO.setPassword(password);
            
            Object[] roleNamesArray = roleNames.toArray();
            Object[] groupNamesArray = groupNames.toArray();
            
            String[] roles = new String[roleNamesArray.length];
            String[] groups = new String[groupNamesArray.length];
            
            for(int i=0; i<roleNamesArray.length; i++)
            	roles[i] = "" + roleNamesArray[i];

            for(int i=0; i<groupNamesArray.length; i++)
            	groups[i] = "" + groupNamesArray[i];

            userControllerProxy.createUser(systemUserVO);
            userControllerProxy.updateUser(systemUserVO, roles, groups);
        }
        catch(Exception e)
        {
        	status = new Boolean(false);
            logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        updateCaches();

        return status;    
    }

    /**
     * Updates a system user.
     */
    
    public Boolean updateUser(final String principalName, SystemUserVO systemUserVO, String[] roleNames, String[] groupNames) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return new Boolean(false);
        }

        Boolean status = new Boolean(true);
        
        logger.info("***************************************");
        logger.info("Updating user through webservice.......");
        logger.info("***************************************");
        
        try
        {
            initializePrincipal(principalName);
            
            userControllerProxy.updateUser(systemUserVO, roleNames, groupNames);
        }
        catch(Exception e)
        {
        	status = new Boolean(false);
            logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        updateCaches();

        return status;    
    }

    /**
     * Deletes a system user.
     */
    
    public Boolean deleteUser(final String principalName, SystemUserVO systemUserVO) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return new Boolean(false);
        }

        Boolean status = new Boolean(true);
        
        logger.info("***************************************");
        logger.info("Delete user through webservice.........");
        logger.info("***************************************");
        
        try
        {
            initializePrincipal(principalName);
            
            userControllerProxy.deleteUser(systemUserVO.getUserName());
        }
        catch(Exception e)
        {
        	status = new Boolean(false);
            logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        updateCaches();

        return status;    
    }

    /**
     * Gets all roles available.
     */
    
    public List<InfoGlueRoleBean> getRoles() 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        List<InfoGlueRoleBean> roles = new ArrayList<InfoGlueRoleBean>();
        
        logger.info("***************************************");
        logger.info("Getting all roles through webservice...");
        logger.info("***************************************");
        
        try
        {
            List rolesList = RoleControllerProxy.getController().getAllRoles();
            
            Iterator rolesListIterator = rolesList.iterator();
            while(rolesListIterator.hasNext())
            {
            	InfoGlueRole role = (InfoGlueRole)rolesListIterator.next();
            	InfoGlueRoleBean bean = new InfoGlueRoleBean();
            	bean.setName(role.getName());
            	bean.setDisplayName(role.getDisplayName());
            	bean.setDescription(role.getDescription());
            	roles.add(bean);	
            }
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return roles;    
    }

    /**
     * Gets all roles available.
     */
    
    public List<InfoGlueGroupBean> getGroups() 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        List<InfoGlueGroupBean> groups = new ArrayList<InfoGlueGroupBean>();
        
        logger.info("***************************************");
        logger.info("Getting all groups through webservice..");
        logger.info("***************************************");
        
        try
        {
            List groupsList = GroupControllerProxy.getController().getAllGroups();

            Iterator groupsListIterator = groupsList.iterator();
            while(groupsListIterator.hasNext())
            {
            	InfoGlueGroup group = (InfoGlueGroup)groupsListIterator.next();
            	InfoGlueGroupBean bean = new InfoGlueGroupBean();
            	bean.setName(group.getName());
            	bean.setDisplayName(group.getDisplayName());
            	bean.setDescription(group.getDescription());
            	groups.add(bean);	
            }
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return groups;    
    }


    /**
     * Gets a principal.
     */
    
    public InfoGluePrincipalBean getPrincipal(String userName) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        InfoGluePrincipalBean bean = null;
        
        logger.info("***************************************");
        logger.info("Getting all principals through webservice..");
        logger.info("***************************************");
        
        try
        {
            InfoGluePrincipal principal = UserControllerProxy.getController().getUser(userName);

        	bean = new InfoGluePrincipalBean();
        	bean.setName(principal.getName());
        	bean.setDisplayName(principal.getDisplayName());
        	bean.setEmail(principal.getEmail());
        	bean.setFirstName(principal.getFirstName());
        	bean.setLastName(principal.getLastName());
        	bean.setAdministrator(false);
        	bean.setMetaInformation(principal.getMetaInformation());
        	
        	List groups = new ArrayList();
        	Iterator groupsListIterator = principal.getGroups().iterator();
            while(groupsListIterator.hasNext())
            {
            	InfoGlueGroup group = (InfoGlueGroup)groupsListIterator.next();
            	InfoGlueGroupBean groupBean = new InfoGlueGroupBean();
            	groupBean.setName(group.getName());
            	groupBean.setDisplayName(group.getDisplayName());
            	groupBean.setDescription(group.getDescription());
            	groups.add(groupBean);	
            }
            bean.setGroups(groups);

        	List roles = new ArrayList();
        	Iterator rolesListIterator = principal.getRoles().iterator();
            while(rolesListIterator.hasNext())
            {
            	InfoGlueRole role = (InfoGlueRole)rolesListIterator.next();
            	InfoGlueRoleBean roleBean = new InfoGlueRoleBean();
            	roleBean.setName(role.getName());
            	roleBean.setDisplayName(role.getDisplayName());
            	roleBean.setDescription(role.getDescription());
            	roles.add(roleBean);	
            }
            bean.setRoles(roles);
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return bean;    
    }

    /**
     * Gets all roles available.
     */
    
    public List<InfoGluePrincipalBean> getPrincipals() 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        List<InfoGluePrincipalBean> users = new ArrayList<InfoGluePrincipalBean>();
        
        logger.info("***************************************");
        logger.info("Getting all principals through webservice..");
        logger.info("***************************************");
        
        try
        {
            List principalList = UserControllerProxy.getController().getAllUsers();

            Iterator principalListIterator = principalList.iterator();
            while(principalListIterator.hasNext())
            {
            	InfoGluePrincipal principal = (InfoGluePrincipal)principalListIterator.next();
            	InfoGluePrincipalBean bean = new InfoGluePrincipalBean();
            	bean.setName(principal.getName());
            	bean.setDisplayName(principal.getDisplayName());
            	bean.setEmail(principal.getEmail());
            	bean.setFirstName(principal.getFirstName());
            	bean.setLastName(principal.getLastName());
            	bean.setAdministrator(false);
            	bean.setMetaInformation(principal.getMetaInformation());
            	
            	List groups = new ArrayList();
            	Iterator groupsListIterator = principal.getGroups().iterator();
                while(groupsListIterator.hasNext())
                {
                	InfoGlueGroup group = (InfoGlueGroup)groupsListIterator.next();
                	InfoGlueGroupBean groupBean = new InfoGlueGroupBean();
                	groupBean.setName(group.getName());
                	groupBean.setDisplayName(group.getDisplayName());
                	groupBean.setDescription(group.getDescription());
                	groups.add(groupBean);	
                }
                bean.setGroups(groups);

            	List roles = new ArrayList();
            	Iterator rolesListIterator = principal.getRoles().iterator();
                while(rolesListIterator.hasNext())
                {
                	InfoGlueRole role = (InfoGlueRole)rolesListIterator.next();
                	InfoGlueRoleBean roleBean = new InfoGlueRoleBean();
                	roleBean.setName(role.getName());
                	roleBean.setDisplayName(role.getDisplayName());
                	roleBean.setDescription(role.getDescription());
                	roles.add(roleBean);	
                }
                bean.setRoles(roles);

            	users.add(bean);	
            }
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return users;    
    }

    /**
     * Gets all roles available.
     */
    
    public List<InfoGluePrincipalBean> getPrincipalsWithRole(String roleName) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        List<InfoGluePrincipalBean> users = new ArrayList<InfoGluePrincipalBean>();
        
        logger.info("***************************************");
        logger.info("Getting all principals through webservice..");
        logger.info("***************************************");
        
        try
        {
            List principalList = RoleControllerProxy.getController().getInfoGluePrincipals(roleName);

            Iterator principalListIterator = principalList.iterator();
            while(principalListIterator.hasNext())
            {
            	InfoGluePrincipal principal = (InfoGluePrincipal)principalListIterator.next();
            	InfoGluePrincipalBean bean = new InfoGluePrincipalBean();
            	bean.setName(principal.getName());
            	bean.setDisplayName(principal.getDisplayName());
            	bean.setEmail(principal.getEmail());
            	bean.setFirstName(principal.getFirstName());
            	bean.setLastName(principal.getLastName());
            	bean.setAdministrator(false);
            	bean.setMetaInformation(principal.getMetaInformation());
            	
            	List groups = new ArrayList();
            	Iterator groupsListIterator = principal.getGroups().iterator();
                while(groupsListIterator.hasNext())
                {
                	InfoGlueGroup group = (InfoGlueGroup)groupsListIterator.next();
                	InfoGlueGroupBean groupBean = new InfoGlueGroupBean();
                	groupBean.setName(group.getName());
                	groupBean.setDisplayName(group.getDisplayName());
                	groupBean.setDescription(group.getDescription());
                	groups.add(groupBean);	
                }
                bean.setGroups(groups);

            	List roles = new ArrayList();
            	Iterator rolesListIterator = principal.getRoles().iterator();
                while(rolesListIterator.hasNext())
                {
                	InfoGlueRole role = (InfoGlueRole)rolesListIterator.next();
                	InfoGlueRoleBean roleBean = new InfoGlueRoleBean();
                	roleBean.setName(role.getName());
                	roleBean.setDisplayName(role.getDisplayName());
                	roleBean.setDescription(role.getDescription());
                	roles.add(roleBean);	
                }
                bean.setRoles(roles);

            	users.add(bean);	
            }
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return users;    
    }

    /**
     * Gets all roles available.
     */
    
    public List<InfoGluePrincipalBean> getPrincipalsWithGroup(String groupName) 
    {
        if(!ServerNodeController.getController().getIsIPAllowed(getRequest()))
        {
            logger.error("A client with IP " + getRequest().getRemoteAddr() + " was denied access to the webservice. Could be a hack attempt or you have just not configured the allowed IP-addresses correct.");
            return null;
        }

        List<InfoGluePrincipalBean> users = new ArrayList<InfoGluePrincipalBean>();
        
        logger.info("***************************************");
        logger.info("Getting all principals through webservice..");
        logger.info("***************************************");
        
        try
        {
            List principalList = GroupControllerProxy.getController().getInfoGluePrincipals(groupName);

            Iterator principalListIterator = principalList.iterator();
            while(principalListIterator.hasNext())
            {
            	InfoGluePrincipal principal = (InfoGluePrincipal)principalListIterator.next();
            	InfoGluePrincipalBean bean = new InfoGluePrincipalBean();
            	bean.setName(principal.getName());
            	bean.setDisplayName(principal.getDisplayName());
            	bean.setEmail(principal.getEmail());
            	bean.setFirstName(principal.getFirstName());
            	bean.setLastName(principal.getLastName());
            	bean.setAdministrator(false);
            	bean.setMetaInformation(principal.getMetaInformation());
            	
            	List groups = new ArrayList();
            	Iterator groupsListIterator = principal.getGroups().iterator();
                while(groupsListIterator.hasNext())
                {
                	InfoGlueGroup group = (InfoGlueGroup)groupsListIterator.next();
                	InfoGlueGroupBean groupBean = new InfoGlueGroupBean();
                	groupBean.setName(group.getName());
                	groupBean.setDisplayName(group.getDisplayName());
                	groupBean.setDescription(group.getDescription());
                	groups.add(groupBean);	
                }
                bean.setGroups(groups);

            	List roles = new ArrayList();
            	Iterator rolesListIterator = principal.getRoles().iterator();
                while(rolesListIterator.hasNext())
                {
                	InfoGlueRole role = (InfoGlueRole)rolesListIterator.next();
                	InfoGlueRoleBean roleBean = new InfoGlueRoleBean();
                	roleBean.setName(role.getName());
                	roleBean.setDisplayName(role.getDisplayName());
                	roleBean.setDescription(role.getDescription());
                	roles.add(roleBean);	
                }
                bean.setRoles(roles);

            	users.add(bean);	
            }
        }
        catch(Exception e)
        {
        	logger.error("En error occurred when we tried to create a new contentVersion:" + e.getMessage(), e);
        }
        
        return users;    
    }

	/**
	 * Checks if the principal exists and if the principal is allowed to create the workflow.
	 * 
	 * @param userName the name of the user.
	 * @param workflowName the name of the workflow to create.
	 * @throws SystemException if the principal doesn't exists or doesn't have permission to create the workflow.
	 */
	private void initializePrincipal(final String userName) throws SystemException 
	{
		try 
		{
			principal = UserControllerProxy.getController().getUser(userName);
		}
		catch(SystemException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new SystemException(e);
		}
		if(principal == null) 
		{
			throw new SystemException("No such principal [" + userName + "].");
		}
	}

}
