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

package org.infoglue.deliver.controllers.kernel.impl.simple;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentCategoryVO;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAsset;
import org.infoglue.cms.entities.content.impl.simple.ContentImpl;
import org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl;
import org.infoglue.cms.entities.content.impl.simple.MediumContentImpl;
import org.infoglue.cms.entities.content.impl.simple.SmallContentImpl;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Repository;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.applications.databeans.NullObject;
import org.infoglue.deliver.controllers.kernel.URLComposer;
import org.infoglue.deliver.util.CacheController;


public class ContentDeliveryController extends BaseDeliveryController
{
	private URLComposer urlComposer = null; 
	private VisualFormatter formatter = new VisualFormatter();
	
	/**
	 * Private constructor to enforce factory-use
	 */
	
	private ContentDeliveryController()
	{
		urlComposer = URLComposer.getURLComposer(); 
	}
	
	/**
	 * Factory method
	 */
	
	public static ContentDeliveryController getContentDeliveryController()
	{
		return new ContentDeliveryController();
	}
	
	/**
	 * This method returns which mode the delivery-engine is running in.
	 * The mode is important to be able to show working, preview and published data separate.
	 */
	
	private Integer getOperatingMode()
	{
		Integer operatingMode = new Integer(0); //Default is working
		try
		{
			operatingMode = new Integer(CmsPropertyHandler.getProperty("operatingMode"));
			//getLogger().info("Operating mode is:" + operatingMode);
		}
		catch(Exception e)
		{
			getLogger().warn("We could not get the operating mode from the propertyFile:" + e.getMessage(), e);
		}
		return operatingMode;
	}
	
	
	/**
	 * This method return a contentVO
	 */
	
	public ContentVO getContentVO(Integer contentId, Database db) throws SystemException, Exception
	{
		ContentVO contentVO = null;
		
		contentVO = (ContentVO)getVOWithId(SmallContentImpl.class, contentId, db);
		
		return contentVO;
	}
	
	/**
	 * This method return a contentVO
	 */
	
	public ContentVO getContentVO(Database db, Integer contentId, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		deliveryContext.addUsedContent("content_" + contentId);

		ContentVO contentVO = (ContentVO)getVOWithId(SmallContentImpl.class, contentId, db);
				
		return contentVO;
	}
	
	
	/**
	 * This method returns that contentVersionVO which matches the parameters sent in and which 
	 * also has the correct state for this delivery-instance.
	 */
	
	public ContentVersionVO getContentVersionVO(Database db, Integer siteNodeId, Integer contentId, Integer languageId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		ContentVersionVO contentVersionVO = null;
		
		SiteNodeVO siteNodeVO = (SiteNodeVO)getVOWithId(SiteNodeImpl.class, siteNodeId, db);
		String contentVersionKey = "contentVersionVO_" + siteNodeVO.getRepositoryId() + "_" + contentId + "_" + languageId + "_" + useLanguageFallback;
		getLogger().info("contentVersionKey:" + contentVersionKey);
		contentVersionVO = (ContentVersionVO)CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", contentVersionKey);
		
		if(contentVersionVO != null)
		{
			//getLogger().info("There was an cached contentVersionVO:" + contentVersionVO.getContentVersionId());
		}
		else
		{
		    ContentVersion contentVersion = this.getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
        	
			if(contentVersion != null)
			{
				contentVersionVO = contentVersion.getValueObject();
				
				CacheController.cacheObjectInAdvancedCache("contentVersionCache", contentVersionKey, contentVersionVO, new String[]{"contentVersion_" + contentVersionVO.getId(), "content_" + contentVersionVO.getContentId()}, true);
			}
    	
        }
		
		if(contentVersionVO != null)
		    deliveryContext.addUsedContentVersion("contentVersion_" + contentVersionVO.getId());
		
		return contentVersionVO;
	}

	
	/**
	 * This method returns that contentVersion which matches the parameters sent in and which 
	 * also has the correct state for this delivery-instance.
	 */
	
