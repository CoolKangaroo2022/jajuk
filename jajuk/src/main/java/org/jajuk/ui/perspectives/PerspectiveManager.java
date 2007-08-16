/*
 *  Jajuk
 *  Copyright (C) 2005 The Jajuk Team
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
 *  $$Revision$$
 */
package org.jajuk.ui.perspectives;

import org.jajuk.Main;
import org.jajuk.base.Event;
import org.jajuk.base.ObservationManager;
import org.jajuk.i18n.Messages;
import org.jajuk.ui.IPerspective;
import org.jajuk.ui.IView;
import org.jajuk.ui.PerspectiveBarJPanel;
import org.jajuk.util.ConfigurationManager;
import org.jajuk.util.EventSubject;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.IconLoader;
import org.jajuk.util.Util;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.vlsolutions.swing.toolbars.ToolBarContainer;
import com.vlsolutions.swing.toolbars.ToolBarPanel;

/**
 * Perspectives Manager
 */
public class PerspectiveManager implements ITechnicalStrings {
	/** Current perspective */
	private static IPerspective currentPerspective = null;

	/** Perspective name -> perspective */
	private static HashMap<String, IPerspective> hmNameInstance = new HashMap<String, IPerspective>(
			10);

	/** perspective */
	private static Set<IPerspective> perspectives = new LinkedHashSet<IPerspective>(10);

	/** Date used by probe */
	private static long lTime;

	/** Temporary perspective name used when parsing */
	private static String sPerspectiveName;

	/**
	 * Reset registered perspectives
	 * 
	 */
	private static void reset() {
		perspectives.clear();
		hmNameInstance.clear();
	}

	/**
	 * Load configuration file
	 * 
	 * @throws JajukException
	 */
	public static void load() throws JajukException {
		registerDefaultPerspectives();
		if (Main.isUpgradeDetected()) {
			// upgrade message
			Messages.showInfoMessage(Messages.getString("Note.0"));
			// force loading of defaults perspectives
			for (IPerspective perspective : getPerspectives()) {
				// Remove current conf file to force using default file from the
				// jar
				File loadFile = Util.getConfFileByPath(perspective.getClass().getName() + ".xml");
				if (loadFile.exists()) {
					loadFile.delete();
				}
			}
		}
		// Load each perspective
		try {
			for (IPerspective perspective : getPerspectives()) {
				perspective.load();
			}
		} catch (Exception e) {
			throw new JajukException(108, e);
		}
	}

	/**
	 * Begins management
	 */
	public static void init() {
		// Use Simple perspective as a default
		IPerspective perspective = hmNameInstance.get(SimplePerspective.class.getName());
		// If it is a crash recover, force physical perspective to avoid
		// being locked on a buggy perspective like Information
		if (!Main.isCrashRecover()) {
			String sPerspective = Main.getDefaultPerspective();
			/*
			 * take a look to see if a default perspective is set (About tray
			 * for example)
			 */
			if (sPerspective == null) {
				sPerspective = ConfigurationManager.getProperty(CONF_PERSPECTIVE_DEFAULT);
				// no? take the configuration ( user last perspective)
			}
			perspective = hmNameInstance.get(sPerspective);
			// If perspective is no more known, take first perspective found
			if (perspective == null) {
				perspective = perspectives.iterator().next();
			}
		}
		setCurrentPerspective(perspective);
	}

	/*
	 * @see org.jajuk.ui.perspectives.IPerspectiveManager#getCurrentPerspective()
	 */
	public static IPerspective getCurrentPerspective() {
		return PerspectiveManager.currentPerspective;
	}

