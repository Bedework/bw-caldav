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
package org.bedework.caldav.server.sysinterface;

import org.bedework.util.jmx.MBeanInfo;

import java.io.Serializable;

/** These are the system properties that the server needs to know about, either
 * because it needs to apply these limits or just to report them to clients.
 *
 * @author douglm
 *
 */
public interface CalDAVSystemProperties extends Serializable {
  /** Set the feature flags property
   *
   * @param val feature flags
   */
  void setFeatureFlags(String val);

  /** Get the feature flags property
   *
   * @return String
   */
  @MBeanInfo("Feature flags - see documentation")
  String getFeatureFlags();

  /** Set the administrator contact property
   *
   * @param val administrator contact
   */
  void setAdminContact(String val);

  /** Get the administrator contact property
   *
   * @return String
   */
  @MBeanInfo("Administrator contact property")
  String getAdminContact();

  /** Set the timezones server uri
   *
   * @param val    String
   */
  void setTzServeruri(String val);

  /** Get the timezones server uri
   *
   * @return String   tzid
   */
  @MBeanInfo("the timezones server uri")
  String getTzServeruri();

  /**
   * @param val boolean true if we are not including the full tz specification..
   */
  void setTimezonesByReference(boolean val);

  /**
   * @return true if we are not including the full tz specification
   */
  @MBeanInfo("true if we are NOT including the full tz specification in iCalendar output")
  boolean getTimezonesByReference();

  /** Set the ischedule service uri - null for no ischedule service
   *
   * @param val    String
   */
  void setIscheduleURI(String val);

  /** get the ischedule service uri - null for no ischedule service
   *
   * @return String
   */
  @MBeanInfo("ischedule service uri - null for no ischedule service")
  String getIscheduleURI();

  /** Set the Free busy service uri - null for no freebusy service
   *
   * @param val    String
   */
  void setFburlServiceURI(String val);

  /** get the Free busy service uri - null for no freebusy service
   *
   * @return String
   */
  @MBeanInfo("Free busy service uri - null for no freebusy service")
  String getFburlServiceURI();

  /** Set the web calendar service uri - null for no web calendar service
   *
   * @param val    String
   */
  void setWebcalServiceURI(String val);

  /** get the web calendar service uri - null for no web calendar service
   *
   * @return String
   */
  @MBeanInfo("Web calendar service uri - null for no web calendar service")
  String getWebcalServiceURI();

  /** Set the calws soap web service uri - null for no service
   *
   * @param val    String
   */
  void setCalSoapWsURI(String val);

  /** Get the calws soap web service uri - null for no service
   *
   * @return String
   */
  @MBeanInfo("Calws soap web service uri - null for no service")
  String getCalSoapWsURI();

  /** Set the max number of items per vpoll. null for no limit
   *
   * @param val    Integer max
   */
  void setVpollMaxItems(Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number of items per vpoll. null for no limit")
  Integer getVpollMaxItems();

  /** Set the max number of active vpolls. null for no limit
   *
   * @param val    Integer max
   */
  void setVpollMaxActive(Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number of voters per vpolls. null for no limit")
  Integer getVpollMaxActive();

  /** Set the max number of active vpolls. null for no limit
   *
   * @param val    Integer max
   */
  void setVpollMaxVoters(Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number of voters per vpolls. null for no limit")
  Integer getVpollMaxVoters();
}
