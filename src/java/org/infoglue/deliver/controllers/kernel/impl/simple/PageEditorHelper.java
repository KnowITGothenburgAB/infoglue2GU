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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.ComponentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.DigitalAssetController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.management.Repository;
import org.infoglue.cms.entities.management.impl.simple.RepositoryImpl;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.workflow.impl.simple.EventImpl;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.cms.util.dom.DOMBuilder;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.infoglue.deliver.applications.databeans.ComponentBinding;
import org.infoglue.deliver.applications.databeans.ComponentProperty;
import org.infoglue.deliver.applications.databeans.ComponentPropertyOption;
import org.infoglue.deliver.applications.databeans.ComponentRestriction;
import org.infoglue.deliver.applications.databeans.ComponentTask;
import org.infoglue.deliver.applications.databeans.Slot;
import org.infoglue.deliver.integration.dataproviders.PropertyOptionsDataProvider;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.HttpHelper;
import org.infoglue.deliver.util.HttpUtilities;
import org.infoglue.deliver.util.NullObject;
import org.infoglue.deliver.util.Timer;

/**
 * This class is the new Helper class for generating all kind of PageEditor-divs etc.
 *
 * @author Mattias Bogeblad
 */

public class PageEditorHelper extends BaseDeliveryController
{
	private final String separator = System.getProperty("line.separator");

	private final static DOMBuilder domBuilder = new DOMBuilder();
	private final static VisualFormatter formatter = new VisualFormatter();
	private final static HttpHelper httpHelper = new HttpHelper();
	//protected NodeDeliveryController nodeDeliveryController = NodeDeliveryController.getNodeDeliveryController();

    private final static Logger logger = Logger.getLogger(PageEditorHelper.class.getName());


