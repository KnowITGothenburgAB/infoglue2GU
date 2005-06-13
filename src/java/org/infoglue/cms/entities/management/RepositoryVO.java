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

package org.infoglue.cms.entities.management;

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.impl.simple.RepositoryImpl;
import org.infoglue.cms.util.*;
import org.infoglue.cms.util.validators.*;

public class RepositoryVO implements BaseEntityVO
{
    private java.lang.Integer repositoryId;
    private java.lang.String name;
    private java.lang.String description;
    private java.lang.String dnsName;
        
	/**
	 * @see org.infoglue.cms.entities.kernel.BaseEntityVO#getId()
	 */
	
    public Integer getId() 
	{
		return getRepositoryId();
	}

	public String toString()
	{  
		return getName();
	}
  
    public java.lang.Integer getRepositoryId()
    {
        return this.repositoryId;
    }
                
    public void setRepositoryId(java.lang.Integer repositoryId)
    {
        this.repositoryId = repositoryId;
    }
    
    public java.lang.String getName()
    {
        return this.name;
    }
                
    public void setName(java.lang.String name)
    {
        this.name = name;		
    }
  
    public java.lang.String getDescription()
    {
        return this.description;
    }
                
    public void setDescription(java.lang.String description)
    {
        this.description = description;		
    }

    public java.lang.String getDnsName()
    {
        return this.dnsName;
    }
                
    public void setDnsName(java.lang.String dnsName)
    {
        this.dnsName = dnsName;
    }

	/**
	 * @see org.infoglue.cms.entities.kernel.BaseEntityVO#validate()
	 */
	public ConstraintExceptionBuffer validate() 
	{
    	ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();
    	
    	ValidatorFactory.createStringValidator("Repository.name", true, 6, 20, true, RepositoryImpl.class, this.getId(), null).validate(this.name, ceb);
        ValidatorFactory.createStringValidator("Repository.description", true, 1, 100).validate(description, ceb); 
    	if(dnsName != null)
    	    ValidatorFactory.createStringValidator("Repository.dnsName", false, 0, 200).validate(dnsName, ceb); 
    	
    	return ceb;
	}
        
}
        
