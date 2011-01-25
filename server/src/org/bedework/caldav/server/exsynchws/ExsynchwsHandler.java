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
package org.bedework.caldav.server.exsynchws;

import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.exsynch.wsmessages.AddItem;
import org.bedework.exsynch.wsmessages.AddItemResponse;
import org.bedework.exsynch.wsmessages.ArrayOfUpdates;
import org.bedework.exsynch.wsmessages.FetchItem;
import org.bedework.exsynch.wsmessages.FetchItemResponse;
import org.bedework.exsynch.wsmessages.GetSycnchInfo;
import org.bedework.exsynch.wsmessages.ObjectFactory;
import org.bedework.exsynch.wsmessages.StartServiceNotification;
import org.bedework.exsynch.wsmessages.StartServiceResponse;
import org.bedework.exsynch.wsmessages.StatusType;
import org.bedework.exsynch.wsmessages.SynchInfoResponse;
import org.bedework.exsynch.wsmessages.SynchInfoType;
import org.bedework.exsynch.wsmessages.UpdateItem;
import org.bedework.exsynch.wsmessages.UpdateItemResponse;
import org.bedework.exsynch.wsmessages.SynchInfoResponse.SynchInfoResponses;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ietf.params.xml.ns.pidf_diff.BaseUpdateType;

import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
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
public class ExsynchwsHandler extends MethodBase {
  private MessageFactory soapMsgFactory;
  private JAXBContext jc;

  /** This represents an active connection to a synch engine. It's possible we
   * would have more than one of these running I guess. For the moment we'll
   * only have one but these probably need a table indexed by url.
   *
   */
  class ActiveConnectionInfo {
    String subscribeUrl;

    String synchToken;
  }

  static volatile Object monitor = new Object();

  static ActiveConnectionInfo activeConnection;

  ObjectFactory of = new ObjectFactory();

  /**
   * @param intf
   * @throws WebdavException
   */
  public ExsynchwsHandler(final CaldavBWIntf intf) throws WebdavException {
    nsIntf = intf;
    debug = getLogger().isDebugEnabled();

    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      if (jc == null) {
        jc = JAXBContext.newInstance("org.bedework.exsynch.wsmessages");
      }
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public void init() {
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp)
        throws WebdavException {

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
      SOAPMessage msg = soapMsgFactory.createMessage(null, // headers
                                                     req.getInputStream());


      SOAPBody body = msg.getSOAPBody();

      Unmarshaller u = jc.createUnmarshaller();

      Object o = u.unmarshal(body.getFirstChild());
      if (o instanceof JAXBElement) {
        o = ((JAXBElement)o).getValue();
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

  /**
   * @return current account
   */
  public String getAccount() {
    return getNsIntf().getAccount();
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return getIntf().getSysi();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private CaldavBWIntf getIntf() {
    return (CaldavBWIntf)getNsIntf();
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

  private Document makeDoc(final Object o) throws WebdavException {
    try {
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = dbf.newDocumentBuilder().newDocument();

//      marshaller.marshal(o, doc);

      marshaller.marshal(new JAXBElement(new QName("uri","local"),
                                         o.getClass(), o),
                         doc);

      return doc;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void marshal(final Object o, final OutputStream out) throws WebdavException {
    try {
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = dbf.newDocumentBuilder().newDocument();

      SOAPMessage msg = soapMsgFactory.createMessage();
      msg.getSOAPBody().addDocument(doc);

      marshaller.marshal(o,
                         msg.getSOAPBody());

      msg.writeTo(out);
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
    String msg = null;

    try {
      added = getIntf().putEvent(req, (CaldavComponentNode)elNode,
                                 ai.getIcalendar(),
                                 true, null);
    } catch (Throwable t) {
      if (debug) {
        error(t);
        msg = t.getLocalizedMessage();
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

    CalDAVEvent ev = ((CaldavComponentNode)elNode).getEvent();

    if (debug) {
      trace("event: " + ev);
    }

    Document doc = makeDoc(getIntf().getSysi().toIcalendar(ev, false));

    ArrayOfUpdates aupd = ui.getUpdates();

    NamespaceContext ctx = new NsContext(null);

    XPathFactory xpathFact = XPathFactory.newInstance();
    XPath xpath = xpathFact.newXPath();

    xpath.setNamespaceContext(ctx);

    try {
      for (JAXBElement<? extends BaseUpdateType> jel: aupd.getBaseUpdates()) {
        BaseUpdateType but = jel.getValue();

        XPathExpression expr = xpath.compile(but.getSel());

        NodeList nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);

        if (debug) {
          trace("expr: " + but.getSel() + " found " + nodes.getLength());
        }
      }

      uir.setStatus(StatusType.OK);

      marshal(uir, resp.getOutputStream());
    } catch (XPathExpressionException xpe) {
      throw new WebdavException(xpe);
    } catch (WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
