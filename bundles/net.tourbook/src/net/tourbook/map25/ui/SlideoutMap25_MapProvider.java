/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.map25.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.Map25View;
import net.tourbook.preferences.MapsforgeThemeStyle;
import net.tourbook.preferences.PrefPage_Map25Provider;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

/**
 * 2.5D map provider slideout
 */
public class SlideoutMap25_MapProvider extends ToolbarSlideout implements IMapProviderListener {

   private SelectionAdapter         _defaultSelectionListener;
   private MouseWheelListener       _defaultMouseWheelListener;
   private FocusListener            _keepOpenListener;

   private PixelConverter           _pc;

   private ActionOpenPrefDialog     _actionPrefDialog;

   private Map25View                _map25View;

   private ArrayList<Map25Provider> _allEnabledMapProvider;

   private boolean                  _isInUpdateUI;

   /*
    * UI controls
    */
   private Composite _parent;

   private Combo     _comboMapProvider;
   private Combo     _comboThemeStyle;

   private Label     _lblMapProvider;
   private Label     _lblThemeStyle;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_MapProvider(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map25View map25View) {

      super(ownerControl, toolBar);

      _map25View = map25View;

      Map25ProviderManager.addMapProviderListener(this);

   }

   private void createActions() {

      _actionPrefDialog = new ActionOpenPrefDialog(
            Messages.Tour_Action_EditChartPreferences,
            PrefPage_Map25Provider.ID);

      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_parent.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableActions();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
         }
         createUI_20_MapProvider(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25Provider_Label_MapProvider);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_MapProvider(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Map provider
             */
            _lblMapProvider = new Label(container, SWT.NONE);
            _lblMapProvider.setText(Messages.Slideout_Map25MapProvider_Label_MapProvider);

            _comboMapProvider = new Combo(container, SWT.READ_ONLY);
            _comboMapProvider.addFocusListener(_keepOpenListener);
            _comboMapProvider.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_MapProvider();
               }
            });
         }
         {
            /*
             * Theme style
             */
            _lblThemeStyle = new Label(container, SWT.NONE);
            _lblThemeStyle.setText(Messages.Slideout_Map25MapProvider_Label_ThemeStyle);

            _comboThemeStyle = new Combo(container, SWT.READ_ONLY);
            _comboThemeStyle.addFocusListener(_keepOpenListener);
            _comboThemeStyle.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_ThemeStyle();
               }
            });
         }
      }

      fillMapProvider();
   }

   private void enableActions() {

   }

   private void fillMapProvider() {

      final ArrayList<Map25Provider> allMapProviders = Map25ProviderManager.getAllMapProviders();
      final ArrayList<Map25Provider> allEnabledMapProviders = new ArrayList<>();

      for (final Map25Provider map25Provider : allMapProviders) {

         // hide disabled map provider

         if (map25Provider.isEnabled) {
            allEnabledMapProviders.add(map25Provider);
         }
      }

      Collections.sort(allEnabledMapProviders, new Comparator<Map25Provider>() {

         @Override
         public int compare(final Map25Provider mp1, final Map25Provider mp2) {
            return mp1.name.compareTo(mp2.name);
         }
      });

      _allEnabledMapProvider = allEnabledMapProviders;

      _comboMapProvider.removeAll();

      for (final Map25Provider map25Provider : allEnabledMapProviders) {
         _comboMapProvider.add(map25Provider.name);
      }
   }

   private Map25Provider getSelectedMapProvider() {

      final int selectedIndex = _comboMapProvider.getSelectionIndex();

      if (selectedIndex < 0) {

         return _allEnabledMapProvider.get(0);

      } else {

         return _allEnabledMapProvider.get(selectedIndex);
      }
   }

   /**
    * @return Returns the map provider which is currently used in the map
    */
   private Map25Provider getUsedMapProvider() {

      return _map25View.getMapApp().getSelectedMapProvider();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeUI();
         }
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   @Override
   public void mapProviderListChanged() {

      if (_comboMapProvider == null) {

         // this can occure when dialog is closed and the event is still fired

         return;
      }

      fillMapProvider();
   }

   private void onChangeUI() {

      saveState();

      enableActions();
   }

   @Override
   protected void onDispose() {

      Map25ProviderManager.removeMapProviderListener(this);

      super.onDispose();
   }

   private void onSelect_MapProvider() {

      if (_isInUpdateUI) {
         return;
      }

      final Map25Provider selectedMapProvider = getSelectedMapProvider();

      // check if a new map provider is selected
      if (selectedMapProvider == getUsedMapProvider()) {
         return;
      }

      updateUI_ThemeStyle(selectedMapProvider);

      _map25View.getMapApp().setMapProvider(selectedMapProvider);
   }

   private void onSelect_ThemeStyle() {

      if (_isInUpdateUI) {
         return;
      }

      final Map25Provider mapProvider = getSelectedMapProvider();
      final List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);

      final int selectedStyleIndex = _comboThemeStyle.getSelectionIndex();

      // update model
      mapProvider.themeStyle = mfStyles.get(selectedStyleIndex).getXmlLayer();

      // update UI
      _map25View.getMapApp().setMapProvider(mapProvider);
   }

   private void restoreState() {

      /*
       * Reselect map provider
       */
      final Map25Provider currentMapProvider = getUsedMapProvider();

      selectMapProvider(currentMapProvider);
   }

   private void saveState() {

   }

   public void selectMapProvider(final Map25Provider mapProvider) {

      if (_allEnabledMapProvider == null) {

         // this can occure when not yet fully setup

         return;
      }

      for (int providerIndex = 0; providerIndex < _allEnabledMapProvider.size(); providerIndex++) {

         final Map25Provider map25Provider = _allEnabledMapProvider.get(providerIndex);
         if (mapProvider.equals(map25Provider)) {

            _isInUpdateUI = true;
            {
               _comboMapProvider.select(providerIndex);
            }
            _isInUpdateUI = false;

            break;
         }
      }
   }

   private void updateUI_ThemeStyle(final Map25Provider mapProvider) {

      _comboThemeStyle.removeAll();

      if (mapProvider.isOfflineMap == false) {

         _comboThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_NotSupported);
         _comboThemeStyle.select(0);

         _comboThemeStyle.setEnabled(false);

         return;
      }

      final List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);

      // check if style is valid
      if (mfStyles == null) {

         _comboThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_InvalidThemeFilename);
         _comboThemeStyle.select(0);

         _comboThemeStyle.setEnabled(false);

         return;
      }

      /*
       * Fill combo with all styles
       */
      _comboThemeStyle.setEnabled(true);
      int styleSelectIndex = 0;

      for (int styleIndex = 0; styleIndex < mfStyles.size(); styleIndex++) {

         final MapsforgeThemeStyle mfStyle = mfStyles.get(styleIndex);

         _comboThemeStyle.add(mfStyle.getLocaleName());

         if (mfStyle.getXmlLayer().equals(mapProvider.themeStyle)) {
            styleSelectIndex = styleIndex;
         }
      }

      // select map provider style
      _isInUpdateUI = true;
      {
         _comboThemeStyle.select(styleSelectIndex);
      }
      _isInUpdateUI = false;
   }

}