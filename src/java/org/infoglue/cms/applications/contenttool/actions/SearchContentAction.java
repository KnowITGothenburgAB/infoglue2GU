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

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.VisualFormatter;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SearchController;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.content.DigitalAssetVO;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.util.Timer;

import webwork.action.Action;


/**
 * Action class for usecase SearchContentAction. Was better before but due to wanted support for multiple 
 * databases and lack of time I had to cut down on functionality - sorry Magnus. 
 *
 * @author Magnus Guvenal
 * @author Mattias Bogeblad
 */

public class SearchContentAction extends InfoGlueAbstractAction 
{
	private SearchController searchController = new SearchController();
	
    private final static Logger logger = Logger.getLogger(SearchContentAction.class.getName());

	private static final long serialVersionUID = 1L;
	
	private List contentVersionVOList = new ArrayList();
	private List<BaseEntityVO> baseEntityVOList = new ArrayList<BaseEntityVO>();
	private Set contentVOSet;
	private List<DigitalAssetVO> digitalAssetVOList = null;
	private Integer repositoryId;
	private String searchString;
	private String name;
	private Integer languageId;
	//private Integer contentTypeDefinitionId;
	private Integer caseSensitive;
	private boolean includeAssets = false;
	private boolean onlyIDSearch = false;
	private Integer inverseSearch;
	private Integer stateId;
	private boolean advancedEnabled = false;
	private List selectedRepositoryIdList = new ArrayList();
	private List selectedContentTypeDefinitionIdList = new ArrayList();
	private String[] allowedContentTypeIds = null;
	private Date modifiedDateTimeStart = null;
	private Date modifiedDateTimeEnd = null;
	
	private int maxRows = 0;
	
	//This is for advanced search
	private List principals 			= null;
	private List availableLanguages 	= null;
	private List contentTypeDefinitions = null;
	private List repositories 			= null;
	
	//This is for replace
	private String replaceString		= null;
	//private String[] contentVersionId  	= null;
	
	private String assetTypeFilter = "*";
	private boolean allowCaseSensitive = true;
	
	private boolean wasQuickSearch = false;
	
	public boolean getWasQuickSearch()
	{
		return wasQuickSearch;
	}
	
	public boolean getAllowCaseSensitive()
	{
		return allowCaseSensitive;
	}

	public void setSearchString(String s)
	{
	    this.searchString = s;
		//this.searchString = s.replaceAll("'","");
	}
	
	public String getSearchString()
	{
		return this.searchString;	
	}
	
	public void setMaxRows(int r)
	{
		this.maxRows = r;	
	}
	
	public int getMaxRows()
	{
		if(maxRows == 0)
		    maxRows=100;
		
		return this.maxRows;	
	}

	public List getContentVersionVOList()
	{
		return this.contentVersionVOList;
	}
	
