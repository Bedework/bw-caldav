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
import org.bedework.exsynch.wsmessages.ObjectFactory;

import edu.rpi.cct.webdav.servlet.common.MethodBase;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

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

  protected ObjectFactory of = new ObjectFactory();

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

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp)
        throws WebdavException {

  }

  protected Object unmarshal(final HttpServletRequest req) throws WebdavException {
    try {
      SOAPMessage msg = soapMsgFactory.createMessage(null, // headers
                                                     req.getInputStream());


      SOAPBody body = msg.getSOAPBody();

      Unmarshaller u = jc.createUnmarshaller();

      return u.unmarshal(body.getFirstChild());
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
   *                   Protected methods
   * ==================================================================== */

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
}
