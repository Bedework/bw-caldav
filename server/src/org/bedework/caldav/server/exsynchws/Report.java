/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import org.bedework.caldav.server.CaldavReportMethod;
import org.bedework.exsynch.wsmessages.SynchInfoType;

import edu.rpi.cct.webdav.servlet.common.PropFindMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.sss.util.xml.NsContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ietf.params.xml.ns.icalendar_2.Icalendar;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

class Report extends CaldavReportMethod {
  /**
   * @param nsIntf
   */
  Report(final WebdavNsIntf nsIntf) {
    super();

    this.nsIntf = nsIntf;
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

      NamespaceContext ctx = new NsContext(null);

      XPathFactory xpathFact = XPathFactory.newInstance();
      XPath xpath = xpathFact.newXPath();

      xpath.setNamespaceContext(ctx);

      XPathExpression expr = xpath.compile("//C:calendar-data");

      String calCompPath = "X:icalendar/X:vcalendar/X:components";

      XPathExpression eventUidExpr = xpath.compile(calCompPath + "/X:vevent/X:properties/X:uid/X:text");
      XPathExpression eventLmExpr = xpath.compile(calCompPath + "/X:vevent/X:properties/X:x-bedework-exsynch-lastmod/X:text");

      XPathExpression taskUidExpr = xpath.compile(calCompPath + "/X:vtodo/X:properties/X:uid/X:text");
      XPathExpression taskLmExpr = xpath.compile(calCompPath + "/X:vtodo/X:properties/X:x-bedework-exsynch-lastmod/X:text");

      Object result = expr.evaluate(resDoc, XPathConstants.NODESET);
      NodeList nodes = (NodeList)result;

      List<SynchInfoType> sis = new ArrayList<SynchInfoType>();

      int numItems = nodes.getLength();

      for (int i = 0; i < numItems; i++) {
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

  Icalendar fetch(final String resourceUri,
                  final String uid) throws WebdavException {
    // Build a report query and execute it.

    StringBuilder sb = new StringBuilder();

    sb.append("<?xml version='1.0' encoding='utf-8' ?>");
    sb.append("<C:calendar-query xmlns:C='urn:ietf:params:xml:ns:caldav'>");
    sb.append("  <D:prop xmlns:D='DAV:'>");
    sb.append("    <C:calendar-data content-type='application/calendar+xml'>");
    sb.append("      <C:comp name='VCALENDAR'>");
    sb.append("        <C:comp name='VEVENT'>");
    sb.append("        </C:comp>");
    sb.append("        <C:comp name='VTODO'>");
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

    process(resourceUri,
            1);  // depth

    return null;
  }

}