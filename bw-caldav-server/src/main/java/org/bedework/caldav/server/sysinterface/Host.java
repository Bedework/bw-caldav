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

import java.util.List;


/** This class provides information about a host. This should eventually come
 * from some form of dns-like lookup based on the CUA.
 *
 * <p>Currently we are adding dynamic look-up and DKIM security to the model.
 * Even with that in place there will be a need for hard-wired connections, with
 * and without DKIM.
 *
 * <p>To increase security we should use some form of authentication. However,
 * if we use servlet authentication we need to create accounts to authenticate
 * against. Those accounts need to be given to administrators at other sites
 * which is probably unacceptable. On the other hand we can run it through the
 * unauthenticated service and check the id/pw ourselves.
 *
 * <p>The information here can be used for outgoing or can provide us with
 * information to handle incoming requests. For incoming we need to resolve the
 * host name and we then search for an entry prefixed with *IN*. We'll need to
 * progressively shorten the name by removing leading elements until we get a
 * match or there's nothing left. For example, if we get an incoming request
 * for cal.example.org we check:<ol>
 * <li>*IN*cal.example.org</li>
 * <li>*IN*example.org</li>
 * <li>*IN*org</li>
 * <li>*IN*</li>
 * </ul>
 *
 * <p>The last entry, if it exists, provides a default behavior. If absent we
 * disallow all unidentified incoming requests. If present they must satisfy the
 * requirements specified, e.g. DKIM
 *
 * @author Mike Douglass       douglm - bedework.edu
 */
public interface Host {
  /**
   *
   * @return String hostname
   */
  String getHostname();

  /**
   * @return int
   */
  int getPort();

  /**
   * @return String
   */
  boolean getSecure();

  /**
   *
   * @return boolean localService
   */
  boolean getLocalService();

  /**
   *
   * @return String
   */
  String getCaldavUrl();

  /**
   *
   * @return String
   */
  String getCaldavPrincipal();

  /**
   *
   * @return String
   */
  String getCaldavCredentials();

  /**
   *
   * @return String
   */
  String getIScheduleUrl();

  /**
   *
   * @return String
   */
  String getISchedulePrincipal();

  /**
   *
   * @return String
   */
  String getIScheduleCredentials();

  /** List of dkim public keys in form selector=key
   *
   * @return String
   */
  List<String> getDkimPublicKeys();

  /** True if we delivered our public key for use for dkim
   *
   * @return String
   */
  boolean getIScheduleUsePublicKey();

  /**
   *
   * @return String
   */
  String getFbUrl();

  /**
   *  @return boolean    true if caldav supported
   */
  boolean getSupportsBedework();

  /**
   *  @return boolean    true if caldav supported
   */
  boolean getSupportsCaldav();

  /**
   *  @return boolean    true if iSchedule supported
   */
  boolean getSupportsISchedule();

  /**
   *  @return boolean    true if Freebusy supported
   */
  boolean getSupportsFreebusy();
}
