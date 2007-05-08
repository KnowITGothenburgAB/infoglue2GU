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
package org.infoglue.deliver.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.PublicationController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeVersionController;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.impl.simple.ContentImpl;
import org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl;
import org.infoglue.cms.entities.content.impl.simple.DigitalAssetImpl;
import org.infoglue.cms.entities.content.impl.simple.MediumContentImpl;
import org.infoglue.cms.entities.content.impl.simple.SmallContentImpl;
import org.infoglue.cms.entities.management.impl.simple.AvailableServiceBindingImpl;
import org.infoglue.cms.entities.management.impl.simple.GroupImpl;
import org.infoglue.cms.entities.management.impl.simple.RoleImpl;
import org.infoglue.cms.entities.management.impl.simple.SmallAvailableServiceBindingImpl;
import org.infoglue.cms.entities.management.impl.simple.SystemUserImpl;
import org.infoglue.cms.entities.publishing.PublicationDetailVO;
import org.infoglue.cms.entities.publishing.impl.simple.PublicationDetailImpl;
import org.infoglue.cms.entities.publishing.impl.simple.PublicationImpl;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.NotificationMessage;
import org.infoglue.deliver.applications.databeans.CacheEvictionBean;
import org.infoglue.deliver.controllers.kernel.impl.simple.DigitalAssetDeliveryController;

/**
 * @author mattias
 *
 * This is a selective publication thread. What that means is that it only throws
 * away objects and pages in the cache which are affected. Experimental for now.
 */
public class SelectiveLivePublicationThread extends PublicationThread
{
    public final static Logger logger = Logger.getLogger(SelectiveLivePublicationThread.class.getName());

    private List cacheEvictionBeans = new ArrayList();
	
	public SelectiveLivePublicationThread()
	{
	}
	
	public List getCacheEvictionBeans()
	{
		return cacheEvictionBeans;
	}

