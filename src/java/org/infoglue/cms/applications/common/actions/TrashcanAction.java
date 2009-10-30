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

package org.infoglue.cms.applications.common.actions;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.CategoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentTypeDefinitionController;
import org.infoglue.cms.controllers.kernel.impl.simple.InterceptionPointController;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SubscriptionController;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.entities.management.ContentTypeDefinitionVO;
import org.infoglue.cms.entities.management.InterceptionPointVO;
import org.infoglue.cms.entities.management.RepositoryVO;
import org.infoglue.cms.entities.management.SubscriptionFilterVO;
import org.infoglue.cms.entities.management.SubscriptionVO;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.cms.entities.structure.SiteNodeVO;

import webwork.action.Action;


/** 
 * This class contains methods to handle the trashcan and the item's in it.
 */

public class TrashcanAction extends InfoGlueAbstractAction
{
    private final static Logger logger = Logger.getLogger(TrashcanAction.class.getName());

	private static final long serialVersionUID = 1L;
	
	private String userSessionKey;

	//private static SubscriptionController subscriptionsController = SubscriptionController.getController();
	private List<RepositoryVO> repositoriesMarkedForDeletion = new ArrayList<RepositoryVO>();
	private List<ContentVO> contentsMarkedForDeletion = new ArrayList<ContentVO>();
	private List<SiteNodeVO> siteNodesMarkedForDeletion = new ArrayList<SiteNodeVO>();
	
	protected String doExecute() throws Exception
    {
		this.repositoriesMarkedForDeletion = RepositoryController.getController().getRepositoryVOListMarkedForDeletion();
		this.contentsMarkedForDeletion = ContentController.getContentController().getContentVOListMarkedForDeletion();
		this.siteNodesMarkedForDeletion = SiteNodeController.getController().getSiteNodeVOListMarkedForDeletion();
		
		return Action.SUCCESS;
    }

	public List<RepositoryVO> getRepositoriesMarkedForDeletion()
	{
		return repositoriesMarkedForDeletion;
	}

	public List<ContentVO> getContentsMarkedForDeletion()
	{
		return contentsMarkedForDeletion;
	}

	public List<SiteNodeVO> getSiteNodesMarkedForDeletion()
	{
		return siteNodesMarkedForDeletion;
	}
    
	
}