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
import org.bedework.webdav.servlet.shared.UrlPrefixer;
import org.bedework.webdav.servlet.shared.UrlUnprefixer;

import java.util.ArrayList;
import java.util.List;

/**
     <!ELEMENT collection-changes (DAV:href, changed-by*, DAV:prop?,
                                   child-created?, child-updated?,
                                   child-deleted?>
       <!-- When coalescing changes from multiple users, the changed-by
            element can appear more than once. -->
 *
 * @author Mike Douglass douglm
 */
public class CollectionChangesType {
  private String href;
  private List<ChangedByType> changedBy;
  private PropType prop;
  private ChildCreatedType childCreated;
  private ChildUpdatedType childUpdated;
  private ChildDeletedType childDeleted;

  /** href of changed collection
   *
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

  /** The DAV:prop element indicates a change to WebDAV properties on the
   * calendar collection resource.
   *
   * @param val the prop
   */
  public void setProp(final PropType val) {
    prop = val;
  }

  /**
   * @return the prop
   */
  public PropType getProp() {
    return prop;
  }

  /**
   * @return the changedBy list
   */
  public List<ChangedByType> getChangedBy() {
    if (changedBy == null) {
      changedBy = new ArrayList<ChangedByType>();
    }

    return changedBy;
  }

  /**
   * @param val the childCreated element
   */
  public void setChildCreated(final ChildCreatedType val) {
    childCreated = val;
  }

  /**
   * @return the childCreated
   */
  public ChildCreatedType getChildCreated() {
    return childCreated;
  }

  /**
   * @param val the childUpdated element
   */
  public void setChildUpdated(final ChildUpdatedType val) {
    childUpdated = val;
  }

  /**
   * @return the childUpdated
   */
  public ChildUpdatedType getChildUpdated() {
    return childUpdated;
  }

  /**
   * @param val the childDeleted element
   */
  public void setChildDeleted(final ChildDeletedType val) {
    childDeleted = val;
  }

  /**
   * @return the childDeleted
   */
  public ChildDeletedType getChildDeleted() {
    return childDeleted;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Called before we send it out via caldav
   *
   * @param prefixer
   * @throws Throwable
   */
  public void prefixHrefs(final UrlPrefixer prefixer) throws Throwable {
    setHref(prefixer.prefix(getHref()));
  }

  /** Called after we obtain it via caldav
   *
   * @param unprefixer
   * @throws Throwable
   */
  public void unprefixHrefs(final UrlUnprefixer unprefixer) throws Throwable {
    setHref(unprefixer.unprefix(getHref()));
  }

  /**
   * @param xml
   * @throws Throwable
   */
  public void toXml(final XmlEmit xml) throws Throwable {
    xml.openTag(AppleServerTags.collectionChanges);
    for (ChangedByType cb: getChangedBy()) {
      cb.toXml(xml);
    }

    if (getProp() != null) {
      getProp().toXml(xml);
    }

    if (getChildCreated() != null) {
      getChildCreated().toXml(xml);
    }

    if (getChildUpdated() != null) {
      getChildUpdated().toXml(xml);
    }

    if (getChildDeleted() != null) {
      getChildDeleted().toXml(xml);
    }

    xml.closeTag(AppleServerTags.collectionChanges);
  }

  /** Add our stuff to the StringBuffer
   *
   * @param ts
   */
  protected void toStringSegment(final ToString ts) {
    for (ChangedByType cb: getChangedBy()) {
      cb.toStringSegment(ts);
    }

    if (getProp() != null) {
      getProp().toStringSegment(ts);
    }

    if (getChildCreated() != null) {
      getChildCreated().toStringSegment(ts);
    }

    if (getChildUpdated() != null) {
      getChildUpdated().toStringSegment(ts);
    }

    if (getChildDeleted() != null) {
      getChildDeleted().toStringSegment(ts);
    }
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
