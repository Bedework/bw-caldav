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

import org.bedework.util.calendar.PropertyIndex;
import org.bedework.util.calendar.PropertyIndex.ParameterInfoIndex;
import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;
import org.bedework.util.misc.Uid;

/** Base filter class for parameters.
 *
 * The name should be unique at least for a set of filters and unique for a
 * given owner if persisted.
 *
 * @author Mike Douglass douglm
 */
public class ParameterFilter extends FilterBase {
  private PropertyIndex.PropertyInfoIndex parentPropertyIndex;
  private ParameterInfoIndex parameterIndex;

  /**
   * @param name - null one will be created
   * @param parameterIndex
   */
  public ParameterFilter(String name, final ParameterInfoIndex parameterIndex) {
    super(name);
    if (name == null) {
      name = Uid.getUid();
      setName(name);
    }
    setParameterIndex(parameterIndex);
  }

  /**
   * @param val
   */
  public void setParameterIndex(final ParameterInfoIndex val) {
    parameterIndex = val;
  }

  /**
   * @return property index
   */
  public ParameterInfoIndex getParameterIndex() {
    return parameterIndex;
  }

  /** Parent property if this is a param
   *
   * @param val
   */
  public void setParentPropertyIndex(final PropertyInfoIndex val) {
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
   @Override
  protected void toStringSegment(final StringBuilder sb) {
    super.toStringSegment(sb);
    sb.append(", propertyIndex=");
    sb.append(getParameterIndex());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("PropertyFilter{");

    super.toStringSegment(sb);
    toStringSegment(sb);

    sb.append("}");

    return sb.toString();
  }
}
