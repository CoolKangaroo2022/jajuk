/*
 *  Jajuk
 *  Copyright (C) 2005 bertrand florat
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

package org.jajuk.ui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jajuk.Main;
import org.jajuk.base.Device;
import org.jajuk.base.Directory;
import org.jajuk.base.Event;
import org.jajuk.base.File;
import org.jajuk.base.FileManager;
import org.jajuk.base.IPropertyable;
import org.jajuk.base.ItemManager;
import org.jajuk.base.ObservationManager;
import org.jajuk.base.PlaylistFile;
import org.jajuk.base.PlaylistFileManager;
import org.jajuk.base.PropertyMetaInformation;
import org.jajuk.base.StyleManager;
import org.jajuk.base.Track;
import org.jajuk.base.TrackManager;
import org.jajuk.i18n.Messages;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.error.NoneAccessibleFileException;
import org.jajuk.util.log.Log;
import org.jdesktop.swingx.JXDatePicker;

/**
 * Item properties wizard for any jajuk item
 * 
 * @author Bertrand Florat
 * @created 6 juin 2005
 */
public class PropertiesWizard extends JDialog implements ITechnicalStrings,ActionListener {
    
    /*Main panel*/
    JPanel jpMain;
    
    /**OK/Cancel panel*/
    OKCancelPanel okc;
    
    /** Layout dimensions*/
    double[][] dSize = { { 0,TableLayout.PREFERRED,10 }, {0, TableLayout.PREFERRED,10,20,20}};
    
    /**Items*/
    ArrayList<IPropertyable> alItems;
    
    /**Items2*/
    ArrayList<IPropertyable> alItems2;
    
    /**Merge flag*/
    boolean bMerged = false;
    
    /**number of editable items (all panels)*/
    int iEditable = 0;
    
    /**First property panel*/
    PropertiesPanel panel1;
    
    /**Second property panel*/
    PropertiesPanel panel2;
    
    /**
     * Constructor for normal wizard with only one wizard panel and n items to display 
     * 
     * @param alItems items to display
     */
    public PropertiesWizard(ArrayList<IPropertyable> alItems) {
        //windows title: name of the element of only one item, or "selection" word otherwise
        super(Main.getWindow(),alItems.size()==1 ? ((IPropertyable)alItems.get(0)).getDesc():Messages.getString("PropertiesWizard.6"),true); //modal //$NON-NLS-1$
        this.alItems = alItems;
        if (alItems.size() > 1){
            bMerged = true;
        }
        panel1 = new PropertiesPanel(alItems,alItems.size()==1 ? 
                ((IPropertyable)alItems.get(0)).getDesc():
                    Messages.getString("PropertiesWizard.6")); //$NON-NLS-1$
        jpMain = new JPanel();
        jpMain.setLayout(new TableLayout(dSize));
        jpMain.add(panel1,"1,1");     //$NON-NLS-1$
        display();
    }
    
    
    /**
     * Constructor for file wizard for ie with 2 wizard panels and n items to display 
     * 
     * @param alItems1 items to display in the first wizard panel (file for ie)
     * @param alItems2 items to display in the second panel (associated track for ie )
     */
    public PropertiesWizard(ArrayList<IPropertyable> alItems1,ArrayList<IPropertyable> alItems2) {
        //windows title: name of the element of only one item, or "selection" word otherwise
        super(Main.getWindow(),alItems1.size()==1 ? ((IPropertyable)alItems1.get(0)).getDesc():Messages.getString("PropertiesWizard.6"),true); //modal //$NON-NLS-1$
        this.alItems = alItems1;
        this.alItems2 = alItems2;
        if (alItems1.size() > 1){
            bMerged = true;
        }
        jpMain = new JPanel();
        jpMain.setLayout(new TableLayout(dSize));
        JPanel jpProperties = new JPanel();
        double[][] dPanels = {{TableLayout.PREFERRED,20,TableLayout.PREFERRED},{TableLayout.PREFERRED}};
        jpProperties.setLayout(new TableLayout(dPanels));
        if (alItems1.size() == 1){
            panel1 = new PropertiesPanel(alItems1,alItems1.get(0).getDesc());
            panel1.setBorder(BorderFactory.createEtchedBorder());
            panel2 = new PropertiesPanel(alItems2,alItems2.get(0).getDesc());
            panel2.setBorder(BorderFactory.createEtchedBorder());
            jpProperties.add(panel1,"0,0");     //$NON-NLS-1$
            jpProperties.add(panel2,"2,0");     //$NON-NLS-1$
        }
        else{
            panel1 = new PropertiesPanel(alItems1,Util.formatPropertyDesc(Messages.getString("Property_files"))); //$NON-NLS-1$
            panel2 = new PropertiesPanel(alItems2,Util.formatPropertyDesc(Messages.getString("Property_tracks"))); //$NON-NLS-1$
            jpProperties.add(panel1,"0,0");     //$NON-NLS-1$
            jpProperties.add(panel2,"2,0"); //$NON-NLS-1$
        }
        jpMain.add(jpProperties,"1,1"); //$NON-NLS-1$
        display();
    }
    
