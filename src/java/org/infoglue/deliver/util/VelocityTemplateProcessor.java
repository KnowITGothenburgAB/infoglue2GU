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

package org.infoglue.deliver.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.infoglue.cms.io.FileHelper;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;

/**
 *
 * @author Mattias Bogeblad
 */

public class VelocityTemplateProcessor
{
    private final static Logger logger = Logger.getLogger(VelocityTemplateProcessor.class.getName());

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	
	public void renderTemplate(Map params, PrintWriter pw, String templateAsString) throws Exception 
	{
	    renderTemplate(params, pw, templateAsString, false);
	}
	
	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	
	public void renderTemplate(Map params, PrintWriter pw, String templateAsString, boolean forceVelocity) throws Exception 
	{
		try
		{
		    Timer timer = new Timer();
		    timer.setActive(false);
			
		    if(templateAsString.indexOf("<%") > -1 || templateAsString.indexOf("http://java.sun.com/products/jsp/dtd/jspcore_1_0.dtd") > -1)
		    {
		    	dispatchJSP(params, pw, templateAsString);
		    }
		    else
		    {
		        boolean useFreeMarker = false;
		        String useFreeMarkerString = CmsPropertyHandler.getUseFreeMarker();
		        if(useFreeMarkerString != null && useFreeMarkerString.equalsIgnoreCase("true"))
		            useFreeMarker = true;
		        
		        if((useFreeMarker || templateAsString.indexOf("<#-- IG:FreeMarker -->") > -1) && !forceVelocity)
		        {
		            FreemarkerTemplateProcessor.getProcessor().renderTemplate(params, pw, templateAsString);
		        }
		        else
		        {
					Velocity.init();
			
			        VelocityContext context = new VelocityContext();
			        Iterator i = params.keySet().iterator();
			        while(i.hasNext())
			        {
			        	String key = (String)i.next();
			            context.put(key, params.get(key));
			        }
			        
			        Reader reader = new StringReader(templateAsString);
			        boolean finished = Velocity.evaluate(context, pw, "Generator Error", reader);        
		        }
		    }

		    timer.printElapsedTime("End renderTemplate");
		}
		catch(Exception e)
		{
		    logger.warn("templateAsString: \n" + (templateAsString.length() > 500 ? templateAsString.substring(0, 500) + "... (template truncated)." : templateAsString));
		    
		    if(CmsPropertyHandler.getOperatingMode().equalsIgnoreCase("0") && (CmsPropertyHandler.getDisableTemplateDebug() == null || !CmsPropertyHandler.getDisableTemplateDebug().equalsIgnoreCase("true")))
		        pw.println("Error:" + e.getMessage());
		    else
		    {
			    logger.warn("Warning:" + e.getMessage(), e);
			    throw e;
		    }
		}
	}

	/**
	 * This methods renders a template which is written in JSP. The string is written to disk and then called.
	 * 
	 * @param params
	 * @param pw
	 * @param templateAsString
	 * @throws ServletException
	 * @throws IOException
	 */

	public void dispatchJSP(Map params, PrintWriter pw, String templateAsString) throws ServletException, IOException, Exception
	{
	    Timer timer = new Timer();
	    timer.setActive(false);

		int hashCode = templateAsString.hashCode();

		String contextRootPath = CmsPropertyHandler.getContextRootPath();
		String fileName = contextRootPath + "jsp" + File.separator + "Template_" + hashCode + ".jsp";
		String tempFileName = contextRootPath + "jsp" + File.separator + Thread.currentThread().getId() + "_tmp_Template_" + hashCode + ".jsp";
		
		File template = new File(fileName);
		File tmpTemplate = new File(tempFileName);

		if(!template.exists())
		{
			logger.info("Going to write template to file: " + template.hashCode());
			//Thread.sleep(50);
			FileHelper.writeToFile(tmpTemplate, templateAsString, false);
		
			synchronized(template) 
			{
				if(tmpTemplate.length() == 0 || template.exists())
				{
					logger.info("written file:" + tmpTemplate.length() + " - removing temp and not renaming it...");	
					tmpTemplate.delete();
				}
				else
				{
					renameTemplate(tmpTemplate, template);
					//tmpTemplate.renameTo(template);
					logger.info("Time for renaming file " + timer.getElapsedTime());
				}					
			}
		}
		
		TemplateController templateController = (TemplateController)params.get("templateLogic");
		DeliveryContext deliveryContext = templateController.getDeliveryContext();
    	RequestDispatcher dispatch = templateController.getHttpServletRequest().getRequestDispatcher("/jsp/Template_" + hashCode + ".jsp");
		templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.templateLogic", templateController);
    	CharResponseWrapper wrapper = new CharResponseWrapper(deliveryContext.getHttpServletResponse());
		
    	dispatch.include(templateController.getHttpServletRequest(), wrapper);

    	String result = wrapper.toString();

    	pw.println(result);
	}
	
	private synchronized void renameTemplate(File tempFile, File newFileName)
	{
		if(tempFile.length() == 0 || newFileName.exists())
		{
			logger.info("written file:" + newFileName.length() + " - removing temp and not renaming it...");	
			tempFile.delete();
		}
		else
		{
			logger.info("written file:" + tempFile.length() + " - renaming it to " + newFileName.getAbsolutePath());	
			tempFile.renameTo(newFileName);
		}	
	}
}
