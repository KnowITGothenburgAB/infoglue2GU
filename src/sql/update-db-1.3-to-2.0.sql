-- ===============================================================================
--
-- Part of the InfoGlue Content Management Platform (www.infoglue.org)
--
-- ===============================================================================
--
--  Copyright (C)
--
-- This program is free software; you can redistribute it and/or modify it under
-- the terms of the GNU General Public License version 2, as published by the
-- Free Software Foundation. See the file LICENSE.html for more information.
--
-- This program is distributed in the hope that it will be useful, but WITHOUT
-- ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License along with
-- this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
-- Place, Suite 330 / Boston, MA 02111-1307 / USA.
--
-- ===============================================================================
--
-- $Id: update-db-1.3-to-2.0.sql,v 1.2 2004/12/02 12:58:41 frank Exp $
--
-- This script contains the database updates required to go from 1.3 to 2.0.
----------------------------------------------------------------------------------
-- Update class names of invokers in cmSiteNodeTypeDefinition to reflect that
-- deliver stuff was moved into a separate package.  You will have to restart
-- your web server for the change to be visible in the management tool.
----------------------------------------------------------------------------------
update cmSiteNodeTypeDefinition
set invokerClassName = 'org.infoglue.deliver.invokers.ComponentBasedHTMLPageInvoker'
where invokerClassName = 'org.infoglue.cms.invokers.ComponentBasedHTMLPageInvoker';

update cmSiteNodeTypeDefinition
set invokerClassName = 'org.infoglue.deliver.invokers.HTMLPageInvoker'
where invokerClassName = 'org.infoglue.cms.invokers.HTMLPageInvoker';


----------------------------------------------------------------------------------
-- Add table for Category
----------------------------------------------------------------------------------
DROP TABLE IF EXISTS cmCategory;

CREATE TABLE cmCategory
(
	categoryId		INTEGER(11) unsigned NOT NULL auto_increment,
	name			VARCHAR(100) NOT NULL,
	description		TEXT,
	active			TINYINT(4) NOT NULL default '1',
	parentId		INTEGER(11),
	PRIMARY KEY (categoryId)
);

----------------------------------------------------------------------------------
-- Add table for ContentCategory
----------------------------------------------------------------------------------
DROP TABLE IF EXISTS cmContentCategory;

CREATE TABLE cmContentCategory
(
	contentCategoryId	INTEGER(11) unsigned NOT NULL auto_increment,
	attributeName		VARCHAR(100) NOT NULL,
	contentVersionId	INTEGER(11),
	categoryId			INTEGER(11),
	PRIMARY KEY (contentCategoryId)
);
