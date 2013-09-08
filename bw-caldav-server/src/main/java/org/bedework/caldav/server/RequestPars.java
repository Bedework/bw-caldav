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
package org.bedework.caldav.server;

import org.bedework.caldav.server.sysinterface.CalDAVSystemProperties;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.util.misc.Util;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 */
public class RequestPars {
  /** */
  public HttpServletRequest req;

  /** */
  public String method;

  /** */
  public String resourceUri;

  /** If this request is for a special URI this is the resource URI without
   * the prefix and any parameters. It may be an empty string but will not be null.
   * <p>for example /ischedule/domainkey/... will become /domainkey/...
   */
  public String noPrefixResourceUri;

  /** from accept header */
  public String acceptType;

  /** type of request body */
  String contentType;

  /** Broken out content type */
  public String[] contentTypePars;

  /** Special parameters for iSchedule
   */
  public IscheduleIn ischedRequest;

  Reader reqRdr;

  SysiIcalendar ic;

  CalDAVCollection cal;

  /** true if this is a CalDAV share request */
  public boolean share;

  /* true if this is an iSchedule request */
  boolean iSchedule;

  /** true if this is a free busy request */
  public boolean freeBusy;

  /** true if this is a web calendar request */
  public boolean webcal;

  /** true if this is a web calendar request with GET + ACCEPT */
  public boolean webcalGetAccept;

  /** true if web service create of entity */
  public boolean entityCreate;

  /** true if this is an synch web service request */
  public boolean synchws;

  /** true if this is a calws soap web service request */
  public boolean calwsSoap;

  /** Set if the content type is xml */
  public Document xmlDoc;

  private boolean getTheReader = true;

  /**
   * @param req
   * @param intf
   * @param resourceUri
   * @throws WebdavException
   */
  public RequestPars(final HttpServletRequest req,
                     final CaldavBWIntf intf,
                     final String resourceUri) throws WebdavException {
    SysIntf sysi = intf.getSysi();

    CalDAVSystemProperties sp = sysi.getSystemProperties();

    this.req = req;
    this.resourceUri = resourceUri;

    method = req.getMethod();

    acceptType = req.getHeader("ACCEPT");

    contentType = req.getContentType();

    if (contentType != null) {
      contentTypePars = contentType.split(";");
    }

    testRequest: {
      iSchedule = checkUri(sp.getIscheduleURI());

      if (iSchedule) {
        ischedRequest = new IscheduleIn(req, sysi.getUrlHandler());

        getTheReader = false;
        break testRequest;
      }

      freeBusy = checkUri(sp.getFburlServiceURI());

      if (freeBusy) {
        getTheReader = false;
        break testRequest;
      }

      webcal = checkUri(sp.getWebcalServiceURI());

      if (webcal) {
        getTheReader = false;
        break testRequest;
      }

      // not ischedule or freeBusy or webcal

      if (intf.getCalWS()) {
        // POST of entity for create?
        if ("create".equals(req.getParameter("action"))) {
          entityCreate = true;
        }
        break testRequest;
      }

      if (intf.getSynchWsURI() != null) {
        synchws = intf.getSynchWsURI().equals(resourceUri);
        if (synchws) {
          getTheReader = false;
          break testRequest;
        }
      }

      if (sp.getCalSoapWsURI() != null) {
        calwsSoap = sp.getCalSoapWsURI().equals(resourceUri);
        if (calwsSoap) {
          getTheReader = false;
          break testRequest;
        }
      }

      /* Not any of the special URIs - this could be a post aimed at one of
       * our caldav resources.
       */
      if (isAppXml()) {
        try {
          reqRdr = req.getReader();
        } catch (Throwable t) {
          throw new WebdavException(t);
        }

        xmlDoc = parseXml(reqRdr);
        getTheReader = false;
      }
    } // testRequest

    if (getTheReader) {
      try {
        reqRdr = req.getReader();
      } catch (Throwable t) {
        throw new WebdavException(t);
      }
    }
  }

  /**
   * @param val
   * @return parsed Document
   * @throws WebdavException
   */
  private Document parseXml(final Reader rdr) throws WebdavException{
    if (rdr == null) {
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(rdr));
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @return true if we have an xml content
   */
  public boolean isAppXml() {
    if (contentTypePars == null) {
      return false;
    }

    return contentTypePars[0].equals("application/xml") ||
        contentTypePars[0].equals("text/xml");
  }

  /**
   * @param val
   */
  public void setContentType(final String val) {
    contentType = val;
  }

  private boolean checkUri(final String specialUri) {
    if (specialUri == null) {
      return false;
    }

    String toMatch = Util.buildPath(true, specialUri);
    String prefix;

    int pos = resourceUri.indexOf("/", 1);

    if (pos < 0) {
      prefix = noParameters(resourceUri);
    } else {
      prefix = resourceUri.substring(0, pos);
    }

    if (!toMatch.equals(Util.buildPath(true, prefix))) {
      noPrefixResourceUri = noParameters(resourceUri);
      return false;
    }

    if (pos < 0) {
      noPrefixResourceUri = "";
    } else {
      noPrefixResourceUri = noParameters(resourceUri.substring(pos));
    }

    return true;
  }

  private String noParameters(String uri) {
    int pos = uri.indexOf("?");
    if (pos > 0) {
      uri = uri.substring(0, pos);
    }

    if (uri.equals("/")) {
      uri = "";
    }

    return uri;
  }
}