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
