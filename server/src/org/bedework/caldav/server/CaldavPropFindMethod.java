/**
 *
 */
package org.bedework.caldav.server;

import org.bedework.caldav.server.calquery.CalendarData;
import org.bedework.davdefs.CaldavTags;

import edu.rpi.cct.webdav.servlet.common.PropFindMethod;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavProperty;
import edu.rpi.sss.util.xml.QName;

import org.w3c.dom.Element;

/**
 * @author douglm
 *
 */
public class CaldavPropFindMethod extends PropFindMethod {
  /** Override this to create namespace specific property objects.
   *
   * @param propnode
   * @return WebdavProperty
   * @throws WebdavException
   */
  public WebdavProperty makeProp(Element propnode) throws WebdavException {
    if (!nodeMatches(propnode, CaldavTags.calendarData)) {
      return new WebdavProperty(new QName(propnode.getNamespaceURI(),
                                          propnode.getLocalName()),
                                          null);
    }

    /* Handle the calendar-data element */

    CalendarData caldata = new CalendarData(new QName(propnode.getNamespaceURI(),
                                                      propnode.getLocalName()),
                                                      debug);
    caldata.parse(propnode);

    return caldata;
  }

}
