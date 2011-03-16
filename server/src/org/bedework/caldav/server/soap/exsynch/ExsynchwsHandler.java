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
package org.bedework.caldav.server.soap.exsynch;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.soap.SoapHandler;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;
import org.bedework.exsynch.wsmessages.AddItem;
import org.bedework.exsynch.wsmessages.AddItemResponse;
import org.bedework.exsynch.wsmessages.AddType;
import org.bedework.exsynch.wsmessages.ArrayOfUpdates;
import org.bedework.exsynch.wsmessages.BaseUpdateType;
import org.bedework.exsynch.wsmessages.FetchItem;
import org.bedework.exsynch.wsmessages.FetchItemResponse;
import org.bedework.exsynch.wsmessages.GetProperties;
import org.bedework.exsynch.wsmessages.GetPropertiesResponse;
import org.bedework.exsynch.wsmessages.GetSycnchInfo;
import org.bedework.exsynch.wsmessages.NamespaceType;
import org.bedework.exsynch.wsmessages.NewValueType;
import org.bedework.exsynch.wsmessages.ObjectFactory;
import org.bedework.exsynch.wsmessages.RemoveType;
import org.bedework.exsynch.wsmessages.StartServiceNotification;
import org.bedework.exsynch.wsmessages.StartServiceResponse;
import org.bedework.exsynch.wsmessages.StatusType;
import org.bedework.exsynch.wsmessages.SynchInfoResponse;
import org.bedework.exsynch.wsmessages.SynchInfoType;
import org.bedework.exsynch.wsmessages.UpdateItem;
import org.bedework.exsynch.wsmessages.UpdateItemResponse;
import org.bedework.exsynch.wsmessages.SynchInfoResponse.SynchInfoResponses;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.NsContext;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ietf.params.xml.ns.icalendar_2.Icalendar;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class ExsynchwsHandler extends SoapHandler {
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
  public ExsynchwsHandler(final CaldavBWIntf intf) throws WebdavException {
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
  public void processPost(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final RequestPars pars) throws WebdavException {

    try {
      Object o = unmarshal(req);
      if (o instanceof JAXBElement) {
        o = ((JAXBElement)o).getValue();
      }

      if (o instanceof GetProperties) {
        doGetProperties((GetProperties)o, resp);
        return;
      }

      if (o instanceof GetSycnchInfo) {
        doGetSycnchInfo((GetSycnchInfo)o, req, resp);
        return;
      }

      if (o instanceof StartServiceNotification) {
        doStartService((StartServiceNotification)o, resp);
        return;
      }

      if (o instanceof AddItem) {
        doAddItem((AddItem)o, req, resp);
        return;
      }

      if (o instanceof FetchItem) {
        doFetchItem((FetchItem)o, req, resp);
        return;
      }

      if (o instanceof UpdateItem) {
        doUpdateItem((UpdateItem)o, req, resp);
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

  private void doGetProperties(final GetProperties gp,
                              final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetProperties: ");
    }

    try {
      String url = null;

      if (gp.getSystem() != null) {
        url = "/";
      } else if (gp.getPrincipalHref() != null) {
        url = gp.getPrincipalHref();
      } else if (gp.getCalendarHref() != null) {
        url = gp.getCalendarHref();
      }

      GetPropertiesResponse gpr = new GetPropertiesResponse();

      if (url != null) {
        WebdavNsNode calNode = getNsIntf().getNode(url,
                                                   WebdavNsIntf.existanceMust,
                                                   WebdavNsIntf.nodeTypeCollection);

        if (calNode != null) {
          CaldavBwNode nd = (CaldavBwNode)calNode;

          gpr.setXRD(((CaldavBWIntf)getNsIntf()).getXRD(nd));
        }

        marshal(gpr, resp.getOutputStream());
      }
    } catch (WebdavException we) {
      throw we;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doStartService(final StartServiceNotification ssn,
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

  private void notificationResponse(final HttpServletResponse resp,
                                    final boolean ok) throws WebdavException {
    try {
      resp.setCharacterEncoding("UTF-8");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/xml; charset=UTF-8");

      StartServiceResponse ssr = of.createStartServiceResponse();

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

  private void doGetSycnchInfo(final GetSycnchInfo gsi,
                               final HttpServletRequest req,
                               final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("GetSycnchInfo: cal=" + gsi.getCalendarHref() +
            "\n       principal=" + gsi.getPrincipalHref() +
            "\n           token=" + gsi.getSynchToken());
    }

    getIntf().reAuth(req, gsi.getPrincipalHref());

    if (!gsi.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }

    SynchInfoResponse sir = new SynchInfoResponse();

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
    sirs.getSynchInfos().addAll(sis);

    try {
      marshal(sir, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doAddItem(final AddItem ai,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("AddItem:       cal=" + ai.getCalendarHref() +
            "\n       principal=" + ai.getPrincipalHref() +
            "\n           token=" + ai.getSynchToken());
    }

    getIntf().reAuth(req, ai.getPrincipalHref());

    if (!ai.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }

    String name = ai.getCalendarHref() + "/" + ai.getUid() + ".ics";

    WebdavNsNode elNode = getNsIntf().getNode(name,
                                              WebdavNsIntf.existanceNot,
                                              WebdavNsIntf.nodeTypeEntity);

    boolean added = false;

    try {
      added = getIntf().putEvent(req, (CaldavComponentNode)elNode,
                                 ai.getIcalendar(),
                                 true, null);
    } catch (Throwable t) {
      if (debug) {
        error(t);
      }
    }

    AddItemResponse air = new AddItemResponse();

    if (added) {
      air.setStatus(StatusType.OK);
    } else {
      air.setStatus(StatusType.ERROR);
    }

    try {
      marshal(air, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doFetchItem(final FetchItem fi,
                           final HttpServletRequest req,
                           final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("FetchItem:       cal=" + fi.getCalendarHref() +
            "\n         principal=" + fi.getPrincipalHref() +
            "\n             token=" + fi.getSynchToken());
    }

    getIntf().reAuth(req, fi.getPrincipalHref());

    if (!fi.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }


    String name = fi.getCalendarHref() + "/" + fi.getUid() + ".ics";

    WebdavNsNode elNode = getNsIntf().getNode(name,
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeEntity);

    FetchItemResponse fir = new FetchItemResponse();

    if (elNode == null) {
      fir.setStatus(StatusType.ERROR);
    } else {
      fir.setStatus(StatusType.OK);
      CalDAVEvent ev = ((CaldavComponentNode)elNode).getEvent();
      fir.setIcalendar(getIntf().getSysi().toIcalendar(ev, false));
    }

    try {
      marshal(fir, resp.getOutputStream());
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doUpdateItem(final UpdateItem ui,
                            final HttpServletRequest req,
                            final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("UpdateItem:       cal=" + ui.getCalendarHref() +
            "\n          principal=" + ui.getPrincipalHref() +
            "\n              token=" + ui.getSynchToken());
    }

    getIntf().reAuth(req, ui.getPrincipalHref());

    if (!ui.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }

    String name = ui.getCalendarHref() + "/" + ui.getUid() + ".ics";

    WebdavNsNode elNode = getNsIntf().getNode(name,
                                              WebdavNsIntf.existanceMust,
                                              WebdavNsIntf.nodeTypeEntity);

    UpdateItemResponse uir = new UpdateItemResponse();

    if (elNode == null) {
      uir.setStatus(StatusType.ERROR);
      return;
    }

    CaldavComponentNode compNode = (CaldavComponentNode)elNode;
    CalDAVEvent ev = compNode.getEvent();

    if (debug) {
      trace("event: " + ev);
    }

    Document doc = makeDoc(XcalTags.icalendar,
                           getIntf().getSysi().toIcalendar(ev, false));

    ArrayOfUpdates aupd = ui.getUpdates();

    NsContext ctx = new NsContext(null);
    ctx.clear();

    for (NamespaceType ns: ui.getNamespaces().getNamespaces()) {
      ctx.add(ns.getPrefix(), ns.getUri());
    }

    XPathFactory xpathFact = XPathFactory.newInstance();
    XPath xpath = xpathFact.newXPath();

    xpath.setNamespaceContext(ctx);

    uir.setStatus(StatusType.OK);

    try {
      for (JAXBElement<? extends BaseUpdateType> jel: aupd.getBaseUpdates()) {
        BaseUpdateType but = jel.getValue();

        XPathExpression expr = xpath.compile(but.getSel());

        NodeList nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

        int nlen = nodes.getLength();
        if (debug) {
          trace("expr: " + but.getSel() + " found " + nlen);
        }

        if (nlen != 1) {
          // We only allow updates to a single node.
          uir.setStatus(StatusType.ERROR);
          getIntf().getSysi().rollback();
          break;
        }

        Node nd = nodes.item(0);

        if (but instanceof RemoveType) {
          removeNode(nd);
          continue;
        }

        NewValueType nv = (NewValueType)but;

        /* Replacement or new value must be of same type
         */
        if (!processNewValue(nv, nd, doc)) {
          uir.setStatus(StatusType.ERROR);
          getIntf().getSysi().rollback();
          break;
        }
      }

      if (uir.getStatus() == StatusType.OK) {
        Unmarshaller u = jc.createUnmarshaller();

        Icalendar ical = (Icalendar)u.unmarshal(doc);

  //      WebdavNsNode calNode = getNsIntf().getNode(ui.getCalendarHref(),
  //                                                 WebdavNsIntf.existanceMust,
  //                                                 WebdavNsIntf.nodeTypeCollection);
        CalDAVCollection col = (CalDAVCollection)compNode.getCollection(true); // deref

        SysiIcalendar cal = getIntf().getSysi().fromIcal(col,
                                                         ical,
                                                         IcalResultType.OneComponent);

        CalDAVEvent newEv = (CalDAVEvent)cal.iterator().next();

        ev.setParentPath(col.getPath());
        newEv.setName(ev.getName());

        getIntf().getSysi().updateEvent(newEv);
      }

      marshal(uir, resp.getOutputStream());
    } catch (XPathExpressionException xpe) {
      throw new WebdavException(xpe);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private boolean processNewValue(final NewValueType nv,
                                  final Node nd,
                                  final Document evDoc) throws WebdavException {
    Node parent = nd.getParentNode();
    Node matchNode;

    boolean add = nv instanceof AddType;
    QName valName;

    if (add) {
      matchNode = nd;
      valName = new QName("urn:ietf:params:xml:ns:pidf-diff", "add");
    } else {
      matchNode = parent;
      valName = new QName("urn:ietf:params:xml:ns:pidf-diff", "replace");
    }

    validate: {
      if (nv.getBaseComponent() != null) {
        // parent must be a components element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.components)) {
          return false;
        }

        break validate;
      }

      if (nv.getBaseProperty() != null) {
        // parent must be a properties element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.properties)) {
          return false;
        }
        break validate;
      }

      if (nv.getBaseParameter() != null) {
        // parent must be a parameters element
        if (!XmlUtil.nodeMatches(matchNode, XcalTags.parameters)) {
          return false;
        }
        break validate;
      }

      return false;
    } // validate

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();

      Marshaller m = jc.createMarshaller();

      m.marshal(makeJAXBElement(valName, nv.getClass(), nv),
                doc);

      Node newNode = doc.getFirstChild().getFirstChild();

      if (add) {
        matchNode.appendChild(evDoc.importNode(newNode, true));

        return true;
      }

      parent.replaceChild(evDoc.importNode(newNode, true), nd);

      return true;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
