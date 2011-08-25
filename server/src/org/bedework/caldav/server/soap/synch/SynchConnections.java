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

import edu.rpi.sss.util.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** This is a simple bean to handle the dynamic connections made between bedework
 * and synch engines. These connections are defined by no more than an id and token.
 *
 * <p>The are stored in a table with a url as the key. The url is the callback
 * url of the synch service.
 *
 * <p>At this point I'm not sure how there can be more than one useful callback
 * url. How do we selct which one to callback to?
 *
 * @author douglm
 */
public class SynchConnections implements SynchConnectionsMBean {
  private boolean started;

  /* A map indexed by the url which identifies 'open' connections */
  static Map<String, SynchConnection> activeConnections =
      new HashMap<String, SynchConnection>();


  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#getName()
   */
  public String getName() {
    /* This apparently must be the same as the name attribute in the
     * jboss service definition
     */
    return "org.bedework:service=DumpRestore";
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  public void setConnection(final SynchConnection val) {
    activeConnections.put(val.getSubscribeUrl(), val);
  }

  public SynchConnection getConnection(final String callbackUrl) {
    return activeConnections.get(callbackUrl);
  }

  public String[] activeConnectionInfo() {
    Collection<SynchConnection> conns = activeConnections.values();

    if (Util.isEmpty(conns)) {
      return new String[0];
    }

    String[] res = new String[conns.size()];

    int i = 0;
    for (SynchConnection sc: conns) {
      res[i] = sc.shortToString();
      i++;
    }

    return res;
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#create()
   */
  public void create() {
    // An opportunity to initialise
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#start()
   */
  public void start() {
    started = true;
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#stop()
   */
  public void stop() {
    started = false;
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#isStarted()
   */
  public boolean isStarted() {
    return started;
  }

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#destroy()
   */
  public void destroy() {
  }
}
