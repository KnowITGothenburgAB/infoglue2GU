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

package org.infoglue.cms.applications.common;

import org.infoglue.cms.util.CmsLogger;
import org.infoglue.cms.util.CmsPropertyHandler;

import java.util.Date;
import java.util.Locale;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class VisualFormatter
{
    public VisualFormatter()
    {
    }
    
    public String formatDate(Date date, String pattern)
    {	
    	CmsLogger.logInfo("About to convert " + date + " with the pattern " + pattern);
        if(date == null)
            return "";
            
        //CmsLogger.logInfo( TimeZone.getDefault().getID() );      
        /*
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, "PST");
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2*60*60*1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*60*60*1000);
        */
        // Format the current time.
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        String dateString = formatter.format(date);
		CmsLogger.logInfo("dateString: " + dateString);
        return dateString;
    }

	public String formatDate(Date date, Locale locale, String pattern)
	{	
		CmsLogger.logInfo("About to convert " + date + " with the pattern " + pattern);
		if(date == null)
			return "";
            
		// Format the current time.
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
		String dateString = formatter.format(date);
		CmsLogger.logInfo("dateString: " + dateString);
		return dateString;
	}
	
    public Date parseDate(String dateString, String pattern)
    {	
    	CmsLogger.logInfo("About to convert " + dateString + " with the pattern " + pattern);
        if(dateString == null)
            return new Date();
        
        Date date = new Date();    
        
        try
        {
	        CmsLogger.logInfo( TimeZone.getDefault().getID() );      
	        // Format the current time.
	        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
	        date = formatter.parse(dateString);
			CmsLogger.logInfo("date: " + date);
        }
        catch(Exception e)
        {
        	CmsLogger.logInfo("Error parsing date:" + dateString);
        }
        
        return date;
    }

	public Date parseDate(String dateString, Locale locale, String pattern)
	 {	
		 CmsLogger.logInfo("About to convert " + dateString + " with the pattern " + pattern);
		 if(dateString == null)
			 return new Date();
        
		 Date date = new Date();    
        
		 try
		 {
			 CmsLogger.logInfo( TimeZone.getDefault().getID() );      
			 // Format the current time.
			 SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
			 date = formatter.parse(dateString);
			 CmsLogger.logInfo("date: " + date);
		 }
		 catch(Exception e)
		 {
			 CmsLogger.logInfo("Error parsing date:" + dateString);
		 }
        
		 return date;
	 }
	
	/**
	 * This method converts all non-standard characters to html-equivalents.
	 */
	
	public final String escapeHTML(String s) throws Exception
	{
		if(s == null)
			return null;
			
	    StringBuffer sb = new StringBuffer();
		int n = s.length();
	    for (int i = 0; i < n; i++) 
	    {
	       	char c = s.charAt(i);
    	   	switch (c) 
	       	{
				case '<': sb.append("&lt;"); break;
	         	case '>': sb.append("&gt;"); break;
	         	case '&': sb.append("&amp;"); break;
	         	case '"': sb.append("&quot;"); break;
	         	/*
	         	case '�': sb.append("&agrave;");break;
	         	case '�': sb.append("&Agrave;");break;
	         	case '�': sb.append("&acirc;");break;
	         	case '�': sb.append("&Acirc;");break;
	         	case '�': sb.append("&auml;");break;
	         	case '�': sb.append("&Auml;");break;
	         	case '�': sb.append("&aring;");break;
	         	case '�': sb.append("&Aring;");break;
	         	case '�': sb.append("&aelig;");break;
	         	case '�': sb.append("&AElig;");break;
	         	case '�': sb.append("&ccedil;");break;
	         	case '�': sb.append("&Ccedil;");break;
	         	case '�': sb.append("&eacute;");break;
	         	case '�': sb.append("&Eacute;");break;
	         	case '�': sb.append("&egrave;");break;
	         	case '�': sb.append("&Egrave;");break;
	         	case '�': sb.append("&ecirc;");break;
	         	case '�': sb.append("&Ecirc;");break;
	         	case '�': sb.append("&euml;");break;
	         	case '�': sb.append("&Euml;");break;
	         	case '�': sb.append("&iuml;");break;
	         	case '�': sb.append("&Iuml;");break;
	         	case '�': sb.append("&ocirc;");break;
	         	case '�': sb.append("&Ocirc;");break;
	         	case '�': sb.append("&ouml;");break;
	         	case '�': sb.append("&Ouml;");break;
	         	case '�': sb.append("&oslash;");break;
	         	case '�': sb.append("&Oslash;");break;
	         	case '�': sb.append("&szlig;");break;
	         	case '�': sb.append("&ugrave;");break;
	         	case '�': sb.append("&Ugrave;");break;         
	         	case '�': sb.append("&ucirc;");break;         
	         	case '�': sb.append("&Ucirc;");break;
	         	case '�': sb.append("&uuml;");break;
	         	case '�': sb.append("&Uuml;");break;
	         	case '�': sb.append("&reg;");break;         
	         	case '�': sb.append("&copy;");break;   
	         	case '�': sb.append("&euro;"); break;
	         	*/

	         	default:  sb.append(c); break;
	      	}
	   	}
	   	return sb.toString();
	}
	

	/**
	 * 
	 * Temporary method, please do not use. (SS, 2004-12-13)
	 * @deprecated
	 */
	
	public final String escapeHTMLforXMLService(String s)
	{
		if(s == null)
			return null;
			
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) 
		{
			char c = s.charAt(i);
			switch (c) 
			{
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '&': sb.append("&amp;"); break;
				case '"': sb.append("&quot;"); break;
				case '�': sb.append("&agrave;");break;
				case '�': sb.append("&Agrave;");break;
				case '�': sb.append("&acirc;");break;
				case '�': sb.append("&Acirc;");break;
				case '�': sb.append("&auml;");break;
				case '�': sb.append("&Auml;");break;
				case '�': sb.append("&aring;");break;
				case '�': sb.append("&Aring;");break;
				case '�': sb.append("&aelig;");break;
				case '�': sb.append("&AElig;");break;
				case '�': sb.append("&ccedil;");break;
				case '�': sb.append("&Ccedil;");break;
				case '�': sb.append("&eacute;");break;
				case '�': sb.append("&Eacute;");break;
				case '�': sb.append("&egrave;");break;
				case '�': sb.append("&ograve;");break;
				case '�': sb.append("&Egrave;");break;
				case '�': sb.append("&ecirc;");break;
				case '�': sb.append("&Ecirc;");break;
				case '�': sb.append("&euml;");break;
				case '�': sb.append("&Euml;");break;
				case '�': sb.append("&iuml;");break;
				case '�': sb.append("&Iuml;");break;
				case '�': sb.append("&ocirc;");break;
				case '�': sb.append("&Ocirc;");break;
				case '�': sb.append("&ouml;");break;
				case '�': sb.append("&Ouml;");break;
				case '�': sb.append("&oslash;");break;
				case '�': sb.append("&Oslash;");break;
				case '�': sb.append("&szlig;");break;
				case '�': sb.append("&ugrave;");break;
				case '�': sb.append("&Ugrave;");break;         
				case '�': sb.append("&ucirc;");break;         
				case '�': sb.append("&Ucirc;");break;
				case '�': sb.append("&uuml;");break;
				case '�': sb.append("&Uuml;");break;
				case '�': sb.append("&reg;");break;         
				case '�': sb.append("&copy;");break;   
				case '�': sb.append("&euro;"); break;
				case '\'': sb.append("&#146;"); break;
				
				default:  sb.append(c); break;
			}
		}
		return sb.toString();
	}
	
	public final String escapeExtendedHTML(String s)
	{
		if(s == null)
			return null;
			
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) 
		{
			char c = s.charAt(i);
			switch (c) 
			{
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '&': sb.append("&amp;"); break;
				case '"': sb.append("&quot;"); break;
				/*
				case '�': sb.append("&agrave;");break;
				case '�': sb.append("&Agrave;");break;
				case '�': sb.append("&acirc;");break;
				case '�': sb.append("&Acirc;");break;
				case '�': sb.append("&auml;");break;
				case '�': sb.append("&Auml;");break;
				case '�': sb.append("&aring;");break;
				case '�': sb.append("&Aring;");break;
				case '�': sb.append("&aelig;");break;
				case '�': sb.append("&AElig;");break;
				case '�': sb.append("&ccedil;");break;
				case '�': sb.append("&Ccedil;");break;
				case '�': sb.append("&eacute;");break;
				case '�': sb.append("&Eacute;");break;
				case '�': sb.append("&egrave;");break;
				case '�': sb.append("&Egrave;");break;
				case '�': sb.append("&ecirc;");break;
				case '�': sb.append("&Ecirc;");break;
				case '�': sb.append("&euml;");break;
				case '�': sb.append("&Euml;");break;
				case '�': sb.append("&iuml;");break;
				case '�': sb.append("&Iuml;");break;
				case '�': sb.append("&ocirc;");break;
				case '�': sb.append("&Ocirc;");break;
				case '�': sb.append("&ouml;");break;
				case '�': sb.append("&Ouml;");break;
				case '�': sb.append("&oslash;");break;
				case '�': sb.append("&Oslash;");break;
				case '�': sb.append("&szlig;");break;
				case '�': sb.append("&ugrave;");break;
				case '�': sb.append("&Ugrave;");break;         
				case '�': sb.append("&ucirc;");break;         
				case '�': sb.append("&Ucirc;");break;
				case '�': sb.append("&uuml;");break;
				case '�': sb.append("&Uuml;");break;
				case '�': sb.append("&reg;");break;         
				case '�': sb.append("&copy;");break;   
				case '�': sb.append("&euro;"); break;
	         	*/
				case '\'': sb.append("&#146;"); break;

				default:  sb.append(c); break;
			}
		}
		return sb.toString();
	}
	
	/**
	 * This method converts all non-standard characters to html-equivalents.
	 */
	
	public final String escapeForJavascripts(String s)
	{
		if(s == null)
			return null;
			
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) 
		{
			char c = s.charAt(i);
			if(c == '\'') sb.append("\\'");
			else sb.append(c);
		}
		
		return sb.toString();
	}
	
	/**
	 * This method converts all non-standard characters to html-equivalents.
	 */
	
	public final String encode(String s) throws Exception
	{
		if(s == null)
			return null;
		
		return URLEncoder.encode(s, "UTF-8");
	}

	/**
	 * This method converts all non-standard characters to html-equivalents.
	 */
	
	public final String encodeURI(String s) throws Exception
	{
		if(s == null)
			return null;
		
		String encoding = CmsPropertyHandler.getProperty("URIEncoding");
		
		return URLEncoder.encode(s, encoding);
	}

}