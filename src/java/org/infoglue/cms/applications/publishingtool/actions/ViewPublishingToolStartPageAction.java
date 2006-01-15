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

package org.infoglue.cms.applications.publishingtool.actions;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;

import org.infoglue.cms.controllers.kernel.impl.simple.*;
import org.infoglue.cms.util.ChangeNotificationController;
import org.infoglue.cms.util.NotificationMessage;
import org.infoglue.cms.util.RemoteCacheUpdater;

import java.util.List;

/**
 * This class implements the action class for the startpage in the management tool.
 * 
 * @author Mattias Bogeblad  
 */

public class ViewPublishingToolStartPageAction extends InfoGlueAbstractAction
{
    private List repositories;
    
    public String doExecute() throws Exception
    {
    	this.repositories = RepositoryController.getController().getAuthorizedRepositoryVOList(this.getInfoGluePrincipal(), false);
    	
        return "success";
    }
    
    public String doPushSystemNotificationMessages() throws Exception
    {
        NotificationMessage notificationMessage = null;
        List messages = RemoteCacheUpdater.getSystemNotificationMessages();
        synchronized(messages)
        {
            if(messages.size() > 0)
                notificationMessage = (NotificationMessage)messages.get(0);
        }
        
        if(notificationMessage != null)
        {
            ChangeNotificationController.getInstance().addNotificationMessage(notificationMessage);
            RemoteCacheUpdater.clearSystemNotificationMessages();
        }
        
        return doExecute();
    }
    
    public List getRepositories()
    {
    	return this.repositories;
    }
            
    public List getSystemNotificationMessages()
    {
        return RemoteCacheUpdater.getSystemNotificationMessages();
    }
}
