/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.IFontEditorListener;
import net.tourbook.common.font.SimpleFontEditor;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.util.Util;
import net.tourbook.ui.views.calendar.CalendarConfigManager.CalendarColor_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.ColumnLayout_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DateColumn_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayContentColor_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.DayHeaderDateFormat_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.ICalendarConfigProvider;
import net.tourbook.ui.views.calendar.CalendarConfigManager.TourBackground_ComboData;
import net.tourbook.ui.views.calendar.CalendarConfigManager.TourBorder_ComboData;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Properties slideout for the calendar view.
 */
public class SlideoutCalendarOptions extends AdvancedSlideout implements ICalendarConfigProvider,
		IColorSelectorListener {

	private static final String		STATE_SELECTED_TAB		= "STATE_SELECTED_TAB";									//$NON-NLS-1$

	private static final int		_numWeekSummaryLines	= 6;

	private final IDialogSettings	_state					= TourbookPlugin.getState("SlideoutCalendarOptions");	//$NON-NLS-1$
	private IFontEditorListener		_defaultFontEditorListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private SelectionAdapter		_defaultSelectionListener;
	private FocusListener			_keepOpenListener;

	private SelectionAdapter		_weekValueListener;

	private boolean					_isUpdateUI;
	private PixelConverter			_pc;

	private int						_subItemIndent;

	/*
	 * This is a hack to vertical center the font label, otherwise it will be complicated to set it
	 * correctly
	 */
	private int						_fontLabelVIndent		= 5;

	/*
	 * UI controls
	 */
	private CalendarView			_calendarView;
	//
	private Button					_btnReset;
	private Button					_chkIsHideDayDateWhenNoTour;
	private Button					_chkUseDraggedScrolling;
	private Button					_chkIsShowDateColumn;
	private Button					_chkIsShowDayDateWeekendColor;
	private Button					_chkIsShowDayDate;
	private Button					_chkIsShowSummaryColumn;
	private Button					_chkIsShowMonthWithAlternateColor;
	private Button					_chkIsShowWeekValueUnit;
	private Button					_chkIsShowYearColumns;
	//
	private ColorSelectorExtended	_colorAlternateMonthColor;
	//
	private Combo					_comboColumnLayout;
	private Combo					_comboConfigName;
	private Combo					_comboDateColumn;
	private Combo					_comboDayContentColor;
	private Combo					_comboDayHeaderDateFormat;
	private Combo					_comboTourBackground;
	private Combo					_comboTourBackgroundColor1;
	private Combo					_comboTourBackgroundColor2;
	private Combo					_comboTourBorder;
	private Combo					_comboTourBorderColor;
	private Combo					_comboTour_Value_1;
	private Combo					_comboTour_Value_2;
	private Combo					_comboTour_Value_3;
	private Combo					_comboTour_Format_1_1;
	private Combo					_comboTour_Format_1_2;
	private Combo					_comboTour_Format_2_1;
	private Combo					_comboTour_Format_2_2;
	private Combo					_comboTour_Format_3_1;
	private Combo					_comboTour_Format_3_2;
	private Combo[]					_comboWeek_AllValues;
	private Combo[]					_comboWeek_AllFormats;
	private Combo					_comboWeek_ValueColor;
	//
	private Composite				_weekFormatterContainer;
	//
	private Label					_lblDateColumn_Content;
	private Label					_lblDateColumn_Font;
	private Label					_lblDateColumn_Width;
	private Label					_lblDayHeader_Font;
	private Label					_lblDayHeader_Format;
	private Label					_lblNumYearColumn;
	private Label					_lblWeek_ColumnWidth;
	private Label[]					_lblWeek_SummaryAll;
	private Label					_lblWeek_ValueColor;
	private Label					_lblWeek_ValueFont;
	private Label					_lblYearColumn_HeaderFont;
	private Label					_lblYearColumn_Spacing;
	private Label					_lblYearColumn_Start;
	//
	private SimpleFontEditor		_fontEditorDayContent;
	private SimpleFontEditor		_fontEditorDayDate;
	private SimpleFontEditor		_fontEditorDateColumn;
	private SimpleFontEditor		_fontEditorWeekValue;
	private SimpleFontEditor		_fontEditorYearColumnHeader;
	//
	private Spinner					_spinnerNumYearColumns;
	private Spinner					_spinnerYearColumnSpacing;
	private Spinner					_spinnerDateColumnWidth;
	private Spinner					_spinnerWeek_ColumnWidth;
	private Spinner					_spinnerTourBackgroundWidth;
	private Spinner					_spinnerTourBorderWidth;
	private Spinner					_spinnerWeekHeight;
	//
	private TabFolder				_tabFolder;
	//
	private Text					_textConfigName;
	//
	private ToolItem				_toolItem;

	/**
	 * @param ownerControl
	 * @param toolItem
	 * @param state
	 * @param calendarView
	 * @param gridPrefPrefix
	 */
	public SlideoutCalendarOptions(	final ToolItem toolItem,
									final IDialogSettings state,
									final CalendarView calendarView) {

		super(toolItem.getParent(), state, null);

		_calendarView = calendarView;
		_toolItem = toolItem;

		setTitleText(Messages.Slideout_CalendarOptions_Label_Title);
		setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		createActions();
		createUI(parent);

		fillUI();
		fillUI_Config();

		restoreState_UI();
		restoreState_Config();
	}

	@Override
	protected void createTitleBarControls(final Composite parent) {

		// this method is called 1st

		initUI(parent);

		{
			/*
			 * Combo: Configuration
			 */
			_comboConfigName = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
			_comboConfigName.setVisibleItemCount(20);
			_comboConfigName.addFocusListener(_keepOpenListener);
			_comboConfigName.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectConfig();
				}
			});
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.indent(20, 0)
					.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
					.applyTo(_comboConfigName);
		}
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			_tabFolder = new TabFolder(shellContainer, SWT.TOP);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(_tabFolder);
			{
				final TabItem tabLayout = new TabItem(_tabFolder, SWT.NONE);
				tabLayout.setControl(createUI_010_Tab_Layout(_tabFolder));
				tabLayout.setText(Messages.Slideout_CalendarOptions_Tab_Layout);

				final TabItem tabDayContent = new TabItem(_tabFolder, SWT.NONE);
				tabDayContent.setControl(createUI_020_Tab_DayContent(_tabFolder));
				tabDayContent.setText(Messages.Slideout_CalendarOptions_Tab_DayContent);

				final TabItem tabWeekSummary = new TabItem(_tabFolder, SWT.NONE);
				tabWeekSummary.setControl(createUI_030_Tab_WeekSummary(_tabFolder));
				tabWeekSummary.setText(Messages.Slideout_CalendarOptions_Tab_WeekSummary);
			}

			createUI_299_Configuration(shellContainer);
		}

		return shellContainer;
	}

	private Control createUI_010_Tab_Layout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI_200_DateColumn(container);
			createUI_220_YearColumns(container);
			createUI_240_Layout(container);
		}

		return container;
	}

	private Control createUI_020_Tab_DayContent(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			createUI_300_DayDate(container);
			createUI_400_DayContent(container);
		}

		return container;
	}

	private Control createUI_030_Tab_WeekSummary(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_500_WeekSummaryColumn(container);
		}

		return container;
	}

	private void createUI_200_DateColumn(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DateColumn);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Date column
				 */
				// checkbox
				_chkIsShowDateColumn = new Button(group, SWT.CHECK);
				_chkIsShowDateColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn);
				_chkIsShowDateColumn.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowDateColumn_Tooltip);
				_chkIsShowDateColumn.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkIsShowDateColumn);
			}

			final Composite container = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				{
					/*
					 * Column width
					 */

					// label
					_lblDateColumn_Width = new Label(container, SWT.NONE);
					_lblDateColumn_Width.setText(Messages.Slideout_CalendarOptions_Label_DateColumn_Width);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(_subItemIndent, 0)
							.applyTo(_lblDateColumn_Width);

					// size
					_spinnerDateColumnWidth = new Spinner(container, SWT.BORDER);
					_spinnerDateColumnWidth.setMinimum(1);
					_spinnerDateColumnWidth.setMaximum(200);
					_spinnerDateColumnWidth.setIncrement(1);
					_spinnerDateColumnWidth.setPageIncrement(10);
					_spinnerDateColumnWidth.addSelectionListener(_defaultSelectionListener);
					_spinnerDateColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
				}
				{
					/*
					 * Date content
					 */

					// label
					_lblDateColumn_Content = new Label(container, SWT.NONE);
					_lblDateColumn_Content.setText(Messages.Slideout_CalendarOptions_Label_DateColumnContent);
					_lblDateColumn_Content.setToolTipText(
							Messages.Slideout_CalendarOptions_Label_DateColumnContent_Tooltip);
					GridDataFactory
							.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(_subItemIndent, 0)
							.applyTo(_lblDateColumn_Content);

					// value
					_comboDateColumn = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
					_comboDateColumn.setVisibleItemCount(20);
					_comboDateColumn.addSelectionListener(_defaultSelectionListener);
					_comboDateColumn.addFocusListener(_keepOpenListener);
				}
			}

			final Composite containerRight = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(containerRight);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerRight);
