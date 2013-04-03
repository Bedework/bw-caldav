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

import java.util.Date;


/** This represents an active connection to a synch engine.
 *
 * @author douglm
 */
public class SynchConnection {
  private String connectorId;

  private String subscribeUrl;

  private String synchToken;

  private long lastPing;

  /**
   * @param connectorId
   * @param subscribeUrl
   * @param synchToken
   */
  public SynchConnection(final String connectorId,
                         final String subscribeUrl,
                         final String synchToken) {
    this.connectorId = connectorId;
    this.subscribeUrl = subscribeUrl;
    this.synchToken = synchToken;
  }

  /**
   * @param val connector id
   */
  public void setConnectorId(final String val) {
    connectorId = val;
  }

  /**
   * @return the connector id
   */
  public String getConnectorId() {
    return connectorId;
  }

  /**
   * @param val the subscribeUrl to set
   */
  public void setSubscribeUrl(final String val) {
    subscribeUrl = val;
  }

  /**
   * @return the subscribeUrl
   */
  public String getSubscribeUrl() {
    return subscribeUrl;
  }

  /**
   * @param val the synchToken to set
   */
  public void setSynchToken(final String val) {
    synchToken = val;
  }

  /**
   * @return the synchToken
   */
  public String getSynchToken() {
    return synchToken;
  }

  /**
   * @param val the lastPing to set
   */
  public void setLastPing(final long val) {
    lastPing = val;
  }

  /**
   * @return the lastPing
   */
  public long getLastPing() {
    return lastPing;
  }

  /**
   * @return short string for display
   */
  public String shortToString() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");

    sb.append("id: \"");
    sb.append(getConnectorId());

    sb.append("\", url: \"");
    sb.append(getSubscribeUrl());

    sb.append("\", token: \"");
    sb.append(getSynchToken());

    if (getLastPing() != 0) {
      sb.append("\", ping: ");
      sb.append(new Date(getLastPing()).toString());
    }

    sb.append("}");

    return sb.toString();
  }
}
