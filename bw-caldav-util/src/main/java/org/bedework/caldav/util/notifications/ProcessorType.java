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
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;

/**
           <!ELEMENT processor type dtstamp status>
 *
 * @author Mike Douglass douglm
 */
public class ProcessorType {
  private String type;
  private String dtstamp;
  private String status;

  /**
   * @param val the type
   */
  public void setType(final String val) {
    type = val;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param val the dtstamp
   */
  public void setDtstamp(final String val) {
    dtstamp = val;
  }

  /**
   * @return the dtstamp
   */
  public String getDtstamp() {
    return dtstamp;
  }

  /**
   * @param val the status
   */
  public void setStatus(final String val) {
    status = val;
  }

  /**
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(BedeworkServerTags.processor);
    xml.property(BedeworkServerTags.type, getType());

    if (dtstamp != null) {
      xml.property(AppleServerTags.dtstamp, getDtstamp());
    }

    if (status != null) {
      xml.property(WebdavTags.status, getStatus());
    }

    xml.closeTag(BedeworkServerTags.processor);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("ChangedParameter:type", getType());
    if (dtstamp != null) {
      ts.append("dtstamp", getDtstamp());
    }
    if (status != null) {
      ts.append("status", getStatus());
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
