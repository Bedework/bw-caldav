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

  protected static volatile Object monitor = new Object();

  /**
   * @param intf
   * @throws WebdavException
   */
  public SoapHandler(final CaldavBWIntf intf) throws WebdavException {
    nsIntf = intf;
    debug = getLogger().isDebugEnabled();

    try {
      if (soapMsgFactory == null) {
        soapMsgFactory = MessageFactory.newInstance();
      }

      if (jc == null) {
        jc = JAXBContext.newInstance(getJaxbContextPath());

//        if (debug) {
  //        debugMsg("Created JAXBContext: " + jc);
    //    }
      }
    } catch(Throwable t) {
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

  protected void initResponse(final HttpServletResponse resp)
        throws WebdavException {
    resp.setCharacterEncoding("UTF-8");
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/xml; charset=UTF-8");
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp)
        throws WebdavException {

  }

  /** Unpack the headers and body
   */
  public static class UnmarshalResult {
    /** */
    public Object[] hdrs;
    /** */
    public Object body;
  }

  protected UnmarshalResult unmarshal(final HttpServletRequest req) throws WebdavException {
    try {
      UnmarshalResult res = new UnmarshalResult();

      SOAPMessage msg = soapMsgFactory.createMessage(null, // headers
                                                     req.getInputStream());

      SOAPBody body = msg.getSOAPBody();
      SOAPHeader hdrMsg = msg.getSOAPHeader();

      Unmarshaller u = jc.createUnmarshaller();

      // Only expect one header at most.
      if ((hdrMsg != null) && hdrMsg.hasChildNodes()) {
        res.hdrs = new Object[1];
        res.hdrs[0] = u.unmarshal(hdrMsg.getFirstChild());
      }

      res.body = u.unmarshal(body.getFirstChild());

      return res;
    } catch(Throwable t) {
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
                             final Object o) throws WebdavException {
    try {
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document doc = dbf.newDocumentBuilder().newDocument();

//      marshaller.marshal(o, doc);

      marshaller.marshal(makeJAXBElement(name,
                                         o.getClass(), o),
                         doc);

      return doc;
    } catch(Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void marshal(final Object o,
                         final OutputStream out) throws WebdavException {
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

  @SuppressWarnings("unchecked")
  protected JAXBElement makeJAXBElement(final QName name,
                                        final Class cl,
                                        final Object o) {
    return new JAXBElement(name, cl, o);
  }

  protected void removeNode(final Node nd) throws WebdavException {
    Node parent = nd.getParentNode();

    parent.removeChild(nd);
  }

  protected String findTzid(final BasePropertyType bp) {
    ArrayOfParameters aop = bp.getParameters();

    for (JAXBElement<? extends BaseParameterType> el: aop.getBaseParameter()) {
      if (el.getName().equals(XcalTags.tzid)) {
        TzidParamType tzid = (TzidParamType)el.getValue();
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

    DateDatetimePropertyType d = (DateDatetimePropertyType)bp;

    if (d.getDate() != null) {
      return null;
    }

    if (d.getDateTime() == null) {
      return null;
    }

    String dt = XcalUtil.getIcalFormatDateTime(
            d.getDateTime().toString());

    if ((dt.length() == 18) && (dt.charAt(17) == 'Z')) {
      return dt;
    }

    return null;
  }
}
