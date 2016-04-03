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

import java.util.ArrayList;
import java.util.List;

/** Class to represent a sharing request.
 *
 * <pre>
 * <![CDATA[
 *
   <!ELEMENT deleted-details ((deleted-component,
                               deleted-summary,
                               deleted-next-instance?,
                               deleted-had-more-instances?) |
                              deleted-displayname)>
   <!-- deleted-displayname is used for a collection delete, the other
        elements used for a resource delete. -->

     <!ELEMENT deleted-component CDATA>

     <!ELEMENT deleted-summary CDATA>

     <!ELEMENT deleted-next-instance CDATA>
     <!ATTLIST deleted-next-instance tzid PCDATA>
     <!-- If present, indicates when the next deleted instance would
          have occurred. For a VEVENT that would be the DTSTART value,
          for a VTODO that would be either DTSTART or DUE, if present.
          In each case the value must match the value in the iCalendar
          data, and any TZID iCalendar property parameter value must
          be included in the tzid XML element attribute value. -->

     <!ELEMENT deleted-had-more-instances EMPTY>
     <!-- If present indicates that there was more than one future
          instances still to occur at the time of deletion. -->

     <!ELEMENT deleted-displayname CDATA>
     <!-- The DAV:getdisplayname property for the collection that
          was deleted.  -->

]]>  </pre>
 *
 * @author Mike Douglass douglm
 */
public class DeletedDetailsType {
  private String deletedComponent;
  private String deletedSummary;
  private String deletedNextInstance;
  private String deletedNextInstanceTzid;
  private boolean deletedHadMoreInstances;
  private String deletedDisplayname;
  private List<ChangedPropertyType> deletedProps = new ArrayList<ChangedPropertyType>();

  /** The main calendar component type of the deleted resource,
   * e.g., "VEVENT", "VTODO"
   *
   * @param val the deletedComponent
   */
  public void setDeletedComponent(final String val) {
    deletedComponent = val;
  }

  /**
   * @return the deletedComponent
   */
  public String getDeletedComponent() {
    return deletedComponent;
  }

  /**
   * @param val the deletedSummary
   */
  public void setDeletedSummary(final String val) {
    deletedSummary = val;
  }

  /** Indicates the "SUMMARY" of the next future instance at the time of
   * deletion, or the previous instance if no future instances existed at the
   * time of deletion.
   *
   * @return the deletedSummary
   */
  public String getDeletedSummary() {
    return deletedSummary;
  }

  /**
   *
   * @param val the deletedNextInstance
   */
  public void setDeletedNextInstance(final String val) {
    deletedNextInstance = val;
  }

  /**
   * @return the deletedNextInstance
   */
  public String getDeletedNextInstance() {
    return deletedNextInstance;
  }

  /**
   *
   * @param val the deletedNextInstanceTzid
   */
  public void setDeletedNextInstanceTzid(final String val) {
    deletedNextInstanceTzid = val;
  }

  /**
   * @return the deletedNextInstanceTzid
   */
  public String getDeletedNextInstanceTzid() {
    return deletedNextInstanceTzid;
  }

  /**
   * @param val the deletedHadMoreInstances
   */
  public void setDeletedHadMoreInstances(final boolean val) {
    deletedHadMoreInstances = val;
  }

  /**
   * @return the deletedHadMoreInstances
   */
  public boolean getDeletedHadMoreInstances() {
    return deletedHadMoreInstances;
  }

  /**
   * @param val the deletedDisplayname
   */
  public void setDeletedDisplayname(final String val) {
    deletedDisplayname = val;
  }

  /**
   * @return the deletedDisplayname
   */
  public String getDeletedDisplayname() {
    return deletedDisplayname;
  }

  /**
   * @param val the deletedProps
   */
  public void setDeletedProps(final List<ChangedPropertyType> val) {
    deletedProps = val;
  }

  /**
   * @return the deletedProps
   */
  public List<ChangedPropertyType> getDeletedProps() {
    return deletedProps;
  }

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.deletedDetails);

    if (getDeletedDisplayname() != null) {
      xml.property(AppleServerTags.deletedDisplayname, getDeletedDisplayname());
      return;
    }

    xml.property(AppleServerTags.deletedComponent, getDeletedComponent());
    xml.property(AppleServerTags.deletedSummary, getDeletedSummary());

    if (getDeletedNextInstance() != null) {
      if (getDeletedNextInstanceTzid() == null) {
        xml.property(AppleServerTags.deletedNextInstance, getDeletedNextInstance());
      } else {
        xml.openTagNoNewline(AppleServerTags.deletedNextInstance,
                             "tzid", getDeletedNextInstanceTzid());
        xml.value(getDeletedNextInstance());
        xml.closeTagNoblanks(AppleServerTags.deletedNextInstance);
      }
    }
    if (getDeletedHadMoreInstances()) {
      xml.emptyTag(AppleServerTags.deletedHadMoreInstances);
    }
    for (ChangedPropertyType cp: getDeletedProps()) {
      cp.toXml(xml);
    }

    xml.closeTag(AppleServerTags.deletedDetails);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    if (getDeletedDisplayname() != null) {
      ts.append("deletedDisplayname", getDeletedDisplayname());
      return;
    }

    ts.append("deletedComponent", getDeletedComponent());
    ts.append("deletedSummary", getDeletedSummary());
    if (getDeletedNextInstance() != null) {
      ts.append("deletedNextInstance", getDeletedNextInstance());
      ts.append("deletedNextInstanceTzid", getDeletedNextInstanceTzid());
    }
    if (getDeletedHadMoreInstances()) {
      ts.append("deletedHadMoreInstances", getDeletedHadMoreInstances());
    }
    for (ChangedPropertyType cp: getDeletedProps()) {
      ts.append("deletedProp", cp);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
