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
package org.bedework.caldav.server.soap;

import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.xml.tagdefs.XcalTags;
import org.bedework.webdav.servlet.common.MethodBase;
import org.bedework.webdav.servlet.shared.WebdavException;

import ietf.params.xml.ns.icalendar_2.ArrayOfParameters;
import ietf.params.xml.ns.icalendar_2.BaseParameterType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.DateDatetimePropertyType;
import ietf.params.xml.ns.icalendar_2.TzidParamType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

/** Class extended by classes which handle special SOAP requests, e.g. the
 * exchange synch service etc.
 *
 * @author Mike Douglass
 */
public abstract class SoapHandler extends MethodBase {
  private MessageFactory soapMsgFactory;
  protected JAXBContext jc;

  protected static final Object monitor = new Object();

  /**
   * @param intf interface to underlying system
   * @throws WebdavException on soap error
   */
  public SoapHandler(final CaldavBWIntf intf) {
    nsIntf = intf;

    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      if (jc == null) {
        jc = JAXBContext.newInstance(getJaxbContextPath());

//        if (debug()) {
  //        debug("Created JAXBContext: " + jc);
    //    }
      }
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return String required to for JAXBContext.newInstance
   */
  protected abstract String getJaxbContextPath();

  @Override
  public void init() {
  }

  protected void initResponse(final HttpServletResponse resp) {
    resp.setCharacterEncoding("UTF-8");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml;charset=utf-8");
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) {

  }

  /** Unpack the headers and body
   */
  public static class UnmarshalResult {
    /** */
    public Object[] hdrs;
    /** */
    public Object body;
  }

  protected UnmarshalResult unmarshal(final HttpServletRequest req) {
    try {
      final UnmarshalResult res = new UnmarshalResult();

      final SOAPMessage msg =
              soapMsgFactory.createMessage(null,// headers
                                           req.getInputStream());

      final SOAPBody body = msg.getSOAPBody();
      final SOAPHeader hdrMsg = msg.getSOAPHeader();

      final Unmarshaller u = jc.createUnmarshaller();

      // Only expect one header at most.
      if ((hdrMsg != null) && hdrMsg.hasChildNodes()) {
        res.hdrs = new Object[1];
        res.hdrs[0] = u.unmarshal(hdrMsg.getFirstChild());
      }

      res.body = u.unmarshal(body.getFirstChild());

      return res;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return current account
   */
  protected String getAccount() {
    return getNsIntf().getAccount();
  }

  /**
   * @return SysIntf
   */
  protected SysIntf getSysi() {
    return getIntf().getSysi();
  }

  protected CaldavBWIntf getIntf() {
    return (CaldavBWIntf)getNsIntf();
  }

  protected Document makeDoc(final QName name,
                             final Object o) {
    try {
      final Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                             Boolean.TRUE);

      final DocumentBuilderFactory dbf =
              DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final Document doc = dbf.newDocumentBuilder().newDocument();

//      marshaller.marshal(o, doc);

      marshaller.marshal(makeJAXBElement(name,
                                         o.getClass(), o),
                         doc);

      return doc;
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void marshal(final Object o,
                         final OutputStream out) {
    try {
      final Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                             Boolean.TRUE);

      final DocumentBuilderFactory dbf =
              DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final Document doc = dbf.newDocumentBuilder().newDocument();

      final SOAPMessage msg = soapMsgFactory.createMessage();
      msg.getSOAPBody().addDocument(doc);

      marshaller.marshal(o,
                         msg.getSOAPBody());

      msg.writeTo(out);
    } catch(final Throwable t) {
      throw new WebdavException(t);
    }
  }

  @SuppressWarnings("unchecked")
  protected JAXBElement<?> makeJAXBElement(final QName name,
                                           final Class<?> cl,
                                           final Object o) {
    return new JAXBElement(name, cl, o);
  }

  protected void removeNode(final Node nd) {
    final Node parent = nd.getParentNode();

    parent.removeChild(nd);
  }

  protected String findTzid(final BasePropertyType bp) {
    final ArrayOfParameters aop = bp.getParameters();

    for (final JAXBElement<? extends BaseParameterType> el: aop.getBaseParameter()) {
      if (el.getName().equals(XcalTags.tzid)) {
        final TzidParamType tzid = (TzidParamType)el.getValue();
        return tzid.getText();
      }
    }

    return null;
  }

  protected String checkUTC(final BasePropertyType bp) {
    if (findTzid(bp) != null) {
      return null;
    }

    if (!(bp instanceof DateDatetimePropertyType)) {
      return null;
    }

    final DateDatetimePropertyType d = (DateDatetimePropertyType)bp;

    if (d.getDate() != null) {
      return null;
    }

    if (d.getDateTime() == null) {
      return null;
    }

    final String dt = XcalUtil.getIcalFormatDateTime(
            d.getDateTime().toString());

    if ((dt.length() == 18) && (dt.charAt(17) == 'Z')) {
      return dt;
    }

    return null;
  }
}
