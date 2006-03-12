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

package org.infoglue.cms.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

public class ChangeNotificationController
{

    private final static Logger logger = Logger.getLogger(ChangeNotificationController.class.getName());

	//The singleton
	private static ChangeNotificationController instance = null;

	/**
	 * A factory method that makes sure we operate on a singeton.
	 * We assign a couple of standard listeners like a logger and a transactionHistoryLogger. 
	 */
	
	public static ChangeNotificationController getInstance()
	{
		if(instance == null)
		{
			instance = new ChangeNotificationController();
			//instance.registerListener(new FileLogger());
			
			String logTransactions = CmsPropertyHandler.getLogTransactions();
			if(logTransactions == null || !logTransactions.equalsIgnoreCase("false"))
			    instance.registerListener(new TransactionHistoryWriter());
			
			instance.registerListener(new RemoteCacheUpdater());
			//instance.registerListener(new WorkflowEngine());
		}
		
		return instance;		
	}

//-------------------- The object stuff ---------------------//

	//List of all listeners.
	private List listeners = new ArrayList();
	
	//List of all listeners that shall be unregistered
	//(to avoid concurrent modification exceptions, and deadlocks)
	private List unregisteredlisteners = new ArrayList();
	
	/**
	 * The standard constructor is private to force use of factory-method.
	 */
	
	private ChangeNotificationController()
	{
	}

	/**
	 * This method registers a new listener to be notified when a new notifiation is available.
	 */
	
	public void registerListener(NotificationListener notificationListener)
	{
		this.listeners.add(notificationListener);
	}

	/**
	 * This method unregisters an existing listener.
	 */
	
	public void unregisterListener(NotificationListener notificationListener)
	{
		this.unregisteredlisteners.add(notificationListener);
	}
	
	/**
	 * This method gets called when a new notification has come. 
	 * It then iterates through the listeners and notifies them.
	 */
	public void addNotificationMessage(NotificationMessage notificationMessage)
	{
		logger.info("Got a new notification:" + notificationMessage.getName() + ":" + notificationMessage.getType() + ":" + notificationMessage.getObjectId() + ":" + notificationMessage.getObjectName());
		Iterator i = listeners.iterator();
		while(i.hasNext())
		{
			try
			{
				NotificationListener nl = (NotificationListener)i.next();
				if(!unregisteredlisteners.contains(nl))
				{
					logger.info("Notifying the listener:" + nl.getClass().getName());
					nl.notify(notificationMessage);
				}
			}
			catch(Exception e)
			{
				logger.error("One of the listeners threw an exception but we carry on with the others. Error: " + e.getMessage(), e);
			}
		}
		listeners.removeAll(unregisteredlisteners);
		unregisteredlisteners.clear();
	}	
			

}