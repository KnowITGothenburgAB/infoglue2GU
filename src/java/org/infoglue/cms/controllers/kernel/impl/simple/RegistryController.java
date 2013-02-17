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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.databeans.ReferenceBean;
import org.infoglue.cms.applications.databeans.ReferenceVersionBean;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.entities.management.ContentTypeDefinition;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Registry;
import org.infoglue.cms.entities.management.RegistryVO;
import org.infoglue.cms.entities.management.impl.simple.RegistryImpl;
import org.infoglue.cms.entities.structure.Qualifyer;
import org.infoglue.cms.entities.structure.ServiceBinding;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.SiteNodeVersionVO;
import org.infoglue.cms.entities.structure.impl.simple.SmallQualifyerImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallServiceBindingImpl;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.Timer;


/**
 * The RegistryController manages the registry-parts of InfoGlue. 
 * The Registry is metadata for how things are related - especially to handle bindings and inline links etc
 * when looking them up in the model is to slow.
 *
 * @author Mattias Bogeblad
 */

public class RegistryController extends BaseController
{
    private final static Logger logger = Logger.getLogger(RegistryController.class.getName());

	private static final RegistryController instance = new RegistryController();
	
	public static RegistryController getController()
	{ 
	    return instance; 
	}

	private RegistryController()
	{
	}
	
    public List getRegistryVOList() throws SystemException, Bug
    {
        return getAllVOObjects(RegistryImpl.class, "registryId");
    }

    public List getRegistryVOList(Database db) throws SystemException, Bug
    {
        return getAllVOObjects(RegistryImpl.class, "registryId", db);
    }
    
	/**
	 * This method return a RegistryVO
	 */
	
	public RegistryVO getRegistryVOWithId(Integer registryId) throws SystemException, Exception
	{
		RegistryVO registryVO = (RegistryVO)getVOWithId(RegistryImpl.class, registryId);

		return registryVO;
	}

	
	/**
	 * This method creates a registry entity in the db.
	 * @param valueObject
	 * @return
	 * @throws ConstraintException
	 * @throws SystemException
	 */
	
    public RegistryVO create(RegistryVO valueObject, Database db) throws ConstraintException, SystemException, Exception
    {
        Registry registry = new RegistryImpl();
        registry.setValueObject(valueObject);
        db.create(registry);
        return registry.getValueObject();
    }     

    /**
     * This method updates a registry entry
     * @param vo
     * @return
     * @throws ConstraintException
     * @throws SystemException
     */
    
    public RegistryVO update(RegistryVO valueObject, Database db) throws ConstraintException, SystemException
    {
    	return (RegistryVO) updateEntity(RegistryImpl.class, (BaseEntityVO) valueObject, db);
    }    
    
    
    /**
     * This method deletes a registry entry
     * @return registryId
     * @throws ConstraintException
     * @throws SystemException
     */
    
    public void delete(Integer registryId) throws ConstraintException, SystemException
    {
    	this.deleteEntity(RegistryImpl.class, registryId);
    }   
    
	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersionVO contentVersionVO) throws ConstraintException, SystemException
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
		
			ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
			updateContentVersion(contentVersion.getValueObject(), db);
		    
			commitTransaction(db);
		}
		catch (Exception e)		
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}
	}

	
	
	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersionVO contentVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    String versionValue = contentVersion.getVersionValue();

	    ContentVersionVO oldContentVersion = contentVersion; //ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
	    // OPTIMIZ
