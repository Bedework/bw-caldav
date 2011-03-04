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

  private int defaultFBPeriod = 31;

  private int maxFBPeriod = 32 * 3;

  private int defaultWebCalPeriod = 31;

  private int maxWebCalPeriod = 32 * 3;

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
   * @param val    maximum date time allowed - null for no limit
   */
  public void setMaxDateTime(final String val) {
    maxDateTime = val;
  }

  /**
   *
   * @return String   maximum date time allowed - null for no limit
   */
  public String getMaxDateTime() {
    return maxDateTime;
  }

  /** Set the c if not specified
   *
   * @param val
   */
  public void setDefaultFBPeriod(final int val) {
    defaultFBPeriod = val;
  }

  /** get the default freebusy fetch period if not specified
   *
   * @return int days
   */
  public int getDefaultFBPeriod() {
    return defaultFBPeriod;
  }

  /** Set the maximum freebusy fetch period
   *
   * @param val
   */
  public void setMaxFBPeriod(final int val) {
    maxFBPeriod = val;
  }

  /** get the maximum freebusy fetch period
   *
   * @return int days
   */
  public int getMaxFBPeriod() {
    return maxFBPeriod;
  }

  /** Set the default webcal fetch period if not specified
   *
   * @param val
   */
  public void setDefaultWebCalPeriod(final int val) {
    defaultWebCalPeriod = val;
  }

  /** Get the default webcal fetch period if not specified
   *
   * @return int days
   */
  public int getDefaultWebCalPeriod() {
    return defaultWebCalPeriod;
  }

  /** Set the maximum webcal fetch period
   *
   * @param val
   */
  public void setMaxWebCalPeriod(final int val) {
    maxWebCalPeriod = val;
  }

  /** Set the maximum webcal fetch period
   *
   * @return int days
   */
  public int getMaxWebCalPeriod() {
    return maxWebCalPeriod;
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
