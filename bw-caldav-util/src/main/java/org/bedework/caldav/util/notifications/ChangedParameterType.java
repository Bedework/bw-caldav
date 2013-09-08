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
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;

/**
           <!ELEMENT changed-parameter EMPTY>
           <!ATTLIST changed-parameter name PCDATA>
           <!-- An iCalendar property parameter changed -->
 *
 * @author Mike Douglass douglm
 */
public class ChangedParameterType {
  private String name;

  private String dataFrom;
  private String dataTo;

  /**
   * @param val the name
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param val the dataFrom
   */
  public void setDataFrom(final String val) {
    dataFrom = val;
  }

  /**
   * @return the dataFrom
   */
  public String getDataFrom() {
    return dataFrom;
  }

  /**
   * @param val the dataTo
   */
  public void setDataTo(final String val) {
    dataTo = val;
  }

  /**
   * @return the dataTo
   */
  public String getDataTo() {
    return dataTo;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    if (Boolean.parseBoolean(xml.getProperty("withBedeworkElements"))) {
      xml.openTag(AppleServerTags.changedParameter, "name", getName());

      if (dataFrom != null) {
        xml.property(BedeworkServerTags.dataFrom, getDataFrom());
      }
      if (dataTo != null) {
        xml.property(BedeworkServerTags.dataTo, getDataTo());
      }

      xml.closeTag(AppleServerTags.changedParameter);
    } else {
      xml.emptyTag(AppleServerTags.changedParameter, "name", getName());
    }
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("ChangedParameter:name", getName());
    if (dataFrom != null) {
      ts.append("dataFrom", getDataFrom());
    }
    if (dataTo != null) {
      ts.append("dataTo", getDataTo());
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
