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
package org.bedework.caldav.util.sharing.parse;

import org.bedework.caldav.util.notifications.BaseNotificationType;
import org.bedework.caldav.util.notifications.parse.BaseNotificationParser;
import org.bedework.caldav.util.sharing.AccessType;
import org.bedework.caldav.util.sharing.InviteNotificationType;
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.InviteType;
import org.bedework.caldav.util.sharing.OrganizerType;
import org.bedework.caldav.util.sharing.RemoveType;
import org.bedework.caldav.util.sharing.SetType;
import org.bedework.caldav.util.sharing.ShareType;
import org.bedework.caldav.util.sharing.UserType;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CaldavTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavBadRequest;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.bedework.util.xml.XmlUtil.getAttrVal;
import static org.bedework.util.xml.XmlUtil.getElementContent;
import static org.bedework.util.xml.XmlUtil.getElementsArray;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/** Class to parse properties and requests related to CalDAV sharing
 * (as defined by Apple).
 *
 * @author Mike Douglass douglm
 */
public class Parser {
  /** */
  public static final QName accessTag = AppleServerTags.access;

  /** */
  public static final QName commonNameTag = AppleServerTags.commonName;

  /** */
  public static final QName compTag = CaldavTags.comp;

  /** */
  public static final QName firstNameTag = AppleServerTags.firstName;

  /** */
  public static final QName lastNameTag = AppleServerTags.lastName;

  /** */
  public static final QName hosturlTag = AppleServerTags.hosturl;

  /** */
  public static final QName hrefTag = WebdavTags.href;

  /** */
  public static final QName inReplyToTag = AppleServerTags.inReplyTo;

  /** */
  public static final QName inviteTag = AppleServerTags.invite;

  /** */
  public static final QName inviteAcceptedTag = AppleServerTags.inviteAccepted;

  /** */
  public static final QName inviteDeclinedTag = AppleServerTags.inviteDeclined;

  /** */
  public static final QName inviteDeletedTag = AppleServerTags.inviteDeleted;

  /** */
  public static final QName inviteInvalidTag = AppleServerTags.inviteInvalid;

  /** */
  public static final QName inviteNoresponseTag = AppleServerTags.inviteNoresponse;

  /** */
  public static final QName inviteNotificationTag = AppleServerTags.inviteNotification;

  /** */
  public static final QName inviteReplyTag = AppleServerTags.inviteReply;

  /** */
  public static final QName organizerTag = AppleServerTags.organizer;

  /** */
  public static final QName readTag = AppleServerTags.read;

  /** */
  public static final QName readWriteTag = AppleServerTags.readWrite;

  /** */
  public static final QName removeTag = AppleServerTags.remove;

  /** */
  public static final QName setTag = AppleServerTags.set;

  /** */
  public static final QName shareTag = AppleServerTags.share;

  /** */
  public static final QName summaryTag = AppleServerTags.summary;

  /** */
  public static final QName bwnameTag = BedeworkServerTags.name;

  /** */
  public static final QName externalUserTag = BedeworkServerTags.externalUser;

  /** */
  public static final QName supportedComponentsTag =
      CaldavTags.supportedCalendarComponentSet;

  /** */
  public static final QName uidTag = AppleServerTags.uid;

  /** */
  public static final QName userTag = AppleServerTags.user;

  private static Map<String, QName> statusToInviteStatus =
      new HashMap<>();

  private static Map<QName, String> inviteStatusToStatus =
      new HashMap<>();

  static {
    setStatusMaps(inviteAcceptedTag);
    setStatusMaps(inviteDeclinedTag);
    setStatusMaps(inviteNoresponseTag);
    setStatusMaps(inviteDeletedTag);
    setStatusMaps(inviteInvalidTag);
  }

  private static final List<BaseNotificationParser> parsers =
      new ArrayList<>();
  static {
    parsers.add(new InviteParser());
    parsers.add(new InviteReplyParser());
  }

  private static void setStatusMaps(final QName val) {
    statusToInviteStatus.put(val.getLocalPart(), val);
    inviteStatusToStatus.put(val, val.getLocalPart());
  }

  /**
   * @return an unmodifiableList of parsers
   */
  public static List<BaseNotificationParser> getParsers() {
    return Collections.unmodifiableList(parsers);
  }