	/*
	 * @see org.jajuk.ui.perspectives.IPerspectiveManager#setCurrentPerspective(Perspective)
	 */
	public static void setCurrentPerspective(final IPerspective perspective) {
		Util.waiting();
		// views display
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				perspective.setAsBeenSelected(true);
				PerspectiveManager.currentPerspective = perspective;
				for (IView view : perspective.getViews()) {
					if (!view.isPopulated()) {
						try{
							view.initUI();
						}
						catch(Exception e){
							Log.error(e);
						}
						view.setIsPopulated(true);
					}
				}
				ToolBarContainer tbcontainer = Main.getToolbarContainer();
				// Remove all non-toolbar items
				if (tbcontainer.getComponentCount() > 0) {
					Component[] components = tbcontainer.getComponents();
					for (int i = 0; i < components.length; i++) {
						if (!(components[i] instanceof ToolBarPanel)) {
							tbcontainer.remove(components[i]);
						}
					}
				}
				tbcontainer.add(perspective.getContentPane(), BorderLayout.CENTER);
				// refresh UI
				tbcontainer.revalidate();
				tbcontainer.repaint();
				// Select right item in perspective selector
				PerspectiveBarJPanel.getInstance().setActivated(perspective);
				// store perspective selection
				ConfigurationManager.setProperty(CONF_PERSPECTIVE_DEFAULT, perspective.getID());
				Util.stopWaiting();
				// Emit a event
				ObservationManager.notify(new Event(EventSubject.EVENT_PERPECTIVE_CHANGED,
						ObservationManager
								.getDetailsLastOccurence(EventSubject.EVENT_FILE_LAUNCHED)));
			}
		});
	}

	/**
	 * Set current perspective
	 * 
	 * @param sPerspectiveName
	 */
	public static void setCurrentPerspective(String sPerspectiveID) {
		IPerspective perspective = hmNameInstance.get(sPerspectiveID);
		if (perspective == null) {
			perspective = perspectives.iterator().next();
		}
		setCurrentPerspective(perspective);
	}

	/**
	 * Get all perspectives
	 * 
	 * @return all perspectives as a collection
	 */
	public static Set<IPerspective> getPerspectives() {
		return perspectives;
	}

	/**
	 * Get a perspective by ID or null if none associated perspective found
	 * 
	 * @param sID
	 *            perspective ID
	 * @return perspective
	 */
	public static IPerspective getPerspective(String sID) {
		return hmNameInstance.get(sID);
	}

	/**
	 * Saves perspectives and views position in the perspective.xml file
	 */
	public static void commit() throws Exception {
		for (IPerspective perspective : getPerspectives()) {
			perspective.commit();
		}
	}

	/**
	 * Register default perspective configuration. Will be overwritten by
	 * perspective.xml parsing if it exists
	 * 
	 */
	public static void registerDefaultPerspectives() {
		reset();

		IPerspective perspective = null;
		// Simple perspective
		perspective = new SimplePerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_SIMPLE.getUrl());
		registerPerspective(perspective);

		// physical perspective
		perspective = new PhysicalPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_PHYSICAL.getUrl());
		registerPerspective(perspective);

		// Logical perspective
		perspective = new LogicalPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_LOGICAL.getUrl());
		registerPerspective(perspective);

		// Player perspective
		perspective = new PlayerPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_PLAYER.getUrl());
		registerPerspective(perspective);

		// Catalog perspective
		perspective = new CatalogPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_CATALOG.getUrl());
		registerPerspective(perspective);

		// Information perspective
		perspective = new InfoPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_INFORMATION.getUrl());
		registerPerspective(perspective);

		// Configuration perspective
		perspective = new ConfigurationPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_CONFIGURATION.getUrl());
		registerPerspective(perspective);

		// Stats perspective
		perspective = new StatPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_STATISTICS.getUrl());
		registerPerspective(perspective);

		// Help perspective
		perspective = new HelpPerspective();
		perspective.setIconPath(IconLoader.ICON_PERSPECTIVE_HELP.getUrl());
		registerPerspective(perspective);
	}

	/**
	 * Register a new perspective
	 * 
	 * @param perspective
	 * @return registered perspective
	 */
	public static IPerspective registerPerspective(IPerspective perspective) {
		hmNameInstance.put(perspective.getID(), perspective);
		perspectives.add(perspective);
		return perspective;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.xpcom.IAppFileLocProvider#getFile(java.lang.String,
	 *      boolean[])
	 */
	public File getFile(String prop, boolean[] persistent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.xpcom.IAppFileLocProvider#getFiles(java.lang.String)
	 */
	public File[] getFiles(String prop) {
		return null;
	}
}
