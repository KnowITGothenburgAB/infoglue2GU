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


import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.entities.content.*;
import org.infoglue.cms.controllers.kernel.impl.simple.*;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.*;
import org.infoglue.cms.exception.*;
import org.infoglue.cms.entities.structure.SiteNodeVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.util.XMLHelper;
import org.infoglue.cms.util.sorters.ContentComparator;
import org.infoglue.cms.util.sorters.ReflectionComparator;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.controllers.kernel.impl.simple.IntegrationDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.LanguageDeliveryController;
import org.infoglue.deliver.controllers.kernel.impl.simple.NodeDeliveryController;

import org.w3c.dom.*;
import org.w3c.dom.Document;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

import java.net.URLEncoder;
import java.util.*;



public class ViewSiteNodePageComponentsAction extends InfoGlueAbstractAction
{
	public static final String CATEGORY_TREE = "showCategoryTree";
	public static final String CATEGORY_TREE_MULTIPLE = "showCategoryTreeForMultipleBinding";

	private Integer repositoryId = null;
	private Integer siteNodeId = null;
	private Integer languageId = null;
	private Integer contentId = null;
	private Integer parentComponentId = null;
	private Integer componentId = null;
	private String propertyName = null;
	private String path 		= null;
	private String slotId		= null;
	private String specifyBaseTemplate = null;
	private String url			= null;
	private Integer direction 	= null;
	private boolean showSimple 	= false;
	
	LanguageVO masterLanguageVO = null;
	
	private List repositories 				 = null;
	private String currentAction 		 	 = null;
	private Integer filterRepositoryId 		 = null; 
	private String sortProperty 			 = "name";
	private String[] allowedContentTypeNames = null;
	
	public ViewSiteNodePageComponentsAction()
	{
	}

	private void initialize() throws Exception
	{
		Integer currentRepositoryId = SiteNodeController.getSiteNodeVOWithId(this.siteNodeId).getRepositoryId();
		this.masterLanguageVO = LanguageController.getController().getMasterLanguage(currentRepositoryId);		
		if(filterRepositoryId == null)
		{
			Map args = new HashMap();
		    args.put("globalKey", "infoglue");
		    PropertySet ps = PropertySetManager.getInstance("jdbc", args);

		    String defaultTemplateRepository = ps.getString("repository_" + currentRepositoryId + "_defaultTemplateRepository");
		    if(defaultTemplateRepository != null && !defaultTemplateRepository.equals(""))
		        filterRepositoryId = new Integer(defaultTemplateRepository);
		    else
		        filterRepositoryId = currentRepositoryId;
		}
	}

	/**
	 * This method initializes the tree
	 */
	
