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
package org.infoglue.deliver.jobs;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.cache.PageCacheHelper;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.deliver.util.RecacheRepositoryRootPagesThread;
import org.infoglue.deliver.util.RequestAnalyser;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author mattias
 *
 * This jobs searches for expiring contents or sitenodes and clears caches if found.
 */

public class ExpireCacheJob implements Job
{
    private final static Logger logger = Logger.getLogger(ExpireCacheJob.class.getName());
    private static Integer intervalCount = 0;
    private static AtomicBoolean running = new AtomicBoolean(false);
    
    private static long lastCacheCheck = System.currentTimeMillis();
    private static long lastCacheCleanup = System.currentTimeMillis();
    private static long lastRecacheCall = System.currentTimeMillis();
    
    public synchronized void execute(JobExecutionContext context) throws JobExecutionException
    {
    	long diffLastCacheCheck = ((System.currentTimeMillis() - lastCacheCheck) / 1000);
		logger.info("diffLastCacheCheck " + diffLastCacheCheck + " in " + CmsPropertyHandler.getApplicationName() + " - " + Thread.currentThread().getId());
		if(running.compareAndSet(false, true))
		{
			//System.out.println("Should run.....");
		}
		else
		{
			//System.out.println("Returning as allready running thread");
			return;
		}
		
	    if(diffLastCacheCheck > 600)
		{
	        synchronized(RequestAnalyser.getRequestAnalyser()) 
		    {
		       	if(RequestAnalyser.getRequestAnalyser().getBlockRequests())
			    {
				    logger.warn("evictWaitingCache allready in progress - returning to avoid conflict");
				    running.set(false);
				    return;
			    }
	
		       	RequestAnalyser.getRequestAnalyser().setBlockRequests(true);
			}
	
			try
	        {
				logger.info("Validating caches in " + CmsPropertyHandler.getApplicationName() + " - " + Thread.currentThread().getId());
				RequestAnalyser.shortenPageStatistics();
		    	lastCacheCheck = System.currentTimeMillis();
	        }
	        catch(Exception e)
	        {
	            logger.error("An error occurred when we tried to validate caches:" + e.getMessage());
	            logger.warn("An error occurred when we tried to validate caches:" + e.getMessage(), e);
	        }
		    
		    logger.info("releasing block");
		    RequestAnalyser.getRequestAnalyser().setBlockRequests(false);
    	}
    	
	    
    	long diff = ((System.currentTimeMillis() - lastCacheCleanup) / 1000);
    	if(diff > 3600 && (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 23 || Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 1 || Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 2))
	    {
    		logger.info("Cleaning heavy caches so memory footprint is kept low:" + diff);
            /*
            synchronized(RequestAnalyser.getRequestAnalyser()) 
    	    {
    	       	if(RequestAnalyser.getRequestAnalyser().getBlockRequests())
    		    {
    			    logger.warn("evictWaitingCache allready in progress - returning to avoid conflict");
					running.set(false);
    		        return;
    		    }

    	       	RequestAnalyser.getRequestAnalyser().setBlockRequests(true);
    		}
            */
			try
            {
    			logger.warn("Flushing heavy caches..");
    			if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 23)
    			{
	    			CacheController.clearCache("contentVersionCache");
	    			CacheController.clearCache("contentVersionIdCache");
	    			CacheController.clearCache("componentPropertyCache");
	    			CacheController.clearCache("componentPropertyVersionIdCache");
    			}
    			if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 1)
    			{
		    		CacheController.clearCache("contentAttributeCache");
	    			//CacheController.clearCache("componentEditorCache");
	    			//CacheController.clearCache("componentEditorVersionIdCache");
    			}
    			/*
    			if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 2)
    			{
			    	CacheController.clearCache("pageCache");
	    			CacheController.clearCache("pageCacheExtra");
    			}
    			*/
    			logger.warn("Done flushing heavy caches..");
    		    lastCacheCleanup = System.currentTimeMillis();
            }
            catch(Exception e)
            {
                logger.error("An error occurred when we tried to clear caches:" + e.getMessage(), e);
            }
            /*
		    logger.info("releasing block");
		    RequestAnalyser.getRequestAnalyser().setBlockRequests(false);
		    */
    	}

        try
        {
            CacheController.evictWaitingCache();
        }
        catch (Exception e)
        {
            logger.error("An error occurred when we tried to update cache:" + e.getMessage());
            logger.warn("An error occurred when we tried to update cache:" + e.getMessage(), e);
        }    	

        logger.info("---" + context.getJobDetail().getFullName() + " executing.[" + new Date() + "]");

        try
        {
            Date firstExpireDateTime = CacheController.expireDateTime;
            logger.info("firstExpireDateTime:" + firstExpireDateTime);
            Date now = new Date();
            
            if(firstExpireDateTime != null && now.after(firstExpireDateTime))
            {
                logger.info("setting block");
                synchronized(RequestAnalyser.getRequestAnalyser()) 
        	    {
        	       	if(RequestAnalyser.getRequestAnalyser().getBlockRequests())
        		    {
        			    logger.warn("evictWaitingCache allready in progress - returning to avoid conflict");
        			    running.set(false);
        		        return;
        		    }

        	       	RequestAnalyser.getRequestAnalyser().setBlockRequests(true);
        		}

				try
                {
	        	    String operatingMode = CmsPropertyHandler.getOperatingMode();
	        	    if(operatingMode != null && operatingMode.equalsIgnoreCase("3"))
	        	    {
	        	        logger.info("Updating all caches as this was a publishing-update");
		    			CacheController.clearCastorCaches();
		
		    			logger.info("clearing all except page cache as we are in publish mode..");
		    		    CacheController.clearCaches(null, null, new String[] {"pageCache", "pageCacheExtra", "NavigationCache", "pagePathCache", "userCache", "pageCacheParentSiteNodeCache", "pageCacheLatestSiteNodeVersions", "pageCacheSiteNodeTypeDefinition"});
		    			
		    			logger.info("Recaching all caches as this was a publishing-update");
		    			CacheController.cacheCentralCastorCaches();
		    			
		    			logger.info("Finally clearing page cache as this was a publishing-update");
		    		    //CacheController.clearFileCaches("pageCache");
		    			PageCacheHelper.getInstance().clearPageCache();

		    		    CacheController.clearCache("pageCache");
		    		    CacheController.clearCache("pageCacheExtra");
	        	    }
	        	    else
	        	    {
		    		    logger.info("Updating all caches as this was a publishing-update");
		    			CacheController.clearCastorCaches();
		
		    			logger.info("clearing all except page cache as we are in publish mode..");
		    		    CacheController.clearCaches(null, null, null);

		    			PageCacheHelper.getInstance().clearPageCache();
		    		    //CacheController.clearFileCaches("pageCache");
	        	    }
                }
                catch(Exception e)
                {
                    logger.error("An error occurred when we tried to update cache:" + e.getMessage());
                    logger.warn("An error occurred when we tried to update cache:" + e.getMessage(), e);
                }
    		    
    		    logger.info("releasing block");
    		    RequestAnalyser.getRequestAnalyser().setBlockRequests(false);
            }

            Date firstPublishDateTime = CacheController.publishDateTime;
            logger.info("firstPublishDateTime:" + firstPublishDateTime);
            
            if(firstPublishDateTime != null && now.after(firstPublishDateTime))
            {
                logger.info("setting block");
                synchronized(RequestAnalyser.getRequestAnalyser()) 
        	    {
                	
        	       	if(RequestAnalyser.getRequestAnalyser().getBlockRequests())
        		    {
        			    logger.warn("evictWaitingCache allready in progress - returning to avoid conflict");
        			    running.set(false);
        		        return;
        		    }

        	       	RequestAnalyser.getRequestAnalyser().setBlockRequests(true);
        		}
                
                try
                {
	        	    String operatingMode = CmsPropertyHandler.getOperatingMode();
	        	    if(operatingMode != null && operatingMode.equalsIgnoreCase("3"))
	        	    {
	                    logger.info("Updating all caches as this was a publishing-update");
		    			CacheController.clearCastorCaches();
		
		    			logger.info("clearing all except page cache as we are in publish mode..");
		    		    CacheController.clearCaches(null, null, new String[] {"pageCache", "pageCacheExtra", "NavigationCache", "pagePathCache", "userCache", "pageCacheParentSiteNodeCache", "pageCacheLatestSiteNodeVersions", "pageCacheSiteNodeTypeDefinition"});
		    			
		    			logger.info("Recaching all caches as this was a publishing-update");
		    			CacheController.cacheCentralCastorCaches();
		    			
		    			logger.info("Finally clearing page cache as this was a publishing-update");
		    		    //CacheController.clearFileCaches("pageCache");
		    			PageCacheHelper.getInstance().clearPageCache();

		    		    CacheController.clearCache("pageCache");
		    		    CacheController.clearCache("pageCacheExtra");
	        	    }
	        	    else
	        	    {
		    		    logger.info("Updating all caches as this was a publishing-update");
		    			CacheController.clearCastorCaches();
		
		    			logger.info("clearing all except page cache as we are in publish mode..");
		    		    CacheController.clearCaches(null, null, null);
	        	    }
                }
                catch(Exception e)
                {
                    logger.error("An error occurred when we tried to update cache:" + e.getMessage());
                    logger.warn("An error occurred when we tried to update cache:" + e.getMessage(), e);
                }

                logger.info("releasing block");
                RequestAnalyser.getRequestAnalyser().setBlockRequests(false);
            }
            
            
            synchronized (intervalCount)
			{
                intervalCount++;
	            if(intervalCount > 200)
	            {
	                logger.error("Cleaning cache directory as intervalCount:" + intervalCount);
	                String dir = CmsPropertyHandler.getDigitalAssetPath() + File.separator + "caches";
	                File dirFile = new File(dir);
	                if(dirFile.exists())
	                {
		                File[] subCaches = dirFile.listFiles();
		                for(int i=0; i<subCaches.length; i++)
		                {
		                	File subCacheDir = subCaches[i];
		                	logger.info("subCacheDir:" + subCacheDir.getName());
	                		int targetDiff = 48;
	                		//if(subCacheDir.getName().equals("pageCache"))
	                		//	targetDiff = 6 + (int)(Math.random() * ((12 - 6) + 1));
	                		logger.warn("targetDiff:" + targetDiff);
	                		
		                	if(subCacheDir.isDirectory())
		                	{
		                    	File[] subSubCacheFiles = subCacheDir.listFiles();
		                    	for(int j=0; j<subSubCacheFiles.length; j++)
		                    	{
		                    		File subSubCacheDir = subSubCacheFiles[j];
		                        	if(subSubCacheDir.isDirectory())
		                        	{
		                            	File[] cacheFiles = subSubCacheDir.listFiles();
		                            	for(int k=0; k<cacheFiles.length; k++)
		                            	{
					                		File cacheFile = cacheFiles[k];
						                	logger.info("cacheFile:" + cacheFile.getName());
						                	long lastModified = cacheFile.lastModified();
					                		long differensInHours = (System.currentTimeMillis() - lastModified) / (60 * 60 * 1000);
					                		//System.out.println("differensInHours:" + differensInHours);
					                		if(differensInHours > targetDiff)
					                		{
					                			logger.warn("Deleting cached file as it was to old:" + differensInHours);
					                			cacheFile.delete();
					                		}
					                		else
					                		{
					                			logger.info("Keeping cached file as it was new:" + differensInHours);
					                		}
		                            	}
		                        	}			                
		                    	}
		                	}			                
		                }
	                }
	                intervalCount = 0;
	            }
			}
            
        	long diffReCache = ((System.currentTimeMillis() - lastRecacheCall) / 1000);
        	if(CmsPropertyHandler.getOperatingMode().equals("0") && diffReCache > 1800)
    	    {    			
    			new Thread(new RecacheRepositoryRootPagesThread()).start();
    			lastRecacheCall = System.currentTimeMillis();
    	    }

        }
        catch (Exception e)
        {
            logger.error("An error occurred when we tried to update cache:" + e.getMessage());
            logger.warn("An error occurred when we tried to update cache:" + e.getMessage(), e);
        }
        
        running.set(false);
    }
    

}
