/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl.resource.file;

import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.config.OwncloudFileResourceTestExecutionListener;

/**
 * A markup Interface to inform the Method {@link OwncloudFileResourceTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)} to copy the Classpath Resource
 * <code>classpath:/owncloud.xml</code> to the File-Location set within the application.yml File.
 *
 * This Interface will also be used by <code>AbstractOwncloudResourceTest.setUp()</code> to always load the Resource-Service with the Data from <code>classpath:/owncloud.xml</code> just right before
 * any Test starts. This will ensure, that every Testcase has the same Information which it builds upon.
 *
 * Also right after a Test the Method <code>AbstractOwncloudResourceTest.tearDown()</code> will call any Methods annotated by {@link CompareResourceAfter} to compare the written Data with the one, the
 * Tests assumes (normally on no Modification with the Original Source <code>classpath:/owncloud.xml</code>)
 *
 * @author mufasa1976
 * @see OwncloudFileResourceTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)
 *
 */
public interface OwncloudFileResourceTest {

  default boolean isCheckAllResourcesAgainstOriginal() {
    return true;
  }

}