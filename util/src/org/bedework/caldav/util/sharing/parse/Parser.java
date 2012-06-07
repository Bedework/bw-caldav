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

import org.bedework.caldav.util.sharing.AccessType;
import org.bedework.caldav.util.sharing.InviteNotificationType;
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.OrganizerType;
import org.bedework.caldav.util.sharing.RemoveType;
import org.bedework.caldav.util.sharing.SetType;
import org.bedework.caldav.util.sharing.ShareType;
import org.bedework.caldav.util.sharing.UserType;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Class to parse properties and requests related to CalDAV sharing
 * (as defined by Apple).
 *
 * @author Mike Douglass douglm
 */
public class Parser {
  private static QName accessTag = AppleServerTags.access;

  private static QName commonNameTag = AppleServerTags.commonName;

  private static QName hosturlTag = AppleServerTags.hosturl;

  private static QName hrefTag = WebdavTags.href;

  private static QName inReplyToTag = AppleServerTags.inReplyTo;

  private static QName inviteAcceptedTag = AppleServerTags.inviteAccepted;

  private static QName inviteDeclinedTag = AppleServerTags.inviteDeclined;

  private static QName inviteDeletedTag = AppleServerTags.inviteDeleted;

  private static QName inviteInvalidTag = AppleServerTags.inviteInvalid;

  private static QName inviteNoresponseTag = AppleServerTags.inviteNoresponse;

  private static QName inviteNotificationTag = AppleServerTags.inviteNotification;

  private static QName inviteReplyTag = AppleServerTags.inviteReply;

  private static QName organizerTag = AppleServerTags.organizer;

  private static QName readTag = AppleServerTags.read;

  private static QName readWriteTag = AppleServerTags.readWrite;

  private static QName removeTag = AppleServerTags.remove;

  private static QName setTag = AppleServerTags.set;

  private static QName shareTag = AppleServerTags.share;

  private static QName summaryTag = AppleServerTags.summary;

  private static QName uidTag = AppleServerTags.uid;

  private static QName userTag = AppleServerTags.user;

  /**
   * @param val
   * @return parsed Document
   * @throws WebdavException
   */
  public static Document parseXmlString(final String val) throws WebdavException{
    if ((val == null) || (val.length() == 0)) {
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(val));
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the share xml element
   * @return populated ShareType object
   * @throws WebdavException
   */
  public ShareType parseShareType(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, shareTag)) {
        throw new WebdavBadRequest("Expected " + shareTag);
      }

      ShareType sh = new ShareType();
      Element[] shareEls = XmlUtil.getElementsArray(nd);

      for (Element curnode: shareEls) {
        if (XmlUtil.nodeMatches(curnode, setTag)) {
          sh.getSet().add(parseSet(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, removeTag)) {
          sh.getRemove().add(parseRemove(curnode));
          continue;
        }

        throw new WebdavBadRequest("Expected " + setTag + " or " + removeTag);
      }

      return sh;
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param nd MUST be the invite-reply xml element
   * @return populated InviteReplyType object
   * @throws WebdavException
   */
  public InviteReplyType parseInviteReply(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, inviteReplyTag)) {
        throw new WebdavBadRequest("Expected " + inviteReplyTag);
      }

      InviteReplyType ir = new InviteReplyType();
      Element[] shareEls = XmlUtil.getElementsArray(nd);

