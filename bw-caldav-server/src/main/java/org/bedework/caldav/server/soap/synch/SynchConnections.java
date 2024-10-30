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

import org.bedework.util.jmx.ConfBase;
import org.bedework.util.misc.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** This is a simple bean to handle the dynamic connections made between the
 * CalDAV server and synch engines. These connections are defined by no more
 * than an id and token.
 *
 * <p>They are stored in a table with a url as the key. The url is the callback
 * url of the synch service.
 *
 * <p>At this point I'm not sure how there can be more than one useful callback
 * url. How do we select which one to callback to?
 *
 * @author douglm
 */
public class SynchConnections extends ConfBase
        implements SynchConnectionsMBean {
  /* Name of the directory holding the config data */
  private static final String confDirName = "caldav";

  /* A map indexed by the url which identifies 'open' connections */
  static Map<String, SynchConnection> activeConnections =
      new HashMap<>();

  /* A map indexed by the id which identifies 'open' connections */
  static Map<String, SynchConnection> activeConnectionsById =
      new HashMap<>();

  public SynchConnections() {
    super(serviceName, confDirName, configName);
  }

  @Override
  public String loadConfig() {
    return "No config to load";
  }

  /* ==============================================================
   * Attributes
   * ============================================================== */

  /* ==============================================================
   * Operations
   * ============================================================== */

  @Override
  public void setConnection(final SynchConnection val) {
    activeConnections.put(val.getSubscribeUrl(), val);
    activeConnectionsById.put(val.getConnectorId(), val);
  }

  @Override
  public SynchConnection getConnection(final String callbackUrl) {
    return activeConnections.get(callbackUrl);
  }

  @Override
  public SynchConnection getConnectionById(final String id) {
    return activeConnectionsById.get(id);
  }

  @Override
  public String[] activeConnectionInfo() {
    final Collection<SynchConnection> conns = activeConnections.values();

    if (Util.isEmpty(conns)) {
      return new String[0];
    }

    final String[] res = new String[conns.size()];

    int i = 0;
    for (final SynchConnection sc: conns) {
      res[i] = sc.shortToString();
      i++;
    }

    return res;
  }
}
