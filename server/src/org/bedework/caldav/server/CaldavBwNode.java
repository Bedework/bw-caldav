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

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavIntfException;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.sss.util.xml.XmlEmit;

import java.io.StringReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.bedework.davdefs.WebdavTags;

/** Class to represent a caldav node.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public abstract class CaldavBwNode extends WebdavNsNode {
  protected String owner;

  protected CaldavURI cdURI;

  /* for accessing calendars */
  private SysIntf sysi;

  CaldavBwNode(CaldavURI cdURI, SysIntf sysi, boolean debug) {
    super(debug);

    this.cdURI = cdURI;
    this.sysi = sysi;

    if (cdURI != null) {
      this.uri = cdURI.getUri();
      this.owner = cdURI.getOwner();
    }
  }

  protected void generateHref(XmlEmit xml) throws WebdavException {
    try {
      String url = sysi.getUrlPrefix() + new URI(getEncodedUri()).toASCIIString();
      xml.property(WebdavTags.href, url);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  protected void generateHref(XmlEmit xml, String uri) throws WebdavException {
    try {
      String enc = new URI(null, null, uri, null).toString();

      String url = sysi.getUrlPrefix() + "/" + new URI(enc).toASCIIString();
      xml.property(WebdavTags.href, url);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* ====================================================================
   *                         Public methods
   * ==================================================================== */

  /**
   * @return boolean if this is a calendar (as against an entity)
   */
  public boolean isCollection() {
    return cdURI.isCalendar();
  }

  /**
   * @return boolean if this is a calendar
   */
  public boolean isCalendarCollection() {
    return (cdURI.isCalendar() && cdURI.getCal().getCalendarCollection());
  }

  /**
   * @return CaldavURI
   */
  public CaldavURI getCDURI() {
    return cdURI;
  }

  /** Return a collection of children objects. These will all be calendar
   * entities.
   *
   * <p>Default is to return null
   *
   * @return Collection
   * @throws WebdavIntfException
   */
  public Collection getChildren() throws WebdavIntfException {
    return null;
  }

  /** Return a collection of property objects
   *
   * <p>Default is to return an empty Collection
   *
   * @param ns      String interface namespace.
   * @return Collection (possibly empty) of WebdavProperty objects
   * @throws WebdavIntfException
   */
  public Collection<WebdavProperty> getProperties(String ns) throws WebdavIntfException {
    return new ArrayList<WebdavProperty>();
  }

  /** Returns an InputStream for the content.
   *
   * @return Reader       A reader for the content.
   * @throws WebdavIntfException
   */
  public Reader getContent() throws WebdavIntfException {
    String cont = getContentString();

    if (cont == null) {
      return null;
    }

    return new StringReader(cont);
  }

  /** Return string content
   *
   * @return String       content.
   * @throws WebdavIntfException
   */
  public String getContentString() throws WebdavIntfException {
    return null;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setOwner()
   */
  public void setOwner(String val) throws WebdavIntfException {
    throw WebdavIntfException.forbidden();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  public String getOwner() throws WebdavIntfException {
    return owner;
  }

  /**
   * @return CalSvcI
   */
  public SysIntf getSysi() {
    return sysi;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public String toString() {
    StringBuffer sb = new StringBuffer(this.getClass().getName());

    sb.append("{");
    sb.append("cdURI=");
    sb.append(cdURI.toString());
    sb.append("}");

    return sb.toString();
  }
}
