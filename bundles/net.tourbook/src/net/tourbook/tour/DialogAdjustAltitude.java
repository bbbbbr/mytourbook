/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartCursor;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.MouseAdapter;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.Util;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.IXAxisSelectionListener;
import net.tourbook.ui.tourChart.SplineDrawingData;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class DialogAdjustAltitude extends TitleAreaDialog implements I2ndAltiLayer {

   private static final String ID = "net.tourbook.tour.DialogAdjustAltitude"; //$NON-NLS-1$

   // 40 is the largest size that the mouse wheel can adjust the scale by 1 (windows)
   public static final int     MAX_ADJUST_GEO_POS_SLICES           = 40;

   private static final String WIDGET_DATA_ALTI_ID                 = "altiId";         //$NON-NLS-1$
   private static final String WIDGET_DATA_METRIC_ALTITUDE         = "metricAltitude"; //$NON-NLS-1$

   private static final int    ALTI_ID_START                       = 1;
   private static final int    ALTI_ID_END                         = 2;
   private static final int    ALTI_ID_MAX                         = 3;

   private static final int    ADJUST_TYPE_SRTM                    = 1010;
   private static final int    ADJUST_TYPE_SRTM_SPLINE             = 1020;
   private static final int    ADJUST_TYPE_WHOLE_TOUR              = 1030;
   private static final int    ADJUST_TYPE_START_AND_END           = 1040;
   private static final int    ADJUST_TYPE_MAX_HEIGHT              = 1050;
   private static final int    ADJUST_TYPE_END                     = 1060;
   private static final int    ADJUST_TYPE_HORIZONTAL_GEO_POSITION = 1100;

// SET_FORMATTING_OFF

   private static AdjustmentType[]      ALL_ADJUSTMENT_TYPES      = new AdjustmentType[] {

         new AdjustmentType(ADJUST_TYPE_SRTM_SPLINE,              Messages.adjust_altitude_type_srtm_spline),
         new AdjustmentType(ADJUST_TYPE_SRTM,                     Messages.adjust_altitude_type_srtm),
         new AdjustmentType(ADJUST_TYPE_START_AND_END,            Messages.adjust_altitude_type_start_and_end),
         new AdjustmentType(ADJUST_TYPE_MAX_HEIGHT,               Messages.adjust_altitude_type_adjust_height),
         new AdjustmentType(ADJUST_TYPE_END,                      Messages.adjust_altitude_type_adjust_end),
         new AdjustmentType(ADJUST_TYPE_WHOLE_TOUR,               Messages.adjust_altitude_type_adjust_whole_tour),
         new AdjustmentType(ADJUST_TYPE_HORIZONTAL_GEO_POSITION,  Messages.Adjust_Altitude_Type_HorizontalGeoPosition),
         //
   };

   private static final String         PREF_ADJUST_TYPE           = "adjust.altitude.adjust_type";                //$NON-NLS-1$
   private static final String         PREF_KEEP_START            = "adjust.altitude.keep_start";                 //$NON-NLS-1$
   private static final String         PREF_SCALE_GEO_POSITION    = "Dialog_AdjustAltitude_GeoPositionScale";     //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final NumberFormat _nf = NumberFormat.getNumberInstance();

   static {
      _nf.setMinimumFractionDigits(0);
      _nf.setMaximumFractionDigits(3);
   }

   private final IDialogSettings  _state     = TourbookPlugin.getState(ID);
   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * data
    */
   private boolean                         _isSliderEventDisabled;
   private boolean                         _isTourSaved              = false;
   private final boolean                   _isSaveTour;
   private final boolean                   _isCreateDummyAltitude;

   private final TourData                  _tourData;
   private SplineData                      _splineData;

   private float[]                         _backupMetricAltitudeSerie;
   private float[]                         _backupSrtmSerie;
   private float[]                         _backupSrtmSerieImperial;

   private float[]                         _metricAdjustedAltitudeWithoutSRTM;

   private int                             _oldAdjustmentType        = -1;
   private final ArrayList<AdjustmentType> _availableAdjustmentTypes = new ArrayList<>();

   private int                             _pointHitIndex            = -1;
   private float                           _altiDiff;
   private double                          _sliderXAxisValue;

   private boolean                         _canDeletePoint;
   private boolean                         _isDisableModifyListener;

   private float                           _initialAltiStart;
   private float                           _initialAltiMax;

   private float                           _altiMaxDiff;
   private float                           _altiStartDiff;

   private float                           _prevAltiMax;
   private float                           _prevAltiStart;

   /*
    * UI controls
    */
   private TourChart              _tourChart;
   private TourChartConfiguration _tourChartConfig;

   private Composite              _dlgContainer;

   private PageBook               _pageBookOptions;
   private Label                  _pageEmpty;
   private Composite              _pageOption_SRTMSpline;
   private Composite              _pageOption_NoSRTM;
   private Composite              _pageOption_SRTM;
   private Composite              _pageOption_GeoPosition;

   private Combo                  _comboAdjustmentType;

   private Button                 _btnSRTMRemoveAllPoints;
   private Button                 _btnResetAltitude;
   private Button                 _btnUpdateAltitude;
   private Link                   _linkSRTMSelectWholeTour;

   private Spinner                _spinnerNewStartAlti;
   private Spinner                _spinnerNewMaxAlti;
   private Spinner                _spinnerNewEndAlti;

   private Label                  _lblOldStartAlti;
   private Label                  _lblOldMaxAlti;
   private Label                  _lblOldEndAlti;

   private Button                 _rdoKeepBottom;
   private Button                 _rdoKeepStart;

   private Label                  _lblSliceValue;
   private Scale                  _scaleSlicePos;

   private ChartLayer2ndAltiSerie _chartLayer2ndAltiSerie;

   private static class AdjustmentType {

      int    __id;
      String __visibleName;

      AdjustmentType(final int id, final String visibleName) {
         __id = id;
         __visibleName = visibleName;
      }
   }

   public DialogAdjustAltitude(final Shell parentShell,
                               final TourData tourData,
                               final boolean isSaveTour,
                               final boolean isCreateDummyAltitude) {

      super(parentShell);

      _tourData = tourData;
      _isSaveTour = isSaveTour;
      _isCreateDummyAltitude = isCreateDummyAltitude;

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude).createImage());

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
   }

   void actionCreateSplinePoint(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

      if (computeNewPoint(mouseDownDevPositionX, mouseDownDevPositionY, 1)) {
         onSelectAdjustmentType();
      }
   }

   void actionCreateSplinePoint3(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {
      if (computeNewPoint(mouseDownDevPositionX, mouseDownDevPositionY, 3)) {
         onSelectAdjustmentType();
      }
   }

   @Override
   public boolean close() {

      saveState();

      if (_isTourSaved == false) {

         // tour is not saved, dialog is canceled, restore original values

         if (_isCreateDummyAltitude) {
            _tourData.altitudeSerie = null;
         } else {
            _tourData.altitudeSerie = _backupMetricAltitudeSerie;
         }
         _tourData.clearAltitudeSeries();

         _tourData.setSRTMValues(_backupSrtmSerie, _backupSrtmSerieImperial);
      }

      return super.close();
   }

   private void computeAltitude_WithoutSRTM() {

      final float newAltiStart = (Float) _spinnerNewStartAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
      final float newAltiEnd = (Float) _spinnerNewEndAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
      final float newAltiMax = (Float) _spinnerNewMaxAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);

      final float[] metricAltitudeSerie = _tourData.altitudeSerie;

      // set adjustment type and enable the field(s) which can be modified
      switch (getSelectedAdjustmentType().__id) {

      case ADJUST_TYPE_WHOLE_TOUR:

         // adjust evenly
         computeAltitudeEvenly(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, newAltiStart);
         break;

      case ADJUST_TYPE_START_AND_END:

         // adjust start, end and max

         // first adjust end alti to start alti, secondly adjust max
         computeAltitudeEnd(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, metricAltitudeSerie[0]);
         computeAltitudeStartAndMax(
               _metricAdjustedAltitudeWithoutSRTM,
               _metricAdjustedAltitudeWithoutSRTM,
               newAltiStart,
               newAltiMax);

         break;

      case ADJUST_TYPE_END:

         // adjust end
         computeAltitudeEnd(metricAltitudeSerie, _metricAdjustedAltitudeWithoutSRTM, newAltiEnd);
         break;

      case ADJUST_TYPE_MAX_HEIGHT:

         // adjust max

         computeAltitudeStartAndMax(
               metricAltitudeSerie,
               _metricAdjustedAltitudeWithoutSRTM,
               newAltiStart,
               newAltiMax);
         break;

      default:
         break;
      }

      _tourData.clearAltitudeSeries();
   }

   /**
    * adjust end altitude
    *
    * @param altiSrc
    * @param tourData
    * @param newEndAlti
    */
   private void computeAltitudeEnd(final float[] altiSrc, final float[] altiDest, final float newEndAlti) {

      double[] xDataSerie = _tourData.getDistanceSerieDouble();
      if (xDataSerie == null) {
         xDataSerie = _tourData.getTimeSerieDouble();
      }

      final float altiEndDiff = newEndAlti - altiSrc[altiDest.length - 1];
      final double lastXValue = xDataSerie[xDataSerie.length - 1];

      for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {
         final double xValue = xDataSerie[serieIndex];
         final float altiDiff = (float) (xValue / lastXValue * altiEndDiff);// + 0.5f;
         altiDest[serieIndex] = altiSrc[serieIndex] + altiDiff;
      }
   }

   /**
    * adjust every altitude with the same difference
    *
    * @param altiSrc
    * @param altiDest
    * @param newStartAlti
    */
   private void computeAltitudeEvenly(final float[] altiSrc, final float[] altiDest, final float newStartAlti) {

      final float altiStartDiff = newStartAlti - altiSrc[0];

      for (int altIndex = 0; altIndex < altiSrc.length; altIndex++) {
         altiDest[altIndex] = altiSrc[altIndex] + altiStartDiff;
      }
   }

   /**
    * Adjust max altitude, keep min value
    *
    * @param altiSrc
    * @param altiDest
    * @param maxAltiNew
    */
   private void computeAltitudeMax(final float[] altiSrc, final float[] altiDest, final float maxAltiNew) {

      // calculate min/max altitude
      float minAltiSrc = altiSrc[0];
      float maxAltiSrc = altiSrc[0];
      for (final float altitude : altiSrc) {
         if (altitude > maxAltiSrc) {
            maxAltiSrc = altitude;
         }
         if (altitude < minAltiSrc) {
            minAltiSrc = altitude;
         }
      }

      // adjust altitude
      final float diffSrc = maxAltiSrc - minAltiSrc;
      final float diffNew = maxAltiNew - minAltiSrc;

      final float scaleDiff = diffSrc / diffNew;

      for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {

         final float altiDiff = altiSrc[serieIndex] - minAltiSrc;
         final float alti0Based = altiDiff / scaleDiff;

         altiDest[serieIndex] = alti0Based + minAltiSrc;
      }
   }

   private void computeAltitudeSRTM() {

      // srtm values are available, otherwise this option is not available in the combo box

      final int serieLength = _tourData.timeSerie.length;

      final float[] adjustedAltiSerie = _tourData.dataSerieAdjustedAlti = new float[serieLength];
      final float[] diffTo2ndAlti = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];

      // get altitude diff serie
      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float srtmAltitude = _backupSrtmSerie[serieIndex];

         diffTo2ndAlti[serieIndex] = 0;
         adjustedAltiSerie[serieIndex] = srtmAltitude / UI.UNIT_VALUE_ALTITUDE;
      }
   }

   /**
    * adjust start altitude until left slider
    */
   private void computeAltitudeSRTMSpline() {

      // srtm values are available, otherwise this option is not available in the combo box

      final int leftSliderIndex = _tourChart.getXSliderPosition().getLeftSliderValueIndex();
      final int serieLength = _tourData.timeSerie.length;

      final float[] adjustedAltiSerie = _tourData.dataSerieAdjustedAlti = new float[serieLength];
      final float[] diffTo2ndAlti = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];
      final float[] splineDataSerie = _tourData.dataSerieSpline = new float[serieLength];

      final double[] xDataSerie = _tourChartConfig.isShowTimeOnXAxis ? //
            _tourData.getTimeSerieDouble()
            : _tourData.getDistanceSerieDouble();
