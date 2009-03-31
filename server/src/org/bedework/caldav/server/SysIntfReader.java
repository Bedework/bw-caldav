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

package org.bedework.caldav.server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/** Not sure why we need this - perhaps just for tracing?
 *
 * @author douglm
 *
 */
public class SysIntfReader extends Reader {
  private transient Logger log;

  // separate out interleaved trace
  private volatile static int objnum;

  private LineNumberReader lnr;

  private char[] curChars;
  private int len;
  private int pos;
  private boolean doneCr;
  private boolean doneLf = true;

  private char nextChar;

  private boolean debug;

  /**
   * @param rdr
   * @param debug
   */
  public SysIntfReader(Reader rdr,
                       boolean debug) {
    super();
    lnr = new LineNumberReader(rdr);
    this.debug = debug;
    objnum++;
  }

  public int read() throws IOException {
    if (!getNextChar()) {
      return -1;
    }

    return nextChar;
  }

  public int read(char[] cbuf, int off, int len) throws IOException {
    int ct = 0;
    while (ct < len) {
      if (!getNextChar()) {
        return ct;
      }

      cbuf[off + ct] = nextChar;
      ct++;
    }

    return ct;
  }

  private boolean getNextChar() throws IOException {
    if (doneLf) {
      // Get new line
      String ln = lnr.readLine();

      if (ln == null) {
        return false;
      }

      if (debug) {
        trace(ln);
      }

      pos = 0;
      len = ln.length();
      curChars = ln.toCharArray();
      doneLf = false;
      doneCr = false;
    }

    if (pos == len) {
      if (!doneCr) {
        doneCr = true;
        nextChar = '\r';
        return true;
      }

      doneLf = true;
      nextChar = '\n';
      return true;
    }

    nextChar = curChars[pos];
    pos ++;
    return true;
  }

  public void close() {
  }

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void trace(String msg) {
    getLogger().debug("[" + objnum + "] " + msg);
  }
}
