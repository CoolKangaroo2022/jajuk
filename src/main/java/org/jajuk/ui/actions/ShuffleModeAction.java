/*
 *  Jajuk
 *  Copyright (C) The Jajuk Team
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
 *  
 */
package org.jajuk.ui.actions;

import java.awt.event.ActionEvent;

import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;

/**
 * Shuffle mode action
 */
@SuppressWarnings("serial")
public class ShuffleModeAction extends JajukAction {
  /**
   * Instantiates a new shuffle mode action.
   */
  ShuffleModeAction() {
    super(Messages.getString("JajukJMenuBar.11"), IconLoader.getIcon(JajukIcons.SHUFFLE), "ctrl H",
        true);
    setShortDescription(Messages.getString("CommandJPanel.2"));
  }

  /**
   * Invoked when an action occurs.
   * 
   * @param evt 
   */
  @Override
  public void perform(ActionEvent evt) {
    Conf.invert(Const.CONF_STATE_SHUFFLE);
    // Refresh mode buttons
    ObservationManager.notify(new JajukEvent(JajukEvents.MODE_STATUS_CHANGED));
  }
}
