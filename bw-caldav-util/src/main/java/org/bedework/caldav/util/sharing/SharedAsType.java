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
package org.bedework.caldav.util.sharing;

import org.bedework.base.ToString;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.io.StringWriter;

/**
 * Class to represent a response to a sharing invite. The href is the
 * href of the new alias in the sharee home
 *
 * @author Mike Douglass douglm
 */
public record SharedAsType(String href) {
  /**
   * @param href alias in the sharee home
   */
  public SharedAsType {
  }

  /**
   * @return the href
   */
  @Override
  public String href() {
    return href;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @return XML version of notification
   */
  public String toXml() {
    final StringWriter str = new StringWriter();
    final XmlEmit xml = new XmlEmit();

    xml.startEmit(str);
    toXml(xml);

    return str.toString();
  }

  /**
   * @param xml builder
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.sharedAs);
    xml.property(WebdavTags.href, href());
    xml.closeTag(AppleServerTags.sharedAs);
  }

  /**
   * Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  public ToString toStringSegment(final ToString ts) {
    ts.append("href", href());

    return ts;
  }

  @Override
  public String toString() {
    return toStringSegment(new ToString(this)).toString();
  }
}
