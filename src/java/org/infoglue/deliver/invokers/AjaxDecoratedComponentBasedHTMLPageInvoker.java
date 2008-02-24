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

package org.infoglue.deliver.invokers;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.PageTemplateController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.io.FileHelper;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.cms.util.XMLHelper;
import org.infoglue.cms.util.dom.DOMBuilder;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.infoglue.deliver.applications.databeans.ComponentBinding;
import org.infoglue.deliver.applications.databeans.ComponentProperty;
import org.infoglue.deliver.applications.databeans.ComponentPropertyOption;
import org.infoglue.deliver.applications.databeans.ComponentTask;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.applications.databeans.Slot;
import org.infoglue.deliver.controllers.kernel.impl.simple.BasicTemplateController;
import org.infoglue.deliver.controllers.kernel.impl.simple.ContentDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.DecoratedComponentLogic;
import org.infoglue.deliver.controllers.kernel.impl.simple.IntegrationDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.LanguageDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.NodeDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.Timer;
import org.infoglue.deliver.util.VelocityTemplateProcessor;
//import org.w3c.dom.NodeList;

/**
* @author Mattias Bogeblad
*
* This class delivers a normal html page by using the component-based method but also decorates it
* so it can be used by the structure tool to manage the page components.
*/

public class AjaxDecoratedComponentBasedHTMLPageInvoker extends ComponentBasedHTMLPageInvoker
{
	private final static DOMBuilder domBuilder = new DOMBuilder();
	private final static VisualFormatter formatter = new VisualFormatter();
	
    private final static Logger logger = Logger.getLogger(AjaxDecoratedComponentBasedHTMLPageInvoker.class.getName());

	//private String propertiesDivs 	= "";
	//private String tasksDivs 		= "";
	
	/**
	 * This is the method that will render the page. It uses the new component based structure. 
	 */ 
	
	public void invokePage() throws SystemException, Exception
	{
		Timer timer = new Timer();
		timer.setActive(false);
		
		String decoratePageTemplate = "";
		
		NodeDeliveryController nodeDeliveryController = NodeDeliveryController.getNodeDeliveryController(this.getDeliveryContext());
		
		timer.printElapsedTime("Initialized controllers");
		
		Integer repositoryId = nodeDeliveryController.getSiteNode(getDatabase(), this.getDeliveryContext().getSiteNodeId()).getRepository().getId();
		String componentXML = getPageComponentsString(getDatabase(), this.getTemplateController(), this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), this.getDeliveryContext().getContentId());
		//logger.info("componentXML:" + componentXML);
		
		componentXML = appendPagePartTemplates(componentXML);
		
		timer.printElapsedTime("After getPageComponentsString");
		
		Timer decoratorTimer = new Timer();
		decoratorTimer.setActive(false);

		InfoGlueComponent baseComponent = null;
		
