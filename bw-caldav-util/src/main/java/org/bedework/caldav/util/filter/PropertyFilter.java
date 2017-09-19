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

import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Uid;

import java.util.Collections;
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
  private List<PropertyInfoIndex> propertyIndexes;
  
  private String strKey; 
  private Integer intKey;

  /**
   * @param name - null one will be created
   * @param propertyIndex identifies property
   */
  public PropertyFilter(String name, PropertyInfoIndex propertyIndex) {
    this(name, propertyIndex, null, null);
  }

  /**
   * @param name - null one will be created
   * @param propertyIndex identifies property
   * @param intKey non-null if property is indexed by the key
   * @param strKey non-null if ditto
   */
  public PropertyFilter(String name, PropertyInfoIndex propertyIndex,
                        final Integer intKey,
                        final String strKey) {
    super(name);
    if (name == null) {
      name = Uid.getUid();
      setName(name);
    }
    this.propertyIndex = propertyIndex;
    
    propertyIndexes = Collections.singletonList(propertyIndex);

    this.intKey = intKey;
    this.strKey = strKey;
  }

  /**
   * @param name - null one will be created
   * @param propertyIndexes identifies dot separated refs to property
   */
  public PropertyFilter(final String name,
                        final List<PropertyInfoIndex> propertyIndexes) {
    this(name, propertyIndexes, null, null);
  }

  /**
   * @param name - null one will be created
   * @param propertyIndexes identifies dot separated refs to property
   * @param intKey non-null if property is indexed by the key
   * @param strKey non-null if ditto
   */
  public PropertyFilter(final String name,
                        final List<PropertyInfoIndex> propertyIndexes,
                        final Integer intKey,
                        final String strKey) {
    super(name);

    if (name == null) {
      setName(Uid.getUid());
    }

    this.propertyIndexes = propertyIndexes;
    
    if (propertyIndexes.size() == 1) {
      propertyIndex = propertyIndexes.get(0);
      return;
    }

    if (propertyIndexes.size() != 2) {
      throw new RuntimeException("Not implemented - subfield depth > 2");
    }
    propertyIndex = propertyIndexes.get(1);
    setParentPropertyIndex(propertyIndexes.get(0));
    
    this.intKey = intKey;
    this.strKey = strKey;
  }

  /**
   * @param val the index
   */
  protected void setPropertyIndex(PropertyInfoIndex val) {
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
   * @param val the index
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
  
  public List<PropertyInfoIndex> getPropertyIndexes() {
    return propertyIndexes;
  }

  /**
   * @return integer key (index) or null
   */
  public Integer getIntKey() {
    return intKey;
  }

  /**
   * @return String key (index) or null
   */
  public String getStrKey() {
    return strKey;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuffer
   *
   * @param ts for result
   */
   @Override
   protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    ts.append("propertyIndex", getPropertyIndex());
  }

  public String toString() {
    final ToString ts = new ToString(this);

    toStringSegment(ts);

    return ts.toString();
  }
}
