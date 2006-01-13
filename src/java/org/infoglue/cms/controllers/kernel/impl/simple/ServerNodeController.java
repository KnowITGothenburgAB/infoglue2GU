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

package org.infoglue.cms.controllers.kernel.impl.simple;

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.RedirectVO;
import org.infoglue.cms.entities.management.ServerNodeVO;
import org.infoglue.cms.entities.management.ServerNode;
import org.infoglue.cms.entities.management.impl.simple.RedirectImpl;
import org.infoglue.cms.entities.management.impl.simple.ServerNodeImpl;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.structure.SiteNodeVO;

import org.infoglue.cms.exception.*;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.*;

import org.infoglue.deliver.util.CacheController;

import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServerNodeController extends BaseController
{ 
    private String useUpdateSecurity = CmsPropertyHandler.getProperty("useUpdateSecurity");
    
	/**
	 * Factory method
	 */

	public static ServerNodeController getController()
	{
		return new ServerNodeController();
	}
	
	public void initialize()
	{
	}
	
	/**
	 * This method creates a serverNode
	 * 
	 * @param vo
	 * @return
	 * @throws ConstraintException
	 * @throws SystemException
	 */
    public ServerNodeVO create(ServerNodeVO vo) throws ConstraintException, SystemException
    {
        ServerNode ent = new ServerNodeImpl();
        ent.setValueObject(vo);
        ent = (ServerNode) createEntity(ent);
        return ent.getValueObject();
    }     
    
    public ServerNodeVO update(ServerNodeVO vo) throws ConstraintException, SystemException
    {
    	return (ServerNodeVO) updateEntity(ServerNodeImpl.class, (BaseEntityVO) vo);
    }        
        
	// Singe object
    public ServerNode getServerNodeWithId(Integer id, Database db) throws SystemException, Bug
    {
		return (ServerNode) getObjectWithId(ServerNodeImpl.class, id, db);
    }

    public ServerNodeVO getServerNodeVOWithId(Integer serverNodeId) throws ConstraintException, SystemException, Bug
    {
		return  (ServerNodeVO) getVOWithId(ServerNodeImpl.class, serverNodeId);        
    }
	
    
	/**
	 * Returns the ServerNodeVO with the given name.
	 * 
	 * @param name
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */
	
	public ServerNodeVO getServerNodeVOWithName(String name) throws SystemException, Bug
	{
		ServerNodeVO serverNodeVO = null;
		
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			ServerNode serverNode = getServerNodeWithName(name, db);
			if(serverNode != null)
				serverNodeVO = serverNode.getValueObject();
			
			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			getLogger().info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
		
		return serverNodeVO;	
	}
	
	/**
	 * Returns the ServerNode with the given name fetched within a given transaction.
	 * 
	 * @param name
	 * @param db
	 * @return
	 * @throws SystemException
	 * @throws Bug
	 */

	public ServerNode getServerNodeWithName(String name, Database db) throws SystemException, Bug
	{
		ServerNode serverNode = null;
		
		try
		{
			OQLQuery oql = db.getOQLQuery("SELECT f FROM org.infoglue.cms.entities.management.impl.simple.ServerNodeImpl f WHERE f.name = $1");
			oql.bind(name);
			
			QueryResults results = oql.execute();
			this.getLogger().info("Fetching entity in read/write mode" + name);

			if (results.hasMore()) 
			{
				serverNode = (ServerNode)results.next();
			}
		}
		catch(Exception e)
		{
			throw new SystemException("An error occurred when we tried to fetch a named serverNode. Reason:" + e.getMessage(), e);    
		}
		
		return serverNode;		
	}

	/**
	 * This method can be used by actions and use-case-controllers that only need to have simple access to the
	 * functionality. They don't get the transaction-safety but probably just wants to show the info.
	 */	
    
    public List getServerNodeVOList() throws ConstraintException, SystemException, Bug
    {   
		/*
        String key = "serverNodeVOList";
		getLogger().info("key:" + key);
		List cachedServerNodeVOList = (List)CacheController.getCachedObject("serverNodeCache", key);
		if(cachedServerNodeVOList != null)
		{
			getLogger().info("There was an cached authorization:" + cachedServerNodeVOList.size());
			return cachedServerNodeVOList;
		}
		*/
        
		List serverNodeVOList = getAllVOObjects(ServerNodeImpl.class, "serverNodeId");

		//CacheController.cacheObject("serverNodeCache", key, serverNodeVOList);
			
		return serverNodeVOList;
    }
	

    public void delete(ServerNodeVO serverNodeVO) throws ConstraintException, SystemException
    {
    	deleteEntity(ServerNodeImpl.class, serverNodeVO.getId());
    }
	
	
	public List getAllowedAdminIPList()
	{
	    Map args = new HashMap();
	    args.put("globalKey", "infoglue");
	    PropertySet ps = PropertySetManager.getInstance("jdbc", args);
	    
	    String allowedAdminIP = ps.getString("allowedAdminIP");
	    if(allowedAdminIP != null)
	        return Arrays.asList(allowedAdminIP.split(","));
	    else
	        return new ArrayList();
	}
	
	/**
	 * This method return if the caller has access to the semi admin services.
	 * @param request
	 * @return
	 */

	public boolean getIsIPAllowed(HttpServletRequest request)
	{
	    boolean isIPAllowed = false;

	    //System.out.println("useUpdateSecurity:" + useUpdateSecurity);
	    if(useUpdateSecurity != null && useUpdateSecurity.equals("true"))
	    {
		    String remoteIP = request.getRemoteAddr();
		    //System.out.println("remoteIP:" + remoteIP);
		    if(remoteIP.equals("127.0.0.1"))
		    {
		        isIPAllowed = true;
		    }
		    else
		    {
		        List allowedAdminIPList = ServerNodeController.getController().getAllowedAdminIPList();
		        Iterator i = allowedAdminIPList.iterator();
		        while(i.hasNext())
		        {
		            String allowedIP = (String)i.next();
		            if(!allowedIP.trim().equals(""))
		            {
			            //System.out.println("allowedIP:" + allowedIP);
			            int index = allowedIP.indexOf(".*");
			            if(index > -1)
			                allowedIP = allowedIP.substring(0, index);
						//System.out.println("allowedIP:" + allowedIP);
				            
			            if(remoteIP.startsWith(allowedIP))
			            {
			                isIPAllowed = true;
			                break;
			            }
		            }
		        }
		    }
	    }
	    else
	        isIPAllowed = true;
	    
	    return isIPAllowed;
	}
	
	public String getAllowedAdminIP()
	{
	    Map args = new HashMap();
	    args.put("globalKey", "infoglue");
	    PropertySet ps = PropertySetManager.getInstance("jdbc", args);
	    
	    String allowedAdminIP = ps.getString("allowedAdminIP");

	    return allowedAdminIP;
	}

	public void setAllowedAdminIP(String allowedAdminIP)
	{
	    Map args = new HashMap();
	    args.put("globalKey", "infoglue");
	    PropertySet ps = PropertySetManager.getInstance("jdbc", args);
	    
	    ps.setString("allowedAdminIP", allowedAdminIP);
	}

	/**
	 * This is a method that gives the user back an newly initialized ValueObject for this entity that the controller
	 * is handling.
	 */

	public BaseEntityVO getNewVO()
	{
		return new ServerNodeVO();
	}

}
 
