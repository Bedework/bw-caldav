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

/** A filter that filters for property presence.
 *
 * The name should be unique at least for a set of filters and unique for a
 * given owner if persisted.
 *
 * @author Mike Douglass douglm
 */
public class PresenceFilter extends PropertyFilter {
  private boolean testPresent;

  /** Match on any of the entities.
   *
   * @param name - null one will be created
   * @param propertyIndex
   * @param testPresent   false for not present
   */
  public PresenceFilter(String name, PropertyInfoIndex propertyIndex,
                          boolean testPresent) {
    super(name, propertyIndex);
    this.testPresent = testPresent;
  }

  /** Set a test for present
   */
  public void setTestPresent() {
    testPresent = true;
  }

  /** See if we're testing for present
   *
   *  @return boolean true if test for present
   */
  public boolean getTestPresent() {
    return testPresent;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuilder sb = new StringBuilder("PresenceFilter{");

    super.toStringSegment(sb);
    if (getTestPresent()) {
      sb.append("\nproperty not null");
    } else {
      sb.append("\nproperty null");
    }

    sb.append("}");

    return sb.toString();
  }
}
