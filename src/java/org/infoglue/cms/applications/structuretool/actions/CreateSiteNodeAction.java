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

package org.infoglue.cms.applications.structuretool.actions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.PageTemplateController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeTypeDefinitionController;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.sorters.ReflectionComparator;

/**
 * This action represents the CreateSiteNode Usecase.
 */

public class CreateSiteNodeAction extends InfoGlueAbstractAction
{
    private final static Logger logger = Logger.getLogger(CreateSiteNodeAction.class.getName());

    private Integer siteNodeId;
    private String name;
    private Boolean isBranch;
    private Integer parentSiteNodeId;
    private Integer siteNodeTypeDefinitionId;
    private Integer pageTemplateContentId;
    private Integer repositoryId;
   	private ConstraintExceptionBuffer ceb;
   	private SiteNodeVO siteNodeVO;
   	private SiteNodeVO newSiteNodeVO;
   	private String sortProperty = "name";
  
  	public CreateSiteNodeAction()
	{
		this(new SiteNodeVO());
	}
	
	public CreateSiteNodeAction(SiteNodeVO siteNodeVO)
	{
		this.siteNodeVO = siteNodeVO;
		this.ceb = new ConstraintExceptionBuffer();			
	}	

	public void setParentSiteNodeId(Integer parentSiteNodeId)
	{
		this.parentSiteNodeId = parentSiteNodeId;
	}

	public Integer getParentSiteNodeId()
	{
		return this.parentSiteNodeId;
	}

	public void setRepositoryId(Integer repositoryId)
	{
		this.repositoryId = repositoryId;
	}

	public Integer getRepositoryId()
	{
		return this.repositoryId;
	}

	public void setSiteNodeTypeDefinitionId(Integer siteNodeTypeDefinitionId)
	{
		this.siteNodeTypeDefinitionId = siteNodeTypeDefinitionId;
	}

	public Integer getSiteNodeTypeDefinitionId()
	{
		return this.siteNodeTypeDefinitionId;
	}	
	
    public java.lang.String getName()
    {
        return this.siteNodeVO.getName();
    }

    public String getPublishDateTime()
    {    		
        return new VisualFormatter().formatDate(this.siteNodeVO.getPublishDateTime(), "yyyy-MM-dd HH:mm");
    }
        
    public String getExpireDateTime()
    {
        return new VisualFormatter().formatDate(this.siteNodeVO.getExpireDateTime(), "yyyy-MM-dd HH:mm");
    }

	public Boolean getIsBranch()
	{
 		return this.siteNodeVO.getIsBranch();
	}    
            
    public void setName(java.lang.String name)
    {
        this.siteNodeVO.setName(name);
    }
    	
    public void setPublishDateTime(String publishDateTime)
    {
       	logger.info("publishDateTime:" + publishDateTime);
   		this.siteNodeVO.setPublishDateTime(new VisualFormatter().parseDate(publishDateTime, "yyyy-MM-dd HH:mm"));
    }

    public void setExpireDateTime(String expireDateTime)
    {
       	logger.info("expireDateTime:" + expireDateTime);
       	this.siteNodeVO.setExpireDateTime(new VisualFormatter().parseDate(expireDateTime, "yyyy-MM-dd HH:mm"));
	}
 
    public void setIsBranch(Boolean isBranch)
    {
       	this.siteNodeVO.setIsBranch(isBranch);
    }
     
	public Integer getSiteNodeId()
	{
		return newSiteNodeVO.getSiteNodeId();
	}
    
	public String getSortProperty()
    {
        return sortProperty;
    }
	
	/**
	 * This method returns the contents that are of contentTypeDefinition "PageTemplate" sorted on the property given.
	 */
	
	public List getSortedPageTemplates(String sortProperty) throws Exception
	{
		SiteNodeVO parentSiteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(this.parentSiteNodeId);
		LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(parentSiteNodeVO.getRepositoryId());

		List components = PageTemplateController.getController().getPageTemplates(this.getInfoGluePrincipal(), masterLanguageVO.getId());
		
		Collections.sort(components, new ReflectionComparator(sortProperty));
		
		return components;
	}
		
	
	/**
	 * This method fetches an url to the asset for the component.
	 */
	
	public String getDigitalAssetUrl(Integer contentId, String key) throws Exception
	{
		String imageHref = null;
		try
		{
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(ContentController.getContentController().getContentVOWithId(contentId).getRepositoryId());
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentId, masterLanguage.getId());
			List digitalAssets = DigitalAssetController.getDigitalAssetVOList(contentVersionVO.getId());
			Iterator i = digitalAssets.iterator();
			while(i.hasNext())
			{
				DigitalAssetVO digitalAssetVO = (DigitalAssetVO)i.next();
				if(digitalAssetVO.getAssetKey().equals(key))
				{
					imageHref = DigitalAssetController.getDigitalAssetUrl(digitalAssetVO.getId()); 
					break;
				}
			}
		}
		catch(Exception e)
		{
			logger.warn("We could not get the url of the digitalAsset: " + e.getMessage(), e);
			imageHref = e.getMessage();
		}
		
		return imageHref;
	}
	
	/**
	 * This method fetches the list of SiteNodeTypeDefinitions
	 */
	
	public List getSiteNodeTypeDefinitions() throws Exception
	{
		return SiteNodeTypeDefinitionController.getController().getSiteNodeTypeDefinitionVOList();
	}      
      
    public String doExecute() throws Exception
    {
        ceb = this.siteNodeVO.validate();
    	ceb.throwIfNotEmpty();
    	
    	logger.info("name:" + this.siteNodeVO.getName());
    	logger.info("publishDateTime:" + this.siteNodeVO.getPublishDateTime());
    	logger.info("expireDateTime:" + this.siteNodeVO.getExpireDateTime());
    	logger.info("isBranch:" + this.siteNodeVO.getIsBranch());
    	
    	Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
            SiteNode newSiteNode = SiteNodeControllerProxy.getSiteNodeControllerProxy().acCreate(this.getInfoGluePrincipal(), this.parentSiteNodeId, this.siteNodeTypeDefinitionId, this.repositoryId, this.siteNodeVO, db);            
            newSiteNodeVO = newSiteNode.getValueObject();
            
            SiteNodeController.getController().createSiteNodeMetaInfoContent(db, newSiteNode, this.repositoryId, this.getInfoGluePrincipal(), this.pageTemplateContentId);
            
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
    	
        return "success";
    }


    public String doInput() throws Exception
    {
        return "input";
    }
        
    public Integer getPageTemplateContentId()
    {
        return pageTemplateContentId;
    }
    
    public void setPageTemplateContentId(Integer pageTemplateContentId)
    {
        this.pageTemplateContentId = pageTemplateContentId;
    }
}
