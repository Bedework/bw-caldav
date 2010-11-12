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

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.CaldavComponentNode;
import org.bedework.caldav.server.CaldavReportMethod;
import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.exsynch.wsmessages.AddItem;
import org.bedework.exsynch.wsmessages.AddItemResponse;
import org.bedework.exsynch.wsmessages.GetSycnchInfo;
import org.bedework.exsynch.wsmessages.ObjectFactory;
import org.bedework.exsynch.wsmessages.StartServiceNotification;
import org.bedework.exsynch.wsmessages.StartServiceResponse;
import org.bedework.exsynch.wsmessages.StatusType;
import org.bedework.exsynch.wsmessages.SynchInfoResponse;
import org.bedework.exsynch.wsmessages.SynchInfoType;
import org.bedework.exsynch.wsmessages.SynchInfoResponse.SynchInfoResponses;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.common.PropFindMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.sss.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
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
import javax.xml.xpath.XPathFactory;

/** Class extended by classes which handle special GET requests, e.g. the
 * freebusy service, web calendars, ischedule etc.
 *
 * @author Mike Douglass
 */
public class ExsynchwsHandler extends MethodBase {
  protected CaldavBWIntf intf;

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

  /**
   * @param intf
   * @throws WebdavException
   */
  public ExsynchwsHandler(final CaldavBWIntf intf) throws WebdavException {
    this.intf = intf;

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
    return intf.getAccount();
  }

