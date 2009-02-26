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

import org.bedework.caldav.server.PropertyHandler.PropertyType;
import org.bedework.calfacade.BwDateTime;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventProxy;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.ScheduleResult;
import org.bedework.calfacade.base.BwShareableDbentity;
import org.bedework.calfacade.configs.CalDAVConfig;
import org.bedework.calfacade.filter.BwFilter;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calfacade.util.ChangeTable;
import org.bedework.icalendar.Icalendar;

import edu.rpi.cct.webdav.servlet.shared.PrincipalPropertySearch;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.UrlHandler;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.Acl;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import net.fortuna.ical4j.model.Calendar;

import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

/** All interactions with the underlying calendar system are made via this
 * interface.
 *
 * <p>We're using the bedework object classes here. To simplify matters (a little)
 * we don't have distinct event, todo and journal classes. They are all currently
 * the BwEvent class with an entityType defining what the object represents.
 *
 * @author Mike Douglass douglm at rpi.edu
 */
public interface SysIntf {
  /** Called before any other method is called to allow initialisation to
   * take place at the first or subsequent requests
   *
   * @param req
   * @param account
   * @param conf  per application type configuration
   * @param debug
   * @throws WebdavException
   */
  public void init(HttpServletRequest req,
                   String account,
                   CalDAVConfig conf,
                   boolean debug) throws WebdavException;

  /** Return the current principal
   *
   * @return String
   * @throws WebdavException
   */
  public AccessPrincipal getPrincipal() throws WebdavException;

  /** Get a property handler
   *
   * @param ptype
   * @return PropertyHandler
   * @throws WebdavException
   */
  public PropertyHandler getPropertyHandler(PropertyType ptype) throws WebdavException;

  /**
   * @return UrlHandler object to manipulate urls.
   */
  public UrlHandler getUrlHandler();

  /* *
   * @return String url prefix derived from request.
   * /
  public String getUrlPrefix();

  /* *
   * @return boolean - true if using relative urls for broken clients
   * /
  public boolean getRelativeUrls();*/

  /** Does the value appear to represent a valid principal?
   *
   * @param val
   * @return true if it's a (possible) principal
   * @throws WebdavException
   */
  public boolean isPrincipal(String val) throws WebdavException;

  /** Return principal information for the given href. Also tests for a valid
   * principal.
   *
   *
   * @param href
   * @return PrincipalInfo
   * @throws WebdavException
   */
  public AccessPrincipal getPrincipal(String href) throws WebdavException;

  /**
   * @param id
   * @param whoType - from WhoDefs
   * @return String href
   * @throws WebdavException
   */
  public String makeHref(String id, int whoType) throws WebdavException;

  /** The urls should be principal urls. principalUrl can null for the current user.
   * The result is a collection of principal urls of which the given url is a
   * member, based upon rootUrl. For example, if rootUrl points to the base of
   * the user principal hierarchy, then the rsult should be at least the current
   * user's principal url, remembering that user principals are themselves groups
   * and the user is considered a member of their own group.
   *
   * @param rootUrl - url to base search on.
   * @param principalUrl - url of principal or null for current user
   * @return Collection of urls - always non-null
   * @throws WebdavException
   */
  public Collection<String>getGroups(String rootUrl,
                                     String principalUrl) throws WebdavException;

  /** Do we allow browsing of directories?
   *
   * @return boolean true if browsing disallowed
   * @throws WebdavException  for errors
   */
  public boolean getDirectoryBrowsingDisallowed() throws WebdavException;

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
   * @return AccessPrincipal or null if not caladdr for this system
   * @throws WebdavException  for errors
   */
  public AccessPrincipal caladdrToPrincipal(String caladdr) throws WebdavException;

  /** The inverse of caladdrToUser
   *
   * @param account
   * @return String calendar user address
   * @throws WebdavException
   */
  public String userToCaladdr(String account) throws WebdavException;

