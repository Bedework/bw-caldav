/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

package org.bedework.caldav.server.sysinterface;

import java.io.Serializable;

/** These are the system properties that the server needs to know about, either
 * because it needs to apply these limits or just to report them to clients.
 *
 * @author douglm
 *
 */
public class SystemProperties implements Serializable {
  private Integer maxUserEntitySize;

  private Integer maxInstances;

  private Integer maxAttendeesPerInstance;

  private String minDateTime;

  private String maxDateTime;

  private String adminContact;

  /** Set the max entity length for users. Probably an estimate. Null for no limit
   *
   * @param val    Integer max
   */
  public void setMaxUserEntitySize(final Integer val) {
    maxUserEntitySize = val;
  }

  /**
   *
   * @return Integer
   */
  public Integer getMaxUserEntitySize() {
    return maxUserEntitySize;
  }

  /** Set the max number recurrence instances. Null for no limit
   *
   * @param val    Integer max
   */
  public void setMaxInstances(final Integer val) {
    maxInstances = val;
  }

  /**
   *
   * @return Integer
   */
  public Integer getMaxInstances() {
    return maxInstances;
  }

  /** Set the max number attendees per instance. Null for no limit
   *
   * @param val    Integer max
   */
  public void setMaxAttendeesPerInstance(final Integer val) {
    maxAttendeesPerInstance = val;
  }

  /**
   *
   * @return Integer
   */
  public Integer getMaxAttendeesPerInstance() {
    return maxAttendeesPerInstance;
  }

  /**
   * @param val    minimum date time allowed - null for no limit
   */
  public void setMinDateTime(final String val) {
    minDateTime = val;
  }

  /**
   *
   * @return String   minimum date time allowed - null for no limit
   */
  public String getMinDateTime() {
    return minDateTime;
  }

  /**
   * @param val    minimum date time allowed - null for no limit
   */
  public void setMaxDateTime(final String val) {
    maxDateTime = val;
  }

  /**
   *
   * @return String   minimum date time allowed - null for no limit
   */
  public String getMaxDateTime() {
    return maxDateTime;
  }

  /** Set the administrator contact property
   *
   * @param val
   */
  public void setAdminContact(final String val) {
    adminContact = val;
  }

  /** Get the administrator contact property
   *
   * @return String
   */
  public String getAdminContact() {
    return adminContact;
  }
}
