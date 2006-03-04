/*
 *  Jajuk
 *  Copyright (C) 2006 Administrateur
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
 *  $Revision$
 */

package org.jajuk.ui.action;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.jajuk.base.Device;
import org.jajuk.base.Directory;
import org.jajuk.base.Event;
import org.jajuk.base.File;
import org.jajuk.base.ObservationManager;
import org.jajuk.base.Track;
import org.jajuk.i18n.Messages;
import org.jajuk.ui.InformationJPanel;
import org.jajuk.util.ConfigurationManager;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;
import org.jajuk.util.log.Log;

public class RefactorAction implements ITechnicalStrings{
    
    ArrayList <File> alFiles;
    String filename;      
    
    public RefactorAction (ArrayList<File> al){
        alFiles = al;
        if (Boolean.valueOf(ConfigurationManager.getProperty(CONF_CONFIRMATIONS_REFACTOR_FILES)).booleanValue()){
            int iResu = Messages.getChoice(Messages.getString("Confirmation_refactor_files"),JOptionPane.INFORMATION_MESSAGE);  //$NON-NLS-1$ //$NON-NLS-2$
            if (iResu != JOptionPane.YES_OPTION){
                return;                       
            }
        } 
        new Thread() {
            public void run() {                
                refactor();
            }
        }.start();
        Util.stopWaiting();   
        ObservationManager.notify(new Event(EVENT_DEVICE_REFRESH));
    }
    
    public void refactor(){               
        Iterator it = alFiles.iterator();
        
        while (it.hasNext()){
            File fCurrent = (File) it.next();
            Track tCurrent = fCurrent.getTrack(); 
            filename = ConfigurationManager.getProperty(CONF_REFACTOR_PATTERN);
            filename = filename.replace("%a",tCurrent.getAuthor().getName2());
            filename = filename.replace("%s",tCurrent.getStyle().getName2());
            filename = filename.replace("%A",tCurrent.getAlbum().getName2());                     
            if (tCurrent.getOrder() < 10) {
                filename = filename.replace("%n","0"+tCurrent.getOrder());
            } else {
                filename = filename.replace("%n",tCurrent.getOrder()+"");
            }
            filename = filename.replace("%t",tCurrent.getName());
            
            if (tCurrent.getYear() != 0){
                filename = filename.replace("%y",tCurrent.getYear()+"");
            } else {
                filename = filename.replace("%y - ","");
            }                      
            filename += "."+tCurrent.getType().getExtension();
            
            // Compute the new filename
            java.io.File fOld = fCurrent.getIO();
            java.io.File fNew = new java.io.File(fCurrent.getDevice().getUrl()+"/"+filename);
            
            // Create Directories
            boolean bDir = fNew.getParentFile().mkdirs();
            
            // Move file but save old Directory pathname
            String sDirUrl = fOld.getParent();
            boolean bState = fOld.renameTo(fNew);
            
            // Put some message
            if (bState){
            	InformationJPanel.getInstance().setMessage("File " + Messages.getString(fNew.getAbsolutePath()+" moved"),InformationJPanel.INFORMATIVE); //$NON-NLS-1$
            } else {
            	InformationJPanel.getInstance().setMessage("File " + Messages.getString(fNew.getAbsolutePath()+" could not be moved !"),InformationJPanel.ERROR); //$NON-NLS-1$
            }
            
            
                                    
            // Delete Empty dir
            java.io.File dOld = new java.io.File(sDirUrl);            
            boolean bDelete = dOld.delete();
            
//          Debug log
            Log.debug("[Refactoring] {{"+ fNew.getAbsolutePath() +"}} Success ? " + bState);
            Log.debug("[Refactoring] Empty Dir should be deleted :  {{"+ sDirUrl +"}} "+ bDelete);
            
        }       
    }
}