  /** XXX At the moment we are still asssuming the principal is a user.
   *
   * @author Mike Douglass
   */
  public static class CalPrincipalInfo implements Serializable {
    /** As supplied
     */
    public AccessPrincipal principal;

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
     * @param principal
     * @param userHomePath
     * @param defaultCalendarPath
     * @param inboxPath
     * @param outboxPath
     */
    public CalPrincipalInfo(AccessPrincipal principal,
                            String userHomePath,
                            String defaultCalendarPath, String inboxPath,
                            String outboxPath) {
      this.principal = principal;
      this.userHomePath = userHomePath;
      this.defaultCalendarPath = defaultCalendarPath;
      this.inboxPath = inboxPath;
      this.outboxPath = outboxPath;
    }
  }

  /** Given a valid AccessPrincipal return the associated calendar user information
   * needed for caldav interactions.
   *
   * @param principal     valid AccessPrincipal
   * @return CalUserInfo or null if not caladdr for this system
   * @throws WebdavException  for errors
   */
  public CalPrincipalInfo getCalPrincipalInfo(AccessPrincipal principal) throws WebdavException;

  /** Given a uri returns a Collection of uris that allow search operations on
   * principals for that resource.
   *
   * @param resourceUri
   * @return Collection of String
   * @throws WebdavException
   */
  public Collection<String> getPrincipalCollectionSet(String resourceUri)
         throws WebdavException;

  /** Given a PrincipalPropertySearch returns a Collection of matching principals.
   *
   * @param resourceUri
   * @param pps Collection of PrincipalPropertySearch
   * @return Collection of CalUserInfo
   * @throws WebdavException
   */
  public Collection<CalPrincipalInfo> getPrincipals(String resourceUri,
                                  PrincipalPropertySearch pps)
          throws WebdavException;

  /** Is account a valid user?
   *
   * @param account
   * @return boolean true for a valid user
   * @throws WebdavException  for errors
   */
  public boolean validUser(String account) throws WebdavException;

  /** Is account a valid group?
   *
   * @param account
   * @return boolean true for a valid group
   * @throws WebdavException  for errors
   */
  public boolean validGroup(String account) throws WebdavException;

  /* ====================================================================
   *                   Scheduling
   * ==================================================================== */

  /** Return a set of hrefs for each resource affecting this users freebusy
   *
   * @return Collection of hrefs
   * @throws WebdavException  for errors
   */
  public Collection<String> getFreebusySet() throws WebdavException;

  /** Request to schedule a meeting. The event object must have the organizer
   * and attendees and possibly recipients set. If no recipients are set, they
   * will be set from the attendees.
   *
   * <p>The functioning of this method must conform to the requirements of iTip.
   * The event object must have the required method (publish, request etc) set.
   *
   * <p>The event will be added to the users outbox which will trigger the send
   * of requests to other users inboxes. For users within this system the
   * request will be immediately addded to the recipients inbox. For external
   * users they are sent via mail.
   *
   * @param ei         EventInfo object
   * @return ScheduleResult
   * @throws WebdavException
   */
  public ScheduleResult schedule(EventInfo ei) throws WebdavException;

  /* ====================================================================
   *                   Events
   * ==================================================================== */

  /** Add an event/task/journal. If this is a scheduling event we are adding,
   * determined by examining the organizer and attendee properties, we will send
   * out invitations to the attendees, unless the noInvites flag is set.
   *
   * @param ei           EventInfo object + overrides to be added
   * @param noInvites    Set from request - if true don't send invites
   * @param rollbackOnError true if we rollback and throw an exception on error
   * @return Collection of overrides which did not match or null if all matched
   * @throws WebdavException
   */
 public Collection<BwEventProxy> addEvent(EventInfo ei,
                                          boolean noInvites,
                                          boolean rollbackOnError) throws WebdavException;

  /** Update an event/todo/journal.
   *
   * @param event         updated BwEvent object
   * @param changes       help for recurrence rule changes to master
   * @throws WebdavException
   */
  public void updateEvent(EventInfo event,
                          ChangeTable changes) throws WebdavException;