	public String getComponentPropertiesDiv(Database db, 
											 InfoGluePrincipal principal, 
											 HttpServletRequest request, 
											 Locale locale,
											 Integer repositoryId, 
											 Integer siteNodeId, 
											 Integer languageId, 
											 Integer contentId, 
											 Integer componentId, 
											 Integer componentContentId, 
											 String slotName, 
											 String showSimple, 
											 String originalFullURL,
											 String showLegend,
											 String targetDiv) throws Exception
	{	
	    if(request.getParameter("skipPropertiesDiv") != null && request.getParameter("skipPropertiesDiv").equalsIgnoreCase("true"))
	        return "";

	    StringBuffer sb = new StringBuffer();
	    
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

		Document document = getComponentPropertiesDOM4JDocument(siteNodeId, languageId, componentContentId, db, principal); 

        ContentVO contentVO = ContentController.getContentController().getContentVOWithId(componentContentId, db);

		String componentName = contentVO.getName();
		if(componentName.length() > 20) 
			componentName = componentName.substring(0, 20) + "...";
		
		if(slotName.length() > 10) 
			slotName = slotName.substring(0, 10) + "...";

		List languages = LanguageDeliveryController.getLanguageDeliveryController().getLanguagesForSiteNode(db, siteNodeId, principal);

		sb.append("<div id=\"componentProperties\">");
		sb.append("	<div id=\"componentPropertiesHandle\"><div id=\"leftComponentPropertiesHandle\">Properties - " + componentName + " - " + slotName + "</div><div id=\"rightComponentPropertiesHandle\"><a href=\"javascript:closeDiv('componentProperties');\" class=\"white\"><img src=\"" + componentEditorUrl + "/images/closeIcon.gif\" border=\"0\"/></a></div></div>");
		sb.append("	<div id=\"componentPropertiesBody\">");
		
		sb.append("	<form id=\"componentPropertiesForm\" name=\"component" + componentId + "PropertiesForm\" action=\"" + componentEditorUrl + "ViewSiteNodePageComponents!updateComponentProperties.action\" method=\"POST\">");
		if(showLegend != null && showLegend.equals("true"))
		{
			sb.append("		<fieldset>");
			sb.append("		<legend>Component properties</legend>");
		}
		else
		{
			sb.append("		<fieldset class=\"hiddenFieldSet\">");
		}
		
		if(languages.size() == 1)
			sb.append("<input type=\"hidden\" name=\"languageId\" value=\"" + ((LanguageVO)languages.get(0)).getId() + "\">");
		else
		{
			sb.append("	<div class=\"propertyRow\">");
	
			sb.append("		<div class=\"propertyRowLeft\">");
			sb.append("			<label for=\"languageId\">" + getLocalizedString(locale, "deliver.editOnSight.changeLanguage") + "</label>");
			sb.append("		</div>");
	
			sb.append("		<div class=\"propertyRowRight\">");
			sb.append("			<div class=\"fieldGroup\">");
			sb.append("			<select name=\"languageId\" onChange=\"javascript:changeLanguage(" + siteNodeId + ", this, " + contentId + ");\">");
						
			Iterator languageIterator = languages.iterator();
			int index = 0;
			int languageIndex = index;
			while(languageIterator.hasNext())
			{
				LanguageVO languageVO = (LanguageVO)languageIterator.next();
				if(languageVO.getLanguageId().intValue() == languageId.intValue())
				{
					sb.append("					<option value=\"" + languageVO.getLanguageId() + "\" selected>" + languageVO.getName() + "</option>");
					languageIndex = index;
				}
				else
				{
					sb.append("					<option value=\"" + languageVO.getLanguageId() + "\">" + languageVO.getName() + "</option>");
				}
				index++;
			}
	
			sb.append("			</select>");
			sb.append("			</div>");
				
			sb.append("		</div>");
			sb.append("	</div>");
			sb.append("	<div style=\"clear:both;\"></div>");
		}
		
		Collection componentProperties = getComponentProperties(componentId, document, siteNodeId, languageId, contentId, locale, db, principal);
		
		String hideProtectedProperties = CmsPropertyHandler.getHideProtectedProperties();
		int numberOfHiddenProperties = 0;
		
		int propertyIndex = 0;
		boolean isAdvancedProperties = false;
		Iterator componentPropertiesIterator = componentProperties.iterator();
		while(componentPropertiesIterator.hasNext())
		{
			ComponentProperty componentProperty = (ComponentProperty)componentPropertiesIterator.next();
			
			boolean hasAccessToProperty = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentPropertyEditor.EditProperty", "" + componentContentId + "_" + componentProperty.getName());
			boolean isFirstAdvancedProperty = false;
			
			if(componentProperty.getName().equalsIgnoreCase("CacheResult"))
			{
				isFirstAdvancedProperty = true;
				isAdvancedProperties = true;
			}
			
			//System.out.println("componentProperty:" + componentProperty.getName() + ":" + isAdvancedProperties);
			if(componentProperty.getName().equalsIgnoreCase("CacheResult") ||
			   componentProperty.getName().equalsIgnoreCase("UpdateInterval") ||
			   componentProperty.getName().equalsIgnoreCase("CacheKey") ||
			   componentProperty.getName().equalsIgnoreCase("PreRenderOrder"))
			{
				hasAccessToProperty = true;
			}

			//Advanced properties
			if(isFirstAdvancedProperty)
			{
				if(componentProperties.size() - numberOfHiddenProperties < 1)
				{
					sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("			<label>" + getLocalizedString(locale, "deliver.editOnSight.noPropertiesVisible") + "</label>");
					sb.append("		</div>");
					sb.append("	</div>");
					sb.append("	<div style=\"clear:both;\"></div>");
				}

				sb.append("	<div class=\"propertyRow\">");
				sb.append("		<div class=\"propertyRowLeft\">");
				sb.append("			" + getLocalizedString(locale, "deliver.editOnSight.advancedProperties") + " <img src='images/downArrow.gif' onclick=\"$('.advancedProperty" + componentId + "').toggle();\"/>");
				sb.append("		</div>");
				sb.append("	</div>");
				sb.append("	<div style=\"clear:both;\"></div>");
			}
			
			if(!hasAccessToProperty && hideProtectedProperties.equalsIgnoreCase("true"))
			{
				numberOfHiddenProperties++;
			}
			else
			{
				if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.BINDING))
				{
					String assignUrl = "";
					String createUrl = "";
					 
					if(componentProperty.getVisualizingAction() != null && !componentProperty.getVisualizingAction().equals(""))
					{
						assignUrl = componentEditorUrl + componentProperty.getVisualizingAction() + "?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
					}
					else
					{	
						if(componentProperty.getEntityClass().equalsIgnoreCase("Content"))
						{
						    String allowedContentTypeIdParameters = "";
	
						    if(componentProperty.getAllowedContentTypeNamesArray() != null && componentProperty.getAllowedContentTypeNamesArray().length > 0)
						    {
						        allowedContentTypeIdParameters = "&" + componentProperty.getAllowedContentTypeIdAsUrlEncodedString(db);
						    }
						    
							if(componentProperty.getIsMultipleBinding())
							{
								if(componentProperty.getIsAssetBinding())
									assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showContentTreeForMultipleAssetBinding.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&showSimple=" + showSimple;
								else
									assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showContentTreeForMultipleBinding.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&showSimple=" + showSimple;
							}
							else
							{
								if(componentProperty.getIsAssetBinding())
								{
									String assignedParameters = "";
									Iterator<ComponentBinding> bindingsIterator = componentProperty.getBindings().iterator();
									while(bindingsIterator.hasNext())
									{
										ComponentBinding componentBinding = bindingsIterator.next();
										assignedParameters = "&assignedContentId=" + componentBinding.getEntityId() + "&assignedAssetKey=" + componentBinding.getAssetKey() + "&assignedPath=" + formatter.encodeURI(componentProperty.getValue());
									}
									
									assignUrl = componentEditorUrl + "ViewContentVersion!viewAssetsForComponentBinding.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&showSimple=" + showSimple + assignedParameters;
								}
								else
									assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showContentTree.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&showSimple=" + showSimple;
							}
						}
						else if(componentProperty.getEntityClass().equalsIgnoreCase("SiteNode"))
						{
							if(componentProperty.getIsMultipleBinding())
								assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showStructureTreeForMultipleBinding.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
							else
								assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showStructureTree.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
						}
						else if(componentProperty.getEntityClass().equalsIgnoreCase("Category"))
						{
							if(componentProperty.getIsMultipleBinding())
								assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showCategoryTreeForMultipleBinding.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
							else
								assignUrl = componentEditorUrl + "ViewSiteNodePageComponents!showCategoryTree.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
						}
					}
						
					if(componentProperty.getCreateAction() != null && !componentProperty.getCreateAction().equals(""))
					{
						createUrl = componentEditorUrl + componentProperty.getCreateAction() + "?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple;
					}
					else
					{	
						if(componentProperty.getVisualizingAction() != null && !componentProperty.getVisualizingAction().equals(""))
						{
							createUrl = assignUrl;
						}
						else if(componentProperty.getEntityClass().equalsIgnoreCase("Content"))
						{
						    String allowedContentTypeIdParameters = "";
	
						    if(componentProperty.getAllowedContentTypeNamesArray() != null && componentProperty.getAllowedContentTypeNamesArray().length > 0)
						    {
						        allowedContentTypeIdParameters = "&" + componentProperty.getAllowedContentTypeIdAsUrlEncodedString(db);
						    }
	
						    String returnAddress = URLEncoder.encode("ViewSiteNodePageComponents!addComponentPropertyBinding.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=-1&entity=Content&entityId=#entityId&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&path=#path&showSimple=" + showSimple + "", "UTF-8");
							
					        String cancelKey = originalFullURL;
					        String cancelAddress = (String)CacheController.getCachedObjectFromAdvancedCache("encodedStringsCache", cancelKey);
					        if(cancelAddress == null)
					        {
					        	cancelAddress = URLEncoder.encode(cancelKey, "UTF-8");
					        	CacheController.cacheObjectInAdvancedCache("encodedStringsCache", cancelKey, cancelAddress);
					        }
	
							if(componentProperty.getIsMultipleBinding())
								createUrl = componentEditorUrl + "CreateContentWizardFinish.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&refreshAddress=" + returnAddress + "&cancelAddress=" + cancelAddress + "&showSimple=" + showSimple;
							else
								createUrl = componentEditorUrl + "CreateContentWizardFinish.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + allowedContentTypeIdParameters + "&refreshAddress=" + returnAddress + "&cancelAddress=" + cancelAddress + "&showSimple=" + showSimple;
						}
						else if(componentProperty.getEntityClass().equalsIgnoreCase("SiteNode"))
						{
						    String returnAddress = URLEncoder.encode("ViewSiteNodePageComponents!addComponentPropertyBinding.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=-1&entity=Content&entityId=#entityId&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&path=#path&showSimple=" + showSimple + "", "UTF-8");
							
					        String cancelKey = originalFullURL;
					        String cancelAddress = (String)CacheController.getCachedObjectFromAdvancedCache("encodedStringsCache", cancelKey);
					        if(cancelAddress == null)
					        {
					        	cancelAddress = URLEncoder.encode(cancelKey, "UTF-8");
					        	CacheController.cacheObjectInAdvancedCache("encodedStringsCache", cancelKey, cancelAddress);
					        }
	
							if(componentProperty.getIsMultipleBinding())
								createUrl = componentEditorUrl + "CreateSiteNodeWizardFinish.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&refreshAddress=" + returnAddress + "&cancelAddress=" + cancelAddress + "&showSimple=" + showSimple;
							else
								createUrl = componentEditorUrl + "CreateSiteNodeWizardFinish.action?repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&refreshAddress=" + returnAddress + "&cancelAddress=" + cancelAddress + "&showSimple=" + showSimple;
						}
					}
					
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					sb.append("			<div class=\"propertyRowLeft\">");
					sb.append("				<label for=\"" + componentProperty.getName() + "\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("			</div>");
	
					sb.append("			<div class=\"propertyRowRight\">");
					
					if(hasAccessToProperty)
					{
						String warningText = getLocalizedString(locale, "deliver.editOnSight.dirtyWarning");
						sb.append("			<div class=\"fieldGroup\">");									
						sb.append("				<a name=\"" + componentProperty.getName() + "\" class=\"componentEditorLink\" href=\"javascript:openAssignDialog('" + warningText + "', '" + assignUrl +"');\">");
					}
					
					sb.append("					" + (componentProperty.getValue() == null || componentProperty.getValue().equalsIgnoreCase("") ? componentProperty.getDefaultValue() : componentProperty.getValue()) + (componentProperty.getIsAssetBinding() ? " (" + componentProperty.getAssetKey() + ")" : ""));
					
					if(hasAccessToProperty)
					{
						sb.append("				</a>");
						sb.append("			</div>");
					}				
					
					sb.append("				<div class=\"actionGroup\">");
					if(componentProperty.getValue() != null && componentProperty.getValue().equalsIgnoreCase("Undefined"))
					{	
						if(hasAccessToProperty && createUrl != null)
						{
							sb.append("			<a class=\"componentEditorLink\" href=\"" + createUrl + "\"><img src=\"" + componentEditorUrl + "/images/createContent.gif\" border=\"0\" alt=\"Create new content to show\"></a>");
						}
					}
					else
					{
						if(hasAccessToProperty)
						{
							sb.append("			<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&propertyName=" + componentProperty.getName() + "&showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						}
					}
					sb.append("			</div>");
					sb.append("			</div>");
	
					sb.append("		</div>");
				}
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.TEXTFIELD))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("			<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("		</div>");
	
					sb.append("		<div class=\"propertyRowRight\">");
					
					sb.append("			<div class=\"fieldGroup\">");									
					if(hasAccessToProperty)
					{
						sb.append("			<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\"/>");
						sb.append("			<input type=\"text\" class=\"propertytextfield\" name=\"" + componentProperty.getName() + "\" value=\"" + componentProperty.getValue() + "\" onkeydown=\"setDirty();\"/>");
					}
					else
						sb.append("	" + componentProperty.getValue() + "");
					sb.append("			</div>");
		
					if(hasAccessToProperty)
					{
						sb.append("	<div class=\"actionGroup\">");
						sb.append("		<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						sb.append("	</div>");
					}
					sb.append("	</div>");
	
					sb.append("	</div>");
	
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.DATEFIELD))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("			<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("		</div>");
	
					sb.append("		<div class=\"propertyRowRight\">");
					
					sb.append("			<div class=\"fieldGroup\">");									
					if(hasAccessToProperty)
					{
						sb.append("			<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\"/>");
						sb.append("			<input type=\"text\" class=\"propertydatefield\" style=\"width: 100px;\" id=\"" + componentProperty.getName() + "\" name=\"" + componentProperty.getName() + "\" value=\"" + componentProperty.getValue() + "\" onkeydown=\"setDirty();\"/>&nbsp;<a name=\"calendar_" + componentProperty.getName() + "\" id=\"calendar_" + componentProperty.getName() + "\"><img src=\"" + componentEditorUrl + "/images/calendar.gif\" border=\"0\"/></a>");
						sb.append("			<script type=\"text/javascript\">");
						sb.append("				Calendar.setup({");
						sb.append("	        		inputField     :    \"" + componentProperty.getName() + "\",");
						sb.append("	        		ifFormat       :    \"%Y-%m-%d %H:%M\",");
						sb.append("	        		button         :    \"calendar_" + componentProperty.getName() + "\",");
						sb.append("	        		align          :    \"BR\",");
						sb.append("	        		singleClick    :    true,");
						sb.append("	        		firstDay  	   : 	1,");
						sb.append("	        		showsTime	   :    true,");
						sb.append("	        		timeFormat     :    \"24\"");
						sb.append("				});");
						sb.append("			</script>");
					}
					else
						sb.append("	" + componentProperty.getValue() + "");
					sb.append("			</div>");
		
					if(hasAccessToProperty)
					{
						sb.append("	<div class=\"actionGroup\">");
						sb.append("		<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						sb.append("	</div>");
					}
					sb.append("	</div>");
	
					sb.append("	</div>");
	
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.TEXTAREA))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("		<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("		</div>");
	
					sb.append("		<div class=\"propertyRowRight\">");
					
					sb.append("			<div class=\"fieldGroup\">");									
					if(hasAccessToProperty)
					{
						sb.append("	<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\"/>");
						if(componentProperty.getIsWYSIWYGEnabled())
							sb.append("	<textarea toolbarName=\"" + componentProperty.getWYSIWYGToolbar() + "\" class=\"propertytextarea wysiwygeditor\" id=\"" + componentProperty.getName() + "\" name=\"" + componentProperty.getName() + "\" onkeydown=\"setDirty();\">" + (componentProperty.getValue() == null ? "" : componentProperty.getValue()) + "</textarea>");
						else
							sb.append("	<textarea class=\"propertytextarea\" id=\"" + componentProperty.getName() + "\" name=\"" + componentProperty.getName() + "\" onkeydown=\"setDirty();\">" + (componentProperty.getValue() == null ? "" : componentProperty.getValue()) + "</textarea>");
					}
					else
						sb.append("	" + componentProperty.getValue() + "");
					sb.append("			</div>");
		
					if(hasAccessToProperty)
					{
						sb.append("	<div class=\"actionGroup\">");
						sb.append("	<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						sb.append("	</div>");
					}
					sb.append("	</div>");
	
					sb.append("	</div>");
					
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.SELECTFIELD))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("		<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("		</div>");
	
					sb.append("		<div class=\"propertyRowRight\">");
					
					sb.append("			<div class=\"fieldGroup\">");									
					if(hasAccessToProperty)
					{
						sb.append("	<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\">");
						sb.append("	<select class=\"propertyselect\" name=\"" + componentProperty.getName() + "\" onchange=\"setDirty();\">");
						
						Iterator<ComponentPropertyOption> optionsIterator = componentProperty.getOptions().iterator();
	
						if(componentProperty.getDataProvider() != null && !componentProperty.getDataProvider().equals(""))
						{
							try
							{
								PropertyOptionsDataProvider provider = (PropertyOptionsDataProvider)Class.forName(componentProperty.getDataProvider()).newInstance();
								Map parameters = httpHelper.toMap(componentProperty.getDataProviderParameters(), "UTF-8");
								optionsIterator = provider.getPropertyOptions(parameters, principal, db).iterator();
							}
							catch (Exception e) 
							{
								logger.warn("A problem loading the data provider for property " + componentProperty.getName() + ":" + e.getMessage(), e);
								List<ComponentPropertyOption> errorOptions = new ArrayList<ComponentPropertyOption>();
								ComponentPropertyOption componentPropertyOption = new ComponentPropertyOption();
								componentPropertyOption.setName("Error:" + e.getMessage());
								componentPropertyOption.setValue("");
								errorOptions.add(componentPropertyOption);
								optionsIterator = errorOptions.iterator();
							}
						}
						
						while(optionsIterator.hasNext())
						{
						    ComponentPropertyOption option = (ComponentPropertyOption)optionsIterator.next();
						    boolean isSame = false;
						    if(componentProperty != null && componentProperty.getValue() != null && option != null && option.getValue() != null)
						    	isSame = componentProperty.getValue().equals(option.getValue());
						    sb.append("<option value=\"" + option.getValue() + "\"" + (isSame ? " selected=\"1\"" : "") + ">" + option.getName() + "</option>");
						}
						
						sb.append("	</select>");					
					}
					else
						sb.append("	" + componentProperty.getName() + "");
					sb.append("		</div>");
		
					if(hasAccessToProperty)
					{
						sb.append("	<div class=\"actionGroup\">");
						sb.append("	<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						sb.append("	</div>");
					}
					sb.append("	</div>");
				
					sb.append("	</div>");
					
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.CHECKBOXFIELD))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<div class=\"propertyRowLeft\">");
					sb.append("		<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					sb.append("		</div>");
	
					sb.append("		<div class=\"propertyRowRight\">");
					sb.append("			<div class=\"fieldGroup\">");									
					if(hasAccessToProperty)
					{
						sb.append("		<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\">");
						int numberOfCheckboxes = 0;
						Iterator optionsIterator = componentProperty.getOptions().iterator();
						while(optionsIterator.hasNext())
						{
						    ComponentPropertyOption option = (ComponentPropertyOption)optionsIterator.next();
						    boolean isSame = false;
						    if(componentProperty != null && componentProperty.getValue() != null && option != null && option.getValue() != null)
						    {
						    	String[] values = componentProperty.getValue().split(",");
						    	for(int i=0; i<values.length; i++)
						    	{
						    		isSame = values[i].equals(option.getValue());
						    		if(isSame)
						    			break;
						    	}
						    }
	
						    sb.append("<input type=\"checkbox\" style=\"width: 20px; border: 0px;\" name=\"" + componentProperty.getName() + "\" value=\"" + option.getValue() + "\"" + (isSame ? " checked=\"1\"" : "") + " onclicked=\"setDirty();\"/>" + option.getName() + " ");
						    numberOfCheckboxes++;
						    if(numberOfCheckboxes == 2)
						    {
						    	numberOfCheckboxes = 0;
						    	//sb.append("<br/>");
						    }
						}
					}
					else
						sb.append("	" + componentProperty.getName() + "");
	
					sb.append("	</div>");
	
					if(hasAccessToProperty)
					{
						sb.append("	<div class=\"actionGroup\">");
						sb.append("	<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
						sb.append("	</div>");
					}
					
					sb.append("	</div>");
					
					sb.append("	</div>");
					
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				/*
				else if(componentProperty.getType().equalsIgnoreCase(ComponentProperty.RADIOBUTTONFIELD))
				{
					if(isAdvancedProperties)
						sb.append("	<div class=\"propertyRow advancedProperty" + componentId + "\" style='display:none;'>");
					else
						sb.append("	<div class=\"propertyRow\">");
					
					//sb.append("	<div class=\"propertyRow\">");
					sb.append("		<label for=\"" + componentProperty.getName() + "\" onMouseOver=\"showDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\" onMouseOut=\"javascript:hideDiv('helpLayer" + componentProperty.getComponentId() + "_" + componentProperty.getName() + "');\">" + componentProperty.getDisplayName() + "</label>");
					
					if(hasAccessToProperty)
					{
						sb.append("	<input type=\"hidden\" name=\"" + propertyIndex + "_propertyName\" value=\"" + componentProperty.getName() + "\">");
						
						Iterator optionsIterator = componentProperty.getOptions().iterator();
						while(optionsIterator.hasNext())
						{
						    ComponentPropertyOption option = (ComponentPropertyOption)optionsIterator.next();
						    boolean isSame = false;
						    if(componentProperty != null && componentProperty.getValue() != null && option != null && option.getValue() != null)
						    {
						    	String[] values = componentProperty.getValue().split(",");
						    	for(int i=0; i<values.length; i++)
						    	{
						    		isSame = values[i].equals(option.getValue());
						    		if(isSame)
						    			break;
						    	}
						    }
	
						    sb.append("<input type=\"checkbox\" style=\"width:30px;\" name=\"" + componentProperty.getName() + "\" value=\"" + option.getValue() + "\"" + (isSame ? " checked=\"1\"" : "") + " onclicked=\"setDirty();\"/> " + option.getName() + " ");
						}
					}
					else
						sb.append("	" + componentProperty.getName() + "");
		
					if(hasAccessToProperty)
						sb.append("	<a class=\"componentEditorLink\" href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponentPropertyValue.action?siteNodeId=" + siteNodeId + "&amp;languageId=" + languageId + "&amp;contentId=" + contentId + "&amp;componentId=" + componentId + "&amp;propertyName=" + componentProperty.getName() + "&amp;showSimple=" + showSimple + "\"><img src=\"" + componentEditorUrl + "/images/delete.gif\" border=\"0\"/></a>");
					
					sb.append("	</div>");
					
					if(hasAccessToProperty)
					    propertyIndex++;
				}
				*/
			
				sb.append("	<div style=\"clear:both;\"></div>");
			}
		}
		
		if(numberOfHiddenProperties > 0)
		{
			sb.append("		<div class=\"buttonRow\" style=\"margin-bottom: 10px; padding-left: 2px; color: darkred;\">" + getLocalizedString(locale, "deliver.editOnSight.protectedPropertiesExists") + "</div>");
		}
		
		sb.append("		<div class=\"buttonRow\">");
		sb.append("			<input type=\"image\" style=\"width: 50px; height: 25px; border: 0px;\" src=\"" + componentEditorUrl + "" + getLocalizedString(locale, "images.contenttool.buttons.save") + "\" width=\"50\" height=\"25\" border=\"0\"/>");
		sb.append("			<a href=\"javascript:clearComponentPropertiesInDiv('" + targetDiv + "');\"><img src=\"" + componentEditorUrl + "" + getLocalizedString(locale, "images.contenttool.buttons.close") + "\" width=\"50\" height=\"25\" border=\"0\"/></a>");
		sb.append("		</div>");
		sb.append("		</fieldset>");
		sb.append("		<input type=\"hidden\" name=\"repositoryId\" value=\"" + repositoryId + "\"/>");
		sb.append("		<input type=\"hidden\" name=\"siteNodeId\" value=\"" + siteNodeId + "\"/>");
		sb.append("		<input type=\"hidden\" name=\"languageId\" value=\"" + languageId + "\"/>");
		sb.append("		<input type=\"hidden\" name=\"contentId\" value=\"" + contentId + "\"/>");
		sb.append("		<input type=\"hidden\" name=\"componentId\" value=\"" + componentId + "\"/>");
		sb.append("		<input type=\"hidden\" name=\"showSimple\" value=\"" + showSimple + "\"/>");

		sb.append("		</form>");
		sb.append("	</div>");

		sb.append("	</div>");
		
		return sb.toString();
	}
	