	private void initializeTreeView(String currentAction) throws Exception
	{
		this.currentAction = currentAction;
		
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), true);
		
		if(this.repositoryId == null)
			this.repositoryId = RepositoryController.getController().getFirstRepositoryVO().getRepositoryId();
	}

	    
	/**
	 * This method which is the default one only serves to show a list 
	 * of tasks to the user so he/she can select one to run. 
	 */
    
	public String doExecute() throws Exception
	{
		initialize();
		return "success";
	}


	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doListComponents() throws Exception
	{
		CmsLogger.logInfo("queryString:" + this.getRequest().getQueryString());
		initialize();

		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), true);

		return "listComponents";
	}

	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doListComponentsForPalette() throws Exception
	{
		initialize();
		return "listComponentsForPalette";
	}
	
	/**
	 * This method shows the user a list of Contents. 
	 */
    
	public String doShowContentTree() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showContentTree.action");
		return "showContentTree";
	}

	/**
	 * This method shows the user a interface to choose multiple contents. 
	 */
    
	public String doShowContentTreeForMultipleBinding() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showContentTreeForMultipleBinding.action");
		return "showContentTreeForMultipleBinding";
	}

	/**
	 * This method shows the user a list of SiteNodes. 
	 */
    
	public String doShowStructureTree() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showStructureTree.action");
		return "showStructureTree";
	}
	
	/**
	 * This method shows the user a interface to choose multiple sitenodes. 
	 */
    
	public String doShowStructureTreeForMultipleBinding() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showStructureTreeForMultipleBinding.action");
		return "showStructureTreeForMultipleBinding";
	}
	
	/**
	 * This method shows the user a list of Categories.
	 */
	public String doShowCategoryTree() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showCategoryTree.action");
		return CATEGORY_TREE;
	}

	/**
	 * This method shows the user a list of Categories to chose multiple.
	 */
	public String doShowCategoryTreeForMultipleBinding() throws Exception
	{
		initialize();
		initializeTreeView("ViewSiteNodePageComponents!showCategoryTreeForMultipleBinding.action");
		return CATEGORY_TREE_MULTIPLE;
	}


	public List getRepositories()
	{
		return this.repositories;
	}

	public String getCurrentAction()
	{
		return this.currentAction;
	}

	public String getContentAttribute(Integer contentId, String attributeName) throws Exception
	{
	    String attribute = "Undefined";
	    
	    ContentVO contentVO = ContentController.getContentController().getContentVOWithId(contentId);
		
		LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(SiteNodeController.getSiteNodeVOWithId(siteNodeId).getRepositoryId());
		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguageVO.getId());

		attribute = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO, attributeName, false);
		
		return attribute;
	}	
	
	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doAddComponent() throws Exception
	{
		CmsLogger.logInfo("************************************************************");
		CmsLogger.logInfo("* ADDING COMPONENT                                         *");
		CmsLogger.logInfo("************************************************************");
		CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
		CmsLogger.logInfo("languageId:" + this.languageId);
		CmsLogger.logInfo("contentId:" + this.contentId);
		CmsLogger.logInfo("queryString:" + this.getRequest().getQueryString());
		CmsLogger.logInfo("parentComponentId:" + this.parentComponentId);
		CmsLogger.logInfo("componentId:" + this.componentId);
		CmsLogger.logInfo("slotId:" + this.slotId);
		CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);

		initialize();

		Integer newComponentId = new Integer(0);

		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
		
		if(this.specifyBaseTemplate.equalsIgnoreCase("true"))
		{
			String componentXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"" + componentId + "\" id=\"" + newComponentId + "\" name=\"base\"><properties></properties><bindings></bindings><components></components></component></components>";
			ContentVO templateContentVO = nodeDeliveryController.getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
			//ContentVO templateContentVO = nodeDeliveryController.getBoundContent(siteNodeId, "Meta information");		
			
			//CmsLogger.logInfo("templateContentVO:" + templateContentVO);
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(templateContentVO.getId(), languageId);
			ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", componentXML, new InfoGluePrincipal("ComponentEditor", "none", "none", "none", new ArrayList(), new ArrayList(), true));
		}
		else
		{
		    String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
	
			Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
			String componentXPath = "//component[@id=" + this.parentComponentId + "]/components";

			NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
			if(anl.getLength() > 0)
			{
				Element component = (Element)anl.item(0);
				
				String componentsXPath = "//component";
				NodeList nodes = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentsXPath);
				for(int i=0; i < nodes.getLength(); i++)
				{
					Element element = (Element)nodes.item(i);
					if(new Integer(element.getAttribute("id")).intValue() > newComponentId.intValue())
						newComponentId = new Integer(element.getAttribute("id"));
				}
				newComponentId = new Integer(newComponentId.intValue() + 1);
				
				Element newComponent = addComponentElement(component, new Integer(newComponentId.intValue()), this.slotId, this.componentId);
				String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 

				SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
				LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
				
				ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, masterLanguage.getId(), true, "Meta information", DeliveryContext.getDeliveryContext());
				ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
				
				ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
			}
		}
		
		CmsLogger.logInfo("newComponentId:" + newComponentId);
		
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + newComponentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}


	/**
	 * This method moves the component up a step if possible within the same slot. 
	 */
    
	public String doMoveComponent() throws Exception
	{
		initialize();
			
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
		
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		//String templateString = getPageTemplateString(templateController, siteNodeId, languageId, contentId); 
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);
		
		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentXPath = "//component[@id=" + this.componentId + "]";
	
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			String name = component.getAttribute("name");
			//CmsLogger.logInfo(XMLHelper.serializeDom(component, new StringBuffer()));
			Node parentNode = component.getParentNode();
			
			boolean hasChanged = false;
			
			if(this.direction.intValue() == 0) //Up
			{
			    Node previousNode = component.getPreviousSibling();
				Element element = ((Element)previousNode);
				while(element != null && !element.getAttribute("name").equalsIgnoreCase(name))
			    {
			        previousNode = previousNode.getPreviousSibling();
					element = ((Element)previousNode);
			    }
				
				if(previousNode != null)
				{
					parentNode.removeChild(component);
				    parentNode.insertBefore(component, previousNode);
				    hasChanged = true;
				}
			}
			else if(this.direction.intValue() == 1) //Down
			{
			    Node nextNode = component.getNextSibling();
			    Element element = ((Element)nextNode);
				while(element != null && !element.getAttribute("name").equalsIgnoreCase(name))
			    {
				    nextNode = nextNode.getNextSibling();
					element = ((Element)nextNode);
			    }
				
				if(nextNode != null)
				    nextNode = nextNode.getNextSibling();
				
				if(nextNode != null)
				{
					parentNode.removeChild(component);
				    parentNode.insertBefore(component, nextNode);
				    hasChanged = true;
				}
				else
				{
				    parentNode.removeChild(component);
				    parentNode.appendChild(component);
				    hasChanged = true;
				}
			}		
			
			if(hasChanged)
			{
				String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
				//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
				
				SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
				LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
				
				ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, masterLanguage.getId(), true, "Meta information", DeliveryContext.getDeliveryContext());
				//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
				ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
				//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
				ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
			}
		}
				
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}
	

	
	/**
	 * This method updates the given properties with new values. 
	 */
    
	public String doUpdateComponentProperties() throws Exception
	{
		initialize();

		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		CmsLogger.logInfo("************************************************************");
		CmsLogger.logInfo("* doUpdateComponentProperties                              *");
		CmsLogger.logInfo("************************************************************");
		CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
		CmsLogger.logInfo("languageId:" + this.languageId);
		CmsLogger.logInfo("contentId:" + this.contentId);
		CmsLogger.logInfo("componentId:" + this.componentId);
		CmsLogger.logInfo("slotId:" + this.slotId);
		CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);
		
		Iterator parameterNames = this.getRequest().getParameterMap().keySet().iterator();
		while(parameterNames.hasNext())
		{
			String name = (String)parameterNames.next();
			String value = (String)this.getRequest().getParameter(name);
			CmsLogger.logInfo(name + "=" + value);
		}

		Integer siteNodeId 	= new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId 	= new Integer(this.getRequest().getParameter("languageId"));
		
		Locale locale = LanguageController.getController().getLocaleWithId(languageId);
		
		String entity  		= this.getRequest().getParameter("entity");
		
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
		
		String componentXML = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);
		
		ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
		//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), this.languageId);
		if(contentVersionVO == null)
		{
			LanguageVO masterLanguageVO = LanguageController.getController().getMasterLanguage(SiteNodeController.getSiteNodeVOWithId(siteNodeId).getRepositoryId());
			contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguageVO.getId());
		}

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String test = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
		//FileHelper.writeToFile(new File("c:\\temp\\enc102.txt"), test, false);
		
		String characterEncoding= this.getRequest().getCharacterEncoding();
		characterEncoding= this.getResponse().getCharacterEncoding();
		
		int propertyIndex = 0;	
		String propertyName = this.getRequest().getParameter(propertyIndex + "_propertyName");
		while(propertyName != null && !propertyName.equals(""))
		{
			//String propertyName	= "alignment"; this.getRequest().getParameter("propertyName");
			String propertyValue= this.getRequest().getParameter(propertyName);
			
			/*
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(!characterEncoding.equalsIgnoreCase("utf-8"))
			{
				CmsLogger.logInfo("Encoding resulting html to " + characterEncoding);
			
				propertyValue = new String(propertyValue.getBytes("UTF-8"), characterEncoding);
			}	
			*/
			
			CmsLogger.logInfo("siteNodeId:" + siteNodeId);
			CmsLogger.logInfo("languageId:" + languageId);
			CmsLogger.logInfo("entity:" + entity);
			CmsLogger.logInfo("propertyName:" + propertyName);
			CmsLogger.logInfo("propertyValue:" + propertyValue);
			
			String componentPropertyXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']";
			//CmsLogger.logInfo("componentPropertyXPath:" + componentPropertyXPath);
			NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
			if(anl.getLength() == 0)
			{
				String componentXPath = "//component[@id=" + this.componentId + "]/properties";
				//CmsLogger.logInfo("componentXPath:" + componentXPath);
				NodeList componentNodeList = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
				if(componentNodeList.getLength() > 0)
				{
					Element componentProperties = (Element)componentNodeList.item(0);
					addPropertyElement(componentProperties, propertyName, propertyValue, "textfield", locale);
					anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
				}
			}
			
			//CmsLogger.logInfo("anl:" + anl);
			if(anl.getLength() > 0)
			{
				Element component = (Element)anl.item(0);
				component.setAttribute("path_" + locale.getLanguage(), propertyValue);
			}

			propertyIndex++;
			
			propertyName = this.getRequest().getParameter(propertyIndex + "_propertyName");
		}

		String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
		//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
		//FileHelper.writeToFile(new File("c:\\temp\\xml1.txt"), modifiedXML.getBytes());
		//FileHelper.writeToFile(new File("c:\\temp\\xml2.txt"), modifiedXML, false);
			
		//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
		ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
		
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + this.componentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}


	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doDeleteComponent() throws Exception
	{
		initialize();
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("* DELETING COMPONENT                                         *");
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
		//CmsLogger.logInfo("languageId:" + this.languageId);
		//CmsLogger.logInfo("contentId:" + this.contentId);
		//CmsLogger.logInfo("componentId:" + this.componentId);
		//CmsLogger.logInfo("slotId:" + this.slotId);
		//CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);
				
		CmsLogger.logInfo("doDeleteComponent:" + this.getRequest().getQueryString());
		
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
	
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		//String templateString = getPageTemplateString(templateController, siteNodeId, languageId, contentId); 
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentXPath = "//component[@id=" + this.componentId + "]";
		//CmsLogger.logInfo("componentXPath:" + componentXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		//CmsLogger.logInfo("anl:" + anl.getLength());
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			component.getParentNode().removeChild(component);
			//CmsLogger.logInfo(XMLHelper.serializeDom(component, new StringBuffer()));
			String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
			//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
			
			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());

			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
			//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
			//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
			ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
		}
		
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}
	
	    
	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doAddComponentPropertyBinding() throws Exception
	{
		initialize();
	//CmsLogger.logInfo("************************************************************");
	//CmsLogger.logInfo("* doAddComponentPropertyBinding                            *");
	//CmsLogger.logInfo("************************************************************");
	//CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
	//CmsLogger.logInfo("languageId:" + this.languageId);
	//CmsLogger.logInfo("contentId:" + this.contentId);
	//CmsLogger.logInfo("componentId:" + this.componentId);
	//CmsLogger.logInfo("slotId:" + this.slotId);
	//CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);

		Integer siteNodeId = new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId = new Integer(this.getRequest().getParameter("languageId"));
		
		Locale locale = LanguageController.getController().getLocaleWithId(languageId);
		
		String entity  = this.getRequest().getParameter("entity");
		Integer entityId  = new Integer(this.getRequest().getParameter("entityId"));
		String propertyName = this.getRequest().getParameter("propertyName");
			
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
	
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		//String templateString = getPageTemplateString(templateController, siteNodeId, languageId, contentId); 
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentPropertyXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']";
		//CmsLogger.logInfo("componentPropertyXPath:" + componentPropertyXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		if(anl.getLength() == 0)
		{
			String componentXPath = "//component[@id=" + this.componentId + "]/properties";
			//CmsLogger.logInfo("componentXPath:" + componentXPath);
			NodeList componentNodeList = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
			if(componentNodeList.getLength() > 0)
			{
				Element componentProperties = (Element)componentNodeList.item(0);
				addPropertyElement(componentProperties, propertyName, path, "contentBinding", locale);
				anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
			}
		}
		
		//CmsLogger.logInfo("anl:" + anl);
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			component.setAttribute("path_" + locale.getLanguage(), path);
			
			NodeList children = component.getChildNodes();
			for(int i=0; i < children.getLength(); i++)
			{
				Node node = children.item(i);
				component.removeChild(node);
			}
			
			Element newComponent = addBindingElement(component, entity, entityId);
			String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
			//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
			
			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());

			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
			//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
			//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
			ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
		}
					
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + this.componentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}


	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doAddComponentPropertyBindingWithQualifyer() throws Exception
	{
		initialize();
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("* doAddComponentPropertyBindingWithQualifyer               *");
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
		//CmsLogger.logInfo("languageId:" + this.languageId);
		//CmsLogger.logInfo("contentId:" + this.contentId);
		//CmsLogger.logInfo("componentId:" + this.componentId);
		//CmsLogger.logInfo("slotId:" + this.slotId);
		//CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);
		
		Integer siteNodeId 	= new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId 	= new Integer(this.getRequest().getParameter("languageId"));
		Integer contentId 	= new Integer(this.getRequest().getParameter("contentId"));
		
		Locale locale = LanguageController.getController().getLocaleWithId(languageId);

		String qualifyerXML = this.getRequest().getParameter("qualifyerXML");
		String propertyName = this.getRequest().getParameter("propertyName");
		
		//CmsLogger.logInfo("siteNodeId:" + siteNodeId);
		//CmsLogger.logInfo("languageId:" + languageId);
		//CmsLogger.logInfo("contentId:" + contentId);
		//CmsLogger.logInfo("qualifyerXML:" + qualifyerXML);
		//CmsLogger.logInfo("propertyName:" + propertyName);
			
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
		
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentPropertyXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']";
		//CmsLogger.logInfo("componentPropertyXPath:" + componentPropertyXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		if(anl.getLength() > 0)
		{
			Node propertyNode = anl.item(0);
			propertyNode.getParentNode().removeChild(propertyNode);
		}

		//Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		//String componentPropertyXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']";
		//CmsLogger.logInfo("componentPropertyXPath:" + componentPropertyXPath);
		//NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		//if(anl.getLength() == 0)
		//{
		String componentXPath = "//component[@id=" + this.componentId + "]/properties";
		//CmsLogger.logInfo("componentXPath:" + componentXPath);
		NodeList componentNodeList = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		if(componentNodeList.getLength() > 0)
		{
			Element componentProperties = (Element)componentNodeList.item(0);
			addPropertyElement(componentProperties, propertyName, path, "contentBinding", locale);
			anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		}
		//}
		
	//CmsLogger.logInfo("anl:" + anl);
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			component.setAttribute("path_" + locale.getLanguage(), path);
			
			/*NodeList children = component.getElementsByTagName("binding");
			for(int i=0; i < children.getLength(); i++)
			{
				Node node = children.item(i);
				component.removeChild(node);
			//CmsLogger.logInfo("Removing childNode:" + node.toString());
			}
			
		//CmsLogger.logInfo("Property:" + XMLHelper.serializeDom(component, new StringBuffer()));
		//CmsLogger.logInfo("YES - now only add the new propertyValue...");		
			*/
									
			addBindingElement(component, qualifyerXML);
			String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
		//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
			
			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());

			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
			//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
		//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
			ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
		}
					
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + this.componentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}
	
	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public String doDeleteComponentBinding() throws Exception
	{
		initialize();
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("* doDeleteComponentBinding               *");
		//CmsLogger.logInfo("************************************************************");
		//CmsLogger.logInfo("siteNodeId:" + this.siteNodeId);
		//CmsLogger.logInfo("languageId:" + this.languageId);
		//CmsLogger.logInfo("contentId:" + this.contentId);
		//CmsLogger.logInfo("componentId:" + this.componentId);
		//CmsLogger.logInfo("slotId:" + this.slotId);
		//CmsLogger.logInfo("specifyBaseTemplate:" + this.specifyBaseTemplate);

		Integer siteNodeId 	= new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId 	= new Integer(this.getRequest().getParameter("languageId"));
		Integer contentId  	= new Integer(this.getRequest().getParameter("contentId"));
		Integer bindingId  	= new Integer(this.getRequest().getParameter("bindingId"));
		
		//CmsLogger.logInfo("siteNodeId:" + siteNodeId);
		//CmsLogger.logInfo("languageId:" + languageId);
		//CmsLogger.logInfo("contentId:" + contentId);
			
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
		
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		//String templateString = getPageTemplateString(templateController, siteNodeId, languageId, contentId); 
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentXPath = "//component[@id=" + this.componentId + "]/bindings/binding[@id=" + bindingId + "]";
		//CmsLogger.logInfo("componentXPath:" + componentXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
		//CmsLogger.logInfo("anl:" + anl.getLength());
		if(anl.getLength() > 0)
		{
			Element component = (Element)anl.item(0);
			component.getParentNode().removeChild(component);
			//CmsLogger.logInfo(XMLHelper.serializeDom(component, new StringBuffer()));
			String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
			//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
			
			SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
			LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());

			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
			//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
			//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
			ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());
		}
			
		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + this.componentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		//this.getResponse().sendRedirect(url);
	    return NONE; 
	}
		    
		    
	/**
	 * This method shows the user a list of Components(HTML Templates). 
	 */
    
	public List getComponentBindings() throws Exception
	{
		List bindings = new ArrayList();
			
		try
		{
			Integer siteNodeId = new Integer(this.getRequest().getParameter("siteNodeId"));
			Integer languageId = new Integer(this.getRequest().getParameter("languageId"));
			Integer contentId  = new Integer(this.getRequest().getParameter("contentId"));
			String propertyName = this.getRequest().getParameter("propertyName");
	
			//CmsLogger.logInfo("**********************************************************************************");
			//CmsLogger.logInfo("siteNodeId:" + siteNodeId);
			//CmsLogger.logInfo("languageId:" + languageId);
			//CmsLogger.logInfo("contentId:" + contentId);
			//CmsLogger.logInfo("**********************************************************************************");
				
			NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
			IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
			
			boolean USE_LANGUAGE_FALLBACK        			= true;
			boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
			
			//String templateString = getPageTemplateString(templateController, siteNodeId, languageId, contentId); 
			String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
			//CmsLogger.logInfo("componentXML:" + componentXML);
	
			Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
			String componentXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']/binding";
			//CmsLogger.logInfo("componentXPath:" + componentXPath);
			NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentXPath);
			//CmsLogger.logInfo("anl:" + anl.getLength());
			for(int i=0; i<anl.getLength(); i++)
			{
				Element component = (Element)anl.item(i);
				String entityName = component.getAttribute("entity");
				String entityId = component.getAttribute("entityId");
				
				try
				{
					String path = "Undefined";
					if(entityName.equalsIgnoreCase("SiteNode"))
					{
						SiteNodeVO siteNodeVO = SiteNodeController.getSiteNodeVOWithId(new Integer(entityId));
						path = siteNodeVO.getName();
					}
					else if(entityName.equalsIgnoreCase("Content")) 
					{
						ContentVO contentVO = ContentController.getContentController().getContentVOWithId(new Integer(entityId));
						path = contentVO.getName();
					}
					
					Map binding = new HashMap();
					binding.put("entityName", entityName);
					binding.put("entityId", entityId);
					binding.put("path", path);
					bindings.add(binding);
				}
				catch(Exception e) 
				{
				    CmsLogger.logWarning("There was " + entityName + " bound to property '" + propertyName + "' on siteNode " + siteNodeId + " which appears to have been deleted.");
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return bindings;
	}
			    
	//Nice code
	
	/**
	 * This method deletes a component property value. This is to enable users to quickly remove a property value no matter what type.
	 */
    
	public String doDeleteComponentPropertyValue() throws Exception
	{
		initialize();
	
		Integer siteNodeId 	= new Integer(this.getRequest().getParameter("siteNodeId"));
		Integer languageId 	= new Integer(this.getRequest().getParameter("languageId"));
		Integer contentId  	= new Integer(this.getRequest().getParameter("contentId"));
		String propertyName	= this.getRequest().getParameter("propertyName");
		
		//CmsLogger.logInfo("siteNodeId:" + siteNodeId);
		//CmsLogger.logInfo("languageId:" + languageId);
		//CmsLogger.logInfo("contentId:" + contentId);
		//CmsLogger.logInfo("propertyName:" + propertyName);
			
		NodeDeliveryController nodeDeliveryController			    = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId);
		IntegrationDeliveryController integrationDeliveryController = IntegrationDeliveryController.getIntegrationDeliveryController(siteNodeId, languageId, contentId);
	
		boolean USE_LANGUAGE_FALLBACK        			= true;
		boolean DO_NOT_USE_LANGUAGE_FALLBACK 			= false;
		
		String componentXML   = getPageComponentsString(siteNodeId, languageId, contentId);			
		//CmsLogger.logInfo("componentXML:" + componentXML);

		Document document = XMLHelper.readDocumentFromByteArray(componentXML.getBytes("UTF-8"));
		String componentPropertyXPath = "//component[@id=" + this.componentId + "]/properties/property[@name='" + propertyName + "']";
		//CmsLogger.logInfo("componentPropertyXPath:" + componentPropertyXPath);
		NodeList anl = org.apache.xpath.XPathAPI.selectNodeList(document.getDocumentElement(), componentPropertyXPath);
		if(anl.getLength() > 0)
		{
			Node propertyNode = anl.item(0);
			propertyNode.getParentNode().removeChild(propertyNode);
		}

		String modifiedXML = XMLHelper.serializeDom(document, new StringBuffer()).toString(); 
		//CmsLogger.logInfo("modifiedXML:" + modifiedXML);
		
		SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
		LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());

		ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());
		//ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(siteNodeId, "Meta information");		
		ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getId());
		//CmsLogger.logInfo("contentVersionVO:" + contentVersionVO.getContentVersionId());
		ContentVersionController.getContentVersionController().updateAttributeValue(contentVersionVO.getContentVersionId(), "ComponentStructure", modifiedXML, this.getInfoGluePrincipal());

		this.url = getComponentRendererUrl() + getComponentRendererAction() + "?siteNodeId=" + this.siteNodeId + "&languageId=" + this.languageId + "&contentId=" + this.contentId + "&activatedComponentId=" + this.componentId + "&showSimple=" + this.showSimple;
		//this.getResponse().sendRedirect(url);		
		
		this.url = this.getResponse().encodeURL(url);
		this.getResponse().sendRedirect(url);
	    return NONE; 
	}
		    
			    
			    
	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private Element addPropertyElement(Element parent, String name, String path, String type, Locale locale)
	{
		Element element = parent.getOwnerDocument().createElement("property");
		element.setAttribute("name", name);
		element.setAttribute("path_" + locale.getLanguage(), path);
		element.setAttribute("type", type);
		parent.appendChild(element);
		return element;
	}
	
	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private Element addComponentElement(Element parent, Integer id, String name, Integer contentId)
	{
		Element element = parent.getOwnerDocument().createElement("component");
		element.setAttribute("id", id.toString());
		element.setAttribute("contentId", contentId.toString());
		element.setAttribute("name", name);
		Element properties = parent.getOwnerDocument().createElement("properties");
		element.appendChild(properties);
		Element subComponents = parent.getOwnerDocument().createElement("components");
		element.appendChild(subComponents);
		parent.appendChild(element);
		return element;
	}
	   
	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private Element addBindingElement(Element parent, String entity, Integer entityId)
	{
		Element element = parent.getOwnerDocument().createElement("binding");
		element.setAttribute("entityId", entityId.toString());
		element.setAttribute("entity", entity);
		parent.appendChild(element);
		return element;
	}

	/**
	 * This method creates a parameter for the given input type.
	 * This is to support form steering information later.
	 */
	
	private void addBindingElement(Element parent, String qualifyerXML) throws Exception
	{
	//CmsLogger.logInfo("qualifyerXML:" + qualifyerXML);
		Document document = XMLHelper.readDocumentFromByteArray(qualifyerXML.getBytes());
		NodeList nl = document.getChildNodes().item(0).getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Element qualifyerElement = (Element)nl.item(i);
		//CmsLogger.logInfo("qualifyerElement:" + qualifyerElement);
			String entityName = qualifyerElement.getNodeName();
			String entityId = qualifyerElement.getFirstChild().getNodeValue();
		//CmsLogger.logInfo("entityName:" + entityName);
		//CmsLogger.logInfo("entityId:" + entityId);
			
			Element element = parent.getOwnerDocument().createElement("binding");
			element.setAttribute("entityId", entityId);
			element.setAttribute("entity", entityName);
			parent.appendChild(element);
		}
	}
	
	
	/**
	 * This method returns the contents that are of contentTypeDefinition "HTMLTemplate" sorted on the property given.
	 */
	
	public List getSortedComponents(String sortProperty) throws Exception
	{
	    List componentVOList = null;
	    
	    try
	    {
	        String direction = "asc";
	        componentVOList = ComponentController.getController().getComponentVOList(sortProperty, direction);
	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();
	    }
		
	    return componentVOList;
	}
	     	     
	/**
	 * This method fetches the template-string.
	 */
    
	private String getPageComponentsString(Integer siteNodeId, Integer languageId, Integer contentId) throws SystemException, Exception
	{
		String template = null;
    	
		try
		{
			ContentVO contentVO = NodeDeliveryController.getNodeDeliveryController(siteNodeId, languageId, contentId).getBoundContent(this.getInfoGluePrincipal(), siteNodeId, languageId, true, "Meta information", DeliveryContext.getDeliveryContext());

			if(contentVO == null)
				throw new SystemException("There was no template bound to this page which makes it impossible to render.");	
			
			ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), languageId);
			if(contentVersionVO == null)
			{
				SiteNodeVO siteNodeVO = SiteNodeController.getSiteNodeVOWithId(siteNodeId);
				LanguageVO masterLanguage = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
				contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), masterLanguage.getLanguageId());
			}
			
			template = ContentVersionController.getContentVersionController().getAttributeValue(contentVersionVO.getId(), "ComponentStructure", false);
			
			if(template == null)
				throw new SystemException("There was no template bound to this page which makes it impossible to render.");	
		}
		catch(Exception e)
		{
			CmsLogger.logSevere(e.getMessage(), e);
			throw e;
		}

		return template;
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
			CmsLogger.logWarning("We could not get the url of the digitalAsset: " + e.getMessage(), e);
			imageHref = e.getMessage();
		}
		
		return imageHref;
	}
	
	public Integer getContentId()
	{
		return contentId;
	}

	public void setContentId(Integer integer)
	{
		contentId = integer;
	}

	public Integer getComponentId()
	{
		return this.componentId;
	}

	public void setComponentId(Integer componentId)
	{
		this.componentId = componentId;
	}
	
	public Integer getParentComponentId() 
	{
		return parentComponentId;
	}
	
    public void setParentComponentId(Integer parentComponentId) 
    {
		this.parentComponentId = parentComponentId;
	}

	public Integer getLanguageId()
	{
		return this.languageId;
	}

	public Integer getSiteNodeId()
	{
		return this.siteNodeId;
	}

	public void setLanguageId(Integer languageId)
	{
		this.languageId = languageId;
	}

	public void setSiteNodeId(Integer siteNodeId)
	{
		this.siteNodeId = siteNodeId;
	}

	public String getSlotId()
	{
		return this.slotId;
	}

	public void setSlotId(String slotId)
	{
		this.slotId = slotId;
	}

	public Integer getRepositoryId()
	{
		return this.repositoryId;
	}

	public void setRepositoryId(Integer repositoryId)
	{
		this.repositoryId = repositoryId;
	}

    public Integer getFilterRepositoryId()
    {
        return filterRepositoryId;
    }
    
    public void setFilterRepositoryId(Integer filterRepositoryId)
    {
        this.filterRepositoryId = filterRepositoryId;
    }

	public String getSpecifyBaseTemplate()
	{
		return this.specifyBaseTemplate;
	}

	public void setSpecifyBaseTemplate(String specifyBaseTemplate)
	{
		this.specifyBaseTemplate = specifyBaseTemplate;
	}

	public String getPropertyName()
	{
		return this.propertyName;
	}

	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}

	public String getPath()
	{
		return this.path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}
	
	public LanguageVO getMasterLanguageVO()
	{
		return masterLanguageVO;
	}
	
    public String getUrl()
    {
        return url;
    }
	
    public String getSortProperty()
    {
        return sortProperty;
    }
    
    public void setSortProperty(String sortProperty)
    {
        this.sortProperty = sortProperty;
    }
    
    public Integer getDirection()
    {
        return direction;
    }
    
    public void setDirection(Integer direction)
    {
        this.direction = direction;
    }
    
    public String[] getAllowedContentTypeNames()
    {
        return allowedContentTypeNames;
    }
    
    public void setAllowedContentTypeNames(String[] allowedContentTypeNames)
    {
        this.allowedContentTypeNames = allowedContentTypeNames;
    }
    
    public String getAllowedContentTypeNamesAsUrlEncodedString() throws Exception
    {
        StringBuffer sb = new StringBuffer();
        
        for(int i=0; i<allowedContentTypeNames.length; i++)
        {
            if(i > 0)
                sb.append("&");
            
            sb.append("allowedContentTypeNames=" + URLEncoder.encode(allowedContentTypeNames[i], "UTF-8"));
        }
        
        return sb.toString();
    }
    
    public boolean getShowSimple()
    {
        return showSimple;
    }
    
    public void setShowSimple(boolean showSimple)
    {
        this.showSimple = showSimple;
    }
}
