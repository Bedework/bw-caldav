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

import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.notifications.NotificationType;
import org.bedework.caldav.util.notifications.eventreg.EventregCancelledNotificationType;
import org.bedework.caldav.util.notifications.eventreg.EventregRegisteredNotificationType;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.WebdavTags;
import org.bedework.webdav.servlet.shared.WebdavException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Class called to handle incoming notify service requests.
 *
 *   @author Mike Douglass   douglm - rpi.edu
 */
public class BwNotifyHandler {
  public void doNotify(final CaldavBWIntf intf,
                       final RequestPars pars,
                       final HttpServletResponse resp)
          throws WebdavException {
    if (!pars.processXml()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      final SysIntf sysi = intf.getSysi();

      final Node root = pars.getXmlDoc().getDocumentElement();

      if (XmlUtil.nodeMatches(root,
                              BedeworkServerTags.eventregCancelled)) {
        doEventregCancel(root, sysi, resp);
        return;
      }

      if (XmlUtil.nodeMatches(root,
                              BedeworkServerTags.eventregRegistered)) {
        doEventregReg(root, sysi, resp);
        return;
      }

      if (XmlUtil.nodeMatches(root,
                              BedeworkServerTags.notifySubscribe)) {
        doNotifySubscribe(root, sysi, resp);
        return;
      }

      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doEventregCancel(final Node root,
                                final SysIntf sysi,
                                final HttpServletResponse resp)
          throws WebdavException {
    try {
      final List<Element> els = XmlUtil.getElements(root);

      if (els.size() < 2) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      // Require event href first

      final String href = mustHref(els.get(0), resp);
      if (href == null) {
        return;
      }

      final String uid = mustUid(els.get(1), resp);
      if (uid == null) {
        return;
      }

      int index = 2;

      // Remaining nodes should be principalURLs

      while (index < els.size()) {
        final String principalHref = mustPrincipalHref(els.get(index),
                                                       resp);
        if (principalHref == null) {
          return;
        }

        index++;

        final EventregCancelledNotificationType ecnt =
                new EventregCancelledNotificationType();

        ecnt.setUid(uid);
        ecnt.setHref(href);
        ecnt.setPrincipalHref(principalHref);
        // comment?

        final NotificationType note = new NotificationType();
        note.setNotification(ecnt);

        sysi.sendNotification(ecnt.getPrincipalHref(), note);
      }
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }

  }

  private void doEventregReg(final Node root,
                             final SysIntf sysi,
                             final HttpServletResponse resp)
          throws WebdavException {
    try {
      final List<Element> els = XmlUtil.getElements(root);

      if (els.size() < 2) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      // Require event href first

      final String href = mustHref(els.get(0), resp);
      if (href == null) {
        return;
      }

      final String uid = mustUid(els.get(1), resp);
      if (uid == null) {
        return;
      }

      final Integer numTicketsRequested =
              mustInt(els.get(2),
                      BedeworkServerTags.eventregNumTicketsRequested,
                      resp);

      final Integer numTickets = mustInt(els.get(3),
                                         BedeworkServerTags.eventregNumTickets,
                                         resp);
      if (numTickets == null) {
        return;
      }

      final String principalHref = mustPrincipalHref(els.get(4),
                                                     resp);
      if (principalHref == null) {
        return;
      }

      final EventregRegisteredNotificationType ereg =
              new EventregRegisteredNotificationType();

      ereg.setUid(uid);
      ereg.setHref(href);
      ereg.setNumTicketsRequested(numTicketsRequested);
      ereg.setNumTickets(numTickets);
      ereg.setPrincipalHref(principalHref);
      // comment?

      final NotificationType note = new NotificationType();
      note.setNotification(ereg);

      sysi.sendNotification(ereg.getPrincipalHref(), note);
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private void doNotifySubscribe(final Node root,
                                 final SysIntf sysi,
                                 final HttpServletResponse resp)
          throws WebdavException {
    try {
      final List<Element> els = XmlUtil.getElements(root);

      /* Expect
          <principal-href><href>principal</href></principal-href>
          <action>add</action>
          <email>email-address</email>

          email may be repeated
       */
      if (els.size() < 3) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      // Require principal href first

      final String principalHref = mustPrincipalHref(els.get(0),
                                                     resp);
      if (principalHref == null) {
        return;
      }

      final String action = must(els.get(1),
                                 BedeworkServerTags.action,
                                 resp);
      if (action == null) {
        return;
      }

      final List<String> emails = new ArrayList<>();

      for (int i = 2; i < els.size(); i++) {
        final String email = must(els.get(i),
                                  BedeworkServerTags.email,
                                  resp);
        if (email == null) {
          return;
        }

        emails.add(email);
      }

      if (!sysi.subscribeNotification(principalHref, action, emails)) {
        resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
      }
    } catch (final WebdavException we) {
      throw we;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private String mustHref(final Element el,
                          final HttpServletResponse resp)
          throws WebdavException {
    return must(el, WebdavTags.href, resp);
  }

  private String mustUid(final Element el,
                         final HttpServletResponse resp)
          throws WebdavException {
    return must(el, AppleServerTags.uid, resp);
  }

  private String must(final Element el,
                      final QName tag,
                      final HttpServletResponse resp)
          throws WebdavException {
    try {
      if (!isElement(el, tag, resp)) {
        return null;
      }

      return XmlUtil.getElementContent(el);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private Integer mustInt(final Element el,
                      final QName tag,
                      final HttpServletResponse resp)
          throws WebdavException {
    final String val = must(el, tag, resp);

    if (val == null) {
      return null;
    }

    return Integer.parseInt(val);
  }

  private boolean isElement(final Element el,
                            final QName tag,
                            final HttpServletResponse resp)
          throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(el, tag)) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return false;
      }

      return true;
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }

  private String mustPrincipalHref(final Element el,
                                   final HttpServletResponse resp)
          throws WebdavException {
    try {
      if (!XmlUtil.nodeMatches(el, WebdavTags.principalURL)) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }

      final Element chEl = XmlUtil.getOnlyElement(el);
      if (!XmlUtil.nodeMatches(chEl, WebdavTags.href)) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return null;
      }

      return XmlUtil.getElementContent(chEl);
    } catch (final Throwable t) {
      throw new WebdavException(t);
    }
  }
}