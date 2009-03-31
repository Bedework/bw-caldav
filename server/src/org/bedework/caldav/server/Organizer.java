/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/

package org.bedework.caldav.server;

import java.io.Serializable;

/**
 * @author douglm
 *
 */
public class Organizer implements Serializable {
  /* Params fields */

  private String cn;
  private String dir;
  private String language;
  private String sentBy;

  /** The uri */
  private String organizerUri;

  /** Constructor
   *
   * @param cn
   * @param dir
   * @param language
   * @param sentBy
   * @param organizerUri
   */
  public Organizer(String cn,
                   String dir,
                   String language,
                   String sentBy,
                   String organizerUri) {
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
  public void setOrganizerUri(String val) {
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
    StringBuilder sb = new StringBuilder("BwOrganizer(");

    sb.append("cn=");
    sb.append(getCn());
    sb.append(", dir=");
    sb.append(getDir());
    sb.append(", language=");
    sb.append(getLanguage());
    sb.append(", sentBy=");
    sb.append(getSentBy());
    sb.append(", organizerUri=");
    sb.append(getOrganizerUri());
    sb.append("}");

    return sb.toString();
  }

}
