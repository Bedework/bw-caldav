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

import edu.rpi.cct.webdav.servlet.shared.UrlPrefixer;
import edu.rpi.cct.webdav.servlet.shared.UrlUnprefixer;

import org.apache.commons.codec.binary.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

/** Class to represent a resource change.
 *
 * <pre>
 * <![CDATA[
 *
   <!ELEMENT resource-change (created | updated+ | deleted |
                              collection-changes)>
     <!ELEMENT created (DAV:href, changed-by?, ANY)>
     <!ELEMENT updated (DAV:href, changed-by?, content?,
                        DAV:prop?, calendar-changes*)>
       <!ELEMENT content EMPTY>
     <!ELEMENT deleted (DAV:href, changed-by?, deleted-details)>

     <!ELEMENT changed-by (common-name | (first-name, last-name),
                           dtstamp?, DAV:href)>
       <!ELEMENT common-name CDATA>
       <!ELEMENT first-name CDATA>
       <!ELEMENT last-name CDATA>
     <!-- CS:changed-by indicates who made the change that caused the
          notification. CS:first-name and CS:last-name are the first
          and last names of the corresponding user. or the
          CS:common-name is the overall display name. CS:dtstamp is the
          time in UTC when the change was made. The DAV:href element
          is the principal URI or email address of the user who made
          the change. -->

     <!ELEMENT collection-changes (DAV:href, changed-by*, DAV:prop?,
                                   child-created?, child-updated?,
                                   child-deleted?>
       <!-- When coalescing changes from multiple users, the changed-by
            element can appear more than once. -->

       <!ELEMENT child-created CDATA>
       <!ELEMENT child-updated CDATA>
       <!ELEMENT child-deleted CDATA>
       <!-- Each of the three elements above MUST contain a positive,
            non-zero integer value indicate the total number of changes
            being reported for the collection. -->

   <!ELEMENT calendar-changes (recurrence+) >

     <!ELEMENT recurrence
         ((master | recurrenceid), added?, removed?, changes?)>
     <!-- Which instances were affected by the change,
          and details on the per-instance changes -->

       <!ELEMENT master EMPTY>
       <!-- The "master" instance was affected -->

       <!ELEMENT recurrenceid CDATA>
       <!-- RECURRENCE-ID value in iCalendar form (in UTC if a
            non-floating DATE-TIME value) for the affected instance -->

       <!ELEMENT added EMPTY>
       <!-- The component was added -->

       <!ELEMENT removed EMPTY>
       <!-- The component was removed -->

       <!ELEMENT changes changed-property*>
       <!-- Detailed changes in the iCalendar data -->

         <!ELEMENT changed-property changed-parameter*>
         <!ATTLIST changed-property name PCDATA>
         <!-- An iCalendar property changed -->

           <!ELEMENT changed-parameter EMPTY>
           <!ATTLIST changed-parameter name PCDATA>
           <!-- An iCalendar property parameter changed -->

   <!ELEMENT deleted-details ((deleted-component,
                               deleted-summary,
                               deleted-next-instance?,
                               deleted-had-more-instances?) |
                              deleted-displayname)>
   <!-- deleted-displayname is used for a collection delete, the other
        elements used for a resource delete. -->

     <!ELEMENT deleted-component CDATA>
     <!-- The main calendar component type of the deleted
          resource, e.g., "VEVENT", "VTODO" -->

     <!ELEMENT deleted-summary CDATA>
     <!-- Indicates the "SUMMARY" of the next future instance at the
          time of deletion, or the previous instance if no future
          instances existed at the time of deletion. -->

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
 * <p>We are going to assume one notification at least per resource identified
 * by the href.
 *
 * @author Mike Douglass douglm
 */
public class ResourceChangeType extends BaseNotificationType {
  // This we use as the resource name
  private String uid;

  // This we store as the encoding
  private String name;

  private CreatedType created;
  private DeletedType deleted;
  private CollectionChangesType collectionChanges;

  private List<UpdatedType> updated;

  /**
   * @param val the created
   */
  public void setCreated(final CreatedType val) {
    created = val;

    checkName(val.getHref());
  }

  /**
   * @return the created
   */
  public CreatedType getCreated() {
    return created;
  }

