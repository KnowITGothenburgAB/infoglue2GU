package org.infoglue.cms.applications.managementtool.actions;

import java.util.List;

import org.infoglue.cms.applications.common.actions.ModelAction;
import org.infoglue.cms.controllers.kernel.impl.simple.CategoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentCategoryController;
import org.infoglue.cms.entities.kernel.Persistent;
import org.infoglue.cms.entities.management.CategoryVO;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;

/**
 * @author Frank Febbraro (frank@phase2technology.com)
 */
public class CategoryAction extends ModelAction
{
	private static final long serialVersionUID = 1L;
	
	public static final String MAIN = "main";

	private CategoryController controller = CategoryController.getController();
	private ContentCategoryController contentCategoryController = ContentCategoryController.getController();

	protected Persistent createModel()	{ return new CategoryVO(); }

	public CategoryVO getCategory()		{ return (CategoryVO)getModel(); }

	public Integer getCategoryId()			{ return getCategory().getCategoryId(); }
	public void setCategoryId(Integer i)	{ getCategory().setCategoryId(i); }

	public List getReferences() throws Exception
	{
		return contentCategoryController.findByCategory(getCategoryId());
	}
	
	public String doList() throws SystemException
	{
		setModels(controller.findRootCategories());
		return SUCCESS;
	}

	public String doNew() throws SystemException
	{
		return SUCCESS;
	}

	public String doEdit() throws SystemException
	{
		setModel(controller.findWithChildren(getCategoryId()));
		return SUCCESS;
	}

	public String doDisplayTreeForMove() throws SystemException
	{
		return SUCCESS;
	}

	public String doMove() throws SystemException
	{
		setModel(controller.moveCategory(getCategoryId(), getCategory().getParentId()));
		return SUCCESS;
	}

	public String doSave() throws SystemException, ConstraintException
	{
		validateModel();
		setModel(controller.save(getCategory()));
		return (getCategory().isRoot())? MAIN : SUCCESS;
	}

	public String doDelete() throws SystemException
	{
		// So we have the parent and know which page to go to
		setModel(controller.findById(getCategoryId()));
		controller.delete(getCategoryId());

		return (getCategory().getParentId() == null) ? MAIN : SUCCESS;
	}

	// Needed as part of WebworklAbstractAction
	public String doExecute() throws Exception
	{ return SUCCESS; }
}