      for (Element curnode: shareEls) {
        if (XmlUtil.nodeMatches(curnode, hrefTag)) {
          if (ir.getHref() != null) {
            throw badInviteReply();
          }

          ir.setHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteAcceptedTag)) {
          if (ir.getAccepted() != null) {
            throw badInviteReply();
          }

          ir.setAccepted(true);
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteDeclinedTag)) {
          if (ir.getAccepted() != null) {
            throw badInviteReply();
          }

          ir.setAccepted(false);
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, hosturlTag)) {
          if (ir.getHostUrl() != null) {
            throw badInviteReply();
          }

          ir.setHostUrl(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inReplyToTag)) {
          if (ir.getInReplyTo() != null) {
            throw badInviteReply();
          }

          ir.setInReplyTo(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, summaryTag)) {
          if (ir.getSummary() != null) {
            throw badInviteReply();
          }

          ir.setSummary(XmlUtil.getElementContent(curnode));
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
    } catch (SAXException e) {
      throw new WebdavBadRequest();
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
  public InviteNotificationType parseInviteNotification(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, inviteNotificationTag)) {
        throw new WebdavBadRequest("Expected " + inviteNotificationTag);
      }

      InviteNotificationType in = new InviteNotificationType();
      Element[] shareEls = XmlUtil.getElementsArray(nd);

      for (Element curnode: shareEls) {
        if (XmlUtil.nodeMatches(curnode, uidTag)) {
          if (in.getUid() != null) {
            throw badInviteNotification();
          }

          in.setUid(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, hrefTag)) {
          if (in.getHref() != null) {
            throw badInviteNotification();
          }

          in.setHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteAcceptedTag) ||
            XmlUtil.nodeMatches(curnode, inviteDeclinedTag) ||
            XmlUtil.nodeMatches(curnode, inviteNoresponseTag) ||
            XmlUtil.nodeMatches(curnode, inviteDeletedTag)) {
          if (in.getStatus() != null) {
            throw badAccess();
          }

          in.setStatus(curnode.getLocalName());

          continue;
        }

        if (XmlUtil.nodeMatches(curnode, accessTag)) {
          if (in.getAccess() != null) {
            throw badInviteNotification();
          }

          in.setAccess(parseAccess(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, hosturlTag)) {
          if (in.getHostUrl() != null) {
            throw badInviteNotification();
          }

          in.setHostUrl(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, organizerTag)) {
          if (in.getOrganizer() != null) {
            throw badInviteNotification();
          }

          in.setOrganizer(parseOrganizer(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, summaryTag)) {
          if (in.getSummary() != null) {
            throw badInviteNotification();
          }

          in.setSummary(XmlUtil.getElementContent(curnode));
          continue;
        }

        throw badInviteNotification();
      }

      if ((in.getUid() == null) ||
          (in.getHref() == null) ||
          (in.getAccess() == null) ||
          (in.getHostUrl() == null) ||
          (in.getOrganizer() == null)) {
        throw badInviteNotification();
      }

      return in;
    } catch (SAXException e) {
      throw new WebdavBadRequest();
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
      if (!XmlUtil.nodeMatches(nd, userTag)) {
        throw new WebdavBadRequest("Expected " + userTag);
      }

      UserType u = new UserType();
      Element[] shareEls = XmlUtil.getElementsArray(nd);

      for (Element curnode: shareEls) {
        if (XmlUtil.nodeMatches(curnode, hrefTag)) {
          if (u.getHref() != null) {
            throw badUser();
          }

          u.setHref(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, commonNameTag)) {
          if (u.getCommonName() != null) {
            throw badUser();
          }

          u.setCommonName(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteAcceptedTag) ||
            XmlUtil.nodeMatches(curnode, inviteDeclinedTag) ||
            XmlUtil.nodeMatches(curnode, inviteNoresponseTag) ||
            XmlUtil.nodeMatches(curnode, inviteDeletedTag)) {
          if (u.getStatus() != null) {
            throw badAccess();
          }

          u.setStatus(curnode.getLocalName());

          continue;
        }

        if (XmlUtil.nodeMatches(curnode, accessTag)) {
          if (u.getAccess() != null) {
            throw badUser();
          }

          u.setAccess(parseAccess(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, summaryTag)) {
          if (u.getSummary() != null) {
            throw badUser();
          }

          u.setSummary(XmlUtil.getElementContent(curnode));
          continue;
        }

        throw badInviteNotification();
      }

      if ((u.getHref() == null) ||
          (u.getStatus() == null) ||
          (u.getAccess() == null)) {
        throw badUser();
      }

      return u;
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private AccessType parseAccess(final Node nd) throws Throwable {
    AccessType a = new AccessType();

    Element[] els = XmlUtil.getElementsArray(nd);

    for (Element curnode: els) {
      if (XmlUtil.nodeMatches(curnode, readTag) ||
          XmlUtil.nodeMatches(curnode, readWriteTag)) {
        if ((a.getRead() != null) || (a.getReadWrite() != null)) {
          throw badAccess();
        }

        if (XmlUtil.nodeMatches(curnode, readTag)) {
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

    Element[] els = XmlUtil.getElementsArray(nd);

    for (Element curnode: els) {
      if (XmlUtil.nodeMatches(curnode, hrefTag)) {
        if (s.getHref() != null) {
          throw badSet();
        }

        s.setHref(XmlUtil.getElementContent(curnode));
        continue;
      }

      if (XmlUtil.nodeMatches(curnode, commonNameTag)) {
        if (s.getCommonName() != null) {
          throw badSet();
        }

        s.setCommonName(XmlUtil.getElementContent(curnode));
        continue;
      }

      if (XmlUtil.nodeMatches(curnode, summaryTag)) {
        if (s.getSummary() != null) {
          throw badSet();
        }

        s.setSummary(XmlUtil.getElementContent(curnode));
        continue;
      }

      if (XmlUtil.nodeMatches(curnode, readTag) ||
          XmlUtil.nodeMatches(curnode, readWriteTag)) {
        if (s.getAccess() != null) {
          throw badSet();
        }

        AccessType a = new AccessType();

        if (XmlUtil.nodeMatches(curnode, readTag)) {
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

    Element[] els = XmlUtil.getElementsArray(nd);

    for (Element curnode: els) {
      if (XmlUtil.nodeMatches(curnode, hrefTag)) {
        if (r.getHref() != null) {
          throw badRemove();
        }

        r.setHref(XmlUtil.getElementContent(curnode));
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

    Element[] els = XmlUtil.getElementsArray(nd);

    for (Element curnode: els) {
      if (XmlUtil.nodeMatches(curnode, hrefTag)) {
        if (o.getHref() != null) {
          throw badOrganizer();
        }

        o.setHref(XmlUtil.getElementContent(curnode));
        continue;
      }

      if (XmlUtil.nodeMatches(curnode, commonNameTag)) {
        if (o.getCommonName() != null) {
          throw badOrganizer();
        }

        o.setCommonName(XmlUtil.getElementContent(curnode));
        continue;
      }

      throw badOrganizer();
    }

    return o;
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
}
