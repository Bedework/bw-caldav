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

import org.bedework.caldav.server.soap.ReportBase;
import org.bedework.exsynch.wsmessages.SynchInfoType;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.sss.util.xml.NsContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * @author douglm
 */
public class Report extends ReportBase {
  /**
   * @param nsIntf
   */
  public Report(final WebdavNsIntf nsIntf) {
    super(nsIntf);
  }

  /**
   * @param resourceUri
   * @return List<SynchInfoType>
   * @throws WebdavException
   */
  public List<SynchInfoType> query(final String resourceUri) throws WebdavException {
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

    try {
      Document resDoc = super.query(sb.toString(), resourceUri);

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
}