	private ContentVersion getContentVersion(Integer siteNodeId, Integer contentId, Integer languageId, Database db, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		if(contentId == null || contentId.intValue() < 1)
			return null;
		
		ContentVersion contentVersion = null;
		
		MediumContentImpl content = (MediumContentImpl)getObjectWithId(MediumContentImpl.class, contentId, db);
		boolean isValidContent = isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, false, db, deliveryContext);
		if(isValidContent)
		{
			contentVersion = getContentVersion(content, languageId, getOperatingMode(), deliveryContext, db);
			if(contentVersion == null && useLanguageFallback)
			{
				getLogger().info("Did not find it in requested languge... lets check the masterlanguage....");
				
				Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId).getLanguageId();
				if(!languageId.equals(masterLanguageId))
				{
					contentVersion = getContentVersion(content, masterLanguageId, getOperatingMode(), deliveryContext, db);
				}
			}
		}
		
		return contentVersion;
	}


	/**
	 * This method gets a contentVersion with a state and a language which is active.
	 */
	/*
	private ContentVersion getContentVersion(Content content, Integer languageId, Integer operatingMode, DeliveryContext deliveryContext, Database db) throws Exception
	{
	    getLogger().info("content:" + content.getId());
	    getLogger().info("operatingMode:" + operatingMode);
	    getLogger().info("languageId:" + languageId);
		
		ContentVersion contentVersion = null;
		
	    String versionKey = "" + content.getId() + "_" + languageId + "_" + operatingMode + "_contentVersionId";
		//System.out.println("versionKey:" + versionKey);
		
		Integer contentVersionId = (Integer)CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", versionKey);
		if(contentVersionId != null)
		{
			getLogger().info("There was a cached content version id:" + contentVersionId);
		    //System.out.println("There was a cached content version id:" + contentVersionId);
		    contentVersion = (ContentVersion)getObjectWithId(ContentVersionImpl.class, contentVersionId, db);
		    //System.out.println("Loaded the version from cache instead of querying it:" + contentVersionId);
		    getLogger().info("contentVersion read");
		}
		else
		{
			Collection contentVersions = content.getContentVersions();
			
			Iterator versionIterator = contentVersions.iterator();
			while(versionIterator.hasNext())
			{
				ContentVersion contentVersionCandidate = (ContentVersion)versionIterator.next();	
				getLogger().info("contentVersionCandidate:" + contentVersionCandidate.getId() + ":" + contentVersionCandidate.getIsActive() + ":" + contentVersionCandidate.getLanguage() + ":" + contentVersionCandidate.getStateId() + ":" + operatingMode);
				getLogger().info("" + contentVersionCandidate.getIsActive().booleanValue());
				getLogger().info("" + contentVersionCandidate.getLanguage().getId().intValue());
				getLogger().info("" + languageId.intValue());
				getLogger().info("" + contentVersionCandidate.getStateId().intValue());
				getLogger().info("" + operatingMode.intValue());
				
				if(contentVersionCandidate.getIsActive().booleanValue() && contentVersionCandidate.getLanguage().getId().intValue() ==  languageId.intValue() && contentVersionCandidate.getStateId().intValue() >= operatingMode.intValue())
				{
					if(contentVersion == null || contentVersion.getId().intValue() < contentVersionCandidate.getId().intValue())
					{
						contentVersion = contentVersionCandidate;
					}
				}
			}
			
			if(contentVersion != null)
				CacheController.cacheObjectInAdvancedCache("contentVersionCache", versionKey, contentVersion.getId(), new String[]{"contentVersion_" + contentVersion.getId(), "content_" + contentVersion.getValueObject().getContentId()}, true);
		}
		
		if(contentVersion != null)
		    deliveryContext.addUsedContentVersion("contentVersion_" + contentVersion.getId());
		
		return contentVersion;
	}
	*/

	private ContentVersion getContentVersion(Content content, Integer languageId, Integer operatingMode, DeliveryContext deliveryContext, Database db) throws Exception
    {
	    ContentVersion contentVersion = null;
		
	    getLogger().info("content:" + content.getId());
	    getLogger().info("operatingMode:" + operatingMode);
	    getLogger().info("languageId:" + languageId);

	    String versionKey = "" + content.getId() + "_" + languageId + "_" + operatingMode + "_contentVersionId";
		//System.out.println("versionKey:" + versionKey);
		
		Object object = CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", versionKey);
		//Integer contentVersionId = (Integer)CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", versionKey);
		if(object instanceof NullObject)
		{
			getLogger().info("There was an cached parentSiteNodeVO but it was null:" + object);
		}
		else if(object != null)
		{
			Integer contentVersionId = (Integer)object;
			getLogger().info("There was a cached content version id:" + contentVersionId);
		    //System.out.println("There was a cached content version id:" + contentVersionId);
		    contentVersion = (ContentVersion)getObjectWithId(ContentVersionImpl.class, contentVersionId, db);
		    //System.out.println("Loaded the version from cache instead of querying it:" + contentVersionId);
		    getLogger().info("contentVersion read");
		}
		else
		{
			//System.out.println("Querying for verson: " + versionKey); 
		    OQLQuery oql = db.getOQLQuery( "SELECT cv FROM org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl cv WHERE cv.contentId = $1 AND cv.language.languageId = $2 AND cv.stateId >= $3 AND cv.isActive = $4 ORDER BY cv.contentVersionId desc");
	    	oql.bind(content.getId());
	    	oql.bind(languageId);
	    	oql.bind(operatingMode);
	    	oql.bind(true);
	
	    	QueryResults results = oql.execute(Database.ReadOnly);
			
			if (results.hasMore()) 
	        {
	        	contentVersion = (ContentVersion)results.next();
	        	getLogger().info("found one:" + contentVersion.getId());

	        	//System.out.println("Caching content version for key:" + versionKey);
				CacheController.cacheObjectInAdvancedCache("contentVersionCache", versionKey, contentVersion.getId(), new String[]{"contentVersion_" + contentVersion.getId(), "content_" + contentVersion.getValueObject().getContentId()}, true);
	        }
			else
			{
				CacheController.cacheObjectInAdvancedCache("contentVersionCache", versionKey, new NullObject(), new String[]{"content_" + content.getId()}, true);
			}

			//if(content.getId().intValue() == 33 || content.getId().intValue() == 7 || content.getId().intValue() == 8 || content.getId().intValue() == 9)
			//	try{throw new Exception("APA");}catch(Exception e){e.printStackTrace();}
		}
		
		if(contentVersion != null)
		    deliveryContext.addUsedContentVersion("contentVersion_" + contentVersion.getId());

		getLogger().info("end getContentVersion");
		
		return contentVersion;
    }


	/**
	 * This is the most common way of getting attributes from a content. 
	 * It selects the correct contentVersion depending on the language and then gets the attribute in the xml associated.
	 */

	public String getContentAttribute(Database db, Integer contentId, Integer languageId, String attributeName, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infogluePrincipal, boolean escapeHTML) throws SystemException, Exception
	{	    	        
		return getContentAttribute(db, contentId, languageId, attributeName, siteNodeId, useLanguageFallback, deliveryContext, infogluePrincipal, escapeHTML, null);
	}
	
	/**
	 * This is the most common way of getting attributes from a content. 
	 * It selects the correct contentVersion depending on the language and then gets the attribute in the xml associated.
	 */

	public String getContentAttribute(Database db, Integer contentId, Integer languageId, String attributeName, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infogluePrincipal, boolean escapeHTML, List usedContentVersionId) throws SystemException, Exception
	{	
		//System.out.println("usedContentVersionId:" + usedContentVersionId);

	    String attributeKey = "" + contentId + "_" + languageId + "_" + attributeName + "_" + siteNodeId + "_" + useLanguageFallback + "_" + escapeHTML;
	    String versionKey = attributeKey + "_contentVersionId";
		//getLogger().info("attributeKey:" + attributeKey);
		
		//String attribute = (String)CacheController.getCachedObject("contentAttributeCache", attributeKey);
		//Integer contentVersionId = (Integer)CacheController.getCachedObject("contentAttributeCache", versionKey);
		
		String attribute = (String)CacheController.getCachedObjectFromAdvancedCache("contentAttributeCache", attributeKey);
		Integer contentVersionId = (Integer)CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", versionKey);
		
	    try
	    {

		if(attribute != null)
		{
			//getLogger().info("There was an cached content attribute:" + attribute);
			//if(contentId != null && contentId.intValue() == 3135)
			//	System.out.println("There was an cached content attribute:" + attribute);
		}
		else
		{
			//if(contentId != null && contentId.intValue() == 3135)
			//	System.out.println("No cached attribute");
			
			ContentVersionVO contentVersionVO = getContentVersionVO(db, siteNodeId, contentId, languageId, useLanguageFallback, deliveryContext, infogluePrincipal);
		   
        	if (contentVersionVO != null) 
			{
			    getLogger().info("found one:" + contentVersionVO);
				attribute = getAttributeValue(db, contentVersionVO, attributeName, escapeHTML);	
				contentVersionId = contentVersionVO.getId();
			}
			else
				attribute = "";

			CacheController.cacheObjectInAdvancedCache("contentAttributeCache", attributeKey, attribute, new String[]{"contentVersion_" + contentVersionId, "content_" + contentId}, true);
			if(contentVersionId != null)
			    CacheController.cacheObjectInAdvancedCache("contentVersionCache", versionKey, contentVersionId, new String[]{"contentVersion_" + contentVersionId, "content_" + contentId}, true);
		}
		
		//getLogger().info("Adding contentVersion:" + contentVersionId);
		deliveryContext.addUsedContentVersion("contentVersion_" + contentVersionId);

		if(usedContentVersionId != null && contentVersionId != null)
		    usedContentVersionId.add(contentVersionId);

	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();
	        throw e;
	    }
	    
		return (attribute == null) ? "" : attribute;
	}


	/**
	 * This is the most common way of getting attributes from a content. 
	 * It selects the correct contentVersion depending on the language and then gets the attribute in the xml associated.
	 */

	public String getContentAttribute(Database db, ContentVersionVO contentVersionVO, String attributeName, boolean escapeHTML) throws SystemException, Exception
	{
		String attribute = getAttributeValue(db, contentVersionVO, attributeName, escapeHTML);		
		
		return attribute;
	}

	/**
	 * Find all ContentVersionVOs that are related to the provided Category.
	 *
	 * TODO: Right now this method depends on the ContentVersion having an owningContent
	 * TODO: This is potentially bad from a performance standpoint app-wide, so a workaround may
	 * TODO: be to look up each Content for the ContentVersions after we have done everything we
	 * TODO: can to wed down the list alot, so the overhead will not be too much.
	 *
	 * @param categoryId The Category to search on
	 * @param attributeName The attribute of the Category relationship
	 * @param infoGluePrincipal The user making the request
	 * @param siteNodeId The SiteNode that the request is coming from
	 * @param languageId The Language of the request
	 * @param useLanguageFallback True is the search is to use the fallback (default) language for the Repository
	 * @return A List of ContentVersionVOs matching the Category search, that are considered valid
	 * @throws SystemException
	 */
	public List findContentVersionVOsForCategory(Database db, Integer categoryId, String attributeName, InfoGluePrincipal infoGluePrincipal, Integer siteNodeId, Integer languageId, boolean useLanguageFallback, DeliveryContext deliveryContext) throws SystemException, Exception
	{
	    deliveryContext.addUsedContent("selectiveCacheUpdateNonApplicable");
	    
		List results = findContentCategories(db, categoryId, attributeName);
		List versions = findContentVersionsForCategories(results, db);

		// Weed out irrelevant versions
		for (Iterator iter = versions.iterator(); iter.hasNext();)
		{
			ContentVersion version = (ContentVersion) iter.next();
			if(!isValidContentVersion(version, infoGluePrincipal, siteNodeId, languageId, useLanguageFallback, db, deliveryContext))
				iter.remove();
		}

		return toVOList(versions);
	}

	/**
	 * Find all ContentCategories for the given Category id and attributeName.
	 * @param categoryId The Category to find ContentCategories
	 * @param attributeName The ContentTYpeDefintion attribute name of a ContentCategory relationship.
	 * @return A List of ContentCategoryVOs for the supplied Category id.
	 * @throws SystemException If an error happens
	 */
	private List findContentCategories(Database db, Integer categoryId, String attributeName) throws SystemException, Exception
	{
		StringBuffer oql = new StringBuffer();
		oql.append("SELECT c FROM org.infoglue.cms.entities.content.impl.simple.ContentCategoryImpl c ");
		oql.append("WHERE c.category.categoryId = $1 AND c.attributeName = $2");

		ArrayList params = new ArrayList();
		params.add(categoryId);
		params.add(attributeName);
		return toVOList(executeQuery(db, oql.toString(), params));
	}

	/**
	 * Find content versions that are in the provided list of version ids. However over time this
	 * could get to be a large list, so lets weed it out initially at the database restricted
	 * on the time parameters. That should keep the lists manageable
	 *
	 * @param contentCategories A ContentCategoryVO list used to find related ContentVersions
	 * @param db A Database to execute the query against
	 * @return A List of ContentVersions that were related to the provided ContentCategories and
	 * 			fell withing the publishing time frame
	 * @throws Exception if an error happens
	 */
	private List findContentVersionsForCategories(List contentCategories, Database db) throws Exception
	{
		if(contentCategories.isEmpty())
			return Collections.EMPTY_LIST;

		/*
		StringBuffer oql = new StringBuffer();
		oql.append("SELECT c FROM org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl c ")
				.append("WHERE c.owningContent.publishDateTime <= $1 AND c.owningContent.expireDateTime >= $2 ")
				.append("AND c.contentVersionId IN LIST ").append(toVersionIdList(contentCategories));
        */
		
		StringBuffer oql = new StringBuffer();
		oql.append("SELECT c FROM org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl c ")
				.append("WHERE c.contentVersionId IN LIST ").append(toVersionIdList(contentCategories));

		ArrayList params = new ArrayList();
		//params.add(new Date());
		//params.add(new Date());
		return  executeQuery(db, oql.toString(), params);
	}

	/**
	 * Is this a valid Content item based on defined rules for publican/expiration etc.,
	 * and is it the most recent ContentVersion for this deployment. If not then we retrieved
	 * based on categories attached to an old version.
	 */
	private boolean isValidContentVersion(ContentVersion version, InfoGluePrincipal infoGluePrincipal, Integer siteNodeId, Integer languageId, boolean useLanguageFallback, Database db, DeliveryContext deliveryContext) throws Exception
	{
		//Content content = version.getOwningContent();
	    Integer contentId = version.getValueObject().getContentId();
	    getLogger().info("contentId:" + contentId);
	    
	    Content content = (MediumContentImpl)getObjectWithId(MediumContentImpl.class, contentId, db);
	    //Content content = ContentController.getContentController().getContentWithId(contentId, db);
	    
		ContentVersionVO mostRecentVersion = getContentVersionVO(db, siteNodeId, content.getContentId(), languageId, useLanguageFallback, deliveryContext, infoGluePrincipal);
		boolean isProperVersion = (mostRecentVersion != null) && (mostRecentVersion.getId().equals(version.getId()));

		boolean isValidContent = isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, false, db, deliveryContext);

		return isProperVersion && isValidContent;
	}

	/**
	 * Builds and IN list for the query to find all potentially relevant content versions.
	 */
	private String toVersionIdList(List results)
	{
		StringBuffer ids = new StringBuffer("(");
		for(Iterator iter = results.iterator(); iter.hasNext();)
			ids.append(((ContentCategoryVO) iter.next()).getContentVersionId() + (iter.hasNext()? ", " : ""));
		ids.append(")");
		return ids.toString();
	}


	/**
	 * This method returns all the assetsKeys available in a contentVersion.
	 */

	public Collection getAssetKeys(Database db, Integer contentId, Integer languageId, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		Collection assetKeys = new ArrayList();
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
        {
        	Collection assets = contentVersion.getDigitalAssets();
        	Iterator keysIterator = assets.iterator();
        	while(keysIterator.hasNext())
        	{
        		DigitalAsset asset = (DigitalAsset)keysIterator.next();
        		String assetKey = asset.getAssetKey();
            	assetKeys.add(assetKey); 		
        	}
        }
		
		return assetKeys;
	}


	/**
	 * This method is used by the getAssetUrl methods, to locate a digital asset in another
	 * languageversion. It is called in the case where no asset where found in the supplied language.
	 * 
	 * This way an image is only required to exist in one of the language versions, reducing the need for 
	 * many duplicates.
	 *  
	 */
	private DigitalAsset getLanguageIndependentAsset(Integer contentId, Integer languageId, Integer siteNodeId, Database db, String assetKey, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		DigitalAsset asset = null;
		// TODO: This method should only return a asset url depending on settings on the actual content in the future
		// or possibly a systemwide setting.
		
		// TODO: experimental
		// addition ss - 030422
		// Search digital asset among language versions.
		List langs = LanguageDeliveryController.getLanguageDeliveryController().getAvailableLanguages(db, siteNodeId);
		Iterator lit = langs.iterator();
		while (lit.hasNext())
		{
			LanguageVO langVO = (LanguageVO) lit.next();
			if (langVO.getLanguageId().compareTo(languageId)!=0)
			{
				ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, langVO.getLanguageId(), db, false, deliveryContext, infoGluePrincipal);
				if (contentVersion != null) 
				{
					DigitalAsset digitalAsset = 
						(assetKey == null) ? getLatestDigitalAsset(contentVersion) :getDigitalAssetWithKey(contentVersion, assetKey); 
					
					if(digitalAsset != null)
					{
						asset = digitalAsset;
						break;
					}
				}									
			}
		}
		return asset;			
	}

	private String getLanguageIndependentAssetUrl(Integer contentId, Integer languageId, Integer siteNodeId, Database db, String assetKey, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		String assetUrl = "";
		assetUrl = urlComposer.composeDigitalAssetUrl("", "", deliveryContext); 
		
		DigitalAsset digitalAsset = getLanguageIndependentAsset(contentId, languageId, siteNodeId, db, assetKey, deliveryContext, infoGluePrincipal);
		if(digitalAsset != null)
		{
			String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();
			
			int i = 0;
			File masterFile = null;
			String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
			while(filePath != null)
			{
			    if(masterFile == null)
			        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				else
				    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(masterFile, fileName, filePath);
			    
			    i++;
				filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
			}
			//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
			//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
		
			SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
			String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
			if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
				dnsName = siteNode.getRepository().getDnsName();
				
			//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + fileName;
			assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, fileName, deliveryContext); 
		}
		return assetUrl;	
	}


	private String getLanguageIndependentAssetThumbnailUrl(Integer contentId, Integer languageId, Integer siteNodeId, Database db, String assetKey, int width, int height, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		String assetUrl = "";
		assetUrl = urlComposer.composeDigitalAssetUrl("", "", deliveryContext); 
		
		DigitalAsset digitalAsset = getLanguageIndependentAsset(contentId, languageId, siteNodeId, db, assetKey, deliveryContext, infoGluePrincipal);
		if(digitalAsset != null)
		{
			String fileName = digitalAsset.getAssetFileName();
			String thumbnailFileName = "thumbnail_" + width + "_" + height + "_" + fileName;

			int i = 0;
			File masterFile = null;
			String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
			while(filePath != null)
			{
			    if(masterFile == null)
			        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
				else
				    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
			    
			    i++;
				filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
			}

			//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
			//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(digitalAsset, fileName, thumbnailFileName, filePath, width, height);
			
			SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
			String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
			if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
				dnsName = siteNode.getRepository().getDnsName();
				
			//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + thumbnailFileName;
			assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, thumbnailFileName, deliveryContext); 
		}
		return assetUrl;	
	}


	/**
	 * This method returns the id of the digital asset. 
	 * It selects the correct contentVersion depending on the language and then gets the digitalAsset associated.
	 */

	public Integer getDigitalAssetId(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
	    Integer digitalAssetId = null;
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
        {
        	DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
			
			if(digitalAsset != null)
			{
			    digitalAssetId = digitalAsset.getId();
			}
        }
            		
		return digitalAssetId;
	}

	/**
	 * This is the basic way of getting an asset-url for a content. 
	 * It selects the correct contentVersion depending on the language and then gets the digitalAsset associated.
	 * If the asset is cached on disk it returns that path imediately it's ok - otherwise it dumps it fresh.
	 */

	public String getAssetUrl(Database db, Integer contentId, Integer languageId, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
	    String assetCacheKey = "" + languageId + "_" + contentId + "_" + siteNodeId + "_" + useLanguageFallback;
		getLogger().info("assetCacheKey:" + assetCacheKey);
		String cacheName = "assetUrlCache";
		String cachedAssetUrl = (String)CacheController.getCachedObject(cacheName, assetCacheKey);
		if(cachedAssetUrl != null)
		{
			getLogger().info("There was an cached cachedAssetUrl:" + cachedAssetUrl);
			return cachedAssetUrl;
		}
		
		String assetUrl = "";
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
        {
        	DigitalAsset digitalAsset = getLatestDigitalAsset(contentVersion);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();

				int i = 0;
				File masterFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
				    if(masterFile == null)
				        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				    else
				        DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(masterFile, fileName, filePath);
					
				    i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				
				SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
				String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
				if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
					dnsName = siteNode.getRepository().getDnsName();

				//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + fileName;
				assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, fileName, deliveryContext); 
			}
			else
			{
				assetUrl = getLanguageIndependentAssetUrl(contentId, languageId, siteNodeId, db, null, deliveryContext, infoGluePrincipal);
			}
        }
            		
        CacheController.cacheObject(cacheName, assetCacheKey, assetUrl);
        
		return assetUrl;
	}



	/**
	 * This is the basic way of getting an asset-url for a content. 
	 * It selects the correct contentVersion depending on the language and then gets the digitalAsset associated with the key.
	 * If the asset is cached on disk it returns that path imediately it's ok - otherwise it dumps it fresh.
	 */

	public String getAssetUrl(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
	    SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId, db);
	    //String assetCacheKey = "" + languageId + "_" + contentId + "_" + siteNodeId + "_" + assetKey + "_" + useLanguageFallback + "_" + deliveryContext.getUseFullUrl();
	    String assetCacheKey = "" + languageId + "_" + contentId + "_" + siteNodeVO.getRepositoryId() + "_" + assetKey + "_" + useLanguageFallback + "_" + deliveryContext.getUseFullUrl();
		getLogger().info("assetCacheKey:" + assetCacheKey);
	    //System.out.println("assetCacheKey:" + assetCacheKey);
	    
		String cacheName = "assetUrlCache";
		String cachedAssetUrl = (String)CacheController.getCachedObject(cacheName, assetCacheKey);
		if(cachedAssetUrl != null)
		{
		    getLogger().info("There was an cached cachedAssetUrl:" + cachedAssetUrl);
			return cachedAssetUrl;
		}
		
		String assetUrl = "";
		assetUrl = urlComposer.composeDigitalAssetUrl("", "", deliveryContext); 
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		ContentVO contentVO = this.getContentVO(db, contentId, deliveryContext);
		if (contentVersion != null) 
        {
        	DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();

				int i = 0;
				File masterFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
				    if(masterFile == null)
				        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);	
					else
					    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(masterFile, fileName, filePath);
				    i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				
				SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
				String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
				if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
					dnsName = siteNode.getRepository().getDnsName();
					
				//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + fileName;
				assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, fileName, deliveryContext); 
			}
			else if(useLanguageFallback)
			{
				assetUrl = getLanguageIndependentAssetUrl(contentId, languageId, siteNodeId, db, assetKey, deliveryContext, infoGluePrincipal);
			}
		}				
		else if(useLanguageFallback && languageId.intValue() != LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForRepository(contentVO.getRepositoryId(), db).getId().intValue())
		{
	    	contentVersion = this.getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		    
	    	getLogger().info("contentVersion:" + contentVersion);
			if(contentVersion != null)
			{
            	DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
				
				if(digitalAsset != null)
				{
					String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();
					
					int i = 0;
					File masterFile = null;
					String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
					while(filePath != null)
					{
					    if(masterFile == null)
					        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
						else
						    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(masterFile, fileName, filePath);

						i++;
						filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
					}

					//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
					//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
					
					SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
					String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
					if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
						dnsName = siteNode.getRepository().getDnsName();
						
					//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + fileName;
					assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, fileName, deliveryContext); 
				}
				else if(useLanguageFallback)
				{
					assetUrl = getLanguageIndependentAssetUrl(contentId, languageId, siteNodeId, db, assetKey, deliveryContext, infoGluePrincipal);
				}
			}
		}
			
        CacheController.cacheObject(cacheName, assetCacheKey, assetUrl);
        
        return assetUrl;
	}
	
		
	
	/**
	 * This is the basic way of getting an asset-url for a content. 
	 * It selects the correct contentVersion depending on the language and then gets the digitalAsset associated.
	 * If the asset is cached on disk it returns that path imediately it's ok - otherwise it dumps it fresh.
	 */

	public String getAssetThumbnailUrl(Database db, Integer contentId, Integer languageId, Integer siteNodeId, boolean useLanguageFallback, int width, int height, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
	    String assetCacheKey = "" + languageId + "_" + contentId + "_" + siteNodeId + "_" + useLanguageFallback + "_" + width + "_" + height;
		getLogger().info("assetCacheKey:" + assetCacheKey);
		String cacheName = "assetThumbnailUrlCache";
		String cachedAssetUrl = (String)CacheController.getCachedObject(cacheName, assetCacheKey);
		if(cachedAssetUrl != null)
		{
			getLogger().info("There was an cached cachedAssetUrl:" + cachedAssetUrl);
			return cachedAssetUrl;
		}
		
		String assetUrl = "";
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
		{
			DigitalAsset digitalAsset = getLatestDigitalAsset(contentVersion);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();
				String thumbnailFileName = "thumbnail_" + width + "_" + height + "_" + fileName;

				int i = 0;
				File masterFile = null;
				File masterThumbFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
				    if(masterFile == null)
				        masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
					else
					    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(masterFile, fileName, filePath);
				    
				    if(masterThumbFile == null)
				        masterThumbFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
				    else
				        DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
				    
					i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(digitalAsset, fileName, thumbnailFileName, filePath, width, height);
				
				SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
				String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
				if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
					dnsName = siteNode.getRepository().getDnsName();

				//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + thumbnailFileName;
				assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, thumbnailFileName, deliveryContext); 
			}
			else
			{
				assetUrl = getLanguageIndependentAssetThumbnailUrl(contentId, languageId, siteNodeId, db, null, width, height, deliveryContext, infoGluePrincipal);
			}
		}
		
		CacheController.cacheObject(cacheName, assetCacheKey, assetUrl);
		
		return assetUrl;
	}



	/**
	 * This is the basic way of getting an asset-url for a content. 
	 * It selects the correct contentVersion depending on the language and then gets the digitalAsset associated with the key.
	 * If the asset is cached on disk it returns that path imediately it's ok - otherwise it dumps it fresh.
	 */

	public String getAssetThumbnailUrl(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, int width, int height, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
	    String assetCacheKey = "" + languageId + "_" + contentId + "_" + siteNodeId + "_" + assetKey + "_" + useLanguageFallback + "_" + width + "_" + height;
		getLogger().info("assetCacheKey:" + assetCacheKey);
		String cacheName = "assetThumbnailUrlCache";
		String cachedAssetUrl = (String)CacheController.getCachedObject(cacheName, assetCacheKey);
		if(cachedAssetUrl != null)
		{
			getLogger().info("There was an cached cachedAssetUrl:" + cachedAssetUrl);
			return cachedAssetUrl;
		}

		String assetUrl = "";
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
		{
			DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getDigitalAssetId() + "_" + digitalAsset.getAssetFileName();
				String thumbnailFileName = "thumbnail_" + width + "_" + height + "_" + fileName;

				int i = 0;
				File masterFile = null;
				File masterThumbFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
				    if(masterFile == null)
						masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
					else
						DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				    
				    if(masterThumbFile == null)
				        masterThumbFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
					else
					    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(fileName, thumbnailFileName, filePath, width, height);
					
					i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAsset(digitalAsset, fileName, filePath);
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpDigitalAssetThumbnail(digitalAsset, fileName, thumbnailFileName, filePath, width, height);
				
				SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
				String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
				if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
					dnsName = siteNode.getRepository().getDnsName();
				
				//assetUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + thumbnailFileName;
				assetUrl = urlComposer.composeDigitalAssetUrl(dnsName, thumbnailFileName, deliveryContext); 
			}
			else
			{
				assetUrl = getLanguageIndependentAssetThumbnailUrl(contentId, languageId, siteNodeId, db, assetKey, width, height, deliveryContext, infoGluePrincipal);
			}
			
		}				
		
		CacheController.cacheObject(cacheName, assetCacheKey, assetUrl);
		
		return assetUrl;
	}
	


	/*
	 * getAssetFileSize. Prelimenary, we should rather supply a assetvo to the template. 
	 */
	 
	public Integer getAssetFileSize(Database db, Integer contentId, Integer languageId, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{ 
		return getAssetFileSize(db, contentId, languageId, null, siteNodeId, useLanguageFallback, deliveryContext, infoGluePrincipal); 
	}
	
	public Integer getAssetFileSize(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		Integer fileSize = null;
	
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
		{
			DigitalAsset digitalAsset =
				(assetKey == null) ? getLatestDigitalAsset(contentVersion) : getDigitalAssetWithKey(contentVersion, assetKey); 
			
			if(digitalAsset == null)
				digitalAsset = getLanguageIndependentAsset(contentId, languageId, siteNodeId, db, assetKey, deliveryContext, infoGluePrincipal);
				
			if(digitalAsset != null)
			{
				fileSize = digitalAsset.getAssetFileSize();
			}								
		}				
            
		return fileSize;
	}
	

	/**
	 * This method deliveres a String with the URL to the base path of the directory resulting from 
	 * an unpacking of a uploaded zip-digitalAsset.
	 */

	public String getArchiveBaseUrl(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		String archiveBaseUrl = null;
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
        {
        	DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getAssetFileName();
				
				int i = 0;
				File masterFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
					File unzipDirectory = new File(filePath + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
					unzipDirectory.mkdirs();
					
					if(masterFile == null)
					    masterFile = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndUnzipDigitalAsset(digitalAsset, fileName, filePath, unzipDirectory);
					else
					    DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndUnzipDigitalAsset(masterFile, fileName, filePath, unzipDirectory);
					    
					i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//File unzipDirectory = new File(filePath + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
				//unzipDirectory.mkdirs();
				//DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndUnzipDigitalAsset(digitalAsset, fileName, filePath, unzipDirectory);
				
				SiteNode siteNode = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getSiteNode(db, siteNodeId);
				String dnsName = CmsPropertyHandler.getProperty("webServerAddress");
				if(siteNode != null && siteNode.getRepository().getDnsName() != null && !siteNode.getRepository().getDnsName().equals(""))
					dnsName = siteNode.getRepository().getDnsName();
					
				//archiveBaseUrl = dnsName + "/" + CmsPropertyHandler.getProperty("digitalAssetBaseUrl") + "/" + fileName.substring(0, fileName.lastIndexOf("."));
				archiveBaseUrl = urlComposer.composeDigitalAssetUrl(dnsName, fileName.substring(0, fileName.lastIndexOf(".")), deliveryContext); 
			}
        }				
		
		return archiveBaseUrl;
	}

	public Vector getArchiveEntries(Database db, Integer contentId, Integer languageId, String assetKey, Integer siteNodeId, boolean useLanguageFallback, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal) throws SystemException, Exception
	{
		Vector entries = null;
		
		ContentVersion contentVersion = getContentVersion(siteNodeId, contentId, languageId, db, useLanguageFallback, deliveryContext, infoGluePrincipal);
		if (contentVersion != null) 
		{
			DigitalAsset digitalAsset = getDigitalAssetWithKey(contentVersion, assetKey);
			
			if(digitalAsset != null)
			{
				String fileName = digitalAsset.getAssetFileName();

				int i = 0;
				File masterFile = null;
				String filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				while(filePath != null)
				{
					File unzipDirectory = new File(filePath + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
					unzipDirectory.mkdirs();
					
					if(masterFile == null)
					{
					    entries = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndGetZipEntries(digitalAsset, fileName, filePath, unzipDirectory);
						masterFile = new File(filePath + File.separator + fileName);
					}					
					else
					{
					    entries = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndGetZipEntries(masterFile, fileName, filePath, unzipDirectory);
					}
					
					i++;
					filePath = CmsPropertyHandler.getProperty("digitalAssetPath." + i);
				}

				//String filePath = CmsPropertyHandler.getProperty("digitalAssetPath");
				//File unzipDirectory = new File(filePath + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
				//unzipDirectory.mkdirs();
				//entries = DigitalAssetDeliveryController.getDigitalAssetDeliveryController().dumpAndGetZipEntries(digitalAsset, fileName, filePath, unzipDirectory);
			}
		}				
		
		return entries;
	}


	
	/**
	 * Returns the digital asset for a contentversion that has a certain key.
	 */
	
	private DigitalAsset getDigitalAssetWithKey(ContentVersion contentVersion, String assetKey)
	{
		Collection digitalAssets = contentVersion.getDigitalAssets();
		Iterator iterator = digitalAssets.iterator();
		
		while(iterator.hasNext())
		{  
			DigitalAsset currentDigitalAsset = (DigitalAsset)iterator.next();	
				
			if(currentDigitalAsset != null && currentDigitalAsset.getAssetKey().equalsIgnoreCase(assetKey))
			{
				return currentDigitalAsset;
			}
		}

		return null;
	}

	/**
	 * Returns the latest digital asset for a contentversion.
	 */
	
	private DigitalAsset getLatestDigitalAsset(ContentVersion contentVersion)
	{
		Collection digitalAssets = contentVersion.getDigitalAssets();
		Iterator iterator = digitalAssets.iterator();
		
		DigitalAsset digitalAsset = null;
		while(iterator.hasNext())
		{
			DigitalAsset currentDigitalAsset = (DigitalAsset)iterator.next();	
			if(digitalAsset == null || currentDigitalAsset.getDigitalAssetId().intValue() > digitalAsset.getDigitalAssetId().intValue())
				digitalAsset = currentDigitalAsset;
		}
		return digitalAsset;
	}
	
	


	/**
	 * This method fetches a value from the xml that is the contentVersions Value. If the 
	 * contentVersioVO is null the contentVersion has not been created yet and no values are present.
	 */
	public String getAttributeValue(Database db, ContentVersionVO contentVersionVO, String key, boolean escapeHTML)
	{
		String value = "";
		if(contentVersionVO != null)
		{
			try
	        {
	        	String xml = contentVersionVO.getVersionValue();
	        	
	        	int startTagIndex = xml.indexOf("<" + key + ">");
	        	int endTagIndex   = xml.indexOf("]]></" + key + ">");
	        	
	        	if(startTagIndex > 0 && startTagIndex < xml.length() && endTagIndex > startTagIndex && endTagIndex <  xml.length())
		        	value = xml.substring(startTagIndex + key.length() + 11, endTagIndex);

	        	if(escapeHTML)
	        	    value = formatter.escapeHTML(value);
	        	
	        	/*
		        InputSource inputSource = new InputSource(new StringReader(contentVersionVO.getVersionValue()));
				
				DOMParser parser = new DOMParser(); 
				parser.parse(inputSource);
				Document document = parser.getDocument();
				
				NodeList nl = document.getDocumentElement().getChildNodes();
				Node n = nl.item(0);
				
				nl = n.getChildNodes();
				for(int i=0; i<nl.getLength(); i++)
				{
					n = nl.item(i);
					if(n.getNodeName().equalsIgnoreCase(key))
					{
						value = n.getFirstChild().getNodeValue();
						getLogger().warn("Getting value: " + value);

						break;
					}
				}		        	
	        	*/
	        } 
	        catch(Exception e)
	        {
	        	getLogger().error("An error occurred so we should not return the attribute value:" + e, e);
	        }
		}

		return value;
	}
	

	/**
	 * This method returns a sorted list of childContents to a content ordered by the given attribute in the direction given.
	 */

	public List getChildContents(Database db, InfoGluePrincipal infoGluePrincipal, Integer contentId, Integer languageId, boolean useLanguageFallback, boolean includeFolders, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		List childContents = new ArrayList();
	
		getChildContents(infoGluePrincipal, childContents, contentId, languageId, useLanguageFallback, 0, false, includeFolders, 1, db, deliveryContext);
	
		return childContents;
	}
		

	/**
	 * This method returns a sorted list of childContents to a content ordered by the given attribute in the direction given.
	 */
	
	public List getSortedChildContents(InfoGluePrincipal infoGluePrincipal, Integer languageId, Integer contentId, Integer siteNodeId, Database db, boolean searchRecursive, Integer maximumNumberOfLevels, String sortAttribute, String sortOrder, boolean useLanguageFallback, boolean includeFolders, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		
		String sortedChildContentsKey = "" + infoGluePrincipal.getName() + "_" + languageId + "_" + contentId + "_" + siteNodeId + "_" + searchRecursive + "_" + maximumNumberOfLevels + "_" + sortAttribute + "_" + sortOrder + "_" + useLanguageFallback + "_" + includeFolders;
		getLogger().info("sortedChildContentsKey:" + sortedChildContentsKey);
		String cacheName = "sortedChildContentsCache";
		List cachedSortedContentVOList = (List)CacheController.getCachedObject(cacheName, sortedChildContentsKey);
		if(cachedSortedContentVOList != null)
		{
			getLogger().info("There was an cached content cachedSortedContentVOList:" + cachedSortedContentVOList.size());
			return cachedSortedContentVOList;
		}
		
		List sortedContentVOList = new ArrayList();
		
		List unsortedChildren = getChildContents(infoGluePrincipal, languageId, useLanguageFallback, contentId, siteNodeId, searchRecursive, maximumNumberOfLevels, db, includeFolders, deliveryContext);
		
		List sortedContents   = sortContents(db, unsortedChildren, languageId, siteNodeId, sortAttribute, sortOrder, useLanguageFallback, includeFolders, deliveryContext, infoGluePrincipal);

		Iterator boundContentsIterator = sortedContents.iterator();
		while(boundContentsIterator.hasNext())
		{
			Content content = (Content)boundContentsIterator.next();
			sortedContentVOList.add(content.getValueObject());
		}
		
		CacheController.cacheObject(cacheName, sortedChildContentsKey, sortedContentVOList);
			
		return sortedContentVOList;
	}
	


	/**
	 * This method returns the contentTypeDefinitionVO of the content sent in.
	 */
	
	public ContentTypeDefinitionVO getContentTypeDefinitionVO(Database db, Integer contentId) throws SystemException, Exception
	{
		ContentTypeDefinitionVO contentTypeDefinitionVO = null;
		
		Content content = (Content)getObjectWithId(ContentImpl.class, contentId, db); 
		contentTypeDefinitionVO = content.getContentTypeDefinition().getValueObject();       
		
		return contentTypeDefinitionVO;
	}


	/**
	 * This method returns a list of children to the content given. It is mostly used to get all 
	 * children a folder has.
	 */
	
	private List getChildContents(InfoGluePrincipal infoGluePrincipal, Integer languageId, boolean useLanguageFallback, Integer contentId, Integer siteNodeId, boolean searchRecursive, Integer maximumNumberOfLevels, Database db, boolean includeFolders, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		List contents = new ArrayList();
		
		getChildContents(infoGluePrincipal, contents, contentId, languageId, useLanguageFallback, 0, searchRecursive, maximumNumberOfLevels.intValue(), db, includeFolders, deliveryContext);
		
		return contents;
	}
	

	/**
	 * This method recurses into the dept of the content-children and fills the list of contents.
	 */
	
	private void getChildContents(InfoGluePrincipal infoGluePrincipal, List contents, Integer contentId, Integer languageId, boolean useLanguageFallback, int currentLevel, boolean searchRecursive, int maximumNumberOfLevels, Database db, boolean includeFolders, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		/*
		OQLQuery oql = db.getOQLQuery("SELECT contentVersion FROM org.infoglue.cms.entities.content.impl.simple.ContentVersionImpl contentVersion WHERE contentVersion.stateId = $1 AND contentVersion.isActive = $2 AND contentVersion.owningContent.parentContent.contentId = $3");
		oql.bind(getOperatingMode());
		oql.bind(new Boolean(true));
    	oql.bind(contentId);
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			ContentVersion contentVersion = (ContentVersion)results.next();
    		Content content = contentVersion.getOwningContent();
    		
			if(searchRecursive && currentLevel < maximumNumberOfLevels)
				getChildContents(contents, content.getContentId(), currentLevel + 1, searchRecursive, maximumNumberOfLevels, db);
    
			if(isValidContent(content, db))
			{
				if(!contents.contains(content))
					contents.add(content);
			}
		}
		*/
		
		deliveryContext.addUsedContent("selectiveCacheUpdateNonApplicable");

		OQLQuery oql = db.getOQLQuery("SELECT content FROM org.infoglue.cms.entities.content.impl.simple.ContentImpl content WHERE content.parentContent.contentId = $1 ORDER BY content.contentId");
    	oql.bind(contentId);
    	
    	QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
        {
        	Content content = (Content)results.next();
    
        	if(searchRecursive && currentLevel < maximumNumberOfLevels)
	        	getChildContents(infoGluePrincipal, contents, content.getContentId(), languageId, useLanguageFallback, currentLevel + 1, searchRecursive, maximumNumberOfLevels, db, includeFolders, deliveryContext);
    
    		if(isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, includeFolders, db, deliveryContext))
    		{
   				contents.add(content);
    		}
        }
	}

	
	/** 
	 * This method recurses into the dept of the content-children and fills the list of contents.
	 */
	
	private void getChildContents(InfoGluePrincipal infoGluePrincipal, List contents, Integer contentId, Integer languageId, boolean useLanguageFallback, int currentLevel, boolean searchRecursive, boolean includeFolders, int maximumNumberOfLevels, Database db, DeliveryContext deliveryContext) throws SystemException, Exception
	{
		deliveryContext.addUsedContent("selectiveCacheUpdateNonApplicable");

		OQLQuery oql = db.getOQLQuery( "SELECT content FROM org.infoglue.cms.entities.content.impl.simple.ContentImpl content WHERE content.parentContent.contentId = $1 ORDER BY content.contentId");
		oql.bind(contentId);
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			Content content = (Content)results.next();
    
			if(searchRecursive && currentLevel < maximumNumberOfLevels)
				getChildContents(infoGluePrincipal, contents, content.getContentId(), languageId, useLanguageFallback, currentLevel + 1, searchRecursive, includeFolders, maximumNumberOfLevels, db, deliveryContext);
    
    		if(content.getIsBranch().booleanValue() && includeFolders && isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, includeFolders, db, deliveryContext))
				contents.add(content);
    		else if(isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, includeFolders, db, deliveryContext))
				contents.add(content);
		}
	}
	
	/**
	 * This method validates that right now is between publishdate and expiredate.
	 */
	
	private boolean isValidOnDates(Date publishDate, Date expireDate)
	{
		boolean isValid = true;
		Date now = new Date();
		
		if(publishDate.after(now) || expireDate.before(now))
			isValid = false;
		
		return isValid;
	}		

	/**
	 * Returns if a content is between dates and has a content version suitable for this delivery mode.
	 * @throws Exception
	 */