		if(componentXML == null || componentXML.length() == 0)
		{
			decoratePageTemplate = showInitialBindingDialog(this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), this.getDeliveryContext().getContentId());
		}
		else
		{
		    Document document = null;
		    try
		    {
		        document = domBuilder.getDocument(componentXML);
		    }
		    catch(Exception e)
		    {
		        throw new SystemException("There was a problem parsing the component structure on the page. Could be invalid XML in the ComponentStructure attribute. Message:" + e.getMessage(), e);
		    }
		    
			decoratorTimer.printElapsedTime("After reading document");
			
			List pageComponents = getPageComponents(getDatabase(), componentXML, document.getRootElement(), "base", this.getTemplateController(), null);

			if(pageComponents.size() > 0)
			{
				baseComponent = (InfoGlueComponent)pageComponents.get(0);
			}

			decoratorTimer.printElapsedTime("After getting basecomponent");
			
			if(baseComponent == null)
			{
				decoratePageTemplate = showInitialBindingDialog(this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), this.getDeliveryContext().getContentId());
			}
			else
			{
				//if(this.getDeliveryContext().getShowSimple() == true)
			    //{
			    //    decoratePageTemplate = showSimplePageStructure(this.getTemplateController(), repositoryId, this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), baseComponent);
			    //}
			    //else
			    //{
				    ContentVO metaInfoContentVO = nodeDeliveryController.getBoundContent(getDatabase(), this.getTemplateController().getPrincipal(), this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), true, "Meta information", this.getDeliveryContext());
					decoratePageTemplate = decorateComponent(baseComponent, this.getTemplateController(), repositoryId, this.getDeliveryContext().getSiteNodeId(), this.getDeliveryContext().getLanguageId(), this.getDeliveryContext().getContentId()/*, metaInfoContentVO.getId()*/);
					decoratePageTemplate = decorateTemplate(this.getTemplateController(), decoratePageTemplate, this.getDeliveryContext(), baseComponent);
				//}
			}
		}
		
		timer.printElapsedTime("After main decoration");
		
		//TODO - TEST
		//decoratePageTemplate += propertiesDivs + tasksDivs;
		decoratePageTemplate += "<div id=\"availableComponents\"></div><div id=\"componentTasks\"><div id=\"componentMenu\" class=\"skin0\">Loading menu...</div></div><div id=\"componentProperties\"></div><div id=\"pageComponentStructure\"></div>";
		
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();
		
		//-- moved the creation of a default context into the baseclass
		// (robert)
		Map context = getDefaultContext();

		context.put("componentEditorUrl", componentEditorUrl);
		boolean oldUseFullUrl = this.getTemplateController().getDeliveryContext().getUseFullUrl();
		this.getTemplateController().getDeliveryContext().setUseFullUrl(true);
		context.put("currentUrl", URLEncoder.encode(this.getTemplateController().getCurrentPageUrl(), "UTF-8"));
		context.put("contextName", this.getRequest().getContextPath());
		
		this.getTemplateController().getDeliveryContext().setUseFullUrl(oldUseFullUrl);
		StringWriter cacheString = new StringWriter();
		PrintWriter cachedStream = new PrintWriter(cacheString);
		
		new VelocityTemplateProcessor().renderTemplate(context, cachedStream, decoratePageTemplate, false, baseComponent);

		String pageString = cacheString.toString();
		//pageString = decorateHeadAndPageWithVarsFromComponents(pageString);

		this.setPageString(pageString);
		
		timer.printElapsedTime("End invokePage");
	}
	
	 /**
	  * This method prints out the first template dialog.
	  */

	 private String showInitialBindingDialog(Integer siteNodeId, Integer languageId, Integer contentId)
	 {
		 String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();
		 String url = "javascript:window.open('" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?eee=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&specifyBaseTemplate=true&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "', 'BaseTemplate', 'width=600,height=700,left=50,top=50,toolbar=no,status=no,scrollbars=yes,location=no,menubar=no,directories=no,resizable=yes');";
		 
		 String pageTemplateHTML = " or choose a page template below.<br><br>";
		 
	     boolean foundPageTemplate = false;

	     try
		 {
	    	 SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
	    	 LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
	    	 
	    	 InfoGluePrincipal principal = this.getTemplateController().getPrincipal();
	    	 String cmsUserName = (String)this.getTemplateController().getHttpServletRequest().getSession().getAttribute("cmsUserName");
	    	 if(cmsUserName != null)
	    		 principal = this.getTemplateController().getPrincipal(cmsUserName);
		    
		     List sortedPageTemplates = PageTemplateController.getController().getPageTemplates(principal, masterLanguageVO.getId());
			 Iterator sortedPageTemplatesIterator = sortedPageTemplates.iterator();
			 int index = 0;
			 pageTemplateHTML += "<table border=\"0\" width=\"80%\" cellspacing=\"0\"><tr>";
			 
		     while(sortedPageTemplatesIterator.hasNext())
			 {
			     ContentVO contentVO = (ContentVO)sortedPageTemplatesIterator.next();
			     ContentVersionVO contentVersionVO = this.getTemplateController().getContentVersion(contentVO.getId(), LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(getDatabase(), siteNodeId).getId(), false);
			     if(contentVersionVO != null)
			     {
				     String imageUrl = this.getTemplateController().getAssetUrl(contentVO.getId(), "thumbnail");
				     if(imageUrl == null || imageUrl.equals(""))
				         imageUrl = this.getRequest().getContextPath() + "/images/undefinedPageTemplate.jpg";
				 
				     pageTemplateHTML += "<td style=\"font-family:verdana, sans-serif; font-size:10px; border: 1px solid #C2D0E2; padding: 5px 5px 5px 5px;\" valign=\"bottom\" align=\"center\"><a href=\"" + componentEditorUrl + "ViewSiteNodePageComponents!addPageTemplate.action?repositoryId=" + contentVO.getRepositoryId() + "&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&pageTemplateContentId=" + contentVO.getId() + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "\"><img src=\"" + imageUrl + "\" border=\"0\" style=\"width: 100px;\"><br>";
				     pageTemplateHTML += contentVO.getName() + "</a>";
				     pageTemplateHTML += "</td>";	

				     index++;
				     if(index >= 5)
				     {
				    	 index = 0;
				    	 pageTemplateHTML += "</tr><tr>";
				     }
				     
				     foundPageTemplate = true;
			     }
			 }
			 pageTemplateHTML += "</tr></table>";

		 }
		 catch(Exception e)
		 {
		     logger.warn("A problem arouse when getting the page templates:" + e.getMessage(), e);
		 }
		 
		 this.getTemplateController().getDeliveryContext().setContentType("text/html");
		 this.getTemplateController().getDeliveryContext().setDisablePageCache(true);
		 return "<html><body style=\"font-family:verdana, sans-serif; font-size:10px;\">The page has no base component assigned yet. Click <a href=\"" + url + "\">here</a> to assign one" + (foundPageTemplate ? pageTemplateHTML : "") + "</body></html>";
	 }


	/**
	 * This method adds the neccessairy html to a template to make it right-clickable.
	 */	

	private String decorateTemplate(TemplateController templateController, String template, DeliveryContext deliveryContext, InfoGlueComponent component)
	{
		Timer timer = new Timer();
		timer.setActive(false);
 
		String decoratedTemplate = template;
		
		try
		{
			String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

			InfoGluePrincipal principal = templateController.getPrincipal();
		    String cmsUserName = (String)templateController.getHttpServletRequest().getSession().getAttribute("cmsUserName");
		    if(cmsUserName != null)
			    principal = templateController.getPrincipal(cmsUserName);

		    boolean hasAccessToAccessRights = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.ChangeSlotAccess", "");
			boolean hasAccessToAddComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.AddComponent", "" + component.getContentId() + "_" + component.getSlotName());
			boolean hasAccessToDeleteComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.DeleteComponent", "" + component.getContentId() + "_" + component.getSlotName());
			boolean hasAccessToChangeComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.ChangeComponent", "" + component.getContentId() + "_" + component.getSlotName());
			boolean hasSaveTemplateAccess = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "StructureTool.SaveTemplate", "");

		    String extraHeader 	= FileHelper.getFileAsString(new File(CmsPropertyHandler.getContextRootPath() + "preview/ajax/pageComponentEditorHeader.vm"));
		    //String extraBody 	= FileHelper.getFileAsString(new File(CmsPropertyHandler.getContextRootPath() + "preview/ajax/pageComponentEditorBody.vm"));
		    String extraBody 	= "";
			
		    String changeUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&slotId=base&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple();
		    extraBody = extraBody + "<script type=\"text/javascript\">initializeComponentEventHandler('base0_" + component.getId() + "Comp', '" + component.getId() + "', '', '" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&slotId=base&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "','" + changeUrl + "', " + templateController.getSiteNode().getRepositoryId() + ", " + templateController.getSiteNodeId() + ", " + templateController.getLanguageId() + ", " + templateController.getContentId() + ", " + component.getId() + ", " + component.getContentId() + ");</script>";

		    //Locale locale = templateController.getLocale();
		    Locale locale = templateController.getLocaleAvailableInTool();
		    
			String submitToPublishHTML = getLocalizedString(locale, "deliver.editOnSight.submitToPublish");
		    String addComponentHTML = getLocalizedString(locale, "deliver.editOnSight.addComponentHTML");
			String deleteComponentHTML = getLocalizedString(locale, "deliver.editOnSight.deleteComponentHTML");
			String changeComponentHTML = getLocalizedString(locale, "deliver.editOnSight.changeComponentHTML");
			String accessRightsHTML = getLocalizedString(locale, "deliver.editOnSight.accessRightsHTML");
			String pageComponentsHTML = getLocalizedString(locale, "deliver.editOnSight.pageComponentsHTML");
			String viewSourceHTML = getLocalizedString(locale, "deliver.editOnSight.viewSourceHTML");
			String componentEditorInNewWindowHTML = getLocalizedString(locale, "deliver.editOnSight.componentEditorInNewWindowHTML");
			String savePageTemplateHTML = getLocalizedString(locale, "deliver.editOnSight.savePageTemplateHTML");
			String savePagePartTemplateHTML = getLocalizedString(locale, "deliver.editOnSight.savePagePartTemplateHTML");

			String saveTemplateUrl = "saveComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + templateController.getSiteNode(deliveryContext.getSiteNodeId()).getMetaInfoContentId() + "');";
			String savePartTemplateUrl = "savePartComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + templateController.getSiteNode(deliveryContext.getSiteNodeId()).getMetaInfoContentId() + "');";
			if(!hasSaveTemplateAccess)
			{
				saveTemplateUrl = "alert('Not authorized to save template');";
				savePartTemplateUrl = "alert('Not authorized to save part template');";
			}
			
			extraBody = extraBody.replaceAll("\\$siteNodeId", "" + templateController.getSiteNodeId());
			extraBody = extraBody.replaceAll("\\$repositoryId", "" + templateController.getSiteNode().getRepositoryId());
			extraBody = extraBody.replaceAll("\\$originalFullURL", URLEncoder.encode(templateController.getOriginalFullURL(), "UTF-8"));
			
			extraBody = extraBody.replaceAll("\\$submitToPublishHTML", submitToPublishHTML);
			extraBody = extraBody.replaceAll("\\$addComponentHTML", addComponentHTML);
			extraBody = extraBody.replaceAll("\\$deleteComponentHTML", deleteComponentHTML);
			extraBody = extraBody.replaceAll("\\$changeComponentHTML", changeComponentHTML);
		    extraBody = extraBody.replaceAll("\\$accessRightsHTML", accessRightsHTML);
		    
		    extraBody = extraBody.replaceAll("\\$pageComponents", pageComponentsHTML);
		    extraBody = extraBody.replaceAll("\\$componentEditorInNewWindowHTML", componentEditorInNewWindowHTML);
		    extraBody = extraBody.replaceAll("\\$savePageTemplateHTML", savePageTemplateHTML);
		    extraBody = extraBody.replaceAll("\\$savePagePartTemplateHTML", savePagePartTemplateHTML);
		    extraBody = extraBody.replaceAll("\\$saveTemplateUrl", saveTemplateUrl);
		    extraBody = extraBody.replaceAll("\\$savePartTemplateUrl", savePartTemplateUrl);
		    extraBody = extraBody.replaceAll("\\$viewSource", viewSourceHTML);
		    
		    extraBody = extraBody.replaceAll("\\$addComponentJavascript", "var hasAccessToAddComponent" + component.getId() + "_" + component.getSlotName() + " = " + hasAccessToAddComponent + ";");
		    extraBody = extraBody.replaceAll("\\$deleteComponentJavascript", "var hasAccessToDeleteComponent" + component.getSlotName() + " = " + hasAccessToDeleteComponent + ";");
		    extraBody = extraBody.replaceAll("\\$changeComponentJavascript", "var hasAccessToChangeComponent" + component.getSlotName() + " = " + hasAccessToChangeComponent + ";");
		    extraBody = extraBody.replaceAll("\\$changeAccessJavascript", "var hasAccessToAccessRights" + component.getSlotName() + " = " + hasAccessToAccessRights + ";");
		    		    
		    //List tasks = getTasks();
			//component.setTasks(tasks);
			
			//String tasks = templateController.getContentAttribute(component.getContentId(), "ComponentTasks", true);
			
			/*
			Map context = new HashMap();
			context.put("templateLogic", templateController);
			StringWriter cacheString = new StringWriter();
			PrintWriter cachedStream = new PrintWriter(cacheString);
			new VelocityTemplateProcessor().renderTemplate(context, cachedStream, extraBody);
			extraBody = cacheString.toString();
			*/
			
			//extraHeader.replaceAll()
			
			timer.printElapsedTime("Read files");
			
			StringBuffer modifiedTemplate = new StringBuffer(template);
			
			//Adding stuff in the header
			int indexOfHeadEndTag = modifiedTemplate.indexOf("</head");
			if(indexOfHeadEndTag == -1)
				indexOfHeadEndTag = modifiedTemplate.indexOf("</HEAD");
			
			if(indexOfHeadEndTag > -1)
			{
				modifiedTemplate = modifiedTemplate.replace(indexOfHeadEndTag, modifiedTemplate.indexOf(">", indexOfHeadEndTag) + 1, extraHeader);
			}
			else
			{
				int indexOfHTMLStartTag = modifiedTemplate.indexOf("<html");
				if(indexOfHTMLStartTag == -1)
					indexOfHTMLStartTag = modifiedTemplate.indexOf("<HTML");
		
				if(indexOfHTMLStartTag > -1)
				{
					modifiedTemplate = modifiedTemplate.insert(modifiedTemplate.indexOf(">", indexOfHTMLStartTag) + 1, "<head>" + extraHeader);
				}
				else
				{
					logger.info("The current template is not a valid document. It does not comply with the simplest standards such as having a correct header.");
				}
			}

			timer.printElapsedTime("Header handled");

			//Adding stuff in the body	
			int indexOfBodyStartTag = modifiedTemplate.indexOf("<body");
			if(indexOfBodyStartTag == -1)
				indexOfBodyStartTag = modifiedTemplate.indexOf("<BODY");
			
			if(indexOfBodyStartTag > -1)
			{
			    //String pageComponentStructureDiv = "";
				//String pageComponentStructureDiv = getPageComponentStructureDiv(templateController, deliveryContext.getSiteNodeId(), deliveryContext.getLanguageId(), component);
				timer.printElapsedTime("pageComponentStructureDiv");
				String componentPaletteDiv = getComponentPaletteDiv(deliveryContext.getSiteNodeId(), deliveryContext.getLanguageId(), templateController);
				//String componentPaletteDiv = "";
				timer.printElapsedTime("componentPaletteDiv");
				modifiedTemplate = modifiedTemplate.insert(modifiedTemplate.indexOf(">", indexOfBodyStartTag) + 1, extraBody + componentPaletteDiv);
				//modifiedTemplate = modifiedTemplate.insert(modifiedTemplate.indexOf(">", indexOfBodyStartTag) + 1, extraBody + componentPaletteDiv);
			}
			else
			{
				logger.info("The current template is not a valid document. It does not comply with the simplest standards such as having a correct body.");
			}
			
			timer.printElapsedTime("Body handled");

			decoratedTemplate = modifiedTemplate.toString();
		}
		catch(Exception e)
		{
			logger.warn("An error occurred when deliver tried to decorate your template to enable onSiteEditing. Reason " + e.getMessage(), e);
		}
		
		return decoratedTemplate;
	}

   
	private String decorateComponent(InfoGlueComponent component, TemplateController templateController, Integer repositoryId, Integer siteNodeId, Integer languageId, Integer contentId/*, Integer metainfoContentId*/) throws Exception
	{
		String decoratedComponent = "";
		
		//logger.info("decorateComponent.contentId:" + contentId);

		//logger.info("decorateComponent:" + component.getName());
		
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

		Timer timer = new Timer();
		timer.setActive(false);

		try
		{
			String componentString = getComponentString(templateController, component.getContentId(), component); 

			if(component.getParentComponent() == null && templateController.getDeliveryContext().getShowSimple())
			{
			    templateController.getDeliveryContext().setContentType("text/html");
			    templateController.getDeliveryContext().setDisablePageCache(true);
			    componentString = "<html><head></head><body onload=\"toggleDiv('pageComponents');\">" + componentString + "</body></html>";
			}
			
			templateController.setComponentLogic(new DecoratedComponentLogic(templateController, component));
			Map context = super.getDefaultContext();
			context.put("templateLogic", templateController);
			StringWriter cacheString = new StringWriter();
			PrintWriter cachedStream = new PrintWriter(cacheString);
			new VelocityTemplateProcessor().renderTemplate(context, cachedStream, componentString, false, component);
			componentString = cacheString.toString();
	
			int bodyIndex = componentString.indexOf("<body");
			if(bodyIndex == -1)
				bodyIndex = componentString.indexOf("<BODY");
		
			if(component.getParentComponent() == null && bodyIndex > -1)
			{
				String onContextMenu = " id=\"base0_0Comp\" onload=\"javascript:setToolbarInitialPosition();\"";
				if(templateController.getDeliveryContext().getShowSimple())
					onContextMenu = " id=\"base0_0Comp\" onload=\"javascript:setToolbarInitialPosition();\"";
				
				
				StringBuffer sb = new StringBuffer(componentString);
				sb.insert(bodyIndex + 5, onContextMenu);
				componentString = sb.toString();

				Document componentPropertiesDocument = getComponentPropertiesDOM4JDocument(templateController, siteNodeId, languageId, component.getContentId()); 
				//this.propertiesDivs += getComponentPropertiesDiv(templateController, repositoryId, siteNodeId, languageId, contentId, component.getId(), component.getContentId(), componentPropertiesDocument, component);

				Document componentTasksDocument = getComponentTasksDOM4JDocument(templateController, siteNodeId, languageId, component.getContentId()); 
				//this.tasksDivs += getComponentTasksDiv(repositoryId, siteNodeId, languageId, contentId, component, 0, 1, componentTasksDocument, templateController);
			}
	
			int offset = 0;
			int slotStartIndex = componentString.indexOf("<ig:slot", offset);
			//logger.info("slotStartIndex:" + slotStartIndex);
			while(slotStartIndex > -1)
			{
				decoratedComponent += componentString.substring(offset, slotStartIndex);
				int slotStopIndex = componentString.indexOf("</ig:slot>", slotStartIndex);
				
				String slot = componentString.substring(slotStartIndex, slotStopIndex + 10);
				String id = slot.substring(slot.indexOf("id") + 4, slot.indexOf("\"", slot.indexOf("id") + 4));
				
				Slot slotBean = new Slot();
			    slotBean.setId(id);

			    String[] allowedComponentNamesArray = null;
			    int allowedComponentNamesIndex = slot.indexOf(" allowedComponentNames");
				if(allowedComponentNamesIndex > -1)
				{    
				    String allowedComponentNames = slot.substring(allowedComponentNamesIndex + 24, slot.indexOf("\"", allowedComponentNamesIndex + 24));
				    allowedComponentNamesArray = allowedComponentNames.split(",");
				    //System.out.println("allowedComponentNamesArray:" + allowedComponentNamesArray.length);
				    slotBean.setAllowedComponentsArray(allowedComponentNamesArray);
				}

				String[] disallowedComponentNamesArray = null;
				int disallowedComponentNamesIndex = slot.indexOf(" disallowedComponentNames");
				if(disallowedComponentNamesIndex > -1)
				{    
				    String disallowedComponentNames = slot.substring(disallowedComponentNamesIndex + 27, slot.indexOf("\"", disallowedComponentNamesIndex + 27));
				    disallowedComponentNamesArray = disallowedComponentNames.split(",");
				    //System.out.println("disallowedComponentNamesArray:" + disallowedComponentNamesArray.length);
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
				component.setContainerSlot(slotBean);
				
				String subComponentString = "";
				
				if(component.getIsInherited())
				    subComponentString += "<div id=\"" + component.getId() + "_" + id + "\" class=\"inheritedComponentDiv\");\">";
				else
				{
				    subComponentString += "<div id=\"" + component.getId() + "_" + id + "\" class=\"componentDiv\">";
				    
				    subComponentString += "\n<script type=\"text/javascript\">\n";
				    subComponentString += "    $(document).ready(function() {";
				    subComponentString += "      var element = document.getElementById(\"" + component.getId() + "_" + id + "\");\n";
				    subComponentString += "	     $('.dropArea').droppable({";
				    subComponentString += "		   accept: '.availableComponentsDiv .dragable',";
				    subComponentString += "		   activeClass: 'droppable-active',";
				    subComponentString += "		   hoverClass: 'droppable-hover'";
				    subComponentString += "	   });";

				    subComponentString += "    if (document.addEventListener != null)\n";
				    subComponentString += "		 element.addEventListener('mouseup', function (event){assignComponent(event, '" + siteNodeId + "', '" + languageId + "', '" + contentId + "', '" + component.getId() + "', '" + id + "', '" + false + "', '" + slotBean.getAllowedComponentsArrayAsUrlEncodedString() + "', '" + slotBean.getDisallowedComponentsArrayAsUrlEncodedString() + "');}, false);\n";
				    subComponentString += "    else{\n";
				    subComponentString += "		 element.attachEvent(\"onmouseup\", function (evt){\n";
				    subComponentString += "			assignComponent(evt, '" + siteNodeId + "', '" + languageId + "', '" + contentId + "', '" + component.getId() + "', '" + id + "', '" + false + "', '" + slotBean.getAllowedComponentsArrayAsUrlEncodedString() + "', '" + slotBean.getDisallowedComponentsArrayAsUrlEncodedString() + "');\n";
				    subComponentString += "		 });";
				    subComponentString += "    }\n";
				    subComponentString += " });\n";
				    subComponentString += "</script>\n";
				}
				
				List subComponents = getInheritedComponents(getDatabase(), templateController, component, templateController.getSiteNodeId(), id, inherit);

			    InfoGluePrincipal principal = templateController.getPrincipal();
			    String cmsUserName = (String)templateController.getHttpServletRequest().getSession().getAttribute("cmsUserName");
			    if(cmsUserName != null)
				    principal = templateController.getPrincipal(cmsUserName);

				String clickToAddHTML = "";
				boolean hasAccessToAddComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.AddComponent", "" + component.getContentId() + "_" + id);
				if(slotBean.getDisableAccessControl())
					hasAccessToAddComponent = true;
				
			    boolean hasMaxComponents = false;
				if(component.getSlotList() != null)
				{
					Iterator slotListIterator = component.getSlotList().iterator();
					while(slotListIterator.hasNext())
					{
						Slot parentSlot = (Slot)slotListIterator.next();
						if(parentSlot.getId().equalsIgnoreCase(id))
						{
							if(parentSlot.getAllowedNumberOfComponents() != -1 && parentSlot.getComponents().size() >= parentSlot.getAllowedNumberOfComponents())
								hasMaxComponents = true;
						}
					}
				}
				
				if(hasAccessToAddComponent && !hasMaxComponents)
				{
					if(slotBean.getAddComponentText() != null)
					{
						clickToAddHTML = slotBean.getAddComponentText();
					}
					else
					{
						Locale locale = templateController.getLocaleAvailableInTool();
						clickToAddHTML = getLocalizedString(locale, "deliver.editOnSight.slotInstructionHTML");
					}
				}
				
				//logger.info("subComponents for " + id + ":" + subComponents);
				if(subComponents != null && subComponents.size() > 0)
				{
					//logger.info("SUBCOMPONENTS:" + subComponents.size());
					int index = 0;
					Iterator subComponentsIterator = subComponents.iterator();
					while(subComponentsIterator.hasNext())
					{
						InfoGlueComponent subComponent = (InfoGlueComponent)subComponentsIterator.next();
						if(subComponent != null)
						{
							component.getComponents().put(subComponent.getSlotName(), subComponent);
							if(subComponent.getIsInherited() && subComponent.getPagePartTemplateComponent() == null)
							{
								//logger.info("Inherited..." + contentId);
								String childComponentsString = decorateComponent(subComponent, templateController, repositoryId, siteNodeId, languageId, contentId/*, metainfoContentId*/);
								if(!this.getTemplateController().getDeliveryContext().getShowSimple())
								    subComponentString += "<span id=\""+ id + index + "Comp\" class=\"inheritedslot\">" + childComponentsString + "</span>";
								else
								    subComponentString += childComponentsString;
								    
								Document componentPropertiesDocument = getComponentPropertiesDOM4JDocument(templateController, siteNodeId, languageId, component.getContentId()); 
								//this.propertiesDivs += getComponentPropertiesDiv(templateController, repositoryId, siteNodeId, languageId, contentId, new Integer(siteNodeId.intValue()*100 + subComponent.getId().intValue()), subComponent.getContentId(), componentPropertiesDocument, subComponent);
								
								Document componentTasksDocument = getComponentTasksDOM4JDocument(templateController, siteNodeId, languageId, subComponent.getContentId()); 
								//this.tasksDivs += getComponentTasksDiv(repositoryId, siteNodeId, languageId, contentId, subComponent, index, subComponents.size() - 1, componentTasksDocument, templateController);
							}
							else
							{
								//logger.info("Not inherited..." + contentId);
								String childComponentsString = decorateComponent(subComponent, templateController, repositoryId, siteNodeId, languageId, contentId/*, metainfoContentId*/);
								//logger.info("childComponentsString:" + childComponentsString);
								
								if(!this.getTemplateController().getDeliveryContext().getShowSimple())
								{    
								    String allowedComponentNamesAsEncodedString = null;
								    String disallowedComponentNamesAsEncodedString = null;

								    for(int i=0; i < subComponent.getParentComponent().getSlotList().size(); i++)
								    {
								        Slot subSlotBean = (Slot)subComponent.getParentComponent().getSlotList().get(i);
								        
								        if(subSlotBean.getId() != null && subSlotBean.getId().equals(subComponent.getSlotName()))
								        {
								            allowedComponentNamesAsEncodedString = subSlotBean.getAllowedComponentsArrayAsUrlEncodedString();
								            disallowedComponentNamesAsEncodedString = subSlotBean.getDisallowedComponentsArrayAsUrlEncodedString();
								            subComponent.setContainerSlot(subSlotBean);
								        }
								    }

								    if(subComponent.getPagePartTemplateComponent() != null)
								    {
									    String changeUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + subComponent.getPagePartTemplateComponent().getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple();
									    subComponentString += "<span id=\""+ id + index + "_" + subComponent.getPagePartTemplateComponent().getId() + "Comp\">" + childComponentsString + "<script type=\"text/javascript\">initializeComponentEventHandler('" + id + index + "_" + subComponent.getPagePartTemplateComponent().getId() + "Comp', '" + subComponent.getPagePartTemplateComponent().getId() + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?aa=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + subComponent.getPagePartTemplateComponent().getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "&AAAA=1")  + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "&AAAA=1") + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + subComponent.getPagePartTemplateComponent().getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "','" + changeUrl + "', " + templateController.getSiteNode().getRepositoryId() + ", " + templateController.getSiteNodeId() + ", " + templateController.getLanguageId() + ", " + templateController.getContentId() + ", " + subComponent.getPagePartTemplateComponent().getId() + ", " + subComponent.getPagePartTemplateComponent().getContentId() + ");</script></span>";
								    }
								    else
								    {
									    String changeUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + subComponent.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple();
									    subComponentString += "<span id=\""+ id + index + "_" + subComponent.getId() + "Comp\">" + childComponentsString + "<script type=\"text/javascript\">initializeComponentEventHandler('" + id + index + "_" + subComponent.getId() + "Comp', '" + subComponent.getId() + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?aa=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + component.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "&AAAA=1")  + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "&AAAA=1") + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&componentId=" + subComponent.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "','" + changeUrl + "', " + templateController.getSiteNode().getRepositoryId() + ", " + templateController.getSiteNodeId() + ", " + templateController.getLanguageId() + ", " + templateController.getContentId() + ", " + subComponent.getId() + ", " + subComponent.getContentId() + ");</script></span>";
								    }
								}
								else
								{
								    subComponentString += childComponentsString;
								}
								
								if(subComponent.getPagePartTemplateComponent() != null)
							    {
									Document componentPropertiesDocument = getComponentPropertiesDOM4JDocument(templateController, siteNodeId, languageId, subComponent.getPagePartTemplateComponent().getContentId()); 
									//this.propertiesDivs += getComponentPropertiesDiv(templateController, repositoryId, siteNodeId, languageId, contentId, subComponent.getPagePartTemplateComponent().getId(), subComponent.getPagePartTemplateComponent().getContentId(), componentPropertiesDocument, subComponent.getPagePartTemplateComponent());
									Document componentTasksDocument = getComponentTasksDOM4JDocument(templateController, siteNodeId, languageId, subComponent.getPagePartTemplateComponent().getContentId()); 
									
									//this.tasksDivs += getComponentTasksDiv(repositoryId, siteNodeId, languageId, contentId, subComponent.getPagePartTemplateComponent(), index, subComponents.size() - 1, componentTasksDocument, templateController);
							    }
							    else
							    {
									Document componentPropertiesDocument = getComponentPropertiesDOM4JDocument(templateController, siteNodeId, languageId, subComponent.getContentId()); 
									//this.propertiesDivs += getComponentPropertiesDiv(templateController, repositoryId, siteNodeId, languageId, contentId, subComponent.getId(), subComponent.getContentId(), componentPropertiesDocument, subComponent);
									Document componentTasksDocument = getComponentTasksDOM4JDocument(templateController, siteNodeId, languageId, subComponent.getContentId()); 

									//this.tasksDivs += getComponentTasksDiv(repositoryId, siteNodeId, languageId, contentId, subComponent, index, subComponents.size() - 1, componentTasksDocument, templateController);
							    }
							}
						}
						index++;
					}
					
					if(component.getContainerSlot().getAddComponentLinkHTML() != null && !component.getIsInherited())
					{
					    String allowedComponentNamesAsEncodedString = null;
					    String disallowedComponentNamesAsEncodedString = null;
					    
					    for(int i=0; i < component.getSlotList().size(); i++)
					    {
					        Slot subSlotBean = (Slot)component.getSlotList().get(i);
					        if(subSlotBean.getId() != null && subSlotBean.getId().equals(id))
					        {
					            allowedComponentNamesAsEncodedString = subSlotBean.getAllowedComponentsArrayAsUrlEncodedString();
					            disallowedComponentNamesAsEncodedString = subSlotBean.getDisallowedComponentsArrayAsUrlEncodedString();
					        }
					    }

						String linkUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?BBB=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + component.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "&BBBB=1") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "&BBBB=1");
						subComponentString += "" + component.getContainerSlot().getAddComponentLinkHTML().replaceAll("\\$linkUrl", linkUrl);
					}
					else if(!component.getIsInherited())
					{
						subComponentString += "" + clickToAddHTML;
					}
				}
				else
				{
					if(component.getContainerSlot().getAddComponentLinkHTML() != null && !component.getIsInherited())
					{
					    String allowedComponentNamesAsEncodedString = null;
					    String disallowedComponentNamesAsEncodedString = null;
					    
					    for(int i=0; i < component.getSlotList().size(); i++)
					    {
					        Slot subSlotBean = (Slot)component.getSlotList().get(i);
					        if(subSlotBean.getId() != null && subSlotBean.getId().equals(id))
					        {
					            allowedComponentNamesAsEncodedString = subSlotBean.getAllowedComponentsArrayAsUrlEncodedString();
					            disallowedComponentNamesAsEncodedString = subSlotBean.getDisallowedComponentsArrayAsUrlEncodedString();
					        }
					    }

						String linkUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?BBB=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + component.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "&BBBB=1") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "&BBBB=1");
						subComponentString += "" + component.getContainerSlot().getAddComponentLinkHTML().replaceAll("\\$linkUrl", linkUrl);
					}
					else if(!component.getIsInherited())
					{
						subComponentString += "" + clickToAddHTML;
					}
				}
				
				if(!component.getIsInherited())
				{
				    String allowedComponentNamesAsEncodedString = null;
				    String disallowedComponentNamesAsEncodedString = null;
				    
				    for(int i=0; i < component.getSlotList().size(); i++)
				    {
				        Slot subSlotBean = (Slot)component.getSlotList().get(i);
				        if(subSlotBean.getId() != null && subSlotBean.getId().equals(id))
				        {
				            allowedComponentNamesAsEncodedString = subSlotBean.getAllowedComponentsArrayAsUrlEncodedString();
				            disallowedComponentNamesAsEncodedString = subSlotBean.getDisallowedComponentsArrayAsUrlEncodedString();
				        }
				    }

				    subComponentString += "<script type=\"text/javascript\">initializeSlotEventHandler('" + component.getId() + "_" + id + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?BBB=1&siteNodeId=" + siteNodeId + "&languageId=" + languageId + "&contentId=" + contentId + "&parentComponentId=" + component.getId() + "&slotId=" + id + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "&BBBB=1") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "&BBBB=1") + "', '', '', '" + id + "', '" + component.getContentId() + "');</script></div>";
				}
				else
				    subComponentString += "</div>";
				  							    
				decoratedComponent += subComponentString;
							
				offset = slotStopIndex + 10;
				slotStartIndex = componentString.indexOf("<ig:slot", offset);
			}
			
			//logger.info("offset:" + offset);
			decoratedComponent += componentString.substring(offset);
		}
		catch(Exception e)
		{		
			logger.warn("An component with either an empty template or with no template in the sitelanguages was found:" + e.getMessage(), e);	
		}
		
		return decoratedComponent;
	}


	
	/**
	 * This method creates a div for the components properties.
	 */
	/*
	private String getComponentTasksDiv(Integer repositoryId, Integer siteNodeId, Integer languageId, Integer contentId, InfoGlueComponent component, int position, int maxPosition, Document document, TemplateController templateController) throws Exception
	{		
	    InfoGluePrincipal principal = templateController.getPrincipal();
	    String cmsUserName = (String)templateController.getHttpServletRequest().getSession().getAttribute("cmsUserName");
	    if(cmsUserName != null)
		    principal = templateController.getPrincipal(cmsUserName);
	    
		boolean hasAccessToAccessRights = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.ChangeSlotAccess", "");
		boolean hasAccessToAddComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.AddComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
		boolean hasAccessToDeleteComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.DeleteComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
		boolean hasAccessToChangeComponent = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "ComponentEditor.ChangeComponent", "" + (component.getParentComponent() == null ? component.getContentId() : component.getParentComponent().getContentId()) + "_" + component.getSlotName());
	    boolean hasSaveTemplateAccess = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "StructureTool.SaveTemplate", "");
	    
	    boolean hasMaxComponents = false;
		if(component.getParentComponent() != null && component.getParentComponent().getSlotList() != null)
		{
			Iterator slotListIterator = component.getParentComponent().getSlotList().iterator();
			while(slotListIterator.hasNext())
			{
				Slot slot = (Slot)slotListIterator.next();
				if(slot.getId().equalsIgnoreCase(component.getSlotName()))
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
		    StringBuffer sb = new StringBuffer();
		    sb.append("<script type=\"text/javascript\">");
			//sb.append("<!--");
		    sb.append("hasAccessToAddComponent" + component.getId() + "_" + component.getSlotName() + " = " + hasAccessToAddComponent + ";");
			sb.append("hasAccessToDeleteComponent" + component.getSlotName() + " = " + hasAccessToDeleteComponent + ";");
			sb.append("hasAccessToChangeComponent" + component.getSlotName() + " = " + hasAccessToChangeComponent + ";");
			sb.append("hasAccessToAccessRights" + component.getSlotName() + " = " + hasAccessToAccessRights + ";");
		    //sb.append("-->");
			sb.append("</script>");
			return sb.toString();
		}
	    
	    StringBuffer sb = new StringBuffer();
		Timer timer = new Timer();
		timer.setActive(false);

		String componentEditorUrl = "" + CmsPropertyHandler.getComponentEditorUrl();
		String originalFullURL = "" + templateController.getOriginalFullURL();
		String componentRendererUrl = "" + CmsPropertyHandler.getComponentRendererUrl();

		//if(component.getIsInherited())
		//    sb.append("<div id=\"inheritedComponent" + component.getId() + "Menu\" class=\"skin0\">");
		//else 
		//if(component.getPagePartTemplateComponent() != null)
		//	sb.append("<div id=\"component" + component.getPagePartTemplateComponent().getId() + "Menu\" class=\"skin0\">");
		//else
			sb.append("<div id=\"component" + component.getId() + "Menu\" class=\"skin0\">");
			
		Collection componentTasks = getComponentTasks(component.getId(), document);

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
		    view = view.replaceAll("\\$componentId", component.getId().toString());
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"executeTask('" + view + "', " + openInPopup + ");\">" + componentTask.getName() + "</div>");
		}

		//Locale locale = templateController.getLocale();
		Locale locale = templateController.getLocaleAvailableInTool();
		
		String editHTML = getLocalizedString(locale, "deliver.editOnSight.editHTML");
		String submitToPublishHTML = getLocalizedString(locale, "deliver.editOnSight.submitToPublish");
		String addComponentHTML = getLocalizedString(locale, "deliver.editOnSight.addComponentHTML");
		String deleteComponentHTML = getLocalizedString(locale, "deliver.editOnSight.deleteComponentHTML");
		String changeComponentHTML = getLocalizedString(locale, "deliver.editOnSight.changeComponentHTML");
		String moveComponentUpHTML = getLocalizedString(locale, "deliver.editOnSight.moveComponentUpHTML");
		String moveComponentDownHTML = getLocalizedString(locale, "deliver.editOnSight.moveComponentDownHTML");
		String propertiesHTML = getLocalizedString(locale, "deliver.editOnSight.propertiesHTML");
		String pageComponentsHTML = getLocalizedString(locale, "deliver.editOnSight.pageComponentsHTML");
		String viewSourceHTML = getLocalizedString(locale, "deliver.editOnSight.viewSourceHTML");
		String componentEditorInNewWindowHTML = getLocalizedString(locale, "deliver.editOnSight.componentEditorInNewWindowHTML");
		String savePageTemplateHTML = getLocalizedString(locale, "deliver.editOnSight.savePageTemplateHTML");
		String savePagePartTemplateHTML = getLocalizedString(locale, "deliver.editOnSight.savePagePartTemplateHTML");

		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"edit();\">" + editHTML + "</div>");
		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"submitToPublish(" + siteNodeId + ", " + repositoryId + ", '" + URLEncoder.encode(templateController.getOriginalFullURL(), "UTF-8") + "');\">" + submitToPublishHTML + "</div>");
		if(hasAccessToAddComponent)
			sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"insertComponent();\">" + addComponentHTML + "</div>");
		if(hasAccessToDeleteComponent)
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"deleteComponent();\">" + deleteComponentHTML + "</div>");
		if(hasAccessToChangeComponent)
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"changeComponent();\">" + changeComponentHTML + "</div>");
		if(hasSaveTemplateAccess)
		{
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"saveComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + templateController.getSiteNode(siteNodeId).getMetaInfoContentId() + "');\">" + savePageTemplateHTML + "</div>");
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"saveComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + templateController.getSiteNode(siteNodeId).getMetaInfoContentId() + "&componentId=" + component.getId() + "');\">" + savePagePartTemplateHTML + "</div>");
		}
		
		String upUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&direction=0&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "";
		String downUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&direction=1&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "";
				
		if(position > 0)
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"invokeAddress('" + upUrl + "');\">" + moveComponentUpHTML + "</div>");
		if(maxPosition > position)
		    sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"invokeAddress('" + downUrl + "');\">" + moveComponentDownHTML + "</div>");
		
		sb.append("<hr/>");
		
		//sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"javascript:showComponent();\">" + propertiesHTML + "</div>");
		String parameterString = "repositoryId=" + templateController.getSiteNode().getRepositoryId() + "&siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&componentContentId=" + component.getContentId() + "&slotName=" + component.getSlotName() + "&showSimple=false&showLegend=false";
		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"javascript:showComponentInDiv('componentProperties', '" + parameterString + "', false);\">" + propertiesHTML + "</div>");
		sb.append("<hr/>");

		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"javascript:toggleDiv('pageComponents');\">" + pageComponentsHTML + "</div>");
		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"window.open(document.location.href,'PageComponents','');\">" + componentEditorInNewWindowHTML + "</div>");
		sb.append("<div class=\"igmenuitems\" onMouseover=\"javascript:highlightie5(event);\" onMouseout=\"javascript:lowlightie5(event);\" onClick=\"javascript:viewSource();\">" + viewSourceHTML + "</div>");

		sb.append("</div>");
		
		sb.append("<script type=\"text/javascript\">");
		//sb.append("<!--");
		sb.append("hasAccessToAddComponent" + component.getId() + "_" + component.getSlotName() + " = " + hasAccessToAddComponent + ";");
		sb.append("hasAccessToDeleteComponent" + component.getSlotName() + " = " + hasAccessToDeleteComponent + ";");
		sb.append("hasAccessToChangeComponent" + component.getSlotName() + " = " + hasAccessToChangeComponent + ";");
		sb.append("hasAccessToAccessRights" + component.getSlotName() + " = " + hasAccessToAccessRights + ";");
		//sb.append("-->");
		sb.append("</script>");
		
		return sb.toString();
	}
	*/

	/**
	 * This method creates a div for the components properties.
	 */
	/*
	private String getPageComponentStructureDiv(TemplateController templateController, Integer siteNodeId, Integer languageId, InfoGlueComponent component) throws Exception
	{		
	    if(templateController.getRequestParameter("skipComponentStructure") != null && templateController.getRequestParameter("skipComponentStructure").equalsIgnoreCase("true"))
	        return "";
	    
		StringBuffer sb = new StringBuffer();
		
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

		sb.append("<div id=\"pageComponents\" style=\"right:5px; top:5px; visibility:hidden; display: none;\">");

		sb.append("	<div id=\"dragCorner\" style=\"position: absolute; width: 16px; height: 16px; background-color: white; bottom: 0px; right: 0px;\"><a href=\"javascript:expandWindow('pageComponents');\"><img src=\"" + this.getRequest().getContextPath() + "/images/enlarge.gif\" border=\"0\" width=\"16\" height=\"16\"></a></div>");
			
		sb.append("		<div id=\"pageComponentsHandle\"><div id=\"leftPaletteHandle\">Page components</div><div id=\"rightPaletteHandle\"><a href=\"javascript:hideDiv('pageComponents');\" class=\"white\">close</a></div></div>");
		sb.append("		<div id=\"pageComponentsBody\"><table class=\"igtable\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");

		sb.append("		<tr class=\"igtr\">");
	    sb.append("			<td class=\"igtd\" colspan=\"20\"><img src=\"" + this.getRequest().getContextPath() + "/images/tcross.png\" width=\"19\" height=\"16\"><span id=\"" + component.getSlotName() + component.getId() + "\" class=\"iglabel\"><img src=\"" + this.getRequest().getContextPath() + "/images/slotIcon.gif\" width=\"16\" height=\"16\"><img src=\"" + this.getRequest().getContextPath() + "/images/trans.gif\" width=\"5\" height=\"1\">" + component.getName() + "</span><script type=\"text/javascript\">initializeSlotEventHandler('" + component.getSlotName() + component.getId() + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?CCC=1&siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&parentComponentId=" + component.getId() + "&slotId=base&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "', '', '', 'base', '" + component.getContentId() + "');</script></td>");
		sb.append("		</tr>");
		
		renderComponentTree(templateController, sb, component, 0, 0, 1);

		sb.append("		<tr class=\"igtr\">");
		for(int i=0; i<20; i++)
		{
			sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/trans.gif\" width=\"19\" height=\"1\"></td>");
		}
		sb.append("		</tr>");
		sb.append("		</table>");
		sb.append("		</div>");
		sb.append("	</div>");
		
		sb.append("	<script type=\"text/javascript\">");
		sb.append("		var theHandle = document.getElementById(\"pageComponentsHandle\");");
		sb.append("		var theRoot   = document.getElementById(\"pageComponents\");");
		sb.append("		Drag.init(theHandle, theRoot);");
		sb.append("     theRoot.style.left = 160;");
		sb.append("     theRoot.style.top = 150;");
		sb.append("	</script>");

		return sb.toString();
	}
	*/
	/**
	 * This method renders the component tree visually
	 */
	/*
	private void renderComponentTree(TemplateController templateController, StringBuffer sb, InfoGlueComponent component, int level, int position, int maxPosition) throws Exception
	{
		String componentEditorUrl = CmsPropertyHandler.getComponentEditorUrl();

		ContentVO componentContentVO = templateController.getContent(component.getContentId());
		
		int colspan = 20 - level;
		
		sb.append("		<tr class=\"igtr\">");
		sb.append("			<td class=\"igtd\"><img src=\"" + this.getRequest().getContextPath() + "/images/trans.gif\" width=\"19\" height=\"16\"></td>");
		
		for(int i=0; i<level; i++)
		{
			sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
		}
			
		String changeUrl = componentEditorUrl + "ViewSiteNodePageComponents!listComponentsForChange.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&slotId=" + component.getId() + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple();
		sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/tcross.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\"><img src=\"" + this.getRequest().getContextPath() + "/images/componentIcon.gif\" width=\"16\" height=\"16\"></td><td class=\"igtd\" colspan=\"" + (colspan - 2) + "\"><span id=\"" + component.getId() + "\">" + componentContentVO.getName() + "</span><script type=\"text/javascript\">initializeComponentInTreeEventHandler('" + component.getId() + "', '" + component.getId() + "', '', '" + componentEditorUrl + "ViewSiteNodePageComponents!deleteComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&slotId=" + component.getId() + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "', '" + changeUrl + "', '" + component.getSlotName() + "', 'APA');</script>");
		String upUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&direction=0&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "";
		String downUrl = componentEditorUrl + "ViewSiteNodePageComponents!moveComponent.action?siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&componentId=" + component.getId() + "&direction=1&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + "";
		
		if(position > 0)
		    sb.append("<a href=\"" + upUrl + "\"><img src=\"" + this.getRequest().getContextPath() + "/images/upArrow.gif\" border=\"0\" width=\"11\" width=\"10\"></a>");
		if(maxPosition > position)
		    sb.append("<a href=\"" + downUrl + "\"><img src=\"" + this.getRequest().getContextPath() + "/images/downArrow.gif\" border=\"0\" width=\"11\" width=\"10\"></a>");
		
		sb.append("</td>");
		
		sb.append("		</tr>");
		
		//Properties
		Iterator slotIterator = component.getSlotList().iterator();
		while(slotIterator.hasNext())
		{
			Slot slot = (Slot)slotIterator.next();
	
			sb.append("		<tr class=\"igtr\">");
			sb.append("			<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/trans.gif\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
			for(int i=0; i<level; i++)
			{
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/vline.png\" width=\"19\" height=\"16\"></td>");
			}
			if(slot.getComponents().size() > 0)
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/tcross.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/slotIcon.gif\" width=\"16\" height=\"16\"></td>");
			else
				sb.append("<td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/endline.png\" width=\"19\" height=\"16\"></td><td class=\"igtd\" width=\"19\"><img src=\"" + this.getRequest().getContextPath() + "/images/slotIcon.gif\" width=\"16\" height=\"16\"></td>");

		    String allowedComponentNamesAsEncodedString = slot.getAllowedComponentsArrayAsUrlEncodedString();
		    String disallowedComponentNamesAsEncodedString = slot.getDisallowedComponentsArrayAsUrlEncodedString();
		    //System.out.println("allowedComponentNamesAsEncodedString:" + allowedComponentNamesAsEncodedString);
		    //System.out.println("disallowedComponentNamesAsEncodedString:" + disallowedComponentNamesAsEncodedString);
		    
		    sb.append("<td class=\"igtd\" colspan=\"" + (colspan - 4) + "\"><span id=\"" + component.getId() + "_" + slot.getId() + "\" class=\"iglabel\">" + slot.getId() + "</span><script type=\"text/javascript\">initializeSlotEventHandler('" + component.getId() + "_" + slot.getId() + "', '" + componentEditorUrl + "ViewSiteNodePageComponents!listComponents.action?ddd=1&siteNodeId=" + templateController.getSiteNodeId() + "&languageId=" + templateController.getLanguageId() + "&contentId=" + templateController.getContentId() + "&parentComponentId=" + component.getId() + "&slotId=" + slot.getId() + "&showSimple=" + this.getTemplateController().getDeliveryContext().getShowSimple() + ((allowedComponentNamesAsEncodedString != null) ? "&" + allowedComponentNamesAsEncodedString : "") + ((disallowedComponentNamesAsEncodedString != null) ? "&" + disallowedComponentNamesAsEncodedString : "") + "', '', '', '" + slot.getId() + "', '" + component.getContentId() + "');</script></td>");
			
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
					//ContentVO componentContent = templateController.getContent(slotComponent.getContentId()); 
					//String imageUrl = "" + this.getRequest().getContextPath() + "/images/componentIcon.gif";
					//String imageUrlTemp = getDigitalAssetUrl(componentContent.getId(), "thumbnail");
					//if(imageUrlTemp != null && imageUrlTemp.length() > 0)
					//	imageUrl = imageUrlTemp;
					
					if(slotComponent.getPagePartTemplateComponent() == null)
						renderComponentTree(templateController, sb, slotComponent, level + 2, newPosition, slotComponents.size() - 1);
					else
						renderComponentTree(templateController, sb, slotComponent.getPagePartTemplateComponent(), level + 2, newPosition, slotComponents.size() - 1);
					
					newPosition++;
				}	
			}
		}
	}
	*/

	/**
	 * This method creates the tabpanel for the component-palette.
	 */
	//private static String componentPaletteDiv = null;
	
	private String getComponentPaletteDiv(Integer siteNodeId, Integer languageId, TemplateController templateController) throws Exception
	{		
		InfoGluePrincipal principal = templateController.getPrincipal();
	    String cmsUserName = (String)templateController.getHttpServletRequest().getSession().getAttribute("cmsUserName");
	    if(cmsUserName != null)
		    principal = templateController.getPrincipal(cmsUserName);

		if(!templateController.getDeliveryContext().getShowSimple())
	    {
		    boolean hasAccess = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "StructureTool.Palette", false, true);
		    if(!hasAccess || templateController.getRequestParameter("skipToolbar") != null && templateController.getRequestParameter("skipToolbar").equalsIgnoreCase("true"))
		        return "";
	    }
	    
		ContentVO contentVO = templateController.getBoundContent(BasicTemplateController.META_INFO_BINDING_NAME);

		//Cache
		String key = "" + templateController.getPrincipal().getName();
		String componentPaletteDiv = (String)CacheController.getCachedObject("componentPaletteDivCache", key);
		if(componentPaletteDiv != null)
		{
			if(componentPaletteDiv != null && (templateController.getRequestParameter("refresh") == null || !templateController.getRequestParameter("refresh").equalsIgnoreCase("true")))
			{
				return componentPaletteDiv.replaceAll("CreatePageTemplate\\!input.action\\?contentId=.*?'", "CreatePageTemplate!input.action?contentId=" + contentVO.getContentId() + "'");
			}
		}
		//End Cache
		
		StringBuffer sb = new StringBuffer();
			
		String componentEditorUrl 		= CmsPropertyHandler.getComponentEditorUrl();
		String componentRendererUrl 	= CmsPropertyHandler.getComponentRendererUrl();
		String componentRendererAction 	= CmsPropertyHandler.getComponentRendererAction();
		
		
		sb.append("<div id=\"buffer\" style=\"top: 0px; left: 0px; z-index:200;\"><img src=\"" + this.getRequest().getContextPath() + "/images/componentDraggedIcon.gif\"></div>");
		
		Map componentGroups = getComponentGroups(getComponentContents(), templateController);
		
		sb.append("<div id=\"paletteDiv\">");
		 
		sb.append("<div id=\"paletteHandle\">");
		sb.append("	<div id=\"leftPaletteHandle\">Component palette</div><div id=\"rightPaletteHandle\"><a href=\"javascript:hideDiv('paletteDiv');\" class=\"white\">close</a></div>");
		sb.append("</div>");

		sb.append("<div id=\"paletteBody\">");
		sb.append("<table class=\"tabPanel\" cellpadding=\"0\" cellspacing=\"0\">");
		sb.append(" <tr class=\"igtr\">");
		
		Iterator groupIterator = componentGroups.keySet().iterator();
		int index = 0;
		String groupName = "";
		String initialGroupName = "";
		while(groupIterator.hasNext())
		{
			groupName = (String)groupIterator.next();
			
			if(index == 0)
			{	
				sb.append("  <td id=\"" + groupName + "Tab\" valign=\"top\" class=\"thistab\" onclick=\"javascript:changeTab('" + groupName + "');\" height=\"20\"><nobr>" + groupName + "</nobr></td>");
				initialGroupName = groupName;
			}
			else if(!groupIterator.hasNext())
				sb.append("  <td id=\"" + groupName + "Tab\" valign=\"top\" class=\"igtab\" style=\"border-right: solid thin black\" onclick=\"javascript:changeTab('" + groupName + "');\"><nobr>" + groupName + "</nobr></td>");
			else
				sb.append("  <td id=\"" + groupName + "Tab\" valign=\"top\" class=\"igtab\" onclick=\"javascript:changeTab('" + groupName + "');\"><nobr>" + groupName + "</nobr></td>");

			index++;
		}
		
	    boolean hasSaveTemplateAccess = AccessRightController.getController().getIsPrincipalAuthorized(templateController.getDatabase(), principal, "StructureTool.SaveTemplate", "");
	    
		sb.append("  <td class=\"igpalettetd\" width=\"90%\" style=\"text-align: right; border-right: solid thin gray; border-bottom: solid thin white\" align=\"right\">&nbsp;<a href=\"javascript:refreshComponents(document.location.href);\" class=\"white\"><img src=\"" + this.getRequest().getContextPath() + "/images/refresh.gif\" alt=\"Refresh palette\" border=\"0\"></a>&nbsp;<a href=\"javascript:moveDivDown('paletteDiv');\" class=\"white\"><img src=\"" + this.getRequest().getContextPath() + "/images/arrowDown.gif\" alt=\"Move down\" border=\"0\"></a>&nbsp;<a href=\"javascript:moveDivUp('paletteDiv');\" class=\"white\"><img src=\"" + this.getRequest().getContextPath() + "/images/arrowUp.gif\" alt=\"Move up\" border=\"0\"></a>&nbsp;<a href=\"javascript:toggleDiv('pageComponents');\" class=\"white\"><img src=\"" + this.getRequest().getContextPath() + "/images/pageStructure.gif\" alt=\"Toggle page structure\" border=\"0\"></a>&nbsp;");
		if(hasSaveTemplateAccess)
		    sb.append("<a href=\"javascript:saveComponentStructure('" + componentEditorUrl + "CreatePageTemplate!input.action?contentId=" + contentVO.getId() + "');\" class=\"white\"><img src=\"" + this.getRequest().getContextPath() + "/images/saveComponentStructure.gif\" alt=\"Save the page as a template page\" border=\"0\"></a>&nbsp;");
		
		sb.append("<a href=\"javascript:window.open(document.location.href, 'PageComponents', '');\"><img src=\"" + this.getRequest().getContextPath() + "/images/fullscreen.gif\" alt=\"Pop up in a large window\" border=\"0\"></a>&nbsp;</td>");
		
		sb.append(" </tr>");
		sb.append("</table>");
		sb.append("</div>");
		
		sb.append("<script type=\"text/javascript\">");
		sb.append("var currentGroup = \"" + initialGroupName + "\";");
		sb.append("</script>");
				
		String openGroupName = "";

		groupIterator = componentGroups.keySet().iterator();
		index = 0;
		while(groupIterator.hasNext())
		{
			groupName = (String)groupIterator.next();

			if(index == 0)
			{
				sb.append("<div id=\"" + groupName + "ComponentsBg\" class=\"componentsBackground\" style=\"zIndex:3; visibility: inherited;\">");
				openGroupName = groupName;
			}
			else
			    sb.append("<div id=\"" + groupName + "ComponentsBg\" class=\"componentsBackground\" style=\"zIndex:2; visibility: inherited;\">");	
			
			sb.append("<div id=\"" + groupName + "Components\" style=\"visibility:inherit; position:absolute; top:1px; left:5px; height:50px; \">");
			sb.append("	<table class=\"igtable\" style=\"width:100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
			sb.append("	<tr class=\"igtr\">");
			//sb.append("	<td width=\"100%\"><nobr>");
			
			String imageUrl = this.getRequest().getContextPath() + "/images/componentIcon.gif";
			List components = (List)componentGroups.get(groupName); //getComponentContents();
			Iterator componentIterator = components.iterator();
			int componentIndex = 0;
			while(componentIterator.hasNext())
			{
				ContentVO componentContentVO = (ContentVO)componentIterator.next();
	
				//String imageUrlTemp = getDigitalAssetUrl(componentContentVO.getId(), "thumbnail");
				//if(imageUrlTemp != null && imageUrlTemp.length() > 0)
				//	imageUrl = imageUrlTemp;
				sb.append("	<td class=\"igpalettetd\">");
				sb.append("		<div id=\"" + componentIndex + "\" style=\"display: block; visibility: inherited;\"><nobr><img src=\"" + imageUrl + "\" width=\"16\" height=\"16\" border=\"0\">");
				sb.append("		<span onMouseDown=\"grabIt(event);\" onmouseover=\"showDetails('" + componentContentVO.getName() + "');\" id=\""+ componentContentVO.getId() + "\" class=\"draggableItem\" nowrap=\"1\">" + ((componentContentVO.getName().length() > 22) ? componentContentVO.getName().substring(0, 17) : componentContentVO.getName()) + "...</span>");
				sb.append("     </nobr></div>"); 
				sb.append("	</td>");
				
				imageUrl = this.getRequest().getContextPath() + "/images/componentIcon.gif";
			}
			sb.append("  <td class=\"igpalettetd\" width=\"90%\">&nbsp;</td>");
			
			//sb.append("	</nobr></td>");
			sb.append("	</tr>");
			sb.append("	</table>");
			sb.append("</div>");
			
			sb.append("</div>");
			
			sb.append("<script type=\"text/javascript\"> if (bw.bw) tabInit('" + groupName + "Components'); </script>");

			
			index++;
		}
		
		sb.append("<div id=\"statusListBg\">");
		sb.append("<table class=\"igtable\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
		sb.append("<tr class=\"igtr\">");
		sb.append("	<td class=\"igpalettetd\" align=\"left\" width=\"15px\">&nbsp;<a href=\"#\" onclick=\"moveLeft(currentGroup)\" return false\" onfocus=\"if(this.blur)this.blur()\"><img src=\"" + this.getRequest().getContextPath() + "/images/arrowleft.gif\" alt=\"previous\" border=\"0\"></a></td>");
		sb.append("	<td class=\"igpalettetd\" align=\"left\" width=\"95%\"><span class=\"componentsStatusText\">Details: </span><span id=\"statusText\" class=\"componentsStatusText\">&nbsp;</span></td>");
		sb.append("	<td class=\"igpalettetd\" align=\"right\"><a href=\"#\" onclick=\"moveRight(currentGroup)\" return false\" onfocus=\"if(this.blur)this.blur()\"><img src=\"" + this.getRequest().getContextPath() + "/images/arrowright.gif\" alt=\"next\" border=\"0\"></a>&nbsp;</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</div>");

		sb.append("	<script type=\"text/javascript\">");
		sb.append("	  	changeTab('" + openGroupName + "');");
		
		sb.append("		var theHandle = document.getElementById(\"paletteHandle\");");
		sb.append("		var theRoot   = document.getElementById(\"paletteDiv\");");
		sb.append("		Drag.init(theHandle, theRoot, 0, 0, 0, 1000);");
		
		sb.append("	</script>");

		sb.append("</div>");
		
		//Caching the result
		componentPaletteDiv = sb.toString();
		CacheController.cacheObject("componentPaletteDivCache", key, componentPaletteDiv);				
		
		return componentPaletteDiv;
	}

	/**
	 * This method gets all component groups from the available components.
	 * This is dynamically so if one states a different group in the component the group is created.
	 */

	private Map getComponentGroups(List components, TemplateController templateController)
	{
		Map componentGroups = new HashMap();
		
		Iterator componentIterator = components.iterator();
		while(componentIterator.hasNext())
		{
			ContentVO componentContentVO = (ContentVO)componentIterator.next();
			String groupName = templateController.getContentAttribute(componentContentVO.getId(), "GroupName", true);
			if(groupName == null || groupName.equals(""))
				groupName = "Other";
			
			List groupComponents = (List)componentGroups.get(groupName);
			if(groupComponents == null)
			{
				groupComponents = new ArrayList();
				componentGroups.put(groupName, groupComponents);
			}
			
			groupComponents.add(componentContentVO);
		}
		
		return componentGroups;
	}

	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate"
	 */
	
	public List getComponentContents() throws Exception
	{
		HashMap arguments = new HashMap();
		arguments.put("method", "selectListOnContentTypeName");
		
		List argumentList = new ArrayList();
		HashMap argument = new HashMap();
		argument.put("contentTypeDefinitionName", "HTMLTemplate");
		argumentList.add(argument);
		arguments.put("arguments", argumentList);
		
		return ContentController.getContentController().getContentVOList(arguments, getDatabase());
	}
	
	/**
	 * This method fetches the pageComponent structure as a document.
	 */
	 /*   
	protected org.w3c.dom.Document getComponentPropertiesDocument(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{ 
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentPropertiesDocument_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		org.w3c.dom.Document cachedComponentPropertiesDocument = (org.w3c.dom.Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesDocument != null)
			return cachedComponentPropertiesDocument;
		
		org.w3c.dom.Document componentPropertiesDocument = null;
   	
		try
		{
			String xml = this.getComponentPropertiesString(templateController, siteNodeId, languageId, contentId);
			//logger.info("xml: " + xml);
			if(xml != null && xml.length() > 0)
			{
				componentPropertiesDocument = XMLHelper.readDocumentFromByteArray(xml.getBytes("UTF-8"));
				
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
	*/
	
	protected Document getComponentPropertiesDOM4JDocument(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{ 
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentPropertiesDocument_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		Document cachedComponentPropertiesDocument = (Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesDocument != null)
			return cachedComponentPropertiesDocument;
		
		Document componentPropertiesDocument = null;
   	
		try
		{
			String xml = this.getComponentPropertiesString(templateController, siteNodeId, languageId, contentId);
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
   
	private String getComponentPropertiesString(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentPropertiesString_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		String cachedComponentPropertiesString = (String)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentPropertiesString != null)
			return cachedComponentPropertiesString;
			
		String componentPropertiesString = null;
   	
		try
		{
		    Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(templateController.getDatabase(), siteNodeId).getId();
		    //logger.info("masterLanguageId:" + masterLanguageId);
		    componentPropertiesString = templateController.getContentAttribute(contentId, masterLanguageId, "ComponentProperties", true);

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
	 * This method fetches the tasks as a document.
	 */
/*	    
	protected org.w3c.dom.Document getComponentTasksDocument(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{ 	    
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTasksDocument_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		org.w3c.dom.Document cachedComponentTasksDocument = (org.w3c.dom.Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentTasksDocument != null)
			return cachedComponentTasksDocument;
		
		org.w3c.dom.Document componentTasksDocument = null;
   	
		try
		{
			String xml = this.getComponentTasksString(templateController, siteNodeId, languageId, contentId);
			if(xml != null && xml.length() > 0)
			{
			    componentTasksDocument = XMLHelper.readDocumentFromByteArray(xml.getBytes("UTF-8"));
				
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
*/
	
	protected Document getComponentTasksDOM4JDocument(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{ 	    
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTasksDocument_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		Document cachedComponentTasksDocument = (Document)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentTasksDocument != null)
			return cachedComponentTasksDocument;
		
		Document componentTasksDocument = null;
   	
		try
		{
			String xml = this.getComponentTasksString(templateController, siteNodeId, languageId, contentId);
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
   
	private String getComponentTasksString(TemplateController templateController, Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{
		String cacheName 	= "componentEditorCache";
		String cacheKey		= "componentTasksString_" + siteNodeId + "_" + templateController.getLanguageId() + "_" + contentId;
		String cachedComponentTasksString = (String)CacheController.getCachedObject(cacheName, cacheKey);
		if(cachedComponentTasksString != null)
			return cachedComponentTasksString;
			
		String componentTasksString = null;
   	
		try
		{
		    Integer masterLanguageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(getDatabase(), siteNodeId).getId();
		    componentTasksString = templateController.getContentAttribute(contentId, masterLanguageId, "ComponentTasks", true);

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
	
	public Collection getComponentProperties(Integer componentId, TemplateController templateController, Integer siteNodeId, Integer languageId, Integer componentContentId) throws Exception
	{
		Document componentPropertiesDocument = getComponentPropertiesDOM4JDocument(templateController, siteNodeId, languageId, componentContentId);
		return getComponentProperties(componentId, componentPropertiesDocument, templateController);
	}
	
	/*
	 * This method returns a bean representing a list of ComponentProperties that the component has.
	 */
	 
	private List getComponentProperties(Integer componentId, Document document) throws Exception
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
					String description					 = binding.attributeValue("description");
					String type							 = binding.attributeValue("type");
					String allowedContentTypeNamesString = binding.attributeValue("allowedContentTypeDefinitionNames");
					String visualizingAction 			 = binding.attributeValue("visualizingAction");
					String createAction 				 = binding.attributeValue("createAction");
					//logger.info("name:" + name);
					//logger.info("type:" + type);

					ComponentProperty property = new ComponentProperty();
					property.setComponentId(componentId);
					property.setName(name);
					property.setDescription(description);
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
						String value = getComponentPropertyValue(componentId, name);
						timer.printElapsedTime("Set property1");

						property.setValue(value);
						property.setIsMultipleBinding(isMultipleBinding);
						property.setIsAssetBinding(isAssetBinding);
						List<ComponentBinding> bindings = getComponentPropertyBindings(componentId, name, this.getTemplateController());
						property.setBindings(bindings);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTAREA))	
					{		
						String value = getComponentPropertyValue(componentId, name);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.SELECTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name);
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
						String value = getComponentPropertyValue(componentId, name);
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
		}
		catch(Exception e)
		{
			logger.warn("The component with id " + componentId + " had a incorrect xml defining it's properties:" + e.getMessage(), e);
		}
							
		return componentProperties;
	}


	/*
	 * This method returns a bean representing a list of ComponentProperties that the component has.
	 */
	 
	private List getComponentProperties(Integer componentId, Document document, TemplateController templateController) throws Exception
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
			//if(componentPropertiesXML != null && componentPropertiesXML.length() > 0)
			//{
				//org.w3c.dom.Document document = XMLHelper.readDocumentFromByteArray(componentPropertiesXML.getBytes("UTF-8"));

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
					String description					 = binding.attributeValue("description");
					String type							 = binding.attributeValue("type");
					String allowedContentTypeNamesString = binding.attributeValue("allowedContentTypeDefinitionNames");
					String visualizingAction 			 = binding.attributeValue("visualizingAction");
					String createAction 				 = binding.attributeValue("createAction");
					//logger.info("name:" + name);
					//logger.info("type:" + type);

					ComponentProperty property = new ComponentProperty();
					property.setComponentId(componentId);
					property.setName(name);
					property.setDescription(description);
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
						boolean isAssetBinding = new Boolean(binding.attributeValue("assetBinding")).booleanValue();
						
						property.setEntityClass(entity);
						String value = getComponentPropertyValue(componentId, name, templateController);
						timer.printElapsedTime("Set property1");

						property.setValue(value);
						property.setIsMultipleBinding(isMultipleBinding);
						property.setIsAssetBinding(isAssetBinding);
						List<ComponentBinding> bindings = getComponentPropertyBindings(componentId, name, templateController);
						property.setBindings(bindings);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name, templateController);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.TEXTAREA))	
					{		
						String value = getComponentPropertyValue(componentId, name, templateController);
						timer.printElapsedTime("Set property2");
						//logger.info("value:" + value);
						property.setValue(value);
					}
					else if(type.equalsIgnoreCase(ComponentProperty.SELECTFIELD))	
					{		
						String value = getComponentPropertyValue(componentId, name, templateController);
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
					
					componentProperties.add(property);
				}
			}
		}
		catch(Exception e)
		{
			logger.warn("The component with id " + componentId + " had a incorrect xml defining it's properties:" + e.getMessage(), e);
			e.printStackTrace();
		}
							
		return componentProperties;
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


	/**
	 * This method returns a value for a property if it's set. The value is collected in the
	 * properties for the page.
	 */
	
	private String getComponentPropertyValue(Integer componentId, String name) throws Exception
	{
		String value = "Undefined";
		
		Timer timer = new Timer();
		timer.setActive(false);
				
		Integer siteNodeId = null;
		Integer languageId = null;
		
		if(this.getRequest().getParameter("siteNodeId") != null && this.getRequest().getParameter("siteNodeId").length() > 0)
			siteNodeId = new Integer(this.getRequest().getParameter("siteNodeId"));
		else
		{
			siteNodeId = this.getTemplateController().getDeliveryContext().getSiteNodeId();
		}
		
		NodeDeliveryController nodeDeliveryController = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, null);

		if(this.getRequest().getParameter("languageId") != null && this.getRequest().getParameter("languageId").length() > 0)
		{
			languageId = new Integer(this.getRequest().getParameter("languageId"));
			if(!languageId.equals(this.getTemplateController().getDeliveryContext().getLanguageId()))
			{
				languageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNodeWithValityCheck(getDatabase(), nodeDeliveryController, siteNodeId).getId();				
			}
		}
		else
		{
			languageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNodeWithValityCheck(getDatabase(), nodeDeliveryController, siteNodeId).getId();
		}
		
		Locale locale = LanguageDeliveryController.getLanguageDeliveryController().getLocaleWithId(getDatabase(), languageId);
		
		Integer contentId  = new Integer(-1);
		if(this.getRequest().getParameter("contentId") != null && this.getRequest().getParameter("contentId").length() > 0)
			contentId  = new Integer(this.getRequest().getParameter("contentId"));
		
		Document document = getPageComponentsDOM4JDocument(getDatabase(), this.getTemplateController(), siteNodeId, languageId, contentId);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']";
		//logger.info("componentXPath:" + componentXPath);
		List anl = document.selectNodes(componentXPath);
		Iterator anlIterator = anl.iterator();
		while(anlIterator.hasNext())
		{
			Element property = (Element)anlIterator.next();
			
			String id 			= property.attributeValue("type");
			String path 		= property.attributeValue("path");
			
			//System.out.println("Locale:" + locale.getLanguage());
			if(property.attribute("path_" + locale.getLanguage()) != null)
				path = property.attributeValue("path_" + locale.getLanguage());

			value 				= path;
		}

		/*
		org.w3c.dom.Document document = getPageComponentsDocument(getDatabase(), this.getTemplateController(), siteNodeId, languageId, contentId);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']";
		//logger.info("componentXPath:" + componentXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		for(int i=0; i < anl.getLength(); i++)
		{
			org.w3c.dom.Element property = (org.w3c.dom.Element)anl.item(i);
			
			String id 			= property.getAttribute("type");
			String path 		= property.getAttribute("path");
			
			if(property.hasAttribute("path_" + locale.getLanguage()))
				path = property.getAttribute("path_" + locale.getLanguage());

			value 				= path;
		}
		*/
		
		return value;
	}

	/**
	 * This method returns a value for a property if it's set. The value is collected in the
	 * properties for the page.
	 */
	
	private List<ComponentBinding> getComponentPropertyBindings(Integer componentId, String name, TemplateController templateController) throws Exception
	{
		List<ComponentBinding> componentBindings = new ArrayList<ComponentBinding>();
		
		Timer timer = new Timer();
		timer.setActive(false);
				
		Integer siteNodeId = null;
		Integer languageId = null;
		
		if(this.getRequest() != null && this.getRequest().getParameter("siteNodeId") != null && this.getRequest().getParameter("siteNodeId").length() > 0)
			siteNodeId = new Integer(this.getRequest().getParameter("siteNodeId"));
		else
		{
			siteNodeId = templateController.getDeliveryContext().getSiteNodeId();
		}
		
		if(this.getRequest() != null && this.getRequest().getParameter("languageId") != null && this.getRequest().getParameter("languageId").length() > 0)
		{
			languageId = new Integer(this.getRequest().getParameter("languageId"));
			if(!languageId.equals(templateController.getDeliveryContext().getLanguageId()))
			{
				languageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(templateController.getDatabase(), siteNodeId).getId();				
			}
		}
		else
		    languageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(templateController.getDatabase(), siteNodeId).getId();
		        
		Locale locale = LanguageDeliveryController.getLanguageDeliveryController().getLocaleWithId(templateController.getDatabase(), languageId);
		
		Integer contentId  = new Integer(-1);
		if(this.getRequest() != null && this.getRequest().getParameter("contentId") != null && this.getRequest().getParameter("contentId").length() > 0)
			contentId  = new Integer(this.getRequest().getParameter("contentId"));

		Document document = getPageComponentsDOM4JDocument(templateController.getDatabase(), templateController, siteNodeId, languageId, contentId);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']/binding";
		//logger.info("componentXPath:" + componentXPath);
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

	/**
	 * This method returns a value for a property if it's set. The value is collected in the
	 * properties for the page.
	 */
	
	private String getComponentPropertyValue(Integer componentId, String name, TemplateController templateController) throws Exception
	{
		String value = "Undefined";
		
		Timer timer = new Timer();
		timer.setActive(false);
				
		Integer languageId = null;
		if(this.getRequest() != null && this.getRequest().getParameter("languageId") != null && this.getRequest().getParameter("languageId").length() > 0)
		    languageId = new Integer(this.getRequest().getParameter("languageId"));
		else
		    languageId = LanguageDeliveryController.getLanguageDeliveryController().getMasterLanguageForSiteNode(templateController.getDatabase(), templateController.getSiteNodeId()).getId();
		        
		Locale locale = LanguageDeliveryController.getLanguageDeliveryController().getLocaleWithId(templateController.getDatabase(), languageId);
		
		Integer contentId  = new Integer(-1);
		if(this.getRequest() != null && this.getRequest().getParameter("contentId") != null && this.getRequest().getParameter("contentId").length() > 0)
			contentId  = new Integer(this.getRequest().getParameter("contentId"));

		NodeDeliveryController nodeDeliveryController = NodeDeliveryController.getNodeDeliveryController(templateController.getSiteNodeId(), languageId, contentId);

		Document document = getPageComponentsDOM4JDocument(templateController.getDatabase(), templateController, templateController.getSiteNodeId(), languageId, contentId);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']";
		//logger.info("componentXPath:" + componentXPath);
		List anl = document.selectNodes(componentXPath);
		Iterator anlIterator = anl.iterator();
		while(anlIterator.hasNext())
		{
			Element property = (Element)anlIterator.next();
			
			String id 			= property.attributeValue("type");
			String path 		= property.attributeValue("path");
			
			if(property.attribute("path_" + locale.getLanguage()) != null)
				path = property.attributeValue("path_" + locale.getLanguage());

			value 				= path;
		}

		/*
		org.w3c.dom.Document document = getPageComponentsDocument(templateController.getDatabase(), templateController, templateController.getSiteNodeId(), languageId, contentId);
		
		String componentXPath = "//component[@id=" + componentId + "]/properties/property[@name='" + name + "']";
		//logger.info("componentXPath:" + componentXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		for(int i=0; i < anl.getLength(); i++)
		{
			org.w3c.dom.Element property = (org.w3c.dom.Element)anl.item(i);
			
			String id 			= property.getAttribute("type");
			String path 		= property.getAttribute("path");

			if(property.hasAttribute("path_" + locale.getLanguage()))
				path = property.getAttribute("path_" + locale.getLanguage());
				
			value 				= path;
		}
		*/

		return value;
	}


	/*
	 * This method returns a bean representing a list of bindings that the component has.
	 */
	 
	private List getContentBindnings(Integer componentId) throws Exception
	{
		List contentBindings = new ArrayList();
		
		Integer siteNodeId = new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId = new Integer(this.getRequest().getParameter("languageId"));
		Integer contentId  = new Integer(this.getRequest().getParameter("contentId"));

		String componentXML = getPageComponentsString(getDatabase(), this.getTemplateController(), siteNodeId, languageId, contentId);			
		////logger.info("componentXML:" + componentXML);

		Document document = domBuilder.getDocument(componentXML);
		String componentXPath = "//component[@id=" + componentId + "]/bindings/binding";
		//logger.info("componentXPath:" + componentXPath);
		List anl = document.selectNodes(componentXPath);
		Iterator anlIterator = anl.iterator();
		while(anlIterator.hasNext())
		{
			Element binding = (Element)anlIterator.next();
			//logger.info(XMLHelper.serializeDom(binding, new StringBuffer()));
			//logger.info("YES - we read the binding properties...");		
			
			String id 			= binding.attributeValue("id");
			String entityClass 	= binding.attributeValue("entity");
			String entityId 	= binding.attributeValue("entityId");
			String assetKey 	= binding.attributeValue("assetKey");
			//logger.info("id:" + id);
			//logger.info("entityClass:" + entityClass);
			//logger.info("entityId:" + entityId);
			
			if(entityClass.equalsIgnoreCase("Content"))
			{
				ContentVO contentVO = ContentDeliveryController.getContentDeliveryController().getContentVO(new Integer(entityId), getDatabase());
				ComponentBinding componentBinding = new ComponentBinding();
				componentBinding.setId(new Integer(id));
				componentBinding.setComponentId(componentId);
				componentBinding.setEntityClass(entityClass);
				componentBinding.setEntityId(new Integer(entityId));
				componentBinding.setAssetKey(assetKey);
				componentBinding.setBindingPath(contentVO.getName());
				
				contentBindings.add(componentBinding);
			}
		}
			
		return contentBindings;
	}
	 
	 
	private void printComponentHierarchy(List pageComponents, int level)
	{
		Iterator pageComponentIterator = pageComponents.iterator();
		while(pageComponentIterator.hasNext())
		{
			InfoGlueComponent tempComponent = (InfoGlueComponent)pageComponentIterator.next();
			
			for(int i=0; i<level; i++)
			    logger.info(" ");
			
			logger.info("  component:" + tempComponent.getName());
			
			Iterator slotIterator = tempComponent.getSlotList().iterator();
			while(slotIterator.hasNext())
			{
				Slot slot = (Slot)slotIterator.next();
				
				for(int i=0; i<level; i++)
					logger.info(" ");
					
				logger.info(" slot for " + tempComponent.getName() + ":" + slot.getId());
				printComponentHierarchy(slot.getComponents(), level + 1);
			}
		}			
	}
	
  	public String getLocalizedString(Locale locale, String key) 
  	{
    	StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);

    	return stringManager.getString(key);
  	}

}