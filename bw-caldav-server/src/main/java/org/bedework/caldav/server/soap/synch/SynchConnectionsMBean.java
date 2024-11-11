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
package org.bedework.caldav.server.soap.synch;

import org.bedework.util.jmx.ConfBaseMBean;
import org.bedework.util.jmx.MBeanInfo;

/** Handle the dynamic connections made between bedework and synch engines.
 *
 * @author douglm
 */
public interface SynchConnectionsMBean extends ConfBaseMBean {
  String configName = "SynchConnections";

  String serviceName =
          "org.bedework.caldav:service=" + configName;

  /* ==============================================================
   * Attributes
   * ============================================================== */

  /* ==============================================================
   * Operations
   * ============================================================== */

  /** Put/update a connection
   *
   * @param val a connection
   */
  @MBeanInfo("Put/update a connection")
  void setConnection(SynchConnection val);

  /** Find a connection
   *
   * @param callbackUrl
   * @return a connection or null
   */
  @MBeanInfo("Get a connection")
  SynchConnection getConnection(String callbackUrl);

  /** Get a connection for outbound calls by id
   *
   * @param id
   * @return a connection or null
   */
  @MBeanInfo("get a connection by id")
  SynchConnection getConnectionById(String id);

  /**
   * @return list of connections
   */
  @MBeanInfo("List of connections")
  String[] activeConnectionInfo();
}
