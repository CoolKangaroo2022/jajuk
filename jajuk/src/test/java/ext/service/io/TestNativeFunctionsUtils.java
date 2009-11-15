/*
 *  Jajuk
 *  Copyright (C) 2003-2009 The Jajuk Team
 *  http://jajuk.info
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $Revision: 3132 $
 */
package ext.service.io;

import org.jajuk.JUnitHelpers;
import org.jajuk.JajukTestCase;

/**
 * 
 */
public class TestNativeFunctionsUtils extends JajukTestCase {

  /**
   * Test method for
   * {@link ext.service.io.NativeFunctionsUtils#getShortPathNameW(java.lang.String)}
   * .
   */
  public void testGetShortPathNameW() {
    // currently it will always return empty, on Unix because it is not
    // implemented
    // and on Windows for some unknown reason, maybe the library path is not set
    // correctly...
    assertEquals("", NativeFunctionsUtils.getShortPathNameW("testpath.txt"));
    assertEquals("", NativeFunctionsUtils.getShortPathNameW("testpath.long"));
    assertEquals("", NativeFunctionsUtils
        .getShortPathNameW("C:\\verylongpathname.testing\\withaverylongtestpath.long"));
  }

  // helper method to emma-coverage of the unused constructor
  public void testPrivateConstructor() throws Exception { // For EMMA
    JUnitHelpers.executePrivateConstructor(NativeFunctionsUtils.class);
  }
}