//	    Content oldContent = oldContentVersion.getOwningContent();
	    ContentVO oldContentVO = ContentController.getContentController().getSmallContentVOWithId(contentVersion.getContentId(), db);
	    ContentTypeDefinition ctd = ContentTypeDefinitionController.getController().getContentTypeDefinitionWithId(oldContentVO.getContentTypeDefinitionId(), db);
	    if(ctd != null && ctd.getName().equalsIgnoreCase("Meta info"))
	    {
	        logger.info("It was a meta info so lets check it for other stuff as well");

	        SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithMetaInfoContentId(db, oldContentVO.getContentId());
//	        SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersion(db, siteNodeVO.getId());
	        SiteNodeVersionVO siteNodeVersionVO = SiteNodeVersionController.getController().getLatestActiveSiteNodeVersionVO(db, siteNodeVO.getId());
	        //SiteNodeVersion siteNodeVersion = getLatestActiveSiteNodeVersionWhichUsesContentVersionAsMetaInfo(oldContentVersion, db);
		    if(siteNodeVersionVO != null)
		    {
		        logger.info("Going to use " + siteNodeVersionVO.getId() + " as reference");
		        clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersionVO.getId().toString());

			    getComponents(siteNodeVersionVO, versionValue, db);
			    getComponentBindings(siteNodeVersionVO, versionValue, db);
			    getPageBindings(siteNodeVersionVO, db);
		    }

		    getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }
	    else
	    {
	        clearRegistryVOList(ContentVersion.class.getName(), oldContentVersion.getContentVersionId().toString());
	        getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }
	}

	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo. All in a threaded matter.
	 * 
	 * @param contentVersionVO
	 * @param contentVersionVO
	 * @throws Exception
	 */
	
	private static List<Object[]> queuedContentVersions = new ArrayList<Object[]>();
	private static AtomicBoolean runningUpdateContentVersion = new AtomicBoolean();
	public void updateContentVersionThreaded(ContentVersionVO contentVersionVO, SiteNodeVersionVO siteNodeVersionVO) throws Exception
	{
		class UpdateContentVersionRunnable implements Runnable 
		{
			ContentVersionVO contentVersionVO;
			SiteNodeVersionVO siteNodeVersionVO;
			
			UpdateContentVersionRunnable(ContentVersionVO contentVersionVO, SiteNodeVersionVO siteNodeVersionVO) 
			{ 
				this.contentVersionVO = contentVersionVO; 
				this.siteNodeVersionVO = siteNodeVersionVO;
			}
	        
	        public void run() 
	        {
	        	Timer t = new Timer();
	    		
    			List<Object[]> localContentVersions = new ArrayList<Object[]>();
	        	synchronized (queuedContentVersions) 
	        	{
	        		localContentVersions.addAll(queuedContentVersions);
	        		queuedContentVersions.clear();
				}
	        	
				try
				{
					Database db = CastorDatabaseService.getDatabase();
					try 
					{
						beginTransaction(db);
						
						for(Object[] bean : localContentVersions)
						{
							updateContentVersion((ContentVersionVO)bean[0], (SiteNodeVersionVO)bean[1], db);
						}
						
						logger.info("Done refreshing registry took:" + t.getElapsedTime());
						
						commitTransaction(db);
					} 
					catch (Exception e) 
					{
						logger.error("Error precaching all access rights for this user: " + e.getMessage(), e);
						rollbackTransaction(db);
					}
				}
				catch (Exception e) 
				{
					logger.error("Could not start PreCacheTask:" + e.getMessage(), e);
				}
				finally
				{
					runningUpdateContentVersion.set(false);
				}
	        }
	    }
	
		synchronized (queuedContentVersions) 
		{
			queuedContentVersions.add(new Object[]{contentVersionVO, siteNodeVersionVO});
		}
	
		if(runningUpdateContentVersion.compareAndSet(false, true))
		{
		    Thread thread = new Thread(new UpdateContentVersionRunnable(contentVersionVO, siteNodeVersionVO));
		    thread.start();	
    	}
    	else
    	{
    		logger.warn("Running allready - queing");
    	}
	}
	
	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo.
	 * 
	 * @param contentVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateContentVersion(ContentVersionVO contentVersion, SiteNodeVersionVO siteNodeVersion, Database db) throws ConstraintException, SystemException, Exception
	{
	    String versionValue = contentVersion.getVersionValue();
	    
	   	ContentVersionVO oldContentVersion = contentVersion; //ContentVersionController.getContentVersionController().getContentVersionWithId(contentVersionVO.getContentVersionId(), db);
	   	//Content oldContent = oldContentVersion.getOwningContent();
	    ContentVO oldContentVO = ContentController.getContentController().getSmallContentVOWithId(contentVersion.getContentId(), db);
	    ContentTypeDefinitionVO ctd = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithId(oldContentVO.getContentTypeDefinitionId(), db);
 
	    if(ctd != null && ctd.getName().equalsIgnoreCase("Meta info"))
	    {
	        logger.info("It was a meta info so lets check it for other stuff as well");
		    
		    if(siteNodeVersion != null)
		    {
		        logger.info("Going to use " + siteNodeVersion.getId() + " as reference");
		    	clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersion.getId().toString());
			    getComponents(siteNodeVersion, versionValue, db);
			    getComponentBindings(siteNodeVersion, versionValue, db);
			    getPageBindings(siteNodeVersion, db);
		    }
	        
		    getInlineSiteNodes(oldContentVersion, versionValue, db);
		    getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }
	    else
	    {
	        clearRegistryVOList(ContentVersion.class.getName(), oldContentVersion.getContentVersionId().toString());
	        if(siteNodeVersion != null)
	        	getPageBindings(siteNodeVersion, db);
		    getInlineSiteNodes(oldContentVersion, versionValue, db);
	        getInlineContents(oldContentVersion, versionValue, db);
		    getRelationSiteNodes(oldContentVersion, versionValue, db);
		    getRelationContents(oldContentVersion, versionValue, db);
	    }		
	}

	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersion
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateSiteNodeVersion(SiteNodeVersionVO siteNodeVersionVO) throws ConstraintException, SystemException
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
		
			logger.info("Starting RegistryController.updateSiteNodeVersion...");
			SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(siteNodeVersionVO.getId(), db);
			logger.info("Before RegistryController.updateSiteNodeVersion...");
			updateSiteNodeVersion(siteNodeVersion.getValueObject(), db);
			logger.info("Before commit RegistryController.updateSiteNodeVersion...");
		    
			commitTransaction(db);
		}
		catch (Exception e)		
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}
	}

	
	/**
	 * this method goes through all inline stuff and all relations if ordinary content 
	 * and all components and bindings if a metainfo. All in a threaded matter.
	 * 
	 * @param contentVersionVO
	 * @param contentVersionVO
	 * @throws Exception
	 */
	private static List<SiteNodeVersionVO> queuedSiteNodeVersions = new ArrayList<SiteNodeVersionVO>();
	private static AtomicBoolean runningUpdateSiteNodeVersion = new AtomicBoolean();
	
	public void updateSiteNodeVersionThreaded(SiteNodeVersionVO siteNodeVersionVO) throws Exception
	{
		class UpdateSiteNodeVersionRunnable implements Runnable 
		{
			SiteNodeVersionVO siteNodeVersionVO;
			
			UpdateSiteNodeVersionRunnable(SiteNodeVersionVO siteNodeVersionVO) 
			{ 
				this.siteNodeVersionVO = siteNodeVersionVO;
			}
	        
	        public void run() 
	        {
	        	Timer t = new Timer();
	    		
	        	List<SiteNodeVersionVO> localContentVersions = new ArrayList<SiteNodeVersionVO>();
	        	synchronized (queuedSiteNodeVersions) 
	        	{
	        		localContentVersions.addAll(queuedSiteNodeVersions);
	        		queuedSiteNodeVersions.clear();
				}
	        	logger.warn("localContentVersions:" + localContentVersions.size());
	        	
				try
				{
					Database db = CastorDatabaseService.getDatabase();
					try 
					{
						beginTransaction(db);

						for(SiteNodeVersionVO siteNodeVersionVO : queuedSiteNodeVersions)
						{	
							updateSiteNodeVersion(siteNodeVersionVO, db);
						}

						logger.warn("Done refreshing page registry took:" + t.getElapsedTime());
						
						commitTransaction(db);
					} 
					catch (Exception e) 
					{
						logger.error("Error precaching all access rights for this user: " + e.getMessage(), e);
						rollbackTransaction(db);
					}
				}
				catch (Exception e) 
				{
					logger.error("Could not start PreCacheTask:" + e.getMessage(), e);
				}
				finally
				{
					runningUpdateSiteNodeVersion.set(false);
				}
	        }
	    }
		
		synchronized (queuedSiteNodeVersions) 
		{
			queuedSiteNodeVersions.add(siteNodeVersionVO);
		}
	
		if(runningUpdateSiteNodeVersion.compareAndSet(false, true))
		{
		    Thread thread = new Thread(new UpdateSiteNodeVersionRunnable(siteNodeVersionVO));
		    thread.start();	
    	}
    	else
    	{
    		logger.warn("Running allready - queing");
    	}
	}

	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersionVO
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void updateSiteNodeVersion(SiteNodeVersionVO siteNodeVersionVO, Database db) throws ConstraintException, SystemException, Exception
	{
	   	//SiteNodeVersion oldSiteNodeVersion = siteNodeVersion;
	    Integer siteNodeId = siteNodeVersionVO.getSiteNodeId();
	    //SiteNode oldSiteNode = oldSiteNodeVersion.getOwningSiteNode();
 
	    logger.info("Before clearing old registry...");
	    clearRegistryVOList(SiteNodeVersion.class.getName(), siteNodeVersionVO.getId().toString());
	    logger.info("After clearing old registry...");

		//Collection serviceBindings = siteNodeVersion.getServiceBindings();
	    List<SmallServiceBindingImpl> serviceBindings = ServiceBindingController.getController().getSmallServiceBindingsListForSiteNodeVersion(siteNodeVersionVO.getId(), db);
		Iterator<SmallServiceBindingImpl> serviceBindingIterator = serviceBindings.iterator();
		while(serviceBindingIterator.hasNext())
		{
			SmallServiceBindingImpl serviceBinding = serviceBindingIterator.next();
		    if(serviceBinding.getBindingQualifyers() != null)
		    {
			    @SuppressWarnings("rawtypes")
			    Iterator<SmallQualifyerImpl> qualifyersIterator = serviceBinding.getBindingQualifyers().iterator();
			    while(qualifyersIterator.hasNext())
			    {
			    	SmallQualifyerImpl qualifyer = (SmallQualifyerImpl)qualifyersIterator.next();
			        String name = qualifyer.getName();
			        String value = qualifyer.getValue();
	
	                try
	                {
				        RegistryVO registryVO = new RegistryVO();
				        registryVO.setReferenceType(RegistryVO.PAGE_BINDING);
			            if(name.equalsIgnoreCase("contentId"))
				        {
			            	// TODO REMOVE OPTMIZ!
//			                Content content = ContentController.getContentController().getContentWithId(new Integer(value), db);
			                ContentVO contentVO = ContentController.getContentController().getContentVOWithId(new Integer(value), db);
			            
			                registryVO.setEntityId(value);
				            registryVO.setEntityName(Content.class.getName());
				            
				            registryVO.setReferencingEntityId(siteNodeVersionVO.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId("" + siteNodeId);
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        
				            SiteNodeVO snVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
				            LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(snVO.getRepositoryId(), db);
				            ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getContentId(), masterLanguage.getId(), db);
				            getComponents(siteNodeVersionVO, contentVersionVO.getVersionValue(), db);
			                getComponentBindings(siteNodeVersionVO, contentVersionVO.getVersionValue(), db);
			            
				            /*
				            Collection contentVersions = content.getContentVersions();
				            Iterator contentVersionIterator = contentVersions.iterator();
				            while(contentVersionIterator.hasNext())
					        {
				                ContentVersion contentVersion = (ContentVersion)contentVersionIterator.next();
				                getComponents(siteNodeVersion, contentVersion.getVersionValue(), db);
				                getComponentBindings(siteNodeVersion, contentVersion.getVersionValue(), db);
				            }
				            */
				        }
			            else if(name.equalsIgnoreCase("siteNodeId"))
				        {
			                registryVO.setEntityId(value);
				            registryVO.setEntityName(SiteNode.class.getName());
				            
				            registryVO.setReferencingEntityId(siteNodeVersionVO.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId("" + siteNodeId);
						    registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        }
			            
			    	    logger.info("Before creating registry entry...");
			    	    
			            this.create(registryVO, db);
	                }
	                catch(Exception e)
	                {
	                	logger.error("Error when updating registries for SiteNodeVersion. Message: " + e.getMessage() + ". Type: " + e.getClass());
	                	logger.warn("Error when updating registries for SiteNodeVersion.", e);
	                }		        
			    }
		    }
		}
	}

	
	/**
	 * this method goes through all page bindings and makes registry entries for them
	 * 
	 * @param siteNodeVersion
	 * @throws SystemException
	 * @throws Exception
	 */
	
	public void getPageBindings(SiteNodeVersionVO siteNodeVersion, Database db) throws ConstraintException, SystemException, Exception
	{
//	    SiteNode oldSiteNode = siteNodeVersion.getOwningSiteNode();
	    
//		Collection serviceBindings = siteNodeVersion.getServiceBindings();
		List<SmallServiceBindingImpl> serviceBindings = ServiceBindingController.getController().getSmallServiceBindingsListForSiteNodeVersion(siteNodeVersion.getSiteNodeVersionId(), db);
		Iterator<SmallServiceBindingImpl> serviceBindingIterator = serviceBindings.iterator();
		while(serviceBindingIterator.hasNext())
		{
			SmallServiceBindingImpl serviceBinding = serviceBindingIterator.next();
		    if(serviceBinding.getBindingQualifyers() != null)
		    {
			    @SuppressWarnings("rawtypes")
			    Iterator qualifyersIterator = serviceBinding.getBindingQualifyers().iterator();
			    while(qualifyersIterator.hasNext())
			    {
			    	Qualifyer qualifyer = (Qualifyer)qualifyersIterator.next();
			        String name = qualifyer.getName();
			        String value = qualifyer.getValue();
	
	                try
	                {
				        RegistryVO registryVO = new RegistryVO();
				        registryVO.setReferenceType(RegistryVO.PAGE_BINDING);
			            if(name.equalsIgnoreCase("contentId"))
				        {
//			                Content content = ContentController.getContentController().getContentWithId(new Integer(value), db);
			            
			                registryVO.setEntityId(value);
				            registryVO.setEntityName(Content.class.getName());
				            
				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
//				            registryVO.setReferencingEntityCompletingId(oldSiteNode.getId().toString());
				            registryVO.setReferencingEntityCompletingId(siteNodeVersion.getSiteNodeId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        
				            /*
				            Collection contentVersions = content.getContentVersions();
				            Iterator contentVersionIterator = contentVersions.iterator();
				            while(contentVersionIterator.hasNext())
				            {
				                ContentVersion contentVersion = (ContentVersion)contentVersionIterator.next();
				                getComponents(siteNodeVersion, contentVersion.getVersionValue(), db);
				                getComponentBindings(siteNodeVersion, contentVersion.getVersionValue(), db);
				            }
				            */
				        }
			            else if(name.equalsIgnoreCase("siteNodeId"))
				        {
//			                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(value), db);
			                
			                registryVO.setEntityId(value);
				            registryVO.setEntityName(SiteNode.class.getName());
				            
				            registryVO.setReferencingEntityId(siteNodeVersion.getId().toString());
				            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
				            registryVO.setReferencingEntityCompletingId(siteNodeVersion.getSiteNodeId().toString());
				            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
				        }
			            
			    	    logger.info("Before creating registry entry...");

			            this.create(registryVO, db);
	                }
	                catch(Exception e)
	                {
	                    e.printStackTrace();
	                }		        
			    }
		    }
		}
	}

	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineSiteNodes(ContentVersionVO contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getPageUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("(");
	        int siteNodeEndIndex = match.indexOf(",");
	        if(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            String siteNodeIdString = match.substring(siteNodeStartIndex + 1, siteNodeEndIndex); 
	            try
	            {
		            if(siteNodeIdString.indexOf("templateLogic.siteNodeId") == -1)
		            {
		            	siteNodeId = new Integer(siteNodeIdString);
			            logger.info("siteNodeId:" + siteNodeId);
			            RegistryVO registryVO = new RegistryVO();
			            registryVO.setEntityId(siteNodeId.toString());
			            registryVO.setEntityName(SiteNode.class.getName());
			            registryVO.setReferenceType(RegistryVO.INLINE_LINK);
			            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
			            registryVO.setReferencingEntityName(ContentVersion.class.getName());
			            registryVO.setReferencingEntityCompletingId(contentVersion.getContentId().toString());
			            registryVO.setReferencingEntityCompletingName(Content.class.getName());
			            
			            this.create(registryVO, db);
		            }
	            }
	            catch(Exception e)
	            {
	                logger.warn("Tried to register inline sitenodes with exception as result:" + e.getMessage(), e);
	            }
	        }
	    }
	}
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineSiteNodes(String versionValue, Set<Integer> boundSiteNodeIds, Set<Integer> boundContentIds) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getPageUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("(");
	        int siteNodeEndIndex = match.indexOf(",");
	        if(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            String siteNodeIdString = match.substring(siteNodeStartIndex + 1, siteNodeEndIndex); 
	            try
	            {
		            if(siteNodeIdString.indexOf("templateLogic.siteNodeId") == -1)
		            {
		            	siteNodeId = new Integer(siteNodeIdString);
			            logger.info("siteNodeId:" + siteNodeId);
			            boundSiteNodeIds.add(siteNodeId);
		            }
	            }
	            catch(Exception e)
	            {
	                logger.warn("Tried to register inline sitenodes with exception as result:" + e.getMessage(), e);
	            }
	        }
	    }
	}

	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineContents(ContentVersionVO contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getInlineAssetUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("(");
	        int contentEndIndex = match.indexOf(",");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	        	String contentIdString = match.substring(contentStartIndex + 1, contentEndIndex);
	            if(contentIdString != null && !contentIdString.equals(""))
	            {
		        	contentId = new Integer(contentIdString);
		            logger.info("contentId:" + contentId);
		            
		            RegistryVO registryVO = new RegistryVO();
		            registryVO.setEntityId(contentId.toString());
		            registryVO.setEntityName(Content.class.getName());
		            registryVO.setReferenceType(RegistryVO.INLINE_ASSET);
		            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
		            registryVO.setReferencingEntityName(ContentVersion.class.getName());
		            registryVO.setReferencingEntityCompletingId(contentVersion.getContentId().toString());
		            registryVO.setReferencingEntityCompletingName(Content.class.getName());
		            
		            this.create(registryVO, db);
	            }
	        }
	    }
	}	

	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getInlineContents(String versionValue, Set<Integer> boundSiteNodeIds, Set<Integer> boundContentIds) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("\\$templateLogic\\.getInlineAssetUrl\\(.*?\\)");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("(");
	        int contentEndIndex = match.indexOf(",");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	        	String contentIdString = match.substring(contentStartIndex + 1, contentEndIndex);
	            if(contentIdString != null && !contentIdString.equals(""))
	            {
		        	contentId = new Integer(contentIdString);
		            logger.info("contentId:" + contentId);
		            boundContentIds.add(contentId);
	            }
	        }
	    }
	}	
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getRelationSiteNodes(ContentVersionVO contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("<qualifyer entity='SiteNode'>.*?</qualifyer>");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer siteNodeId;
	        
	        int siteNodeStartIndex = match.indexOf("<id>");
	        int siteNodeEndIndex = match.indexOf("</id>");
	        while(siteNodeStartIndex > 0 && siteNodeEndIndex > 0 && siteNodeEndIndex > siteNodeStartIndex)
	        {
	            siteNodeId = new Integer(match.substring(siteNodeStartIndex + 4, siteNodeEndIndex));
	            logger.info("siteNodeId:" + siteNodeId);
	            RegistryVO registryVO = new RegistryVO();
	            registryVO.setEntityId(siteNodeId.toString());
	            registryVO.setEntityName(SiteNode.class.getName());
	            registryVO.setReferenceType(RegistryVO.INLINE_SITE_NODE_RELATION);
	            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
	            registryVO.setReferencingEntityName(ContentVersion.class.getName());
	            registryVO.setReferencingEntityCompletingId(contentVersion.getContentId().toString());
	            registryVO.setReferencingEntityCompletingName(Content.class.getName());
	            
	            this.create(registryVO, db);
	            
	            siteNodeStartIndex = match.indexOf("<id>", siteNodeEndIndex);
		        siteNodeEndIndex = match.indexOf("</id>", siteNodeStartIndex);
	        }
	    }
	}
	
	/**
	 * This method fetches all inline links from any text.
	 */
	
	public void getRelationContents(ContentVersionVO contentVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    Pattern pattern = Pattern.compile("<qualifyer entity='Content'>.*?</qualifyer>");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("<id>");
	        int contentEndIndex = match.indexOf("</id>");
	        while(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 4, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            RegistryVO registryVO = new RegistryVO();
	            registryVO.setEntityId(contentId.toString());
	            registryVO.setEntityName(Content.class.getName());
	            registryVO.setReferenceType(RegistryVO.INLINE_CONTENT_RELATION);
	            registryVO.setReferencingEntityId(contentVersion.getContentVersionId().toString());
	            registryVO.setReferencingEntityName(ContentVersion.class.getName());
	            registryVO.setReferencingEntityCompletingId(contentVersion.getContentId().toString());
	            registryVO.setReferencingEntityCompletingName(Content.class.getName());
	            
	            this.create(registryVO, db);
	            
	            contentStartIndex = match.indexOf("<id>", contentEndIndex);
	            contentEndIndex = match.indexOf("</id>", contentStartIndex);
	        }
	    }
	}
	                
	
	/**
	 * This method fetches all components and adds entries to the registry.
	 */
	
	public void getComponents(SiteNodeVersionVO siteNodeVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{
	    List<Integer> foundComponents = new ArrayList<Integer>();
	    
	    Pattern pattern = Pattern.compile("contentId=\".*?\"");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("\"");
	        int contentEndIndex = match.lastIndexOf("\"");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 1, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            if(!foundComponents.contains(contentId))
	            {
		            RegistryVO registryVO = new RegistryVO();
		            registryVO.setEntityId(contentId.toString());
		            registryVO.setEntityName(Content.class.getName());
		            registryVO.setReferenceType(RegistryVO.PAGE_COMPONENT);
		            registryVO.setReferencingEntityId(siteNodeVersion.getSiteNodeVersionId().toString());
		            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
		            registryVO.setReferencingEntityCompletingId("" + siteNodeVersion.getSiteNodeId());
		            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
		            
		            this.create(registryVO, db);
		            
		            foundComponents.add(contentId);
	            }
	        }
	    }
	}
	
	/**
	 * This method fetches all components and adds entries to the registry.
	 */
	
	public Set<Integer> getComponents(String versionValue) throws Exception
	{
	    Set foundComponents = new HashSet();
	    
	    Pattern pattern = Pattern.compile("contentId=\".*?\"");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        Integer contentId;
	        
	        int contentStartIndex = match.indexOf("\"");
	        int contentEndIndex = match.lastIndexOf("\"");
	        if(contentStartIndex > 0 && contentEndIndex > 0 && contentEndIndex > contentStartIndex)
	        {
	            contentId = new Integer(match.substring(contentStartIndex + 1, contentEndIndex));
	            logger.info("contentId:" + contentId);
	            
	            foundComponents.add(contentId);
	        }
	    }
	    
	    return foundComponents;
	}

	/**
	 * This method fetches all components and adds entries to the registry.
	 */

	public void getComponentBindings(SiteNodeVersionVO siteNodeVersion, String versionValue, Database db) throws ConstraintException, SystemException, Exception
	{		    
		List<String> foundComponents = new ArrayList<String>();

	    Pattern pattern = Pattern.compile("<binding.*?entity=\".*?\" entityId=\".*?\">");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        String entityName;
	        String entityId;
	        
	        int entityNameStartIndex = match.indexOf("entity=\"");
	        int entityNameEndIndex = match.indexOf("\"", entityNameStartIndex + 8);
	        logger.info("entityNameStartIndex:" + entityNameStartIndex);
	        logger.info("entityNameEndIndex:" + entityNameEndIndex);
	        if(entityNameStartIndex > 0 && entityNameEndIndex > 0 && entityNameEndIndex > entityNameStartIndex)
	        {
	            entityName = match.substring(entityNameStartIndex + 8, entityNameEndIndex);
	            logger.info("entityName:" + entityName);

		        int entityIdStartIndex = match.indexOf("entityId=\"", entityNameEndIndex + 1);
		        int entityIdEndIndex = match.indexOf("\"", entityIdStartIndex + 10);
		        logger.info("entityIdStartIndex:" + entityIdStartIndex);
		        logger.info("entityIdEndIndex:" + entityIdEndIndex);
		        if(entityIdStartIndex > 0 && entityIdEndIndex > 0 && entityIdEndIndex > entityIdStartIndex)
		        {
		            entityId = match.substring(entityIdStartIndex + 10, entityIdEndIndex);
		            logger.info("entityId:" + entityId);

		            String key = entityName + ":" + entityId;
		            if(!foundComponents.contains(key))
		            {	        
			            RegistryVO registryVO = new RegistryVO();
			            if(entityName.indexOf("Content") > -1)
			                registryVO.setEntityName(Content.class.getName());
			            else
			                registryVO.setEntityName(SiteNode.class.getName());
			                
			            registryVO.setEntityId(entityId);
			            registryVO.setReferenceType(RegistryVO.PAGE_COMPONENT_BINDING);
			            registryVO.setReferencingEntityId(siteNodeVersion.getSiteNodeVersionId().toString());
			            registryVO.setReferencingEntityName(SiteNodeVersion.class.getName());
			            registryVO.setReferencingEntityCompletingId(siteNodeVersion.getSiteNodeId().toString());
			            registryVO.setReferencingEntityCompletingName(SiteNode.class.getName());
			            
			            this.create(registryVO, db);

			            foundComponents.add(key);
		            }
		        }
	        }
	    }
	}
	
	/**
	 * This method fetches all components and adds entries to the registry.
	 */

	public void getComponentBindings(String versionValue, Set<Integer> boundSiteNodeIds, Set<Integer> boundContentIds) throws Exception
	{
	    Pattern pattern = Pattern.compile("<binding.*?entity=\".*?\" entityId=\".*?\">");
	    Matcher matcher = pattern.matcher(versionValue);
	    while ( matcher.find() ) 
	    { 
	        String match = matcher.group();
	        logger.info("Adding match to registry after some processing: " + match);
	        String entityName;
	        String entityId;
	        
	        int entityNameStartIndex = match.indexOf("entity=\"");
	        int entityNameEndIndex = match.indexOf("\"", entityNameStartIndex + 8);
	        logger.info("entityNameStartIndex:" + entityNameStartIndex);
	        logger.info("entityNameEndIndex:" + entityNameEndIndex);
	        if(entityNameStartIndex > 0 && entityNameEndIndex > 0 && entityNameEndIndex > entityNameStartIndex)
	        {
	            entityName = match.substring(entityNameStartIndex + 8, entityNameEndIndex);
	            logger.info("entityName:" + entityName);

		        int entityIdStartIndex = match.indexOf("entityId=\"", entityNameEndIndex + 1);
		        int entityIdEndIndex = match.indexOf("\"", entityIdStartIndex + 10);
		        logger.info("entityIdStartIndex:" + entityIdStartIndex);
		        logger.info("entityIdEndIndex:" + entityIdEndIndex);
		        if(entityIdStartIndex > 0 && entityIdEndIndex > 0 && entityIdEndIndex > entityIdStartIndex)
		        {
		            entityId = match.substring(entityIdStartIndex + 10, entityIdEndIndex);
		            logger.info("entityId:" + entityId);

		            if(entityName.indexOf("Content") > -1)
		            	boundContentIds.add(new Integer(entityId));
		            else
		            	boundSiteNodeIds.add(new Integer(entityId));
		        }
	        }
	    }
	}

	/**
	 * Implemented for BaseController
	 */
	public BaseEntityVO getNewVO()
	{
		return new CategoryVO();
	}

    /**
     * This method gets all referencing content versions
     * 
     * @param contentId
     * @return
     */
	/*
    public List getReferencingObjectsForContent(Integer contentId) throws SystemException
    {
        List referenceBeanList = new ArrayList();
        
		Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			Map entries = new HashMap();
			
	        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), db);
	        Iterator registryEntiresIterator = registryEntires.iterator();
	        while(registryEntiresIterator.hasNext())
	        {
	            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
	            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
	            
	            ReferenceBean referenceBean = new ReferenceBean();
	            	            
	            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
	            {
		            ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("contentVersion:" + contentVersion.getContentVersionId());
		    		referenceBean.setName(contentVersion.getOwningContent().getName());
		    		referenceBean.setReferencingObject(contentVersion.getValueObject());
	            }
	            else
	            {
	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		referenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
		    		referenceBean.setReferencingObject(siteNodeVersion.getValueObject());
	            }
	            
	            String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
	            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
	            if(existingReferenceBean == null)
	            {
		            List registryVOList = new ArrayList();
		            registryVOList.add(registryVO);
		            referenceBean.setRegistryVOList(registryVOList);
		            logger.info("Adding referenceBean to entries with key:" + key);
		            entries.put(key, referenceBean);
		            referenceBeanList.add(referenceBean);
	            }
	            else
	            {
	                logger.info("Found referenceBean in entries with key:" + key);
	                existingReferenceBean.getRegistryVOList().add(registryVO);
	            }
	        }

	        commitTransaction(db);
		}
		catch ( Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}

        return referenceBeanList;
    }
    */

	public List getReferencingObjectsForContent(Integer contentId) throws SystemException
    {
		return getReferencingObjectsForContent(contentId, -1);
    }

	public List getReferencingObjectsForContent(Integer contentId, int maxRows) throws SystemException
    {
		List referenceBeanList = new ArrayList();
        
		Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			referenceBeanList = getReferencingObjectsForContent(contentId, maxRows, db);
	    
	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("One of the references was not found which is bad but not critical:" + e.getMessage(), e);
		    rollbackTransaction(db);
			//throw new SystemException("An error occurred when we tried to fetch a list of roles in the repository. Reason:" + e.getMessage(), e);			
		}
		
		logger.info("referenceBeanList:" + referenceBeanList.size());
		
        return referenceBeanList;
    }

	public Set<SiteNodeVO> getReferencingSiteNodesForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
    {
		Timer t = new Timer();
        Set<SiteNodeVO> referenceBeanList = new HashSet<SiteNodeVO>();

        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
        //t.printElapsedTime("registryEntires:" + registryEntires.size());
        logger.info("registryEntires:" + registryEntires.size());
        Iterator registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
        	RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
        	if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
        		continue;
        	
        	logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());

            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            try
            {
                SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(new Integer(registryVO.getReferencingEntityCompletingId()), db);
                //t.printElapsedTime("siteNodeVersion 1");
	    		referenceBeanList.add(siteNodeVO);
            }
            catch(Exception e)
            {
                logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
            }
        } 
	    
		logger.info("referenceBeanList:" + referenceBeanList.size());
		
        return referenceBeanList;
    }
	
	public List getReferencingObjectsForContent(Integer contentId, int maxRows, Database db) throws SystemException, Exception
    {
		Timer t = new Timer();
        List referenceBeanList = new ArrayList();

        Map entries = new HashMap();
		
        List registryEntires = getMatchingRegistryVOList(Content.class.getName(), contentId.toString(), maxRows, db);
        logger.info("registryEntires:" + registryEntires.size());
        Iterator registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            boolean add = true;
            
            String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
            //String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
            if(existingReferenceBean == null)
            {
                
                existingReferenceBean = new ReferenceBean();
	            logger.info("Adding referenceBean to entries with key:" + key);
	            entries.put(key, existingReferenceBean);
	            referenceBeanList.add(existingReferenceBean);
	        }

            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
            {
                try
                {
                    ContentVersionVO contentVersion = ContentVersionController.getContentVersionController().getContentVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
                    ContentVO content = ContentController.getContentController().getContentVOWithId(contentVersion.getContentId(), db);
		    		existingReferenceBean.setName(content.getName());
		    		existingReferenceBean.setReferencingCompletingObject(content);
		    		referenceVersionBean.setReferencingObject(contentVersion);

		    		referenceVersionBean.getRegistryVOList().add(registryVO);
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            else
            {
                try
                {
	                SiteNodeVersionVO siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
	                SiteNodeVO siteNode = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeVersion.getSiteNodeId(), db);
	                //SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
                    //t.printElapsedTime("siteNodeVersion 2");
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		logger.info("siteNode:" + siteNode.getId());
		    		existingReferenceBean.setName(siteNode.getName());
		    		existingReferenceBean.setReferencingCompletingObject(siteNode);

		    		referenceVersionBean.setReferencingObject(siteNodeVersion);
		    		referenceVersionBean.getRegistryVOList().add(registryVO);
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            
            if(add)
            {
                boolean exists = false;
                ReferenceVersionBean existingReferenceVersionBean = null;
	            Iterator versionsIterator = existingReferenceBean.getVersions().iterator();
	            while(versionsIterator.hasNext())
	            {
	                existingReferenceVersionBean = (ReferenceVersionBean)versionsIterator.next();
	                if(existingReferenceVersionBean.getReferencingObject().equals(referenceVersionBean.getReferencingObject()))
	                {
	                    exists = true;
	                    break;
	                }
	            }

	            if(!exists)
	                existingReferenceBean.getVersions().add(referenceVersionBean);
	            else
	                existingReferenceVersionBean.getRegistryVOList().add(registryVO);

            }
            
        }
        
        Iterator i = referenceBeanList.iterator();
        while(i.hasNext())
        {
            ReferenceBean referenceBean = (ReferenceBean)i.next();
            if(referenceBean.getVersions().size() == 0)
                i.remove();
        }
	    
		logger.info("referenceBeanList:" + referenceBeanList.size());
		
        return referenceBeanList;
    }
    
    
    /**
     * This method gets all referencing sitenode versions
     * 
     * @param siteNodeId
     * @return
     */
	/*
    public List getReferencingObjectsForSiteNode(Integer siteNodeId) throws SystemException, Exception
    {
        List referenceBeanList = new ArrayList();
        
        Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			Map entries = new HashMap();
			
			List registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), db);
	        Iterator registryEntiresIterator = registryEntires.iterator();
	        while(registryEntiresIterator.hasNext())
	        {
	            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
	            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
	            
	            ReferenceBean referenceBean = new ReferenceBean();
	           
	            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
	            {
                    ContentVersion contentVersion = ContentVersionController.getContentVersionController().getContentVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("contentVersion:" + contentVersion.getContentVersionId());
		    		referenceBean.setName(contentVersion.getOwningContent().getName());
		    		referenceBean.setReferencingObject(contentVersion.getValueObject());
		    	}
	            else
	            {
	                SiteNodeVersion siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionWithId(new Integer(registryVO.getReferencingEntityId()), db);
		    		logger.info("siteNodeVersion:" + siteNodeVersion.getSiteNodeVersionId());
		    		referenceBean.setName(siteNodeVersion.getOwningSiteNode().getName());
		    		referenceBean.setReferencingObject(siteNodeVersion.getValueObject());
	            }
	            
	            String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
	            //String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
	            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
	            if(existingReferenceBean == null)
	            {
		            List registryVOList = new ArrayList();
		            registryVOList.add(registryVO);
		            referenceBean.setRegistryVOList(registryVOList);
		            logger.info("Adding referenceBean to entries with key:" + key);
		            entries.put(key, referenceBean);
		            referenceBeanList.add(referenceBean);
	            }
	            else
	            {
	                logger.info("Found referenceBean in entries with key:" + key);
	                existingReferenceBean.getRegistryVOList().add(registryVO);
	            }
	        }
	        
	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
			//throw new SystemException("An error occurred when we tried to fetch a list of roles in the repository. Reason:" + e.getMessage(), e);			
		}
		
        return referenceBeanList;
    }
    */

    public List getReferencingObjectsForSiteNode(Integer siteNodeId) throws SystemException, Exception
    {
    	return getReferencingObjectsForSiteNode(siteNodeId, -1);
    }
    
    public List getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows) throws SystemException, Exception
    {
        List referenceBeanList = new ArrayList();
        
        Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);

		    referenceBeanList = getReferencingObjectsForSiteNode(siteNodeId, maxRows, db);

		    commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("One of the references was not found which is bad but not critical:" + e.getMessage(), e);
		    rollbackTransaction(db);
		}
		
        return referenceBeanList;
    }

		    
    public List getReferencingObjectsForSiteNode(Integer siteNodeId, int maxRows, Database db) throws SystemException, Exception
    {
        List referenceBeanList = new ArrayList();
        
		Map entries = new HashMap();
		
		List registryEntires = getMatchingRegistryVOList(SiteNode.class.getName(), siteNodeId.toString(), maxRows, db);
        Iterator registryEntiresIterator = registryEntires.iterator();
        while(registryEntiresIterator.hasNext())
        {
            RegistryVO registryVO = (RegistryVO)registryEntiresIterator.next();
            logger.info("registryVO:" + registryVO.getReferencingEntityId() + ":" +  registryVO.getReferencingEntityCompletingId());
            boolean add = true;

            String key = "" + registryVO.getReferencingEntityCompletingName() + "_" + registryVO.getReferencingEntityCompletingId();
            //String key = "" + registryVO.getReferencingEntityName() + "_" + registryVO.getReferencingEntityId();
            ReferenceBean existingReferenceBean = (ReferenceBean)entries.get(key);
            if(existingReferenceBean == null)
            {
                existingReferenceBean = new ReferenceBean();
	            logger.info("Adding referenceBean to entries with key:" + key);
	            entries.put(key, existingReferenceBean);
	            referenceBeanList.add(existingReferenceBean);
	        }

            ReferenceVersionBean referenceVersionBean = new ReferenceVersionBean();
            
            if(registryVO.getReferencingEntityName().indexOf("Content") > -1)
            {
                try
                {
                    ContentVersionVO contentVersion = ContentVersionController.getContentVersionController().getContentVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
                    ContentVO content = ContentController.getContentController().getContentVOWithId(contentVersion.getContentId(), db);
		    		existingReferenceBean.setName(content.getName());
		    		existingReferenceBean.setReferencingCompletingObject(content);

		    		referenceVersionBean.setReferencingObject(contentVersion);
		    		referenceVersionBean.getRegistryVOList().add(registryVO);
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("content:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
            else
            {
                try
                {
	                SiteNodeVersionVO siteNodeVersion = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(new Integer(registryVO.getReferencingEntityId()), db);
                	SiteNodeVO siteNode = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeVersion.getSiteNodeId(), db);
		    		existingReferenceBean.setName(siteNode.getName());
		    		existingReferenceBean.setReferencingCompletingObject(siteNode);
		    		referenceVersionBean.setReferencingObject(siteNodeVersion);

		    		referenceVersionBean.getRegistryVOList().add(registryVO);
                }
                catch(Exception e)
                {
                    add = false;
                    logger.info("siteNode:" + registryVO.getReferencingEntityId() + " did not exist - skipping..");
                }
            }
               
            if(add)
            {
	            boolean exists = false;
	            ReferenceVersionBean existingReferenceVersionBean = null;
	            Iterator versionsIterator = existingReferenceBean.getVersions().iterator();
	            while(versionsIterator.hasNext())
	            {
	                existingReferenceVersionBean = (ReferenceVersionBean)versionsIterator.next();
	                if(existingReferenceVersionBean.getReferencingObject().equals(referenceVersionBean.getReferencingObject()))
	                {
	                    exists = true;
	                    break;
	                }
	            }
	            
	            if(!exists)
	                existingReferenceBean.getVersions().add(referenceVersionBean);
	            else
	                existingReferenceVersionBean.getRegistryVOList().add(registryVO);
	        }
        }
        
        Iterator i = referenceBeanList.iterator();
        while(i.hasNext())
        {
            ReferenceBean referenceBean = (ReferenceBean)i.next();
            if(referenceBean.getVersions().size() == 0)
                i.remove();
        }
		
        return referenceBeanList;
    }

	/**
	 * Gets matching references
	 */
	
	public List getMatchingRegistryVOList(String entityName, String entityId, int maxRows, Database db) throws SystemException, Exception
	{
	    List matchingRegistryVOList = new ArrayList();
	    
		String SQL = "CALL SQL select registryid, entityname, entityid, referencetype, referencingentityname, referencingentityid, referencingentitycomplname, referencingentitycomplid from cmregistry r where r.entityName = $1 AND r.entityId = $2 AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl";
		if(maxRows > 0)
		{
			if(CmsPropertyHandler.getDatabaseEngine().equalsIgnoreCase("mysql"))
				SQL = "CALL SQL select registryid, entityname, entityid, referencetype, referencingentityname, referencingentityid, referencingentitycomplname, referencingentitycomplid from cmregistry r where r.entityName = $1 AND r.entityId = $2 LIMIT " + maxRows + " AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl";
			else if(CmsPropertyHandler.getDatabaseEngine().equalsIgnoreCase("oracle"))
				SQL = "CALL SQL select * from (select registryid, entityname, entityid, referencetype, referencingentityname, referencingentityid, referencingentitycomplname, referencingentitycomplid from cmregistry r where r.entityName = $1 AND r.entityId = $2) where rownum <= " + maxRows + " AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl";
			else if(CmsPropertyHandler.getDatabaseEngine().equalsIgnoreCase("sqlserver"))
				SQL = "CALL SQL select top " + maxRows + " registryid, entityname, entityid, referencetype, referencingentityname, referencingentityid, referencingentitycomplname, referencingentitycomplid from cmregistry r where r.entityName = $1 AND r.entityId = $2 AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl";
		}
		
		OQLQuery oql = db.getOQLQuery(SQL);
		//OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2 ORDER BY r.registryId");
		oql.bind(entityName);
		oql.bind(entityId);
		
		QueryResults results = oql.execute(Database.ReadOnly);
		
		int i = 0;
		while (results.hasMore() && (maxRows == -1 || i < maxRows)) 
        {
            Registry registry = (Registry)results.next();
            RegistryVO registryVO = registry.getValueObject();
                
            matchingRegistryVOList.add(registryVO);

            i++;
        }            
		
		results.close();
		oql.close();

		return matchingRegistryVOList;		
	}
	
	/**
	 * Gets matching references
	 */
	
	public Map<String,Set<SiteNodeVO>> getContentSiteNodeVOListMap(String entityName, String[] entityIds, int maxRows, Database db) throws SystemException, Exception
	{
	    //Timer t = new Timer();
	    Map<String,Set<SiteNodeVO>> contentSiteNodeVOListMap = new HashMap<String,Set<SiteNodeVO>>();
	    Map<String,Set<Integer>> contentSiteNodeIdListMap = new HashMap<String,Set<Integer>>();
	    
		Set<Integer> siteNodeIds = new HashSet<Integer>();

		int startIndex = 0;
		int endIndex = (entityIds.length > 100 ? 100 : entityIds.length);
		
		String[] partOfEntityIds = Arrays.copyOfRange(entityIds, 0, endIndex);
		
		while(partOfEntityIds != null && partOfEntityIds.length > 0)
		{
		    StringBuilder variables = new StringBuilder();
		    for(int i=0; i<partOfEntityIds.length; i++)
		    	variables.append("$" + (i+2) + (i+1!=partOfEntityIds.length ? "," : ""));
			
		    //System.out.println("partOfEntityIds:" + partOfEntityIds.length);
		    //System.out.println("variables:" + variables);

			String SQL = "CALL SQL select registryid, entityname, entityid, referencetype, referencingentityname, referencingentityid, referencingentitycomplname, referencingentitycomplid from cmregistry r where r.entityName = $1 AND entityid IN (" + variables + ") AND r.registryid = (select max(registryId) from cmregistry where entityName = r.entityName AND entityid = r.entityId) ORDER BY r.registryId AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl";
			//System.out.println("SQL:" + SQL);
			OQLQuery oql = db.getOQLQuery(SQL);
			//OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId IN LIST(" + variables + ") ORDER BY r.registryId");
			oql.bind(entityName);
			for(String entityId : partOfEntityIds)
			{
				//System.out.print("'" + entityId + "',");
				oql.bind(entityId);
			}
			//t.printElapsedTime("bindings done");
			
			
			QueryResults results = oql.execute(Database.ReadOnly);
			//t.printElapsedTime("results");
	
			int i = 0;
			while (results.hasMore() && (maxRows == -1 || i < maxRows)) 
	        {
	            Registry registry = (Registry)results.next();
	            RegistryVO registryVO = registry.getValueObject();
	            
	            if(registryVO.getReferencingEntityCompletingName().indexOf("SiteNode") > -1)
	            {
	            	try
	            	{
	            		Set<Integer> existingSiteNodeIds = contentSiteNodeIdListMap.get("" + registryVO.getEntityId());
	            		if(existingSiteNodeIds == null)
	            		{
	            			existingSiteNodeIds = new HashSet<Integer>();
	            			contentSiteNodeIdListMap.put("" + registryVO.getEntityId(), existingSiteNodeIds);
	            		}
	            		siteNodeIds.add(new Integer(registryVO.getReferencingEntityCompletingId()));
	            		//SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(new Integer(registryVO.getReferencingEntityCompletingId()), db);
	            		existingSiteNodeIds.add(new Integer(registryVO.getReferencingEntityCompletingId()));
	            	}
	            	catch (Exception e) 
	            	{
	            		logger.error("Error getting related sitenode:" + e.getMessage());
					}
	            }
	
	            i++;
	        }           
			results.close();
			oql.close();
			
			startIndex = endIndex+1;
			endIndex = (entityIds.length > startIndex+100 ? startIndex + 100 : entityIds.length - 1);
			if(startIndex >= endIndex)
				partOfEntityIds = null;
			else
				partOfEntityIds = Arrays.copyOfRange(entityIds, startIndex, endIndex);
		}
		
		//t.printElapsedTime("after registry pullout:" + siteNodeIds.size());

		Map<Integer,SiteNodeVO> siteNodeVOMap = SiteNodeController.getController().getSiteNodeVOMap(siteNodeIds.toArray(new Integer[siteNodeIds.size()]), db);
		
		//t.printElapsedTime("siteNodeVOMap:" + siteNodeVOMap.size());
		
		for(String contentId : contentSiteNodeIdListMap.keySet())
		{
			Set<Integer> contentSiteNodeIds = contentSiteNodeIdListMap.get(contentId);
			for(Integer siteNodeId : contentSiteNodeIds)
			{
				SiteNodeVO sn = siteNodeVOMap.get(siteNodeId);
        		Set<SiteNodeVO> existingSiteNodeVOList = contentSiteNodeVOListMap.get("" + siteNodeId);
        		if(existingSiteNodeVOList == null)
        		{
        			existingSiteNodeVOList = new HashSet<SiteNodeVO>();
        			contentSiteNodeVOListMap.put("" + contentId, existingSiteNodeVOList);
        		}
        		existingSiteNodeVOList.add(sn);
			}
		}
		
		//t.printElapsedTime("contentSiteNodeVOListMap:" + contentSiteNodeVOListMap.size());
		
		return contentSiteNodeVOListMap;		
	}
	
	
	public List getReferencedObjects(String referencingEntityName, String referencingEntityId) throws SystemException, Exception
	{
	    List result = new ArrayList();
	    
        Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
		
			List registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);
		    
			Iterator i = registryVOList.iterator();
			while(i.hasNext())
			{
			    RegistryVO registryVO = (RegistryVO)i.next();
			    if(registryVO.getEntityName().indexOf("Content") > -1)
	            {
	                try
	                {
	                    Content content = ContentController.getContentController().getContentWithId(new Integer(registryVO.getEntityId()), db);
			    		logger.info("contentVersion:" + content.getContentId());
			    		result.add(content.getValueObject());
	                }
	                catch(Exception e)
	                {
	                    logger.info("content:" + registryVO.getEntityId() + " did not exist - skipping..");
	                }
	            }
	            else if(registryVO.getEntityName().indexOf("SiteNode") > -1)
	            {
	                try
	                {
		                SiteNode siteNode = SiteNodeController.getController().getSiteNodeWithId(new Integer(registryVO.getEntityId()), db);
			    		logger.info("siteNode:" + siteNode.getId());
			    		result.add(siteNode.getValueObject());
			    	}
	                catch(Exception e)
	                {
	                    logger.info("siteNode:" + registryVO.getEntityId() + " did not exist - skipping..");
	                }
	            }
			}
			
			commitTransaction(db);
		}
		catch (Exception e)		
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}
		
		return result;
	}

	public List getReferencedObjects(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
	    List result = new ArrayList();
	    
		List registryVOList = getMatchingRegistryVOListForReferencingEntity(referencingEntityName, referencingEntityId, db);
	    
		Iterator i = registryVOList.iterator();
		while(i.hasNext())
		{
		    RegistryVO registryVO = (RegistryVO)i.next();
		    if(registryVO.getEntityName().indexOf("Content") > -1)
            {
                try
                {
                    ContentVO contentVO = ContentController.getContentController().getContentVOWithId(new Integer(registryVO.getEntityId()), db);
		    		logger.info("contentVO:" + contentVO.getId());
		    		result.add(contentVO);
                }
                catch(Exception e)
                {
                    logger.info("content:" + registryVO.getEntityId() + " did not exist - skipping..");
                }
            }
            else if(registryVO.getEntityName().indexOf("SiteNode") > -1)
            {
                try
                {
	                SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(new Integer(registryVO.getEntityId()), db);
		    		logger.info("siteNodeVO:" + siteNodeVO.getId());
		    		result.add(siteNodeVO);
		    	}
                catch(Exception e)
                {
                    logger.info("siteNode:" + registryVO.getEntityId() + " did not exist - skipping..");
                }
            }
		}
		
		return result;
	}

	/**
	 * Gets matching references
	 */
	public List getMatchingRegistryVOListForReferencingEntity(String referencingEntityName, String referencingEntityId, Database db) throws SystemException, Exception
	{
		String cacheName = "registryCache";
		String cacheKey = "" + referencingEntityName + "_" + referencingEntityId;
		
		List matchingRegistryVOList = (List)CacheController.getCachedObjectFromAdvancedCache(cacheName, cacheKey); 
		if(matchingRegistryVOList != null)
		{
			return matchingRegistryVOList;
		}
		else
		{
		    matchingRegistryVOList = new ArrayList();
		    			
		    OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
			oql.bind(referencingEntityName);
			oql.bind(referencingEntityId);
			
			QueryResults results = oql.execute(Database.ReadOnly);
			
			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            RegistryVO registryVO = registry.getValueObject();
	    	    //logger.info("found match:" + registryVO.getEntityName() + ":" + registryVO.getEntityId());
	            
	            matchingRegistryVOList.add(registryVO);
	        }       
			
			CacheController.cacheObjectInAdvancedCache(cacheName, cacheKey, matchingRegistryVOList, new String[]{""+cacheKey.hashCode()}, true);
			
			results.close();
			oql.close();
		}

		return matchingRegistryVOList;		
	}
	
	
	/**
	 * Gets matching references
	 */
	public List<RegistryVO> getMatchingRegistryVOListForReferencingEntities(String referencingEntityName, List<Integer> referencingEntityIds, Database db) throws SystemException, Exception
	{
		List<RegistryVO> matchingRegistryVOList = new ArrayList<RegistryVO>();
		   
		StringBuilder variables = new StringBuilder();
	    for(int i=0; i<referencingEntityIds.size(); i++)
	    	variables.append("$" + (i+2) + (i+1!=referencingEntityIds.size() ? "," : ""));

	    OQLQuery oql = db.getOQLQuery("CALL SQL SELECT registryId, entityName, entityId, referenceType, referencingEntityName, referencingEntityId, referencingEntityComplName, referencingEntityComplId FROM cmRegistry WHERE referencingEntityName = $1 AND referencingEntityId IN (" + variables + ") order by registryId AS org.infoglue.cms.entities.management.impl.simple.RegistryImpl");
		oql.bind(referencingEntityName);

		for(Integer entityId : referencingEntityIds)
			oql.bind(entityId.toString());
		
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
        {
            Registry registry = (Registry)results.next();
            RegistryVO registryVO = registry.getValueObject();
    	    //logger.info("found match:" + registryVO.getEntityName() + ":" + registryVO.getEntityId());
            
            matchingRegistryVOList.add(registryVO);
        }       
		
		results.close();
		oql.close();

		return matchingRegistryVOList;		
	}
	
	/**
	 * Gets matching references
	 */
	
	public void clearRegistryVOList(String referencingEntityName, String referencingEntityId) throws SystemException, Exception
	{
		Database db = CastorDatabaseService.getDatabase();
			
		try 
		{
			beginTransaction(db);
		
			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
			oql.bind(referencingEntityName);
			oql.bind(referencingEntityId);
			
			QueryResults results = oql.execute();
			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            //System.out.println("Removing registry:" + registry.getRegistryId());
	            db.remove(registry);
	        }
			
			results.close();
			oql.close();
		    
			commitTransaction(db);
		}
		catch (Exception e)		
		{
		    rollbackTransaction(db);
			throw new SystemException("An error occurred when we tried to fetch which sitenode uses a metainfo. Reason:" + e.getMessage(), e);			
		}
	}
	

	/**
	 * Gets matching references
	 */
	
	public void clearRegistryForReferencedEntity(String entityName, String entityId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2 ORDER BY r.registryId");
			oql.bind(entityName);
			oql.bind(entityId);
					
			QueryResults results = oql.execute();

			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            db.remove(registry);
	        }
		    
			results.close();
			oql.close();

	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	/**
	 * Gets matching references
	 */
	
	public void clearRegistryForReferencingEntityCompletingName(String entityCompletingName, String entityCompletingId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityCompletingName = $1 AND r.referencingEntityCompletingId = $2 ORDER BY r.registryId");
			oql.bind(entityCompletingName);
			oql.bind(entityCompletingId);
					
			QueryResults results = oql.execute();

			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            db.remove(registry);
	        }
		    
			results.close();
			oql.close();

	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	/**
	 * Gets matching references
	 */
	
	public void clearRegistryForReferencingEntityName(String entityName, String entityId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			OQLQuery oql = db.getOQLQuery("SELECT r FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.referencingEntityName = $1 AND r.referencingEntityId = $2 ORDER BY r.registryId");
			oql.bind(entityName);
			oql.bind(entityId);
					
			QueryResults results = oql.execute();

			while (results.hasMore()) 
	        {
	            Registry registry = (Registry)results.next();
	            db.remove(registry);
	        }

			results.close();
			oql.close();

	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}

	/**
	 * Clears all references to a entity
	 */
