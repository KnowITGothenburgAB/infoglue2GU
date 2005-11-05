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

package org.infoglue.cms.applications.contenttool.actions;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentStateController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.EventController;
import org.infoglue.cms.controllers.kernel.impl.simple.PublicationController;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersion;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.publishing.PublicationVO;
import org.infoglue.cms.entities.workflow.EventVO;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.util.AccessConstraintExceptionBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 *  @author Stefan Sik
 * 
 * Present a list of contentVersion under a given content, recursing down in the hierarcy
 * 
 */

public class UnpublishContentVersionAction extends InfoGlueAbstractAction 
{

	private List contentVersionVOList = new ArrayList();
	private Integer contentId;
	private Integer repositoryId;

	private List contentVersionId = new ArrayList();
	private Integer stateId;
	private Integer languageId;
	private String versionComment;
	private boolean overrideVersionModifyer = false;
	private String attemptDirectPublishing = "false";

	
	public String doInput() throws Exception 
	{
		if(this.contentId != null)
		{
		    ContentVO contentVO = ContentController.getContentController().getContentVOWithId(this.contentId);
		    this.repositoryId = contentVO.getRepositoryId();
		    
			AccessConstraintExceptionBuffer ceb = new AccessConstraintExceptionBuffer();
		
			Integer protectedContentId = ContentControllerProxy.getController().getProtectedContentId(contentId);
			if(protectedContentId != null && !AccessRightController.getController().getIsPrincipalAuthorized(this.getInfoGluePrincipal(), "Content.SubmitToPublish", protectedContentId.toString()))
				ceb.add(new AccessConstraintException("Content.contentId", "1005"));
			
			ceb.throwIfNotEmpty();

			contentVersionVOList = ContentVersionController.getContentVersionController().getContentVersionVOWithParentRecursive(contentId, ContentVersionVO.PUBLISHED_STATE);
		}

	    return "input";
	}
	
	/**
	 * This method gets called when calling this action. 
	 * If the stateId is 2 which equals that the user tries to prepublish the page. If so we
	 * ask the user for a comment as this is to be regarded as a new version. 
	 */
	   
    public String doExecute() throws Exception
    {   
		setContentVersionId( getRequest().getParameterValues("sel") );
		Iterator it = getContentVersionId().iterator();
		
		List events = new ArrayList();
		while(it.hasNext())
		{
			Integer contentVersionId = (Integer)it.next();
		    //ContentVersion contentVersion = ContentStateController.changeState((Integer) it.next(), ContentVersionVO.PUBLISH_STATE, getVersionComment(), this.getInfoGluePrincipal(), null, events);
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getContentVersionVOWithId(contentVersionId);
			
			EventVO eventVO = new EventVO();
			eventVO.setDescription(this.versionComment);
			eventVO.setEntityClass(ContentVersion.class.getName());
			eventVO.setEntityId(contentVersionId);
			eventVO.setName(contentVersionVO.getContentName() + "(" + contentVersionVO.getLanguageName() + ")");
			eventVO.setTypeId(EventVO.UNPUBLISH_LATEST);
			eventVO = EventController.create(eventVO, this.repositoryId, this.getInfoGluePrincipal());
			events.add(eventVO);
		}
		
		if(attemptDirectPublishing.equalsIgnoreCase("true"))
		{
		    PublicationVO publicationVO = new PublicationVO();
		    publicationVO.setName("Direct publication by " + this.getInfoGluePrincipal().getName());
		    publicationVO.setDescription(getVersionComment());
		    //publicationVO.setPublisher(this.getInfoGluePrincipal().getName());
		    publicationVO.setRepositoryId(repositoryId);
		    publicationVO = PublicationController.getController().createAndPublish(publicationVO, events, this.overrideVersionModifyer, this.getInfoGluePrincipal());
		}
		
       	return "success";
    }


	public List getContentVersions()
	{
		return this.contentVersionVOList;		
	}
	
	public Integer getContentId() 
	{
		return contentId;
	}

	public void setContentId(Integer contentId) 
	{
		this.contentId = contentId;
	}

    public Integer getRepositoryId()
    {
        return repositoryId;
    }
    
    public java.lang.Integer getLanguageId()
    {
        return this.languageId;
    }
        
    public void setLanguageId(Integer languageId)
    {
	    this.languageId = languageId;
    }
                	
	public void setStateId(Integer stateId)
	{
		this.stateId = stateId;
	}

	public void setVersionComment(String versionComment)
	{
		this.versionComment = versionComment;
	}
	
	public String getVersionComment()
	{
		return this.versionComment;
	}
	
	public Integer getStateId()
	{
		return this.stateId;
	}
            
	/**
	 * @return
	 */
	public List getContentVersionId() 
	{
		return contentVersionId;
	}

	/**
	 * @param list
	 */
	private void setContentVersionId(String[] list) 
	{
		contentVersionId = new ArrayList();
		for(int i=0; i < list.length; i++)
		{
			contentVersionId.add(new Integer(list[i]));
		}		
	}

	public void setAttemptDirectPublishing(String attemptDirectPublishing)
    {
        this.attemptDirectPublishing = attemptDirectPublishing;
    }
    
    public void setRepositoryId(Integer repositoryId)
    {
        this.repositoryId = repositoryId;
    }
    
    public boolean getOverrideVersionModifyer()
    {
        return overrideVersionModifyer;
    }
    
    public void setOverrideVersionModifyer(boolean overrideVersionModifyer)
    {
        this.overrideVersionModifyer = overrideVersionModifyer;
    }
}
