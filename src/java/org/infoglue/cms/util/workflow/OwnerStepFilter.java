/**
 * $Id: OwnerStepFilter.java,v 1.5 2004/12/29 16:56:46 jed Exp $
 * Created by jed on Dec 28, 2004
 */
package org.infoglue.cms.util.workflow;

import org.infoglue.cms.security.*;
import org.infoglue.cms.entities.mydesktop.WorkflowStepVO;
import org.infoglue.cms.util.workflow.StepFilter;
import org.infoglue.cms.applications.common.Session;

/**
 * Filters steps according to owner.  If a step has no owner, it is assumed that anyone can view it.
 * An administrator can view all steps regardless of owner.
 * @author <a href="mailto:jedprentice@gmail.com">Jed Prentice</a>
 * @version $Revision: 1.5 $ $Date: 2004/12/29 16:56:46 $
 */
public class OwnerStepFilter implements StepFilter
{
	private final InfoGluePrincipal userPrincipal;

	/**
	 * Constructs an OwnerStepFilter from the user associated with the current session/action context
	 */
	public OwnerStepFilter()
	{
		this(new Session().getInfoGluePrincipal());
	}

	/**
	 * Constructs an owner step filter with the given user principal
	 * @param userPrincipal an InfoGluePrincipal representing a user
	 */
	public OwnerStepFilter(InfoGluePrincipal userPrincipal)
	{
		this.userPrincipal = userPrincipal;
	}

	/**
	 * Indicates whether the step is allowed
	 * @param step a workflow step.
	 * @return true if the step is owned by the current user, the current user is an administrator, or if the owner
	 * of the step is null; otherwise returns false.
	 */
	public boolean isAllowed(WorkflowStepVO step)
	{
		return hasNoOwner(step) || isUserOwner(step) || isUserAdministrator();
	}

	private static boolean hasNoOwner(WorkflowStepVO step)
	{
		return step.getOwner() == null;
	}

	private boolean isUserOwner(WorkflowStepVO step)
	{
		return step.getOwner().equalsIgnoreCase(userPrincipal.getName());
	}

	private boolean isUserAdministrator()
	{
		return userPrincipal.getIsAdministrator();
	}
}