  /**
   * @param val
   * @return String form
   */
  public static String getInviteStatusToStatus(final QName val) {
    return inviteStatusToStatus.get(val);
  }

  /**
   * @param val
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
   * @param val
   * @return parsed Document
   * @throws WebdavException
   */
  public static Document parseXml(final Reader val) throws WebdavException{
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(val));
    } catch (SAXException e) {
      throw parseException(e);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private static abstract class SharingNotificationParser implements BaseNotificationParser {
    private static final int maxPoolSize = 10;
    private final List<Parser> parsers = new ArrayList<>();

    protected Parser parser;

    protected QName element;

    protected SharingNotificationParser(final QName element) {
      this.element = element;
    }

    protected Parser getParser() {
      if (parser != null) {
        return parser;
      }

      synchronized (parsers) {
        if (parsers.size() > 0) {
          parser = parsers.remove(0);
          return parser;
        }

        parser = new Parser();
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

  static class InviteParser extends SharingNotificationParser {
    InviteParser() {
      super(AppleServerTags.inviteNotification);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseInviteNotification(nd);
      } finally {
        putParser();
      }
    }
  }

  static class InviteReplyParser extends SharingNotificationParser {
    InviteReplyParser() {
      super(AppleServerTags.inviteReply);
    }

    @Override
    public BaseNotificationType parse(final Element nd) throws WebdavException {
      try {
        return getParser().parseInviteReply(nd);
      } finally {
        putParser();
      }
    }
  }

  /**
   * @param val XML representation
   * @return populated InviteType object
   * @throws WebdavException
   */
  public InviteType parseInvite(final String val) throws WebdavException {
    Document d = parseXmlString(val);

    return parseInvite(d.getDocumentElement());
  }

  /**
   * @param nd MUST be the invite xml element
   * @return populated InviteType object
   * @throws WebdavException
   */
  public InviteType parseInvite(final Node nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, inviteTag)) {
        throw new WebdavBadRequest("Expected " + inviteTag);
      }

      InviteType in = new InviteType();
      Element[] shareEls = getElementsArray(nd);

      for (final Element curnode: shareEls) {
        if (nodeMatches(curnode, organizerTag)) {
          in.setOrganizer(parseOrganizer(curnode));
          continue;
        }

        if (nodeMatches(curnode, userTag)) {
          in.getUsers().add(parseUser(curnode));
          continue;
        }

        throw new WebdavBadRequest("Expected " + userTag);
      }

      return in;
    } catch (SAXException e) {
      throw parseException(e);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the share xml element
   * @return populated ShareType object
   * @throws WebdavException
   */
  public ShareType parseShare(final Node nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, shareTag)) {
        throw new WebdavBadRequest("Expected " + shareTag);
      }

      ShareType sh = new ShareType();
      Element[] shareEls = getElementsArray(nd);

      for (Element curnode: shareEls) {
        if (nodeMatches(curnode, setTag)) {
          sh.getSet().add(parseSet(curnode));
          continue;
        }

        if (nodeMatches(curnode, removeTag)) {
          sh.getRemove().add(parseRemove(curnode));
          continue;
        }

        throw new WebdavBadRequest("Expected " + setTag + " or " + removeTag);
      }

      return sh;
    } catch (SAXException e) {
      throw parseException(e);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public InviteReplyType parseInviteReply(final String s) throws WebdavException {
    final Document d = parseXmlString(s);
    
    return parseInviteReply(d.getDocumentElement());
  }
  
  /**
   * @param nd MUST be the invite-reply xml element
   * @return populated InviteReplyType object
   * @throws WebdavException on error
   */
  public InviteReplyType parseInviteReply(final Element nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, inviteReplyTag)) {
        throw new WebdavBadRequest("Expected " + inviteReplyTag);
      }

      final InviteReplyType ir = new InviteReplyType();
      ir.setSharedType(getAttrVal(nd, "shared-type"));
      
      final Element[] shareEls = getElementsArray(nd);

      for (final Element curnode: shareEls) {
        if (nodeMatches(curnode, bwnameTag)) {
          ir.setName(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, commonNameTag)) {
          if (ir.getCommonName() != null) {
            throw badInviteReply();
          }

          ir.setCommonName(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, firstNameTag)) {
          continue;
        }

        if (nodeMatches(curnode, lastNameTag)) {
          continue;
        }

        if (nodeMatches(curnode, hrefTag)) {
          if (ir.getHref() != null) {
            throw badInviteReply();
          }

          ir.setHref(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, inviteAcceptedTag)) {
          if (ir.getAccepted() != null) {
            throw badInviteReply();
          }

          ir.setAccepted(true);
          continue;
        }

        if (nodeMatches(curnode, inviteDeclinedTag)) {
          if (ir.getAccepted() != null) {
            throw badInviteReply();
          }

          ir.setAccepted(false);
          continue;
        }

        if (nodeMatches(curnode, hosturlTag)) {
          if (ir.getHostUrl() != null) {
            throw badInviteReply();
          }

          ir.setHostUrl(parseHostUrl(curnode));
          continue;
        }

        if (nodeMatches(curnode, inReplyToTag)) {
          if (ir.getInReplyTo() != null) {
            throw badInviteReply();
          }

          ir.setInReplyTo(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, summaryTag)) {
          if (ir.getSummary() != null) {
            throw badInviteReply();
          }

          ir.setSummary(getElementContent(curnode));
          continue;
        }

        throw badInviteReply();
      }

      if ((ir.getHref() == null) ||
          (ir.getAccepted() == null) ||
          (ir.getHostUrl() == null) ||
          (ir.getInReplyTo() == null)) {
        throw badInviteReply();
      }

      return ir;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the invite-notification xml element
   * @return populated InviteNotificationType object
   * @throws WebdavException
   */
  public InviteNotificationType parseInviteNotification(final Element nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, inviteNotificationTag)) {
        throw new WebdavBadRequest("Expected " + inviteNotificationTag);
      }

      final InviteNotificationType in = new InviteNotificationType();
      in.setSharedType(getAttrVal(nd, "shared-type"));

      final Element[] els = getElementsArray(nd);

      for (Element curnode: els) {
        if (nodeMatches(curnode, bwnameTag)) {
          in.setName(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, uidTag)) {
          if (in.getUid() != null) {
            throw badInviteNotification();
          }

          in.setUid(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, hrefTag)) {
          if (in.getHref() != null) {
            throw badInviteNotification();
          }

          in.setHref(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, inviteAcceptedTag) ||
            nodeMatches(curnode, inviteDeclinedTag) ||
            nodeMatches(curnode, inviteNoresponseTag) ||
            nodeMatches(curnode, inviteDeletedTag)) {
          if (in.getInviteStatus() != null) {
            throw badAccess();
          }

          in.setInviteStatus(new QName(AppleServerTags.appleCaldavNamespace,
                                       curnode.getLocalName()));

          continue;
        }

        if (nodeMatches(curnode, accessTag)) {
          if (in.getAccess() != null) {
            throw badInviteNotification();
          }

          in.setAccess(parseAccess(curnode));
          continue;
        }

        if (nodeMatches(curnode, hosturlTag)) {
          if (in.getHostUrl() != null) {
            throw badInviteNotification();
          }

          in.setHostUrl(parseHostUrl(curnode));
          continue;
        }

        if (nodeMatches(curnode, organizerTag)) {
          if (in.getOrganizer() != null) {
            throw badInviteNotification();
          }

          in.setOrganizer(parseOrganizer(curnode));
          continue;
        }

        if (nodeMatches(curnode, summaryTag)) {
          if (in.getSummary() != null) {
            throw badInviteNotification();
          }

          in.setSummary(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, supportedComponentsTag)) {
          if (!in.getSupportedComponents().isEmpty()) {
            throw badInviteNotification();
          }

          parseSupportedComponents(curnode, in.getSupportedComponents());
          continue;
        }

        throw badInviteNotification();
      }

      if ((in.getUid() == null) ||
          (in.getHref() == null) ||
          (in.getHostUrl() == null) ||
          (in.getOrganizer() == null)) {
        throw badInviteNotification();
      }

      if (!in.getInviteStatus().equals(AppleServerTags.inviteDeleted) &&
          (in.getAccess() == null)) {
        throw badInviteNotification();
      }

      return in;
    } catch (SAXException e) {
      throw parseException(e);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the user xml element
   * @return populated UserType object
   * @throws WebdavException
   */
  public UserType parseUser(final Node nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, userTag)) {
        throw new WebdavBadRequest("Expected " + userTag);
      }

      final UserType u = new UserType();
      final Element[] shareEls = getElementsArray(nd);
      boolean parsedExternalElement = false;

      for (final Element curnode: shareEls) {
        if (nodeMatches(curnode, hrefTag)) {
          if (u.getHref() != null) {
            throw badUser();
          }

          u.setHref(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, commonNameTag)) {
          if (u.getCommonName() != null) {
            throw badUser();
          }

          u.setCommonName(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, inviteAcceptedTag) ||
            nodeMatches(curnode, inviteDeclinedTag) ||
            nodeMatches(curnode, inviteNoresponseTag) ||
            nodeMatches(curnode, inviteDeletedTag)) {
          if (u.getInviteStatus() != null) {
            throw badAccess();
          }

          u.setInviteStatus(new QName(
                  AppleServerTags.appleCaldavNamespace,
                  curnode.getLocalName()));

          continue;
        }

        if (nodeMatches(curnode, accessTag)) {
          if (u.getAccess() != null) {
            throw badUser();
          }

          u.setAccess(parseAccess(curnode));
          continue;
        }

        if (nodeMatches(curnode, summaryTag)) {
          if (u.getSummary() != null) {
            throw badUser();
          }

          u.setSummary(getElementContent(curnode));
          continue;
        }

        if (nodeMatches(curnode, externalUserTag)) {
          if (parsedExternalElement) {
            throw badUser();
          }

          parsedExternalElement = true;
          u.setExternalUser(Boolean.parseBoolean(getElementContent(curnode)));
          continue;
        }

        throw badInviteNotification();
      }

      if ((u.getHref() == null) ||
          (u.getInviteStatus() == null) ||
          (u.getAccess() == null)) {
        throw badUser();
      }

      return u;
    } catch (final SAXException e) {
      throw parseException(e);
    } catch (final WebdavException wde) {
      throw wde;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void parseSupportedComponents(final Node nd,
                                        final List<String> comps) throws Throwable {
    for (Element curnode: getElementsArray(nd)) {
      if (!nodeMatches(curnode, compTag)) {
        throw badComps();
      }

      comps.add(getAttrVal(curnode, "name"));
    }
  }

  private AccessType parseAccess(final Node nd) throws Throwable {
    AccessType a = new AccessType();

    Element[] els = getElementsArray(nd);

    for (Element curnode: els) {
      if (nodeMatches(curnode, readTag) ||
          nodeMatches(curnode, readWriteTag)) {
        if ((a.getRead() != null) || (a.getReadWrite() != null)) {
          throw badAccess();
        }

        if (nodeMatches(curnode, readTag)) {
          a.setRead(true);
        } else {
          a.setReadWrite(true);
        }

        continue;
      }

      throw badAccess();
    }

    if ((a.getRead() == null) && (a.getReadWrite() == null)) {
      throw badAccess();
    }

    return a;
  }

  private SetType parseSet(final Node nd) throws Throwable {
    SetType s = new SetType();

    Element[] els = getElementsArray(nd);

    for (Element curnode: els) {
      if (nodeMatches(curnode, hrefTag)) {
        if (s.getHref() != null) {
          throw badSet();
        }

        s.setHref(getElementContent(curnode));
        continue;
      }

      if (nodeMatches(curnode, commonNameTag)) {
        if (s.getCommonName() != null) {
          throw badSet();
        }

        s.setCommonName(getElementContent(curnode));
        continue;
      }

      if (nodeMatches(curnode, summaryTag)) {
        if (s.getSummary() != null) {
          throw badSet();
        }

        s.setSummary(getElementContent(curnode));
        continue;
      }

      if (nodeMatches(curnode, readTag) ||
          nodeMatches(curnode, readWriteTag)) {
        if (s.getAccess() != null) {
          throw badSet();
        }

        AccessType a = new AccessType();

        if (nodeMatches(curnode, readTag)) {
          a.setRead(true);
        } else {
          a.setReadWrite(true);
        }

        s.setAccess(a);
        continue;
      }

      throw badSet();
    }

    if (s.getHref() == null) {
      throw badSet();
    }

    if (s.getAccess() == null) {
      throw badSet();
    }

    return s;
  }

  private RemoveType parseRemove(final Node nd) throws Throwable {
    RemoveType r = new RemoveType();

    Element[] els = getElementsArray(nd);

    for (Element curnode: els) {
      if (nodeMatches(curnode, hrefTag)) {
        if (r.getHref() != null) {
          throw badRemove();
        }

        r.setHref(getElementContent(curnode));
        continue;
      }

      throw badRemove();
    }

    if (r.getHref() == null) {
      throw badRemove();
    }

    return r;
  }

  private OrganizerType parseOrganizer(final Node nd) throws Throwable {
    OrganizerType o = new OrganizerType();

    Element[] els = getElementsArray(nd);

    for (Element curnode: els) {
      if (nodeMatches(curnode, hrefTag)) {
        if (o.getHref() != null) {
          throw badOrganizer();
        }

        o.setHref(getElementContent(curnode));
        continue;
      }

      if (nodeMatches(curnode, commonNameTag)) {
        if (o.getCommonName() != null) {
          throw badOrganizer();
        }

        o.setCommonName(getElementContent(curnode));
        continue;
      }

      throw badOrganizer();
    }

    return o;
  }

  private String parseHostUrl(final Node nd) throws WebdavException {
    try {
      if (!nodeMatches(nd, hosturlTag)) {
        throw new WebdavBadRequest("Expected " + hosturlTag);
      }

      String href = null;
      Element[] els = getElementsArray(nd);

      for (Element curnode: els) {
        if (nodeMatches(curnode, hrefTag)) {
          if (href != null) {
            throw badHostUrl();
          }

          href = getElementContent(curnode);
          continue;
        }

        throw badHostUrl();
      }

      if (href == null) {
        throw badHostUrl();
      }

      return href;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private WebdavBadRequest badHostUrl() {
    return new WebdavBadRequest("Expected " +
        hrefTag);
  }

  private WebdavBadRequest badAccess() {
    return new WebdavBadRequest("Expected " +
        readTag + " or " + readWriteTag);
  }

  private WebdavBadRequest badSet() {
    return new WebdavBadRequest("Expected " +
        hrefTag +
        ", " + commonNameTag +
        "(optional), " + summaryTag +
        "(optional), (" + readTag + " or " + readWriteTag + ")");
  }

  private WebdavBadRequest badRemove() {
    return new WebdavBadRequest("Expected " +
        hrefTag);
  }

  private WebdavBadRequest badOrganizer() {
    return new WebdavBadRequest("Expected " +
        hrefTag +
        ", " + commonNameTag);
  }

  private WebdavBadRequest badComps() {
    return new WebdavBadRequest("Expected " +
        compTag);
  }

  private WebdavBadRequest badInviteNotification() {
    return new WebdavBadRequest("Expected " +
        uidTag +
        ", " + hrefTag +
        ", (" + inviteNoresponseTag + " or " + inviteDeclinedTag +
             " or " + inviteDeletedTag + " or " + inviteAcceptedTag + "), " +
        hosturlTag +
        ", " + organizerTag +
        ", " + summaryTag + "(optional)");
  }

  private WebdavBadRequest badInviteReply() {
    return new WebdavBadRequest("Expected " +
        hrefTag +
        ", (" + inviteAcceptedTag + " or " + inviteDeclinedTag + "), " +
        hosturlTag +
        ", " + inReplyToTag +
        ", " + summaryTag + "(optional)");
  }

  private WebdavBadRequest badUser() {
    return new WebdavBadRequest("Expected " +
        hrefTag +
        ", " + commonNameTag +
        "(optional), (" + inviteNoresponseTag + " or " + inviteDeclinedTag +
             " or " + inviteDeletedTag + " or " + inviteAcceptedTag + "), " +
        ", " + summaryTag + "(optional)");
  }

  private static WebdavException parseException(final SAXException e) {
    return new WebdavBadRequest();
  }
}