/*
	public void clearRegistryForReferencedEntity(String entityName, String entityId) throws SystemException, Exception
	{
	    Database db = CastorDatabaseService.getDatabase();
		
		try 
		{
			beginTransaction(db);
			
			OQLQuery oql = db.getOQLQuery("DELETE FROM org.infoglue.cms.entities.management.impl.simple.RegistryImpl r WHERE r.entityName = $1 AND r.entityId = $2");
			oql.bind(entityName);
			oql.bind(entityId);
			QueryResults results = oql.execute();		
		    
	        commitTransaction(db);
		}
		catch (Exception e)		
		{
		    logger.warn("An error occurred so we should not complete the transaction:" + e);
		    rollbackTransaction(db);
		}
	}
*/
	
	/**
	 * Gets siteNodeVersions which uses the metainfo
	 */
	/*
	public List getSiteNodeVersionsWhichUsesContentVersionAsMetaInfo(ContentVersion contentVersion, Database db) throws SystemException, Exception
	{
	    List siteNodeVersions = new ArrayList();
	    
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.serviceBindings.availableServiceBinding.name = $1 AND snv.serviceBindings.bindingQualifyers.name = $2 AND snv.serviceBindings.bindingQualifyers.value = $3");
	    oql.bind("Meta information");
		oql.bind("contentId");
		oql.bind(contentVersion.getOwningContent().getId());
		
		QueryResults results = oql.execute();
		this.logger.info("Fetching entity in read/write mode");

		while (results.hasMore()) 
        {
		    SiteNodeVersion siteNodeVersion = (SiteNodeVersion)results.next();
		    siteNodeVersions.add(siteNodeVersion);
		    //logger.info("siteNodeVersion:" + siteNodeVersion.getId());
        }
    	
		results.close();
		oql.close();

		return siteNodeVersions;		
	}
	*/

	/**
	 * Gets siteNodeVersions which uses the metainfo
	 */
	public SiteNodeVersion getLatestActiveSiteNodeVersionWhichUsesContentVersionAsMetaInfo(ContentVersion contentVersion, Database db) throws SystemException, Exception
	{
	    SiteNodeVersion siteNodeVersion = null;
	    
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.owningSiteNode.metaInfoContentId = $1 AND snv.isActive = $2 ORDER BY snv.siteNodeVersionId desc");
	    oql.bind(contentVersion.getValueObject().getContentId());
		oql.bind(new Boolean(true));
		
		/*
	    OQLQuery oql = db.getOQLQuery("SELECT snv FROM org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl snv WHERE snv.serviceBindings.availableServiceBinding.name = $1 AND snv.serviceBindings.bindingQualifyers.name = $2 AND snv.serviceBindings.bindingQualifyers.value = $3 AND snv.isActive = $4 ORDER BY snv.siteNodeVersionId desc");
	    oql.bind("Meta information");
		oql.bind("contentId");
		oql.bind(contentVersion.getOwningContent().getId());
		oql.bind(new Boolean(true));
		*/
		
		QueryResults results = oql.execute();
		this.logger.info("Fetching entity in read/write mode");

		if (results.hasMore()) 
        {
		    siteNodeVersion = (SiteNodeVersion)results.next();
        }
    	
		results.close();
		oql.close();

		return siteNodeVersion;		
	}
}
