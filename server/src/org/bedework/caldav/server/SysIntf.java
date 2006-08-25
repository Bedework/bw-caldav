/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server;

import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwFreeBusy;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;

import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

/** All interactions with the underlying calendar system are made via this
 * interface.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public interface SysIntf {
  /** Prefix which defines a principal URIs
   */
  public static final String principalPrefix = "/principals";

  /** Prefix which defines a user principal URI
   */
  public static final String userPrincipalPrefix = principalPrefix + "/users";

  /** Prefix which defines a group principal URI
   */
  public static final String groupPrincipalPrefix = principalPrefix + "/groups";

  /** Called before any other method is called to allow initialisation to
   * take place at the first or subsequent requests
   *
   * @param req
   * @param envPrefix
   * @param account
   * @param debug
   * @throws WebdavIntfException
   */
  public void init(HttpServletRequest req,
                   String envPrefix,
                   String account,
                   boolean debug) throws WebdavIntfException;

  /**
   * @return String url prefix derived from request.
   */
  public String getUrlPrefix();

  /** Do we allow browsing of directories?
   *
   * @return boolean true if browsing disallowed
   * @throws WebdavIntfException  for errors
   */
  public boolean getDirectoryBrowsingDisallowed() throws WebdavIntfException;

  /** Given a calendar address return the associated calendar account.
   * For example, we might have a calendar address<br/>
   *   auser@ahost.org
   * <br/>with the associated account of <br/>
   * auser.<br/>
   *
   * <p>Whereever we need a user account use the converted value. Call
   * userToCaladdr for the inverse.
   *
   * @param caladdr      calendar address
   * @return account or null if not caladdr for this system
   * @throws WebdavIntfException  for errors
   */
  public String caladdrToUser(String caladdr) throws WebdavIntfException;

  /**
   * @author Mike Douglass
   */
  public static class CalUserInfo implements Serializable {
    /** account as returned by caladdrToUer
     */
    public String account;

    /** Path to user home
     */
    public String userHomePath;

    /** Path to default calendar
     */
    public String defaultCalendarPath;

    /** Path to inbox. null for no scheduling permitted (or supported)
     */
    public String inboxPath;

    /** Path to outbox. null for no scheduling permitted (or supported)
     */
    public String outboxPath;

    /**
     * @param account
     * @param userHomePath
     * @param defaultCalendarPath
     * @param inboxPath
     * @param outboxPath
     */
    public CalUserInfo(String account, String userHomePath,
                       String defaultCalendarPath, String inboxPath,
                       String outboxPath) {
      this.account = account;
      this.userHomePath = userHomePath;
      this.defaultCalendarPath = defaultCalendarPath;
      this.inboxPath = inboxPath;
      this.outboxPath = outboxPath;
    }
  }

  /** Given a calendar address return the associated calendar user information
   * needed for caldav interactions.
   *
   * @param account     as returned by caladdrToUser
   * @return CalUserInfo or null if not caladdr for this system
   * @throws WebdavIntfException  for errors
   */
  public CalUserInfo getCalUserInfo(String account) throws WebdavIntfException;

  /** Given a uri returns a Collection of uris that allow search operations on
   * principals for that resource.
   *
   * @param resourceUri
   * @return Collection of String
   * @throws WebdavIntfException
   */
  public Collection getPrincipalCollectionSet(String resourceUri)
         throws WebdavIntfException;

  /** Given a PrincipalPropertySearch returns a Collection of matching principals.
   *
   * @param resourceUri
   * @param pps Collection of PrincipalPropertySearch
   * @return Collection of WebdavNsNode principals
   * @throws WebdavIntfException
   */
  public Collection getPrincipals(String resourceUri,
                                  PrincipalPropertySearch pps)
          throws WebdavIntfException;

  /** Is account a valid user?
   *
   * @param account
   * @return boolean true for a valid user
   * @throws WebdavIntfException  for errors
   */
  public boolean validUser(String account) throws WebdavIntfException;

  /** Is account a valid group?
   *
   * @param account
   * @return boolean true for a valid group
   * @throws WebdavIntfException  for errors
   */
  public boolean validGroup(String account) throws WebdavIntfException;

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  /** Request to schedule a meeting. The event object must have the organizer
   * and attendees and possibly recipients set. If no recipients are set, they
   * will be set from the attendees.
   *
   * <p>The event will be added to the users outbox which will trigger the send
   * of requests to other users inboxes. For users within this system the
   * request will be immediately addded to the recipients inbox. For external
   * users they are sent via mail.
   *
   * @param event         BwEvent object
   * @throws CalFacadeException
   */
  public void scheduleRequest(BwEvent event) throws WebdavIntfException;

  /* ====================================================================
   *                   Events
   * ==================================================================== */

  /** Add an event.
  *
  * @param cal          BwCalendar defining recipient calendar
  * @param event        BwEvent object to be added
  * @param overrides    Collection of BwEventProxy objects which override instances
  *                     of the new event
  * @throws WebdavIntfException
  */
 public void addEvent(BwCalendar cal,
                      BwEvent event,
                      Collection overrides) throws WebdavIntfException;

 /** Update an event.
  *
  * @param event         updated BwEvent object
  * @throws WebdavIntfException
  */
 public void updateEvent(BwEvent event) throws WebdavIntfException;

  /** Get events in the given calendar with recurrences expanded
   *
   * @param cal
   * @return Collection of BwEvent
   * @throws WebdavIntfException
   */
  public Collection getEventsExpanded(BwCalendar cal) throws WebdavIntfException;

  /** Return events for the current user in the given calendar.
   *
   * @param cal
   * @param recurRetrieval Takes value defined in.CalFacadeDefs
   * @return Collection of EventInfo objects
   * @throws WebdavIntfException
   */
  public Collection getEvents(BwCalendar cal,
                              int recurRetrieval) throws WebdavIntfException;

  /** Return the events for the current user in the given calendar within the
   * given date and time range.
   *
   * @param cal
   * @param startDate    DateTimeVO start - may be null
   * @param endDate      DateTimeVO end - may be null.
   * @param recurRetrieval Takes value defined in.CalFacadeDefs
   * @return Collection  populated event value objects
   * @throws WebdavIntfException
   */
  public Collection getEvents(BwCalendar cal,
                              BwDateTime startDate, BwDateTime endDate,
                              int recurRetrieval) throws WebdavIntfException;

  /** Get events given the calendar and String name. Return null for not
   * found. For non-recurring there should be only one event. Otherwise we
   * return the currently expanded set of recurring events.
   *
   * @param cal        CalendarVO object
   * @param val        String possible name
   * @return Collection of EventInfo or null
   * @throws WebdavIntfException
   */
  public Collection findEventsByName(BwCalendar cal, String val)
          throws WebdavIntfException;

  /**
   * @param ev
   * @throws WebdavIntfException
   */
  public void deleteEvent(BwEvent ev) throws WebdavIntfException;

  /**
   * @param cal
   * @param account
   * @param start
   * @param end
   * @return BwFreeBusy
   * @throws WebdavException
   */
  public BwFreeBusy getFreeBusy(BwCalendar cal,
                                String account,
                                BwDateTime start,
                                BwDateTime end) throws WebdavException;

  /** Check the access for the given entity. Returns the current access
   * or null or optionally throws a no access exception.
   *
   * @param ent
   * @param desiredAccess
   * @param returnResult
   * @return CurrentAccess
   * @throws WebdavException if returnResult false and no access
   */
  public CurrentAccess checkAccess(BwShareableDbentity ent,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException;

  /**
   * @param cal
   * @param aces
   * @throws WebdavIntfException
   */
  public void updateAccess(BwCalendar cal,
                           Collection aces) throws WebdavIntfException;

  /**
   * @param ev
   * @param aces
   * @throws WebdavIntfException
   */
  public void updateAccess(BwEvent ev,
                           Collection aces) throws WebdavIntfException;

  /**
   * @param name
   * @param calendarCollection
   * @param parentPath
   * @throws WebdavIntfException
   */
  public void makeCollection(String name, boolean calendarCollection,
                             String parentPath) throws WebdavIntfException;

  /** Get a calendar given the path
   *
   * @param  path     String path of calendar
   * @return BwCalendar null for unknown calendar
   * @throws WebdavIntfException
   */
  public BwCalendar getCalendar(String path) throws WebdavIntfException;

  /** Make an ical Calendar from an event.
   *
   * @param ev
   * @return Calendar
   * @throws WebdavIntfException
   */
  public Calendar toCalendar(BwEvent ev) throws WebdavIntfException;

  /** Convert the Icalendar reader to a Collection of Calendar objects
   *
   * @param cal       calendar in which to place entities
   * @param rdr
   * @return Icalendar
   * @throws WebdavIntfException
   */
  public Icalendar fromIcal(BwCalendar cal, Reader rdr) throws WebdavIntfException;

  /** Make an ical Calendar from a Collection of events.
   *
   * @param evs
   * @return Calendar
   * @throws WebdavIntfException
   */
  public Calendar toCalendar(Collection evs) throws WebdavIntfException;

  /**
   * @return CalTimezones
   * @throws WebdavIntfException
   */
  public CalTimezones getTimezones() throws WebdavIntfException;

  /** Get the default timezone for this system.
   *
   * @return default TimeZone or null for none set.
   * @throws WebdavIntfException
   */
  public TimeZone getDefaultTimeZone() throws WebdavIntfException;

  /** Create a Calendar object from the named timezone and convert to
   * a String representation
   *
   * @param tzid       String timezone id
   * @return String
   * @throws WebdavIntfException
   */
  public String toStringTzCalendar(String tzid) throws WebdavIntfException;

  /** Max size for an entity
   *
   * @return int
   * @throws WebdavIntfException
   */
  public int getMaxUserEntitySize() throws WebdavIntfException;

  /** End any transactions.
   *
   * @throws WebdavIntfException
   */
  public void close() throws WebdavIntfException;
}