/*	
	public boolean isValidContent(Integer contentId, InfoGluePrincipal infoGluePrincipal, Database db) throws Exception
	{
		boolean isValidContent = false;
		
		Content content = (Content)getObjectWithId(ContentImpl.class, contentId, db); 
		isValidContent = isValidContent(content, db);
    	
		return isValidContent;					
	}
*/
	/**
	 * Returns if a content is between dates and has a content version suitable for this delivery mode.
	 * @throws Exception
	 */

	public boolean isValidContent(Database db, Integer contentId, Integer languageId, boolean useLanguageFallback, boolean includeFolders, InfoGluePrincipal infoGluePrincipal, DeliveryContext deliveryContext) throws Exception
	{
	    boolean isValidContent = false;
		
		Content content = (Content)getObjectWithId(ContentImpl.class, contentId, db); 
		isValidContent = isValidContent(infoGluePrincipal, content, languageId, useLanguageFallback, includeFolders, db, deliveryContext);
		
		return isValidContent;					
	}
	
	/**
	 * Returns if a content is between dates and has a content version suitable for this delivery mode.
	 * @throws Exception
	 */

	public boolean isValidContent(InfoGluePrincipal infoGluePrincipal, Content content, Integer languageId, boolean useLanguageFallBack, boolean includeFolders, Database db, DeliveryContext deliveryContext) throws Exception
	{
	    boolean isValidContent = false;
		if(infoGluePrincipal == null)
		    throw new SystemException("There was no anonymous user found in the system. There must be - add the user anonymous/anonymous and try again.");
		
		if(content.getContentTypeDefinition() != null && content.getContentTypeDefinition().getName().equalsIgnoreCase("Meta info"))
			return true;

		getLogger().info("content:" + content.getName());
		
		Integer protectedContentId = getProtectedContentId(db, content);
		getLogger().info("IsProtected:" + protectedContentId);
	    
		if(protectedContentId != null && !AccessRightController.getController().getIsPrincipalAuthorized(db, infoGluePrincipal, "Content.Read", protectedContentId.toString()))
		{
		    return false;
		}
			    
		if(!includeFolders && content.getIsBranch().booleanValue() && isValidOnDates(content.getPublishDateTime(), content.getExpireDateTime()))
		{
			isValidContent = true; 
		}
		else if(isValidOnDates(content.getPublishDateTime(), content.getExpireDateTime()))
		{
		    ContentVersion contentVersion = getContentVersion(content, languageId, getOperatingMode(), deliveryContext, db);

			Integer repositoryId = null;
			Repository repository = content.getRepository();
			if(repository == null)
			{
			    if(content instanceof MediumContentImpl)
			        repositoryId = ((MediumContentImpl)content).getRepositoryId();
			    else if(content instanceof SmallContentImpl)
			        repositoryId = ((SmallContentImpl)content).getRepositoryId();
			}
			else
			{
			    repositoryId = repository.getId();
			}
			
			if(contentVersion == null && useLanguageFallBack && repositoryId != null)
			{
				LanguageVO masterLanguage = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForRepository(repositoryId, db);
				if(masterLanguage != null && !masterLanguage.getId().equals(languageId))
					contentVersion = getContentVersion(content, masterLanguage.getId(), getOperatingMode(), deliveryContext, db);
			}

			if(contentVersion != null)
				isValidContent = true;
			
			/*
			Collection versions = content.getContentVersions();
			Iterator versionsIterator = versions.iterator();
			while(versionsIterator.hasNext())
			{
				ContentVersion contentVersion = (ContentVersion)versionsIterator.next();

				if(contentVersion.getIsActive().booleanValue() && contentVersion.getStateId().intValue() >= getOperatingMode().intValue())
					isValidContent = true;
			}
			*/
		}
    	
		if(isValidContent && !content.getExpireDateTime().before(new Date()))
		{
		    Date expireDateTimeCandidate = content.getExpireDateTime();
		    if(CacheController.expireDateTime == null || expireDateTimeCandidate.before(CacheController.expireDateTime))
			{
			    CacheController.expireDateTime = expireDateTimeCandidate;
			}
		}
		else if(content.getPublishDateTime().after(new Date())) //If it's a publish date to come we consider it
		{
		    Date publishDateTimeCandidate = content.getPublishDateTime();
		    if(CacheController.publishDateTime == null || publishDateTimeCandidate.after(CacheController.publishDateTime))
			{
			    CacheController.publishDateTime = publishDateTimeCandidate;
			}
		}
	    
		return isValidContent;					
	}

	
	
	/**
	 * This method returns the id of the content that is protected if any. Looks recursive upwards.
	 */
	
	public Integer getProtectedContentId(Database db, Integer contentId) throws SystemException, Exception
	{
	    Integer protectedContentId = null;
		
	    Content content = (Content)getObjectWithId(ContentImpl.class, contentId, db);
    	protectedContentId = getProtectedContentId(db, content);
        		
		return protectedContentId;
	}

	
	/**
	 * This method returns the id of the content that is protected if any. Looks recursive upwards.
	 */
	
	public Integer getProtectedContentId(Database db, Content content)
	{
		Integer protectedContentId = null;
		
		try
		{
			getLogger().info("content:" + content.getId() + ":" + content.getIsProtected());

			if(content != null && content.getIsProtected() != null)
			{	
				if(content.getIsProtected().intValue() == ContentVO.NO.intValue())
					protectedContentId = null;
				else if(content.getIsProtected().intValue() == ContentVO.YES.intValue())
					protectedContentId = content.getId();
				else if(content.getIsProtected().intValue() == ContentVO.INHERITED.intValue())
				{
					Content parentContent = null; //= content.getParentContent();
					if(content instanceof MediumContentImpl)
					{
						Integer parentContentId = ((MediumContentImpl)content).getParentContentId();
						if(parentContentId != null)
							parentContent = (MediumContentImpl)getObjectWithId(MediumContentImpl.class, parentContentId, db);
					}
					else if(content instanceof SmallContentImpl)
					{
						Integer parentContentId = ((SmallContentImpl)content).getParentContentId();
						if(parentContentId != null)
							parentContent = (SmallContentImpl)getObjectWithId(SmallContentImpl.class, parentContentId, db);
					}
					else if(content instanceof ContentImpl)
					{
						parentContent = content.getParentContent();
					}
					
					if(parentContent != null)
					{
						protectedContentId = getProtectedContentId(db, parentContent); 
					}
				}
			}

		}
		catch(Exception e)
		{
			getLogger().warn("An error occurred trying to get if the siteNodeVersion has disabled pageCache:" + e.getMessage(), e);
		}
				
		return protectedContentId;
	}

	
	/**
	 * This method just sorts the list of qualifyers on sortOrder.
	 */
	
	public List sortContents(Database db, Collection contents, Integer languageId, Integer siteNodeId, String sortAttributeName, String sortOrder, boolean useLanguageFallback, boolean includeFolders, DeliveryContext deliveryContext, InfoGluePrincipal infoGluePrincipal)
	{
		List sortedContents = new ArrayList();

		try
		{		
			Iterator iterator = contents.iterator();
			while(iterator.hasNext())
			{
				Content content = (Content)iterator.next();
				if(includeFolders || content.getIsBranch().booleanValue() == false)
				{
					int index = 0;
					Iterator sortedListIterator = sortedContents.iterator();
					while(sortedListIterator.hasNext())
					{
						Content sortedContent = (Content)sortedListIterator.next();
						
						//Here we sort on date if the dates on a container is wanted 
						if(sortAttributeName.equalsIgnoreCase("publishDateTime"))
						{
							Date date       = content.getPublishDateTime();
							Date sortedDate = sortedContent.getPublishDateTime();
							if(date != null && sortedDate != null && sortOrder.equalsIgnoreCase("asc") && date.before(sortedDate))
					    	{
					    		break;
					    	}
					    	else if(date != null && sortedDate != null && sortOrder.equalsIgnoreCase("desc") && date.after(sortedDate))
					    	{
					    		break;
					    	}
					    	
						}
						else if(sortAttributeName.equalsIgnoreCase("expireDateTime"))
						{
							Date date       = content.getExpireDateTime();
							Date sortedDate = sortedContent.getExpireDateTime();
							if(date != null && sortedDate != null && sortOrder.equalsIgnoreCase("asc") && date.before(sortedDate))
					    	{
					    		break;
					    	}
					    	else if(date != null && sortedDate != null && sortOrder.equalsIgnoreCase("desc") && date.after(sortedDate))
					    	{
					    		break;
					    	}
					    	
						}
						else
						{
							String contentAttribute       = this.getContentAttribute(db, content.getId(), languageId, sortAttributeName, siteNodeId, useLanguageFallback, deliveryContext, infoGluePrincipal, false);
							String sortedContentAttribute = this.getContentAttribute(db, sortedContent.getId(), languageId, sortAttributeName, siteNodeId, useLanguageFallback, deliveryContext, infoGluePrincipal, false);
							if(contentAttribute != null && sortedContentAttribute != null && sortOrder.equalsIgnoreCase("asc") && contentAttribute.compareTo(sortedContentAttribute) < 0)
					    	{
					    		break;
					    	}
					    	else if(contentAttribute != null && sortedContentAttribute != null && sortOrder.equalsIgnoreCase("desc") && contentAttribute.compareTo(sortedContentAttribute) > 0)
					    	{
					    		break;
					    	}
						}				    	
				    	index++;
					}
					sortedContents.add(index, content);
				}			    					
			}
		}
		catch(Exception e)
		{
			getLogger().warn("The sorting of contents failed:" + e.getMessage(), e);
		}
			
		return sortedContents;
	}
	

}