//			containerRight.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				{
					/*
					 * Font
					 */

					// label
					_lblDateColumn_Font = new Label(containerRight, SWT.NONE);
					_lblDateColumn_Font.setText(Messages.Slideout_CalendarOptions_Label_DateColumnFont);
					GridDataFactory
							.fillDefaults()//
							.indent(0, _fontLabelVIndent)
							.applyTo(_lblDateColumn_Font);

					// value
					_fontEditorDateColumn = new SimpleFontEditor(containerRight, SWT.NONE);
					_fontEditorDateColumn.addFontListener(_defaultFontEditorListener);
					GridDataFactory.fillDefaults().grab(true, true).applyTo(_fontEditorDateColumn);
				}
			}
		}
	}

	private void createUI_220_YearColumns(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_YearColumns);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year columns
				 */

				// checkbox
				_chkIsShowYearColumns = new Button(group, SWT.CHECK);
				_chkIsShowYearColumns.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumns);
				_chkIsShowYearColumns.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowYearColumns_Tooltip);
				_chkIsShowYearColumns.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowYearColumns);
			}
			createUI_222_Col1(group);
			createUI_224_Col2(group);
		}
	}

	private void createUI_222_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Number of year columns
				 */

				// label
				_lblNumYearColumn = new Label(container, SWT.NONE);
				_lblNumYearColumn.setText(Messages.Slideout_CalendarOptions_Label_NumYearColumns);
				_lblNumYearColumn.setToolTipText(Messages.Slideout_CalendarOptions_Label_NumYearColumns_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblNumYearColumn);

				// spinner: columns
				_spinnerNumYearColumns = new Spinner(container, SWT.BORDER);
				_spinnerNumYearColumns.setMinimum(CalendarConfigManager.YEAR_COLUMNS_MIN);
				_spinnerNumYearColumns.setMaximum(CalendarConfigManager.YEAR_COLUMNS_MAX);
				_spinnerNumYearColumns.setIncrement(1);
				_spinnerNumYearColumns.setPageIncrement(2);
				_spinnerNumYearColumns.addSelectionListener(_defaultSelectionListener);
				_spinnerNumYearColumns.addMouseWheelListener(_defaultMouseWheelListener);

			}
			{
				/*
				 * Year columns space
				 */

				// label
				_lblYearColumn_Spacing = new Label(container, SWT.NONE);
				_lblYearColumn_Spacing.setText(Messages.Slideout_CalendarOptions_Label_YearColumnsSpace);
				_lblYearColumn_Spacing.setToolTipText(
						Messages.Slideout_CalendarOptions_Label_YearColumnsSpace_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYearColumn_Spacing);

				// spinner: columns
				_spinnerYearColumnSpacing = new Spinner(container, SWT.BORDER);
				_spinnerYearColumnSpacing.setMinimum(CalendarConfigManager.CALENDAR_COLUMNS_SPACE_MIN);
				_spinnerYearColumnSpacing.setMaximum(CalendarConfigManager.CALENDAR_COLUMNS_SPACE_MAX);
				_spinnerYearColumnSpacing.setIncrement(1);
				_spinnerYearColumnSpacing.setPageIncrement(10);
				_spinnerYearColumnSpacing.addSelectionListener(_defaultSelectionListener);
				_spinnerYearColumnSpacing.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Column start
				 */

				// label
				_lblYearColumn_Start = new Label(container, SWT.NONE);
				_lblYearColumn_Start.setText(Messages.Slideout_CalendarOptions_Label_YearColumnsStart);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblYearColumn_Start);

				// value
				_comboColumnLayout = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboColumnLayout.setVisibleItemCount(20);
				_comboColumnLayout.addSelectionListener(_defaultSelectionListener);
				_comboColumnLayout.addFocusListener(_keepOpenListener);
			}
		}
	}

	private void createUI_224_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Year header
				 */

				// label
				_lblYearColumn_HeaderFont = new Label(container, SWT.NONE);
				_lblYearColumn_HeaderFont.setText(Messages.Slideout_CalendarOptions_Label_YearHeaderFont);
				GridDataFactory
						.fillDefaults()//
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblYearColumn_HeaderFont);

				// font/size
				_fontEditorYearColumnHeader = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorYearColumnHeader.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorYearColumnHeader);
			}
		}
	}

	private void createUI_240_Layout(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_Layout);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, LayoutConstants.getSpacing().y)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_242_Col1(group);
			createUI_244_Col2(group);
		}
	}

	private void createUI_242_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Week height
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_RowHeight);
				label.setToolTipText(Messages.Slideout_CalendarOptions_Label_RowHeight_Tooltip);

				// spinner: height
				_spinnerWeekHeight = new Spinner(container, SWT.BORDER);
				_spinnerWeekHeight.setMinimum(CalendarConfigManager.WEEK_HEIGHT_MIN);
				_spinnerWeekHeight.setMaximum(CalendarConfigManager.WEEK_HEIGHT_MAX);
				_spinnerWeekHeight.setIncrement(1);
				_spinnerWeekHeight.setPageIncrement(10);
				_spinnerWeekHeight.addSelectionListener(_defaultSelectionListener);
				_spinnerWeekHeight.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_244_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Mouse wheel scrolling
				 */

				// checkbox
				_chkUseDraggedScrolling = new Button(container, SWT.CHECK);
				_chkUseDraggedScrolling.setText(Messages.Slideout_CalendarOptions_Checkbox_UseDraggedScrolling);
				_chkUseDraggedScrolling.setToolTipText(
						Messages.Slideout_CalendarOptions_Checkbox_UseDraggedScrolling_Tooltip);
				_chkUseDraggedScrolling.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkUseDraggedScrolling);
			}
		}
	}

	private void createUI_299_Configuration(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Name
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_Name);
				label.setToolTipText(Messages.Slideout_CalendarOptions_Label_Name_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			}
			{
				/*
				 * Text
				 */
				_textConfigName = new Text(container, SWT.BORDER);
				_textConfigName.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(final ModifyEvent e) {
						onModifyName();
					}
				});
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.applyTo(_textConfigName);
			}
			{
				/*
				 * Button: Reset
				 */
				_btnReset = new Button(container, SWT.PUSH);
				_btnReset.setText(Messages.App_Action_Reset);
				_btnReset.setToolTipText(Messages.App_Action_ResetConfig_Tooltip);
				_btnReset.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectConfig_Default(e);
					}
				});
				GridDataFactory
						.fillDefaults()//
						//						.grab(true, false)
						.align(SWT.END, SWT.CENTER)
						.applyTo(_btnReset);
			}
		}
	}

	private void createUI_300_DayDate(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DayDate);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * Day date
				 */

				// checkbox
				_chkIsShowDayDate = new Button(group, SWT.CHECK);
				_chkIsShowDayDate.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayDate);
				_chkIsShowDayDate.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowDayDate);
			}
			{
				final Composite container = new Composite(group, SWT.NONE);
//				container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.applyTo(container);
				GridLayoutFactory
						.fillDefaults()//
						.numColumns(2)
						.spacing(10, LayoutConstants.getSpacing().y)
						.applyTo(container);
				{
					createUI_320_Col1(container);
					createUI_330_Col2(container);
				}
			}
		}
	}

	private void createUI_320_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Day date format
				 */

				// label
				_lblDayHeader_Format = new Label(container, SWT.NONE);
				_lblDayHeader_Format.setText(Messages.Slideout_CalendarOptions_Label_DayHeaderFormat);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_subItemIndent, 0)
						.applyTo(_lblDayHeader_Format);

				// value
				_comboDayHeaderDateFormat = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayHeaderDateFormat.setVisibleItemCount(20);
				_comboDayHeaderDateFormat.addSelectionListener(_defaultSelectionListener);
				_comboDayHeaderDateFormat.addFocusListener(_keepOpenListener);
			}
			{
				/*
				 * Hide day when empty
				 */

				// checkbox
				_chkIsHideDayDateWhenNoTour = new Button(container, SWT.CHECK);
				_chkIsHideDayDateWhenNoTour.setText(Messages.Slideout_CalendarOptions_Checkbox_IsHideDayWhenEmpty);
				_chkIsHideDayDateWhenNoTour.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsHideDayDateWhenNoTour);
			}
			{
				/*
				 * Show weekend color
				 */

				// checkbox
				_chkIsShowDayDateWeekendColor = new Button(container, SWT.CHECK);
				_chkIsShowDayDateWeekendColor.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowDayWeekendColor);
				_chkIsShowDayDateWeekendColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.indent(_subItemIndent, 0)
						.span(2, 1)
						.applyTo(_chkIsShowDayDateWeekendColor);
			}
		}
	}

	private void createUI_330_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Font
				 */

				// label
				_lblDayHeader_Font = new Label(container, SWT.NONE);
				_lblDayHeader_Font.setText(Messages.Slideout_CalendarOptions_Label_DayDateFont);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblDayHeader_Font);

				// value
				_fontEditorDayDate = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorDayDate.addFontListener(_defaultFontEditorListener);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_fontEditorDayDate);
			}
		}
	}

	private void createUI_400_DayContent(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_CalendarOptions_Group_DayContent);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			{
				/*
				 * Tour background
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_TourBackground);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTourBackground = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackground.setVisibleItemCount(20);
				_comboTourBackground.addSelectionListener(_defaultSelectionListener);
				_comboTourBackground.addFocusListener(_keepOpenListener);

				// combo color 1
				_comboTourBackgroundColor1 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackgroundColor1.setVisibleItemCount(20);
				_comboTourBackgroundColor1.addSelectionListener(_defaultSelectionListener);

				// combo color 2
				_comboTourBackgroundColor2 = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBackgroundColor2.setVisibleItemCount(20);
				_comboTourBackgroundColor2.addSelectionListener(_defaultSelectionListener);

				// background width
				_spinnerTourBackgroundWidth = new Spinner(group, SWT.BORDER);
				_spinnerTourBackgroundWidth.setMinimum(0);
				_spinnerTourBackgroundWidth.setMaximum(100);
				_spinnerTourBackgroundWidth.setIncrement(1);
				_spinnerTourBackgroundWidth.setPageIncrement(10);
				_spinnerTourBackgroundWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTourBackgroundWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Tour border
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_TourBorder);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				// value
				_comboTourBorder = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBorder.setVisibleItemCount(20);
				_comboTourBorder.addSelectionListener(_defaultSelectionListener);
				_comboTourBorder.addFocusListener(_keepOpenListener);

				// combo color
				_comboTourBorderColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboTourBorderColor.setVisibleItemCount(20);
				_comboTourBorderColor.addSelectionListener(_defaultSelectionListener);

				// spacer
				new Label(group, SWT.NONE);

				// border width
				_spinnerTourBorderWidth = new Spinner(group, SWT.BORDER);
				_spinnerTourBorderWidth.setMinimum(0);
				_spinnerTourBorderWidth.setMaximum(100);
				_spinnerTourBorderWidth.setIncrement(1);
				_spinnerTourBorderWidth.setPageIncrement(10);
				_spinnerTourBorderWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerTourBorderWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Font
				 */

				// label
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_CalendarOptions_Label_DayContentFont);
				GridDataFactory
						.fillDefaults()//
						//						.grab(true, true)
						.indent(0, _fontLabelVIndent)
						.applyTo(label);

				// font/size
				_fontEditorDayContent = new SimpleFontEditor(group, SWT.NONE);
				_fontEditorDayContent.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_fontEditorDayContent);

				// combo color
				_comboDayContentColor = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboDayContentColor.setVisibleItemCount(20);
				_comboDayContentColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.span(3, 1)
						.applyTo(_comboDayContentColor);
			}
			{
				/*
				 * Month alternate color
				 */
				// checkbox
				_chkIsShowMonthWithAlternateColor = new Button(group, SWT.CHECK);
				_chkIsShowMonthWithAlternateColor.setText(
						Messages.Slideout_CalendarOptions_Checkbox_IsShowMonthWithAlternatingColor);
				_chkIsShowMonthWithAlternateColor.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.span(4, 1)
						.applyTo(_chkIsShowMonthWithAlternateColor);
			}
			{
				/*
				 * Alternate color
				 */

				// Color selector
				_colorAlternateMonthColor = new ColorSelectorExtended(group);
				GridDataFactory
						.swtDefaults()//
						.grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_colorAlternateMonthColor.getButton());

				_colorAlternateMonthColor.addOpenListener(this);
				_colorAlternateMonthColor.addListener(_defaultPropertyChangeListener);
			}

		}
	}

	private void createUI_500_WeekSummaryColumn(final Composite parent) {

		{
			/*
			 * Show week summary column
			 */

			// checkbox
			_chkIsShowSummaryColumn = new Button(parent, SWT.CHECK);
			_chkIsShowSummaryColumn.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowSummaryColumn);
			_chkIsShowSummaryColumn.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.BEGINNING)
					//					.span(2, 1)
					.applyTo(_chkIsShowSummaryColumn);
		}

		_weekFormatterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.indent(_subItemIndent, 0)
				.applyTo(_weekFormatterContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_weekFormatterContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_510_Layout(_weekFormatterContainer);
			createUI_550_Values(_weekFormatterContainer);
		}
	}

	private void createUI_510_Layout(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_512_Layout_Col1(container);
			createUI_514_Layout_Col2(container);
		}
	}

	private void createUI_512_Layout_Col1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Column width
				 */

				// label
				_lblWeek_ColumnWidth = new Label(container, SWT.NONE);
				_lblWeek_ColumnWidth.setText(Messages.Slideout_CalendarOptions_Label_SummaryColumn_Width);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblWeek_ColumnWidth);

				// size
				_spinnerWeek_ColumnWidth = new Spinner(container, SWT.BORDER);
				_spinnerWeek_ColumnWidth.setMinimum(1);
				_spinnerWeek_ColumnWidth.setMaximum(200);
				_spinnerWeek_ColumnWidth.setIncrement(1);
				_spinnerWeek_ColumnWidth.setPageIncrement(10);
				_spinnerWeek_ColumnWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerWeek_ColumnWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Value color
				 */

				// label
				_lblWeek_ValueColor = new Label(container, SWT.NONE);
				_lblWeek_ValueColor.setText(Messages.Slideout_CalendarOptions_Label_WeekValueColor);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblWeek_ValueColor);

				// combo
				_comboWeek_ValueColor = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboWeek_ValueColor.setVisibleItemCount(20);
				_comboWeek_ValueColor.addSelectionListener(_defaultSelectionListener);

			}
			{
				/*
				 * Show value unit
				 */

				// checkbox
				_chkIsShowWeekValueUnit = new Button(container, SWT.CHECK);
				_chkIsShowWeekValueUnit.setText(Messages.Slideout_CalendarOptions_Checkbox_IsShowWeekValueUnit);
				_chkIsShowWeekValueUnit.addSelectionListener(_defaultSelectionListener);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.BEGINNING)
						.span(2, 1)
						.applyTo(_chkIsShowWeekValueUnit);
			}
		}
	}

	private void createUI_514_Layout_Col2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * Font
				 */

				// label
				_lblWeek_ValueFont = new Label(container, SWT.NONE);
				_lblWeek_ValueFont.setText(Messages.Slideout_CalendarOptions_Label_WeekValueFont);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.indent(0, _fontLabelVIndent)
						.applyTo(_lblWeek_ValueFont);

				// value
				_fontEditorWeekValue = new SimpleFontEditor(container, SWT.NONE);
				_fontEditorWeekValue.addFontListener(_defaultFontEditorListener);
				GridDataFactory
						.fillDefaults()//
						.grab(true, true)
						.applyTo(_fontEditorWeekValue);
			}
		}
	}

	private void createUI_550_Values(final Composite parent) {

		_lblWeek_SummaryAll = new Label[_numWeekSummaryLines];
		_comboWeek_AllValues = new Combo[_numWeekSummaryLines];
		_comboWeek_AllFormats = new Combo[_numWeekSummaryLines];

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)

				// set bottom margin to look less ugly
				.extendedMargins(0, 0, 0, 10)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			for (int lineIndex = 0; lineIndex < _numWeekSummaryLines; lineIndex++) {

				// label
				final Label lblWeek = new Label(container, SWT.NONE);
				lblWeek.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, 1));

				// value
				final Combo comboWeek_Value = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboWeek_Value.setVisibleItemCount(20);
				comboWeek_Value.addSelectionListener(_weekValueListener);
				comboWeek_Value.addFocusListener(_keepOpenListener);

				// value format
				final Combo comboWeek_Format = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				comboWeek_Format.setVisibleItemCount(20);
				comboWeek_Format.addSelectionListener(_defaultSelectionListener);
				comboWeek_Format.addFocusListener(_keepOpenListener);

				_lblWeek_SummaryAll[lineIndex] = lblWeek;
				_comboWeek_AllValues[lineIndex] = comboWeek_Value;
				_comboWeek_AllFormats[lineIndex] = comboWeek_Format;
			}
		}
	}

	private void createUI_a980_TourInfo(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
		{
			/*
			 * 1. Line
			 */

			// label
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, 1));

			// value
			_comboTour_Value_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_1.setVisibleItemCount(20);
			_comboTour_Value_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_1.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_1_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_1.setVisibleItemCount(20);
			_comboTour_Format_1_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_1_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_1_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_1_2.setVisibleItemCount(20);
			_comboTour_Format_1_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_1_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 2. Line
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, 2));

			_comboTour_Value_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_2.setVisibleItemCount(20);
			_comboTour_Value_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_2.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_2_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_1.setVisibleItemCount(20);
			_comboTour_Format_2_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_2_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_2_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_2_2.setVisibleItemCount(20);
			_comboTour_Format_2_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_2_2.addFocusListener(_keepOpenListener);
		}
		{
			/*
			 * 3. Line
			 */
			final Label label = new Label(container, SWT.NONE);
			label.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, 3));

			_comboTour_Value_3 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Value_3.setVisibleItemCount(20);
			_comboTour_Value_3.addSelectionListener(_defaultSelectionListener);
			_comboTour_Value_3.addFocusListener(_keepOpenListener);

			// value 1 format
			_comboTour_Format_3_1 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_1.setVisibleItemCount(20);
			_comboTour_Format_3_1.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_3_1.addFocusListener(_keepOpenListener);

			// value 2 format
			_comboTour_Format_3_2 = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTour_Format_3_2.setVisibleItemCount(20);
			_comboTour_Format_3_2.addSelectionListener(_defaultSelectionListener);
			_comboTour_Format_3_2.addFocusListener(_keepOpenListener);
		}
	}

	private void enableControls() {

		final boolean isShowDateColumn = _chkIsShowDateColumn.getSelection();
		final boolean isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		final boolean isShowDayDate = _chkIsShowDayDate.getSelection();
		final boolean isYearColumns = _chkIsShowYearColumns.getSelection();

		// date column
		_comboDateColumn.setEnabled(isShowDateColumn);
		_fontEditorDateColumn.setEnabled(isShowDateColumn);
		_lblDateColumn_Content.setEnabled(isShowDateColumn);
		_lblDateColumn_Font.setEnabled(isShowDateColumn);
		_lblDateColumn_Width.setEnabled(isShowDateColumn);
		_spinnerDateColumnWidth.setEnabled(isShowDateColumn);

		// day date
		_chkIsShowDayDateWeekendColor.setEnabled(isShowDayDate);
		_chkIsHideDayDateWhenNoTour.setEnabled(isShowDayDate);
		_comboDayHeaderDateFormat.setEnabled(isShowDayDate);
		_fontEditorDayDate.setEnabled(isShowDayDate);
		_lblDayHeader_Format.setEnabled(isShowDayDate);
		_lblDayHeader_Font.setEnabled(isShowDayDate);

		// day content
		final TourBackground_ComboData selectedTourBackgroundData = getSelectedTourBackgroundData();
		final TourBorder_ComboData selectedTourBorderData = getSelectedTourBorderData();

		_comboTourBackgroundColor1.setEnabled(selectedTourBackgroundData.isColor1);
		_comboTourBackgroundColor2.setEnabled(selectedTourBackgroundData.isColor2);
		_spinnerTourBackgroundWidth.setEnabled(selectedTourBackgroundData.isWidth);

		_comboTourBorderColor.setEnabled(selectedTourBorderData.isColor);
		_spinnerTourBorderWidth.setEnabled(selectedTourBorderData.isWidth);

		// layout
		_comboColumnLayout.setEnabled(isYearColumns);
		_fontEditorYearColumnHeader.setEnabled(isYearColumns);
		_lblYearColumn_HeaderFont.setEnabled(isYearColumns);
		_lblYearColumn_Spacing.setEnabled(isYearColumns);
		_lblYearColumn_Start.setEnabled(isYearColumns);
		_lblNumYearColumn.setEnabled(isYearColumns);
		_spinnerNumYearColumns.setEnabled(isYearColumns);
		_spinnerYearColumnSpacing.setEnabled(isYearColumns);

		// week summary
		_chkIsShowWeekValueUnit.setEnabled(isShowSummaryColumn);
		_fontEditorWeekValue.setEnabled(isShowSummaryColumn);
		_lblWeek_ColumnWidth.setEnabled(isShowSummaryColumn);
		_lblWeek_ValueColor.setEnabled(isShowSummaryColumn);
		_lblWeek_ValueFont.setEnabled(isShowSummaryColumn);
		_spinnerWeek_ColumnWidth.setEnabled(isShowSummaryColumn);
		enableControls_WeekSummary();
	}

	private void enableControls_WeekSummary() {

		final boolean isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();

		final WeekFormatter[] tourWeekSummaryFormatter = CalendarConfigManager.allWeekFormatter;

		for (int lineIndex = 0; lineIndex < _numWeekSummaryLines; lineIndex++) {

			final Label labelWeekSummary = _lblWeek_SummaryAll[lineIndex];
			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

			boolean canDoFormatting = false;

			if (isShowSummaryColumn) {

				final int selectedValueIndex = comboWeekValue.getSelectionIndex();

				if (selectedValueIndex >= 0) {

					final WeekFormatter selectedWeekFormatter = tourWeekSummaryFormatter[selectedValueIndex];

					canDoFormatting = selectedWeekFormatter.getValueFormats() != null;
				}
			}

			labelWeekSummary.setEnabled(isShowSummaryColumn);
			comboWeekValue.setEnabled(isShowSummaryColumn);
			comboWeekFormat.setEnabled(isShowSummaryColumn && canDoFormatting);
		}
	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			/*
			 * Fill Combos
			 */

			final CalendarColor_ComboData[] allCalendarColor_ComboData = CalendarConfigManager
					.getAllCalendarColor_ComboData();

			for (final DateColumn_ComboData data : CalendarConfigManager.getAllDateColumnData()) {
				_comboDateColumn.add(data.label);
			}

			for (final DayHeaderDateFormat_ComboData data : CalendarConfigManager.getAllDayHeaderDateFormat_ComboData()) {
				_comboDayHeaderDateFormat.add(data.label);
			}

			for (final ColumnLayout_ComboData data : CalendarConfigManager.getAllColumnLayout_ComboData()) {
				_comboColumnLayout.add(data.label);
			}

			/*
			 * Tour background
			 */
			for (final TourBackground_ComboData data : CalendarConfigManager.getAllTourBackground_ComboData()) {
				_comboTourBackground.add(data.label);
			}
			for (final CalendarColor_ComboData data : allCalendarColor_ComboData) {
				_comboTourBackgroundColor1.add(data.label);
			}
			for (final CalendarColor_ComboData data : allCalendarColor_ComboData) {
				_comboTourBackgroundColor2.add(data.label);
			}

			/*
			 * Tour border
			 */
			for (final TourBorder_ComboData data : CalendarConfigManager.getAllTourBorderData()) {
				_comboTourBorder.add(data.label);
			}
			for (final CalendarColor_ComboData data : allCalendarColor_ComboData) {
				_comboTourBorderColor.add(data.label);
			}

			for (final DayContentColor_ComboData data : CalendarConfigManager.getAllDayContentColor_ComboData()) {
				_comboDayContentColor.add(data.label);
			}

			/*
			 * Fill week summary values, formatter is filled when a value is selected
			 */
			for (int lineIndex = 0; lineIndex < _numWeekSummaryLines; lineIndex++) {

				final Label labelWeekSummary = _lblWeek_SummaryAll[lineIndex];
				final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];

				labelWeekSummary.setText(NLS.bind(Messages.Slideout_CalendarOptions_Label_Line, lineIndex + 1));

				for (final WeekFormatter weekFormatter : CalendarConfigManager.allWeekFormatter) {

					comboWeekValue.add(weekFormatter.getText());
				}
			}

			// week summary colors
			for (final CalendarColor_ComboData data : allCalendarColor_ComboData) {
				_comboWeek_ValueColor.add(data.label);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void fillUI_Config() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			_comboConfigName.removeAll();

			for (final CalendarConfig config : CalendarConfigManager.getAllCalendarConfigs()) {
				_comboConfigName.add(config.name);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void fillWeekFormats(final Widget widget, final Combo comboWeek_Value, final Combo comboWeek_Format) {

		if (widget != comboWeek_Value) {

			// another combo fired the event

			return;
		}

		comboWeek_Format.removeAll();

		final int selectedFormatterIndex = comboWeek_Value.getSelectionIndex();

		if (selectedFormatterIndex < 0) {
			return;
		}

		final int defaultIndex = fillWeekFormats_UI(comboWeek_Format, selectedFormatterIndex);

		comboWeek_Format.select(defaultIndex);
	}

	/**
	 * @param comboWeek_Format
	 * @param selectedFormatterIndex
	 *            Index in {@link CalendarConfigManager#allWeekFormatter}
	 * @return
	 */
	private int fillWeekFormats_UI(final Combo comboWeek_Format, final int selectedFormatterIndex) {

		final WeekFormatter selectedFormatter =
				CalendarConfigManager.allWeekFormatter[selectedFormatterIndex];

		final ValueFormat[] valueFormats = selectedFormatter.getValueFormats();

		if (valueFormats == null) {
			return 0;
		}

		final ValueFormat defaultFormat = selectedFormatter.getDefaultFormat();
		int defaultIndex = 0;

		for (int formatIndex = 0; formatIndex < valueFormats.length; formatIndex++) {

			final ValueFormat valueFormat = valueFormats[formatIndex];

			// fill format combo
			comboWeek_Format.add(FormatManager.getValueFormatterName(valueFormat));

			// get index for the default format
			if (valueFormat == defaultFormat) {
				defaultIndex = formatIndex;
			}
		}

		return defaultIndex;
	}

	private int getCalendarColorIndex(final CalendarColor requestedData) {

		final CalendarColor_ComboData[] allData = CalendarConfigManager.getAllCalendarColor_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final CalendarColor_ComboData data = allData[dataIndex];

			if (data.color.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getCalendarColumLayoutIndex(final ColumnStart requestedData) {

		final ColumnLayout_ComboData[] allData = CalendarConfigManager.getAllColumnLayout_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final ColumnLayout_ComboData data = allData[dataIndex];

			if (data.columnLayout.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDateColumnIndex(final DateColumnContent requestedData) {

		final DateColumn_ComboData[] allInfoColumnData = CalendarConfigManager.getAllDateColumnData();

		for (int dataIndex = 0; dataIndex < allInfoColumnData.length; dataIndex++) {

			final DateColumn_ComboData data = allInfoColumnData[dataIndex];

			if (data.dateColumn.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayContentColorIndex(final CalendarColor requestedData) {

		final DayContentColor_ComboData[] allData = CalendarConfigManager.getAllDayContentColor_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayContentColor_ComboData data = allData[dataIndex];

			if (data.dayContentColor.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getDayHeaderDateFormatIndex(final DayDateFormat requestedData) {

		final DayHeaderDateFormat_ComboData[] allData = CalendarConfigManager.getAllDayHeaderDateFormat_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final DayHeaderDateFormat_ComboData data = allData[dataIndex];

			if (data.dayHeaderDateFormat.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _toolItem.getBounds();
		final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private CalendarColor getSelectedCalendarColor(final Combo comboColor) {

		final int selectedIndex = comboColor.getSelectionIndex();

		final CalendarColor_ComboData[] allCalendarColorData = CalendarConfigManager.getAllCalendarColor_ComboData();

		if (selectedIndex < 0) {
			return allCalendarColorData[0].color;
		}

		return allCalendarColorData[selectedIndex].color;
	}

	private ColumnStart getSelectedColumnLayout() {

		final int selectedIndex = _comboColumnLayout.getSelectionIndex();

		if (selectedIndex < 0) {
			return ColumnStart.CONTINUOUSLY;
		}

		final ColumnLayout_ComboData data = CalendarConfigManager.getAllColumnLayout_ComboData()[selectedIndex];

		return data.columnLayout;
	}

	private DateColumnContent getSelectedDateColumn() {

		final int selectedIndex = _comboDateColumn.getSelectionIndex();

		if (selectedIndex < 0) {
			return DateColumnContent.WEEK_NUMBER;
		}

		final DateColumn_ComboData selectedInfoColumnData = CalendarConfigManager.getAllDateColumnData()[selectedIndex];

		return selectedInfoColumnData.dateColumn;
	}

	private DayContentColor_ComboData getSelectedDayContentColor() {

		final int selectedIndex = _comboDayContentColor.getSelectionIndex();

		final DayContentColor_ComboData[] allData = CalendarConfigManager.getAllDayContentColor_ComboData();

		if (selectedIndex < 0) {

			for (final DayContentColor_ComboData data : allData) {

				if (data.dayContentColor == CalendarConfigManager.DEFAULT_DAY_CONTENT_COLOR) {
					return data;
				}
			}

			// return default default
			return allData[0];
		}

		return allData[selectedIndex];
	}

	private DayDateFormat getSelectedDayDateFormat() {

		final int selectedIndex = _comboDayHeaderDateFormat.getSelectionIndex();

		if (selectedIndex < 0) {
			return DayDateFormat.AUTOMATIC;
		}

		final DayHeaderDateFormat_ComboData selectedData = CalendarConfigManager
				.getAllDayHeaderDateFormat_ComboData()[selectedIndex];

		return selectedData.dayHeaderDateFormat;
	}

	private TourBackground_ComboData getSelectedTourBackgroundData() {

		final int selectedIndex = _comboTourBackground.getSelectionIndex();

		final TourBackground_ComboData[] allTourBackgroundData = CalendarConfigManager.getAllTourBackground_ComboData();

		if (selectedIndex < 0) {

			for (final TourBackground_ComboData data : allTourBackgroundData) {

				if (data.tourBackground == CalendarConfigManager.DEFAULT_TOUR_BACKGROUND) {
					return data;
				}
			}

			// return default default
			return allTourBackgroundData[0];
		}

		return allTourBackgroundData[selectedIndex];
	}

	private TourBorder_ComboData getSelectedTourBorderData() {

		final int selectedIndex = _comboTourBorder.getSelectionIndex();

		final TourBorder_ComboData[] allTourBorderData = CalendarConfigManager.getAllTourBorderData();

		if (selectedIndex < 0) {

			for (final TourBorder_ComboData data : allTourBorderData) {

				if (data.tourBorder == CalendarConfigManager.DEFAULT_TOUR_BORDER) {
					return data;
				}
			}

			// return default default
			return allTourBorderData[0];
		}

		return allTourBorderData[selectedIndex];
	}

	private WeekFormatterData[] getSelectedWeekFormatter() {

		final ArrayList<WeekFormatterData> selectedFormatterData = new ArrayList<>();

		final WeekFormatter[] tourWeekSummaryFormatter = CalendarConfigManager.allWeekFormatter;

		for (int lineIndex = 0; lineIndex < _numWeekSummaryLines; lineIndex++) {

			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

			final int selectedValueIndex = comboWeekValue.getSelectionIndex();
			if (selectedValueIndex > -1) {

				final WeekFormatter selectedWeekFormatter = tourWeekSummaryFormatter[selectedValueIndex];

				final ValueFormat[] valueFormats = selectedWeekFormatter.getValueFormats();
				final boolean canDoFormatting = valueFormats != null;

				if (canDoFormatting) {

					final int selectedFormatIndex = comboWeekFormat.getSelectionIndex();
					if (selectedFormatIndex > -1) {

						final WeekFormatterData formatterData = new WeekFormatterData(
								selectedWeekFormatter.id,
								valueFormats[selectedFormatIndex]);

						selectedFormatterData.add(formatterData);
					}
				}
			}
		}

		return selectedFormatterData.toArray(
				new WeekFormatterData[selectedFormatterData.size()]);
	}

	private int getTourBackgroundIndex(final TourBackground requestedData) {

		final TourBackground_ComboData[] allData = CalendarConfigManager.getAllTourBackground_ComboData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBackground_ComboData data = allData[dataIndex];

			if (data.tourBackground.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private int getTourBorderIndex(final TourBorder requestedData) {

		final TourBorder_ComboData[] allData = CalendarConfigManager.getAllTourBorderData();

		for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

			final TourBorder_ComboData data = allData[dataIndex];

			if (data.tourBorder.equals(requestedData)) {
				return dataIndex;
			}
		}

		// this should not happen
		return 0;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_subItemIndent = _pc.convertHorizontalDLUsToPixels(12);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyConfig();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onModifyConfig();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onModifyConfig();
			}
		};

		_weekValueListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChange_WeekValue(e.widget);
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsKeepOpenInternally(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsKeepOpenInternally(false);
			}
		};

		_defaultFontEditorListener = new IFontEditorListener() {

			@Override
			public void fontDialogOpened(final boolean isDialogOpened) {
				setIsKeepOpenInternally(isDialogOpened);
			}

			@Override
			public void fontSelected(final FontData font) {
				onModifyConfig();
			}
		};

		CalendarConfigManager.setConfigProvider(this);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {

				onDisposeSlideout();
			}
		});
	}

	private void onChange_WeekValue(final Widget widget) {

		for (int lineIndex = 0; lineIndex < _numWeekSummaryLines; lineIndex++) {

			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];
			final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

			fillWeekFormats(widget, comboWeekValue, comboWeekFormat);
		}

		_weekFormatterContainer.layout(true, true);

		onModifyConfig();
	}

	private void onDisposeSlideout() {

		// reset config provider
		CalendarConfigManager.setConfigProvider((SlideoutCalendarOptions) null);

		saveState_UI();
	}

	@Override
	protected void onFocus() {

		_comboConfigName.setFocus();
	}

	private void onModifyConfig() {

		saveState_Config();

		enableControls();

		updateUI();
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboConfigName.getSelectionIndex();

		_comboConfigName.setItem(selectedIndex, _textConfigName.getText());

		saveState_Config();
	}

	@Override
	protected void onReparentShell(final Shell reparentedShell) {

		super.onReparentShell(reparentedShell);

		// size for the resizable shell is set to DEFAULT
		reparentedShell.pack(true);
	}

	private void onSelectConfig() {

		final int selectedIndex = _comboConfigName.getSelectionIndex();
		final ArrayList<CalendarConfig> allConfigurations = CalendarConfigManager.getAllCalendarConfigs();

		final CalendarConfig selectedConfig = allConfigurations.get(selectedIndex);
		final CalendarConfig activeConfig = CalendarConfigManager.getActiveCalendarConfig();

		if (selectedConfig.equals(activeConfig)) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState_Config();

		CalendarConfigManager.setActiveCalendarConfig(selectedConfig, this);

		restoreState_Config();

		updateUI();
	}

	private void onSelectConfig_Default(final SelectionEvent selectionEvent) {

		if (Util.isCtrlKeyPressed(selectionEvent)) {

			// reset All configurations

			CalendarConfigManager.resetAllCalendarConfigurations();

			fillUI_Config();

		} else {

			// reset active config

			CalendarConfigManager.resetActiveCalendarConfiguration();
		}

		restoreState_Config();

		updateUI();
	}

	private void restoreState_Config() {

		_isUpdateUI = true;
		{
			final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

			// get active config AFTER getting the index because this could change the active config
			final int activeConfigIndex = CalendarConfigManager.getActiveCalendarConfigIndex();

			// config
			_comboConfigName.select(activeConfigIndex);
			_textConfigName.setText(config.name);

			// day date
			_chkIsHideDayDateWhenNoTour.setSelection(config.isHideDayDateWhenNoTour);
			_chkIsShowDayDate.setSelection(config.isShowDayDate);
			_chkIsShowDayDateWeekendColor.setSelection(config.isShowDayDateWeekendColor);
			_comboDayHeaderDateFormat.select(getDayHeaderDateFormatIndex(config.dayDateFormat));
			_comboDayContentColor.select(getDayContentColorIndex(config.dayContentColor));
			_fontEditorDayDate.setSelection(config.dayDateFont);

			// day content
			_colorAlternateMonthColor.setColorValue(config.alternateMonthRGB);
			_chkIsShowMonthWithAlternateColor.setSelection(config.isToggleMonthColor);
			_comboTourBackground.select(getTourBackgroundIndex(config.tourBackground));
			_comboTourBackgroundColor1.select(getCalendarColorIndex(config.tourBackgroundColor1));
			_comboTourBackgroundColor2.select(getCalendarColorIndex(config.tourBackgroundColor2));
			_comboTourBorder.select(getTourBorderIndex(config.tourBorder));
			_comboTourBorderColor.select(getCalendarColorIndex(config.tourBorderColor));
			_fontEditorDayContent.setSelection(config.dayContentFont);
			_spinnerTourBackgroundWidth.setSelection(config.tourBackgroundWidth);
			_spinnerTourBorderWidth.setSelection(config.tourBorderWidth);

			// date column
			_chkIsShowDateColumn.setSelection(config.isShowDateColumn);
			_spinnerDateColumnWidth.setSelection(config.dateColumnWidth);
			_comboDateColumn.select(getDateColumnIndex(config.dateColumnContent));
			_fontEditorDateColumn.setSelection(config.dateColumnFont);

			// week summary column
			_chkIsShowSummaryColumn.setSelection(config.isShowSummaryColumn);
			_chkIsShowWeekValueUnit.setSelection(config.isShowWeekValueUnit);
			_comboWeek_ValueColor.select(getCalendarColorIndex(config.weekValueColor));
			_fontEditorWeekValue.setSelection(config.weekValueFont);
			_spinnerWeek_ColumnWidth.setSelection(config.summaryColumnWidth);
			selectWeekFormatter(config.allWeekFormatterData);

			// year columns
			_chkIsShowYearColumns.setSelection(config.isShowYearColumns);
			_comboColumnLayout.select(getCalendarColumLayoutIndex(config.yearColumnsStart));
			_spinnerNumYearColumns.setSelection(config.numYearColumns);
			_spinnerYearColumnSpacing.setSelection(config.yearColumnsSpacing);
			_fontEditorYearColumnHeader.setSelection(config.yearHeaderFont);

			// layout
			_spinnerWeekHeight.setSelection(config.weekHeight);
			_chkUseDraggedScrolling.setSelection(config.useDraggedScrolling);
		}
		_isUpdateUI = false;

		enableControls();
	}

	private void restoreState_UI() {

		_tabFolder.setSelection(Util.getStateInt(_state, STATE_SELECTED_TAB, 0));
	}

	private void saveState_Config() {

		// update config

		final CalendarConfig config = CalendarConfigManager.getActiveCalendarConfig();

		// config
		config.name = _textConfigName.getText();

		// day date
		config.dayDateFont = _fontEditorDayDate.getSelection();
		config.dayDateFormat = getSelectedDayDateFormat();
		config.isHideDayDateWhenNoTour = _chkIsHideDayDateWhenNoTour.getSelection();
		config.isShowDayDate = _chkIsShowDayDate.getSelection();
		config.isShowDayDateWeekendColor = _chkIsShowDayDateWeekendColor.getSelection();

		// day content
		config.alternateMonthRGB = _colorAlternateMonthColor.getColorValue();
		config.dayContentColor = getSelectedDayContentColor().dayContentColor;
		config.dayContentFont = _fontEditorDayContent.getSelection();
		config.isToggleMonthColor = _chkIsShowMonthWithAlternateColor.getSelection();
		config.tourBackground = getSelectedTourBackgroundData().tourBackground;
		config.tourBackgroundColor1 = getSelectedCalendarColor(_comboTourBackgroundColor1);
		config.tourBackgroundColor2 = getSelectedCalendarColor(_comboTourBackgroundColor2);
		config.tourBackgroundWidth = _spinnerTourBackgroundWidth.getSelection();
		config.tourBorder = getSelectedTourBorderData().tourBorder;
		config.tourBorderColor = getSelectedCalendarColor(_comboTourBorderColor);
		config.tourBorderWidth = _spinnerTourBorderWidth.getSelection();

		// date column
		config.dateColumnContent = getSelectedDateColumn();
		config.dateColumnWidth = _spinnerDateColumnWidth.getSelection();
		config.dateColumnFont = _fontEditorDateColumn.getSelection();
		config.isShowDateColumn = _chkIsShowDateColumn.getSelection();

		// week summary column
		config.allWeekFormatterData = getSelectedWeekFormatter();
		config.isShowSummaryColumn = _chkIsShowSummaryColumn.getSelection();
		config.isShowWeekValueUnit = _chkIsShowWeekValueUnit.getSelection();
		config.summaryColumnWidth = _spinnerWeek_ColumnWidth.getSelection();
		config.weekValueFont = _fontEditorWeekValue.getSelection();
		config.weekValueColor = getSelectedCalendarColor(_comboWeek_ValueColor);

		// year columns
		config.isShowYearColumns = _chkIsShowYearColumns.getSelection();
		config.numYearColumns = _spinnerNumYearColumns.getSelection();
		config.yearColumnsStart = getSelectedColumnLayout();
		config.yearColumnsSpacing = _spinnerYearColumnSpacing.getSelection();
		config.yearHeaderFont = _fontEditorYearColumnHeader.getSelection();

		// layout
		config.weekHeight = _spinnerWeekHeight.getSelection();
		config.useDraggedScrolling = _chkUseDraggedScrolling.getSelection();

		_calendarView.updateUI_CalendarConfig();

		CalendarConfigManager.updateFormatterValueFormat();

	}

	private void saveState_UI() {

		final int selectedTabIndex = _tabFolder.getSelectionIndex();
		_state.put(STATE_SELECTED_TAB, selectedTabIndex < 0 ? 0 : selectedTabIndex);
	}

	private void selectWeekFormatter(final WeekFormatterData[] weekFormatterData) {

		final WeekFormatter[] allWeekFormatter = CalendarConfigManager.allWeekFormatter;

		int numFormatterSet = 0;

		// loop: all lines
		for (int lineIndex = 0; lineIndex < weekFormatterData.length; lineIndex++) {

			final Combo comboWeekValue = _comboWeek_AllValues[lineIndex];

			final WeekFormatterData formatterData = weekFormatterData[lineIndex];
			final WeekFormatterID formatterId = formatterData.id;

			// loop: all formatter
			for (int formatterIndex = 0; formatterIndex < allWeekFormatter.length; formatterIndex++) {

				final WeekFormatter weekSummaryFormatter = allWeekFormatter[formatterIndex];

				if (formatterId == weekSummaryFormatter.id) {

					final Combo comboWeekFormat = _comboWeek_AllFormats[lineIndex];

					// select formatter value
					comboWeekValue.select(formatterIndex);

					// remove old formats
					comboWeekFormat.removeAll();

					// fill value format combo
					fillWeekFormats_UI(comboWeekFormat, formatterIndex);

					// select value format
					final ValueFormat[] valueFormats = weekSummaryFormatter.getValueFormats();

					if (valueFormats != null) {

						int formatterFormatIndex = 0;

						for (int formatIndex = 0; formatIndex < valueFormats.length; formatIndex++) {

							final ValueFormat valueFormat = valueFormats[formatIndex];

							if (formatterData.valueFormat == valueFormat) {

								formatterFormatIndex = formatIndex;
								break;
							}
						}

						comboWeekFormat.select(formatterFormatIndex);

						numFormatterSet++;

						break;
					}
				}
			}

		}

		// set remaining formatters empty
		for (int lineIndex = numFormatterSet; lineIndex < _comboWeek_AllValues.length; lineIndex++) {

			_comboWeek_AllValues[lineIndex].select(0);

			// remove old content
			_comboWeek_AllFormats[lineIndex].removeAll();
		}

		_weekFormatterContainer.layout(true, true);
	}

	private void updateUI() {

		_calendarView.updateUI_Layout(true);
	}

	@Override
	public void updateUI_CalendarConfig() {

		fillUI_Config();

		restoreState_Config();

		updateUI();
	}

}
