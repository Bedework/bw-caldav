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
import org.bedework.caldav.server.RequestPars;
import org.bedework.caldav.server.soap.calws.CalwsHandler;
import org.bedework.synch.wsmessages.KeepAliveNotificationType;
import org.bedework.synch.wsmessages.KeepAliveResponseType;
import org.bedework.synch.wsmessages.ObjectFactory;
import org.bedework.synch.wsmessages.StartServiceNotificationType;
import org.bedework.synch.wsmessages.StartServiceResponseType;
import org.bedework.synch.wsmessages.SynchIdTokenType;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.oasis_open.docs.ws_calendar.ns.soap.BaseRequestType;
import org.oasis_open.docs.ws_calendar.ns.soap.StatusType;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class SynchwsHandler extends CalwsHandler {
  private final ObjectFactory of = new ObjectFactory();

  /**
   * @param intf caldav interface
   * @throws WebdavException
   */
  public SynchwsHandler(final CaldavBWIntf intf) throws WebdavException {
    super(intf);
  }

  @Override
  protected String getJaxbContextPath() {
    return "org.bedework.synch.wsmessages";
  }

  /**
   * @param req http request
   * @param resp http response
   * @param pars processed request parameters
   * @throws WebdavException
   */
  @Override
  public void processPost(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final RequestPars pars) throws WebdavException {
    try {
      initResponse(resp);
      final UnmarshalResult ur = unmarshal(req);

      Object body = ur.body;
      if (body instanceof JAXBElement) {
        body = ((JAXBElement)body).getValue();
      }

      if (body instanceof StartServiceNotificationType) {
        doStartService((StartServiceNotificationType)body, resp);
        return;
      }

      if (body instanceof KeepAliveNotificationType) {
        doKeepAlive((KeepAliveNotificationType)body, resp);
        return;
      }

      SynchIdTokenType idToken = null;
      Object o;
      if ((ur.hdrs != null) && (ur.hdrs.length == 1)) {
        o = ur.hdrs[0];
        if (o instanceof JAXBElement) {
          o = ((JAXBElement<?>)o).getValue();
        }
        if (o instanceof SynchIdTokenType) {
          idToken = (SynchIdTokenType)o;
        }
      }

      if (idToken != null) {
        handleIdToken(req, idToken);
      }

      if (body instanceof BaseRequestType) {
        processRequest(req, resp,
                       (BaseRequestType)body,
                       pars,
                       false);
        return;
      }

      throw new WebdavException("Unhandled request");
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private void doStartService(final StartServiceNotificationType ssn,
                              final HttpServletResponse resp) throws WebdavException {
    try {
      if (debug()) {
        debug("StartServiceNotification: url=" + ssn.getSubscribeUrl());
      }

      SynchConnection sc = getActiveConnection(ssn.getSubscribeUrl());

      if (sc == null) {
        sc = new SynchConnection(ssn.getConnectorId(),
                                 ssn.getSubscribeUrl(),
                                 UUID.randomUUID().toString());
      } else {
        sc.setSynchToken(UUID.randomUUID().toString());
      }

      sc.setLastPing(System.currentTimeMillis());
      setActiveConnection(sc);

      startServiceResponse(resp, sc, true);
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doKeepAlive(final KeepAliveNotificationType kan,
                           final HttpServletResponse resp) throws WebdavException {
    if (debug()) {
      debug("KeepAliveNotification: url=" + kan.getSubscribeUrl() +
            "\n                token=" + kan.getToken());
    }

    try {
      synchronized (monitor) {
        final KeepAliveResponseType kar = of.createKeepAliveResponseType();

        final SynchConnection sc = getActiveConnection(kan.getSubscribeUrl());

        if (sc == null) {
          kar.setStatus(StatusType.NOT_FOUND);
        } else if (!sc.getSynchToken().equals(kan.getToken())) {
          kar.setStatus(StatusType.ERROR);
        } else {
          kar.setStatus(StatusType.OK);
          sc.setLastPing(System.currentTimeMillis());
          setActiveConnection(sc);
        }

        final JAXBElement<KeepAliveResponseType> jax = of.createKeepAliveResponse(kar);

        marshal(jax, resp.getOutputStream());
      }
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void handleIdToken(final HttpServletRequest req,
                             final SynchIdTokenType idToken) throws WebdavException {
    try {
      if (idToken.getPrincipalHref() != null) {
        getIntf().reAuth(req, idToken.getPrincipalHref(),
                         false, // service
                         idToken.getOpaqueData());
      }

      final SynchConnection sc = getActiveConnection(idToken.getSubscribeUrl());

      if ((sc != null) &&
          (idToken.getSynchToken().equals(sc.getSynchToken()))) {
        return;
      }

      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void startServiceResponse(final HttpServletResponse resp,
                                    final SynchConnection sc,
                                    final boolean ok) throws WebdavException {
    try {
      final StartServiceResponseType ssr = of.createStartServiceResponseType();

      if (ok) {
        ssr.setStatus(StatusType.OK);
        ssr.setToken(sc.getSynchToken());
      } else {
        ssr.setStatus(StatusType.ERROR);
      }

      final JAXBElement<StartServiceResponseType> jax = of.createStartServiceResponse(ssr);

      marshal(jax, resp.getOutputStream());
    } catch (final WebdavException we) {
      throw we;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private SynchConnectionsMBean conns;

  private SynchConnectionsMBean getActiveConnections() throws Throwable {
    if (conns == null) {
      /*
      conns = (SynchConnectionsMBean)MBeanUtil.getMBean(SynchConnectionsMBean.class,
                                         "org.bedework:service=CalDAVSynchConnections");
                                         */
      conns = getIntf().getActiveConnections();
    }

    return conns;
  }

  private void setActiveConnection(final SynchConnection val) throws Throwable {
    getActiveConnections().setConnection(val);
  }

  private SynchConnection getActiveConnection(final String url) throws Throwable {
    return getActiveConnections().getConnection(url);
  }
}
