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

import org.bedework.base.ToString;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class Organizer implements Serializable {
  /* Params fields */

  private final String cn;
  private final String dir;
  private final String language;
  private final String sentBy;

  /** The uri */
  private String organizerUri;

  /** Constructor
   *
   * @param cn common name
   * @param dir directory for lookup
   * @param language code
   * @param sentBy optional
   * @param organizerUri required calendar address
   */
  public Organizer(final String cn,
                   final String dir,
                   final String language,
                   final String sentBy,
                   final String organizerUri) {
    this.cn = cn;
    this.dir = dir;
    this.language = language;
    this.sentBy = sentBy;
    this.organizerUri = organizerUri;
  }

  /** Get the cn
   *
   *  @return String     cn
   */
  public String getCn() {
    return cn;
  }

  /** Get the dir
   *
   *  @return String     dir
   */
  public String getDir() {
    return dir;
  }

  /** Get the language
   *
   *  @return String     language
   */
  public String getLanguage() {
    return language;
  }

  /** Get the sentBy
   *
   *  @return String     sentBy
   */
  public String getSentBy() {
    return sentBy;
  }

  /** Set the organizerUri
   *
   *  @param  val   String organizerUri
   */
  public void setOrganizerUri(final String val) {
    organizerUri = val;
  }

  /** Get the organizerUri
   *
   *  @return String     organizerUri
   */
  public String getOrganizerUri() {
    return organizerUri;
  }

  public String toString() {
    return new ToString(this)
            .append("cn", getCn())
            .append("dir", getDir())
            .append("language", getLanguage())
            .append("sentBy", getSentBy())
            .append("organizerUri", getOrganizerUri())
            .toString();
  }

}
