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

import org.bedework.base.ToString;
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
      qnames = new ArrayList<>();
    }

    return qnames;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(WebdavTags.prop);
    for (final QName qn: getQnames()) {
      xml.emptyTag(qn);
    }
    xml.closeTag(WebdavTags.prop);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    for (final QName qn: getQnames()) {
      ts.append(qn);
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
