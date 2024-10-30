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

import java.util.ArrayList;
import java.util.List;

/**
         <!ELEMENT changed-property changed-parameter*>
         <!ATTLIST changed-property name PCDATA>
         <!-- An iCalendar property changed -->
 *
 * @author Mike Douglass douglm
 */
public class ChangedPropertyType {
  private String name;

  private String dataFrom;
  private String dataTo;

  private List<ChangedParameterType> changedParameter;

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

  /**
   * @return the changedParameter list
   */
  public List<ChangedParameterType> getChangedParameter() {
    if (changedParameter == null) {
      changedParameter = new ArrayList<>();
    }

    return changedParameter;
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

  /**
   * @param xml emitter
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.changedProperty, "name", getName());
    for (final ChangedParameterType cp: getChangedParameter()) {
      cp.toXml(xml);
    }

    if (Boolean.parseBoolean(xml.getProperty("withBedeworkElements"))) {
      if (getDataFrom() != null) {
        xml.property(BedeworkServerTags.dataFrom, getDataFrom());
      }
      if (getDataTo() != null) {
        xml.property(BedeworkServerTags.dataTo, getDataTo());
      }
    }

    xml.closeTag(AppleServerTags.changedProperty);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts for output
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("ChangedProperty:name", getName());
    for (final ChangedParameterType cp: getChangedParameter()) {
      cp.toStringSegment(ts);
    }
    if (getDataFrom() != null) {
      ts.append("dataFrom", getDataFrom());
    }
    if (getDataTo() != null) {
      ts.append("dataTo", getDataTo());
    }
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
