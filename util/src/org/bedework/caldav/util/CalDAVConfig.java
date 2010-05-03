/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.util;

import java.io.Serializable;

/** This class defines the various properties we need for a caldav server
 *
 * @author Mike Douglass
 */
public class CalDAVConfig implements Serializable {
  /* Fro bedework build */
  private String appType;

  private boolean guestMode;

  private boolean doScheduleTag;

  /* System interface implementation */
  private String sysintfImpl;

  /* ischedule service uri - null for no ischedule service */
  private String ischeduleURI;

  /* Free busy service uri - null for no freebusy service */
  private String fburlServiceURI;

  /* Web calendar service uri - null for no web calendar service */
  private String webcalServiceURI;

  /**
   * @param val
   */
  public void setAppType(final String val) {
    appType = val;
  }

  /**
   * @return String
   */
  public String getAppType() {
    return appType;
  }

  /** True for a guest mode (non-auth) client.
   *
   * @param val
   */
  public void setGuestMode(final boolean val) {
    guestMode = val;
  }

  /**
   * @return boolean
   */
  public boolean getGuestMode() {
    return guestMode;
  }

  /** True for schedule tag support.
   *
   * @param val
   */
  public void setDoScheduleTag(final boolean val) {
    doScheduleTag = val;
  }

  /**
   * @return boolean
   */
  public boolean getDoScheduleTag() {
    return doScheduleTag;
  }

  /** Set the System interface implementation
   *
   * @param val    String
   */
  public void setSysintfImpl(final String val) {
    sysintfImpl = val;
  }

  /** get the System interface implementation
   *
   * @return String
   */
  public String getSysintfImpl() {
    return sysintfImpl;
  }

  /** Set the ischedule service uri - null for no ischedule service
   *
   * @param val    String
   */
  public void setIscheduleURI(final String val) {
    ischeduleURI = val;
  }

  /** get the ischedule service uri - null for no ischedule service
   *
   * @return String
   */
  public String getIscheduleURI() {
    return ischeduleURI;
  }

  /** Set the Free busy service uri - null for no freebusy service
   *
   * @param val    String
   */
  public void setFburlServiceURI(final String val) {
    fburlServiceURI = val;
  }

  /** get the Free busy service uri - null for no freebusy service
   *
   * @return String
   */
  public String getFburlServiceURI() {
    return fburlServiceURI;
  }

  /** Set the web calendar service uri - null for no web calendar service
   *
   * @param val    String
   */
  public void setWebcalServiceURI(final String val) {
    webcalServiceURI = val;
  }

  /** get the web calendar service uri - null for no web calendar service
   *
   * @return String
   */
  public String getWebcalServiceURI() {
    return webcalServiceURI;
  }
}
