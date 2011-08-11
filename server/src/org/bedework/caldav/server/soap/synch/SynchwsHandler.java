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

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.soap.calws.CalwsHandler;
import org.bedework.caldav.server.soap.calws.Report;
import org.bedework.synch.wsmessages.GetSynchInfoType;
import org.bedework.synch.wsmessages.ObjectFactory;
import org.bedework.synch.wsmessages.StartServiceNotificationType;
import org.bedework.synch.wsmessages.StartServiceResponseType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.synch.wsmessages.SynchInfoResponseType;
import org.bedework.synch.wsmessages.SynchInfoResponseType.SynchInfoResponses;
import org.bedework.synch.wsmessages.SynchInfoType;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import org.oasis_open.docs.ns.wscal.calws_soap.BaseRequestType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class SynchwsHandler extends CalwsHandler {
  /** This represents an active connection to a synch engine. It's possible we
   * would have more than one of these running I guess. For the moment we'll
   * only have one but these probably need a table indexed by url.
   *
   */
  class ActiveConnectionInfo {
    String subscribeUrl;

    String synchToken;
  }

  static ActiveConnectionInfo activeConnection;

  private ObjectFactory of = new ObjectFactory();

  /**
   * @param intf
   * @throws WebdavException
   */
  public SynchwsHandler(final CaldavBWIntf intf) throws WebdavException {
    super(intf);
  }

  @Override
  protected String getJaxbContextPath() {
    return "org.bedework.exsynch.wsmessages";
  }

  /**
   * @param req
   * @param resp
   * @param pars
   * @throws WebdavException
   */
  @Override
  public void processPost(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final RequestPars pars) throws WebdavException {
    try {
      Object o = unmarshal(req);

      SynchIdTokenType idToken = new SynchIdTokenType(); // Actually from message

      if (idToken != null) {
        handleIdToken(req, idToken);
      }

      if (o instanceof JAXBElement) {
        o = ((JAXBElement)o).getValue();
      }

      if (o instanceof BaseRequestType) {
        processRequest(req, resp,
                       (BaseRequestType)o,
                       pars,
                       false);
        return;
      }

      if (o instanceof GetSynchInfoType) {
        doGetSycnchInfo((GetSynchInfoType)o, req, resp);
        return;
      }

      if (o instanceof StartServiceNotificationType) {
        doStartService((StartServiceNotificationType)o, resp);
        return;
      }

      throw new WebdavException("Unhandled request");
    } catch (WebdavException we) {
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void doStartService(final StartServiceNotificationType ssn,
                              final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("StartServiceNotification: url=" + ssn.getSubscribeUrl() +
            "\n                token=" + ssn.getToken());
    }

    synchronized (monitor) {
      if (activeConnection == null) {
        activeConnection = new ActiveConnectionInfo();
      }

      activeConnection.subscribeUrl = ssn.getSubscribeUrl();
      activeConnection.synchToken = ssn.getToken();
    }

    notificationResponse(resp, true);
  }

  private void handleIdToken(final HttpServletRequest req,
                             final SynchIdTokenType idToken) throws WebdavException {
    getIntf().reAuth(req, idToken.getPrincipalHref());

    if (!idToken.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }
  }

  private void notificationResponse(final HttpServletResponse resp,
                                    final boolean ok) throws WebdavException {
    try {
      resp.setCharacterEncoding("UTF-8");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml; charset=UTF-8");

      StartServiceResponseType ssr = of.createStartServiceResponseType();

      if (ok) {
        ssr.setStatus(StatusType.OK);
      } else {
        ssr.setStatus(StatusType.ERROR);
      }

      ssr.setToken(activeConnection.synchToken);

      marshal(ssr, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doGetSycnchInfo(final GetSynchInfoType gsi,
                               final HttpServletRequest req,
                               final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetSycnchInfo: cal=" + gsi.getCalendarHref());
    }

    SynchInfoResponseType sir = new SynchInfoResponseType();

    sir.setCalendarHref(gsi.getCalendarHref());

    WebdavNsNode calNode = getNsIntf().getNode(gsi.getCalendarHref(),
                                               WebdavNsIntf.existanceMust,
                                               WebdavNsIntf.nodeTypeCollection);

    if (calNode == null) {
      // Drop this subscription
      throw new WebdavException("Unreachable " + gsi.getCalendarHref());
    }

    List<SynchInfoType> sis = new Report(nsIntf).query(gsi.getCalendarHref());

    SynchInfoResponses sirs = new SynchInfoResponses();
    sir.setSynchInfoResponses(sirs);
    sirs.getSynchInfo().addAll(sis);

    try {
      marshal(sir, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