	public void run()
	{
		logger.info("Run in SelectiveLivePublicationThread....");
		
		int publicationDelay = 5000;
	    String publicationThreadDelay = CmsPropertyHandler.getPublicationThreadDelay();
	    if(publicationThreadDelay != null && !publicationThreadDelay.equalsIgnoreCase("") && publicationThreadDelay.indexOf("publicationThreadDelay") == -1)
	        publicationDelay = Integer.parseInt(publicationThreadDelay);
	    
	    logger.info("\n\n\nSleeping " + publicationDelay + "ms.\n\n\n");
		try 
		{
			sleep(publicationDelay);
		} 
		catch (InterruptedException e1) 
		{
			e1.printStackTrace();
		}

		logger.info("cacheEvictionBeans.size:" + cacheEvictionBeans.size() + ":" + RequestAnalyser.getRequestAnalyser().getBlockRequests());
        if(cacheEvictionBeans.size() > 0)
		{
			try
			{		
				logger.info("setting block");
		        RequestAnalyser.getRequestAnalyser().setBlockRequests(true);
				
				Iterator i = cacheEvictionBeans.iterator();
				while(i.hasNext())
				{
				    CacheEvictionBean cacheEvictionBean = (CacheEvictionBean)i.next();
				    String className = cacheEvictionBean.getClassName();
				    String objectId = cacheEvictionBean.getObjectId();
				    String objectName = cacheEvictionBean.getObjectName();
					String typeId = cacheEvictionBean.getTypeId();
					
				    logger.info("className:" + className);
					logger.info("objectId:" + objectId);
					logger.info("objectName:" + objectName);
					logger.info("typeId:" + typeId);
	
			        boolean isDependsClass = false;
				    if(className != null && className.equalsIgnoreCase(PublicationDetailImpl.class.getName()))
				        isDependsClass = true;
			
				    CacheController.clearCaches(className, objectId, null);
		
				    logger.info("Updating className with id:" + className + ":" + objectId);
					if(className != null && !typeId.equalsIgnoreCase("" + NotificationMessage.SYSTEM))
					{
					    Class type = Class.forName(className);
		
					    if(!isDependsClass && className.equalsIgnoreCase(SystemUserImpl.class.getName()) || className.equalsIgnoreCase(RoleImpl.class.getName()) || className.equalsIgnoreCase(GroupImpl.class.getName()))
					    {
					        Object[] ids = {objectId};
					        CacheController.clearCache(type, ids);
						}
					    else if(!isDependsClass)
					    {
					        Object[] ids = {new Integer(objectId)};
						    CacheController.clearCache(type, ids);
					    }
		
						//If it's an contentVersion we should delete all images it might have generated from attributes.
						if(Class.forName(className).getName().equals(ContentImpl.class.getName()))
						{
						    logger.info("We clear all small contents as well " + objectId);
							Class typesExtra = SmallContentImpl.class;
							Object[] idsExtra = {new Integer(objectId)};
							CacheController.clearCache(typesExtra, idsExtra);
			
							logger.info("We clear all medium contents as well " + objectId);
							Class typesExtraMedium = MediumContentImpl.class;
							Object[] idsExtraMedium = {new Integer(objectId)};
							CacheController.clearCache(typesExtraMedium, idsExtraMedium);
						}
						else if(Class.forName(className).getName().equals(AvailableServiceBindingImpl.class.getName()))
						{
						    Class typesExtra = SmallAvailableServiceBindingImpl.class;
							Object[] idsExtra = {new Integer(objectId)};
							CacheController.clearCache(typesExtra, idsExtra);
						}
						else if(Class.forName(className).getName().equals(SiteNodeImpl.class.getName()))
						{
						    Class typesExtra = SmallSiteNodeImpl.class;
							Object[] idsExtra = {new Integer(objectId)};
							CacheController.clearCache(typesExtra, idsExtra);
						}
						else if(Class.forName(className).getName().equals(DigitalAssetImpl.class.getName()))
						{
							CacheController.clearCache("digitalAssetCache");
						    logger.info("We should delete all images with digitalAssetId " + objectId);
							DigitalAssetDeliveryController.getDigitalAssetDeliveryController().deleteDigitalAssets(new Integer(objectId));
						}
						else if(Class.forName(className).getName().equals(PublicationImpl.class.getName()))
						{
							List publicationDetailVOList = PublicationController.getController().getPublicationDetailVOList(new Integer(objectId));
							Iterator publicationDetailVOListIterator = publicationDetailVOList.iterator();
							while(publicationDetailVOListIterator.hasNext())
							{
								PublicationDetailVO publicationDetailVO = (PublicationDetailVO)publicationDetailVOListIterator.next();
								logger.info("publicationDetailVO.getEntityClass():" + publicationDetailVO.getEntityClass());
								logger.info("publicationDetailVO.getEntityId():" + publicationDetailVO.getEntityId());
								if(Class.forName(publicationDetailVO.getEntityClass()).getName().equals(ContentVersion.class.getName()))
								{
									logger.info("We clear all caches having references to contentVersion: " + publicationDetailVO.getEntityId());
									Integer contentId = ContentVersionController.getContentVersionController().getContentIdForContentVersion(publicationDetailVO.getEntityId());
								    CacheController.clearCaches(publicationDetailVO.getEntityClass(), publicationDetailVO.getEntityId().toString(), null);

									logger.info("We clear all small contents as well " + contentId);
									Class typesExtra = SmallContentImpl.class;
									Object[] idsExtra = {contentId};
									CacheController.clearCache(typesExtra, idsExtra);
					
									logger.info("We clear all medium contents as well " + contentId);
									Class typesExtraMedium = MediumContentImpl.class;
									Object[] idsExtraMedium = {contentId};
									CacheController.clearCache(typesExtraMedium, idsExtraMedium);
								}
								else if(Class.forName(publicationDetailVO.getEntityClass()).getName().equals(SiteNodeVersion.class.getName()))
								{
									Integer siteNodeId = SiteNodeVersionController.getController().getSiteNodeVersionVOWithId(publicationDetailVO.getEntityId()).getSiteNodeId();
								    CacheController.clearCaches(publicationDetailVO.getEntityClass(), publicationDetailVO.getEntityId().toString(), null);

								    logger.info("We clear all small siteNodes as well " + siteNodeId);
								    Class typesExtra = SmallSiteNodeImpl.class;
									Object[] idsExtra = {siteNodeId};
									CacheController.clearCache(typesExtra, idsExtra);
									
								    logger.info("We also clear the meta info content..");
									SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);

									logger.info("We clear all contents as well " + siteNodeVO.getMetaInfoContentId());
									Class metaInfoContentExtra = ContentImpl.class;
									Object[] idsMetaInfoContentExtra = {siteNodeVO.getMetaInfoContentId()};
									CacheController.clearCache(metaInfoContentExtra, idsMetaInfoContentExtra);
									
									logger.info("We clear all small contents as well " + siteNodeVO.getMetaInfoContentId());
									Class metaInfoContentExtraSmall = SmallContentImpl.class;
									CacheController.clearCache(metaInfoContentExtraSmall, idsMetaInfoContentExtra);
									
									logger.info("We clear all medium contents as well " + siteNodeVO.getMetaInfoContentId());
									Class metaInfoContentExtraMedium = MediumContentImpl.class;
									CacheController.clearCache(metaInfoContentExtraMedium, idsMetaInfoContentExtra);
									
									CacheController.clearCaches(ContentImpl.class.getName(), siteNodeVO.getMetaInfoContentId().toString(), null);

									Database db = CastorDatabaseService.getDatabase();
									db.begin();
									
									Content content = ContentController.getContentController().getContentWithId(siteNodeVO.getMetaInfoContentId(), db);
									List contentVersionIds = new ArrayList();
									Iterator contentVersionIterator = content.getContentVersions().iterator();
									logger.info("Versions:" + content.getContentVersions().size());
									while(contentVersionIterator.hasNext())
									{
										ContentVersion contentVersion = (ContentVersion)contentVersionIterator.next();
										contentVersionIds.add(contentVersion.getId());
										logger.info("We clear the meta info contentVersion " + contentVersion.getId());
									}

									db.rollback();

									db.close();
									
									Iterator contentVersionIdsIterator = contentVersionIds.iterator();
									logger.info("Versions:" + contentVersionIds.size());
									while(contentVersionIdsIterator.hasNext())
									{
										Integer contentVersionId = (Integer)contentVersionIdsIterator.next();
										logger.info("We clear the meta info contentVersion " + contentVersionId);
										Class metaInfoContentVersionExtra = ContentVersionImpl.class;
										Object[] idsMetaInfoContentVersionExtra = {contentVersionId};
										CacheController.clearCache(metaInfoContentVersionExtra, idsMetaInfoContentVersionExtra);
										CacheController.clearCaches(ContentVersionImpl.class.getName(), contentVersionId.toString(), null);
									}
									
									logger.info("After:" + content.getContentVersions().size());

								}
								
							}
						}
					}	
				}
			} 
			catch (Exception e)
			{
			    logger.error("An error occurred in the SelectiveLivePublicationThread:" + e.getMessage(), e);
			}
		}

        logger.info("released block \n\n DONE---");
		RequestAnalyser.getRequestAnalyser().setBlockRequests(false);
	}
}
