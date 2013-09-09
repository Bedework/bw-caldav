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
package org.bedework.caldav.server;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import javax.servlet.http.HttpServletRequest;

/** Retrieve and process CalDAV header values
 *
 *   @author Mike Douglass   douglm  bedework.edu
 */
public class CalDavHeaders {
  /** Identifies the caller - probably only trust for super user */
  public static final String clientId = "Client-Id";

  /** Ask to run as somebody else - probably only trust for super user -
   * or if they have full proxy access? */
  public static final String runAs = "Run-As";

  /** Look for the Schedule-Reply header
   *
   * @param req    HttpServletRequest
   * @return boolean true if present
   * @throws WebdavException
   */
  public static boolean scheduleReply(final HttpServletRequest req)
          throws WebdavException {
    String hdrStr = req.getHeader("Schedule-Reply");

    if (hdrStr == null) {
      return true;
    }

    return "T".equals(hdrStr);
  }

  /** Run as somebody?
   *
   * @param req    HttpServletRequest
   * @return String non-null if present with value
   * @throws WebdavException
   */
  public static String getRunAs(final HttpServletRequest req)
          throws WebdavException {
    return req.getHeader(runAs);
  }

  /** Have a client-id?
   *
   * @param req    HttpServletRequest
   * @return String non-null if present with value
   * @throws WebdavException
   */
  public static String getClientId(final HttpServletRequest req)
          throws WebdavException {
    return req.getHeader(clientId);
  }

  /** Is this the scheduling assistant?
   *
   * @param req    HttpServletRequest
   * @return boolean true if present
   * @throws WebdavException
   */
  public static boolean isSchedulingAssistant(final HttpServletRequest req)
          throws WebdavException {
    String cid = getClientId(req);

    if (cid == null) {
      return false;
    }

    return "Jasig Scheduling Assistant".equals(cid);
  }

}
