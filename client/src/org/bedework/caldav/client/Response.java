/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.client;

import org.bedework.calfacade.svc.HostInfo;
import org.bedework.http.client.dav.DavResp;

import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Mike Douglass
 *
 */
public class Response implements Serializable {
  private HostInfo hostInfo;

  private int responseCode;

  private boolean noResponse;

  private Throwable exception;

  private DavResp cdresp;

  /**
   * @param val HostInfo
   */
  public void setHostInfo(HostInfo val) {
    hostInfo = val;
  }

  /**
   * @return HostInfo
   */
  public HostInfo getHostInfo() {
    return hostInfo;
  }

  /**
   * @param val int
   */
  public void setResponseCode(int val) {
    responseCode = val;
  }

  /**
   * @return int
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * @param val boolean
   */
  public void setNoResponse(boolean val) {
    noResponse = val;
  }

  /**
   * @return boolean
   */
  public boolean getNoResponse() {
    return noResponse;
  }

  /**
   * @param val Throwable
   */
  public void setException(Throwable val) {
    exception = val;
  }

  /**
   * @return Throwable
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * @param val
   */
  public void setCdresp(DavResp val) {
    cdresp = val;
  }

  /**
   * @return DavResp
   */
  public DavResp getCdresp() {
    return cdresp;
  }

  /**
   * @return boolean
   */
  public boolean okResponse() {
    return (getResponseCode() == HttpServletResponse.SC_OK) &&
           !getNoResponse() && (getException() == null);
  }
}