  /** Return the events for the current user in the given collection using the
   * supplied filter. Stored freebusy objects are returned as BwEvent
   * objects with the appropriate entity type.
   *
   * <p>We flag the desired entity types.
   *
   * @param col
   * @param filter - if non-null defines a search filter
   * @param recurRetrieval How recurring event is returned.
   * @return Collection  populated event value objects
   * @throws WebdavException
   */
  public Collection<EventInfo> getEvents(CalDAVCollection col,
                                         BwFilter filter,
                                         RecurringRetrievalMode recurRetrieval)
          throws WebdavException;

  /** Get events given the collection and String name. Return null for not
   * found. There should be only one event or none. For recurring, the
   * overrides and possibly the instances will be attached.
   *
   * @param col        CalDAVCollection object
   * @param val        String possible name
   * @param recurRetrieval
   * @return EventInfo or null
   * @throws WebdavException
   */
  public EventInfo getEvent(CalDAVCollection col, String val,
                            RecurringRetrievalMode recurRetrieval)
          throws WebdavException;

  /**
   * @param ev
   * @param scheduleReply - true if we want a schduling reply posted
   * @throws WebdavException
   */
  public void deleteEvent(BwEvent ev,
                          boolean scheduleReply) throws WebdavException;

  /** Get the free busy for one or more principals based on the given VFREEBUSY
   * request.
   *
   * @param val    A representation of a scheduling freebusy request to be
   *               acted upon.
   * @return ScheduleResult
   * @throws WebdavException
   */
  public ScheduleResult requestFreeBusy(EventInfo val)
          throws WebdavException;

  /** Generate a free busy object for the given time period which reflects
   * the state of the given collection.
   *
   * @param col
   * @param depth
   * @param account
   * @param start
   * @param end
   * @return BwEvent
   * @throws WebdavException
   */
  public BwEvent getFreeBusy(final CalDAVCollection col,
                             final int depth,
                             final String account,
                             final BwDateTime start,
                             final BwDateTime end) throws WebdavException;

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
   * @param ev
   * @param acl
   * @throws WebdavException
   */
  public void updateAccess(BwEvent ev,
                           Acl acl) throws WebdavException;

  /** Copy or move the given entity to the destination collection with the given name.
   * Status is set on return
   *
   * @param from      Source entity
   * @param to        Destination collection
   * @param name      String name of new entity
   * @param copy      true for copying
   * @param overwrite destination exists
   * @return true if destination created (i.e. not updated)
   * @throws WebdavException
   */
  public boolean copyMove(EventInfo from,
                          CalDAVCollection to,
                          String name,
                          boolean copy,
                          boolean overwrite) throws WebdavException;

  /* ====================================================================
   *                   Collections
   * ==================================================================== */

  /** Return a new object representing the parameters. No collection is
   * created. makeCollection must be called subsequently with the object.
   *
   * @param isCalendarCollection
   * @param parentPath
   * @return CalDAVCollection
   * @throws WebdavException
   */
  public CalDAVCollection newCollectionObject(boolean isCalendarCollection,
                                              String parentPath) throws WebdavException;

  /**
   * @param col
   * @param acl
   * @throws WebdavException
   */
  public void updateAccess(CalDAVCollection col,
                           Acl acl) throws WebdavException;

  /** Check the access for the given entity. Returns the current access
   * or null or optionally throws a no access exception.
   *
   * @param col
   * @param desiredAccess
   * @param returnResult
   * @return CurrentAccess
   * @throws WebdavException if returnResult false and no access
   */
  public CurrentAccess checkAccess(CalDAVCollection col,
                                   int desiredAccess,
                                   boolean returnResult)
          throws WebdavException;

  /**
   * @param col   Initialised collection object
   * @return int status
   * @throws WebdavException
   */
  public int makeCollection(CalDAVCollection col) throws WebdavException;

  /** Copy or move the collection to another location.
   * Status is set on return
   *
   * @param from      Source collection
   * @param to        Destination collection
   * @param copy      true for copying
   * @param overwrite destination exists
   * @throws WebdavException
   */
  public void copyMove(CalDAVCollection from,
                       CalDAVCollection to,
                       boolean copy,
                       boolean overwrite) throws WebdavException;

