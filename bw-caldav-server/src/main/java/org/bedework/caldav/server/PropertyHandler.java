/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:
        
    http://www.apache.org/licenses/LICENSE-2.0
        
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.server;

import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;

import java.io.Serializable;
import java.util.Map;

import javax.xml.namespace.QName;

/** This type of object will handle system dependent properties through the
 * system interface.
 *
 * @author Mike Douglass
 */
public abstract class PropertyHandler implements Serializable {
  /** Allow callers to specify which set of properties are to be processed
   *
   * @author douglm
   */
  public static enum PropertyType {
    /** Property that can apply to anything */
    generalProperty,

    /** */
    principalProperty,

    /** */
    userProperty,

    /** */
    groupProperty,

    /** */
    collectionProperty,

    /** */
    folderProperty,

    /** */
    calendarProperty,
  }

  /**
   * @return Map of valid property names.
   */
  public abstract Map<QName, PropertyTagEntry> getPropertyNames();

  /** Return true if a call to generatePropertyValue will return a value.
   *
   * @param tag
   * @return boolean
   */
  public boolean knownProperty(QName tag) {
    return getPropertyNames().get(tag) != null;
  }
}
