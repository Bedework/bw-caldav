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
package org.bedework.caldav.util.filter;

import org.bedework.util.misc.Uid;

import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;

import java.util.List;

/** Base filter class for properties.
 *
 * The name should be unique at least for a set of filters and unique for a
 * given owner if persisted.
 *
 * @author Mike Douglass douglm
 */
public class PropertyFilter extends FilterBase {
  private PropertyInfoIndex parentPropertyIndex;
  private PropertyInfoIndex propertyIndex;

  /**
   * @param name - null one will be created
   * @param propertyIndex
   */
  public PropertyFilter(String name, PropertyInfoIndex propertyIndex) {
    super(name);
    if (name == null) {
      name = Uid.getUid();
      setName(name);
    }
    setPropertyIndex(propertyIndex);
  }

  /**
   * @param name - null one will be created
   * @param propertyIndexes
   */
  public PropertyFilter(final String name,
                        final List<PropertyInfoIndex> propertyIndexes) {
    super(name);

    if (name == null) {
      setName(Uid.getUid());
    }

    if (propertyIndexes.size() == 1) {
      setPropertyIndex(propertyIndexes.get(0));
      return;
    }

    if (propertyIndexes.size() != 2) {
      throw new RuntimeException("Not implemented - subfield depth > 2");
    }
    setPropertyIndex(propertyIndexes.get(1));
    setParentPropertyIndex(propertyIndexes.get(0));
  }

  /**
   * @param val
   */
  public void setPropertyIndex(PropertyInfoIndex val) {
    propertyIndex = val;
  }

  /**
   * @return property index
   */
  public PropertyInfoIndex getPropertyIndex() {
    return propertyIndex;
  }

  /** Parent property if this is a param
   *
   * @param val
   */
  public void setParentPropertyIndex(PropertyInfoIndex val) {
    parentPropertyIndex = val;
  }

  /**
   * @return parent Property index
   */
  public PropertyInfoIndex getParentPropertyIndex() {
    return parentPropertyIndex;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param sb    StringBuffer for result
   */
   protected void toStringSegment(StringBuilder sb) {
    super.toStringSegment(sb);
    sb.append(", propertyIndex=");
    sb.append(getPropertyIndex());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("PropertyFilter{");

    super.toStringSegment(sb);
    toStringSegment(sb);

    sb.append("}");

    return sb.toString();
  }
}