//      final float[] yDataSerie = _tourData.getAltitudeSerie();
      final float[] yDataSerie = _tourData.altitudeSerie;

      _sliderXAxisValue = xDataSerie[leftSliderIndex];
      _altiDiff = _backupSrtmSerie[0] - yDataSerie[0];

      // ensure that a point can be moved with the mouse
      _altiDiff = _altiDiff == 0 ? 1 : _altiDiff;

      final CubicSpline cubicSpline = updateSplineData();

      /*
       * get adjusted altitude serie
       */
      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float yValue = yDataSerie[serieIndex];
         final float srtmValue = _backupSrtmSerie[serieIndex];

         if (serieIndex <= leftSliderIndex && leftSliderIndex != 0) {

            // add adjusted altitude

            final double distance = xDataSerie[serieIndex];
            final double distanceScale = 1 - (distance / _sliderXAxisValue);

            final float linearAdjustedAltiDiff = (float) (distanceScale * _altiDiff);
            final float newAltitude = yValue + linearAdjustedAltiDiff;

            float splineAlti = 0;
            try {

               splineAlti = (float) cubicSpline.interpolate(distance);

            } catch (final IllegalArgumentException e) {

               System.out.println(e.getMessage());
            }

            final float adjustedAlti = newAltitude + splineAlti;

            splineDataSerie[serieIndex] = splineAlti;

            adjustedAltiSerie[serieIndex] = adjustedAlti / UI.UNIT_VALUE_ALTITUDE;
            diffTo2ndAlti[serieIndex] = srtmValue - adjustedAlti;

         } else {

            // set altitude which is not adjusted

            adjustedAltiSerie[serieIndex] = yValue / UI.UNIT_VALUE_ALTITUDE;
            diffTo2ndAlti[serieIndex] = srtmValue - yValue;
         }
      }

      /*
       * compute position of the spline points within the graph
       */
      final double[] spRelativePositionX = _splineData.relativePositionX;
      final int pointLength = spRelativePositionX.length;
      _splineData.serieIndex = new int[pointLength];

      for (int pointIndex = 0; pointIndex < pointLength; pointIndex++) {

         final double pointAbsolutePosX = _sliderXAxisValue * spRelativePositionX[pointIndex];

         boolean isSet = false;

         for (int serieIndex = 0; serieIndex < xDataSerie.length; serieIndex++) {

            final double graphX = xDataSerie[serieIndex];

            if (graphX >= pointAbsolutePosX) {
               isSet = true;
               _splineData.serieIndex[pointIndex] = serieIndex;
               break;
            }
         }

         if (isSet == false && pointAbsolutePosX > xDataSerie[xDataSerie.length - 1]) {
            // set point for the last graph value
            _splineData.serieIndex[pointIndex] = xDataSerie.length - 1;
         }
      }
   }

   /**
    * Adjust start and max at the same time
    * <p>
    * it took me several days to figure out this algorithim, 10.4.2007 Wolfgang
    */
   private void computeAltitudeStartAndMax(final float[] altiSrc,
                                           final float[] altiDest,
                                           final float newAltiStart,
                                           final float newAltiMax) {

      /*
       * adjust max
       */
      _altiStartDiff = _altiStartDiff - (_prevAltiStart - newAltiStart);
      _altiMaxDiff = _altiMaxDiff - (_prevAltiMax - newAltiMax);

      final float oldStart = altiSrc[0];
      computeAltitudeMax(altiSrc, altiDest, _initialAltiMax + _altiMaxDiff);
      final float newStart = altiDest[0];

      /*
       * adjust start
       */
      float startDiff;
      if (_rdoKeepStart.getSelection()) {
         startDiff = 0;
      } else {
         startDiff = newStart - oldStart;
      }
      computeAltitudeEvenly(altiDest, altiDest, _initialAltiStart + _altiStartDiff + startDiff);
   }

   private void computeDeletedPoint() {

      if (_splineData.isPointMovable.length <= 3) {
         // prevent deleting less than 3 points
         return;
      }

      final boolean[] oldIsPointMovable = _splineData.isPointMovable;
      final double[] oldPosX = _splineData.relativePositionX;
      final double[] oldPosY = _splineData.relativePositionY;
      final double[] oldXValues = _splineData.graphXValues;
      final double[] oldYValues = _splineData.graphYValues;
      final double[] oldXMinValues = _splineData.graphXMinValues;
      final double[] oldXMaxValues = _splineData.graphXMaxValues;

      final int newLength = oldIsPointMovable.length - 1;

      final boolean[] newIsPointMovable = _splineData.isPointMovable = new boolean[newLength];
      final double[] newPosX = _splineData.relativePositionX = new double[newLength];
      final double[] newPosY = _splineData.relativePositionY = new double[newLength];
      final double[] newXValues = _splineData.graphXValues = new double[newLength];
      final double[] newYValues = _splineData.graphYValues = new double[newLength];
      final double[] newXMinValues = _splineData.graphXMinValues = new double[newLength];
      final double[] newXMaxValues = _splineData.graphXMaxValues = new double[newLength];

      int srcPos, destPos, length;

      if (_pointHitIndex == 0) {

         // remove first point

         srcPos = 1;
         destPos = 0;
         length = newLength;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

      } else if (_pointHitIndex == newLength) {

         // remove last point

         srcPos = 0;
         destPos = 0;
         length = newLength;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

      } else {

         // remove points in the middle

         srcPos = 0;
         destPos = 0;
         length = _pointHitIndex;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

         srcPos = _pointHitIndex + 1;
         destPos = _pointHitIndex;
         length = newLength - _pointHitIndex;

         System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
         System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
         System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

         System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
         System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
         System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
         System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);
      }
   }

   private boolean computeNewPoint(final int mouseDownDevPositionX,
                                   final int mouseDownDevPositionY,
                                   final int numberOfPoints) {

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();

      final double scaleX = drawingData.scaleX;
      final double scaleY = drawingData.scaleY;

      float devX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
      final float devY = drawingData.devY0Spline - mouseDownDevPositionY;

      final double graphXMin = 0;
      final double graphXMax = _sliderXAxisValue;

      float graphX = (float) (devX / scaleX);

      // check min/max value
      if (graphX <= graphXMin || graphX >= graphXMax) {
         // click is outside of the allowed area
         return false;
      }

      /*
       * add the new point at the end of the existing points, CubicSpline will resort them
       */
      final boolean[] oldIsPointMovable = _splineData.isPointMovable;
      final double[] oldPosX = _splineData.relativePositionX;
      final double[] oldPosY = _splineData.relativePositionY;
      final double[] oldXValues = _splineData.graphXValues;
      final double[] oldYValues = _splineData.graphYValues;
      final double[] oldXMinValues = _splineData.graphXMinValues;
      final double[] oldXMaxValues = _splineData.graphXMaxValues;

      final int newLength = oldXValues.length + numberOfPoints;
      final boolean[] newIsPointMovable = _splineData.isPointMovable = new boolean[newLength];
      final double[] newPosX = _splineData.relativePositionX = new double[newLength];
      final double[] newPosY = _splineData.relativePositionY = new double[newLength];
      final double[] newXValues = _splineData.graphXValues = new double[newLength];
      final double[] newYValues = _splineData.graphYValues = new double[newLength];
      final double[] newXMinValues = _splineData.graphXMinValues = new double[newLength];
      final double[] newXMaxValues = _splineData.graphXMaxValues = new double[newLength];

      final int oldLength = oldXValues.length;

      // copy old values into new arrays
      System.arraycopy(oldIsPointMovable, 0, newIsPointMovable, 0, oldLength);
      System.arraycopy(oldPosX, 0, newPosX, 0, oldLength);
      System.arraycopy(oldPosY, 0, newPosY, 0, oldLength);

      System.arraycopy(oldXValues, 0, newXValues, 0, oldLength);
      System.arraycopy(oldYValues, 0, newYValues, 0, oldLength);
      System.arraycopy(oldXMinValues, 0, newXMinValues, 0, oldLength);
      System.arraycopy(oldXMaxValues, 0, newXMaxValues, 0, oldLength);

      final float dev1X = (float) (graphXMax * scaleX);
      final float dev1Y = (float) (_altiDiff * scaleY);

      /*
       * creat a new points
       */
      if (numberOfPoints == 1) {

         final float posX = dev1X == 0 ? 0 : devX / dev1X;
         final float posY = dev1Y == 0 ? 0 : devY / dev1Y;

         final int lastIndex = newLength - 1;
         newIsPointMovable[lastIndex] = true;
         newPosX[lastIndex] = posX;
         newPosY[lastIndex] = posY;
         newXValues[lastIndex] = graphX;
         newYValues[lastIndex] = 0;
         newXMinValues[lastIndex] = graphXMin;
         newXMaxValues[lastIndex] = graphXMax;

      } else {

         for (int pointIndex = 0; pointIndex < numberOfPoints; pointIndex++) {

            final float posX = (1f / (numberOfPoints + 1)) * (pointIndex + 1);
            final float posY = dev1Y == 0 ? 0 : devY / dev1Y;

            devX = dev1X / (pointIndex + 1);
            graphX = (float) (devX / scaleX);

            final int splineIndex = oldLength + pointIndex;
            newIsPointMovable[splineIndex] = true;
            newPosX[splineIndex] = posX;
            newPosY[splineIndex] = posY;
            newXValues[splineIndex] = graphX;
            newYValues[splineIndex] = 0;
            newXMinValues[splineIndex] = graphXMin;
            newXMaxValues[splineIndex] = graphXMax;
         }
      }

      // don't move the point immediately
      _pointHitIndex = -1;

      return true;
   }

   /**
    * Compute relative position of the moved point
    *
    * @param mouseEvent
    */
   private void computePointMoveValues(final ChartMouseEvent mouseEvent) {

      if (_pointHitIndex == -1) {
         return;
      }

      final boolean isPointMovable = _splineData.isPointMovable[_pointHitIndex];

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();
      final double scaleX = drawingData.scaleX;
      final double scaleY = drawingData.scaleY;

      double devX = drawingData.devGraphValueXOffset + mouseEvent.devXMouse;
      final float devY = drawingData.devY0Spline - mouseEvent.devYMouse;

      final double graphXMin = _splineData.graphXMinValues[_pointHitIndex];
      final double graphXMax = _splineData.graphXMaxValues[_pointHitIndex];

      double graphX = (devX / scaleX);

      _canDeletePoint = false;

      if (isPointMovable) {

         // point can be moved horizontal and vertical

         // check min value
         if (Double.isNaN(graphXMin) == false) {
            if (graphX < graphXMin) {
               graphX = graphXMin;
               _canDeletePoint = true;
            }
         }
         // check max value
         if (Double.isNaN(graphXMax) == false) {
            if (graphX > graphXMax) {
               graphX = graphXMax;
               _canDeletePoint = true;
            }
         }
      }

      /*
       * set new relative position
       */
      devX = (graphX * scaleX);

      final double graph1X = _sliderXAxisValue;
      final float graph1Y = _altiDiff;

      final double dev1X = scaleX * graph1X;
      final float dev1Y = (float) (scaleY * graph1Y);

      if (isPointMovable) {
         // horizontal move is allowed
         _splineData.relativePositionX[_pointHitIndex] = devX / dev1X;
      }

      // set vertical position
      final float devYRelativ = devY / dev1Y;
      _splineData.relativePositionY[_pointHitIndex] = devYRelativ;

// this is not easy to implement, current solution do NOT work
//
//      /*
//       * sync start & end
//       */
//      final int lastIndex = _splineData.graphXMaxValues.length - 1;
//      final boolean isSync = (_pointHitIndex == 0 || _pointHitIndex == lastIndex)
//            && _chkSRTMSyncStartEnd.getSelection();
//
//      if (isSync) {
//         if (_pointHitIndex == 0) {
//            _splineData.relativePositionY[lastIndex] = devYRelativ;
//         } else {
//            _splineData.relativePositionY[0] = devYRelativ;
//         }
//      }
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.adjust_altitude_dlg_shell_title);
   }

   @Override
   public void create() {

      createDataBackup();

      // create UI widgets
      super.create();

      restoreState();

      setTitle(Messages.adjust_altitude_dlg_dialog_title);
      setMessage(NLS.bind(Messages.adjust_altitude_dlg_dialog_message, TourManager.getTourTitle(_tourData)));

      updateTourChart();

      restoreState_PostChartUpdate();
   }

   @Override
   public ChartLayer2ndAltiSerie create2ndAltiLayer() {

      final double[] xDataSerie = _tourChartConfig.isShowTimeOnXAxis ? //
            _tourData.getTimeSerieDouble()
            : _tourData.getDistanceSerieDouble();

      _chartLayer2ndAltiSerie = new ChartLayer2ndAltiSerie(_tourData, xDataSerie, _tourChartConfig, _splineData);

      return _chartLayer2ndAltiSerie;
   }

   /**
    * Create altitude spinner field
    *
    * @param startContainer
    * @return Returns the field
    */
   private Spinner createAltiField(final Composite startContainer) {

      final Spinner spinner = new Spinner(startContainer, SWT.BORDER);
      spinner.setMinimum(0);
      spinner.setMaximum(99999);
      spinner.setIncrement(1);
      spinner.setPageIncrement(1);
      UI.setWidth(spinner, convertWidthInCharsToPixels(6));

      spinner.addModifyListener(new ModifyListener() {

         @Override
         public void modifyText(final ModifyEvent e) {

            if (_isDisableModifyListener) {
               return;
            }

            final Spinner spinner = (Spinner) e.widget;

            if (UI.UNIT_VALUE_ALTITUDE == 1) {

               final float modifiedAlti = spinner.getSelection();

               spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, modifiedAlti);

            } else {

               /**
                * adjust the non metric (imperial) value, this seems to be complicate and it is
                * <p>
                * the altitude data are always saved in the database with the metric system therefor
                * the altitude must always match to the metric system, changing the altitude in the
                * imperial system has always 3 or 4 value differences from one meter to the next
                * meter
                * <p>
                * after many hours of investigation this seems to work
                */

               final float modifiedAlti = spinner.getSelection();
               final float metricAlti = (Float) spinner.getData(WIDGET_DATA_METRIC_ALTITUDE);

               final float oldAlti = metricAlti / UI.UNIT_VALUE_ALTITUDE;
               float newMetricAlti = modifiedAlti * UI.UNIT_VALUE_ALTITUDE;

               if (modifiedAlti > oldAlti) {
                  newMetricAlti++;
               }

               spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, newMetricAlti);
            }

            onChangeAltitude();
         }
      });

      spinner.addMouseWheelListener(new MouseWheelListener() {

         @Override
         public void mouseScrolled(final MouseEvent e) {

            if (_isDisableModifyListener) {
               return;
            }

            final Spinner spinner = (Spinner) e.widget;

            int accelerator = (e.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
            accelerator *= (e.stateMask & SWT.SHIFT) != 0 ? 5 : 1;
            accelerator *= e.count > 0 ? 1 : -1;

            float metricAltitude = (Float) e.widget.getData(WIDGET_DATA_METRIC_ALTITUDE);
            metricAltitude = metricAltitude + accelerator;

            _isDisableModifyListener = true;
            {
               spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(metricAltitude));
               spinner.setSelection((int) (metricAltitude / UI.UNIT_VALUE_ALTITUDE));
            }
            _isDisableModifyListener = false;

            onChangeAltitude();
         }
      });

      spinner.addFocusListener(new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {}

         @Override
         public void focusLost(final FocusEvent e) {
            onChangeAltitude();
         }
      });

      return spinner;
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // rename OK button
      final Button buttonOK = getButton(IDialogConstants.OK_ID);

      if (_isSaveTour) {
         buttonOK.setText(Messages.adjust_altitude_btn_save_modified_tour);
      } else {
         buttonOK.setText(Messages.adjust_altitude_btn_update_modified_tour);
      }

      setButtonLayoutData(buttonOK);
   }

   private void createDataBackup() {

      float[] altitudeSerie;

      if (_isCreateDummyAltitude) {

         altitudeSerie = new float[_tourData.timeSerie.length];

         for (int index = 0; index < altitudeSerie.length; index++) {
            /*
             * it's better to set a value instead of having 0, but the value should not be too high,
             * I had this idea on 28.08.2010 -> 88
             */
            altitudeSerie[index] = 88;
         }

         // altitude must be set because it's used
         _tourData.altitudeSerie = altitudeSerie;

      } else {
         altitudeSerie = _tourData.altitudeSerie;
      }

      /*
       * keep a backup of the altitude data because these data will be changed in this dialog
       */
      _backupMetricAltitudeSerie = Util.createFloatCopy(altitudeSerie);

      final float[][] srtmValues = _tourData.getSRTMValues();
      if (srtmValues != null) {
         _backupSrtmSerie = srtmValues[0];
         _backupSrtmSerieImperial = srtmValues[1];
      }
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgArea = (Composite) super.createDialogArea(parent);

      createUI(dlgArea);

      initializeSplineData();
      initializeAltitude(_backupMetricAltitudeSerie);

      return dlgArea;
   }

   private void createUI(final Composite parent) {

      _dlgContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_dlgContainer);
      GridLayoutFactory.fillDefaults().margins(9, 0).applyTo(_dlgContainer);
      {
         createUI_10_AdjustmentType(_dlgContainer);
         createUI_20_TourChart(_dlgContainer);
         createUI_30_Options(_dlgContainer);
      }

      parent.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            // with the new e4 toolbar update the chart has it's default size (pack() is used) -> resize to window size
