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
import org.bedework.calfacade.BwCategory;
import org.bedework.calfacade.BwLocation;
import org.bedework.calfacade.BwUser;
import org.bedework.calfacade.CalFacadeException;
import org.bedework.calfacade.timezones.CalTimezones;
import org.bedework.icalendar.IcalCallback;
import org.bedework.icalendar.URIgen;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;

import java.util.Collection;

/** Class to allow icaltranslator to be used from a standalone non-bedework
 * caldav server.
 *
 * @author douglm
 *
 */
public class SAICalCallback implements IcalCallback {
  private int strictness;

  private CalTimezones timezones;
  private BwUser user;

  /** Constructor
   *
   * @param timezones
   * @param account
   */
  public SAICalCallback(CalTimezones timezones, String account) {
    this.timezones = timezones;
    user = new BwUser(account);
  }

  public void setStrictness(int val) throws CalFacadeException {
    strictness = val;
  }

  public int getStrictness() throws CalFacadeException {
    return strictness;
  }

  public BwUser getUser() throws CalFacadeException {
    return user;
  }

  public BwCategory findCategory(BwCategory val) throws CalFacadeException {
    return null;
  }

  public void addCategory(BwCategory val) throws CalFacadeException {
  }

  public BwLocation ensureLocationExists(String address) throws CalFacadeException {
    BwLocation loc = new BwLocation();
    loc.setAddress(address);
    loc.setOwner(getUser());

    return loc;
  }

  public Collection getEvent(BwCalendar cal, String guid, String rid,
                             int recurRetrieval) throws CalFacadeException {
    return null;
  }

  public URIgen getURIgen() throws CalFacadeException {
    return null;
  }

  public CalTimezones getTimezones() throws CalFacadeException {
    return timezones;
  }

  public void saveTimeZone(String tzid,
                           VTimeZone vtz) throws CalFacadeException {
    timezones.saveTimeZone(tzid, vtz);
  }

  public void storeTimeZone(final String id) throws CalFacadeException {
    timezones.storeTimeZone(id, getUser());
  }

  public void registerTimeZone(String id, TimeZone timezone)
            throws CalFacadeException {
    timezones.registerTimeZone(id, timezone);
  }

  public TimeZone getTimeZone(final String id) throws CalFacadeException {
    return timezones.getTimeZone(id);
  }

  public VTimeZone findTimeZone(final String id, BwUser owner) throws CalFacadeException {
    return timezones.findTimeZone(id, owner);
  }
}