    private void display(){
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                //OK/Cancel buttons
                okc = new OKCancelPanel(PropertiesWizard.this,Messages.getString("Apply"),Messages.getString("Close")); //$NON-NLS-1$ //$NON-NLS-2$
                //If none editable item, save button is disabled
                if (iEditable == 0){
                    okc.getOKButton().setEnabled(false);
                }
                jpMain.add(okc,"1,3"); //$NON-NLS-1$
                getRootPane().setDefaultButton(okc.getOKButton());
                getContentPane().add(new JScrollPane(jpMain));
                pack();
                setLocationRelativeTo(Main.getWindow());
                setVisible(true);
            }
        });
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okc.getCancelButton()){
            dispose();
        }
        else if (e.getSource().equals(okc.getOKButton())){
            try{ 
                panel1.save();
                if (panel2 != null){
                    panel2.save();
                }
            }
            catch(Exception ex){
                Messages.showErrorMessage("104",ex.getMessage()); //$NON-NLS-1$
                Log.error("104",ex.getMessage(),ex); //$NON-NLS-1$
                return;
            }
        }
    }
    
    /**
     * Tells whether a link button should be shown for a given property 
     * @param meta
     * @return
     */
    public boolean isLinkable(PropertyMetaInformation meta){
        String sKey = meta.getName();
        return sKey.equals(XML_DEVICE) || sKey.equals(XML_TRACK)
        || sKey.equals(XML_DEVICE) || sKey.equals(XML_TRACK)
        ||     sKey.equals(XML_ALBUM) || sKey.equals(XML_AUTHOR) || sKey.equals(XML_STYLE)
        ||     sKey.equals(XML_DIRECTORY) || sKey.equals(XML_FILE)
        ||     sKey.equals(XML_PLAYLIST) || sKey.equals(XML_PLAYLIST_FILE)
        ||    sKey.equals(XML_FILES)|| sKey.equals(XML_PLAYLIST_FILES)
        ||    ( sKey.equals(XML_TYPE) && !(alItems.get(0) instanceof Device)) ;   //avoid to confuse between music types and device types
    }
    
    /**
     * 
     * A properties panel
     * @author Bertrand Florat
     *
     */
    class PropertiesPanel extends JPanel implements ActionListener{
        
        /**Properties panel*/
        JPanel jpProperties;
        
        /**Item description*/
        JLabel jlDesc;
        
        /**All dynamic widgets*/
        JComponent[][] widgets;
        
        /**Properties to display*/
        ArrayList<PropertyMetaInformation> alToDisplay;
        
        /**Items*/
        ArrayList<IPropertyable> alItems;
        /**
         * Property panel for single types elements
         * @param alItems items to display
         * @param sDesc Description (title)
         */
        PropertiesPanel(ArrayList<IPropertyable> alItems,String sDesc) {
            int iX_SEPARATOR = 5;
            int iY_SEPARATOR = 10;
            this.alItems = alItems;
            IPropertyable pa = alItems.get(0); //first item 
            //Process properties to display
            alToDisplay = new ArrayList(10);
            for (PropertyMetaInformation meta:ItemManager.getItemManager(pa.getClass()).getProperties()){;//add only editable and non constructor properties
                if (meta.isVisible() && (bMerged ? meta.isMergeable() : true)){ //if more than one item to display, show only mergeable properties
                    alToDisplay.add(meta);    
                }
            }
            widgets = new JComponent[alToDisplay.size()][5]; //contains widgets for properties
            //Varname | value | link | type | all album 
            double p = TableLayout.PREFERRED;
            double[] dHoriz = {iX_SEPARATOR,p,iX_SEPARATOR,p,iX_SEPARATOR,p,iX_SEPARATOR,p,iX_SEPARATOR,p,iX_SEPARATOR};
            double[] dVert = new double[(2*alToDisplay.size())+3];//*2n+1 rows for spaces + 2 rows for title
            dVert[0]=iY_SEPARATOR;
            dVert[1]=20; //title
            int index = 0;
            for (PropertyMetaInformation meta:alToDisplay){
                //Set layout
                dVert[2*index+2] = iY_SEPARATOR;
                dVert[(2*index)+3] = 20;
                Dimension dim = new Dimension(200,20);
                //Set widgets
                //Property name
                String sName = meta.getHumanName();
                JLabel jlName = new JLabel(sName+" :"); //check if property name is translated (for custom properties));  //$NON-NLS-1$
                if (meta.isCustom()){
                    jlName.setForeground(Color.BLUE);
                }
                widgets[index][0] = jlName;
                //Property value
                //computes editable state
                boolean bEditable = meta.isEditable(); //property editable ?
                if (!meta.isCustom()){ //custom properties are always editable, even for offline items
                    bEditable = bEditable
                    && !(pa instanceof Directory && !((Directory)pa).getDevice().isMounted()) //item is not an unmounted dir
                    && !(pa instanceof File && !((File)pa).isReady())//item is not an unmounted file
                    && !(pa instanceof PlaylistFile && !((PlaylistFile)pa).isReady());//item is not an unmounted playlist file
                }
                if (bEditable){
                    iEditable ++;
                    if (meta.getType().equals(Date.class)){
                        JXDatePicker jdp = new JXDatePicker(pa.getDateValue(meta.getName()).getTime()); //If several items, take first value found
                        widgets[index][1] = jdp;     
                    }
                    else if(meta.getType().equals(Boolean.class)){ //for a boolean, value is a checkbox
                        JCheckBox jcb = new JCheckBox();
                        jcb.setSelected(pa.getBooleanValue(meta.getName()));
                        widgets[index][1] = jcb;
                    }
                    else if(meta.getType().equals(Double.class)){ //for a double, value is a formatted textfield
                        JFormattedTextField jtfValue = new JFormattedTextField(NumberFormat.getInstance());
                        jtfValue.setText(pa.getHumanValue(meta.getName()));//If several items, take first value found
                        jtfValue.setPreferredSize(dim);
                        widgets[index][1] = jtfValue;
                    }
                    else if(meta.getType().equals(Long.class)){ //for a double, value is a formatted textfield
                        JFormattedTextField jtfValue = new JFormattedTextField(NumberFormat.getIntegerInstance());
                        jtfValue.setPreferredSize(dim);
                        jtfValue.setText(pa.getHumanValue(meta.getName()));//If several items, take first value found
                        widgets[index][1] = jtfValue;     
                    }
                    else if (meta.getType().equals(String.class) && meta.getName().equals("style")){ //for styles //$NON-NLS-1$
                        ArrayList<String> alStyles = (ArrayList)StyleManager.getStylesList().clone();
                        alStyles.add(0,pa.getHumanValue(meta.getName()));//display the current genre as default
                        JComboBox jcb = new JComboBox();
                        jcb.setEditable(true);
                        jcb.setPreferredSize(dim);
                        for (String style:alStyles){
                            jcb.addItem(style);
                        }
                        widgets[index][1] = jcb;
                    }
                    else { //for all others formats (string, class)
                        JTextField jtfValue = new JTextField();
                        jtfValue.setText(pa.getHumanValue(meta.getName()));//If several items, take first value found
                        jtfValue.setPreferredSize(dim);
                        widgets[index][1] = jtfValue;     
                    }
                }
                else{
                    JLabel jl = new JLabel(pa.getHumanValue(meta.getName())); //If several items, take first value found
                    jl.setToolTipText(pa.getHumanValue(meta.getName()));
                    jl.setPreferredSize(dim);
                    widgets[index][1] = jl;
                    
                }
                //Link
                if (isLinkable(meta)){
                    JButton jbLink = new JButton(Util.getIcon(ICON_PROPERTIES));
                    jbLink.addActionListener(this);
                    jbLink.setActionCommand("link"); //$NON-NLS-1$
                    widgets[index][2] = jbLink;   
                }
                //Type
                widgets[index][3] = new JLabel(meta.getHumanType());
                //Full album checkbox
                JCheckBox jcbFull = new JCheckBox();
                widgets[index][4] = jcbFull;
                jcbFull.setVisible(pa instanceof Track && !bMerged && meta.isMergeable() && meta.isEditable()); //full album is only available for non-unique and editable properties on single tracks
                
                index ++;
            }
            if (dVert.length > 0){
                dVert[dVert.length-1] = iY_SEPARATOR;//last row is a separator    
            }
            //Create layout
            double[][] dSizeProperties = new double[][]{dHoriz,dVert};  
            dSizeProperties[0]=dHoriz;
            dSizeProperties[1]=dVert;
            //construct properties panel
            jpProperties = new JPanel();
            jpProperties.setLayout(new TableLayout(dSizeProperties));
            //Add title
            JLabel jlName = new JLabel("<html><b>"+Messages.getString("PropertiesWizard.1")+"</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            jlName.setToolTipText(Messages.getString("PropertiesWizard.1")); //$NON-NLS-1$
            JLabel jlValue = new JLabel("<html><b>"+Messages.getString("PropertiesWizard.2")+"</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            jlValue.setToolTipText(Messages.getString("PropertiesWizard.2")); //$NON-NLS-1$
            JLabel jlLink = new JLabel("<html><b>"+Messages.getString("PropertiesWizard.4")+"</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            jlLink.setToolTipText(Messages.getString("PropertiesWizard.4")); //$NON-NLS-1$
            JLabel jlType = new JLabel("<html><b>"+Messages.getString("PropertiesWizard.7")+"</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            jlType.setToolTipText(Messages.getString("PropertiesWizard.7")); //$NON-NLS-1$
            JLabel jlFullAlbum = new JLabel("<html><b>"+Messages.getString("PropertiesWizard.5")+"</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            jlFullAlbum.setToolTipText(Messages.getString("PropertiesWizard.5")); //$NON-NLS-1$
            
            jpProperties.add(jlName,"1,1,c,c"); //$NON-NLS-1$
            jpProperties.add(jlValue,"3,1,c,c"); //$NON-NLS-1$
            jpProperties.add(jlLink,"5,1,c,c"); //$NON-NLS-1$
            jpProperties.add(jlType,"7,1,c,c"); //$NON-NLS-1$
            jpProperties.add(jlFullAlbum,"9,1,c,c"); //$NON-NLS-1$
            
            //Add widgets
            int i = 0;
            int j= 2;
            for (PropertyMetaInformation meta:alToDisplay){
                j = (2*i)+3;
                jpProperties.add(widgets[i][0],"1,"+j+",c,c"); //$NON-NLS-1$ //$NON-NLS-2$
                jpProperties.add(widgets[i][1],"3,"+j); //$NON-NLS-1$
                if (widgets[i][2] != null){ //link widget can be null
                    jpProperties.add(widgets[i][2],"5,"+j+",c,c"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                jpProperties.add(widgets[i][3],"7,"+j+",c,c"); //$NON-NLS-1$ //$NON-NLS-2$
                jpProperties.add(widgets[i][4],"9,"+j+",c,c"); //$NON-NLS-1$ //$NON-NLS-2$
                i++;
            }
            double[][] dSize = { { 0.99 }, { 20, iY_SEPARATOR, 0.99} };
            setLayout(new TableLayout(dSize));
            //desc
            jlDesc = new JLabel(Util.formatPropertyDesc(sDesc));
            add(jlDesc, "0,0"); //$NON-NLS-1$
            add(jpProperties, "0,2"); //$NON-NLS-1$
        }
        
        
        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent ae) {
            //Link
            if (ae.getActionCommand().equals("link")){ //$NON-NLS-1$
                Object oValue = null;
                PropertyMetaInformation meta = alToDisplay.get(getWidgetIndex((JComponent)ae.getSource()));
                String sProperty = meta.getName();
                if (XML_FILES.equals(sProperty)) {
                    String sValue = alItems.get(0).getStringValue(sProperty); //can be only a set a files
                    StringTokenizer st = new StringTokenizer(sValue, ","); //$NON-NLS-1$
                    ArrayList alItems = new ArrayList(3);
                    while (st.hasMoreTokens()) {
                        String sFile = st.nextToken();
                        IPropertyable pa = FileManager.getInstance().getItem(sFile);
                        if (pa != null) {
                            alItems.add(pa);
                        }
                    }
                    new PropertiesWizard(alItems); //show properties window for this item
                }
                else if (XML_PLAYLIST_FILES.equals(sProperty)) {
                    String sValue = alItems.get(0).getStringValue(sProperty); //can be only a set a files
                    StringTokenizer st = new StringTokenizer(sValue, ","); //$NON-NLS-1$
                    ArrayList alItems = new ArrayList(3);
                    while (st.hasMoreTokens()) {
                        String sPlf = st.nextToken();
                        IPropertyable pa = PlaylistFileManager.getInstance().getItem(sPlf);
                        if (pa != null) {
                            alItems.add(pa);
                        }
                    }
                    new PropertiesWizard(alItems); //show properties window for this item
                } else {
                    String sValue = alItems.get(0).getStringValue(sProperty); //can be only an ID
                    IPropertyable pa = ItemManager.getItemManager(sProperty).getItem(sValue);
                    if (pa != null) {
                        ArrayList alItems = new ArrayList(1);
                        alItems.add(pa);
                        new PropertiesWizard(alItems); //show properties window for this item
                    }
                }
            }
        }
        
        /**
         * Save changes in tags
         */
        protected void save() throws Exception{
            Object oValue = null;
            IPropertyable newItem = null;
            ArrayList<PropertyMetaInformation> alChanged = new ArrayList(2);
            int index = -1;
            for (PropertyMetaInformation meta:alToDisplay){
                index++;
                JComponent component = widgets[index][1];
                //non editable item
                if (component instanceof JLabel){
                    continue;
                }
                //Boolean value
                else if (component instanceof JCheckBox){
                    oValue = ((JCheckBox)component).isSelected();
                }
                //Date value
                else if (component instanceof JXDatePicker){
                    oValue = ((JXDatePicker)component).getDate();
                }
                //combobox value
                else if (component instanceof JComboBox){
                    oValue = ((JComboBox)component).getSelectedItem();
                    if (((String)oValue).length() < 1){ //check that string length > 0
                        ((JComboBox)component).setSelectedIndex(0);
                        Log.error("137",meta.getName(),null); //$NON-NLS-1$
                        Messages.showErrorMessage("137",meta.getName()); //$NON-NLS-1$
                        return;
                    }
                }
                //formatted text field
                else if (component instanceof JFormattedTextField){
                    try {
                        ((JFormattedTextField)component).commitEdit();
                    }
                    catch (ParseException e) {
                        Log.error("137",meta.getName(),null); //$NON-NLS-1$
                        Messages.showErrorMessage("137",meta.getName()); //$NON-NLS-1$
                        return;
                    }
                    oValue = ((JFormattedTextField)component).getValue();
                }
                //textfield value
                else if (component instanceof JTextField){
                    oValue = ((JTextField)component).getText();
                    //we check that field is not void (except for comments and custom properties)
                    if (((String)oValue).length() < 1 && !meta.getName().equals(XML_TRACK_COMMENT) && !meta.isCustom()){ //check that string length > 0
                        String sOldValue = alItems.get(0).getHumanValue(meta.getName()); //reset old value
                        ((JTextField)component).setText(sOldValue);
                        Log.error("137",meta.getName(),null); //$NON-NLS-1$
                        Messages.showErrorMessage("137",meta.getHumanType()); //$NON-NLS-1$
                        return;
                    }
                }
                /*Check if reference element (first one inselection) has changed, otherwise, we leave. 
                 * (we only change properties changed in the UI and not properties different between UI and
                 * value otherwise we can overwrite unwanted properties)
                 */
                String sOldValueFirstElement = alItems.get(0).getHumanValue(meta.getName());
                if ( (sOldValueFirstElement == null || Util.format(oValue,meta).equals(sOldValueFirstElement))){
                    continue;
                }
                //Full album ?
                boolean bFull = ((JCheckBox)widgets[index][4]).isSelected();
                //Computes all items to check
                ArrayList<IPropertyable> alItemsToCheck = new ArrayList(alItems.size()); //contains items to be changed
                for (IPropertyable item:alItems){
                    alItemsToCheck.add(item); //add item
                    //add others items from the same album if bFull
                    if (bFull && item instanceof Track){
                        alItemsToCheck.addAll(((Track)item).getAlbum().getTracks());
                    }
                }
                //Now we have all items to concidere, check if we need write
                for (IPropertyable item:alItemsToCheck){
                    //Old value
                    String  sOldValue = item.getHumanValue(meta.getName());
                    if ( (sOldValue!= null && ! Util.format(oValue,meta).equals(sOldValue))){
                        if (oValue == null){
                            throw new JajukException("137"); //$NON-NLS-1$
                        }
                        try{
                            newItem = ItemManager.changeItem(item,meta.getName(),oValue);
                        }
                        catch(NoneAccessibleFileException none){
                            Messages.showErrorMessage(none.getCode());
                            dispose(); //close window to avoid reseting all properties to old values
                            return;
                        }
                        //if this item was element of property panel elements, update it
                        if (alItems.contains(item)){
                            alItems.remove(item);
                            alItems.add(newItem);
                        }
                        //if individual item, change title in case of constructor element change
                        if (!bMerged){ 
                            jlDesc.setText(Util.formatPropertyDesc(newItem.getDesc()));
                        }
                        //note this property have been changed
                        if (!alChanged.contains(meta)){ 
                            alChanged.add(meta);
                        }
                    }
                }
                /*Display a warning message if some files not updated
                note that this message will appear only for first property in failure,
                after, current track will have changed and will no more contain unmounted files*/ 
                if (alItems.get(0) instanceof Track && TrackManager.getInstance().isChangePbm()){
                    Messages.showWarningMessage(Messages.getString("Error.138")); //$NON-NLS-1$
                }
            }
            //display a message if user changed at least one property
            if (alChanged.size() > 0){
                StringBuffer sbChanged = new StringBuffer();
                sbChanged.append("{"); //$NON-NLS-1$
                for (PropertyMetaInformation meta:alChanged){
                    sbChanged.append(meta.getHumanName()).append(' ');  
                }
                sbChanged.append('}');
                InformationJPanel.getInstance().setMessage(
                        alChanged.size()  +" "+Messages.getString("PropertiesWizard.10")+": "+sbChanged.toString(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        InformationJPanel.INFORMATIVE);
            }
            //UI refresh
            if (alChanged.size() > 0){
                Properties properties = new Properties();
                ObservationManager.notify(new Event(EVENT_DEVICE_REFRESH)); 
            }
        }
        
        /**
         * 
         * @param widget
         * @return index of a given widget in the widget table
         */
        private int getWidgetIndex(JComponent widget){
            int resu = -1;
            for (int row=0;row<widgets.length;row++){
                for (int col=0;col<widgets[0].length;col++){
                    if (widget.equals(widgets[row][col])){
                        resu = row;
                        break;
                    }    
                }
                
            }
            return resu;
        }
    }
}
