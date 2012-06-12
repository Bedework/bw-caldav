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

import org.bedework.caldav.server.PostMethod.RequestPars;
import org.bedework.caldav.server.sysinterface.SysIntf;
import org.bedework.caldav.util.notifications.NotificationType;
import org.bedework.caldav.util.sharing.InviteNotificationType;
import org.bedework.caldav.util.sharing.InviteReplyType;
import org.bedework.caldav.util.sharing.InviteType;
import org.bedework.caldav.util.sharing.OrganizerType;
import org.bedework.caldav.util.sharing.RemoveType;
import org.bedework.caldav.util.sharing.SetType;
import org.bedework.caldav.util.sharing.ShareType;
import org.bedework.caldav.util.sharing.SharedAsType;
import org.bedework.caldav.util.sharing.UserType;
import org.bedework.caldav.util.sharing.parse.Parser;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cmt.access.AccessException;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.AceWho;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.PrivilegeSet;
import edu.rpi.sss.util.Uid;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.tagdefs.AppleServerTags;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.DtStamp;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

/** This type of object will handle sharing operations.
 *
 * @author Mike Douglass
 */
public class SharingHandler implements PrivilegeDefs {
  private static final QName removeStatus = Parser.inviteDeletedTag;

  private static final QName noresponseStatus = Parser.inviteNoresponseTag;

  private CaldavBWIntf intf;

  /** Handle sharing
   *
   * @param intf
   */
  public SharingHandler(final CaldavBWIntf intf) {
    this.intf = intf;
  }

  /**
   * @param node MUST be a sharable node
   * @param root MUST be the share element
   * @throws WebdavException
   */
  public void share(final WebdavNsNode node,
                    final Element root) throws WebdavException {
    if (!node.isCollection() || !node.getCollection(false).getCanShare()) {
      throw new WebdavForbidden("Cannot share");
    }

    SysIntf sysi = intf.getSysi();

    CaldavCalNode calnode = (CaldavCalNode)node;
    CalDAVCollection col = (CalDAVCollection)calnode.getCollection(false);

    WebdavNsIntf.AclInfo ainfo = new WebdavNsIntf.AclInfo(node.getUri());
    ainfo.acl = node.getCurrentAccess().getAcl();
    String calAddr = sysi.principalToCaladdr(sysi.getPrincipal());

    Parser parse = new Parser();

    String inviteStr = calnode.getSharingStatus();
    InviteType invite;

    if (inviteStr == null) {
      invite = new InviteType();
    } else {
      invite = parse.parseInvite(inviteStr);
    }

    ShareType share = parse.parseShare(root);
    List<InviteNotificationType> notifications =
        new ArrayList<InviteNotificationType>();

    boolean addedSharee = false;

    for (RemoveType rem: share.getRemove()) {
      InviteNotificationType n = doRemove(rem, ainfo, calAddr, invite);

      if (n != null) {
        notifications.add(n);
      }
    }

    for(SetType set: share.getSet()) {
      InviteNotificationType n = doSet(set, ainfo, calAddr, invite);

      if (n != null) {
        addedSharee = true;
        notifications.add(n);
      }
    }

    if (notifications.isEmpty()) {
      // Nothing changed
      return;
    }

    intf.updateAccess(ainfo, calnode);

    /* Send the invitations and update the sharing status.
     * If it's a removal and the current status is not
     * accepted then just delete the current invitation
     */

    sendNotifications:
    for (InviteNotificationType in: notifications) {
      boolean remove = in.getInviteStatus().equals(removeStatus);

      List<NotificationType> notes = sysi.getNotifications(in.getHref(),
                                                           AppleServerTags.inviteNotification);

      if (!Util.isEmpty(notes)) {
        for (NotificationType n: notes) {
          InviteNotificationType nin = (InviteNotificationType)n.getNotification();

          if (!nin.getHostUrl().equals(in.getHostUrl())) {
            continue;
          }

          /* If it's a removal and the current status is not
           * accepted then just delete the current invitation
           *
           * If it's not a removal - remove the current one and add the new one
           */

          if (remove) {
            if (nin.getInviteStatus().equals(noresponseStatus)) {
              sysi.removeNotification(in.getHref(),
                                      n);
              continue sendNotifications;
            }
          } else {
            sysi.removeNotification(in.getHref(),
                                    n);
          }
        }
      }

      NotificationType note = new NotificationType();

      note.setDtstamp(new DtStamp(new DateTime(true)).getValue());
      note.setNotification(in);

      sysi.sendNotification(in.getHref(), note);

      /* Add the invite to the set of properties associated with this collection
       * We give it a name consisting of the inviteNotification tag + uid.
       */
      QName qn = new QName(AppleServerTags.inviteNotification.getNamespaceURI(),
                           AppleServerTags.inviteNotification.getLocalPart() + in.getUid());
      try {
        col.setProperty(qn, in.toXml());
      } catch (WebdavException we) {
        throw we;
      } catch (Throwable t) {
        throw new WebdavException(t);
      }
    }

    if (addedSharee) {
      // Mark the collection as shared?
      col.setProperty(AppleServerTags.shared, "true");
    }

    sysi.updateCollection((CalDAVCollection)calnode.getCollection(false));
  }

