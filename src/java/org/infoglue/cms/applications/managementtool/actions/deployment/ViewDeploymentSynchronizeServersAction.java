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

package org.infoglue.cms.applications.managementtool.actions.deployment;

import java.io.IOException;
import java.io.StringReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.xml.namespace.QName;

import org.apache.xerces.parsers.DOMParser;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.databeans.AssetKeyDefinition;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.CategoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.GroupControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.RoleControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.WorkflowDefinitionController;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.management.Category;
import org.infoglue.cms.entities.management.CategoryAttribute;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.entities.management.ContentTypeAttribute;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.workflow.WorkflowDefinition;
import org.infoglue.cms.entities.workflow.WorkflowDefinitionVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.mail.MailServiceFactory;
import org.infoglue.cms.util.sorters.ReflectionComparator;
import org.infoglue.deliver.util.HttpUtilities;
import org.infoglue.deliver.util.webservices.DynamicWebservice;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ViewDeploymentSynchronizeServersAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 1L;
	
	private boolean synchronizeContentTypeDefinitions;
	private boolean synchronizeCategories;
	private boolean synchronizeWorkflows;
	private boolean synchronizeComponents;
	
	private Integer deploymentServerIndex = null;
	private String synchronizationMethod = "pull";
	private List<DeploymentCompareBean> deviatingContentTypes = new ArrayList<DeploymentCompareBean>();
	private List<DeploymentCompareBean> deviatingCategoryVOList = new ArrayList<DeploymentCompareBean>();
	private List<DeploymentCompareBean> deviatingWorkflows = new ArrayList<DeploymentCompareBean>();
	private List<DeploymentCompareBean> deviatingContents = new ArrayList<DeploymentCompareBean>();
	private List<DeploymentCompareBean> deviatingSiteNodes = new ArrayList<DeploymentCompareBean>();
	
	private static ContentTypeDefinitionController contentTypeDefinitionController = ContentTypeDefinitionController.getController();
	private static CategoryController categoryController = CategoryController.getController();

    public String doInput() throws Exception
    {
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);
    	
    	String targetEndpointAddress = deploymentServerUrl + "/services/RemoteDeploymentService";
    	
    	if(synchronizeContentTypeDefinitions)
    	{
	    	Object[] contentTypeDefinitionVOArray = (Object[])invokeOperation(targetEndpointAddress, "getContentTypeDefinitions", "contentTypeDefinition", null, ContentTypeDefinitionVO.class, "infoglue");
	    	List remoteContentTypeDefinitionVOList = Arrays.asList(contentTypeDefinitionVOArray);
		    Collections.sort(remoteContentTypeDefinitionVOList, new ReflectionComparator("name"));
	
	    	//System.out.println("remoteContentTypeDefinitionVOList:" + remoteContentTypeDefinitionVOList.size());
	    	if(this.synchronizationMethod.equalsIgnoreCase("pull"))
	    	{
		    	Iterator remoteContentTypeDefinitionVOListIterator = remoteContentTypeDefinitionVOList.iterator();
		    	while(remoteContentTypeDefinitionVOListIterator.hasNext())
		    	{
		    		ContentTypeDefinitionVO remoteContentTypeDefinitionVO = (ContentTypeDefinitionVO)remoteContentTypeDefinitionVOListIterator.next();
		    		//System.out.println("remoteContentTypeDefinitionVO:" + remoteContentTypeDefinitionVO.getName());
		    		ContentTypeDefinitionVO localContentTypeDefinitionVO = (ContentTypeDefinitionVO)ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(remoteContentTypeDefinitionVO.getName());
		    		DeploymentCompareBean bean = new DeploymentCompareBean();
		    		bean.setRemoteVersion(remoteContentTypeDefinitionVO);
		    		if(localContentTypeDefinitionVO != null)
		    		{
		    			//System.out.println("localContentTypeDefinitionVO:" + localContentTypeDefinitionVO.getName());
		        		bean.setLocalVersion(localContentTypeDefinitionVO);    			
		    		}
		    		deviatingContentTypes.add(bean);
		    	}
	    	}
	    	else
	    	{
	    		System.out.println("Getting what content types are not the same from a push perspective...");
	    		List localContentTypeDefinitionVOList = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList();
	    		Iterator localContentTypeDefinitionVOListIterator = localContentTypeDefinitionVOList.iterator();
		    	while(localContentTypeDefinitionVOListIterator.hasNext())
		    	{
		    		ContentTypeDefinitionVO localContentTypeDefinitionVO = (ContentTypeDefinitionVO)localContentTypeDefinitionVOListIterator.next();
		    		Iterator remoteContentTypeDefinitionVOListIterator = remoteContentTypeDefinitionVOList.iterator();
		    		ContentTypeDefinitionVO remoteContentTypeDefinitionVO = null;
			    	while(remoteContentTypeDefinitionVOListIterator.hasNext())
			    	{
			    		ContentTypeDefinitionVO remoteContentTypeDefinitionVOCandidate = (ContentTypeDefinitionVO)remoteContentTypeDefinitionVOListIterator.next();
			    		//System.out.println("remoteContentTypeDefinitionVO:" + remoteContentTypeDefinitionVO.getName());
			    		if(remoteContentTypeDefinitionVOCandidate.getName().equals(localContentTypeDefinitionVO.getName()))
			    			remoteContentTypeDefinitionVO = remoteContentTypeDefinitionVOCandidate;
			    	}	
			    	
			    	DeploymentCompareBean bean = new DeploymentCompareBean();
			    	bean.setLocalVersion(localContentTypeDefinitionVO); 
			    	if(remoteContentTypeDefinitionVO != null)
		    		{
		    			//System.out.println("localContentTypeDefinitionVO:" + localContentTypeDefinitionVO.getName());
			    		bean.setRemoteVersion(remoteContentTypeDefinitionVO);
			    	}
		    		deviatingContentTypes.add(bean);
		    	}
	    	}
    	}
    	
    	if(synchronizeCategories)
    	{
	    	//Getting deviatingCategories
	    	Object[] categoryVOArray = (Object[])invokeOperation(targetEndpointAddress, "getAllActiveCategories", "category", null, CategoryVO.class, "infoglue");
	    	List remoteCategoryVOList = Arrays.asList(categoryVOArray);
		    Collections.sort(remoteCategoryVOList, new ReflectionComparator("name"));
		    //System.out.println("remoteCategoryVOList:" + remoteCategoryVOList.size());
	    	
		    List<CategoryVO> allLocalCategories = CategoryController.getController().findAllActiveCategories(true);
		    //System.out.println("allLocalCategories:" + allLocalCategories.size());
	    	
		    if(this.synchronizationMethod.equalsIgnoreCase("push"))
		    	compareCategoryLists(remoteCategoryVOList, allLocalCategories);
		    else
		    	compareCategoryLists(allLocalCategories, remoteCategoryVOList);
		    	
		    //System.out.println("deviatingCategoryVOList:" + deviatingCategoryVOList.size());
		    
    	}
    	
    	if(synchronizeWorkflows)
    	{
	    	//Getting deviatingWorkflows
	    	Object[] workflowVOArray = (Object[])invokeOperation(targetEndpointAddress, "getWorkflowDefinitions", "workflowDefinition", null, WorkflowDefinitionVO.class, "infoglue");
	    	List remoteWorkflowDefinitionVOList = Arrays.asList(workflowVOArray);
		    Collections.sort(remoteWorkflowDefinitionVOList, new ReflectionComparator("name"));
	
		    //System.out.println("remoteWorkflowDefinitionVOList:" + remoteWorkflowDefinitionVOList.size());
	    	
	    	if(this.synchronizationMethod.equalsIgnoreCase("pull"))
	    	{
		    	Iterator remoteWorkflowDefinitionVOListIterator = remoteWorkflowDefinitionVOList.iterator();
		    	while(remoteWorkflowDefinitionVOListIterator.hasNext())
		    	{
		    		WorkflowDefinitionVO remoteWorkflowDefinitionVO = (WorkflowDefinitionVO)remoteWorkflowDefinitionVOListIterator.next();
		    		//System.out.println("remoteWorkflowDefinitionVO:" + remoteWorkflowDefinitionVO.getName());
		    		WorkflowDefinitionVO localWorkflowDefinitionVO = (WorkflowDefinitionVO)WorkflowDefinitionController.getController().getWorkflowDefinitionVOWithName(remoteWorkflowDefinitionVO.getName());
		    		//System.out.println("localWorkflowDefinitionVO:" + localWorkflowDefinitionVO);
		    		DeploymentCompareBean bean = new DeploymentCompareBean();
		    		bean.setRemoteVersion(remoteWorkflowDefinitionVO);
		    		if(localWorkflowDefinitionVO != null)
		    		{
		    			//System.out.println("localWorkflowDefinitionVO:" + localWorkflowDefinitionVO.getName());
		        		bean.setLocalVersion(localWorkflowDefinitionVO);    			
		    		}
		    		deviatingWorkflows.add(bean);
		    	}
	    	}
	    	else
	    	{
	    		System.out.println("Getting what workflow definitions are not the same from a push perspective...");
	    		List localWorkflowDefinitionVOList = WorkflowDefinitionController.getController().getWorkflowDefinitionVOList();
	    		Iterator localWorkflowDefinitionVOListIterator = localWorkflowDefinitionVOList.iterator();
		    	while(localWorkflowDefinitionVOListIterator.hasNext())
		    	{
		    		WorkflowDefinitionVO localWorkflowDefinitionVO = (WorkflowDefinitionVO)localWorkflowDefinitionVOListIterator.next();
		    		Iterator remoteWorkflowDefinitionVOListIterator = remoteWorkflowDefinitionVOList.iterator();
		    		WorkflowDefinitionVO remoteWorkflowDefinitionVO = null;
			    	while(remoteWorkflowDefinitionVOListIterator.hasNext())
			    	{
			    		WorkflowDefinitionVO remoteWorkflowDefinitionVOCandidate = (WorkflowDefinitionVO)remoteWorkflowDefinitionVOListIterator.next();
			    		//System.out.println("remoteWorkflowDefinitionVOCandidate:" + remoteWorkflowDefinitionVOCandidate.getName());
			    		if(remoteWorkflowDefinitionVOCandidate.getName().equals(localWorkflowDefinitionVO.getName()))
			    			remoteWorkflowDefinitionVO = remoteWorkflowDefinitionVOCandidate;
			    	}	
			    	
			    	DeploymentCompareBean bean = new DeploymentCompareBean();
			    	bean.setLocalVersion(localWorkflowDefinitionVO); 
			    	if(remoteWorkflowDefinitionVO != null)
		    		{
		    			//System.out.println("localContentTypeDefinitionVO:" + localContentTypeDefinitionVO.getName());
			    		bean.setRemoteVersion(remoteWorkflowDefinitionVO);
			    	}
			    	deviatingWorkflows.add(bean);
		    	}
	    	}
	    }
    	
    	//System.out.println("synchronizeComponents:" + synchronizeComponents);
    	if(synchronizeComponents)
    	{
	    	//Getting deviatingComponents
	    	Object[] contentVOArray = (Object[])invokeOperation(targetEndpointAddress, "getComponents", "content", null, ContentVO.class, "infoglue");
	    	List remoteContentVOList = Arrays.asList(contentVOArray);
	    	List components = ContentController.getContentController().getContentVOWithContentTypeDefinition("HTMLTemplate");

	    	if(this.synchronizationMethod.equalsIgnoreCase("pull"))
	    	{
		    	Iterator remoteContentVOListIterator = remoteContentVOList.iterator();
		    	while(remoteContentVOListIterator.hasNext())
		    	{
		    		ContentVO remoteContentVO = (ContentVO)remoteContentVOListIterator.next();
		    		//System.out.println("remoteContentVO:" + remoteContentVO.getName());
		    		
		    		Iterator componentsIterator = components.iterator();
		    		ContentVO localContentVO = null;
		    		while(componentsIterator.hasNext())
		    		{
		    			ContentVO candidate = (ContentVO)componentsIterator.next();
		    			if(candidate.getName().equals(remoteContentVO.getName()))
		    			{
		    				localContentVO = candidate;
		    			}
		    		}
	
		    		DeploymentCompareBean bean = new DeploymentCompareBean();
		    		bean.setRemoteVersion(remoteContentVO);
		    		if(localContentVO != null)
		    		{
		        		bean.setLocalVersion(localContentVO);
						LanguageVO languageVO = LanguageController.getController().getMasterLanguage(localContentVO.getRepositoryId());
						ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(localContentVO.getId(), languageVO.getId());
						if(contentVersionVO != null)
						{
							localContentVO.setVersions(new String[]{contentVersionVO.getVersionValue()});
						}
		    		}
	
		    		deviatingContents.add(bean);
		    	}
	    	}
	    	else
	    	{
	    		Iterator componentsIterator = components.iterator();
	    		while(componentsIterator.hasNext())
	    		{
	    			ContentVO localContentVO = (ContentVO)componentsIterator.next();
					String fullPath = ContentController.getContentController().getContentPath(localContentVO.getId(), true, true);
					localContentVO.setFullPath(fullPath);

					Iterator remoteContentVOListIterator = remoteContentVOList.iterator();
		    		ContentVO remoteContentVO = null;
		    		while(remoteContentVOListIterator.hasNext())
			    	{
			    		ContentVO remoteContentVOCandidate = (ContentVO)remoteContentVOListIterator.next();
			    		if(localContentVO.getName().equals(remoteContentVOCandidate.getName()))
			    			remoteContentVO = remoteContentVOCandidate;
			    	}
		    		
		    		DeploymentCompareBean bean = new DeploymentCompareBean();
					LanguageVO languageVO = LanguageController.getController().getMasterLanguage(localContentVO.getRepositoryId());
					ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(localContentVO.getId(), languageVO.getId());
					if(contentVersionVO != null)
					{
						localContentVO.setVersions(new String[]{contentVersionVO.getVersionValue()});
					}
		    		bean.setLocalVersion(localContentVO);

		    		if(remoteContentVO != null)
		    		{
		        		bean.setRemoteVersion(remoteContentVO);
		    		}
	
		    		deviatingContents.add(bean);
	    		}
	    	}
    	}

    	return "input";
    }

    private void compareCategoryLists(List remoteCategoryVOList, List<CategoryVO> allLocalCategories)
	{
    	Iterator remoteCategoryVOListIterator = remoteCategoryVOList.iterator();
    	while(remoteCategoryVOListIterator.hasNext())
    	{
    		CategoryVO remoteCategoryVO = (CategoryVO)remoteCategoryVOListIterator.next();
    		//System.out.println("remoteCategoryVO:" + remoteCategoryVO.getName());
    		
    		boolean categoryExists = false;
    		CategoryVO localCategoryVO = null;
    		Iterator allLocalCategoriesIterator = allLocalCategories.iterator();
    		while(allLocalCategoriesIterator.hasNext())
        	{
        		localCategoryVO = (CategoryVO)allLocalCategoriesIterator.next();
        		//System.out.println("remoteCategoryVO:" + remoteCategoryVO.getName());
        		if(localCategoryVO.getName().equals(remoteCategoryVO.getName()))
        		{
        			categoryExists = true;
        			break;
        		}
        	}
        	
        	if(!categoryExists)
        	{
        		DeploymentCompareBean bean = new DeploymentCompareBean();
	    		bean.setRemoteVersion(remoteCategoryVO);
	    		deviatingCategoryVOList.add(bean);
        	}
        	
        	if(remoteCategoryVO.getChildren() != null && remoteCategoryVO.getChildren().size() > 0)
        	{
        		if(localCategoryVO != null)
        			compareCategoryLists(remoteCategoryVO.getChildren(), localCategoryVO.getChildren());
        		else
        			compareCategoryLists(remoteCategoryVO.getChildren(), new ArrayList());
        	}
        }
    }
    
    
    
    //********************** UPDATE methods ************************
    
    /**
     * 
     * @return
     * @throws Exception
     */
    
	public String doUpdateContentTypes() throws Exception
    {
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);
    	
    	System.out.println("Synchronization method:" + this.synchronizationMethod);
    	//System.out.println("Fetching sync info from deploymentServerUrl:" + deploymentServerUrl);
    	
    	String targetEndpointAddress = deploymentServerUrl + "/services/RemoteDeploymentService";
    	//System.out.println("targetEndpointAddress:" + targetEndpointAddress);
    	
    	if(this.synchronizationMethod == null || this.synchronizationMethod.equalsIgnoreCase("pull"))
    	{
	    	Object[] contentTypeDefinitionVOArray = (Object[])invokeOperation(targetEndpointAddress, "getContentTypeDefinitions", "contentTypeDefinition", null, ContentTypeDefinitionVO.class, "infoglue");
	    	List remoteContentTypeDefinitionVOList = Arrays.asList(contentTypeDefinitionVOArray);
		    Collections.sort(remoteContentTypeDefinitionVOList, new ReflectionComparator("name"));
	
		    //System.out.println("remoteContentTypeDefinitionVOList:" + remoteContentTypeDefinitionVOList.size());
	
	    	String[] missingContentTypeNameArray = this.getRequest().getParameterValues("missingContentTypeName");
	    	//System.out.println("missingContentTypeNameArray:" + missingContentTypeNameArray);
	    	
	    	if(missingContentTypeNameArray != null)
	    	{
		    	for(int i=0; i<missingContentTypeNameArray.length; i++)
		    	{
		    		String missingContentTypeName = missingContentTypeNameArray[i];
		    		//System.out.println("Updating missingContentTypeName:" + missingContentTypeName);
		
		        	Iterator remoteContentTypeDefinitionVOListIterator = remoteContentTypeDefinitionVOList.iterator();
		        	while(remoteContentTypeDefinitionVOListIterator.hasNext())
		        	{
		        		ContentTypeDefinitionVO remoteContentTypeDefinitionVO = (ContentTypeDefinitionVO)remoteContentTypeDefinitionVOListIterator.next();
		        		//System.out.println("remoteContentTypeDefinitionVO:" + remoteContentTypeDefinitionVO.getName());
		        		if(remoteContentTypeDefinitionVO.getName().equals(missingContentTypeName))
		        		{
		        			ContentTypeDefinitionController.getController().create(remoteContentTypeDefinitionVO);
		        		}
		        	}
		    	}
	    	}
    	
	    	String[] deviatingContentTypeNameArray = this.getRequest().getParameterValues("deviatedContentTypeName");
	    	//System.out.println("deviatingContentTypeNameArray:" + deviatingContentTypeNameArray);
	    	
	    	if(deviatingContentTypeNameArray != null)
	    	{
		    	for(int i=0; i<deviatingContentTypeNameArray.length; i++)
		    	{
		    		String deviatingContentTypeName = deviatingContentTypeNameArray[i];
		    		//System.out.println("Updating deviatingContentTypeName:" + deviatingContentTypeName);
		
		        	Iterator remoteContentTypeDefinitionVOListIterator = remoteContentTypeDefinitionVOList.iterator();
		        	while(remoteContentTypeDefinitionVOListIterator.hasNext())
		        	{
		        		ContentTypeDefinitionVO remoteContentTypeDefinitionVO = (ContentTypeDefinitionVO)remoteContentTypeDefinitionVOListIterator.next();
		        		//System.out.println("remoteContentTypeDefinitionVO:" + remoteContentTypeDefinitionVO.getName());
		        		if(remoteContentTypeDefinitionVO.getName().equals(deviatingContentTypeName))
		        		{
		        			ContentTypeDefinitionVO localContentTypeDefinitionVO = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(remoteContentTypeDefinitionVO.getName());
		        			String newSchemaValue = localContentTypeDefinitionVO.getSchemaValue();
		        			
		        	    	String[] attributeNameArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_attributeName");
		        	    	//System.out.println("attributeNameArray:" + attributeNameArray);
		        	    	if(attributeNameArray != null)
		        	    	{
		        	    		for(int j=0; j<attributeNameArray.length; j++)
		        	    		{
			        	    		String attributeName = attributeNameArray[j];
			        	    		//System.out.println("  * Updating attributeName:" + attributeName);
			        			
				        			newSchemaValue = contentTypeDefinitionController.copyAttribute(remoteContentTypeDefinitionVO.getSchemaValue(), newSchemaValue, attributeName);
				        		}
		        	    	}	        			
	
		        	    	String[] categoryNameArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_categoryName");
		        	    	//System.out.println("categoryNameArray:" + categoryNameArray);
		        	    	if(categoryNameArray != null)
		        	    	{
		        	    		for(int j=0; j<categoryNameArray.length; j++)
		        	    		{
			        	    		String categoryName = categoryNameArray[j];
			        	    		//System.out.println("  * Updating categoryName:" + categoryName);
			        			
				        			newSchemaValue = contentTypeDefinitionController.copyCategory(remoteContentTypeDefinitionVO.getSchemaValue(), newSchemaValue, categoryName);
				        		}
		        	    	}	
	
		        	    	String[] assetKeyArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_assetKey");
		        	    	//System.out.println("assetKeyArray:" + assetKeyArray);
		        	    	if(assetKeyArray != null)
		        	    	{
		        	    		for(int j=0; j<assetKeyArray.length; j++)
		        	    		{
			        	    		String assetKey = assetKeyArray[j];
			        	    		//System.out.println("  * Updating assetKey:" + assetKey);
			        			
				        			newSchemaValue = contentTypeDefinitionController.copyAssetKey(remoteContentTypeDefinitionVO.getSchemaValue(), newSchemaValue, assetKey);
				        		}
		        	    	}
		        	    	
		        			localContentTypeDefinitionVO.setSchemaValue(newSchemaValue);
				        	ContentTypeDefinitionController.getController().update(localContentTypeDefinitionVO);
		        		}
		        	}
		    	}
	    	}
    	}
    	else
    	{
	    	Map input = new HashMap();

    		List contentTypeDefinitionVOList = new ArrayList();
    		String[] missingContentTypeNameArray = this.getRequest().getParameterValues("missingContentTypeName");
	    	//System.out.println("missingContentTypeNameArray:" + missingContentTypeNameArray);
	    	
	    	if(missingContentTypeNameArray != null)
	    	{
		    	for(int i=0; i<missingContentTypeNameArray.length; i++)
		    	{
		    		String missingContentTypeName = missingContentTypeNameArray[i];
		    		System.out.println("Updating missingContentTypeName:" + missingContentTypeName);
		    		ContentTypeDefinitionVO localContentTypeDefinitionVO = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(missingContentTypeName);
		    		System.out.println("Updating localContentTypeDefinitionVO:" + localContentTypeDefinitionVO);
		    		contentTypeDefinitionVOList.add(localContentTypeDefinitionVO);
		    	}
	    	}
    	
	    	String[] deviatingContentTypeNameArray = this.getRequest().getParameterValues("deviatedContentTypeName");
	    	//System.out.println("deviatingContentTypeNameArray:" + deviatingContentTypeNameArray);
	    	
	    	if(deviatingContentTypeNameArray != null)
	    	{
		    	for(int i=0; i<deviatingContentTypeNameArray.length; i++)
		    	{
		    		String deviatingContentTypeName = deviatingContentTypeNameArray[i];
		    		System.out.println("Updating deviatingContentTypeName:" + deviatingContentTypeName);
		    		ContentTypeDefinitionVO localContentTypeDefinitionVO = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(deviatingContentTypeName);
		    		System.out.println("Updating localContentTypeDefinitionVO as it was different:" + localContentTypeDefinitionVO);
		    		contentTypeDefinitionVOList.add(localContentTypeDefinitionVO);

		    		Map deviationArguments = new HashMap();

		    		List attributes = new ArrayList();
        	    	String[] attributeNameArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_attributeName");
        	    	if(attributeNameArray != null)
        	    	{
        	    		for(int j=0; j<attributeNameArray.length; j++)
        	    		{
	        	    		String attributeName = attributeNameArray[j];
	        	    		//System.out.println("  * Updating attributeName:" + attributeName);
	        	    		attributes.add(attributeName);
        	    		}
        	    	}	        			
        	    	
		    		List categories = new ArrayList();
        	    	String[] categoryNameArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_categoryName");
        	    	//System.out.println("categoryNameArray:" + categoryNameArray);
        	    	if(categoryNameArray != null)
        	    	{
        	    		for(int j=0; j<categoryNameArray.length; j++)
        	    		{
	        	    		String categoryName = categoryNameArray[j];
	        	    		//System.out.println("  * Updating categoryName:" + categoryName);
	        	    		categories.add(categoryName);
		        		}
        	    	}	

		    		List assets = new ArrayList();
        	    	String[] assetKeyArray = this.getRequest().getParameterValues(deviatingContentTypeName + "_assetKey");
        	    	//System.out.println("assetKeyArray:" + assetKeyArray);
        	    	if(assetKeyArray != null)
        	    	{
        	    		for(int j=0; j<assetKeyArray.length; j++)
        	    		{
	        	    		String assetKey = assetKeyArray[j];
	        	    		//System.out.println("  * Updating assetKey:" + assetKey);
	        	    		assets.add(assetKey);
		        		}
        	    	}

        	    	deviationArguments.put("attributes", attributes);
        	    	deviationArguments.put("categories", categories);
        	    	deviationArguments.put("assets", assets);
        	    	
        	    	input.put("deviationArguments_" + deviatingContentTypeName, deviationArguments);
		    	}
	    	}
	    	
	    	System.out.println("contentTypeDefinitionVOList to send:" + contentTypeDefinitionVOList.size());
	    	
	    	input.put("contentTypeDefinitionVOList", contentTypeDefinitionVOList);
	    	
	    	if(missingContentTypeNameArray != null)
	    	{
	    		List missingContentTypeNameList = new ArrayList();
	    		missingContentTypeNameList.addAll(Arrays.asList(missingContentTypeNameArray));
	    		input.put("missingContentTypeNameArray", missingContentTypeNameList);
	    	}
	    	if(deviatingContentTypeNameArray != null)
	    	{
	    		List deviatingContentTypeNameList = new ArrayList();
	    		deviatingContentTypeNameList.addAll(Arrays.asList(deviatingContentTypeNameArray));
	    		input.put("deviatingContentTypeNameArray", deviatingContentTypeNameList);
	    	}
	    	
	    	Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateContentTypeDefinitions", "contentTypeDefinition", input, Boolean.class, "java", ContentTypeDefinitionVO.class);	    	
	    	//Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateContentTypeDefinitions", "contentTypeDefinition", contentTypeDefinitionVOList, Boolean.class, "java", ContentTypeDefinitionVO.class);	    	
	    	//Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateContentTypeDefinitions", "contentTypeDefinition", contentTypeDefinitionVOList.get(0), Boolean.class, "java");	    	
    	}

    	return doInput();
    }

    public String doUpdateCategories() throws Exception
    {
    	//System.out.println("*****************************");
    	//System.out.println("*    UPDATING CATEGORIES    *");
    	//System.out.println("*****************************");
    	
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);
    	
    	//System.out.println("Fetching sync info from deploymentServerUrl:" + deploymentServerUrl);
    	
    	String targetEndpointAddress = deploymentServerUrl + "/services/RemoteDeploymentService";
    	//System.out.println("targetEndpointAddress:" + targetEndpointAddress);
    	
    	if(this.synchronizationMethod == null || this.synchronizationMethod.equalsIgnoreCase("pull"))
    	{
	    	Object[] categoryVOArray = (Object[])invokeOperation(targetEndpointAddress, "getAllActiveCategories", "category", null, CategoryVO.class, "infoglue");
	    	List remoteCategoryVOList = Arrays.asList(categoryVOArray);
		    Collections.sort(remoteCategoryVOList, new ReflectionComparator("name"));
		    //System.out.println("remoteCategoryVOList:" + remoteCategoryVOList.size());
	    	
		    List<CategoryVO> allLocalCategories = CategoryController.getController().findAllActiveCategories();
		    //System.out.println("allLocalCategories:" + allLocalCategories.size());
	    	
		    Map handledRemoteCategoryPaths = new HashMap();
		    
		    Map requestMap = HttpUtilities.requestToHashtable(getRequest());
		    
		    categoryController.compareAndCompleteCategoryLists(remoteCategoryVOList, allLocalCategories, null, handledRemoteCategoryPaths, requestMap);
		    //System.out.println("deviatingCategoryVOList:" + deviatingCategoryVOList.size());
    	}
    	else
    	{
	    	Map input = new HashMap();

	    	Map requestMap = HttpUtilities.requestToHashtable(getRequest());
	    	
		    List<CategoryVO> allLocalCategories = CategoryController.getController().findAllActiveCategories();
	    	input.put("categoryVOList", allLocalCategories);
	    	input.put("requestMap", requestMap);
	    	
	    	Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateCategories", "category", input, Boolean.class, "java", CategoryVO.class);	    	    		
    	}
    	
	    return doInput();
    }

    public String doUpdateWorkflows() throws Exception
    {
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);
    	
    	String targetEndpointAddress = deploymentServerUrl + "/services/RemoteDeploymentService";
    	
    	if(this.synchronizationMethod == null || this.synchronizationMethod.equalsIgnoreCase("pull"))
    	{
	    	Object[] workflowDefinitionVOArray = (Object[])invokeOperation(targetEndpointAddress, "getWorkflowDefinitions", "workflowDefinition", null, WorkflowDefinitionVO.class, "infoglue");
	    	List remoteWorkflowDefinitionVOList = Arrays.asList(workflowDefinitionVOArray);
		    Collections.sort(remoteWorkflowDefinitionVOList, new ReflectionComparator("name"));
	
		    //System.out.println("remoteWorkflowDefinitionVOList:" + remoteWorkflowDefinitionVOList.size());
	
	    	String[] missingWorkflowDefinitionNameArray = this.getRequest().getParameterValues("missingWorkflowDefinitionName");
	    	//System.out.println("missingWorkflowDefinitionNameArray:" + missingWorkflowDefinitionNameArray);
	    	
	    	if(missingWorkflowDefinitionNameArray != null)
	    	{
		    	for(int i=0; i<missingWorkflowDefinitionNameArray.length; i++)
		    	{
		    		String missingWorkflowDefinitionName = missingWorkflowDefinitionNameArray[i];
		    		//System.out.println("Updating missingWorkflowDefinitionName:" + missingWorkflowDefinitionName);
		
		        	Iterator remoteWorkflowDefinitionVOListIterator = remoteWorkflowDefinitionVOList.iterator();
		        	while(remoteWorkflowDefinitionVOListIterator.hasNext())
		        	{
		        		WorkflowDefinitionVO remoteWorkflowDefinitionVO = (WorkflowDefinitionVO)remoteWorkflowDefinitionVOListIterator.next();
		        		//System.out.println("remoteContentTypeDefinitionVO:" + remoteContentTypeDefinitionVO.getName());
		        		if(remoteWorkflowDefinitionVO.getName().equals(missingWorkflowDefinitionName))
		        		{
		        			WorkflowDefinitionController.getController().create(remoteWorkflowDefinitionVO);
		        		}
		        	}
		    	}
	    	}
    	}
    	else
    	{
	    	Map input = new HashMap();

	    	Map requestMap = HttpUtilities.requestToHashtable(getRequest());
	    	
		    List<WorkflowDefinitionVO> workflowDefinitionVOList = WorkflowDefinitionController.getController().getWorkflowDefinitionVOList();
	    	input.put("workflowDefinitionVOList", workflowDefinitionVOList);
	    	input.put("requestMap", requestMap);
	    	
	    	Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateWorkflows", "workflow", input, Boolean.class, "java", WorkflowDefinitionVO.class);	    	    		
    	}
    	
    	return doInput();
    }

    public String doUpdateComponents() throws Exception
    {
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);
    	
    	String targetEndpointAddress = deploymentServerUrl + "/services/RemoteDeploymentService";
    	//System.out.println("targetEndpointAddress:" + targetEndpointAddress);
    	
    	if(this.synchronizationMethod == null || this.synchronizationMethod.equalsIgnoreCase("pull"))
    	{
    		Object[] contentVOArray = (Object[])invokeOperation(targetEndpointAddress, "getComponents", "content", null, ContentVO.class, "infoglue");
	    	List remoteContentVOList = Arrays.asList(contentVOArray);
		    Collections.sort(remoteContentVOList, new ReflectionComparator("name"));
	
		    //System.out.println("remoteContentVOList:" + remoteContentVOList.size());
	
	    	String[] deviatingRemoteContentIdArray = this.getRequest().getParameterValues("deviatingContentId");
	    	//System.out.println("deviatingRemoteContentIdArray:" + deviatingRemoteContentIdArray);
	    	
	    	List components = ContentController.getContentController().getContentVOWithContentTypeDefinition("HTMLTemplate");
	    	
	    	if(deviatingRemoteContentIdArray != null)
	    	{
		    	for(int i=0; i<deviatingRemoteContentIdArray.length; i++)
		    	{
		    		String deviatingRemoteContentId = deviatingRemoteContentIdArray[i];
		    		
		    		//String deviatingContentName = deviatingRemoteContentIdArray[i];
		    		//System.out.println("Updating deviatingContentName:" + deviatingContentName);
		
		        	Iterator remoteContentVOListIterator = remoteContentVOList.iterator();
		        	while(remoteContentVOListIterator.hasNext())
		        	{
		        		ContentVO remoteContentVO = (ContentVO)remoteContentVOListIterator.next();
		        		//System.out.println("remoteContentVO:" + remoteContentVO.getName());
		        		if(remoteContentVO.getId().equals(deviatingRemoteContentId))
		        		{
		        			String[] versionValues = remoteContentVO.getVersions();
		        			if(versionValues != null && versionValues.length > 0)
		        			{
			        			String remoteVersionValue = versionValues[0];
	
		        				Iterator componentsIterator = components.iterator();
		        	    		ContentVO localContentVO = null;
		        	    		while(componentsIterator.hasNext())
		        	    		{
		        	    			ContentVO candidate = (ContentVO)componentsIterator.next();
		        	    			if(candidate.getName().equals(remoteContentVO.getName()))
		        	    			{
		        	    				localContentVO = candidate;
		        	    			}
		        	    		}
		        	    		
		    					LanguageVO languageVO = LanguageController.getController().getMasterLanguage(localContentVO.getRepositoryId());
		    					ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(localContentVO.getId(), languageVO.getId());
		    					if(contentVersionVO != null)
		    					{
		    						contentVersionVO.setVersionValue(remoteVersionValue);
		    						//System.out.println("Updating :" + localContentVO.getName() + " with new latest versionValue");
		    						ContentVersionController.getContentVersionController().update(contentVersionVO.getId(), contentVersionVO);
		    					}
		        			}
		        		}
		        	}
		    	}
	    	}
    	}
    	else
    	{
    		System.out.println("Updating components with push....");

	    	Map input = new HashMap();

	    	String[] missingLocalContentIdArray = this.getRequest().getParameterValues("missingContentId");
	    	System.out.println("missingLocalContentIdArray:" + missingLocalContentIdArray);
	    	
	    	List missingComponents = new ArrayList();
	    	if(missingLocalContentIdArray != null)
	    	{
		    	for(int i=0; i<missingLocalContentIdArray.length; i++)
		    	{
		    		String missingLocalContentId = missingLocalContentIdArray[i];
		    		ContentVO contentVO = ContentController.getContentController().getContentVOWithId(new Integer(missingLocalContentId).intValue());
		    		if(contentVO != null)
		    		{
						LanguageVO languageVO = LanguageController.getController().getMasterLanguage(contentVO.getRepositoryId());
						
						String fullPath = ContentController.getContentController().getContentPath(contentVO.getId(), true, true);
						
						ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), languageVO.getId());
						if(contentVersionVO != null)
							contentVO.setVersions(new String[]{contentVersionVO.getVersionValue()});
						
						contentVO.setFullPath(fullPath);

						missingComponents.add(contentVO);
		    		}
		    	}
	    	}

	    	String[] deviatingLocalContentIdArray = this.getRequest().getParameterValues("deviatingContentId");
	    	System.out.println("deviatingLocalContentIdArray:" + deviatingLocalContentIdArray);
	    	
	    	List deviatingComponents = new ArrayList();
	    	if(deviatingLocalContentIdArray != null)
	    	{
		    	for(int i=0; i<deviatingLocalContentIdArray.length; i++)
		    	{
		    		String deviatingLocalContentId = deviatingLocalContentIdArray[i];
		    		ContentVO contentVO = ContentController.getContentController().getContentVOWithId(new Integer(deviatingLocalContentId).intValue());
		    		if(contentVO != null)
		    		{
						LanguageVO languageVO = LanguageController.getController().getMasterLanguage(contentVO.getRepositoryId());
						
						String fullPath = ContentController.getContentController().getContentPath(contentVO.getId(), true, true);
						
						ContentVersionVO contentVersionVO = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(contentVO.getId(), languageVO.getId());
						if(contentVersionVO != null)
							contentVO.setVersions(new String[]{contentVersionVO.getVersionValue()});
						
						contentVO.setFullPath(fullPath);

						deviatingComponents.add(contentVO);
		    		}
		    	}
	    	}

	    	input.put("missingComponents", missingComponents);
	    	input.put("deviatingComponents", deviatingComponents);
	    	//input.put("requestMap", requestMap);
	    	
	    	Boolean success = (Boolean)invokeOperation(targetEndpointAddress, "updateComponents", "content", input, Boolean.class, "java", ContentVO.class);	    	    		
    	}
    	
    	return doInput();
    }

    public String doExecute() throws Exception
    {
    	List<String> deploymentServers = CmsPropertyHandler.getDeploymentServers();
    	String deploymentServerUrl = deploymentServers.get(deploymentServerIndex);

    	//System.out.println("Synchronizing with deploymentServerUrl:" + deploymentServerUrl);

    	return "success";
    }

	public void setDeploymentServerIndex(Integer deploymentServerIndex)
	{
		this.deploymentServerIndex = deploymentServerIndex;
	}

	/*
	public List<DeploymentCompareBean> getDeviatingCategories()
	{
		return deviatingCategories;
	}
	*/

	public List<DeploymentCompareBean> getDeviatingContents()
	{
		return deviatingContents;
	}

	public List<DeploymentCompareBean> getDeviatingContentTypes()
	{
		return deviatingContentTypes;
	}

	public List<DeploymentCompareBean> getDeviatingSiteNodes()
	{
		return deviatingSiteNodes;
	}

	public List<DeploymentCompareBean> getDeviatingWorkflows()
	{
		return deviatingWorkflows;
	}

	public List<DeploymentCompareBean> getDeviatingCategoryVOList()
	{
		return deviatingCategoryVOList;
	}

	public List getDeviatingAttributes(String remoteSchemaValue, String localSchemaValue)
	{
		List deviatingAttributes = new ArrayList();
		
		List remoteAttributes = ContentTypeDefinitionController.getController().getContentTypeAttributes(remoteSchemaValue);
		List localAttributes = ContentTypeDefinitionController.getController().getContentTypeAttributes(localSchemaValue);

		Iterator remoteAttributesIterator = remoteAttributes.iterator();
		while(remoteAttributesIterator.hasNext())
		{
			ContentTypeAttribute conentTypeAttribute = (ContentTypeAttribute)remoteAttributesIterator.next();
			//System.out.println("conentTypeAttribute:" + conentTypeAttribute.getName());
			Iterator localAttributesIterator = localAttributes.iterator();
			boolean attributeExisted = false;
			while(localAttributesIterator.hasNext())
			{
				ContentTypeAttribute localConentTypeAttribute = (ContentTypeAttribute)localAttributesIterator.next();
				if(localConentTypeAttribute.getName().equals(conentTypeAttribute.getName()))
					attributeExisted = true;
			}
			if(!attributeExisted)
				deviatingAttributes.add(conentTypeAttribute);
		}
		
		return deviatingAttributes;
	}

	public List getDeviatingAssetKeys(String remoteSchemaValue, String localSchemaValue) throws Exception
	{
		List deviatingAssetKeys = new ArrayList();
		
		List remoteAssetKeys = ContentTypeDefinitionController.getController().getDefinedAssetKeys(remoteSchemaValue);
		List localAssetKeys = ContentTypeDefinitionController.getController().getDefinedAssetKeys(localSchemaValue);

		Iterator assetsIterator = remoteAssetKeys.iterator();
		while(assetsIterator.hasNext())
		{
			AssetKeyDefinition assetKeyDefinition = (AssetKeyDefinition)assetsIterator.next();
			//System.out.println("assetKeyDefinition:" + assetKeyDefinition.getAssetKey());
			
			Iterator localAssetKeysIterator = localAssetKeys.iterator();
			boolean assetKeyExisted = false;
			while(localAssetKeysIterator.hasNext())
			{
				AssetKeyDefinition localAssetKeyDefinition = (AssetKeyDefinition)localAssetKeysIterator.next();
				//System.out.println("localAssetKeyDefinition:" + localAssetKeyDefinition.getAssetKey());
				if(localAssetKeyDefinition.getAssetKey().equals(localAssetKeyDefinition.getAssetKey()))
					assetKeyExisted = true;
			}
			
			//System.out.println("assetKeyExisted:" + assetKeyExisted);
			if(!assetKeyExisted)
				deviatingAssetKeys.add(assetKeyDefinition);
		}
		
		return deviatingAssetKeys;
	}

	public List getDeviatingCategories(String remoteSchemaValue, String localSchemaValue) throws Exception
	{
		List deviatingCategories = new ArrayList();
		
		List remoteCategoryKeys = getDefinedCategoryKeys(remoteSchemaValue);
		List localCategoryKeys = getDefinedCategoryKeys(localSchemaValue);

		Iterator categoriesIterator = remoteCategoryKeys.iterator();
		while(categoriesIterator.hasNext())
		{
			CategoryAttribute categoryAttribute = (CategoryAttribute)categoriesIterator.next();
			//System.out.println("categoryAttribute:" + categoryAttribute.getCategoryName());
			
			Iterator localCategoriesIterator = localCategoryKeys.iterator();
			boolean categoryExisted = false;
			while(localCategoriesIterator.hasNext())
			{
				CategoryAttribute localCategoryAttribute = (CategoryAttribute)localCategoriesIterator.next();
				if(localCategoryAttribute.getCategoryName().equals(categoryAttribute.getCategoryName()))
					categoryExisted = true;
			}
			//System.out.println("categoryExisted:" + categoryExisted);
			
			if(!categoryExisted)
				deviatingCategories.add(categoryAttribute);
		}
		
		return deviatingCategories;
	}

	/**
	 * Gets the list of defined categoryKeys, also populate the category name for the UI.
	 */
	
	public List getDefinedCategoryKeys(String schemaValue) throws Exception
	{
		List categoryKeys = ContentTypeDefinitionController.getController().getDefinedCategoryKeys(schemaValue);
		for (Iterator iter = categoryKeys.iterator(); iter.hasNext();)
		{
			CategoryAttribute info = (CategoryAttribute) iter.next();
			if(info.getCategoryId() != null)
				info.setCategoryName(getCategoryName(info.getCategoryId()));
			else
				info.setCategoryName("Undefined");
		}
		
		return categoryKeys;
	}

	/**
	 * Return the Category name, if we cannot find the category name (id not an int, bad id, etc)
	 * then do not barf, but return a user friendly name. This can happen if someone removes a
	 * category that is references by a content type definition.
	 */
	public String getCategoryName(Integer id)
	{
		try
		{
			return CategoryController.getController().findById(id).getName();
		}
		catch(SystemException e)
		{
			return "Category not found";
		}
	}
	

	protected Object invokeOperation(String endpointAddress, String operationName, String name, Object argument, Class returnType, String nameSpace) throws JspException
    {
		return invokeOperation(endpointAddress, operationName, name, argument, returnType, nameSpace, null);
    }
	
	protected Object invokeOperation(String endpointAddress, String operationName, String name, Object argument, Class returnType, String nameSpace, Class extraClassInfo) throws JspException
    {
		Object result = null;
		
        try
        {
        	InfoGluePrincipal principal = this.getInfoGluePrincipal();

            final DynamicWebservice ws = new DynamicWebservice(principal);

            ws.setTargetEndpointAddress(endpointAddress);
            ws.setOperationName(operationName);
            //ws.setReturnType(ContentVersionVO.class, new QName(nameSpace, ws.getClassName(ContentVersionVO.class)));
            ws.setReturnType(returnType, new QName(nameSpace, ws.getClassName(returnType)));
            
            if(argument != null)
            {
	            if(argument instanceof Map || argument instanceof HashMap)
	                ws.addArgument(name, (Map)argument, extraClassInfo);
	            else if(argument instanceof List || argument instanceof ArrayList)
	                ws.addArgument(name, (List)argument, extraClassInfo);
	            else
	                ws.addArgument(name, argument);
            }
            
            ws.callService();
            result = ws.getResult();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new JspTagException(e.getMessage());
        }
        
        return result;
    }

	public String getCategoryPath(Integer categoryId) throws SystemException
	{
		return CategoryController.getController().getCategoryPath(categoryId);
	}

	public void setSynchronizeCategories(boolean synchronizeCategories)
	{
		this.synchronizeCategories = synchronizeCategories;
	}

	public void setSynchronizeContentTypeDefinitions(boolean synchronizeContentTypeDefinitions)
	{
		this.synchronizeContentTypeDefinitions = synchronizeContentTypeDefinitions;
	}

	public void setSynchronizeWorkflows(boolean synchronizeWorkflows)
	{
		this.synchronizeWorkflows = synchronizeWorkflows;
	}

	public boolean getSynchronizeCategories()
	{
		return this.synchronizeCategories;
	}

	public boolean getSynchronizeContentTypeDefinitions()
	{
		return this.synchronizeContentTypeDefinitions;
	}

	public boolean getSynchronizeWorkflows()
	{
		return this.synchronizeWorkflows;
	}

	public void setSynchronizeComponents(boolean synchronizeComponents)
	{
		this.synchronizeComponents = synchronizeComponents;
	}

	public boolean getSynchronizeComponents()
	{
		return this.synchronizeComponents;
	}

	public String getSynchronizationMethod()
	{
		return synchronizationMethod;
	}

	public void setSynchronizationMethod(String synchronizationMethod)
	{
		this.synchronizationMethod = synchronizationMethod;
	}

}
