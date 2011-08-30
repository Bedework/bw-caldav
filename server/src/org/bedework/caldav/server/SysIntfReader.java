/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
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

  private boolean eof;

  private char nextChar;

  /**
   * @param rdr
   */
  public SysIntfReader(final Reader rdr) {
    super();
    lnr = new LineNumberReader(rdr);
    objnum++;
  }

  @Override
  public int read() throws IOException {
    if (!getNextChar()) {
      return -1;
    }

    return nextChar;
  }

  @Override
  public int read(final char[] cbuf, final int off,
                  final int len) throws IOException {
    if (eof) {
      return -1;
    }

    int ct = 0;
    while (ct < len) {
      if (!getNextChar()) {
        eof = true;
        return ct;
      }

      cbuf[off + ct] = nextChar;
      ct++;
    }

    return ct;
  }

  private boolean getNextChar() throws IOException {
    if (eof) {
      return false;
    }

    if (doneLf) {
      // Get new line
      String ln = lnr.readLine();

      if (ln == null) {
        eof = true;
        return false;
      }

      if (getLogger().isDebugEnabled()) {
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

  @Override
  public void close() {
  }

  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void trace(final String msg) {
    getLogger().debug("[" + objnum + "] " + msg);
  }
}
