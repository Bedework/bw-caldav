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
package org.bedework.caldav.util;

import java.io.Serializable;

/** This class defines the various properties we need for a caldav server
 *
 * @author Mike Douglass
 */
public class CalDAVConfig implements Serializable {
  /* For bedework build */
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

  /* Exchange synch web service uri - null for no service */
  private String exsynchWsURI;

  /* Set at server init */
  private boolean calWS;

  private boolean timezonesByReference;

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

  /** Set the exsynch web service uri - null for no service
   *
   * @param val    String
   */
  public void setExsynchWsURI(final String val) {
    exsynchWsURI = val;
  }

  /** Get the exsynch web service uri - null for no service
   *
   * @return String
   */
  public String getExsynchWsURI() {
    return exsynchWsURI;
  }

  /** True for a web service - set by server..
   *
   * @param val
   */
  public void setCalWS(final boolean val) {
    calWS = val;

    if (val) {
      setTimezonesByReference(true);
    }
  }

  /**
   * @return boolean
   */
  public boolean getCalWS() {
    return calWS;
  }

  /**
   * @param val boolean true if we are not including the full tz specification..
   */
  public void setTimezonesByReference(final boolean val) {
    timezonesByReference = val;
  }

  /**
   * @return true if we are not including the full tz specification
   */
  public boolean getTimezonesByReference() {
    return timezonesByReference;
  }
}