	/**
	 * This method generates an appropriate context-menu for a component or slot.
	 * Usually this is used by an ajax-based GUI.
	 * 
	 * @param db
	 * @param principal
	 * @param request
	 * @param locale
	 * @param repositoryId
	 * @param siteNodeId
	 * @param languageId
	 * @param contentId
	 * @param componentId
	 * @param componentContentId
	 * @param slotName
	 * @param slotId
	 * @param showSimple
	 * @param originalFullURL
	 * @param showLegend
	 * @param targetDiv
	 * @param slotClicked
	 * @param treeItem
	 * @return
	 * @throws Exception
	 */
	public String getComponentTasksDiv(Database db, 
			 InfoGluePrincipal principal, 
			 HttpServletRequest request, 
			 Locale locale,
			 Integer repositoryId, 
			 Integer siteNodeId, 
			 Integer languageId, 
			 Integer contentId, 
			 Integer componentId, 
			 Integer componentContentId, 
			 String slotName, 
			 String slotId, 
			 String showSimple, 
			 String originalFullURL,
			 String showLegend,
			 String targetDiv,
			 String slotClicked,
			 boolean treeItem) throws Exception
		{	
		
		//System.out.println("treeItem: " + treeItem);
		
		StringBuffer sb = new StringBuffer();

		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();
		String componentRendererUrl = CmsPropertyHandler.getComponentRendererUrl();

		ContentVO metaInfoContentVO = getPageMetaInfoContentVO(db, siteNodeId, languageId, contentId, principal);
		LanguageVO masterLanguageVO = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId);

		String pageComponentsString = getPageComponentsString(db, siteNodeId, languageId, contentId, principal);
   		if(pageComponentsString != null && pageComponentsString.length() != 0)
		{
   			Document document = domBuilder.getDocument(pageComponentsString);
			List pageComponents = getPageComponents(db, pageComponentsString, document.getRootElement(), "base", null, null, siteNodeId, languageId, principal);
			InfoGlueComponent component = (InfoGlueComponent)pageComponents.get(0);
			if(!component.getId().equals(componentId))
				component = getComponentWithId(component, componentId);

			//System.out.println("Getting tasks for " + component.getName() + ":" + component.getSlotName() + ":" + component.getId());
			boolean hasAccessToAccessRights = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentEditor.ChangeSlotAccess", "");
			boolean hasAccessToAddComponent = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentEditor.AddComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
			boolean hasAccessToDeleteComponent = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentEditor.DeleteComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
			boolean hasAccessToChangeComponent = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentEditor.ChangeComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
		    boolean hasSaveTemplateAccess = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "StructureTool.SaveTemplate", "");
		    boolean hasSavePagePartTemplateAccess = hasSaveTemplateAccess;
		    if(slotClicked != null && slotClicked.equalsIgnoreCase("true"))
		    	hasAccessToAddComponent = AccessRightController.getController().getIsPrincipalAuthorized(db, principal, "ComponentEditor.AddComponent", "" + component.getContentId() + "_" + slotId);
			
		    boolean hasMaxComponents = false;
			if(component.getParentComponent() != null && component.getParentComponent().getSlotList() != null)
			{
				Iterator slotListIterator = component.getParentComponent().getSlotList().iterator();
				while(slotListIterator.hasNext())
				{
					Slot slot = (Slot)slotListIterator.next();
					if(slot.getId().equalsIgnoreCase(slotName))
					{
						if(slot.getAllowedNumberOfComponents() != -1 && slot.getComponents().size() >= slot.getAllowedNumberOfComponents())
						{
							hasMaxComponents = true;
						}
					}
				}
			}
			
		    if(component.getContainerSlot() != null && component.getContainerSlot().getDisableAccessControl())
		    {
		    	hasAccessToAddComponent = true;
		    	hasAccessToDeleteComponent = true;
		    }
		    
		    if(hasMaxComponents)
		    	hasAccessToAddComponent = false;
		    
		    if(component.getIsInherited())
			{
			    StringBuffer sb2 = new StringBuffer();
				return sb2.toString();
			}
		    
		    sb.append("<div id=\"componentMenu\" class=\"skin0\">");
			
		    Document componentTasksDocument = getComponentTasksDOM4JDocument(masterLanguageVO.getId(), metaInfoContentVO.getId(), db);
			Collection componentTasks = getComponentTasks(componentId, componentTasksDocument);
	
