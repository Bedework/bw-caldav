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
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

/**  <!ELEMENT created (DAV:href, changed-by?, ANY)>

 *
 * @author Mike Douglass douglm
 */
public class BaseEntityChangeType {
  private String href;
  private ChangedByType changedBy;

  /**
   * @param val the href
   */
  public void setHref(final String val) {
    href = val;
  }

  /**
   * @return the href
   */
  public String getHref() {
    return href;
  }

  /**
   * @param val the changedBy
   */
  public void setChangedBy(final ChangedByType val) {
    changedBy = val;
  }

  /**
   * @return the first name
   */
  public ChangedByType getChangedBy() {
    return changedBy;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  public void copyForAlias(final BaseEntityChangeType copy, 
                           final String collectionHref) {
    final String[] split = Util.splitName(href);

    copy.href = Util.buildPath(href.endsWith("/"), collectionHref,
                               "/", split[1]);
    copy.changedBy = changedBy;
  }
  
  /** Called before we send it out via caldav
   *
   * @param prefixer processor
   */
  public void prefixHrefs(final UrlPrefixer prefixer) {
    setHref(prefixer.prefix(getHref()));
  }

  /** Called after we obtain it via caldav
   *
   * @param unprefixer processor
   */
  public void unprefixHrefs(final UrlUnprefixer unprefixer) {
    setHref(unprefixer.unprefix(getHref()));
  }

  /**
   * @param xml builder
   */
  public void toXmlSegment(final XmlEmit xml) {
    xml.property(WebdavTags.href, getHref());
    if (getChangedBy() != null) {
      getChangedBy().toXml(xml);
    }
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("href", getHref());
    if (getChangedBy() != null) {
      getChangedBy().toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
