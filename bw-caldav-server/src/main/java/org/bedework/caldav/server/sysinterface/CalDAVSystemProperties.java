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

import edu.rpi.cmt.jmx.MBeanInfo;

import java.io.Serializable;

/** These are the system properties that the server needs to know about, either
 * because it needs to apply these limits or just to report them to clients.
 *
 * @author douglm
 *
 */
public interface CalDAVSystemProperties extends Serializable {
  /** Set the max entity length for users. Probably an estimate. null for no limit
   *
   * @param val    Integer max
   */
  void setMaxUserEntitySize(final Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max entity length for users. Probably an estimate. null for no limit")
  Integer getMaxUserEntitySize();

  /** Set the max number recurrence instances. null for no limit
   *
   * @param val    Integer max
   */
  void setMaxInstances(final Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number recurrence instances. null for no limit")
  Integer getMaxInstances();

  /** Set the max number attendees per instance. null for no limit
   *
   * @param val    Integer max
   */
  void setMaxAttendeesPerInstance(final Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number attendees per instance. null for no limit")
  Integer getMaxAttendeesPerInstance();

  /**
   * @param val    minimum date time allowed - null for no limit
   */
  void setMinDateTime(final String val);

  /**
   *
   * @return String   minimum date time allowed - null for no limit
   */
  @MBeanInfo("Minimum date time allowed. null for no limit")
  String getMinDateTime();

  /**
   * @param val    maximum date time allowed - null for no limit
   */
  void setMaxDateTime(final String val);

  /**
   *
   * @return String   maximum date time allowed - null for no limit
   */
  @MBeanInfo("Maximum date time allowed. null for no limit")
  String getMaxDateTime();

  /** Set the default freebusy fetch period - null if not specified
   *
   * @param val
   */
  void setDefaultFBPeriod(final Integer val);

  /** get the default freebusy fetch period - null if not specified
   *
   * @return Integer days
   */
  @MBeanInfo("Default freebusy fetch period. null for no limit")
  Integer getDefaultFBPeriod();

  /** Set the maximum freebusy fetch period
   *
   * @param val
   */
  void setMaxFBPeriod(final Integer val);

  /** get the maximum freebusy fetch period
   *
   * @return Integer days
   */
  @MBeanInfo("Maximum freebusy fetch period.")
  Integer getMaxFBPeriod();

  /** Set the default webcal fetch period null if not specified
   *
   * @param val
   */
  void setDefaultWebCalPeriod(final Integer val);

  /** Get the default webcal fetch period null if not specified
   *
   * @return Integer days
   */
  @MBeanInfo("Default webcal fetch period. null for no limit")
  Integer getDefaultWebCalPeriod();

  /** Set the maximum webcal fetch period
   *
   * @param val
   */
  void setMaxWebCalPeriod(final Integer val);

  /** Set the maximum webcal fetch period
   *
   * @return Integer days
   */
  @MBeanInfo("Maximum webcal fetch period. null for no limit")
  Integer getMaxWebCalPeriod();

  /** Set the administrator contact property
   *
   * @param val
   */
  void setAdminContact(final String val);

  /** Get the administrator contact property
   *
   * @return String
   */
  @MBeanInfo("Administrator contact property")
  String getAdminContact();

  /** Set the ischedule service uri - null for no ischedule service
   *
   * @param val    String
   */
  void setIscheduleURI(final String val);

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
  void setFburlServiceURI(final String val);

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
  void setWebcalServiceURI(final String val);

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
  void setCalSoapWsURI(final String val);

  /** Get the calws soap web service uri - null for no service
   *
   * @return String
   */
  @MBeanInfo("Calws soap web service uri - null for no service")
  String getCalSoapWsURI();

  /** Set the directoryBrowsingDisallowed flag
   *
   * @param val    boolean directoryBrowsingDisallowed
   */
  void setDirectoryBrowsingDisallowed(final boolean val);

  /**
   *
   * @return boolean
   */
  @MBeanInfo("true if directory browsing is NOT allowed")
  boolean getDirectoryBrowsingDisallowed();

  /** Set the max number of items per vpoll. null for no limit
   *
   * @param val    Integer max
   */
  void setVpollMaxItems(final Integer val);

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
  void setVpollMaxActive(final Integer val);

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
  void setVpollMaxVoters(final Integer val);

  /**
   *
   * @return Integer
   */
  @MBeanInfo("Max number of voters per vpolls. null for no limit")
  Integer getVpollMaxVoters();
}