  /**
   * @param val deleted
   */
  public void setDeleted(final DeletedType val) {
    deleted = val;

    checkName(val.getHref());
  }

  /**
   * @return deleted
   */
  public DeletedType getDeleted() {
    return deleted;
  }

  /**
   *
   * @param val the collectionChanges
   */
  public void setCollectionChanges(final CollectionChangesType val) {
    collectionChanges = val;

    checkName(val.getHref());
  }

  /**
   * @return the collectionChanges
   */
  public CollectionChangesType getCollectionChanges() {
    return collectionChanges;
  }

  /**
   * @return the updated list as an unmodifiable list
   */
  public List<UpdatedType> getUpdated() {
    if (updated == null) {
      updated = new ArrayList<UpdatedType>();
    }

    return Collections.unmodifiableList(updated);
  }

  /**
   * @param val an update entry
   */
  public void addUpdate(final UpdatedType val) {
    if (updated == null) {
      updated = new ArrayList<UpdatedType>();
    }

    updated.add(val);

    checkName(val.getHref());
  }

  /**
   */
  public void clearUpdated() {
    if (updated == null) {
      return;
    }

    updated.clear();
  }

  /* ====================================================================
   *                   BaseNotificationType methods
   * ==================================================================== */

  @Override
  public QName getElementName() {
    return AppleServerTags.resourceChange;
  }

  /** SCHEMA - some squirrelly stuff until we get a name long enough to really be
   * the name
   *
   * @param val
   */
  @Override
  public void setName(final String val) {
    uid = val;
  }

  @Override
  public String getName() {
    return uid;
  }

  @Override
  public void setEncoding(final String val) {
    name = val;
  }

  @Override
  public String getEncoding() {
    return name;
  }

  private List<AttributeType> attrs;

  @Override
  public List<AttributeType> getElementAttributes() {
    if (attrs != null) {
      return attrs;
    }

    attrs = new ArrayList<AttributeType>();

    return attrs;
  }

  @Override
  public void prefixHrefs(final UrlPrefixer prefixer) throws Throwable {
    if (getCreated() != null) {
      getCreated().prefixHrefs(prefixer);
      return;
    }

    if (getDeleted() != null) {
      getDeleted().prefixHrefs(prefixer);
      return;
    }

    if (getCollectionChanges() != null) {
      getCollectionChanges().prefixHrefs(prefixer);
      return;
    }

    for (UpdatedType u: getUpdated()) {
      u.prefixHrefs(prefixer);
    }
  }

  @Override
  public void unprefixHrefs(final UrlUnprefixer unprefixer) throws Throwable {
    if (getCreated() != null) {
      getCreated().unprefixHrefs(unprefixer);
      return;
    }

    if (getDeleted() != null) {
      getDeleted().unprefixHrefs(unprefixer);
      return;
    }

    if (getCollectionChanges() != null) {
      getCollectionChanges().unprefixHrefs(unprefixer);
      return;
    }

    for (UpdatedType u: getUpdated()) {
      u.unprefixHrefs(unprefixer);
    }
  }

  @Override
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.resourceChange);

    if (getCollectionChanges() != null) {
      getCollectionChanges().toXml(xml);
      return;
    }

    if (getCreated() != null) {
      getCreated().toXml(xml);
    }

    for (UpdatedType u: getUpdated()) {
      u.toXml(xml);
    }

    if (getDeleted() != null) {
      getDeleted().toXml(xml);
    }

    xml.closeTag(AppleServerTags.resourceChange);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    if (getCollectionChanges() != null) {
      getCollectionChanges().toStringSegment(ts);
      return;
    }

    if (getCreated() != null) {
      getCreated().toStringSegment(ts);
    }

    for (UpdatedType u: getUpdated()) {
      u.toStringSegment(ts);
    }

    if (getDeleted() != null) {
      getDeleted().toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void checkName(final String val) {
    String bval = Base64.encodeBase64String(val.getBytes());

    if (getEncoding() == null) {
      setEncoding(bval);
    } else if (!getEncoding().equals(bval)) {
      throw new RuntimeException("Attempt to store different href in change " +
          "notification. Old: " + getEncoding() + " new: " + val);
    }
  }
}