			int taskIndex = 0;
			Iterator componentTasksIterator = componentTasks.iterator();
			while(componentTasksIterator.hasNext())
			{
			    ComponentTask componentTask = (ComponentTask)componentTasksIterator.next();
			    
			    String view = componentTask.getView();
			    boolean openInPopup = componentTask.getOpenInPopup();
			    
			    view = view.replaceAll("\\$componentEditorUrl", componentEditorUrl);
				view = view.replaceAll("\\$originalFullURL", originalFullURL);
				view = view.replaceAll("\\$componentRendererUrl", componentRendererUrl);
			    view = view.replaceAll("\\$repositoryId", repositoryId.toString());
			    view = view.replaceAll("\\$siteNodeId", siteNodeId.toString());
			    view = view.replaceAll("\\$languageId", languageId.toString());
			    view = view.replaceAll("\\$componentId", componentId.toString());
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"executeTask('" + view + "', " + openInPopup + ");\">" + componentTask.getName() + "</div>");
			}
	
			String editHTML 						= getLocalizedString(locale, "deliver.editOnSight.editHTML");
			String submitToPublishHTML 				= getLocalizedString(locale, "deliver.editOnSight.submitToPublish");
			String addComponentHTML 				= getLocalizedString(locale, "deliver.editOnSight.addComponentHTML");
			String deleteComponentHTML 				= getLocalizedString(locale, "deliver.editOnSight.deleteComponentHTML");
			String changeComponentHTML 				= getLocalizedString(locale, "deliver.editOnSight.changeComponentHTML");
			String moveComponentUpHTML 				= getLocalizedString(locale, "deliver.editOnSight.moveComponentUpHTML");
			String moveComponentDownHTML 			= getLocalizedString(locale, "deliver.editOnSight.moveComponentDownHTML");
			String propertiesHTML 					= getLocalizedString(locale, "deliver.editOnSight.propertiesHTML");
			String accessRightsHTML 				= getLocalizedString(locale, "deliver.editOnSight.accessRightsHTML");
			String pageComponentsHTML 				= getLocalizedString(locale, "deliver.editOnSight.pageComponentsHTML");
			String viewSourceHTML 					= getLocalizedString(locale, "deliver.editOnSight.viewSourceHTML");
			String componentEditorInNewWindowHTML 	= getLocalizedString(locale, "deliver.editOnSight.componentEditorInNewWindowHTML");
			String savePageTemplateHTML		 		= getLocalizedString(locale, "deliver.editOnSight.savePageTemplateHTML");
			String savePagePartTemplateHTML 		= getLocalizedString(locale, "deliver.editOnSight.savePagePartTemplateHTML");

			//System.out.println("slotId:" + slotId);
		    Slot slot = null;
		    InfoGlueComponent parentComponent = null;
		    if(slotClicked == null || slotClicked.equalsIgnoreCase("true"))
		    {
		    	//System.out.println("Was slot which was clicked..");
			    slot = component.getSlot(slotId);
			    parentComponent = component;
			}
		    else
		    {
		    	//System.out.println("Was component which was clicked..");
		    	slot = component.getContainerSlot();
			    parentComponent = component.getParentComponent();
		    }
		    
		    String allowedComponentNamesAsEncodedString = "";
		    String disallowedComponentNamesAsEncodedString = "";
		    if(slot != null)
		    {
			    allowedComponentNamesAsEncodedString = slot.getAllowedComponentsArrayAsUrlEncodedString();
			    disallowedComponentNamesAsEncodedString = slot.getDisallowedComponentsArrayAsUrlEncodedString();		    	
		    }
		    
		    String addComponentUrl = "";
		    String deleteComponentUrl = "";
		    String changeComponentUrl = "";
			String savePartTemplateUrl = "";
		    
		    if(parentComponent != null)
		    {
		    	//System.out.println("slot:" + slot.getId());
		    	//System.out.println("parentComponent:" + parentComponent.getId());
		    	//System.out.println("allowedComponentNamesAsEncodedString:" + allowedComponentNamesAsEncodedString);
		    	//System.out.println("disallowedComponentNamesAsEncodedString:" + disallowedComponentNamesAsEncodedString);
		    
			    addComponentUrl = "" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + parentComponent.getId() + "&slotId=" + slotId + "&showSimple=" + showSimple + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "");
			    deleteComponentUrl = "" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&slotId=" + slotId + "&showSimple=" + showSimple;
			    changeComponentUrl = "" + componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&slotId=" + slotId + "&showSimple=" + showSimple + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "");
			    savePartTemplateUrl = "savePartComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + metaInfoContentVO.getId() + "', '" + component.getId() + "');";

			    //System.out.println("addComponentUrl:" + addComponentUrl);
			    //System.out.println("deleteComponentUrl:" + deleteComponentUrl);
			    //System.out.println("changeComponentUrl:" + changeComponentUrl);
			    //System.out.println("savePartTemplateUrl:" + savePartTemplateUrl);
		    }
		    else
		    {
			    addComponentUrl = "";
			    deleteComponentUrl = "" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&slotId=" + slotId + "&showSimple=" + showSimple;
			    changeComponentUrl = "" + componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&slotId=" + slotId + "&showSimple=" + showSimple + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "");
			    savePartTemplateUrl = "";

			    hasAccessToAddComponent = false;
			    hasSavePagePartTemplateAccess = false;
		    }
		    
		    if(treeItem != true)
		    	sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"edit();\">" + editHTML + "</div>");
		    if(treeItem != true)
		    	sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"editInline(" + repositoryId + ");\">" + editHTML + " Inline</div>");
		    if(treeItem != true)
		    	sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"submitToPublish(" + siteNodeId + ", " + repositoryId + ", '" + URLEncoder.encode(originalFullURL, "UTF-8") + "');\">" + submitToPublishHTML + "</div>");
			if(hasAccessToAddComponent)
				sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"insertComponentByUrl('" + addComponentUrl + "');\">" + addComponentHTML + "</div>");
			if(hasAccessToDeleteComponent)
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"deleteComponentByUrl('" + deleteComponentUrl + "');\">" + deleteComponentHTML + "</div>");
			if(hasAccessToChangeComponent)
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"changeComponentByUrl('" + changeComponentUrl + "');\">" + changeComponentHTML + "</div>");
			if(hasSaveTemplateAccess)
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"saveComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + metaInfoContentVO.getId() + "');\">" + savePageTemplateHTML + "</div>");
			if(hasSavePagePartTemplateAccess)
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"" + savePartTemplateUrl + "\">" + savePagePartTemplateHTML + "</div>");
	
			String upUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&direction=0&showSimple=" + showSimple + "";
			String downUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&direction=1&showSimple=" + showSimple + "";
			//System.out.println("upUrl:" + upUrl);
			//System.out.println("downUrl:" + downUrl);
			//System.out.println("component.getPositionInSlot():" + component.getPositionInSlot());
			
			if(component.getPositionInSlot() > 0)
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"invokeAddress('" + upUrl + "');\">" + moveComponentUpHTML + "</div>");
			if(component.getContainerSlot() != null && component.getContainerSlot().getComponents().size() - 1 > component.getPositionInSlot())
			    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"invokeAddress('" + downUrl + "');\">" + moveComponentDownHTML + "</div>");
			
			sb.append("<hr/>");
			sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"showComponentInDiv('componentPropertiesDiv', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + componentId + "&componentContentId=" + componentContentId + "&slotName=" + component.getSlotName() + "&showSimple=" + showSimple + "&showLegend=false&originalUrl=" + URLEncoder.encode(originalFullURL, "UTF-8") + "', false);\">" + propertiesHTML + "</div>");
			sb.append("<hr/>");
			if(hasAccessToAccessRights)
				sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"setAccessRights('" + slotId + "', " + componentContentId + ");\">" + accessRightsHTML + "</div>");
			if(treeItem != true)
				sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"showComponentStructure('componentStructure', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&originalUrl=" + URLEncoder.encode(originalFullURL, "UTF-8") + "', event);\">" + pageComponentsHTML + "</div>");

			sb.append("<div id=\"componentEditorInNewWindowDiv\" class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"openWindow(document.location.href, 'PageEditor', '');\">" + componentEditorInNewWindowHTML + "</div>");
			sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"javascript:viewSource();\">" + viewSourceHTML + "</div>");

			sb.append("</div>");
		}
   		
		return sb.toString();
	}
	

	public String getComponentStructureDiv(Database db, InfoGluePrincipal principal, HttpServletRequest request, Locale locale, Integer repositoryId, Integer siteNodeId, Integer languageId, Integer contentId, String showSimple, String originalFullURL, String showLegend, String targetDiv) throws Exception
	{
		StringBuffer sb = new StringBuffer();
		
		//String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();
		//String componentRendererUrl = CmsPropertyHandler.getComponentRendererUrl();
		String contextPath = request.getContextPath();
		
		ContentVO metaInfoContentVO = getPageMetaInfoContentVO(db, siteNodeId, languageId, contentId, principal);
		LanguageVO masterLanguageVO = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId);

		String pageComponentsString = getPageComponentsString(db, siteNodeId, languageId, contentId, principal);
   		if(pageComponentsString != null && pageComponentsString.length() != 0)
		{
   			Document document = domBuilder.getDocument(pageComponentsString);
   			//System.out.println("Getting components...");

			List pageComponents = getPageComponents(db, pageComponentsString, document.getRootElement(), "base", null, null, siteNodeId, languageId, principal);
			InfoGlueComponent component = (InfoGlueComponent)pageComponents.get(0);
			
			sb.append("<div id=\"pageComponents\" style=\"left:0px; top:0px; width: 450px; height: 500px;\">");
	
			sb.append("	 <div id=\"dragCorner\" style=\"position: absolute; width: 16px; height: 16px; background-color: white; bottom: 0px; right: 0px;\"><a href=\"javascript:expandWindow('pageComponents');\"><img src=\"" + contextPath + "/images/enlarge.gif\" border=\"0\" width=\"16\" height=\"16\"></a></div>");
				
			sb.append("	 <div id=\"pageComponentsHandle\" class=\"componentPropertiesHandle\"><div id=\"leftHandleNarrow\">Page components</div><div id=\"rightPaletteHandle\"><a href=\"javascript:hideDiv('pageComponents');\" class=\"white\"><img src=\"" + contextPath + "/images/closeIcon.gif\" border=\"0\"/></a></div></div>");
			sb.append("	 <div id=\"pageComponentsBody\" class=\"componentPropertiesBody\" style=\"height: 480px;\"><table class=\"igtable\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
	
			sb.append("	 <tr class=\"igtr\">");
		    sb.append("		<td class=\"igtd\" colspan=\"20\"><img src=\"" + contextPath + "/images/tcross.png\" width=\"19\" height=\"16\"><span id=\"" + component.getId() + component.getSlotName() + "ClickableDiv\" class=\"iglabel\"><img src=\"" + contextPath + "/images/slotIcon.gif\" width=\"16\" height=\"16\"><img src=\"" + contextPath + "/images/trans.gif\" width=\"5\" height=\"1\">" + component.getName() + "</span><script type=\"text/javascript\">initializeSlotEventHandler('" + component.getId() + component.getSlotName() + "ClickableDiv', '" + contextPath + "ViewSiteNodePageComponents!listComponents.action?CCC=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + component.getId() + "&slotId=base&showSimple=" + showSimple + "', '', '', 'base', '" + component.getContentId() + "');</script></td>");
			sb.append("	 </tr>");
			
			renderComponentTree(db, sb, component, 0, 0, 1, contextPath, repositoryId, siteNodeId, languageId, contentId, showSimple, originalFullURL);
	
			sb.append("	 <tr class=\"igtr\">");
			for(int i=0; i<20; i++)
			{
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/trans.gif\" width=\"19\" height=\"1\"></td>");
			}
			sb.append("	 </tr>");
			sb.append("	 </table>");
			sb.append("	 </div>");
			sb.append("</div>");
		}
   		
		return sb.toString();
	}
	
	
	/**
	 * This method renders the component tree visually
	 */
	
	private void renderComponentTree(Database db, StringBuffer sb, InfoGlueComponent component, int level, int position, int maxPosition, String contextPath, Integer repositoryId, Integer siteNodeId, Integer languageId, Integer contentId, String showSimple, String originalUrl) throws Exception
	{
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

		ContentVO componentContentVO = ContentController.getContentController().getContentVOWithId(component.getContentId(), db);
		
		int colspan = 20 - level;
		
		sb.append("		<tr class=\"igtr\">");
		sb.append("			<td class=\"igtd\"><img src=\"" + contextPath + "/images/trans.gif\" width=\"19\" height=\"16\"></td>");
		
		for(int i=0; i<level; i++)
		{
			sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
		}

		String tasks = "";
		if(component.getPagePartTemplateComponent() != null)
		{
			tasks = "showComponentTasks('componentTasks', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getPagePartTemplateComponent().getId() + "&componentContentId=" + component.getPagePartTemplateComponent().getContentId() + "&slotId=" + component.getPagePartTemplateComponent().getContainerSlot().getId() + "&showSimple=false&showLegend=false&slotClicked=false&treeItem=true&originalUrl=" + URLEncoder.encode(originalUrl, "UTF-8") + "', false, event);";
		}
		else
		{
			if(component.getContainerSlot() != null)
				tasks = "showComponentTasks('componentTasks', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&componentContentId=" + component.getContentId() + "&slotId=" + component.getContainerSlot().getId() + "&showSimple=false&showLegend=false&slotClicked=false&treeItem=true&originalUrl=" + URLEncoder.encode(originalUrl, "UTF-8") + "', false, event);";
			else
				tasks = "showComponentTasks('componentTasks', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&componentContentId=" + component.getContentId() + "&slotId=-1&showSimple=false&showLegend=false&slotClicked=false&treeItem=true&originalUrl=" + URLEncoder.encode(originalUrl, "UTF-8") + "', false, event);";
		}

		sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/tcross.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\"><img src=\"" + contextPath + "/images/componentIcon.gif\" width=\"16\" height=\"16\"></td><td class=\"igtd\" colspan=\"" + (colspan - 2) + "\"><span id=\"" + component.getId() + "\" onclick=\"" + tasks + "\" oncontextmenu=\"hideDiv('pageComponents');\">" + componentContentVO.getName() + "</span>");
		String upUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&direction=0&showSimple=" + showSimple + "";
		String downUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&direction=1&showSimple=" + showSimple + "";
		
		if(position > 0)
		    sb.append("<a href=\"" + upUrl + "\"><img src=\"" + contextPath + "/images/upArrow.gif\" border=\"0\" width=\"11\" width=\"10\"></a>");
		if(maxPosition > position)
		    sb.append("<a href=\"" + downUrl + "\"><img src=\"" + contextPath + "/images/downArrow.gif\" border=\"0\" width=\"11\" width=\"10\"></a>");
		
		sb.append("</td>");
		
		sb.append("		</tr>");
		
		Iterator slotIterator = component.getSlotList().iterator();
		while(slotIterator.hasNext())
		{
			Slot slot = (Slot)slotIterator.next();
	
			sb.append("		<tr class=\"igtr\">");
			sb.append("			<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/trans.gif\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
			for(int i=0; i<level; i++)
			{
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
			}
			if(slot.getComponents().size() > 0)
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/tcross.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/slotIcon.gif\" width=\"16\" height=\"16\"></td>");
			else
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/endline.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + contextPath + "/images/slotIcon.gif\" width=\"16\" height=\"16\"></td>");

		    String allowedComponentNamesAsEncodedString = slot.getAllowedComponentsArrayAsUrlEncodedString();
		    String disallowedComponentNamesAsEncodedString = slot.getDisallowedComponentsArrayAsUrlEncodedString();
		    
		    String slotTasks = "showComponentTasks('componentTasks', 'repositoryId=" + repositoryId + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + component.getId() + "&componentContentId=" + component.getContentId() + "&slotId=" + slot.getId() + "&showSimple=false&showLegend=false&slotClicked=true&treeItem=true&originalUrl=" + URLEncoder.encode(originalUrl, "UTF-8") + "', false, event);";
		    
		    sb.append("<td class=\"igtd\" colspan=\"" + (colspan - 4) + "\"><span id=\"" + slot.getId() + "ClickableDiv\" class=\"iglabel\" onclick=\"" + slotTasks + "\" oncontextmenu=\"hideDiv('pageComponents');\">" + slot.getId() + "</span></td>");
			
			sb.append("		</tr>");

			List slotComponents = slot.getComponents();
			//logger.info("Number of components in slot " + slot.getId() + ":" + slotComponents.size());

			if(slotComponents != null)
			{
				Iterator slotComponentIterator = slotComponents.iterator();
				int newPosition = 0;
				while(slotComponentIterator.hasNext())
				{
					InfoGlueComponent slotComponent = (InfoGlueComponent)slotComponentIterator.next();
					ContentVO componentContent = ContentController.getContentController().getContentVOWithId(slotComponent.getContentId(), db); 
					
					String imageUrl = "" + contextPath + "/images/componentIcon.gif";
					//String imageUrlTemp = getDigitalAssetUrl(componentContent.getId(), "thumbnail");
					//if(imageUrlTemp != null && imageUrlTemp.length() > 0)
					//	imageUrl = imageUrlTemp;
		
					renderComponentTree(db, sb, slotComponent, level + 2, newPosition, slotComponents.size() - 1, contextPath, repositoryId, siteNodeId, languageId, contentId, showSimple, originalUrl);

					newPosition++;
				}	
			}
		}
	}

	
	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	
	public List getComponentContents(Database db) throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "HTMLTemplate");
		argumentList.add(argument);
		arguments.put("arguments", argumentList);
		
		return ContentController.getContentController().getContentVOList(arguments, db);
	}

	
	/*
	 * This method returns a bean representing a list of ComponentProperties that the component has.
	 */
	 
	private List getComponentProperties(Integer componentId, Document document, Integer siteNodeId, Integer languageId, Integer contentId, Locale locale, Database db, InfoGluePrincipal principal) throws Exception
	{
		//TODO - h�r kan vi s�kert cache:a.
		
		//logger.info("componentPropertiesXML:" + componentPropertiesXML);
		List componentProperties = new ArrayList();
		Timer timer = new Timer();
		timer.setActive(false);

		try
		{
			if(document != null)
			{
				timer.printElapsedTime("Read document");

				String propertyXPath = "//property";
				//logger.info("propertyXPath:" + propertyXPath);
				List anl = document.selectNodes(propertyXPath);
				timer.printElapsedTime("Set property xpath");
				//logger.info("*********************************************************anl:" + anl.getLength());
				Iterator anlIterator = anl.iterator();
				while(anlIterator.hasNext())
				{
					Element binding = (Element)anlIterator.next();
					
					String name							 = binding.attributeValue("name");
					String displayName					 = binding.attributeValue("displayName");
					String description					 = binding.attributeValue("description");
					String defaultValue					 = binding.attributeValue("defaultValue");
					String dataProvider					 = binding.attributeValue("dataProvider");
					String dataProviderParameters		 = binding.attributeValue("dataProviderParameters");
					String type							 = binding.attributeValue("type");
					String allowedContentTypeNamesString = binding.attributeValue("allowedContentTypeDefinitionNames");
					String visualizingAction 			 = binding.attributeValue("visualizingAction");
					String createAction 				 = binding.attributeValue("createAction");
					//logger.info("name:" + name);
					//logger.info("type:" + type);

					ComponentProperty property = new ComponentProperty();
					property.setComponentId(componentId);
					property.setName(name);
					property.setDisplayName(displayName);
					property.setDescription(description);
					property.setDefaultValue(defaultValue);
					property.setDataProvider(dataProvider);
					property.setDataProviderParameters(dataProviderParameters);
					property.setType(type);
					property.setVisualizingAction(visualizingAction);
					property.setCreateAction(createAction);
					if(allowedContentTypeNamesString != null && allowedContentTypeNamesString.length() > 0)
					{
					    String[] allowedContentTypeNamesArray = allowedContentTypeNamesString.split(",");
					    property.setAllowedContentTypeNamesArray(allowedContentTypeNamesArray);
					}
					
					if(type.equalsIgnoreCase(ComponentProperty.BINDING))
					{
						String entity 	= binding.attributeValue("entity");
						boolean isMultipleBinding = new Boolean(binding.attributeValue("multiple")).booleanValue();
						boolean isAssetBinding 	  = new Boolean(binding.attributeValue("assetBinding")).booleanValue();
						
						property.setEntityClass(entity);
						String value = getComponentPropertyValue(componentId, name, siteNodeId, languageId, contentId, locale, db, principal, property);
						timer.printElapsedTime("Set property1");

						property.setValue(value);
						property.setIsMultipleBinding(isMultipleBinding);
						property.setIsAssetBinding(isAssetBinding);
						List<ComponentBinding> bindings = getComponentPropertyBindings(componentId, name, siteNodeId, languageId, contentId, locale, db, principal);
						property.setBindings(bindings);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name, siteNodeId, languageId, contentId, locale, db, principal, property);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTAREA))	
					{		
						boolean WYSIWYGEnabled = new Boolean(binding.attributeValue("WYSIWYGEnabled")).booleanValue();
						property.setWYSIWYGEnabled(WYSIWYGEnabled);
						String WYSIWYGToolbar = binding.attributeValue("WYSIWYGToolbar");
						property.setWYSIWYGToolbar(WYSIWYGToolbar);

						String value = getComponentPropertyValue(componentId, name, siteNodeId, languageId, contentId, locale, db, principal, property);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.SELECTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name, siteNodeId, languageId, contentId, locale, db, principal, property);
						timer.printElapsedTime("Set property2");
						
						List optionList = binding.elements("option");
						Iterator optionListIterator = optionList.iterator();
						while(optionListIterator.hasNext())
						{
							Element option = (Element)optionListIterator.next();
							String optionName	= option.attributeValue("name");
							String optionValue	= option.attributeValue("value");
							ComponentPropertyOption cpo = new ComponentPropertyOption();
							cpo.setName(optionName);
							cpo.setValue(optionValue);
							property.getOptions().add(cpo);
						}
						
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.CHECKBOXFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name, siteNodeId, languageId, contentId, locale, db, principal, property);
						timer.printElapsedTime("Set property3");
						
						List optionList = binding.elements("option");
						Iterator optionListIterator = optionList.iterator();
						while(optionListIterator.hasNext())
						{
							Element option = (Element)optionListIterator.next();
							String optionName	= option.attributeValue("name");
							String optionValue	= option.attributeValue("value");
							ComponentPropertyOption cpo = new ComponentPropertyOption();
							cpo.setName(optionName);
							cpo.setValue(optionValue);
							property.getOptions().add(cpo);
						}
						
						//logger.info("value:" + value);
						property.setValue(value);
					}
					
					componentProperties.add(property);
				}
			}
			
			addSystemProperties(componentProperties, componentId, siteNodeId, languageId, contentId, locale, db, principal);
		}
		catch(Exception e)
		{
			logger.warn("The component with id " + componentId + " had a incorrect xml defining it's properties:" + e.getMessage(), e);
		}
							
		return componentProperties;
	}

	/**
	 * This method returns a value for a property if it's set. The value is collected in the
	 * properties for the page.
	 */
	
	private String getComponentPropertyValue(Integer componentId, String name, Integer siteNodeId, Integer languageId, Integer contentId, Locale locale, Database db, InfoGluePrincipal principal, ComponentProperty componentProperty) throws Exception
	{
		String value = componentProperty.getDefaultValue();
		
		Timer timer = new Timer();
		timer.setActive(false);
				
		Document document = getPageComponentsDOM4JDocument(db, siteNodeId, languageId, contentId, principal);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']";
		List anl = document.selectNodes(componentXPath);
		Iterator anlIterator = anl.iterator();
		while(anlIterator.hasNext())
		{
			Element property = (Element)anlIterator.next();
			
			String id 			= property.attributeValue("type");
			String path 		= property.attributeValue("path");
			
			if(property.attribute("path_" + locale.getLanguage()) != null)
				path = property.attributeValue("path_" + locale.getLanguage());

			value = path;
		
			if(value != null)
				value = value.replaceAll("igbr", separator);
		}
		
		return value;
	}

  	public String getLocalizedString(Locale locale, String key) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key);
  	}

	/**
	 * This method returns a value for a property if it's set. The value is collected in the
	 * properties for the page.
	 */
	
	private List<ComponentBinding> getComponentPropertyBindings(Integer componentId, String name, Integer siteNodeId, Integer languageId, Integer contentId, Locale locale, Database db, InfoGluePrincipal principal) throws Exception
	{
		List<ComponentBinding> componentBindings = new ArrayList<ComponentBinding>();
		
		Document document = getPageComponentsDOM4JDocument(db, siteNodeId, languageId, contentId, principal);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']/binding";
		List anl = document.selectNodes(componentXPath);
		Iterator anlIterator = anl.iterator();
		while(anlIterator.hasNext())
		{
			Element property = (Element)anlIterator.next();
			
			String entity   = property.attributeValue("entity");
			String entityId = property.attributeValue("entityId");
			String assetKey = property.attributeValue("assetKey");
			
			ComponentBinding componentBinding = new ComponentBinding();
			componentBinding.setEntityClass(entity);
			componentBinding.setEntityId(new Integer(entityId));
			componentBinding.setAssetKey(assetKey);

			componentBindings.add(componentBinding);
		}
		
		return componentBindings;
	}

	protected org.dom4j.Document getPageComponentsDOM4JDocument(Database db, Integer siteNodeId, Integer languageId, Integer contentId, InfoGluePrincipal principal) throws SystemException, Exception
	{ 
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "pageComponentDocument_" + siteNodeId + "_" + languageId + "_" + contentId;
		org.dom4j.Document cachedPageComponentsDocument = (org.dom4j.Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedPageComponentsDocument != null)
			return cachedPageComponentsDocument;
		
		org.dom4j.Document pageComponentsDocument = null;
   	
		try
		{
			String xml = getPageComponentsString(db, siteNodeId, languageId, contentId, principal);
			pageComponentsDocument = domBuilder.getDocument(xml);
			
			CacheController.cacheObject(cacheName, cacheKey, pageComponentsDocument);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
		
		return pageComponentsDocument;
	}

	/**
	 * This method fetches the pageComponent structure from the metainfo content.
	 */
	    
	protected ContentVO getPageMetaInfoContentVO(Database db, Integer siteNodeId, Integer languageId, Integer contentId, InfoGluePrincipal principal) throws SystemException, Exception
	{
	    SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId, db);
	    ContentVO contentVO = null;
	    if(siteNodeVO.getMetaInfoContentId() != null && siteNodeVO.getMetaInfoContentId().intValue() > -1)
	        contentVO = ContentController.getContentController().getContentVOWithId(siteNodeVO.getMetaInfoContentId(), db);
	    else
		    contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(db, principal, siteNodeId, languageId, true, "Meta information", null);		

		if(contentVO == null)
			throw new SystemException("There was no Meta Information bound to this page which makes it impossible to render.");	
				    		
		return contentVO;
	}

	
	/**
	 * This method fetches the pageComponent structure from the metainfo content.
	 */
	    
	protected String getPageComponentsString(Database db, Integer siteNodeId, Integer languageId, Integer contentId, InfoGluePrincipal principal) throws SystemException, Exception
	{
	    SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId, db);
	    ContentVO contentVO = null;
	    if(siteNodeVO.getMetaInfoContentId() != null && siteNodeVO.getMetaInfoContentId().intValue() > -1)
	        contentVO = ContentController.getContentController().getContentVOWithId(siteNodeVO.getMetaInfoContentId(), db);
	    else
		    contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(db, principal, siteNodeId, languageId, true, "Meta information", null);		

		if(contentVO == null)
			throw new SystemException("There was no Meta Information bound to this page which makes it impossible to render.");	

	    String cacheName 	= "componentEditorCache";
		String cacheKey		= "pageComponentString_" + siteNodeId + "_" + languageId + "_" + contentId;
		String versionKey 	= cacheKey + "_contentVersionId";

		String attributeName = "ComponentStructure";

	    String cachedPageComponentsString = (String)CacheController.getCachedObject(cacheName, cacheKey);
	    Set contentVersionId = (Set)CacheController.getCachedObjectFromAdvancedCache("contentVersionCache", versionKey);

		if(cachedPageComponentsString != null)
		{			
		    return cachedPageComponentsString;
		}
		
		String pageComponentsString = null;
   					
		Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId).getId();
		//pageComponentsString = ContentDeliveryController.getContentDeliveryController().getContentAttribute(db, contentVO.getContentId(), masterLanguageId, "ComponentStructure", siteNodeId, true, null, principal, false, true);
		pageComponentsString = ContentController.getContentController().getContentAttribute(db, contentVO.getContentId(), masterLanguageId, "ComponentStructure");
		
		if(pageComponentsString == null)
			throw new SystemException("There was no Meta Information bound to this page which makes it impossible to render.");	
				    		
		return pageComponentsString;
	}

	public Document getComponentPropertiesDOM4JDocument(Integer siteNodeId, Integer languageId, Integer contentId, Database db, InfoGluePrincipal principal) throws SystemException, Exception
	{ 
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentPropertiesDocument_" + siteNodeId + "_" + languageId + "_" + contentId;
		Document cachedComponentPropertiesDocument = (Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesDocument != null)
			return cachedComponentPropertiesDocument;
		
		Document componentPropertiesDocument = null;
   	
		try
		{
			String xml = this.getComponentPropertiesString(siteNodeId, languageId, contentId, db, principal);
			//logger.info("xml: " + xml);
			if(xml != null && xml.length() > 0)
			{
				componentPropertiesDocument = domBuilder.getDocument(xml);
				
				CacheController.cacheObject(cacheName, cacheKey, componentPropertiesDocument);
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
		
		return componentPropertiesDocument;
	}

	/**
	 * This method fetches the template-string.
	 */
   
	private String getComponentPropertiesString(Integer siteNodeId, Integer languageId, Integer contentId, Database db, InfoGluePrincipal principal) throws SystemException, Exception
	{
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentPropertiesString_" + siteNodeId + "_" + languageId + "_" + contentId;
		String cachedComponentPropertiesString = (String)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesString != null)
			return cachedComponentPropertiesString;
			
		String componentPropertiesString = null;
   	
		try
		{
		    Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId).getId();
			componentPropertiesString = ContentController.getContentController().getContentAttribute(db, contentId, masterLanguageId, "ComponentProperties", true);
			//componentPropertiesString = ContentDeliveryController.getContentDeliveryController().getContentAttribute(db, contentId, masterLanguageId, "ComponentProperties", siteNodeId, true, null, principal, false, true);

			if(componentPropertiesString == null)
				throw new SystemException("There was no properties assigned to this content.");
		
			CacheController.cacheObject(cacheName, cacheKey, componentPropertiesString);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}

		return componentPropertiesString;
	}

	/**
	 * This method fetches the template-string.
	 */
   
	private String getComponentTemplateString(Integer componentContentId, Integer languageId, Database db, InfoGluePrincipal principal) throws SystemException, Exception
	{
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTemplateString_" + componentContentId + "_" + languageId;
		String cachedComponentPropertiesString = (String)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesString != null)
			return cachedComponentPropertiesString;
			
		String templateString = null;
   	
		try
		{
			ContentVO contentVO = ContentController.getContentController().getContentVOWithId(componentContentId, db);
		    Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForRepository(contentVO.getRepositoryId(), db).getId();
			templateString = ContentController.getContentController().getContentAttribute(db, componentContentId, masterLanguageId, "Template", true);

			if(templateString == null)
				throw new SystemException("There was no template on the content: " + componentContentId);
		
			CacheController.cacheObject(cacheName, cacheKey, templateString);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}

		return templateString;
	}

	
	
	
	public String getAvailableComponentsDiv(Database db, InfoGluePrincipal principal, Locale locale, Integer repositoryId, Integer languageId, Integer componentContentId, String slotName, String showLegend, String targetDiv)
	{
		StringBuffer sb = new StringBuffer();
		
	    try
	    {
	    	List<Slot> slots = getSlots(componentContentId, languageId, db, principal);
	    	Iterator<Slot> slotsIterator = slots.iterator();
	    	while(slotsIterator.hasNext())
	    	{
	    		Slot slot = slotsIterator.next();
	    		if(slot.getId().equals(slotName));
	    		{
	    	        String direction = "asc";
	    	        List componentVOList = ComponentController.getController().getComponentVOList("name", direction, slot.getAllowedComponentsArray(), slot.getDisallowedComponentsArray(), db, principal);
	    	        Iterator componentVOListIterator = componentVOList.iterator();

    	        	sb.append("<fieldset>");
    	        	sb.append("<legend>Drag component to slot</legend>");
    	        	
    	        	sb.append("<div id=\"availableComponents\">");
    	        	
    	        	int i = 0;
	    	        while(componentVOListIterator.hasNext())
	    	        {
	    	        	ContentVO componentContentVO = (ContentVO)componentVOListIterator.next();
	    	        	if(repositoryId != null && !componentContentVO.getRepositoryId().equals(repositoryId))
	    	        		continue;
	    	        	
	    	        	String imageUrl = getDigitalAssetUrl(componentContentVO.getId(), "thumbnail", db);
	    				if(imageUrl == null || imageUrl.length() == 0)
	    					imageUrl = "images/componentIcon.gif";

	    				sb.append("<div id=\"componentRow\" name=\"" +  componentContentVO.getId() + "\" class=\"dragable\">");
	    	    		
	    				i++;
	    				
	    	        	sb.append("	<div id=\"componentName\" style=\"padding-left: 22px; background-image: url('" + imageUrl + "'); background-repeat: no-repeat;\">" + componentContentVO.getName() + "</div>");

	    	        	/*
    					<td valign="middle" rowspan="2" style="border-bottom: 1px solid #666666;">
    						#if($content.extraProperties.get("Description") != "Unknown" && $content.extraProperties.get("Description") != "")
    						<img src="images/questionMarkGrad.gif" onclick="toggleDiv('descLayer$content.id');" aonMouseOver="showDiv('descLayer$content.id');" aonMouseOut="hideDiv('descLayer$content.id');">
    						#else
    						&nbsp;
    						#end
    					</td>

    					<td colspan="3" style="height: 0px; border-bottom: 1px solid #666666;">
    						<span id="descLayer$content.id" style="display:none; padding-bottom: 4px;"><b>Description:</b><br/>
    						#if($content.extraProperties.get("Description"))
    							$content.extraProperties.get("Description")
    						#else
    							No description
    						#end
    						</span>
    					</td>
	    				#set($imageUrl = "")
	    	    		*/

	    	        	sb.append("</div>");
	    	        }
	    	        
		        	sb.append("</fieldset>");
	    	        break;
	    		}
	    	}
	        
	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();
	    }
	
		return sb.toString();
	}

	public List<Slot> getSlots(Integer componentContentId, Integer languageId, InfoGluePrincipal principal) throws Exception
	{
		List<Slot> slots = new ArrayList<Slot>();
	
		Database db = CastorDatabaseService.getDatabase();

		beginTransaction(db);
		
		try
	    {
			slots = getSlots(componentContentId, languageId, db, principal);
			
	        commitTransaction(db);
	    }
	    catch(Exception e)
	    {
	    	//logger.error("An error occurred so we should not completes the transaction:" + e, e);
	        rollbackTransaction(db);
	        throw new SystemException(e.getMessage());
	    }
	    
	    return slots;
	}
	
	public List<Slot> getSlots(Integer componentContentId, Integer languageId, Database db, InfoGluePrincipal principal) throws Exception
	{
		List<Slot> slots = new ArrayList<Slot>();
		
		String template = getComponentTemplateString(componentContentId, languageId, db, principal);
		
		int offset = 0;
		int slotStartIndex = template.indexOf("<ig:slot", offset);
		//logger.info("slotStartIndex:" + slotStartIndex);
		while(slotStartIndex > -1)
		{
			int slotStopIndex = template.indexOf("</ig:slot>", slotStartIndex);
			
			String slot = template.substring(slotStartIndex, slotStopIndex + 10);
			String id = slot.substring(slot.indexOf("id") + 4, slot.indexOf("\"", slot.indexOf("id") + 4));
			
			Slot slotBean = new Slot();
		    slotBean.setId(id);

		    String[] allowedComponentNamesArray = null;
		    int allowedComponentNamesIndex = slot.indexOf(" allowedComponentNames");
			if(allowedComponentNamesIndex > -1)
			{    
			    String allowedComponentNames = slot.substring(allowedComponentNamesIndex + 24, slot.indexOf("\"", allowedComponentNamesIndex + 24));
			    allowedComponentNamesArray = allowedComponentNames.split(",");
			    slotBean.setAllowedComponentsArray(allowedComponentNamesArray);
			}

			String[] disallowedComponentNamesArray = null;
			int disallowedComponentNamesIndex = slot.indexOf(" disallowedComponentNames");
			if(disallowedComponentNamesIndex > -1)
			{    
			    String disallowedComponentNames = slot.substring(disallowedComponentNamesIndex + 27, slot.indexOf("\"", disallowedComponentNamesIndex + 27));
			    disallowedComponentNamesArray = disallowedComponentNames.split(",");
			    slotBean.setDisallowedComponentsArray(disallowedComponentNamesArray);
			}

			boolean inherit = true;
			int inheritIndex = slot.indexOf("inherit");
			if(inheritIndex > -1)
			{    
			    String inheritString = slot.substring(inheritIndex + 9, slot.indexOf("\"", inheritIndex + 9));
			    inherit = Boolean.parseBoolean(inheritString);
			}
			slotBean.setInherit(inherit);
			
			boolean disableAccessControl = false;
			int disableAccessControlIndex = slot.indexOf("disableAccessControl");
			if(disableAccessControlIndex > -1)
			{    
			    String disableAccessControlString = slot.substring(disableAccessControlIndex + "disableAccessControl".length() + 2, slot.indexOf("\"", disableAccessControlIndex + "disableAccessControl".length() + 2));
			    disableAccessControl = Boolean.parseBoolean(disableAccessControlString);
			}

			String addComponentText = null;
			int addComponentTextIndex = slot.indexOf("addComponentText");
			if(addComponentTextIndex > -1)
			{    
			    addComponentText = slot.substring(addComponentTextIndex + "addComponentText".length() + 2, slot.indexOf("\"", addComponentTextIndex + "addComponentText".length() + 2));
			}

			String addComponentLinkHTML = null;
			int addComponentLinkHTMLIndex = slot.indexOf("addComponentLinkHTML");
			if(addComponentLinkHTMLIndex > -1)
			{    
			    addComponentLinkHTML = slot.substring(addComponentLinkHTMLIndex + "addComponentLinkHTML".length() + 2, slot.indexOf("\"", addComponentLinkHTMLIndex + "addComponentLinkHTML".length() + 2));
			}

			int allowedNumberOfComponentsInt = -1;
			int allowedNumberOfComponentsIndex = slot.indexOf("allowedNumberOfComponents");
			if(allowedNumberOfComponentsIndex > -1)
			{    
				String allowedNumberOfComponents = slot.substring(allowedNumberOfComponentsIndex + "allowedNumberOfComponents".length() + 2, slot.indexOf("\"", allowedNumberOfComponentsIndex + "allowedNumberOfComponents".length() + 2));
				try
				{
					allowedNumberOfComponentsInt = new Integer(allowedNumberOfComponents);
				}
				catch (Exception e) 
				{
					allowedNumberOfComponentsInt = -1;
				}
			}

			slotBean.setDisableAccessControl(disableAccessControl);
			slotBean.setAddComponentLinkHTML(addComponentLinkHTML);
		    slotBean.setAddComponentText(addComponentText);
		    slotBean.setAllowedNumberOfComponents(new Integer(allowedNumberOfComponentsInt));
		    
		    slots.add(slotBean);
		    
			offset = slotStopIndex + 10;
			slotStartIndex = template.indexOf("<ig:slot", offset);
		}
		
		return slots;
	}
	
		/**
	 * This method fetches an url to the asset for the component.
	 */
	
	public String getDigitalAssetUrl(Integer contentId, String key, Database db) throws Exception
	{
		String imageHref = null;
		try
		{
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(ContentController.getContentController().getContentVOWithId(contentId, db).getRepositoryId(), db);
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentId, masterLanguage.getId(), db);
			if(contentVersionVO != null)
			{
				List digitalAssets = DigitalAssetController.getDigitalAssetVOList(contentVersionVO.getId(), db);
				Iterator i = digitalAssets.iterator();
				while(i.hasNext())
				{
					DigitalAssetVO digitalAssetVO = (DigitalAssetVO)i.next();
					if(digitalAssetVO.getAssetKey().equals(key))
					{
						imageHref = DigitalAssetController.getController().getDigitalAssetUrl(digitalAssetVO, db); 
						break;
					}
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
	 * This method gets the component structure on the page.
	 *
	 * @author mattias
	 */

	protected List getPageComponents(Database db, String componentXML, Element element, String slotName, Slot containerSlot, InfoGlueComponent parentComponent, Integer siteNodeId, Integer languageId, InfoGluePrincipal principal) throws Exception
	{
		//List components = new ArrayList();
		
		Locale locale = LanguageDeliveryController.getLanguageDeliveryController().getLocaleWithId(db, languageId);

		String key = "" + componentXML.hashCode() + "_" +  languageId + "_" + slotName;
		if(parentComponent != null)
			key = "" + componentXML.hashCode() + "_" +  languageId + "_" + slotName + "_" + parentComponent.getId() + "_" + parentComponent.getName() + "_" + parentComponent.getIsInherited();
		
		Object componentsCandidate = CacheController.getCachedObjectFromAdvancedCache("pageComponentsCache", key);
		List components = new ArrayList();
		String[] groups = null;
			
		if(componentsCandidate != null)
		{
			if(componentsCandidate instanceof NullObject)
				components = null;				
			else
				components = (List)componentsCandidate;
		}
		else
		{
			String componentXPath = "component[@name='" + slotName + "']";
			//System.out.println("componentXPath:" + componentXPath);
			List componentElements = element.selectNodes(componentXPath);
			//logger.info("componentElements:" + componentElements.size());
			//System.out.println("componentElements:" + componentElements.size());
			
			Iterator componentIterator = componentElements.iterator();
			int slotPosition = 0;
			while(componentIterator.hasNext())
			{
				Element componentElement = (Element)componentIterator.next();
			
				Integer id 							= new Integer(componentElement.attributeValue("id"));
				Integer contentId 					= new Integer(componentElement.attributeValue("contentId"));
				String name 	  					= componentElement.attributeValue("name");
				String isInherited 					= componentElement.attributeValue("isInherited");
				String pagePartTemplateContentId 	= componentElement.attributeValue("pagePartTemplateContentId");
				
				//System.out.println("id 1: " + id);
				//System.out.println("contentId 1:" + contentId);
				//System.out.println("name 1: " + name);
				//System.out.println("isInherited 1: " + isInherited);
				//System.out.println("pagePartTemplateContentId 1: " + pagePartTemplateContentId);
				
				try
				{
				    ContentVO contentVO = ContentDeliveryController.getContentDeliveryController().getContentVO(contentId, db);
				    //System.out.println("contentVO for current component:" + contentVO.getName());
				   
				    //System.out.println("slotName:" + slotName + " should get connected with content_" + contentVO.getId());
				    
				    groups = new String[]{"content_" + contentVO.getId()};
				    
					InfoGlueComponent component = new InfoGlueComponent();
					component.setPositionInSlot(new Integer(slotPosition));
					component.setId(id);
					component.setContentId(contentId);
					component.setName(contentVO.getName());
					component.setSlotName(name);
					component.setParentComponent(parentComponent);
					if(containerSlot != null)
					{
						System.out.println("Adding component to container slot:" + component.getId() + ":" + containerSlot.getId());
						//containerSlot.getComponents().add(component);
						component.setContainerSlot(containerSlot);
						//System.out.println("containerSlot:" + containerSlot);
						//System.out.println("containerSlot:" + containerSlot);
					}
					if(isInherited != null && isInherited.equals("true"))
						component.setIsInherited(true);
					else if(parentComponent != null)
						component.setIsInherited(parentComponent.getIsInherited());

					if(pagePartTemplateContentId != null && !pagePartTemplateContentId.equals("") && !pagePartTemplateContentId.equals("-1"))
					{
						Integer pptContentId = new Integer(pagePartTemplateContentId);
					    ContentVO pptContentIdContentVO = ContentDeliveryController.getContentDeliveryController().getContentVO(pptContentId, db);

						InfoGlueComponent partTemplateReferenceComponent = new InfoGlueComponent();
						partTemplateReferenceComponent.setPositionInSlot(new Integer(slotPosition));
						partTemplateReferenceComponent.setId(id);
						//System.out.println("Setting component:" + partTemplateReferenceComponent.getId() + " - " + partTemplateReferenceComponent.getPositionInSlot());
						partTemplateReferenceComponent.setContentId(pptContentId);
						partTemplateReferenceComponent.setName(pptContentIdContentVO.getName());
						partTemplateReferenceComponent.setSlotName(name);
						partTemplateReferenceComponent.setParentComponent(parentComponent);
						if(containerSlot != null)
						{
							//System.out.println("Adding component to container slot:" + partTemplateReferenceComponent.getId() + ":" + containerSlot.getId());
							partTemplateReferenceComponent.setContainerSlot(containerSlot);
							//containerSlot.getComponents().add(partTemplateReferenceComponent);
						}
						partTemplateReferenceComponent.setIsInherited(true);
						
						component.setPagePartTemplateContentId(pptContentId);
						component.setPagePartTemplateComponent(partTemplateReferenceComponent);
					}
			
					//Use this later
					//getComponentProperties(componentElement, component, locale, templateController);
					List propertiesNodeList = componentElement.selectNodes("properties");
					if(propertiesNodeList.size() > 0)
					{
						Element propertiesElement = (Element)propertiesNodeList.get(0);
						
						List propertyNodeList = propertiesElement.selectNodes("property");
						Iterator propertyNodeListIterator = propertyNodeList.iterator();
						while(propertyNodeListIterator.hasNext())
						{
							Element propertyElement = (Element)propertyNodeListIterator.next();
							
							String propertyName = propertyElement.attributeValue("name");
							String type = propertyElement.attributeValue("type");
							String path = propertyElement.attributeValue("path");
			
							if(path == null)
							{
								LanguageVO langaugeVO = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(db, siteNodeId);
								if(propertyElement.attributeValue("path_" + langaugeVO.getLanguageCode()) != null)
									path = propertyElement.attributeValue("path_" + langaugeVO.getLanguageCode());
							}
								
							if(propertyElement.attributeValue("path_" + locale.getLanguage()) != null)
								path = propertyElement.attributeValue("path_" + locale.getLanguage());
					
							if(path == null || path.equals(""))
							{
								System.out.println("Falling back to content master language 1 for property:" + propertyName);
								LanguageVO contentMasterLangaugeVO = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForRepository(db, contentVO.getRepositoryId());
								if(propertyElement.attributeValue("path_" + contentMasterLangaugeVO.getLanguageCode()) != null)
									path = propertyElement.attributeValue("path_" + contentMasterLangaugeVO.getLanguageCode());	
							}

							Map property = new HashMap();
							property.put("name", propertyName);
							property.put("path", path);
							property.put("type", type);
							
							if(propertyName.equals(InfoGlueComponent.CACHE_RESULT_PROPERTYNAME) && (path.equalsIgnoreCase("true") || path.equalsIgnoreCase("yes")))
							{
								component.setCacheResult(true);
							}
							if(propertyName.equals(InfoGlueComponent.UPDATE_INTERVAL_PROPERTYNAME) && !path.equals(""))
							{
								try { component.setUpdateInterval(Integer.parseInt(path)); } catch (Exception e) { logger.warn("The component " + component.getName() + " " + InfoGlueComponent.UPDATE_INTERVAL_PROPERTYNAME + " with a faulty value on page with siteNodeId=" + siteNodeId + ":" + e.getMessage()); }
							}
							if(propertyName.equals(InfoGlueComponent.CACHE_KEY_PROPERTYNAME) && !path.equals(""))
							{
								component.setCacheKey(path);
							}
							if(propertyName.equals(InfoGlueComponent.PREPROCESSING_ORDER_PROPERTYNAME) && !path.equals(""))
							{
								component.setPreProcessingOrder(path);
							}

							List<ComponentBinding> bindings = new ArrayList<ComponentBinding>();
							List bindingNodeList = propertyElement.selectNodes("binding");
							Iterator bindingNodeListIterator = bindingNodeList.iterator();
							while(bindingNodeListIterator.hasNext())
							{
								Element bindingElement = (Element)bindingNodeListIterator.next();
								String entity = bindingElement.attributeValue("entity");
								String entityId = bindingElement.attributeValue("entityId");
								String assetKey = bindingElement.attributeValue("assetKey");

								ComponentBinding componentBinding = new ComponentBinding();
								//componentBinding.setId(new Integer(id));
								//componentBinding.setComponentId(componentId);
								componentBinding.setEntityClass(entity);
								componentBinding.setEntityId(new Integer(entityId));
								componentBinding.setAssetKey(assetKey);
								componentBinding.setBindingPath(path);
								
								bindings.add(componentBinding);
							}
			
							property.put("bindings", bindings);
							
							component.getProperties().put(propertyName, property);
						}
					}
					
					
					getComponentRestrictions(componentElement, component, locale);
					
					//Getting slots for the component
					try
					{
						String componentString = getComponentTemplateString(contentId, languageId, db, principal);
						
						int offset = 0;
						int slotStartIndex = componentString.indexOf("<ig:slot", offset);
						while(slotStartIndex > -1)
						{
							int slotStopIndex = componentString.indexOf("</ig:slot>", slotStartIndex);
							String slotString = componentString.substring(slotStartIndex, slotStopIndex + 10);
							String slotId = slotString.substring(slotString.indexOf("id") + 4, slotString.indexOf("\"", slotString.indexOf("id") + 4));
							
							boolean inherit = true;
							int inheritIndex = slotString.indexOf("inherit");
							if(inheritIndex > -1)
							{    
							    String inheritString = slotString.substring(inheritIndex + 9, slotString.indexOf("\"", inheritIndex + 9));
							    inherit = Boolean.parseBoolean(inheritString);
							}
	
							boolean disableAccessControl = false;
							int disableAccessControlIndex = slotString.indexOf("disableAccessControl");
							if(disableAccessControlIndex > -1)
							{    
							    String disableAccessControlString = slotString.substring(disableAccessControlIndex + "disableAccessControl".length() + 2, slotString.indexOf("\"", disableAccessControlIndex + "disableAccessControl".length() + 2));
							    disableAccessControl = Boolean.parseBoolean(disableAccessControlString);
							}

							String[] allowedComponentNamesArray = null;
							int allowedComponentNamesIndex = slotString.indexOf(" allowedComponentNames");
							if(allowedComponentNamesIndex > -1)
							{    
							    String allowedComponentNames = slotString.substring(allowedComponentNamesIndex + 24, slotString.indexOf("\"", allowedComponentNamesIndex + 24));
							    allowedComponentNamesArray = allowedComponentNames.split(",");
							}

							String[] disallowedComponentNamesArray = null;
							int disallowedComponentNamesIndex = slotString.indexOf(" disallowedComponentNames");
							if(disallowedComponentNamesIndex > -1)
							{    
							    String disallowedComponentNames = slotString.substring(disallowedComponentNamesIndex + 27, slotString.indexOf("\"", disallowedComponentNamesIndex + 27));
							    disallowedComponentNamesArray = disallowedComponentNames.split(",");
							}
							
							String addComponentText = null;
							int addComponentTextIndex = slotString.indexOf("addComponentText");
							if(addComponentTextIndex > -1)
							{    
							    addComponentText = slotString.substring(addComponentTextIndex + "addComponentText".length() + 2, slotString.indexOf("\"", addComponentTextIndex + "addComponentText".length() + 2));
							}

							String addComponentLinkHTML = null;
							int addComponentLinkHTMLIndex = slotString.indexOf("addComponentLinkHTML");
							if(addComponentLinkHTMLIndex > -1)
							{    
							    addComponentLinkHTML = slotString.substring(addComponentLinkHTMLIndex + "addComponentLinkHTML".length() + 2, slotString.indexOf("\"", addComponentLinkHTMLIndex + "addComponentLinkHTML".length() + 2));
							}

							int allowedNumberOfComponentsInt = -1;
							int allowedNumberOfComponentsIndex = slotString.indexOf("allowedNumberOfComponents");
							if(allowedNumberOfComponentsIndex > -1)
							{    
								String allowedNumberOfComponents = slotString.substring(allowedNumberOfComponentsIndex + "allowedNumberOfComponents".length() + 2, slotString.indexOf("\"", allowedNumberOfComponentsIndex + "allowedNumberOfComponents".length() + 2));
								try
								{
									allowedNumberOfComponentsInt = new Integer(allowedNumberOfComponents);
								}
								catch (Exception e) 
								{
									allowedNumberOfComponentsInt = -1;
								}
							}

							Slot slot = new Slot();
							slot.setId(slotId);
							slot.setInherit(inherit);
							slot.setDisableAccessControl(disableAccessControl);
							slot.setAllowedComponentsArray(allowedComponentNamesArray);
							slot.setDisallowedComponentsArray(disallowedComponentNamesArray);
						    slot.setAddComponentLinkHTML(addComponentLinkHTML);
						    slot.setAddComponentText(addComponentText);
						    slot.setAllowedNumberOfComponents(new Integer(allowedNumberOfComponentsInt));

							Element componentsElement = (Element)componentElement.selectSingleNode("components");
							
							//groups = new String[]{"content_" + contentVO.getId()};
							
							List subComponents = getPageComponents(db, componentXML, componentsElement, slotId, slot, component, siteNodeId, languageId, principal);
							//logger.info("subComponents:" + subComponents);
							slot.setComponents(subComponents);
							
							component.getSlotList().add(slot);
					
							offset = slotStopIndex;
							slotStartIndex = componentString.indexOf("<ig:slot", offset);
						}
					}
					catch(Exception e)
					{		
						e.printStackTrace();
						logger.warn("An component with either an empty template or with no template in the sitelanguages was found:" + e.getMessage(), e);	
					}
					
					components.add(component);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					logger.warn("There was deleted referenced component or some other problem when rendering siteNode: " + siteNodeId + ") in language " + languageId + ":" + e.getMessage(), e);
				}
				slotPosition++;
			}			
		}		
		
		if(groups == null)
			groups = new String[]{"selectiveCacheUpdateNonApplicable"};
		
		if(components != null)
			CacheController.cacheObjectInAdvancedCache("pageComponentsCache", key, components, groups, false);
		else
			CacheController.cacheObjectInAdvancedCache("pageComponentsCache", key, new NullObject(), groups, false);
		
		//System.out.println("Returning " + components.size() + " components for:" + slotName);
		return components;
	}

	/**
	 * This method gets the restrictions for this component
	 */
	private void getComponentRestrictions(Element child, InfoGlueComponent component, Locale locale) throws Exception
	{
	    //System.out.println("Getting restrictions for " + component.getId() + ":" + child.getName());
		List restrictionsNodeList = child.selectNodes("restrictions");
		//logger.info("restrictionsNodeList:" + restrictionsNodeList.getLength());
		if(restrictionsNodeList.size() > 0)
		{
			Element restrictionsElement = (Element)restrictionsNodeList.get(0);
			
			List restrictionNodeList = restrictionsElement.selectNodes("restriction");
			//logger.info("restrictionNodeList:" + restrictionNodeList.getLength());
			Iterator restrictionNodeListIterator = restrictionNodeList.iterator();
			while(restrictionNodeListIterator.hasNext())
			{
				Element restrictionElement = (Element)restrictionNodeListIterator.next();
				
				ComponentRestriction restriction = new ComponentRestriction();
			    
				String type = restrictionElement.attributeValue("type");
				if(type.equals("blockComponents"))
				{
				    String slotId = restrictionElement.attributeValue("slotId");
				    String arguments = restrictionElement.attributeValue("arguments");

				    restriction.setType(type);
					restriction.setSlotId(slotId);
					restriction.setArguments(arguments);
				}
				
				component.getRestrictions().add(restriction);
			}
		}
	}
	
	private InfoGlueComponent getComponentWithId(InfoGlueComponent parentComponent, Integer componentId)
	{
		InfoGlueComponent component = null;
		
		Iterator slotListIterator = parentComponent.getSlotList().iterator();
		outer:while(slotListIterator.hasNext())
		{
			Slot slot = (Slot)slotListIterator.next();
			//System.out.println("slot:" + slot.getId());
			
			List components = slot.getComponents();
			//System.out.println("components:" + components.size());
			
			Iterator componentsIterator = components.iterator();
			while(componentsIterator.hasNext())
			{
				InfoGlueComponent subComponent = (InfoGlueComponent)componentsIterator.next();
				//System.out.println("subComponent:" + subComponent.getId());
				if(subComponent.getId().equals(componentId))
				{
					component = subComponent;
					break outer;
				}
				else
				{
					component = getComponentWithId(subComponent, componentId);
					if(component != null)
						break;
				}
			}
		}
		
		return component;
	}

	/*
	 * This method returns a bean representing a list of ComponentProperties that the component has.
	 */
	 
	private List getComponentTasks(Integer componentId, Document document) throws Exception
	{
		List componentTasks = new ArrayList();
		Timer timer = new Timer();
		timer.setActive(false);

		try
		{
			if(document != null)
			{
				timer.printElapsedTime("Read document");

				String propertyXPath = "//task";
				//logger.info("propertyXPath:" + propertyXPath);
				List anl = document.selectNodes(propertyXPath);
				timer.printElapsedTime("Set property xpath");
				Iterator anlIterator = anl.iterator();
				while(anlIterator.hasNext())
				{
					Element binding = (Element)anlIterator.next();
					
					String name			= binding.attributeValue("name");
					String view			= binding.attributeValue("view");
					String openInPopup 	= binding.attributeValue("openInPopup");
					if(openInPopup == null || (!openInPopup.equals("true") && !openInPopup.equals("false")))
						openInPopup = "true";
					
					if(logger.isInfoEnabled())
					{
						logger.info("name:" + name);
						logger.info("view:" + view);
						logger.info("openInPopup:" + openInPopup);
					}
					
					ComponentTask task = new ComponentTask();
					task.setName(name);
					task.setView(view);
					task.setOpenInPopup(new Boolean(openInPopup));
					task.setComponentId(componentId);
					
					componentTasks.add(task);
				}
			}
		}
		catch(Exception e)
		{
			logger.warn("The component with id " + componentId + " had a incorrect xml defining it's properties:" + e.getMessage(), e);
		}
							
		return componentTasks;
	}

	protected Document getComponentTasksDOM4JDocument(Integer masterLanguageId, Integer metaInfoContentId, Database db) throws SystemException, Exception
	{ 	    
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTasksDocument_" + masterLanguageId + "_" + metaInfoContentId;
		Document cachedComponentTasksDocument = (Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentTasksDocument != null)
			return cachedComponentTasksDocument;
		
		Document componentTasksDocument = null;
   	
		try
		{
			String xml = this.getComponentTasksString(masterLanguageId, metaInfoContentId, db);
			if(xml != null && xml.length() > 0)
			{
			    componentTasksDocument = domBuilder.getDocument(xml);
				
				CacheController.cacheObject(cacheName, cacheKey, componentTasksDocument);
			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}
		
		return componentTasksDocument;
	}

	/**
	 * This method fetches the tasks for a certain component.
	 */
   
	private String getComponentTasksString(Integer masterLanguageId, Integer metaInfoContentId, Database db) throws SystemException, Exception
	{
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTasksString_" + masterLanguageId + "_" + metaInfoContentId;
		String cachedComponentTasksString = (String)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentTasksString != null)
			return cachedComponentTasksString;
			
		String componentTasksString = null;
   	
		try
		{
		    componentTasksString = ContentController.getContentController().getContentAttribute(db, metaInfoContentId, masterLanguageId, "ComponentTasks");

			if(componentTasksString == null)
				throw new SystemException("There was no tasks assigned to this content.");
		
			CacheController.cacheObject(cacheName, cacheKey, componentTasksString);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			throw e;
		}

		return componentTasksString;
	}

	public static String transformAttribute(String plainAttribute, String languageId)
	{
		String newAttribute = "";
		String remainingAttribute = plainAttribute;
		int startPosition;
		int endPosition;

		startPosition = remainingAttribute.indexOf("$templateLogic.getInlineAssetUrl(");
		while(startPosition > -1)
		{
			newAttribute = newAttribute + remainingAttribute.substring(0, startPosition);
			remainingAttribute = remainingAttribute.substring(startPosition + 33);
			
			int seperatorCharIndex = remainingAttribute.indexOf(",");
			String contentId = remainingAttribute.substring(0, seperatorCharIndex);
			String assetKey = remainingAttribute.substring(seperatorCharIndex + 1, remainingAttribute.indexOf(")")).trim();
			assetKey = assetKey.substring(1, assetKey.length() - 1);
			
			newAttribute = newAttribute + "DownloadAsset.action?contentId=" + contentId +"&languageId=" + languageId + "&assetKey=" + assetKey + "\"";
			
			int endIndex = remainingAttribute.indexOf(")");
			
			remainingAttribute = remainingAttribute.substring(endIndex + 1);
			
			startPosition = remainingAttribute.indexOf("$templateLogic.getInlineAssetUrl(");
		}
		newAttribute = newAttribute + remainingAttribute;

		return newAttribute;
	}



	public static String untransformAttribute(String plainAttribute)
	{
		String newAttribute = "";
		String remainingAttribute = plainAttribute;
		int startPosition;
		int endPosition;

		startPosition = remainingAttribute.indexOf("DownloadAsset.action?contentId=");
		while(startPosition > -1)
		{
			newAttribute = newAttribute + remainingAttribute.substring(0, startPosition);
			remainingAttribute = remainingAttribute.substring(startPosition + 31);
			
			int seperatorCharIndex = remainingAttribute.indexOf("&amp;");
			String contentId = remainingAttribute.substring(0, seperatorCharIndex);
			String assetKey = remainingAttribute.substring(seperatorCharIndex + 1, remainingAttribute.indexOf("\""));
			int assetStartIndex = assetKey.indexOf("assetKey=") + 9;
			assetKey = assetKey.substring(assetStartIndex);
			
			newAttribute = newAttribute + "$templateLogic.getInlineAssetUrl(" + contentId + ", \"" + assetKey + "\")\"";
			
			int endIndex = remainingAttribute.indexOf("\"");
			
			remainingAttribute = remainingAttribute.substring(endIndex + 1);
			
			startPosition = remainingAttribute.indexOf("DownloadAsset.action?contentId=");
		}
		newAttribute = newAttribute + remainingAttribute;

		return newAttribute;
	}	

	/*
	 * This method returns a bean representing a list of ComponentProperties that the component has.
	 */
	 
	public void addSystemProperties(List componentProperties, Integer componentId, Integer siteNodeId, Integer languageId, Integer contentId, Locale locale, Database db, InfoGluePrincipal principal) throws Exception
	{
		ComponentProperty cacheResultProperty = new ComponentProperty();
		cacheResultProperty.setComponentId(componentId);
		cacheResultProperty.setName("CacheResult");
		cacheResultProperty.setDisplayName("Cache Result");
		cacheResultProperty.setDescription("Do you want to cache the components rendered result.");
		cacheResultProperty.setDefaultValue("false");
		cacheResultProperty.setDataProvider("");
		cacheResultProperty.setType("select");
		cacheResultProperty.setVisualizingAction("");
		cacheResultProperty.setCreateAction("");
		
		ComponentPropertyOption cpoNo = new ComponentPropertyOption("No", "false");
		ComponentPropertyOption cpoYes = new ComponentPropertyOption("Yes", "true");
		cacheResultProperty.getOptions().add(cpoNo);
		cacheResultProperty.getOptions().add(cpoYes);
			
		String value = getComponentPropertyValue(componentId, "CacheResult", siteNodeId, languageId, contentId, locale, db, principal, cacheResultProperty);
		cacheResultProperty.setValue(value);
		
		componentProperties.add(cacheResultProperty);

		ComponentProperty cacheIntervalProperty = new ComponentProperty();
		cacheIntervalProperty.setComponentId(componentId);
		cacheIntervalProperty.setName("UpdateInterval");
		cacheIntervalProperty.setDisplayName("Cache Update Interval");
		cacheIntervalProperty.setDescription("Interval before the cache gets updated");
		cacheIntervalProperty.setDefaultValue("-1");
		cacheIntervalProperty.setDataProvider("");
		cacheIntervalProperty.setType("select");
		cacheIntervalProperty.setVisualizingAction("");
		cacheIntervalProperty.setCreateAction("");
		
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("1 second", "1"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("2 seconds", "2"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("3 seconds", "3"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("4 seconds", "4"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("5 seconds", "5"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("10 seconds", "10"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("15 seconds", "15"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("20 seconds", "20"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("30 seconds", "30"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("1 minute", "60"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("2 minutes", "120"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("5 minutes", "300"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("10 minutes", "600"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("30 minutes", "1800"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("1 hour", "3600"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("2 hours", "7200"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("6 hours", "21600"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("12 hours", "43200"));
		cacheIntervalProperty.getOptions().add(new ComponentPropertyOption("24 hours", "86400"));

		String updateIntervalValue = getComponentPropertyValue(componentId, "UpdateInterval", siteNodeId, languageId, contentId, locale, db, principal, cacheIntervalProperty);
		cacheIntervalProperty.setValue(updateIntervalValue);
		
		componentProperties.add(cacheIntervalProperty);

		ComponentProperty cacheKeyProperty = new ComponentProperty();
		cacheKeyProperty.setComponentId(componentId);
		cacheKeyProperty.setName("CacheKey");
		cacheKeyProperty.setDisplayName("Cache Key");
		cacheKeyProperty.setDescription("Key for the component cache");
		cacheKeyProperty.setDefaultValue("");
		cacheKeyProperty.setDataProvider("");
		cacheKeyProperty.setType("textfield");
		cacheKeyProperty.setVisualizingAction("");
		cacheKeyProperty.setCreateAction("");
		
		String cacheKeyValue = getComponentPropertyValue(componentId, "CacheKey", siteNodeId, languageId, contentId, locale, db, principal, cacheKeyProperty);
		cacheKeyProperty.setValue(cacheKeyValue);
		
		componentProperties.add(cacheKeyProperty);

		ComponentProperty priorityProperty = new ComponentProperty();
		priorityProperty.setComponentId(componentId);
		priorityProperty.setName("PreRenderOrder");
		priorityProperty.setDisplayName("Pre processing order");
		priorityProperty.setDescription("State the order in which the component get's prerendered");
		priorityProperty.setDefaultValue("99");
		priorityProperty.setDataProvider("");
		priorityProperty.setType("select");
		priorityProperty.setVisualizingAction("");
		priorityProperty.setCreateAction("");
		
		for(int i=0; i<15; i++)
			priorityProperty.getOptions().add(new ComponentPropertyOption("" + i, "" + i));

		String preRenderOrderPropertyValue = getComponentPropertyValue(componentId, "PreRenderOrder", siteNodeId, languageId, contentId, locale, db, principal, priorityProperty);
		priorityProperty.setValue(preRenderOrderPropertyValue);
		
		componentProperties.add(priorityProperty);

	}
}