  /**
   * @return SysIntf
   */
  public SysIntf getSysi() {
    return intf.getSysi();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

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

      ObjectFactory of = new ObjectFactory();
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

  class Report extends CaldavReportMethod {
    Report() {
      super();

      nsIntf = intf;
      xml = nsIntf.getXmlEmit();
    }

    List<SynchInfoType> query(final String resourceUri) throws WebdavException {
      // Build a report query and execute it.

      StringBuilder sb = new StringBuilder();

      sb.append("<?xml version='1.0' encoding='utf-8' ?>");
      sb.append("<C:calendar-query xmlns:C='urn:ietf:params:xml:ns:caldav'>");
      sb.append("  <D:prop xmlns:D='DAV:'>");
      sb.append("    <D:getetag/>");
      sb.append("    <C:calendar-data content-type='application/calendar+xml'>");
      sb.append("      <C:comp name='VCALENDAR'>");
      sb.append("        <C:comp name='VEVENT'>");
      sb.append("          <C:prop name='X-BEDEWORK-EXSYNC-LASTMOD'/>");
      sb.append("          <C:prop name='UID'/>");
      sb.append("        </C:comp>");
      sb.append("        <C:comp name='VTODO'>");
      sb.append("          <C:prop name='X-BEDEWORK-EXSYNC-LASTMOD'/>");
      sb.append("          <C:prop name='UID'/>");
      sb.append("        </C:comp>");
      sb.append("      </C:comp>");
      sb.append("    </C:calendar-data>");
      sb.append("  </D:prop>");
      sb.append("  <C:filter>");
      sb.append("    <C:comp-filter name='VCALENDAR'>");
      //sb.append("      <C:comp-filter name='VEVENT'>");
      //sb.append("      </C:comp-filter>");
      sb.append("    </C:comp-filter>");
      sb.append("  </C:filter>");
      sb.append("</C:calendar-query>");

      pm = new PropFindMethod();
      pm.init(getNsIntf(), debug, true);

      Document doc = parseContent(sb.length(),
                                  new StringReader(sb.toString()));

      processDoc(doc);

      try {
        // Set up XmlEmit so we can process the output.
        StringWriter sw = new StringWriter();
        xml.startEmit(sw);

        process(resourceUri,
                1);  // depth

        String s = sw.toString();
        Document resDoc = parseContent(s.length(),
                                       new StringReader(s));

        NamespaceContext ctx = new NamespaceContext() {
          public String getNamespaceURI(final String prefix) {
            if (prefix.equals("D")) {
              return "DAV";
            }

            if (prefix.equals("C")) {
              return "urn:ietf:params:xml:ns:caldav";
            }

            if (prefix.equals("X")) {
              return "urn:ietf:params:xml:ns:icalendar-2.0";
            }

            return null;
          }

          public Iterator getPrefixes(final String val) {
            return null;
          }


          public String getPrefix(final String uri) {
            return null;
          }
        };

        XPathFactory xpathFact = XPathFactory.newInstance();
        XPath xpath = xpathFact.newXPath();

        xpath.setNamespaceContext(ctx);

        XPathExpression expr = xpath.compile("//D:propstat/D:prop/C:calendar-data");

        String calCompPath = "X:icalendar/X:vcalendar/X:components";

        XPathExpression eventUidExpr = xpath.compile(calCompPath + "/X:vevent/X:properties/X:uid/X:text");
        XPathExpression eventLmExpr = xpath.compile("/X:vevent/X:properties/X:x-bedework-exsync-lastmod/X:text");

        XPathExpression taskUidExpr = xpath.compile(calCompPath + "/X:vevent/X:properties/X:uid/X:text");
        XPathExpression taskLmExpr = xpath.compile("/X:vevent/X:properties/X:x-bedework-exsync-lastmod/X:text");

        Object result = expr.evaluate(resDoc, XPathConstants.NODESET);
        NodeList nodes = (NodeList)result;

        List<SynchInfoType> sis = new ArrayList<SynchInfoType>();

        for (int i = 0; i < nodes.getLength(); i++) {
          Node n = nodes.item(i);

          String uid = (String)eventUidExpr.evaluate(n, XPathConstants.STRING);
          String lm = (String)eventLmExpr.evaluate(n, XPathConstants.STRING);

          if ((uid == null) || (uid.length() == 0)) {
            // task?
            uid = (String)taskUidExpr.evaluate(n, XPathConstants.STRING);
            lm = (String)taskLmExpr.evaluate(n, XPathConstants.STRING);
          }

          if (uid == null) {
            continue;
          }
          SynchInfoType si = new SynchInfoType();

          si.setUid(uid);
          si.setExchangeLastmod(lm);

          sis.add(si);
        }

        return sis;
      } catch (WebdavException we) {
        throw we;
      } catch (Throwable t) {
        throw new WebdavException(t);
      }
    }

    private void expect(final Element el, final QName q) throws WebdavException {
      if (!XmlUtil.nodeMatches(el, q)) {
        throw new WebdavBadRequest("Expected " + q);
      }
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

    intf.reAuth(req, gsi.getPrincipalHref());

    if (!gsi.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }

    SynchInfoResponse sir = new SynchInfoResponse();

    sir.setCalendarHref(gsi.getCalendarHref());

    WebdavNsNode calNode = intf.getNode(gsi.getCalendarHref(),
                                        WebdavNsIntf.existanceMust,
                                        WebdavNsIntf.nodeTypeCollection);

    if (calNode == null) {
      // Drop this subscription
      throw new WebdavException("Unreachable " + gsi.getCalendarHref());
    }

    List<SynchInfoType> sis = new Report().query(gsi.getCalendarHref());

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

  private void doAddItem(final AddItem ai,
                         final HttpServletRequest req,
                         final HttpServletResponse resp) throws WebdavException {
    if (debug) {
      trace("AddItem:       cal=" + ai.getCalendarHref() +
            "\n       principal=" + ai.getPrincipalHref() +
            "\n           token=" + ai.getSynchToken());
    }


    intf.reAuth(req, ai.getPrincipalHref());

    if (!ai.getSynchToken().equals(activeConnection.synchToken)) {
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Invalid synch token");
    }

    String name = ai.getCalendarHref() + "/" + ai.getUid() + ".ics";

    WebdavNsNode elNode = intf.getNode(name,
                                        WebdavNsIntf.existanceNot,
                                        WebdavNsIntf.nodeTypeEntity);

    boolean added = false;
    String msg = null;

    try {
      added = intf.putEvent(req, (CaldavComponentNode)elNode,
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
}
