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
package org.bedework.caldav.util.notifications.parse;

import org.bedework.caldav.util.notifications.NotificationType;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.xml.XmlUtil;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;

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
  /* Notifications we know about */

  private static QName inviteNotificationTag = AppleServerTags.inviteNotification;

  private static QName inviteReplyTag = AppleServerTags.inviteReply;

  /* General notifications elements */

  private static QName dtstampTag = AppleServerTags.dtstamp;

  private static QName notificationTag = AppleServerTags.notification;

  private org.bedework.caldav.util.sharing.parse.Parser sharingParser;

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
   * @param nd MUST be the notification xml element
   * @return populated ShareType object
   * @throws WebdavException
   */
  public NotificationType parseNotification(final Node nd) throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(nd, notificationTag)) {
        throw new WebdavBadRequest("Expected " + notificationTag);
      }

      NotificationType n = new NotificationType();
      Element[] els = XmlUtil.getElementsArray(nd);

      for (Element curnode: els) {
        if (XmlUtil.nodeMatches(curnode, dtstampTag)) {
          n.setDtstamp(XmlUtil.getElementContent(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteNotificationTag)) {
          if (n.getNotification() != null) {
            throw badNotification(curnode);
          }

          n.setNotification(getSharingParser().parseInviteNotification(curnode));
          continue;
        }

        if (XmlUtil.nodeMatches(curnode, inviteReplyTag)) {
          if (n.getNotification() != null) {
            throw badNotification(curnode);
          }

          n.setNotification(getSharingParser().parseInviteReply(curnode));
          continue;
        }

        throw badNotification(curnode);
      }

      return n;
    } catch (SAXException e) {
      throw new WebdavBadRequest();
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  private org.bedework.caldav.util.sharing.parse.Parser getSharingParser() {
    if (sharingParser == null) {
      sharingParser = new org.bedework.caldav.util.sharing.parse.Parser();
    }

    return sharingParser;
  }

  private WebdavBadRequest badNotification(final Element curnode) {
    return new WebdavBadRequest("Unexpected element " + curnode);
  }
}
