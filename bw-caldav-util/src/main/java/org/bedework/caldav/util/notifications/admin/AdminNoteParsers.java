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
package org.bedework.caldav.util.notifications.admin;

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.caldav.util.notifications.parse.BaseNotificationParser;
import org.bedework.caldav.util.notifications.parse.Parser;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Class to parse administrative notifications.
 *
 * @author Mike Douglass douglm
 */
public class AdminNoteParsers {
  /** */
  public static final QName awaitingApprovalTag =
          BedeworkServerTags.awaitingApproval;

  /** */
  public static final QName approvalResponseTag =
          BedeworkServerTags.approvalResponse;

  /** */
  public static final QName acceptedTag = BedeworkServerTags.accepted;

  /** */
  public static final QName commentTag = BedeworkServerTags.comment;

  /** */
  public static final QName hrefTag = WebdavTags.href;

  /** */
  public static final QName nameTag = BedeworkServerTags.name;

  /** */
  public static final QName principalURLTag = WebdavTags.principalURL;

  /** */
  public static final QName uidTag = AppleServerTags.uid;

  static {
    Parser.register(new AwaitingApprovalParser());
    Parser.register(new ApprovalResponseParser());
  }

  /**
   * @param val the XML
   * @return parsed Document
   * @throws WebdavException
   */
  public static Document parseXmlString(final String val) throws WebdavException {
    if ((val == null) || (val.length() == 0)) {
      return null;
    }

    return parseXml(new StringReader(val));
  }

  /**
   * @param val a reader
   * @return parsed Document
   * @throws WebdavException
   */
  public static Document parseXml(final Reader val) throws WebdavException{
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(val));
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static abstract class AdmParser implements
          BaseNotificationParser {
    private static final int maxPoolSize = 10;
    private final List<AdminNoteParsers> parsers = new ArrayList<>();

    protected AdminNoteParsers parser;

    protected QName element;

    protected AdmParser(final QName element) {
      this.element = element;
    }

    protected AdminNoteParsers getParser() {
      if (parser != null) {
        return parser;
      }

      synchronized (parsers) {
        if (parsers.size() > 0) {
          parser = parsers.remove(0);
          return parser;
        }

        parser = new AdminNoteParsers();
        parsers.add(parser);

        return parser;
      }
    }

    protected void putParser() {
      synchronized (parsers) {
        if (parsers.size() >= maxPoolSize) {
          return;
        }

        parsers.add(parser);
      }
    }

    @Override
    public QName getElement() {
      return element;
    }
  }

  static class AwaitingApprovalParser extends AdmParser {
    AwaitingApprovalParser() {
      super(awaitingApprovalTag);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseAwaitingApproval(nd);
      } finally {
        putParser();
      }
    }
  }

  static class ApprovalResponseParser extends AdmParser {
    ApprovalResponseParser() {
      super(approvalResponseTag);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseApprovalResponse(nd);
      } finally {
        putParser();
      }
    }
  }

  /**
   * @param val XML representation
   * @return populated EventregCancelledNotificationType object
   * @throws WebdavException
   */
  public AwaitingApprovalNotificationType parseparseAwaitingApproval(final String val) throws WebdavException {
    final Document d = parseXmlString(val);

    return parseAwaitingApproval(d.getDocumentElement());
  }

  /**
   * @param nd MUST be the cancelled xml element
   * @return populated AwaitingApprovalNotificationType object
   * @throws WebdavException
   */
  public AwaitingApprovalNotificationType parseAwaitingApproval(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, awaitingApprovalTag)) {
        throw new WebdavBadRequest("Expected " + awaitingApprovalTag);
      }

      final AwaitingApprovalNotificationType note =
              new AwaitingApprovalNotificationType();
      final Element[] els = XmlUtil.getElementsArray(nd);

      for (final Element curnode: els) {
        if (adminBaseNode(note, curnode)) {
          continue;
        }

        throw new WebdavBadRequest("Unexpected element " + curnode);
      }

      return note;
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the cancelled xml element
   * @return populated ApprovalResponseNotificationType object
   * @throws WebdavException
   */
  public ApprovalResponseNotificationType parseApprovalResponse(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, approvalResponseTag)) {
        throw new WebdavBadRequest("Expected " + approvalResponseTag);
      }

      final ApprovalResponseNotificationType note =
              new ApprovalResponseNotificationType();
      final Element[] els = XmlUtil.getElementsArray(nd);

      for (final Element curnode: els) {
        if (adminBaseNode(note, curnode)) {
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, acceptedTag)) {
          note.setAccepted(Boolean.parseBoolean(
                  XmlUtil.getElementContent(curnode)));
          continue;
        }

        throw new WebdavBadRequest("Unexpected element " + curnode);
      }

      return note;
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** handle base notification elements
   *
   * @param curnode MAY be one of the notification xml elements
   * @return true if absorbed
   * @throws WebdavException
   */
  private boolean adminBaseNode(final AdminNotificationType base,
                                   final Element curnode) throws WebdavException {
    try {
      // Standard notification name element
      if (XmlUtil.nodeMatches(curnode, nameTag)) {
        base.setName(XmlUtil.getElementContent(curnode));
        return true;
      }

      if (XmlUtil.nodeMatches(curnode, uidTag)) {
        base.setUid(XmlUtil.getElementContent(curnode));
        return true;
      }

      if (XmlUtil.nodeMatches(curnode, hrefTag)) {
        base.setHref(XmlUtil.getElementContent(curnode));
        return true;
      }

      if (XmlUtil.nodeMatches(curnode, principalURLTag)) {
        final Element href = XmlUtil.getOnlyElement(curnode);

        if ((href == null) || !XmlUtil.nodeMatches(href, hrefTag)) {
          throw new WebdavBadRequest("Expected " + hrefTag);
        }
        base.setPrincipalHref(XmlUtil.getElementContent(href));
        return true;
      }

      if (XmlUtil.nodeMatches(curnode, commentTag)) {
        base.setComment(XmlUtil.getElementContent(curnode));
        return true;
      }

      return false;
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static WebdavException parseException(final SAXException e) throws WebdavException {
    final Logger log = getLog();

    if (log.isDebugEnabled()) {
      log.error("Parse error:", e);
    }

    return new WebdavBadRequest();
  }

  private static Logger getLog() {
    return Logger.getLogger(AdminNoteParsers.class);
  }
}
