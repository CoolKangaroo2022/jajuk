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

package org.jajuk.ui;

import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jajuk.Main;
import org.jajuk.base.ITechnicalStrings;
import org.jajuk.i18n.Messages;
import org.jajuk.util.log.Log;

/**
 * Manages views
 *
 * @author     bflorat
 * @created    16 nov. 2003
 */
public class ViewManager implements ITechnicalStrings{

	/**View name -> perspective hashmap */
	static HashMap hmViewPerspective = new HashMap(20);
	
	/**View name -> container hashmap */
	static HashMap hmViewContainer = new HashMap(20);
	
	
	/**Maintain relation view/perspective, a view can be in only one perspective*/
	public static void registerView(IView view,IPerspective perspective){
		hmViewPerspective.put(view.getName(),perspective);
		JInternalFrame ji = new JInternalFrame(view.getDesc(),true,true,true,true);
		ji.setContentPane((JComponent)view);
		hmViewContainer.put(view.getName(),ji);
	}
	
	/**
	 * Notify the manager for a request ( refresh...)
	 * @param sEvent
	 * @param view
	 */
	public static void notify(String sEvent,IView view){
		try{
			if (sEvent.equals(EVENT_VIEW_REFRESH_REQUEST)){
				JInternalFrame ji = (JInternalFrame)hmViewContainer.get(view.getName());
				IView newView = (IView)view.getClass().newInstance();  //reinstanciate the view, needed to avoid many repaint problems
				ji.setContentPane((JPanel)newView); //reset content pane. A repaint() inside the JPanel doesn't work.
				ji.setOpaque(true);
				ji.repaint();
				IPerspective perspective = (IPerspective)hmViewPerspective.get(view.getName());
				perspective.getDesktop().repaint();
				Main.jframe.repaint();
			}
			else if (sEvent.equals(EVENT_VIEW_CLOSE_REQUEST)){
				JInternalFrame ji = (JInternalFrame)hmViewContainer.get(view.getName());
				IPerspective perspective = (IPerspective)hmViewPerspective.get(view.getName());
				ji.setVisible(false);
				perspective.getDesktop().repaint();
			}
			else if (sEvent.equals(EVENT_VIEW_SHOW_REQUEST)){
				JInternalFrame ji = (JInternalFrame)hmViewContainer.get(view.getName());
				ji.setVisible(true);
				IPerspective perspective = (IPerspective)hmViewPerspective.get(view.getName());
				perspective.getDesktop().repaint();
				Main.jframe.repaint();
			}
		}catch(Exception e){
			Log.error("118",sEvent,e);
			Messages.showErrorMessage("118",sEvent);
		}
	}
	
	public static void setSize(IView view,int iWidth,int iHeight){
		JInternalFrame frame = (JInternalFrame)hmViewContainer.get(view.getName());
		frame.setSize(iWidth,iHeight);
	}
	
	public static void setLocation(IView view,int iX,int iY){
		JInternalFrame frame = (JInternalFrame)hmViewContainer.get(view.getName());
		frame.setLocation(iX,iY);
	}
	
	public static void setVisible(IView view,boolean b){
		JInternalFrame frame = (JInternalFrame)hmViewContainer.get(view.getName());
		frame.setVisible(b);
	}
	
	public static JInternalFrame getFrame(IView view){
		return (JInternalFrame)hmViewContainer.get(view.getName());
	}
	
	
	
	
	
	

}