	public String doExecute() throws Exception 
	{
		Timer t = new Timer();
	    int maxRows = 100;
		try
		{
			maxRows = Integer.parseInt(CmsPropertyHandler.getMaxRows());
		}
		catch(Exception e)
		{
		}

		String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
		String[] contentTypeDefinitionId = this.getRequest().getParameterValues("contentTypeDefinitionId");
		if(onlyIDSearch)
		{
			try
			{
				this.baseEntityVOList = searchController.getBaseEntityVOListFromCastor(new Integer(this.getSearchString()));				
			}
			catch (Exception e) 
			{
				logger.warn("Not a valid id:" + e.getMessage());
			}
		}
		else
		{
			Integer[] repositoryIdAsIntegerToSearch = null;
			if(repositoryIdToSearch != null)
			{
				repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
				for(int i=0; i < repositoryIdToSearch.length; i++)
				{
					repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
					selectedRepositoryIdList.add(repositoryIdToSearch[i]);
				}
			}
			else
			{
				repositoryIdAsIntegerToSearch = new Integer[1];
				repositoryIdAsIntegerToSearch[0] = new Integer(repositoryId);
				selectedRepositoryIdList.add(repositoryId);
			}
			
			Integer[] contentTypeDefinitionIdAsInteger = null;
			if(contentTypeDefinitionId != null)
			{
				contentTypeDefinitionIdAsInteger = new Integer[contentTypeDefinitionId.length];
				for(int i=0; i < contentTypeDefinitionId.length; i++)
				{
					contentTypeDefinitionIdAsInteger[i] = new Integer(contentTypeDefinitionId[i]);
					selectedContentTypeDefinitionIdList.add(contentTypeDefinitionId[i]);
				}
			}
			else
			{
				wasQuickSearch = true;
				//Defaulting to limited search...
				logger.info("LIMITING SEARCH ON CONTENT TYPES");
				//String includedContentTypes = "Artikel,GUNyhet,Miniartikel,GU Kontakt information"; //CmsPropertyHandler.getFastSearchIncludedContentTypes();
				String includedContentTypes = CmsPropertyHandler.getFastSearchIncludedContentTypes();
				if(includedContentTypes != null && includedContentTypes.equals(""))
				{
					String[] contentTypes = includedContentTypes.split(",");
					List<Integer> contentTypeDefinitionIds = new ArrayList<Integer>();
					for(String contentType : contentTypes)
					{
						try
						{
							ContentTypeDefinitionVO ctdVO = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOWithName(contentType);
							contentTypeDefinitionIds.add(ctdVO.getId());
						}
						catch (Exception e) 
						{
							logger.warn("Could not find content type:" + contentType + ". Message: " + e.getMessage());
						}
					}
					contentTypeDefinitionIdAsInteger = (Integer[])contentTypeDefinitionIds.toArray(new Integer[contentTypeDefinitionIds.size()]);
				}
			}

			if(modifiedDateTimeStart == null)
			{
				wasQuickSearch = true;
				logger.info("LIMITING SEARCH ON DATES");
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MONTH, -1);
				modifiedDateTimeStart = cal.getTime();
			}
			
			contentVersionVOList = searchController.getContentVersionVOList(repositoryIdAsIntegerToSearch, this.getSearchString(), maxRows, this.name, languageId, contentTypeDefinitionIdAsInteger, caseSensitive, stateId, includeAssets, modifiedDateTimeStart, modifiedDateTimeEnd);
		}
		
		if(CmsPropertyHandler.getInternalSearchEngine().equalsIgnoreCase("lucene"))
		{
			allowCaseSensitive = false;
		}

	    this.principals = new ArrayList(); //UserControllerProxy.getController().getAllUsers();
	    this.availableLanguages = LanguageController.getController().getLanguageVOList(this.repositoryId);
	    this.contentTypeDefinitions = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList();
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
		
