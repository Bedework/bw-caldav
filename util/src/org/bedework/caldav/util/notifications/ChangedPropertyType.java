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

import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;

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
   * @return the changedParameter list
   */
  public List<ChangedParameterType> getChangedParameter() {
    if (changedParameter == null) {
      changedParameter = new ArrayList<ChangedParameterType>();
    }

    return changedParameter;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.changedProperty, "name", getName());
    for (ChangedParameterType cp: getChangedParameter()) {
      cp.toXml(xml);
    }
    xml.closeTag(AppleServerTags.changedProperty);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    ts.append("ChangedProperty:name", getName());
    for (ChangedParameterType cp: getChangedParameter()) {
      cp.toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
