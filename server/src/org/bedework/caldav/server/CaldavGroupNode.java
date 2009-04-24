/* **********************************************************************
    Copyright 2008 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import org.bedework.caldav.server.sysinterface.CalPrincipalInfo;
import org.bedework.caldav.server.sysinterface.SysIntf;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;

/** Class to represent a group in caldav.
 *
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CaldavGroupNode extends CaldavPrincipalNode {
  /**
   * @param cdURI
   * @param sysi
   * @param ui    User Info
   * @param debug
   * @throws WebdavException
   */
  public CaldavGroupNode(CaldavURI cdURI, SysIntf sysi,
                         CalPrincipalInfo ui,
                         boolean debug) throws WebdavException {
    super(cdURI, sysi, ui, false, debug);
  }
}
