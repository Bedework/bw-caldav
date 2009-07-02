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

  /* System interface implementation */
  private String sysintfImpl;

  /* Real time service uri - null for no real time service */
  private String realTimeServiceURI;

  /* Free busy service uri - null for no freebusy service */
  private String fburlServiceURI;

  /* Web calendar service uri - null for no web calendar service */
  private String webcalServiceURI;

  /**
   * @param val
   */
  public void setAppType(String val) {
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
  public void setGuestMode(boolean val) {
    guestMode = val;
  }

  /**
   * @return boolean
   */
  public boolean getGuestMode() {
    return guestMode;
  }

  /** Set the System interface implementation
   *
   * @param val    String
   */
  public void setSysintfImpl(String val) {
    sysintfImpl = val;
  }

  /** get the System interface implementation
   *
   * @return String
   */
  public String getSysintfImpl() {
    return sysintfImpl;
  }

  /** Set the Real time service uri - null for no real time service
   *
   * @param val    String
   */
  public void setRealTimeServiceURI(String val) {
    realTimeServiceURI = val;
  }

  /** get the Real time service uri - null for no real time service
   *
   * @return String
   */
  public String getRealTimeServiceURI() {
    return realTimeServiceURI;
  }

  /** Set the Free busy service uri - null for no freebusy service
   *
   * @param val    String
   */
  public void setFburlServiceURI(String val) {
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
  public void setWebcalServiceURI(String val) {
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
