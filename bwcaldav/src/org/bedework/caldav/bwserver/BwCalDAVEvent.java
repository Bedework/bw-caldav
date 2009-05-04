/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.caldav.bwserver;

import org.bedework.caldav.server.CalDAVEvent;
import org.bedework.caldav.server.Organizer;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.BwOrganizer;
import org.bedework.calfacade.CalFacadeDefs;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.icalendar.IcalTranslator;

import edu.rpi.cct.webdav.servlet.shared.WdEntity;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.sss.util.xml.XmlEmit;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.ICalTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 *
 * @author douglm
 *
 */
public class BwCalDAVEvent extends CalDAVEvent {
  private BwSysIntfImpl intf;

  private EventInfo evi;
  private BwEvent ev;

  /**
   * @param intf
   * @param evi
   * @throws WebdavException
   */
  BwCalDAVEvent(BwSysIntfImpl intf, EventInfo evi) throws WebdavException {
    this.intf = intf;
    this.evi = evi;

    if (evi != null) {
      ev = evi.getEvent();
    }
  }

  /* ====================================================================
   *                      Abstract method implementations
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#isAlias()
   */
  public boolean isAlias() throws WebdavException {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#getAliasTarget()
   */
  public WdEntity getAliasTarget() throws WebdavException {
    return this;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#getSummary()
   */
  public String getSummary() throws WebdavException {
    return getEv().getSummary();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#getNewEvent()
   */
  public boolean isNew() throws WebdavException {
    return getEvinfo().getNewEvent();
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#getEntityType()
   */
  public int getEntityType() throws WebdavException {
    int entityType = getEv().getEntityType();

    if (entityType == CalFacadeDefs.entityTypeEvent) {
      return entityTypeEvent;
    }

    if (entityType == CalFacadeDefs.entityTypeTodo) {
      return entityTypeTodo;
    }

    if (entityType == CalFacadeDefs.entityTypeJournal) {
      return entityTypeJournal;
    }

    if (entityType == CalFacadeDefs.entityTypeFreeAndBusy) {
      return entityTypeFreeAndBusy;
    }

    if (entityType == CalFacadeDefs.entityTypeVavailability) {
      return entityTypeVavailability;
    }

    return -1;
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#setOrganizer(org.bedework.caldav.server.Organizer)
   */
  public void setOrganizer(Organizer val) throws WebdavException {
    BwOrganizer org = new BwOrganizer();

    org.setCn(val.getCn());
    org.setDir(val.getDir());
    org.setLanguage(val.getLanguage());
    org.setSentBy(val.getSentBy());
    org.setOrganizerUri(val.getOrganizerUri());

    getEv().setOrganizer(org);
  }

  public void setOriginator(String val) throws WebdavException {
    getEv().setOriginator(val);
  }

  public void setRecipients(Set<String> val) throws WebdavException {
    getEv().setRecipients(val);
  }

  public Set<String> getRecipients() throws WebdavException {
    return getEv().getRecipients();
  }

  public void addRecipient(String val) throws WebdavException {
    getEv().addRecipient(val);
  }

  public void setScheduleMethod(int val) throws WebdavException {
    getEv().setScheduleMethod(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.caldav.server.CalDAVEvent#getUid()
   */
  public String getUid() throws WebdavException {
    return getEv().getUid();
  }

  public boolean generatePropertyValue(QName tag,
                                       XmlEmit xml) throws WebdavException {
    try {
      BwEvent ev = getEv();

      if (tag.equals(CaldavTags.scheduleState)) {
        xml.openTag(tag);
        if (ev.getScheduleState() == BwEvent.scheduleStateNotProcessed) {
          xml.emptyTag(CaldavTags.notProcessed);
        } else {
          xml.emptyTag(CaldavTags.processed);
        }
        xml.closeTag(tag);
        return true;
      }

      if (tag.equals(CaldavTags.originator)) {
        if (ev.getOriginator() != null) {
          xml.openTag(tag);
          xml.property(WebdavTags.href, ev.getOriginator());
          xml.closeTag(tag);
        }
        return true;
      }

      if (tag.equals(CaldavTags.recipient)) {
        Collection<String> r = ev.getRecipients();
        if ((r == null) || (r.isEmpty())) {
          return true;
        }

        xml.openTag(tag);
        for (String recip: r) {
          xml.property(WebdavTags.href, recip);
        }
        xml.closeTag(tag);
        return true;
      }

      /* =============== ICalTags follow ================= */

      if (tag.equals(ICalTags.action)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.attach)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.attendee)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.categories)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags._class)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.comment)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.completed)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.contact)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.created)) {
        xml.property(tag, ev.getCreated());
        return true;
      }

      if (tag.equals(ICalTags.description)) {
        if (ev.getDescription() != null) {
          xml.property(tag, ev.getDescription());
        }
        return true;
      }

      if (tag.equals(ICalTags.dtend)) {
        xml.property(tag, ev.getDtend().getDate());
        return true;
      }

      if (tag.equals(ICalTags.dtstamp)) {
        xml.property(tag, ev.getDtstamp());
        return true;
      }

      if (tag.equals(ICalTags.dtstart)) {
        xml.property(tag, ev.getDtstart().getDate());
        return true;
      }

      /* TODO
     if (tag.equals(ICalTags.due)) {
     pv.val = ev.
     return pv;
     }
       */

      if (tag.equals(ICalTags.duration)) {
        xml.property(tag, ev.getDuration());
        return true;
      }

      if (tag.equals(ICalTags.exdate)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.exrule)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.freebusy)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.geo)) {
        // PROPTODO
        return true;
      }

      /*
     if (tag.equals(ICalTags.hasRecurrence)) {
     pv.val = ev
     return pv;
     }

     if (tag.equals(ICalTags.hasAlarm)) {
     pv.val = ev
     return pv;
     }

     if (tag.equals(ICalTags.hasAttachment)) {
     pv.val = ev
     return pv;
     }*/

      if (tag.equals(ICalTags.lastModified)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.lastModified)) {
        xml.property(tag, ev.getLastmod());
        return true;
      }

      if (tag.equals(ICalTags.location)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.organizer)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.organizer)) {
        if (ev.getOrganizer() != null) {
          xml.property(tag, ev.getOrganizer().getOrganizerUri());
        }
        return true;
      }

      if (tag.equals(ICalTags.percentComplete)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.priority)) {
        Integer val = ev.getPriority();
        if ((val != null) && (val != 0)) {
          xml.property(tag, String.valueOf(val));
        }

        return true;
      }

      if (tag.equals(ICalTags.rdate)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.recurrenceId)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.recurrenceId)) {
        if (ev.getRecurrenceId() != null) {
          xml.property(tag, ev.getRecurrenceId());
        }
        return true;
      }

      if (tag.equals(ICalTags.relatedTo)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.repeat)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.resources)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.requestStatus)) {
        // PROPTODO
        /*
      if (ev.getRequestStatus() != null) {
        xml.property(tag, ev.getRequestStatus().strVal());
      }
         */
        return true;
      }

      if (tag.equals(ICalTags.rrule)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.sequence)) {
        xml.property(tag, String.valueOf(ev.getSequence()));

        return true;
      }

      if (tag.equals(ICalTags.status)) {
        xml.property(tag, ev.getStatus());
        return true;
      }

      if (tag.equals(ICalTags.summary)) {
        xml.property(tag, ev.getSummary());
        return true;
      }

      if (tag.equals(ICalTags.transp)) {
        xml.property(tag, ev.getTransparency());
        return true;
      }

      if (tag.equals(ICalTags.trigger)) {
        // PROPTODO
        return true;
      }

      if (tag.equals(ICalTags.uid)) {
        xml.property(tag, ev.getUid());
        return true;
      }

      if (tag.equals(ICalTags.url)) {
        if (ev.getLink() != null) {
          xml.property(tag, ev.getLink());
        }
        return true;
      }

      if (tag.equals(ICalTags.version)) {
        // PROPTODO
        return true;
      }

      return false;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  public String toIcalString(int methodType) throws WebdavException {
    try {
      return IcalTranslator.toIcalString(methodType, getEv());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                      Overrides
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setName(java.lang.String)
   */
  public void setName(String val) throws WebdavException {
    getEv().setName(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getName()
   */
  public String getName() throws WebdavException {
    return getEv().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdEntity#setDisplayName(java.lang.String)
   */
  public void setDisplayName(String val) throws WebdavException {
    // No display name
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getDisplayName()
   */
  public String getDisplayName() throws WebdavException {
    return getEv().getSummary();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setPath(java.lang.String)
   */
  public void setPath(String val) throws WebdavException {
    // Not actually saved
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getPath()
   */
  public String getPath() throws WebdavException {
    return getEv().getColPath() + "/" + getEv().getName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setParentPath(java.lang.String)
   */
  public void setParentPath(String val) throws WebdavException {
    getEv().setColPath(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getParentPath()
   */
  public String getParentPath() throws WebdavException {
    return getEv().getColPath();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setOwner(edu.rpi.cmt.access.AccessPrincipal)
   */
  public void setOwner(AccessPrincipal val) throws WebdavException {
    getEv().setOwnerHref(val.getPrincipalRef());
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getOwner()
   */
  public AccessPrincipal getOwner() throws WebdavException {
    return intf.getPrincipal(getEv().getOwnerHref());
  }

  public void setCreated(String val) throws WebdavException {
    getEv().setCreated(val);
  }

  public String getCreated() throws WebdavException {
    return getEv().getCreated();
  }

  public void setLastmod(String val) throws WebdavException {
    getEv().setLastmod(val);
  }

  public String getLastmod() throws WebdavException {
    return getEv().getLastmod();
  }

  public void setSequence(int val) throws WebdavException {
  }

  public int getSequence() throws WebdavException {
    return 0;
  }

  public void setPrevLastmod(String val) throws WebdavException {
    getEvinfo().setPrevLastmod(val);
  }

  public String getPrevLastmod() throws WebdavException {
    return getEvinfo().getPrevLastmod();
  }

  public void setPrevSequence(int val) throws WebdavException {
  }

  public int getPrevSequence() throws WebdavException {
    return 0;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#setDescription(java.lang.String)
   */
  public void setDescription(String val) throws WebdavException {
    getEv().setDescription(val);
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WdCollection#getDescription()
   */
  public String getDescription() throws WebdavException {
    return getEv().getDescription();
  }

  /* ====================================================================
   *                      Private methods
   * ==================================================================== */

  EventInfo getEvinfo() throws WebdavException {
    if (evi == null) {
      evi = new EventInfo(new BwEventObj());
      ev = evi.getEvent();
    }

    return evi;
  }

  BwEvent getEv() throws WebdavException {
    getEvinfo();

    return ev;
  }
}
