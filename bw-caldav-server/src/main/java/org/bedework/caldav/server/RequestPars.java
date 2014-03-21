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

import org.bedework.caldav.server.sysinterface.CalDAVSystemProperties;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.webdav.servlet.common.PostRequestPars;
import org.bedework.webdav.servlet.shared.WebdavException;

import javax.servlet.http.HttpServletRequest;

/**
 */
public class RequestPars extends PostRequestPars {
  private IscheduleIn ischedRequest;

  private SysiIcalendar ic;

  private CalDAVCollection col;

  private boolean share;

  private boolean iSchedule;

  private boolean freeBusy;

  private boolean webcal;

  private boolean webcalGetAccept;

  private boolean synchws;

  private boolean calwsSoap;

  /**
   * @param req - the request
   * @param intf service interface
   * @param resourceUri the uri
   * @throws WebdavException
   */
  public RequestPars(final HttpServletRequest req,
                     final CaldavBWIntf intf,
                     final String resourceUri) throws WebdavException {
    super(req, intf, resourceUri);

    final SysIntf sysi = intf.getSysi();

    final CalDAVSystemProperties sp = sysi.getSystemProperties();

    testRequest: {
      if (processRequest()) {
        break testRequest;
      }

      iSchedule = checkUri(sp.getIscheduleURI());

      if (iSchedule) {
        ischedRequest = new IscheduleIn(req, sysi.getUrlHandler());

        getTheReader = false;
        break testRequest;
      }

      freeBusy = checkUri(sp.getFburlServiceURI());

      if (freeBusy) {
        getTheReader = false;
        break testRequest;
      }

      webcal = checkUri(sp.getWebcalServiceURI());

      if (webcal) {
        getTheReader = false;
        break testRequest;
      }

      // not ischedule or freeBusy or webcal

      if (intf.getCalWS()) {
        // POST of entity for create? - same as DAV:add-member
        if ("create".equals(req.getParameter("action"))) {
          addMember = true;
        }
        break testRequest;
      }

      if (intf.getSynchWsURI() != null) {
        synchws = intf.getSynchWsURI().equals(resourceUri);
        if (synchws) {
          getTheReader = false;
          break testRequest;
        }
      }

      if (sp.getCalSoapWsURI() != null) {
        calwsSoap = sp.getCalSoapWsURI().equals(resourceUri);
        if (calwsSoap) {
          getTheReader = false;
          break testRequest;
        }
      }

      /* Not any of the special URIs - this could be a post aimed at one of
       * our caldav resources.
       */
      processXml();
    } // testRequest

    getReader();
  }

  /**
   * @return  Special parameters for iSchedule
   */
  public IscheduleIn getIschedRequest() {
    return ischedRequest;
  }

  /**
   * @return true if this is a calws soap web service request
   */
  public boolean isCalwsSoap() {
    return calwsSoap;
  }

  /**
   * @return true if this is an synch web service request
   */
  public boolean isSynchws() {
    return synchws;
  }

  /**
   * @return true if this is a web calendar request
   */
  public boolean isWebcal() {
    return webcal;
  }

  /**
   * @param val true if this is a web calendar request with GET + ACCEPT
   */
  public void setWebcalGetAccept(final boolean val) {
    webcalGetAccept = val;
  }

  /**
   * @return true if this is a web calendar request with GET + ACCEPT
   */
  public boolean isWebcalGetAccept() {
    return webcalGetAccept;
  }

  /**
   * @return true if this is a free busy request
   */
  public boolean isFreeBusy() {
    return freeBusy;
  }

  /**
   * @return true if this is an iSchedule request
   */
  public boolean isiSchedule() {
    return iSchedule;
  }

  /**
   * @return true if this is a CalDAV share request
   */
  public boolean isShare() {
    return share;
  }

  /**
   * @param val a collection
   */
  public void setCol(final CalDAVCollection val) {
    col = val;
  }

  /**
   * @return a collection
   */
  public CalDAVCollection getCol() {
    return col;
  }

  /**
   * @param val SysiCalendar object
   */
  public void setIcalendar(final SysiIcalendar val) {
    ic = val;
  }

  /**
   * @return SysiCalendar object
   */
  public SysiIcalendar getIcalendar() {
    return ic;
  }
}