//            parent.layout(true, true);
         }
      });
   }

   private void createUI_10_AdjustmentType(final Composite parent) {

      final Composite typeContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(typeContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 5, 0).applyTo(typeContainer);
      {
         final Label label = new Label(typeContainer, SWT.NONE);
         label.setText(Messages.adjust_altitude_label_adjustment_type);

         // combo: adjust type
         _comboAdjustmentType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboAdjustmentType.setVisibleItemCount(20);
         _comboAdjustmentType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectAdjustmentType();
            }
         });
      }

      // fill combo
      for (final AdjustmentType adjustType : ALL_ADJUSTMENT_TYPES) {

         if (_backupSrtmSerie == null && (//
         adjustType.__id == ADJUST_TYPE_SRTM_SPLINE //
               || adjustType.__id == ADJUST_TYPE_SRTM //
               || adjustType.__id == ADJUST_TYPE_HORIZONTAL_GEO_POSITION
         //
         )) {

            // skip types which require srtm data
            continue;
         }

         _availableAdjustmentTypes.add(adjustType);

         _comboAdjustmentType.add(adjustType.__visibleName);
      }
   }

   private void createUI_20_TourChart(final Composite parent) {

      _tourChart = new TourChart(parent, SWT.BORDER, null, _state);

      GridDataFactory.fillDefaults()
            .grab(true, true)
            .indent(0, 0)
            .minSize(600, 400)
            .applyTo(_tourChart);

      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setNoToolbarPack();

      _tourChart.setContextProvider(new DialogAdjustAltitudeChartContextProvider(this), true);

      _tourChart.addDataModelListener(new IDataModelListener() {
         @Override
         public void dataModelChanged(final ChartDataModel changedChartDataModel) {
            // set title
            changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
         }
      });

      _tourChart.addSliderMoveListener(new ISliderMoveListener() {
         @Override
         public void sliderMoved(final SelectionChartInfo chartInfo) {

            if (_isSliderEventDisabled) {
               return;
            }

            onSelectAdjustmentType();
         }
      });

      _tourChart.addChartMouseListener(new MouseAdapter() {

         @Override
         public void mouseDown(final ChartMouseEvent event) {
            onMouseDown(event);
         }

         @Override
         public void mouseMove(final ChartMouseEvent event) {
            onMouseMove(event);
         }

         @Override
         public void mouseUp(final ChartMouseEvent event) {
            onMouseUp(event);
         }

      });

      _tourChart.addXAxisSelectionListener(new IXAxisSelectionListener() {
         @Override
         public void selectionChanged(final boolean showTimeOnXAxis) {
            if (isAdjustmentType_SRTM_SPline()) {
               computeAltitudeSRTMSpline();
            }
         }
      });

      /*
       * create chart configuration
       */
      _tourChartConfig = new TourChartConfiguration(true);

      // set altitude visible
      _tourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

      // show srtm values
      _tourChartConfig.isSRTMDataVisible = true;

      // overwrite x-axis from pref store
      _tourChartConfig.setIsShowTimeOnXAxis(
            _prefStore
                  .getString(
                        ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT)
                  .equals(TourManager.X_AXIS_TIME));
   }

   /**
    * create options for each adjustment type in a pagebook
    */
   private void createUI_30_Options(final Composite parent) {

      _pageBookOptions = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageBookOptions);
      {
         _pageEmpty = new Label(_pageBookOptions, SWT.NONE);

         _pageOption_SRTM = createUI_40_Option_WithSRTM(_pageBookOptions);
         _pageOption_SRTMSpline = createUI_50_Option_WithSRTMSpline(_pageBookOptions);
         _pageOption_NoSRTM = createUI_60_Option_WithoutSRTM(_pageBookOptions);
         _pageOption_GeoPosition = createUI_70_Option_GeoPosition(_pageBookOptions);
      }
   }

   private Composite createUI_40_Option_WithSRTM(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * button: update altitude
          */
         final Button btnUpdateAltitude = new Button(container, SWT.NONE);
         btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         btnUpdateAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onUpdateAltitudeSRTM();
            }
         });
         setButtonLayoutData(btnUpdateAltitude);

         /*
          * button: reset altitude
          */
         final Button btnResetAltitude = new Button(container, SWT.NONE);
         btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude);
         btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_tooltip);
         btnResetAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onResetAltitudeSRTM();
            }
         });
         setButtonLayoutData(btnResetAltitude);
      }

      return container;
   }

   private Composite createUI_50_Option_WithSRTMSpline(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_52_SRTMOptions(container);
         createUI_54_SRTMActions(container);
      }

      return container;
   }

   private void createUI_52_SRTMOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Link: Select whole tour
          */
         _linkSRTMSelectWholeTour = new Link(container, SWT.NONE);
         _linkSRTMSelectWholeTour.setText(Messages.Dialog_AdjustAltitude_Link_ApproachWholeTour);
         _linkSRTMSelectWholeTour.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onModifySRTMSelection();
            }
         });
         GridDataFactory.swtDefaults().applyTo(_linkSRTMSelectWholeTour);
      }
   }

   private Composite createUI_54_SRTMActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * button: update altitude
          */
         final Button btnUpdateAltitude = new Button(container, SWT.NONE);
         btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         btnUpdateAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onUpdateAltitudeSRTMSpline();
            }
         });
         setButtonLayoutData(btnUpdateAltitude);

         /*
          * button: reset altitude
          */
         final Button btnResetAltitude = new Button(container, SWT.NONE);
         btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude_and_points);
         btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_and_points_tooltip);
         btnResetAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onResetAltitudeSRTMSpline();
            }
         });
         setButtonLayoutData(btnResetAltitude);

         /*
          * button: remove all points
          */
         _btnSRTMRemoveAllPoints = new Button(container, SWT.NONE);
         _btnSRTMRemoveAllPoints.setText(Messages.adjust_altitude_btn_srtm_remove_all_points);
         _btnSRTMRemoveAllPoints.setToolTipText(Messages.adjust_altitude_btn_srtm_remove_all_points_tooltip);
         _btnSRTMRemoveAllPoints.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {

               initializeSplineData();
               onSelectAdjustmentType();
            }
         });
         setButtonLayoutData(_btnSRTMRemoveAllPoints);
      }
      return container;
   }

   private Composite createUI_60_Option_WithoutSRTM(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         createUI_61_StartEnd(container);
         createUI_62_Max(container);
         createUI_63_Actions(container);
      }

      return container;
   }

   private void createUI_61_StartEnd(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * field: start altitude
          */
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_start_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

         _spinnerNewStartAlti = createAltiField(container);
         _spinnerNewStartAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_START));
         _spinnerNewStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

         _lblOldStartAlti = new Label(container, SWT.NONE);
         _lblOldStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

         /*
          * field: end altitude
          */
         label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_end_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

         _spinnerNewEndAlti = createAltiField(container);
         _spinnerNewEndAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_END));
         _spinnerNewEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

         _lblOldEndAlti = new Label(container, SWT.NONE);
         _lblOldEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);
      }

   }

   private void createUI_62_Max(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .indent(40, 0)
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         /*
          * field: max altitude
          */
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_AdjustAltitude_Label_max_altitude);
         label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

         _spinnerNewMaxAlti = createAltiField(container);
         _spinnerNewMaxAlti.setData(WIDGET_DATA_ALTI_ID, Float.valueOf(ALTI_ID_MAX));
         _spinnerNewMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

         _lblOldMaxAlti = new Label(container, SWT.NONE);
         _lblOldMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

         /*
          * group: keep start/bottom
          */
         final Group groupKeep = new Group(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(3, 1).applyTo(groupKeep);
         GridLayoutFactory.swtDefaults().applyTo(groupKeep);
         groupKeep.setText(Messages.Dlg_AdjustAltitude_Group_options);
         {
            final SelectionAdapter keepButtonSelectionAdapter = new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onChangeAltitude();
               }
            };

            _rdoKeepBottom = new Button(groupKeep, SWT.RADIO);
            _rdoKeepBottom.setText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude);
            _rdoKeepBottom.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip);
            _rdoKeepBottom.setLayoutData(new GridData());
            _rdoKeepBottom.addSelectionListener(keepButtonSelectionAdapter);
            // fRadioKeepBottom.setSelection(true);

            _rdoKeepStart = new Button(groupKeep, SWT.RADIO);
            _rdoKeepStart.setText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude);
            _rdoKeepStart.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip);
            _rdoKeepStart.setLayoutData(new GridData());
            _rdoKeepStart.addSelectionListener(keepButtonSelectionAdapter);
         }
      }

   }

   private void createUI_63_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(20, 0).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * button: reset altitude
          */
         _btnResetAltitude = new Button(container, SWT.NONE);
         _btnResetAltitude.setText(Messages.adjust_altitude_btn_reset_altitude);
         _btnResetAltitude.setToolTipText(Messages.adjust_altitude_btn_reset_altitude_tooltip);
         _btnResetAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onResetAltitude();
            }
         });
         setButtonLayoutData(_btnResetAltitude);

         /*
          * button: update altitude
          */
         _btnUpdateAltitude = new Button(container, SWT.NONE);
         _btnUpdateAltitude.setText(Messages.adjust_altitude_btn_update_altitude);
         _btnUpdateAltitude.setToolTipText(Messages.adjust_altitude_btn_update_altitude_tooltip);
         _btnUpdateAltitude.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onUpdateAltitude();
            }
         });
         setButtonLayoutData(_btnUpdateAltitude);
      }
   }

   private Composite createUI_70_Option_GeoPosition(final PageBook parent) {

      final PixelConverter pc = new PixelConverter(parent);
      final int valueWidth = pc.convertWidthInCharsToPixels(4);
      Label label;

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.Adjust_Altitude_Group_GeoPosition);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: adjusted slices
          */
         label = new Label(group, SWT.NONE);
         label.setText(Messages.Adjust_Altitude_Label_GeoPosition_Slices);

         /*
          * label: slice value
          */
         _lblSliceValue = new Label(group, SWT.TRAIL);
         GridDataFactory
               .fillDefaults()
               .align(SWT.END, SWT.CENTER)
               .hint(valueWidth, SWT.DEFAULT)
               .applyTo(_lblSliceValue);

         /*
          * scale: slice position
          */
         _scaleSlicePos = new Scale(group, SWT.HORIZONTAL);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleSlicePos);
         _scaleSlicePos.setMinimum(0);
         _scaleSlicePos.setMaximum(MAX_ADJUST_GEO_POS_SLICES * 2);
         _scaleSlicePos.setPageIncrement(5);
         _scaleSlicePos.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectSlicePosition();
            }
         });
         _scaleSlicePos.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(final Event event) {
               onDoubleClickGeoPos(event.widget);
            }
         });
      }

      return group;
   }

   private void enableFieldsWithoutSRTM() {

      // set adjustment type and enable the field(s) which can be modified
      switch (getSelectedAdjustmentType().__id) {

      case ADJUST_TYPE_START_AND_END:

         _spinnerNewStartAlti.setEnabled(true);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(true);
         _rdoKeepStart.setEnabled(true);
         _rdoKeepBottom.setEnabled(true);

         break;

      case ADJUST_TYPE_WHOLE_TOUR:

         _spinnerNewStartAlti.setEnabled(true);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(false);
         _rdoKeepStart.setEnabled(false);
         _rdoKeepBottom.setEnabled(false);

         break;

      case ADJUST_TYPE_END:

         _spinnerNewStartAlti.setEnabled(false);
         _spinnerNewEndAlti.setEnabled(true);
         _spinnerNewMaxAlti.setEnabled(false);
         _rdoKeepStart.setEnabled(false);
         _rdoKeepBottom.setEnabled(false);

         break;

      case ADJUST_TYPE_MAX_HEIGHT:

         _spinnerNewStartAlti.setEnabled(false);
         _spinnerNewEndAlti.setEnabled(false);
         _spinnerNewMaxAlti.setEnabled(true);
         _rdoKeepStart.setEnabled(true);
         _rdoKeepBottom.setEnabled(true);

         break;

      default:
         break;
      }
   }

   private void enableFieldsWithSRTM() {

      /*
       * srtm options
       */
      if (_splineData != null && _splineData.isPointMovable != null) {
         _btnSRTMRemoveAllPoints.setEnabled(_splineData.isPointMovable.length > 3);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
   }

   @Override
   protected Point getInitialSize() {
      final Point calculatedSize = super.getInitialSize();
      if (calculatedSize.x < 600) {
         calculatedSize.x = 600;
      }
      if (calculatedSize.y < 600) {
         calculatedSize.y = 600;
      }
      return calculatedSize;
   }

   /**
    * @return the adjustment type which is selected in the combox
    */
   private AdjustmentType getSelectedAdjustmentType() {

      int comboIndex = _comboAdjustmentType.getSelectionIndex();

      if (comboIndex == -1) {
         comboIndex = 0;
         _comboAdjustmentType.select(comboIndex);
      }

      return _availableAdjustmentTypes.get(comboIndex);
   }

   /**
    * reset altitudes to it's original values
    *
    * @param metricAltitudeSerie
    */
   private void initializeAltitude(final float[] metricAltitudeSerie) {

      final int serieLength = metricAltitudeSerie.length;

      final float startAlti = metricAltitudeSerie[0];
      final float endAlti = metricAltitudeSerie[serieLength - 1];
      float maxAlti = startAlti;

      /*
       * get altitude from original data, calculate max altitude
       */
      _metricAdjustedAltitudeWithoutSRTM = new float[serieLength];

      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float altitude = metricAltitudeSerie[serieIndex];
         _metricAdjustedAltitudeWithoutSRTM[serieIndex] = altitude;

         if (altitude > maxAlti) {
            maxAlti = altitude;
         }
      }

      /*
       * update UI
       */
      _lblOldStartAlti.setText(Integer.toString((int) (startAlti / UI.UNIT_VALUE_ALTITUDE)));
      _lblOldEndAlti.setText(Integer.toString((int) (endAlti / UI.UNIT_VALUE_ALTITUDE)));
      _lblOldMaxAlti.setText(Integer.toString((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE)));

      _lblOldStartAlti.pack(true);
      _lblOldEndAlti.pack(true);
      _lblOldMaxAlti.pack(true);

      _isDisableModifyListener = true;
      {
         _spinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(startAlti));
         _spinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));

         _spinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(endAlti));
         _spinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));

         _spinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(maxAlti));
         _spinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
      }
      _isDisableModifyListener = false;

   }

   /**
    * create spline values, these are 3 points at start/middle/end
    *
    * @param altiDiff
    * @param sliderDistance
    * @return
    */
   private void initializeSplineData() {

      _splineData = new SplineData();

      final float borderValueLeft = -0.0000000000001f;
      final float borderValueRight = 1.0000000000001f;

      final int pointLength = 3;

      final boolean[] isMovable = _splineData.isPointMovable = new boolean[pointLength];
      isMovable[0] = false;
      isMovable[1] = true;
      isMovable[2] = false;

      final double[] posX = _splineData.relativePositionX = new double[pointLength];
      final double[] posY = _splineData.relativePositionY = new double[pointLength];

      posX[0] = borderValueLeft;
      posX[1] = 0.5f;
      posX[2] = borderValueRight;

      posY[0] = 0;
      posY[1] = 0;
      posY[2] = 0;

      final double[] splineMinX = _splineData.graphXMinValues = new double[pointLength];
      final double[] splineMaxX = _splineData.graphXMaxValues = new double[pointLength];
      splineMinX[0] = borderValueLeft;
      splineMaxX[0] = borderValueLeft;
      splineMinX[1] = 0;
      splineMaxX[1] = 0;
      splineMinX[2] = borderValueRight;
      splineMaxX[2] = borderValueRight;

      _splineData.graphXValues = new double[pointLength];
      _splineData.graphYValues = new double[pointLength];
   }

   boolean isActionEnabledCreateSplinePoint(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

      final SplineDrawingData drawingData = _chartLayer2ndAltiSerie.getDrawingData();

      final double scaleX = drawingData.scaleX;
      final float devX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
      final float graphX = (float) (devX / scaleX);

      final float graphXMin = 0;
      final double graphXMax = _sliderXAxisValue;

      // check min/max value
      if (graphX <= graphXMin || graphX >= graphXMax) {
         // click is outside of the allowed area
         return false;
      } else {
         return true;
      }
   }

   private boolean isAdjustmentType_SRTM_SPline() {
      return getSelectedAdjustmentType().__id == ADJUST_TYPE_SRTM_SPLINE;
   }

   @Override
   protected void okPressed() {

      saveTour();

      super.okPressed();
   }

   private void onChangeAltitude() {

      // calcuate new altitude values
      computeAltitude_WithoutSRTM();

      enableFieldsWithoutSRTM();

      // set new values into the fields which can change the altitude
      updateUIAltiFields();

      updateUI2ndLayer();
   }

   private void onDoubleClickGeoPos(final Widget widget) {

      final Scale scale = (Scale) widget;
      final int max = scale.getMaximum();

      scale.setSelection(max / 2);

      onSelectSlicePosition();
   }

   private void onModifySRTMSelection() {

      final int maxIndex = _tourData.getTimeSerieDouble().length - 1;

      /*
       * set slider position, BOTH sliders must be set to the right side otherwise the left
       * slider is not moved because of slider optimization
       */
      _tourChart.setXSliderPosition(
            new SelectionChartXSliderPosition(
                  _tourChart, //
                  maxIndex,
                  maxIndex));

      _tourChart.getDisplay().timerExec(100, new Runnable() {
         @Override
         public void run() {
            updateTourChart();
         }
      });
   }

   private void onMouseDown(final ChartMouseEvent mouseEvent) {

      if (_chartLayer2ndAltiSerie == null) {
         return;
      }

      final Rectangle[] pointHitRectangles = _chartLayer2ndAltiSerie.getPointHitRectangels();
      if (pointHitRectangles == null) {
         return;
      }

      _pointHitIndex = -1;
//      final boolean[] isPointMovable = fSplineData.isPointMovable;

      // check if the mouse hits a spline point
      for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

//         if (isPointMovable[pointIndex] == false) {
//            // ignore none movable points
//            continue;
//         }

         if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

            _pointHitIndex = pointIndex;

            mouseEvent.isWorked = true;
            return;
         }
      }
   }

   private void onMouseMove(final ChartMouseEvent mouseEvent) {

      if (_chartLayer2ndAltiSerie == null) {
         return;
      }

      final Rectangle[] pointHitRectangles = _chartLayer2ndAltiSerie.getPointHitRectangels();
      if (pointHitRectangles == null) {
         return;
      }

      if (_pointHitIndex != -1) {

         // point is moved

         computePointMoveValues(mouseEvent);

         onSelectAdjustmentType();

         mouseEvent.isWorked = true;

      } else {

         // point is not moved, check if the mouse hits a spline point

         for (final Rectangle pointHitRectangle : pointHitRectangles) {

            if (pointHitRectangle.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {
               mouseEvent.isWorked = true;
               break;
            }
         }
      }

      if (mouseEvent.isWorked) {

         // show dragged cursor

         mouseEvent.cursor = ChartCursor.Dragged;
      }
   }

   private void onMouseUp(final ChartMouseEvent mouseEvent) {

      if (_pointHitIndex == -1) {
         return;
      }

      if (_canDeletePoint) {

         _canDeletePoint = false;

         computeDeletedPoint();

         // redraw layer to update the hit rectangles
         onSelectAdjustmentType();
      }

      mouseEvent.isWorked = true;
      _pointHitIndex = -1;
   }

   /**
    * display altitude with the original altitude data
    */
   private void onResetAltitude() {

      _altiMaxDiff = 0;
      _altiStartDiff = 0;
      _prevAltiStart = 0;
      _prevAltiMax = 0;

      _tourData.altitudeSerie = Util.createFloatCopy(_backupMetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      initializeAltitude(_backupMetricAltitudeSerie);
      onChangeAltitude();

      updateTourChart();
   }

   private void onResetAltitudeSRTM() {

      _tourData.altitudeSerie = Util.createFloatCopy(_backupMetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      computeAltitudeSRTM();

      updateTourChart();
   }

   private void onResetAltitudeSRTMSpline() {

      _tourData.altitudeSerie = Util.createFloatCopy(_backupMetricAltitudeSerie);
      _tourData.clearAltitudeSeries();

      /*
       * set all points to y=0
       */
      final double[] posY = _splineData.relativePositionY;
      for (int pointIndex = 0; pointIndex < posY.length; pointIndex++) {
         posY[pointIndex] = 0;
      }

      computeAltitudeSRTMSpline();

      updateTourChart();
   }

   private void onSelectAdjustmentType() {

      // hide all 2nd data series
      _tourData.dataSerieAdjustedAlti = null;
      _tourData.dataSerieDiffTo2ndAlti = null;
      _tourData.dataSerie2ndAlti = null;
      _tourData.dataSerieSpline = null;
      _tourData.setSRTMValues(_backupSrtmSerie, _backupSrtmSerieImperial);

      // hide splines
      _tourData.splineDataPoints = null;
      _splineData.serieIndex = null;

      final int adjustmentType = getSelectedAdjustmentType().__id;
      switch (adjustmentType) {
      case ADJUST_TYPE_SRTM:

         _pageBookOptions.showPage(_pageOption_SRTM);

         computeAltitudeSRTM();

         break;

      case ADJUST_TYPE_HORIZONTAL_GEO_POSITION:

         _pageBookOptions.showPage(_pageOption_GeoPosition);

         onSelectSlicePosition();

         break;

      case ADJUST_TYPE_SRTM_SPLINE:

         _pageBookOptions.showPage(_pageOption_SRTMSpline);

         // display splines
         _tourData.splineDataPoints = _splineData;
         computeAltitudeSRTMSpline();

         break;

      case ADJUST_TYPE_WHOLE_TOUR:
      case ADJUST_TYPE_START_AND_END:
      case ADJUST_TYPE_END:
      case ADJUST_TYPE_MAX_HEIGHT:

         _pageBookOptions.showPage(_pageOption_NoSRTM);
         onResetAltitude();

         break;

      default:
         _pageBookOptions.showPage(_pageEmpty);
         break;
      }

      /*
       * layout is a performance hog, optimize it
       */
      if (_oldAdjustmentType != adjustmentType) {
         _dlgContainer.layout(true);
      }

      _oldAdjustmentType = adjustmentType;

      updateUI2ndLayer();
   }

   private void onSelectSlicePosition() {

      int diffGeoSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;
      final int serieLength = _tourData.timeSerie.length;

      // adjust slices to bounds
      if (diffGeoSlices > serieLength) {
         diffGeoSlices = serieLength - 1;
      } else if (-diffGeoSlices > serieLength) {
         diffGeoSlices = -(serieLength - 1);
      }

      /*
       * adjust srtm data
       */
      final float[] adjustedSRTM = new float[serieLength];
      final float[] adjustedSRTMImperial = new float[serieLength];

      final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
      final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
      final int adjustedLength = serieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

      System.arraycopy(_backupSrtmSerie, srcPos, adjustedSRTM, destPos, adjustedLength);
      System.arraycopy(_backupSrtmSerieImperial, srcPos, adjustedSRTMImperial, destPos, adjustedLength);

      _tourData.setSRTMValues(adjustedSRTM, adjustedSRTMImperial);

      final float[] metricAltiSerie = _tourData.altitudeSerie;
      final float[] diffTo2ndAlti = _tourData.dataSerieDiffTo2ndAlti = new float[serieLength];

      // get altitude diff serie
      for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

         final float srtmAltitude = adjustedSRTM[serieIndex];

         // ignore diffs which are outside of the adjusted srtm
         if ((diffGeoSlices >= 0 && serieIndex >= diffGeoSlices)
               || (diffGeoSlices < 0 && serieIndex < (serieLength - (-diffGeoSlices)))) {

            final float sliceDiff = metricAltiSerie[serieIndex] - srtmAltitude;
            diffTo2ndAlti[serieIndex] = sliceDiff;
         }
      }

      updateUIGeoPos();
      updateTourChart();

      // this is not working, srtm data must be adjusted !!!
      // update only the second layer, this is much faster
//      _tourChart.update2ndAltiLayer(this, true);
   }

   /**
    * display altitude with the adjusted altitude data
    */
   private void onUpdateAltitude() {

      _tourData.altitudeSerie = Util.createFloatCopy(_metricAdjustedAltitudeWithoutSRTM);
      _tourData.clearAltitudeSeries();

      updateTourChart();
   }

   private void onUpdateAltitudeSRTM() {

      saveTour10AdjustSRTM();
      _tourData.clearAltitudeSeries();

      computeAltitudeSRTM();

      updateTourChart();
   }

   private void onUpdateAltitudeSRTMSpline() {

      saveTour10AdjustSRTM();
      _tourData.clearAltitudeSeries();

      /*
       * set all points to y=0
       */
      final double[] posY = _splineData.relativePositionY;
      for (int pointIndex = 0; pointIndex < posY.length; pointIndex++) {
         posY[pointIndex] = 0;
      }

      computeAltitudeSRTMSpline();

      updateTourChart();
   }

   private void restoreState() {

      // get previous selected adjustment type, use first type if not found
      final int prefAdjustType = _prefStore.getInt(PREF_ADJUST_TYPE);
      int comboIndex = 0;
      int typeIndex = 0;
      for (final AdjustmentType availAdjustType : _availableAdjustmentTypes) {
         if (prefAdjustType == availAdjustType.__id) {
            comboIndex = typeIndex;
            break;
         }
         typeIndex++;
      }
      _comboAdjustmentType.select(comboIndex);

      /*
       * A selection event is not fired, this is necessary that the layout of the dialog is run,
       * otherwise the options can be hidden
       */
      onSelectAdjustmentType();

      // get max options
      boolean isKeepStart;
      if (_prefStore.contains(PREF_KEEP_START)) {
         isKeepStart = _prefStore.getBoolean(PREF_KEEP_START);
      } else {
         isKeepStart = true;
      }
      _rdoKeepStart.setSelection(isKeepStart);
      _rdoKeepBottom.setSelection(!isKeepStart);

      /*
       * scale: geo position
       */
      int scaleGeoPos;
      if (_prefStore.contains(PREF_SCALE_GEO_POSITION)) {
         scaleGeoPos = _prefStore.getInt(PREF_SCALE_GEO_POSITION);
      } else {
         scaleGeoPos = MAX_ADJUST_GEO_POS_SLICES;
      }
      _scaleSlicePos.setSelection(scaleGeoPos);
   }

   private void restoreState_PostChartUpdate() {

      if (isAdjustmentType_SRTM_SPline()) {

         Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
               onModifySRTMSelection();
            }
         });
      }
   }

   private void saveState() {

      _prefStore.setValue(PREF_ADJUST_TYPE, getSelectedAdjustmentType().__id);

      _prefStore.setValue(
            ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT,
            _tourChartConfig.isShowTimeOnXAxis
                  ? TourManager.X_AXIS_TIME
                  : TourManager.X_AXIS_DISTANCE);

      _prefStore.setValue(PREF_KEEP_START, _rdoKeepStart.getSelection());

      _prefStore.setValue(PREF_SCALE_GEO_POSITION, _scaleSlicePos.getSelection());
   }

   /**
    * Tour is saved in the database when the dialog is closed, this method prepares the data.
    */
   private void saveTour() {

      _isTourSaved = true;

      switch (getSelectedAdjustmentType().__id) {
      case ADJUST_TYPE_SRTM:
      case ADJUST_TYPE_SRTM_SPLINE:
         saveTour10AdjustSRTM();
         break;

      case ADJUST_TYPE_HORIZONTAL_GEO_POSITION:
         saveTour20AdjustGeoSlicePosition();
         break;

      case ADJUST_TYPE_WHOLE_TOUR:
      case ADJUST_TYPE_START_AND_END:
      case ADJUST_TYPE_END:
      case ADJUST_TYPE_MAX_HEIGHT:
         _tourData.altitudeSerie = _metricAdjustedAltitudeWithoutSRTM;
         break;

      default:
         break;
      }

      // force the imperial altitude series to be recomputed
      _tourData.clearAltitudeSeries();

      // adjust altitude up/down values
      _tourData.computeAltitudeUpDown();

      // compute max altitude which requires other computed data series, e.g. speed (highly complex)
      _tourData.computeComputedValues();
   }

   private void saveTour10AdjustSRTM() {

      final float[] dataSerieAdjustedAlti = _tourData.dataSerieAdjustedAlti;
      final float[] newAltitudeSerie = _tourData.altitudeSerie = new float[dataSerieAdjustedAlti.length];

      for (int serieIndex = 0; serieIndex < dataSerieAdjustedAlti.length; serieIndex++) {
         newAltitudeSerie[serieIndex] = dataSerieAdjustedAlti[serieIndex] * UI.UNIT_VALUE_ALTITUDE;
      }
   }

   private void saveTour20AdjustGeoSlicePosition() {

      int diffGeoSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;
      final int serieLength = _tourData.timeSerie.length;

      // adjust slices to bounds
      if (diffGeoSlices > serieLength) {
         diffGeoSlices = serieLength - 1;
      } else if (-diffGeoSlices > serieLength) {
         diffGeoSlices = -(serieLength - 1);
      }

      final double[] oldLatSerie = _tourData.latitudeSerie;
      final double[] oldLonSerie = _tourData.longitudeSerie;

      final double[] newLatSerie = _tourData.latitudeSerie = new double[serieLength];
      final double[] newLonSerie = _tourData.longitudeSerie = new double[serieLength];

      final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
      final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
      final int adjustedLength = serieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

      System.arraycopy(oldLatSerie, srcPos, newLatSerie, destPos, adjustedLength);
      System.arraycopy(oldLonSerie, srcPos, newLonSerie, destPos, adjustedLength);

      // fill gaps with starting/ending position
      if (diffGeoSlices >= 0) {

         final double startLat = oldLatSerie[0];
         final double startLon = oldLonSerie[0];

         for (int serieIndex = 0; serieIndex < diffGeoSlices; serieIndex++) {
            newLatSerie[serieIndex] = startLat;
            newLonSerie[serieIndex] = startLon;
         }

      } else {

         // diffGeoSlices < 0

         final int lastIndex = serieLength - 1;
         final int validEndIndex = lastIndex - (-diffGeoSlices);
         final double endLat = oldLatSerie[lastIndex];
         final double endLon = oldLonSerie[lastIndex];

         for (int serieIndex = validEndIndex; serieIndex < serieLength; serieIndex++) {
            newLatSerie[serieIndex] = endLat;
            newLonSerie[serieIndex] = endLon;
         }
      }

      _tourData.computeGeo_Bounds();
   }

   private CubicSpline updateSplineData() {

      final double[] splineX = _splineData.graphXValues;
      final double[] splineY = _splineData.graphYValues;

      final double[] splineMinX = _splineData.graphXMinValues;
      final double[] splineMaxX = _splineData.graphXMaxValues;

      final double[] relativPosX = _splineData.relativePositionX;
      final double[] relativePosY = _splineData.relativePositionY;

      final int serieLength = _splineData.isPointMovable.length;

      for (int pointIndex = 0; pointIndex < serieLength; pointIndex++) {

         splineX[pointIndex] = relativPosX[pointIndex] * _sliderXAxisValue;
         splineY[pointIndex] = relativePosY[pointIndex] * _altiDiff;

         splineMinX[pointIndex] = 0;
         splineMaxX[pointIndex] = _sliderXAxisValue;
      }

      return new CubicSpline(splineX, splineY);
   }

   private void updateTourChart() {

      _isSliderEventDisabled = true;

      _tourChart.updateTourChart(_tourData, _tourChartConfig, true);

      _isSliderEventDisabled = false;
   }

   private void updateUI2ndLayer() {
      enableFieldsWithSRTM();
      _tourChart.updateLayer2ndAlti(this, true);
   }

   /**
    * set the altitude fields with the current altitude values
    */
   private void updateUIAltiFields() {

      final float[] metricAltitudeSerie = _metricAdjustedAltitudeWithoutSRTM;
      final float[] adjustedAltitude = _tourData.dataSerieAdjustedAlti = new float[metricAltitudeSerie.length];

      final float startAlti = metricAltitudeSerie[0];
      final float endAlti = metricAltitudeSerie[metricAltitudeSerie.length - 1];

      /*
       * get max and current measurement altitude
       */
      float maxAlti = metricAltitudeSerie[0];
      for (int serieIndex = 0; serieIndex < metricAltitudeSerie.length; serieIndex++) {

         final float metricAltitude = metricAltitudeSerie[serieIndex];

         if (metricAltitude > maxAlti) {
            maxAlti = metricAltitude;
         }

         adjustedAltitude[serieIndex] = metricAltitude / UI.UNIT_VALUE_ALTITUDE;
      }

      // keep current start/max values
      _prevAltiStart = startAlti;
      _prevAltiMax = maxAlti;

      _spinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(startAlti));
      _spinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(endAlti));
      _spinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, Float.valueOf(maxAlti));

      /*
       * prevent to fire the selection event in the spinner when a selection is set, this would
       * cause endless loops
       */
      _isDisableModifyListener = true;
      {
         _spinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));
         _spinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));
         _spinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
      }
      _isDisableModifyListener = false;

      getButton(IDialogConstants.OK_ID).setEnabled(true);
   }

   private void updateUIGeoPos() {

      final int geoPosSlices = _scaleSlicePos.getSelection() - MAX_ADJUST_GEO_POS_SLICES;

      _lblSliceValue.setText(Float.toString(geoPosSlices));
   }

}
