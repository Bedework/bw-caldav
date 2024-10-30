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
import org.bedework.util.xml.tagdefs.AppleServerTags;

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
public class CollectionChangesType extends BaseEntityChangeType {
  private List<ChangedByType> changedByList;
  private PropType prop;
  private ChildCreatedType childCreated;
  private ChildUpdatedType childUpdated;
  private ChildDeletedType childDeleted;
  
  public List<ChangedByType> getChangedByList() {
    if (changedByList != null) {
      return changedByList;
    }
    
    changedByList = new ArrayList<>();
    return changedByList;
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

  public CollectionChangesType copyForAlias(final String collectionHref) {
    final CollectionChangesType copy = new CollectionChangesType();

    copyForAlias(copy, collectionHref);
    
    if (!Util.isEmpty(changedByList)) {
      copy.changedByList = new ArrayList<>(changedByList);
    }
    
    copy.prop = prop;
    copy.childCreated = childCreated;
    copy.childDeleted = childDeleted;
    copy.childUpdated = childUpdated;

    return copy;
  }

  /**
   * @param xml builder
   */
  public void toXml(final XmlEmit xml) {
    xml.openTag(AppleServerTags.collectionChanges);
    toXmlSegment(xml);

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
   * @param ts builder
   */
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

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
}
