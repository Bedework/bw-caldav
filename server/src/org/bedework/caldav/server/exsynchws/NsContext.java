package org.bedework.caldav.server.exsynchws;

import edu.rpi.sss.util.xml.tagdefs.IcalendarDefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/** Class used for diff etc.
 * @author douglm
 *
 */
public class NsContext implements NamespaceContext {
  private static Map<String, String> keyPrefix = new HashMap<String, String>();
  private static Map<String, String> keyUri = new HashMap<String, String>();

  static {
    addToMap("D", "DAV");
    addToMap("C", "urn:ietf:params:xml:ns:caldav");
    addToMap("X", IcalendarDefs.namespace);
    addToMap("df", "urn:ietf:params:xml:ns:pidf-diff");
  }

  private String defaultNS;

  /** Constructor
   *
   * @param defaultNS
   */
  public NsContext(final String defaultNS) {
    this.defaultNS = defaultNS;
  }

  /**
   * @return default ns or null
   */
  public String getDefaultNS() {
    return defaultNS;
  }

  public String getNamespaceURI(final String prefix) {
    return keyPrefix.get(prefix);
  }

  public Iterator getPrefixes(final String val) {
    return keyPrefix.keySet().iterator();
  }

  public String getPrefix(final String uri) {
    if ((defaultNS != null) && uri.equals(defaultNS)) {
      return null;
    }

    return keyUri.get(uri);
  }

  /** Append the name with abbreviated namespace.
   *
   * @param sb
   * @param nm
   */
  public void appendNsName(final StringBuilder sb,
                           final QName nm) {
    String uri = nm.getNamespaceURI();
    String abbr;

    if ((defaultNS != null) && uri.equals(defaultNS)) {
      abbr = null;
    } else {
      abbr = keyUri.get(uri);
      if (abbr == null) {
        abbr = uri;
      }
    }

    if (abbr != null) {
      sb.append(abbr);
      sb.append(":");
    }

    sb.append(nm.getLocalPart());
  }

  private static void addToMap(final String prefix, final String uri) {
    keyPrefix.put(prefix, uri);
    keyUri.put(uri, prefix);
  }
}
