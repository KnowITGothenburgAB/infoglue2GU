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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.applications.common.ImageButton;
import org.infoglue.cms.applications.common.ToolbarButton;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.impl.simple.ContentImpl;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.Language;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Repository;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.entities.management.ServiceDefinition;
import org.infoglue.cms.entities.management.ServiceDefinitionVO;
import org.infoglue.cms.entities.management.SiteNodeTypeDefinition;
import org.infoglue.cms.entities.management.impl.simple.RepositoryImpl;
import org.infoglue.cms.entities.management.impl.simple.SiteNodeTypeDefinitionImpl;
import org.infoglue.cms.entities.structure.ServiceBindingVO;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.structure.SiteNodeVersion;
import org.infoglue.cms.entities.structure.SiteNodeVersionVO;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeImpl;
import org.infoglue.cms.entities.structure.impl.simple.SiteNodeVersionImpl;
import org.infoglue.cms.entities.structure.impl.simple.SmallSiteNodeVersionImpl;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.DateHelper;
import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.cms.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

public class StructureToolbarController 
{
    private final static Logger logger = Logger.getLogger(StructureToolbarController.class.getName());

   	private static VisualFormatter formatter = new VisualFormatter();

	public static ToolbarButton getPreviewButtons(Integer repositoryId, Integer siteNodeId, Locale locale) throws Exception
	{
		RepositoryVO repositoryVO = RepositoryController.getController().getRepositoryVOWithId(repositoryId);
		
		String dnsName = repositoryVO.getDnsName();

	    String workingUrl = null;
	    
	    String keyword = "working=";
	    int startIndex = (dnsName == null) ? -1 : dnsName.indexOf(keyword);
	    if(startIndex != -1)
	    {
	        int endIndex = dnsName.indexOf(",", startIndex);
		    if(endIndex > -1)
	            dnsName = dnsName.substring(startIndex, endIndex);
	        else
	            dnsName = dnsName.substring(startIndex);

		    workingUrl = dnsName.split("=")[1] + CmsPropertyHandler.getComponentRendererUrl() + "ViewPage.action";
	    }
	    else
	    {
	        workingUrl = CmsPropertyHandler.getPreviewDeliveryUrl();
	    }
	    
	    return new ToolbarButton("",
				  getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageLabel"), 
				  getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageLabel"),
				  "javascript:openPopup('" + workingUrl + "?siteNodeId=" + siteNodeId + "', 'Import', 'resizable=yes,toolbar=yes,scrollbars=yes,status=yes,location=yes,menubar=yes');",
				  "",
				  "left",
				  "preview",
				  true);
		/*
		return new ToolbarButton("",
				getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageLabel"), 
				getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageTitle"),
				"" + workingUrl + "?siteNodeId=" + siteNodeId,
				"",
				"preview");
		*/
	}

	
	public static ToolbarButton getCoverButtons(Integer repositoryId, Integer siteNodeId, Locale locale, InfoGluePrincipal principal)
	{
		try
		{
		    boolean isMetaInfoInWorkingState = false;
			LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(repositoryId);
			Integer languageId = masterLanguageVO.getLanguageId();

			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
			if(siteNodeVO.getMetaInfoContentId() != null && siteNodeVO.getMetaInfoContentId().intValue() != -1)
			{
				ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNodeVO.getMetaInfoContentId(), languageId);
				if(contentVersionVO.getStateId().equals(ContentVersionVO.WORKING_STATE))
					isMetaInfoInWorkingState = true;
			}

			logger.info("isMetaInfoInWorkingState:" + isMetaInfoInWorkingState);
			if(isMetaInfoInWorkingState)
			{
				return new ToolbarButton("",
						getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverLabel"), 
						getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverTitle"),
						"ViewSiteNode.action?siteNodeId=" + siteNodeId + "&stay=true",
						"",
						"pageCover",
						"structureWorkIframe");
			}
			else
			{
				return new ToolbarButton("",
						getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverLabel"), 
						getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverTitel"),
						"javascript:alert('Cannot edit this page. You must first set the meta info to working. Do this by entering node properties and changing the state to working.');",
						"",
						"pageCover");
			}
		}
		catch(Exception e)
		{
			return new ToolbarButton("",
					getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverLabel"), 
					getLocalizedString(locale, "tool.structuretool.toolbarV3.pageCoverTitel"),
					"javascript:alert('Cannot edit this page. You must first set the meta info to working. Do this by entering node properties and changing the state to working.');",
					"",
					"pageCover");
		}
	}

	/*
	coverButton.getSubButtons().add(getSimplePageComponentsButton());
	buttons.add(coverButton);	

	if(!isReadOnly())
	{
	    ImageButton pageComponentsButton = getViewPageComponentsButton();
	    pageComponentsButton.getSubButtons().add(getSimplePageComponentsButton());
	    buttons.add(pageComponentsButton);	
	}
	
	buttons.add(getExecuteTaskButton());
	 */
	
	public static ToolbarButton getPublishButtons(Integer repositoryId, Integer siteNodeId, Locale locale)
	{
		return new ToolbarButton("",
				getLocalizedString(locale, "tool.structuretool.toolbarV3.publishPageLabel"), 
				getLocalizedString(locale, "tool.structuretool.toolbarV3.publishPageTitle"),
				"ViewListSiteNodeVersion!v3.action?siteNodeId=" + siteNodeId + "&repositoryId=" + repositoryId + "&recurseSiteNodes=true&returnAddress=ViewInlineOperationMessages.action&originalAddress=refreshParent",
				"",
				"publishPages");
	}

	public static ToolbarButton getPublishCurrentNodeButton(Integer repositoryId, Integer siteNodeId, Locale locale)
	{
		return new ToolbarButton("",
				getLocalizedString(locale, "tool.structuretool.toolbarV3.publishPagesLabel"), 
				getLocalizedString(locale, "tool.structuretool.toolbarV3.publishPagesTitle"),
				"ViewListSiteNodeVersion!v3.action?siteNodeId=" + siteNodeId + "&repositoryId=" + repositoryId + "&recurseSiteNodes=false&returnAddress=ViewInlineOperationMessages.action&originalAddress=refreshParent",
				"",
				"publishPage");
	}

	/*
	public static ToolbarButton getTasksButtons(Integer repositoryId, Integer siteNodeId, Locale locale)
	{
		return new ToolbarButton("",
				getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageLabel"), 
				getLocalizedString(locale, "tool.structuretool.toolbarV3.previewPageTitle"),
				"" + workingUrl + "?siteNodeId=" + siteNodeId,
				"",
				"preview");
	}
	*/

	
	public static String getLocalizedString(Locale locale, String key) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key);
  	}

}
 
