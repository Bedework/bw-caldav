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
package org.bedework.caldav.server.soap.calws;

import org.bedework.caldav.server.CalDAVCollection;
import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavBwNode;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.SysiIcalendar;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.soap.SoapHandler;
import org.bedework.caldav.server.sysinterface.SysIntf.IcalResultType;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.NsContext;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.XcalTags;

import org.oasis_open.docs.ns.wscal.calws_soap.AddItem;
import org.oasis_open.docs.ns.wscal.calws_soap.AddItemResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.AddType;
import org.oasis_open.docs.ns.wscal.calws_soap.ArrayOfUpdates;
import org.oasis_open.docs.ns.wscal.calws_soap.BaseUpdateType;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItem;
import org.oasis_open.docs.ns.wscal.calws_soap.FetchItemResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.GetProperties;
import org.oasis_open.docs.ns.wscal.calws_soap.GetPropertiesResponse;
import org.oasis_open.docs.ns.wscal.calws_soap.NamespaceType;
import org.oasis_open.docs.ns.wscal.calws_soap.NewValueType;
import org.oasis_open.docs.ns.wscal.calws_soap.RemoveType;
import org.oasis_open.docs.ns.wscal.calws_soap.StatusType;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItem;
import org.oasis_open.docs.ns.wscal.calws_soap.UpdateItemResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ietf.params.xml.ns.icalendar_2.Icalendar;

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
public class CalwsHandler extends SoapHandler {
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

  /**
   * @param intf
   * @throws WebdavException
   */
  public CalwsHandler(final CaldavBWIntf intf) throws WebdavException {
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
      String url = gp.getHref();

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

  private void doAddItem(final AddItem ai,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("AddItem:       cal=" + ai.getHref());
    }

    WebdavNsNode elNode = getNsIntf().getNode(ai.getHref(),
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
      trace("FetchItem:       cal=" + fi.getHref());
    }

    WebdavNsNode elNode = getNsIntf().getNode(fi.getHref(),
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
      trace("UpdateItem:       cal=" + ui.getHref());
    }

    WebdavNsNode elNode = getNsIntf().getNode(ui.getHref(),
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
