/*
 *  Jajuk
 *  Copyright (C) 2003-2008 The Jajuk Team
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
package org.jajuk.ui.windows;

import static org.jajuk.ui.actions.JajukActions.NEXT_TRACK;
import static org.jajuk.ui.actions.JajukActions.PAUSE_RESUME_TRACK;
import static org.jajuk.ui.actions.JajukActions.PREVIOUS_TRACK;
import static org.jajuk.ui.actions.JajukActions.STOP_TRACK;

import com.vlsolutions.swing.docking.ui.DockingUISettings;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.substance.CircleButtonShaper;
import org.jajuk.ui.substance.LeftConcaveButtonShaper;
import org.jajuk.ui.substance.RightConcaveButtonShaper;
import org.jajuk.ui.substance.RoundRectButtonShaper;
import org.jajuk.ui.views.AnimationView;
import org.jajuk.ui.views.CoverView;
import org.jajuk.ui.widgets.JajukButton;
import org.jajuk.ui.widgets.TrackPositionSliderToolbar;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * The full screen window Note that not all operating support full screen mode.
 * If the OS doesn't support it, the user cannot access to it so we have not to
 * handle any errors.
 * 
 */
public class JajukFullScreenWindow extends JWindow implements JajukWindow {

  private static final long serialVersionUID = -2859302706462954993L;

  private static JajukFullScreenWindow instance = null;

  private final DisplayMode origDisplayMode;

  private final GraphicsDevice graphicsDevice;

  private JButton jbPrevious;

  private JButton jbNext;

  private JButton jbPlayPause;

  private JButton jbStop;

  private JajukButton jbFull;

  private JajukButton jbExit;

  private CoverView coverView;

  /**
   * State decorator
   */
  private WindowStateDecorator decorator;

  public static JajukFullScreenWindow getInstance() {
    if (instance == null) {
      instance = new JajukFullScreenWindow();
      instance.decorator = new WindowStateDecorator(instance) {
        @Override
        public void specificBeforeShown() {
          JajukMainWindow.getInstance().setVisible(false);
          instance.graphicsDevice.setFullScreenWindow(instance);

          // topPanel should have 10% of the display resolution height
          instance.setPreferredSize(new Dimension(instance.graphicsDevice.getDisplayMode()
              .getWidth(), (instance.graphicsDevice.getDisplayMode().getHeight() / 100) * 10));

          instance.validate();
        }

        @Override
        public void specificAfterShown() {
          // TODO Auto-generated method stub

        }

        @Override
        public void specificAfterHidden() {
          // TODO Auto-generated method stub

        }

        @Override
        public void specificBeforeHidden() {
          // TODO Auto-generated method stub

        }
      };
    }
    return instance;
  }

  public JajukFullScreenWindow() {
    // get the active graphic device and store the current mode
    this.graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice();
    this.origDisplayMode = graphicsDevice.getDisplayMode();
  }

  public void initUI() {

    // Light drag and drop for VLDocking
    UIManager.put("DragControler.paintBackgroundUnderDragRect", Boolean.FALSE);
    DockingUISettings.getInstance().installUI();

    // Set windows decoration to look and feel
    JFrame.setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);

    // Full screen switch button
    jbFull = new JajukButton(ActionManager.getAction(JajukActions.FULLSCREEN_JAJUK));

    // Exit button
    jbExit = new JajukButton(ActionManager.getAction(JajukActions.EXIT));

    // Animation view
    AnimationView animationView = new AnimationView();
    animationView.initUI();

    // Cover view
    coverView = new CoverView();
    coverView.initUI(false);

    // Player toolbar
    JPanel jtbPlay = getPlayerPanel();

    // Information panel
    TrackPositionSliderToolbar tpst = new TrackPositionSliderToolbar();

    // Add items
    setLayout(new MigLayout("ins 0", "[grow]", "[][grow][70%!][][]"));
    add(jbFull, "right,split 2,gapright 5");
    add(jbExit, "right,wrap");
    add(animationView, "alignx center,aligny top,grow,gap bottom 20,wrap");
    add(coverView, "alignx center, grow,gap bottom 20,wrap");
    add(jtbPlay, "alignx center,gap bottom 20,wrap");
    add(tpst, "alignx center,width 50%!,aligny bottom,gap bottom 10");

    // Set new state
    decorator.setWindowState(WindowState.BUILD_NOT_DISPLAYED);
  }

  /**
   * @return
   */
  private JPanel getPlayerPanel() {
    JPanel jPanelPlay = new JPanel();
    jPanelPlay.setLayout(new MigLayout("insets 5", "[grow][grow][grow]"));

    // previous
    jbPrevious = new JajukButton(ActionManager.getAction(PREVIOUS_TRACK));
    int concavity = IconLoader.getIcon(JajukIcons.PLAYER_PLAY).getIconHeight();
    jbPrevious.putClientProperty(SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY,
        new RightConcaveButtonShaper(concavity));
    jbPrevious.setBorderPainted(true);
    jbPrevious.setContentAreaFilled(true);
    jbPrevious.setFocusPainted(true);

    // next
    jbNext = new JajukButton(ActionManager.getAction(NEXT_TRACK));
    jbNext.putClientProperty(SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY,
        new LeftConcaveButtonShaper(concavity));

    // play pause
    jbPlayPause = new JajukButton(ActionManager.getAction(PAUSE_RESUME_TRACK));
    jbPlayPause.putClientProperty(SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY,
        new CircleButtonShaper());

    // stop
    jbStop = new JajukButton(ActionManager.getAction(STOP_TRACK));
    jbStop.putClientProperty(SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY,
        new RoundRectButtonShaper());

    jPanelPlay.add(jbStop, "center,split 6,width 40!,height 30,gapright 5!");
    jPanelPlay.add(jbPrevious, "center,width 62!,height 30!,gapright 0");
    jPanelPlay.add(jbPlayPause, "center,width 45!,height 45!,gapright 0");
    jPanelPlay.add(jbNext, "center,width 62!,height 30!,gapright 3");

    return jPanelPlay;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.widgets.JajukWindow#getWindowStateDecorator()
   */
  public WindowStateDecorator getWindowStateDecorator() {
    return decorator;
  }
}
