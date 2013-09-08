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
package org.bedework.caldav.util.notifications;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/** Represent a dav:PROP element which contains a list of QName
 *
 * <p> DAV:PROP is defined as containing ANY so this is not complete.
 *
 * @author Mike Douglass douglm
 */
public class PropType {
  private List<QName> qnames;

  /**
   * @return the names
   */
  public List<QName> getQnames() {
    if (qnames == null) {
      qnames = new ArrayList<QName>();
    }

    return qnames;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(WebdavTags.prop);
    for (QName qn: getQnames()) {
      xml.emptyTag(qn);
    }
    xml.closeTag(WebdavTags.prop);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    for (QName qn: getQnames()) {
      ts.append(qn);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
