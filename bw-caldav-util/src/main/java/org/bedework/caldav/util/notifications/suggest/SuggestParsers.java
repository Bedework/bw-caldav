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
package org.bedework.caldav.util.notifications.suggest;

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

/** Class to parse suggest notification.
 *
 * @author Mike Douglass douglm
 */
public class SuggestParsers {
  /** */
  public static final QName acceptedTag = BedeworkServerTags.accepted;

  /** */
  public static final QName commentTag = BedeworkServerTags.comment;

  /** */
  public static final QName hrefTag = WebdavTags.href;

  /** */
  public static final QName nameTag = BedeworkServerTags.name;

  /** */
  public static final QName suggesteeHrefTag = BedeworkServerTags.suggesteeHref;

  /** */
  public static final QName suggesterHrefTag = BedeworkServerTags.suggesterHref;

  /** */
  public static final QName suggestTag = BedeworkServerTags.suggest;

  /** */
  public static final QName suggestReplyTag = BedeworkServerTags.suggestReply;

  /** */
  public static final QName uidTag = AppleServerTags.uid;

  static {
    Parser.register(new SuggestParser());
    Parser.register(new SuggestReplyParser());
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

  private static abstract class SuggestionParser implements BaseNotificationParser {
    private static final int maxPoolSize = 10;
    private final List<SuggestParsers> parsers = new ArrayList<>();

    protected SuggestParsers parser;

    protected QName element;

    protected SuggestionParser(final QName element) {
      this.element = element;
    }

    protected SuggestParsers getParser() {
      if (parser != null) {
        return parser;
      }

      synchronized (parsers) {
        if (parsers.size() > 0) {
          parser = parsers.remove(0);
          return parser;
        }

        parser = new SuggestParsers();
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

  static class SuggestParser extends SuggestionParser {
    SuggestParser() {
      super(suggestTag);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseSuggest(nd);
      } finally {
        putParser();
      }
    }
  }

  static class SuggestReplyParser extends SuggestionParser {
    SuggestReplyParser() {
      super(suggestReplyTag);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseSuggestReply(nd);
      } finally {
        putParser();
      }
    }
  }

  /**
   * @param val XML representation
   * @return populated SuggestNotificationType object
   * @throws WebdavException
   */
  public SuggestNotificationType parseSuggest(final String val) throws WebdavException {
    final Document d = parseXmlString(val);

    return parseSuggest(d.getDocumentElement());
  }

  /**
   * @param nd MUST be the invite xml element
   * @return populated SuggestNotificationType object
   * @throws WebdavException
   */
  public SuggestNotificationType parseSuggest(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, suggestTag)) {
        throw new WebdavBadRequest("Expected " + suggestTag);
      }

      final SuggestNotificationType snt = new SuggestNotificationType();
      final Element[] els = XmlUtil.getElementsArray(nd);

      for (final Element curnode: els) {
        if (XmlUtil.nodeMatches(curnode, nameTag)) {
          snt.setName(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, uidTag)) {
          snt.setUid(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, hrefTag)) {
          snt.setHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, suggesteeHrefTag)) {
          snt.setSuggesteeHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, suggesterHrefTag)) {
          snt.setSuggesterHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, commentTag)) {
          snt.setComment(XmlUtil.getElementContent(curnode));
          continue;
        }

        throw new WebdavBadRequest("Unexpected element " + curnode);
      }

      return snt;
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the invite xml element
   * @return populated SuggestNotificationType object
   * @throws WebdavException
   */
  public SuggestResponseNotificationType parseSuggestReply(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, suggestReplyTag)) {
        throw new WebdavBadRequest("Expected " + suggestReplyTag);
      }

      final SuggestResponseNotificationType srnt = new SuggestResponseNotificationType();
      final Element[] els = XmlUtil.getElementsArray(nd);

      for (final Element curnode: els) {
        if (XmlUtil.nodeMatches(curnode, nameTag)) {
          srnt.setName(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, uidTag)) {
          srnt.setUid(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, hrefTag)) {
          srnt.setHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, suggesteeHrefTag)) {
          srnt.setSuggesteeHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, suggesterHrefTag)) {
          srnt.setSuggesterHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, commentTag)) {
          srnt.setComment(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, acceptedTag)) {
          srnt.setAccepted(Boolean.parseBoolean(XmlUtil.getElementContent(curnode)));
          continue;
        }

        throw new WebdavBadRequest("Unexpected element " + curnode);
      }

      return srnt;
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
    return Logger.getLogger(SuggestParsers.class);
  }
}
