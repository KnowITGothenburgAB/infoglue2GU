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
package org.infoglue.cms.jobs;

import org.apache.log4j.Logger;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author mattias
 *
 * This jobs searches for expiring contents or sitenodes and clears caches if found.
 */

public class CleanOldVersionsJob implements Job
{
    private final static Logger logger = Logger.getLogger(CleanOldVersionsJob.class.getName());

    private static boolean running = false;
    
    public synchronized void execute(JobExecutionContext context) throws JobExecutionException
    {
    	logger.info("*********************************************************************");
    	logger.info("* Starting version cleanup job which should run with nice intervals *");
    	logger.info("* Purpose is to keep the database size at a minimum 				 *");
    	logger.info("*********************************************************************");

    	if(!running)
    		running = true;
    	else
    	{
    		logger.info("CleanOldVersionsJob allready running... skipping.");
    		return;
    	}
    	
    	try
		{
    		Boolean deleteVersions = (Boolean)context.get("deleteVersions");
    		if(deleteVersions == null)
    			deleteVersions = new Boolean(true);
	    	logger.info("deleteVersions:" + deleteVersions);
	    	String numberOfVersionsToKeepDuringClean = CmsPropertyHandler.getNumberOfVersionsToKeepDuringClean();
	    	logger.info("numberOfVersionsToKeepDuringClean:" + numberOfVersionsToKeepDuringClean);
			Integer numberOfVersionsToKeepDuringCleanInteger = new Integer(numberOfVersionsToKeepDuringClean);
	    	String keepOnlyOldPublishedVersionsString = CmsPropertyHandler.getKeepOnlyOldPublishedVersionsDuringClean();
	    	logger.info("keepOnlyOldPublishedVersionsString:" + keepOnlyOldPublishedVersionsString);
	    	long minimumTimeBetweenVersionsDuringClean = CmsPropertyHandler.getMinimumTimeBetweenVersionsDuringClean();
	    	logger.info("minimumTimeBetweenVersionsDuringClean:" + minimumTimeBetweenVersionsDuringClean);
	    	boolean keepOnlyOldPublishedVersions = Boolean.parseBoolean(keepOnlyOldPublishedVersionsString);
			if(numberOfVersionsToKeepDuringCleanInteger.intValue() < 3)
				numberOfVersionsToKeepDuringCleanInteger = new Integer(3);
			if(numberOfVersionsToKeepDuringCleanInteger.intValue() > -1)
			{
				int cleanedVersions = ContentVersionController.getContentVersionController().cleanContentVersions(numberOfVersionsToKeepDuringCleanInteger.intValue(), keepOnlyOldPublishedVersions, minimumTimeBetweenVersionsDuringClean, deleteVersions);
				context.setResult(new Integer(cleanedVersions));
			}
		}
		catch(Exception e)
	    {
	    	logger.error("Could not clean up old versions: " + e.getMessage());
	    }
		finally
		{
			running = false;
		}
		
	   	logger.info("Cleanup-job finished");
    }
    
}