		return "success";
	}


	public String doBindingResult() throws Exception 
	{
		Integer[] allowedContentTypeId = new Integer[0];
		if(allowedContentTypeIds != null && allowedContentTypeIds.length != 0)
		{
			allowedContentTypeId = new Integer[allowedContentTypeIds.length];
			for(int i=0; i<allowedContentTypeIds.length; i++)
				allowedContentTypeId[i] = new Integer(allowedContentTypeIds[i]);
		}
		
		int maxRows = 100;
		try
		{
			maxRows = Integer.parseInt(CmsPropertyHandler.getMaxRows());
		}
		catch(Exception e)
		{
		}

		String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
		if(repositoryIdToSearch != null)
		{
			Integer[] repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
			for(int i=0; i < repositoryIdToSearch.length; i++)
			{
				repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
				selectedRepositoryIdList.add(repositoryIdToSearch[i]);
			}
			
			contentVOSet = searchController.getContents(repositoryIdAsIntegerToSearch, this.getSearchString(), maxRows, name, languageId, allowedContentTypeId, caseSensitive, stateId);
		}
		else
		{
			contentVOSet = searchController.getContents(this.repositoryId, this.getSearchString(), maxRows, name, languageId, allowedContentTypeId, caseSensitive, stateId);
			selectedRepositoryIdList.add("" + this.repositoryId);
		}

		return "successBindingResult";
	}

	public String doInlineAssetResult() throws Exception 
	{
		Integer[] allowedContentTypeId = new Integer[0];
		if(allowedContentTypeIds != null && allowedContentTypeIds.length != 0)
		{
			allowedContentTypeId = new Integer[allowedContentTypeIds.length];
			for(int i=0; i<allowedContentTypeIds.length; i++)
				allowedContentTypeId[i] = new Integer(allowedContentTypeIds[i]);
		}
		
		int maxRows = 100;
		try
		{
			maxRows = Integer.parseInt(CmsPropertyHandler.getMaxRows());
		}
		catch(Exception e)
		{
		}

		String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
		if(repositoryIdToSearch != null)
		{
			Integer[] repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
			for(int i=0; i < repositoryIdToSearch.length; i++)
			{
				repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
				selectedRepositoryIdList.add(repositoryIdToSearch[i]);
			}
			
			contentVOSet = searchController.getContents(repositoryIdAsIntegerToSearch, this.getSearchString(), maxRows, name, languageId, allowedContentTypeId, caseSensitive, stateId, true);
		}
		else 
		{
			contentVOSet = searchController.getContents(this.repositoryId, this.getSearchString(), maxRows, name, languageId, allowedContentTypeId, caseSensitive, stateId, true);
			selectedRepositoryIdList.add("" + this.repositoryId);
		}

		return "successInlineAssetResult";
	}

	public String doInlineAssetSearchV3() throws Exception 
	{
		Timer t = new Timer();
		
		int maxRows = 100;
		/*
		try
		{
			maxRows = Integer.parseInt(CmsPropertyHandler.getMaxRows());
		}
		catch(Exception e)
		{
		}
		*/
		String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
		if(repositoryIdToSearch != null)
		{
			Integer[] repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
			for(int i=0; i < repositoryIdToSearch.length; i++)
			{
				repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
				selectedRepositoryIdList.add(repositoryIdToSearch[i]);
			}
			
			digitalAssetVOList = SearchController.getDigitalAssets(repositoryIdAsIntegerToSearch, this.getSearchString(), assetTypeFilter, maxRows);
		}
		else 
		{
			digitalAssetVOList = SearchController.getDigitalAssets(new Integer[]{this.repositoryId}, this.getSearchString(), assetTypeFilter, maxRows);
			selectedRepositoryIdList.add("" + this.repositoryId);
		}

		return "successInlineAssetSearchV3";
	}

	public String doLatestInlineAssetsV3() throws Exception 
	{
		Timer t = new Timer();
		int maxRows = 20;
		/*
		try
		{
			maxRows = Integer.parseInt(CmsPropertyHandler.getMaxRows());
			if(maxRows > 50)
				maxRows = 30;
		}
		catch(Exception e)
		{
		}
		*/
		String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
		if(repositoryIdToSearch != null)
		{
			Integer[] repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
			for(int i=0; i < repositoryIdToSearch.length; i++)
			{
				repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
				selectedRepositoryIdList.add(repositoryIdToSearch[i]);
			}
			
			digitalAssetVOList = SearchController.getLatestDigitalAssets(repositoryIdAsIntegerToSearch, assetTypeFilter, maxRows);
		}
		else 
		{
			digitalAssetVOList = SearchController.getLatestDigitalAssets(new Integer[]{this.repositoryId}, assetTypeFilter, maxRows);
			selectedRepositoryIdList.add("" + this.repositoryId);
		}

		return "successLatestInlineAssetsV3";
	}

	/**
	 * This method returns the advanced search interface to the user.
	 */

	public String doInput() throws Exception 
	{
		if(CmsPropertyHandler.getInternalSearchEngine().equalsIgnoreCase("lucene"))
		{
			includeAssets = true;
			allowCaseSensitive = false;
		}

	    this.principals = new ArrayList(); //UserControllerProxy.getController().getAllUsers();
	    this.availableLanguages = LanguageController.getController().getLanguageVOList(this.repositoryId);
	    this.contentTypeDefinitions = ContentTypeDefinitionController.getController().getContentTypeDefinitionVOList();
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
		selectedRepositoryIdList.add("" + this.repositoryId);
	    
	    return Action.INPUT;
	}

	/**
	 * This method returns the binding search interface to the user.
	 */

	public String doInputBinding() throws Exception 
	{
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
	    
	    return Action.INPUT + "Binding";
	}
	
	public String doSearchBindingV3() throws Exception 
	{
		if(this.modifiedDateTimeStart == null)
		{
			Calendar startCal = Calendar.getInstance();
			//System.out.println("startCal:" + startCal.getTime());
			startCal.add(Calendar.WEEK_OF_YEAR, -1);
			//System.out.println("startCal:" + startCal.getTime());
			this.modifiedDateTimeStart = startCal.getTime();
		}
		
		int maxRows = 5;
		if (this.maxRows <= 100 && this.maxRows > 0)
		{
			maxRows = this.maxRows;
		}
		else
		{
			this.maxRows = maxRows;
		}

		
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);

		if (getSearchString() != null)
		{
			//System.out.println("Will search:" + allowedContentTypeIds);
			Integer[] allowedContentTypeId = null;
			if(allowedContentTypeIds != null && allowedContentTypeIds.length != 0)
			{
				allowedContentTypeId = new Integer[allowedContentTypeIds.length];
				for(int i=0; i<allowedContentTypeIds.length; i++)
					allowedContentTypeId[i] = new Integer(allowedContentTypeIds[i]);
			}

			String[] repositoryIdToSearch = this.getRequest().getParameterValues("repositoryIdToSearch");
			Integer[] repositoryIdAsIntegerToSearch = null;
			if(repositoryIdToSearch != null)
			{
				repositoryIdAsIntegerToSearch = new Integer[repositoryIdToSearch.length];
				for(int i=0; i < repositoryIdToSearch.length; i++)
				{
					repositoryIdAsIntegerToSearch[i] = new Integer(repositoryIdToSearch[i]);
					selectedRepositoryIdList.add(repositoryIdToSearch[i]);
				}
			}
			contentVersionVOList = searchController.getContentVersionVOList(repositoryIdAsIntegerToSearch, this.getSearchString(), maxRows, name, languageId, allowedContentTypeId, caseSensitive, stateId, includeAssets, modifiedDateTimeStart, modifiedDateTimeEnd);
		}
		return "searchBindingV3";
	}

	/**
	 * This method returns the binding search interface to the user.
	 */

	public String doInputInlineAsset() throws Exception 
	{
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
	    
	    return Action.INPUT + "InlineAsset";
	}

	/**
	 * This method returns the binding search interface to the user.
	 */

	public String doInputInlineAssetV3() throws Exception 
	{
		this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
	    
	    return Action.INPUT + "InlineAssetV3";
	}

	public ContentVO getContentVO(Integer contentId)
	{
		ContentVO contentVO = null;
		
		try
		{
			if(contentId != null)
			{
				contentVO = ContentController.getContentController().getContentVOWithId(contentId);
			}
		}
		catch(Exception e)
		{
			logger.error("An error occurred when we tried to get the content for this version:" + e.getMessage(), e);
		}
		
		return contentVO;
	}

	public LanguageVO getLanguageVO(Integer languageId)
	{
		LanguageVO languageVO = null;
		
		try
		{
			if(languageId != null)
			{
				languageVO = LanguageController.getController().getLanguageVOWithId(languageId);
			}
		}
		catch(Exception e)
		{
		    logger.error("An error occurred when we tried to get the language for this version:" + e.getMessage(), e);
		}
		
		return languageVO;
	}

	public Integer getRepositoryId()
	{
		return repositoryId;
	}

	public void setRepositoryId(Integer integer)
	{
		repositoryId = integer;
	}

    public List getAvailableLanguages()
    {
        return availableLanguages;
    }
    
    public List getContentTypeDefinitions()
    {
        return contentTypeDefinitions;
    }
    
	public List getRepositories() 
	{
		return repositories;
	}

	/*
    public List getPrincipals()
    {
        return principals;
    }
 	*/
	
   /* 
    public String[] getContentVersionId()
    {
        return contentVersionId;
    }
    
    public void setContentVersionIds(String[] contentVersionId)
    {
        this.contentVersionId = contentVersionId;
    }
    */
    public String getReplaceString()
    {
        return replaceString;
    }
    
    public void setReplaceString(String replaceString)
    {
        this.replaceString = replaceString;
    }
    
    public Integer getCaseSensitive()
    {
        return caseSensitive;
    }
    
    public void setCaseSensitive(Integer caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
    
    /*
    public Integer getContentTypeDefinitionId()
    {
        return contentTypeDefinitionId;
    }
    
    public void setContentTypeDefinitionId(Integer contentTypeDefinitionId)
    {
        this.contentTypeDefinitionId = contentTypeDefinitionId;
    }
    */
    
    public Integer getInverseSearch()
    {
        return inverseSearch;
    }
    
    public void setInverseSearch(Integer inverseSearch)
    {
        this.inverseSearch = inverseSearch;
    }
    
    public Integer getLanguageId()
    {
        return languageId;
    }
    
    public void setLanguageId(Integer languageId)
    {
        this.languageId = languageId;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public Integer getStateId()
    {
        return stateId;
    }
    
    public void setStateId(Integer stateId)
    {
        this.stateId = stateId;
    }
    
    public boolean isAdvancedEnabled()
    {
        return advancedEnabled;
    }
    
    public void setAdvancedEnabled(boolean advancedEnabled)
    {
        this.advancedEnabled = advancedEnabled;
    }

    public void setModifiedDateTimeStart(String dateString)
    {
    	if(dateString != null && !dateString.equals(""))
    		this.modifiedDateTimeStart = new VisualFormatter().parseDate(dateString, "yyyy-MM-dd");
    }

    public void setModifiedDateTimeEnd(String dateString)
    {
    	if(dateString != null && !dateString.equals(""))
    		this.modifiedDateTimeEnd = new VisualFormatter().parseDate(dateString, "yyyy-MM-dd");
	}
    
    public String getModifiedDateTimeStart()
    {    		
        return new VisualFormatter().formatDate(this.modifiedDateTimeStart, "yyyy-MM-dd");
    }
        
    public String getModifiedDateTimeEnd()
    {
        return new VisualFormatter().formatDate(this.modifiedDateTimeEnd, "yyyy-MM-dd");
    }
    
	public List getSelectedRepositoryIdList() 
	{
		return selectedRepositoryIdList;
	}
	
	public List getSelectedContentTypeDefinitionIdList()
	{
		return this.selectedContentTypeDefinitionIdList;
	}


    public void setAllowedContentTypeIds(String[] allowedContentTypeIds)
    {
        this.allowedContentTypeIds = allowedContentTypeIds;
    }
    
    public String getAllowedContentTypeIdsAsUrlEncodedString() throws Exception
    {
        StringBuffer sb = new StringBuffer();
        
        for(int i=0; i<allowedContentTypeIds.length; i++)
        {
            if(i > 0)
                sb.append("&");
            
            sb.append("allowedContentTypeIds=" + URLEncoder.encode(allowedContentTypeIds[i], "UTF-8"));
        }

        return sb.toString();
    }

	public String[] getAllowedContentTypeIds()
	{
		return allowedContentTypeIds;
	}

	public Set getContentVOSet()
	{
		return contentVOSet;
	}

	public boolean getIncludeAssets()
	{
		return includeAssets;
	}

	public void setIncludeAssets(boolean includeAssets)
	{
		this.includeAssets = includeAssets;
	}

	public boolean getOnlyIDSearch()
	{
		return onlyIDSearch;
	}

	public void setOnlyIDSearch(boolean onlyIDSearch)
	{
		this.onlyIDSearch = onlyIDSearch;
	}

	public String getAssetTypeFilter()
	{
		return assetTypeFilter;
	}

	public void setAssetTypeFilter(String assetTypeFilter)
	{
		if(assetTypeFilter != null && !assetTypeFilter.equals(""))
			this.assetTypeFilter = assetTypeFilter;
	}
	
	public List<DigitalAssetVO> getDigitalAssetVOList()
	{
		return digitalAssetVOList;
	}

	public List<BaseEntityVO> getBaseEntityVOList()
	{
		return baseEntityVOList;
	}

}
