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

import java.util.Iterator;
import java.util.List;
import java.util.Collection;

import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;

import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.kernel.*;
import org.infoglue.cms.entities.management.impl.simple.*;
import org.infoglue.cms.entities.structure.*;
import org.infoglue.cms.entities.structure.ServiceBindingVO;
import org.infoglue.cms.entities.structure.impl.simple.*;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.CmsLogger;

/**
 * @author ss
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ServiceBindingController extends BaseController 
{

    public static ServiceBindingController getController()
    {
        return new ServiceBindingController();
    }
    
    public static ServiceBindingVO getServiceBindingVOWithId(Integer serviceBindingId) throws SystemException, Bug
    {
		return (ServiceBindingVO) getVOWithId(ServiceBindingImpl.class, serviceBindingId);
    }

	/*
    public static ServiceBinding getServiceBindingWithId(Integer serviceBindingId) throws SystemException, Bug
    {
		return (ServiceBinding) getObjectWithId(ServiceBindingImpl.class, serviceBindingId);
    }
	*/
	
    public static ServiceBinding getServiceBindingWithId(Integer serviceBindingId, Database db) throws SystemException, Bug
    {
		return (ServiceBinding) getObjectWithId(ServiceBindingImpl.class, serviceBindingId, db);
    }

    public List getServiceBindingVOList() throws SystemException, Bug
    {
        return getAllVOObjects(ServiceBindingImpl.class, "serviceBindingId");
    }

    public static ServiceBindingVO create(ServiceBindingVO serviceBindingVO, String qualifyerXML, Integer availableServiceBindingId, Integer siteNodeVersionId, Integer serviceDefinitionId) throws ConstraintException, SystemException
    {
    	CmsLogger.logInfo("Creating a serviceBinding with the following...");

    	CmsLogger.logInfo("name:" + serviceBindingVO.getName());
    	CmsLogger.logInfo("bindingTypeId:" + serviceBindingVO.getBindingTypeId());
    	CmsLogger.logInfo("availableServiceBindingId:" + availableServiceBindingId);
    	CmsLogger.logInfo("siteNodeVersionId:" + siteNodeVersionId);
    	CmsLogger.logInfo("serviceDefinitionId:" + serviceDefinitionId);

    	Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
		
		ServiceBinding serviceBinding = null;
		
        beginTransaction(db);

        try
        { 
	        SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(siteNodeVersionId, db);
	        serviceBinding = new ServiceBindingImpl();
	        serviceBinding.setValueObject(serviceBindingVO);
	        serviceBinding.setAvailableServiceBinding((AvailableServiceBindingImpl)AvailableServiceBindingController.getController().getAvailableServiceBindingWithId(availableServiceBindingId, db));
	        serviceBinding.setServiceDefinition((ServiceDefinitionImpl)ServiceDefinitionController.getController().getServiceDefinitionWithId(serviceDefinitionId, db));
	        serviceBinding.setSiteNodeVersion((SiteNodeVersionImpl)siteNodeVersion);
			
			//siteNodeVersion.getServiceBindings().add(serviceBinding);
			
	        CmsLogger.logInfo("createEntity: " + serviceBinding.getSiteNodeVersion().getSiteNodeVersionId());
	                    
            serviceBinding.setBindingQualifyers(QualifyerController.createQualifyers(qualifyerXML, serviceBinding));
	        db.create(serviceBinding);
            
			siteNodeVersion.getServiceBindings().add(serviceBinding);
			
            commitTransaction(db);
        }
        catch(Exception e)
        {
            CmsLogger.logSevere("An error occurred so we should not complete the transaction:" + e, e);
            e.printStackTrace();
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return serviceBinding.getValueObject();
    }      

	/**
	 * This is a method that lets you create a new service binding within a transaction.
	 */
	
	public ServiceBindingVO create(Database db, ServiceBindingVO serviceBindingVO, String qualifyerXML, Integer availableServiceBindingId, Integer siteNodeVersionId, Integer serviceDefinitionId) throws ConstraintException, SystemException
	{
		CmsLogger.logInfo("Creating a serviceBinding with the following...");

		CmsLogger.logInfo("name:" + serviceBindingVO.getName());
		CmsLogger.logInfo("bindingTypeId:" + serviceBindingVO.getBindingTypeId());
		CmsLogger.logInfo("availableServiceBindingId:" + availableServiceBindingId);
		CmsLogger.logInfo("siteNodeVersionId:" + siteNodeVersionId);
		CmsLogger.logInfo("serviceDefinitionId:" + serviceDefinitionId);

		ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
		
		ServiceBinding serviceBinding = null;
		
		try
		{ 
			SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(siteNodeVersionId, db);
			serviceBinding = new ServiceBindingImpl();
			serviceBinding.setValueObject(serviceBindingVO);
			serviceBinding.setAvailableServiceBinding((AvailableServiceBindingImpl)AvailableServiceBindingController.getController().getAvailableServiceBindingWithId(availableServiceBindingId, db));
			serviceBinding.setServiceDefinition((ServiceDefinitionImpl)ServiceDefinitionController.getController().getServiceDefinitionWithId(serviceDefinitionId, db));
			serviceBinding.setSiteNodeVersion((SiteNodeVersionImpl)siteNodeVersion);
			
			//siteNodeVersion.getServiceBindings().add(serviceBinding);
			
			CmsLogger.logInfo("createEntity: " + serviceBinding.getSiteNodeVersion().getSiteNodeVersionId());
	                    
			serviceBinding.setBindingQualifyers(QualifyerController.createQualifyers(qualifyerXML, serviceBinding));
			db.create((ServiceBinding)serviceBinding);
			
			siteNodeVersion.getServiceBindings().add(serviceBinding);
		}
		catch(Exception e)
		{
			CmsLogger.logSevere("An error occurred so we should not complete the transaction:" + e, e);
			e.printStackTrace();
			throw new SystemException(e.getMessage());
		}

		return serviceBinding.getValueObject();
	}      


    protected static ServiceBinding create(ServiceBindingVO serviceBindingVO, Integer availableServiceBindingId, Integer siteNodeVersionId, Integer serviceDefinitionId, Database db) throws ConstraintException, SystemException, Exception
    {
    	CmsLogger.logInfo("Creating a serviceBinding with the following...");

    	CmsLogger.logInfo("name:" + serviceBindingVO.getName());
    	CmsLogger.logInfo("bindingTypeId:" + serviceBindingVO.getBindingTypeId());
    	CmsLogger.logInfo("availableServiceBindingId:" + availableServiceBindingId);
    	CmsLogger.logInfo("siteNodeVersionId:" + siteNodeVersionId);
    	CmsLogger.logInfo("serviceDefinitionId:" + serviceDefinitionId);

		ServiceBinding serviceBinding = null;
		
        serviceBinding = new ServiceBindingImpl();
        serviceBinding.setValueObject(serviceBindingVO);
        serviceBinding.setAvailableServiceBinding((AvailableServiceBindingImpl)AvailableServiceBindingController.getController().getAvailableServiceBindingWithId(availableServiceBindingId, db));
        serviceBinding.setServiceDefinition((ServiceDefinitionImpl)ServiceDefinitionController.getController().getServiceDefinitionWithId(serviceDefinitionId, db));
        serviceBinding.setSiteNodeVersion((SiteNodeVersionImpl)SiteNodeVersionController.getController().getSiteNodeVersionWithId(siteNodeVersionId, db));

        CmsLogger.logInfo("createEntity: " + serviceBinding.getSiteNodeVersion().getSiteNodeVersionId());
        
        db.create(serviceBinding);
            
        return serviceBinding;
    }      



    public static ServiceBindingVO update(ServiceBindingVO serviceBindingVO, String qualifyerXML) throws ConstraintException, SystemException
    {
    	CmsLogger.logInfo("Updating a serviceBinding with the following...");

    	CmsLogger.logInfo("name:" + serviceBindingVO.getName());
    	CmsLogger.logInfo("bindingTypeId:" + serviceBindingVO.getBindingTypeId());
    	
    	Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
		
		ServiceBinding serviceBinding = null;
		
        beginTransaction(db);

        try
        { 
	        serviceBinding = getServiceBindingWithId(serviceBindingVO.getServiceBindingId(), db);
	        serviceBinding.setPath(serviceBindingVO.getPath());
	        serviceBinding.getBindingQualifyers().clear();
	        Collection newQualifyers = QualifyerController.createQualifyers(qualifyerXML, serviceBinding);
            serviceBinding.setBindingQualifyers(newQualifyers);
            commitTransaction(db);
        }
        catch(Exception e)
        {
            CmsLogger.logSevere("An error occurred so we should not complete the transaction:" + e, e);
            e.printStackTrace();
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return serviceBinding.getValueObject();
    }      


	/**
	 * This method deletes all service bindings pointing to a content.
	 */

	public static void deleteServiceBindingsReferencingContent(Content content, Database db) throws ConstraintException, SystemException, Exception
	{		
		OQLQuery oql = db.getOQLQuery( "SELECT sb FROM org.infoglue.cms.entities.structure.impl.simple.ServiceBindingImpl sb WHERE sb.bindingQualifyers.name = $1 AND sb.bindingQualifyers.value = $2 ORDER BY sb.serviceBindingId");
		oql.bind("contentId");
		oql.bind(content.getContentId().toString());
		
		QueryResults results = oql.execute();
		while(results.hasMore()) 
		{
			ServiceBinding serviceBinding = (ServiceBindingImpl)results.next();
			//CmsLogger.logInfo("serviceBinding:" + serviceBinding.getServiceBindingId());
			Collection qualifyers = serviceBinding.getBindingQualifyers();
			Iterator qualifyersIterator = qualifyers.iterator();
			while(qualifyersIterator.hasNext())
			{	
				Qualifyer qualifyer = (Qualifyer)qualifyersIterator.next();
				//CmsLogger.logInfo("qualifyer:" + qualifyer.getName() + ":" + qualifyer.getValue() + " == " + qualifyer.getValue().equals(content.getContentId().toString()));
				if(qualifyer.getName().equalsIgnoreCase("contentId") && qualifyer.getValue().equals(content.getContentId().toString()))
				{
					//db.remove(qualifyer);
					qualifyersIterator.remove();
					//CmsLogger.logInfo("Qualifyers:" + serviceBinding.getBindingQualifyers().size());
					serviceBinding.getBindingQualifyers().remove(qualifyer);

					//CmsLogger.logInfo("Qualifyers2:" + serviceBinding.getBindingQualifyers().size());
					if(serviceBinding.getBindingQualifyers() == null || serviceBinding.getBindingQualifyers().size() == 0)
					{
						//CmsLogger.logInfo("Removing service binding...");
						db.remove(serviceBinding);
					}
				}
			}
		}			
	}       
	
	
	/**
	 * This method deletes all service bindings pointing to a content.
	 */

	public static void deleteServiceBindingsReferencingSiteNode(SiteNode siteNode, Database db) throws ConstraintException, SystemException, Exception
	{		
		OQLQuery oql = db.getOQLQuery( "SELECT sb FROM org.infoglue.cms.entities.structure.impl.simple.ServiceBindingImpl sb WHERE sb.bindingQualifyers.name = $1 AND sb.bindingQualifyers.value = $2 ORDER BY sb.serviceBindingId");
		oql.bind("siteNodeId");
		oql.bind(siteNode.getSiteNodeId().toString());
		
		QueryResults results = oql.execute();
		while(results.hasMore()) 
		{
			ServiceBinding serviceBinding = (ServiceBindingImpl)results.next();
			//CmsLogger.logInfo("serviceBinding:" + serviceBinding.getServiceBindingId());
			Collection qualifyers = serviceBinding.getBindingQualifyers();
			Iterator qualifyersIterator = qualifyers.iterator();
			while(qualifyersIterator.hasNext())
			{	
				Qualifyer qualifyer = (Qualifyer)qualifyersIterator.next();
				//CmsLogger.logInfo("qualifyer:" + qualifyer.getName() + ":" + qualifyer.getValue() + " == " + qualifyer.getValue().equals(content.getContentId().toString()));
				if(qualifyer.getName().equalsIgnoreCase("siteNodeId") && qualifyer.getValue().equals(siteNode.getSiteNodeId().toString()))
				{
					//db.remove(qualifyer);
					qualifyersIterator.remove();
					//CmsLogger.logInfo("Qualifyers:" + serviceBinding.getBindingQualifyers().size());
					serviceBinding.getBindingQualifyers().remove(qualifyer);

					//CmsLogger.logInfo("Qualifyers2:" + serviceBinding.getBindingQualifyers().size());
					if(serviceBinding.getBindingQualifyers() == null || serviceBinding.getBindingQualifyers().size() == 0)
					{
						//CmsLogger.logInfo("Removing service binding...");
						db.remove(serviceBinding);
					}
				}
			}
		}			
	}       
	
	/**
	 * This method deletes a service binding an all associated qualifyers.
	 */
	
    public static void delete(ServiceBindingVO serviceBindingVO) throws ConstraintException, SystemException
    {
    	Database db = CastorDatabaseService.getDatabase();
    	beginTransaction(db);
    	try
        {	
        	ServiceBinding serviceBinding = ServiceBindingController.getServiceBindingWithId(serviceBindingVO.getServiceBindingId(), db);
			//QualifyerController.deleteQualifyersForServiceBinding(serviceBinding, db);
			//deleteEntity(ServiceBindingImpl.class, serviceBindingVO.getServiceBindingId(), db);
        	db.remove(serviceBinding);
        	commitTransaction(db);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        	rollbackTransaction(db);
        	throw new SystemException("An error occurred when we tried to remove a serviceBinding and it's qualifyers.");
        }
    }       
    
	/**
	 * This method deletes a service binding an all associated qualifyers.
	 */
	
	public static void delete(ServiceBindingVO serviceBindingVO, Database db) throws ConstraintException, SystemException, Exception
	{
		ServiceBinding serviceBinding = ServiceBindingController.getServiceBindingWithId(serviceBindingVO.getServiceBindingId(), db);
		db.remove(serviceBinding);
	}        

    public static ServiceBindingVO update(ServiceBindingVO serviceBindingVO) throws ConstraintException, SystemException
    {
    	return (ServiceBindingVO) updateEntity(ServiceBindingImpl.class, serviceBindingVO);
    }        

	/**
	 * This method returns a list with QualifyerVO-objects which are available for the
	 * serviceBinding sent in
	 */
	
	public static List getQualifyerVOList(Integer serviceBindingId) throws ConstraintException, SystemException
	{
		Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        List qualifyerVOList = null;

        beginTransaction(db);

        try
        {
        	ServiceBinding serviceBinding = getServiceBindingWithId(serviceBindingId, db);
            Collection qualifyerList = serviceBinding.getBindingQualifyers();
        	qualifyerVOList = toVOList(qualifyerList);
        	
            //If any of the validations or setMethods reported an error, we throw them up now before create.
            ceb.throwIfNotEmpty();
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
            CmsLogger.logWarning("An error occurred so we should not complete the transaction:" + ce, ce);
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
            CmsLogger.logSevere("An error occurred so we should not complete the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return qualifyerVOList;
	}

	/**
	 * This is a method that gives the user back an newly initialized ValueObject for this entity that the controller
	 * is handling.
	 */

	public BaseEntityVO getNewVO()
	{
		return new ServiceBindingVO();
	}

}
