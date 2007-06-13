/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
import edu.rpi.cct.webdav.servlet.shared.WebdavNsNode.PropertyTagEntry;
import edu.rpi.sss.util.xml.QName;

import java.io.Serializable;
import java.util.Map;

/** This type of object will handle system dependent properties through the
 * system interface.
 *
 * @author Mike Douglass
 */
public abstract class PropertyHandler implements Serializable {
  /** Allow callers to specify which set of properties are to be processed
   *
   * @author douglm
   */
  public static enum PropertyType {
    /** Property that can apply to anything */
    generalProperty,

    /** */
    principalProperty,

    /** */
    userProperty,

    /** */
    groupProperty,

    /** */
    collectionProperty,

    /** */
    folderProperty,

    /** */
    calendarProperty,
  }

  /**
   * @return Map of valid property names.
   */
  public abstract Map<QName, PropertyTagEntry> getPropertyNames();

  /** Return true if a call to generatePropertyValue will return a value.
   *
   * @param tag
   * @return boolean
   * @throws WebdavException
   */
  public boolean knownProperty(QName tag) throws WebdavException {
    return getPropertyNames().get(tag) != null;
  }
}
