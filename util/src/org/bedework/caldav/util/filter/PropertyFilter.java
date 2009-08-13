/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.caldav.util.filter;

import edu.rpi.cmt.calendar.PropertyIndex.PropertyInfoIndex;
import edu.rpi.sss.util.Uid;

/** Base filter class for properties.
 *
 * The name should be unique at least for a set of filters and unique for a
 * given owner if persisted.
 *
 * @author Mike Douglass douglm
 */
public class PropertyFilter extends Filter {
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