  /** Get a collection given the path
   *
   * @param  path     String path of calendar
   * @return CalDAVCollection null for unknown collection
   * @throws WebdavException
   */
  public CalDAVCollection getCollection(String path) throws WebdavException;

  /** Update a collection.
   *
   * @param val           updated CalDAVCollection object
   * @throws WebdavException
   */
  public void updateCollection(CalDAVCollection val) throws WebdavException;

  /**
   * @param col
   * @throws WebdavException
   */
  public void deleteCollection(CalDAVCollection col) throws WebdavException;

  /** Returns children of the given collection to which the current user has
   * some access.
   *
   * @param  col          parent collection
   * @return Collection   of CalDAVCollection
   * @throws WebdavException
   */
  public Collection<CalDAVCollection> getCollections(CalDAVCollection col)
          throws WebdavException;

  /** If the parameter is an alias and the target has not been resolved it will
   * be fetched and implanted in the parameter object.
   *
   * @param col
   * @throws WebdavException
   */
  public void resolveAlias(CalDAVCollection col) throws WebdavException;

  /* ====================================================================
   *                   Files
   * ==================================================================== */

  /** PUT a file.
   *
   * @param coll         CalDAVCollection defining recipient collection
   * @param val          BwResource
   * @throws WebdavException
   */
  public void putFile(CalDAVCollection coll,
                      BwResource val) throws WebdavException;

  /** GET a file.
   *
   * @param coll         CalDAVCollection containing file
   * @param name
   * @return BwResource
   * @throws WebdavException
   */
  public BwResource getFile(CalDAVCollection coll,
                            String name) throws WebdavException;

  /** Get resource content given the resource. It will be set in the resource
   * object
   *
   * @param  val BwResource
   * @throws WebdavException
   */
  public void getFileContent(BwResource val) throws WebdavException;

  /** Get the files in a collection.
   *
   * @param coll         CalDAVCollection containing file
   * @return Collection of BwResource
   * @throws WebdavException
   */
  public Collection<BwResource> getFiles(CalDAVCollection coll) throws WebdavException;

  /** Update a file.
   *
   * @param val          BwResource
   * @param updateContent if true we also update the content
   * @throws WebdavException
   */
  public void updateFile(BwResource val,
                         boolean updateContent) throws WebdavException;

  /** Delete a file.
   *
   * @param val          BwResource
   * @throws WebdavException
   */
  public void deleteFile(BwResource val) throws WebdavException;

  /** Copy or move the given file to the destination collection with the given name.
   * Status is set on return
   *
   * @param from      Source resource
   * @param toPath    Destination collection path
   * @param name      String name of new entity
   * @param copy      true for copying
   * @param overwrite destination exists
   * @return true if destination created (i.e. not updated)
   * @throws WebdavException
   */
  public boolean copyMoveFile(BwResource from,
                              String toPath,
                              String name,
                              boolean copy,
                              boolean overwrite) throws WebdavException;

  /** Make an ical Calendar from an event.
   *
   * @param ev
   * @return Calendar
   * @throws WebdavException
   */
  public Calendar toCalendar(EventInfo ev) throws WebdavException;

  /** Make an ical Calendar from a collection of events.
   *
   * @param evs
   * @param method
   * @return Calendar
   * @throws WebdavException
   */
  public Calendar toCalendar(Collection<EventInfo> evs,
                             int method) throws WebdavException;

  /** Convert the Icalendar reader to a Collection of Calendar objects
   *
   * @param col       collection in which to place entities
   * @param rdr
   * @return Icalendar
   * @throws WebdavException
   */
  public Icalendar fromIcal(CalDAVCollection col, Reader rdr) throws WebdavException;

  /** Create a Calendar object from the named timezone and convert to
   * a String representation
   *
   * @param tzid       String timezone id
   * @return String
   * @throws WebdavException
   */
  public String toStringTzCalendar(String tzid) throws WebdavException;

  /** Max size for an entity
   *
   * @return int
   * @throws WebdavException
   */
  public int getMaxUserEntitySize() throws WebdavException;

  /** End any transactions.
   *
   * @throws WebdavException
   */
  public void close() throws WebdavException;
}
