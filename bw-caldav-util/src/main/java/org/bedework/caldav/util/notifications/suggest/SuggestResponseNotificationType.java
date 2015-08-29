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
package org.bedework.caldav.util.notifications.suggest;

import org.bedework.util.misc.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;

import javax.xml.namespace.QName;

/** Public events admin suggested an event to another group.
 *
 * @author Mike Douglass douglm
 */
public class SuggestResponseNotificationType extends SuggestBaseNotificationType {
  private boolean accepted;

  /**
   * @param val true for accepted
   */
  public void setAccepted(final boolean val) {
    accepted = val;
  }

  /**
   * @return true for accepted
   */
  public boolean getAccepted() {
    return accepted;
  }

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  @Override
  public QName getElementName() {
    return BedeworkServerTags.suggestReply;
  }

  @Override
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(BedeworkServerTags.suggestReply);

    bodyToXml(xml);

    xml.property(BedeworkServerTags.accepted, String.valueOf(getAccepted()));

    xml.closeTag(BedeworkServerTags.suggestReply);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the ToString
   *
   * @param ts to build result
   */
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    ts.append("accepted", getAccepted());
  }
}