  /**
   * @param pars for the POST method
   * @param root MUST be the share element
   * @return null for any failure - or a SharedAsType.
   * @throws WebdavException
   */
  public SharedAsType reply(final RequestPars pars,
                            final Element root) throws WebdavException {
    SysIntf sysi = intf.getSysi();

    Parser parse = new Parser();

    InviteReplyType reply = parse.parseInviteReply(root);

    String newUri = sysi.sharingReply(pars.resourceUri, reply);

    if (newUri == null) {
      return null;
    }

    SharedAsType sa = new SharedAsType();

    sa.setHref(newUri);

    return sa;
  }

  private InviteNotificationType doRemove(final RemoveType rem,
                                          final WebdavNsIntf.AclInfo ainfo,
                                          final String calAddr,
                                          final InviteType invite) throws WebdavException {
    if (ainfo.acl == null) {
      return null;
    }

    String href = rem.getHref();

    UserType uentry = null;
    for (UserType u: invite.getUsers()) {
      if (u.getHref().equals(href)) {
        uentry = u;
        break;
      }
    }

    if (uentry == null) {
      // Not in list of sharers
      return null;
    }

    invite.getUsers().remove(uentry);

    try {
      if (Util.isEmpty(ainfo.acl.getAces())) {
        return null;
      }

      SysIntf sysi = intf.getSysi();

      AccessPrincipal ap = sysi.caladdrToPrincipal(href);

      AceWho who = AceWho.getAceWho(ap.getAccount(), ap.getKind(), false);

      Acl newAcl = ainfo.acl.removeWho(who);

      if (newAcl == null) {
        // no change
        return null;
      }

      InviteNotificationType in = new InviteNotificationType();

      in.setUid(Uid.getUid());
      in.setHref(href);
      in.setInviteStatus(removeStatus);
      // in.setAccess(xxx); <-- current access from sharing status?
      in.setHostUrl(ainfo.what);

      OrganizerType org = new OrganizerType();
      org.setHref(calAddr);

      in.setOrganizer(org);

      return in;
    } catch (AccessException ae) {
      throw new WebdavException(ae);
    }
  }


  /** Read-write privileges
   */
  private static PrivilegeSet readWritePrivileges =
    new PrivilegeSet(denied,   // privAll
                     allowed,   // privRead
                     denied,   // privReadAcl
                     allowed,   // privReadCurrentUserPrivilegeSet
                     allowed,   // privReadFreeBusy
                     denied,   // privWrite
                     denied,   // privWriteAcl
                     denied,   // privWriteProperties
                     allowed,   // privWriteContent
                     denied,   // privBind
                     denied,   // privSchedule
                     denied,   // privScheduleRequest
                     denied,   // privScheduleReply
                     denied,   // privScheduleFreeBusy
                     denied,   // privUnbind
                     allowed,   // privUnlock

                     denied,   // privScheduleDeliver
                     denied,   // privScheduleDeliverInvite
                     denied,   // privScheduleDeliverReply
                     denied,  // privScheduleQueryFreebusy

                     denied,   // privScheduleSend
                     denied,   // privScheduleSendInvite
                     denied,   // privScheduleSendReply
                     denied,   // privScheduleSendFreebusy
                     allowed);   // privNone

  private static PrivilegeSet readOnlyPrivileges = PrivilegeSet.readOnlyPrivileges;

  private InviteNotificationType doSet(final SetType s,
                                       final WebdavNsIntf.AclInfo ainfo,
                                       final String calAddr,
                                       final InviteType invite) throws WebdavException {
    try {

      String href = s.getHref();

      UserType uentry = null;
      for (UserType u: invite.getUsers()) {
        if (u.getHref().equals(href)) {
          uentry = u;
          break;
        }
      }

      SysIntf sysi = intf.getSysi();

      AccessPrincipal ap = sysi.caladdrToPrincipal(href);

      AceWho who = AceWho.getAceWho(ap.getAccount(), ap.getKind(), false);

      PrivilegeSet desiredPriv;

      if (s.getAccess().testRead()) {
        desiredPriv = readOnlyPrivileges;
      } else {
        desiredPriv = readWritePrivileges;
      }

      boolean removeCurrentPrivs = false;

      for (Ace a: ainfo.acl.getAces()) {
        if (a.getWho().equals(who)) {
          if (a.getHow().equals(desiredPriv)) {
            // Already have that access
            return null;
          }

          removeCurrentPrivs = true;
        }
      }

      if (removeCurrentPrivs) {
        ainfo.acl = ainfo.acl.removeWho(who);
      }

      Collection<Ace> aces = ainfo.acl.getAces();

      aces.add(Ace.makeAce(who, desiredPriv.getPrivs(), null));

      ainfo.acl = new Acl(aces);

      InviteNotificationType in = new InviteNotificationType();

      in.setUid(Uid.getUid());
      in.setHref(href);
      in.setInviteStatus(Parser.inviteNoresponseTag);
      in.setAccess(s.getAccess());
      in.setHostUrl(ainfo.what);

      OrganizerType org = new OrganizerType();
      org.setHref(calAddr);

      in.setOrganizer(org);

      // Update the collection sharing status
      if (uentry != null) {
        uentry.setInviteStatus(in.getInviteStatus());
      } else {
        uentry = new UserType();

        uentry.setHref(href);
        uentry.setInviteStatus(in.getInviteStatus());
        uentry.setCommonName(s.getCommonName());
        uentry.setAccess(in.getAccess());
        uentry.setSummary(s.getSummary());

        invite.getUsers().add(uentry);
      }

      return in;
    } catch (AccessException ae) {
      throw new WebdavException(ae);
    }
  }
}
