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

package org.infoglue.cms.entities.structure;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.kernel.BaseGloballyIdentifyableEntity;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.DateHelper;
import org.infoglue.cms.util.validators.ValidatorFactory;

public class SiteNodeVO implements BaseEntityVO, BaseGloballyIdentifyableEntity
{

    private java.lang.Integer siteNodeId 	= null;
    private java.lang.String name			= "";
    private java.util.Date publishDateTime 	= new Date();
    private java.util.Date expireDateTime  	= new Date();
    private java.lang.Boolean isBranch		= new Boolean(false);             
  	private java.lang.Integer repositoryId 	= null;    
  	private java.lang.Integer siteNodeTypeDefinitionId = null;  
  	private Integer childCount		= null;
  	private Integer siteNodeVersionId= null;
  	private Integer stateId			= null;
  	private Integer isProtected 	= null;
  	private Date modifiedDateTime 	= null;
  	
  	private String creatorName;
  	private String versionModifier;
	private Integer metaInfoContentId 	= new Integer(-1);
	
	private Integer parentSiteNodeId 	= null;

	private Integer languageId		 	= null;
	private Integer contentVersionId	= null;
	private String attributes 			= null;

  	//Used if an application wants to add more properties to this item... used for performance reasons.
  	private Map extraProperties = new Hashtable();
  	
  	private static SimpleTimeZone stmz = new SimpleTimeZone(-8 * 60 * 60 * 1000, "GMT");


	public SiteNodeVO()
  	{
  		//Initilizing the expireDateTime... 
  		Calendar calendar = Calendar.getInstance(stmz);
  		
  		int years = 50;
  		try
  		{
	  		String numberOfYears = CmsPropertyHandler.getDefaultNumberOfYearsBeforeExpire();
	  		if(numberOfYears != null && !numberOfYears.equals(""))
	  			years = new Integer(numberOfYears).intValue();
  		}
  		catch (Throwable t) 
  		{}
  		
  		calendar.add(Calendar.YEAR, years);
  		expireDateTime = calendar.getTime();
  	}
  	
	/**
	 * Returns the childCount.
	 * @return Integer
	 */
	public Integer getChildCount()
	{
		return childCount;
	}

	/**
	 * Sets the childCount.
	 * @param childCount The childCount to set
	 */
	public void setChildCount(Integer childCount)
	{
		this.childCount = childCount;
	}
  
    public java.lang.Integer getSiteNodeId()
    {
        return this.siteNodeId;
    }
                
    public void setSiteNodeId(java.lang.Integer siteNodeId)
    {
        this.siteNodeId = siteNodeId;
    }
    
    public java.lang.String getName()
    {
        return this.name;
    }
                
    public void setRepositoryId(java.lang.Integer repositoryId)
    {
        this.repositoryId = repositoryId;
    }
    
    public java.lang.Integer getRepositoryId()
    {
        return this.repositoryId;
    }

    public void setSiteNodeTypeDefinitionId(java.lang.Integer siteNodeTypeDefinitionId)
    {
        this.siteNodeTypeDefinitionId = siteNodeTypeDefinitionId;
    }
    
    public java.lang.Integer getSiteNodeTypeDefinitionId()
    {
        return this.siteNodeTypeDefinitionId;
    }
    
    public void setName(java.lang.String name)
    {
        this.name = name;
    }
    
    public java.util.Date getPublishDateTime()
    {
        return this.publishDateTime;
    }
                
    public void setPublishDateTime(java.util.Date publishDateTime)
    {
        this.publishDateTime = publishDateTime;
    }
    
    public java.util.Date getExpireDateTime()
    {
        return this.expireDateTime;
    }
                
    public void setExpireDateTime(java.util.Date expireDateTime)
    {
        this.expireDateTime = expireDateTime;
    }
    
    public java.lang.Boolean getIsBranch()
    {
    	return this.isBranch;
	}
    
    public void setIsBranch(java.lang.Boolean isBranch)
	{
		this.isBranch = isBranch;
	}
	
    public Integer getMetaInfoContentId()
    {
        return metaInfoContentId;
    }
    
    public void setMetaInfoContentId(Integer metaInfoContentId)
    {
        this.metaInfoContentId = metaInfoContentId;
    }

	/**
	 * @see org.infoglue.cms.entities.kernel.BaseEntityVO#getId()
	 */
	public Integer getId() 
	{
		return getSiteNodeId();
	}
	
	public String getUUId() 
	{
		return getId().toString();
	}

	/**
	 * @see org.infoglue.cms.entities.kernel.BaseEntityVO#validate()
	 */
	public ConstraintExceptionBuffer validate() 
	{ 
		ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
 		ValidatorFactory.createStringValidator("SiteNode.name", true, 2, 100).validate(name, ceb);
 		
 		if(this.publishDateTime.after(this.expireDateTime))
			ceb.add(new ConstraintException("SiteNode.publishDateTime", "308"));
		
		return ceb;
	}	
	          
	/**
	 * Returns the creatorName.
	 * @return String
	 */
	public String getCreatorName()
	{
		return creatorName;
	}

	/**
	 * Sets the creatorName.
	 * @param creatorName The creatorName to set
	 */
	public void setCreatorName(String creatorName)
	{
		this.creatorName = creatorName;
	}

    public Map getExtraProperties()
    {
        return extraProperties;
    }

	public Integer getParentSiteNodeId()
	{
		return parentSiteNodeId;
	}

	public void setParentSiteNodeId(Integer parentSiteNodeId)
	{
		this.parentSiteNodeId = parentSiteNodeId;
	}

    public Integer getSiteNodeVersionId()
    {
        return siteNodeVersionId;
    }
            
    public void setSiteNodeVersionId(Integer siteNodeVersionId)
    {
        this.siteNodeVersionId = siteNodeVersionId;
    }

    public Integer getStateId()
    {
        return stateId;
    }
            
    public void setStateId(Integer stateId)
    {
        this.stateId = stateId;
    }

    public Integer getIsProtected()
    {
        return this.isProtected;
    }
            
    public void setIsProtected(Integer isProtected)
    {
        this.isProtected = isProtected;
    }

    public String getVersionModifier()
    {
        return versionModifier;
    }
            
    public void setVersionModifier(String versionModifier)
    {
        this.versionModifier = versionModifier;
    }

    public Date getModifiedDateTime()
    {
        return this.modifiedDateTime;
    }
                
    public void setModifiedDateTime(Date modifiedDateTime)
    {
        this.modifiedDateTime = modifiedDateTime;
    }
    
	public Integer getLanguageId()
	{
		return languageId;
	}

	public void setLanguageId(Integer languageId)
	{
		//if(this.attributes != null)
		//	addAttributes(languageId, this.attributes);
		this.languageId = languageId;
	}

	public Integer getContentVersionId()
	{
		return contentVersionId;
	}

	public void setContentVersionId(Integer contentVersionId)
	{
		this.contentVersionId = contentVersionId;
	}

	public String getAttributes()
	{
		return this.attributes;
	}

	public void setAttributes(String attributes)
	{
		//if(this.languageId != null)
		//	addAttributes(languageId, attributes);
		this.attributes = attributes;
	}
/*
	public void addAttributes(Integer languageId, String attributes)
	{
		//this.extraProperties.put("attributes_" + languageId, attributes);
	}

	public String getAttributes(Integer languageId)
	{
		//return (String)this.extraProperties.get("attributes_" + languageId);
	}
*/
}
        
