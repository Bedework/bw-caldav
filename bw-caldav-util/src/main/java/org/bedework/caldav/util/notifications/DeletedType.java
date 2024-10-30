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

/**
     <!ELEMENT deleted (DAV:href, changed-by?, deleted-details)>
 *
 * @author Mike Douglass douglm
 */
public class DeletedType extends BaseEntityChangeType {
  private DeletedDetailsType deletedDetails;

  /**
   * @param val the deletedDetails
   */
  public void setDeletedDetails(final DeletedDetailsType val) {
    deletedDetails = val;
  }

  /**
   * @return the deletedDetails
   */
  public DeletedDetailsType getDeletedDetails() {
    return deletedDetails;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  public DeletedType copyForAlias(final String collectionHref) {
    final DeletedType copy = new DeletedType();

    copyForAlias(copy, collectionHref);
    copy.deletedDetails = deletedDetails;

    return copy;
  }

  /**
   * @param xml builder
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.deleted);
    toXmlSegment(xml);

    getDeletedDetails().toXml(xml);
    xml.closeTag(AppleServerTags.deleted);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts builder
   */
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    getDeletedDetails().toStringSegment(ts);
  }
}
