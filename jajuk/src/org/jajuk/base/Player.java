/*
 *  Jajuk
 *  Copyright (C) 2003 bflorat
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
 * $Revision$
 */
package org.jajuk.base;

import org.jajuk.i18n.Messages;
import org.jajuk.util.log.Log;

/**
 *  abstract class for music player, independent from real implementation
 *
 * @author     bflorat
 * @created    12 oct. 2003
 */
public class Player {

	private static File fCurrent;
	private static IPlayerImpl pCurrentPlayerImpl;
	/**
	 * Asynchronous play for specified file
	 * @param file
	 */
	public static void play(File file) {
		fCurrent = file;
		pCurrentPlayerImpl = file.getTrack().getType().getPlayerImpl();
		new Thread() {
			public void run() {
				try {
					pCurrentPlayerImpl.play(fCurrent);
				} catch (Exception e) {
					Log.error(Messages.getString("Player.Error_playing____1") + fCurrent.getAbsolutePath(), e); //$NON-NLS-1$
				}
			}
		}
		.start();
	}

	/**
	 * Stop the played track
	 * @param type
	 */
	public static void stop() {
		try {
			if (fCurrent!=null){
				fCurrent.getTrack().getType().getPlayerImpl().stop();
			}
		} catch (Exception e) {
			Log.error(Messages.getString("Player.Error_stoping____2"), e); //$NON-NLS-1$
		}
	}

}
