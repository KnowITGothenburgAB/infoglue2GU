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

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.DateHelper;
import org.infoglue.cms.util.validators.ValidatorFactory;

public class SiteNodeVO implements BaseEntityVO
{

    private java.lang.Integer siteNodeId 	= null;
    private java.lang.String name			= "";
    private java.util.Date publishDateTime 	= DateHelper.getSecondPreciseDate();
    private java.util.Date expireDateTime  	= DateHelper.getSecondPreciseDate();
    private java.lang.Boolean isBranch		= new Boolean(false);             
  	private java.lang.Integer repositoryId 	= null;    
  	private java.lang.Integer siteNodeTypeDefinitionId = null;  
  	private Integer childCount;
  	
  	private String creatorName;
	private Integer metaInfoContentId 	= null;

	public SiteNodeVO()
  	{
  		//Initilizing the expireDateTime... 
  		Calendar calendar = Calendar.getInstance();
  		calendar.setTime(DateHelper.getSecondPreciseDate());
  		calendar.add(Calendar.YEAR, 10);
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


}
        
