/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.device.daum.ergobike;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

public class DaumErgoBikeDataReader extends TourbookDevice {

	private static final String	DAUM_ERGO_BIKE_CSV_ID	= "Elapsed Time (s);Distance (km);Phys. kJoule;Slope (%);NM;RPM;Speed (km/h);Watt;Gear;Device Active;Pulse;Pulse Type;";	//$NON-NLS-1$

	private static final String	CSV_STRING_TOKEN		= ";";																														//$NON-NLS-1$

	private DecimalFormat		_decimalFormat			= (DecimalFormat) DecimalFormat.getInstance();

	// plugin constructor
	public DaumErgoBikeDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	@Override
	public String getDeviceModeName(final int profileId) {
		return null;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	@Override
	public int getTransferDataSize() {
		return -1;
	}

	private float parseFloat(final String stringValue) {

		try {
			return _decimalFormat.parse(stringValue).floatValue();
		} catch (final ParseException e1) {
			e1.printStackTrace();
		}

		return 0;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//	public boolean processDeviceData(	final String importFilePath,
//										final DeviceData deviceData,
//										final HashMap<Long, TourData> tourDataMap) {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT)) {

			/*
			 * use customized number format
			 */
			try {

				final DecimalFormatSymbols dfs = _decimalFormat.getDecimalFormatSymbols();

				final String groupSep = prefStore.getString(ITourbookPreferences.REGIONAL_GROUP_SEPARATOR);
				final String decimalSep = prefStore.getString(ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR);

				dfs.setGroupingSeparator(groupSep.charAt(0));
				dfs.setDecimalSeparator(decimalSep.charAt(0));

				_decimalFormat.setDecimalFormatSymbols(dfs);

			} catch (final Exception e) {
				e.printStackTrace();
			}
		} else {

			// use default number format

			_decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
		}

		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(importFilePath));

			/*
			 * check if the file is from a Daum Ergometer
			 */
			final String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
				return false;
			}

			StringTokenizer tokenizer;

			/*
			 * extract data from the file name
			 */
			final String fileName = new File(importFilePath).getName();

			//           1         2         3         4         5         6         7
			// 01234567890123456789012345678901234567890123456789012345678901234567890
			//
			// 0026  12_12_2007 20_35_02    1min    0_3km  Manuelles Training (Watt).csv
			// 0031  19_12_2007 19_11_37   35min   13_5km  Coaching - 003 - 2_5.csv
			// 0032  19_12_2007 19_46_44    1min    0_3km  Manuelles Training (Watt).csv

			// start date
			final int tourDay = Integer.parseInt(fileName.substring(6, 8));
			final int tourMonth = Integer.parseInt(fileName.substring(9, 11));
			final int tourYear = Integer.parseInt(fileName.substring(12, 16));

			// start time
			final int tourHour = Integer.parseInt(fileName.substring(17, 19));
			final int tourMin = Integer.parseInt(fileName.substring(20, 22));
			final int tourSec = Integer.parseInt(fileName.substring(23, 25));

			String title = fileName.substring(44);
			title = title.substring(0, title.length() - 4);

			/*
			 * set tour data
			 */
			final TourData tourData = new TourData();

			tourData.setTourStartTime(tourYear, tourMonth, tourDay, tourHour, tourMin, tourSec);

			tourData.setTourTitle(title);
			tourData.setTourDescription(fileName);

			tourData.setDeviceMode((short) 0);
			tourData.setDeviceTimeInterval((short) -1);

			tourData.setImportFilePath(importFilePath);

			/*
			 * set time serie from the imported trackpoints
			 */
			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int time;
			int previousTime = 0;

			int distance = 0;
			int previousDistance = 0;

			float cadence;
			int pulse;
			int power;
			float speed;
			boolean isFirstTime = true;

			String tokenLine;

			// read all data points
			while ((tokenLine = fileReader.readLine()) != null) {

				tokenizer = new StringTokenizer(tokenLine, CSV_STRING_TOKEN);

				time = (short) Integer.parseInt(tokenizer.nextToken()); // 				1  Elapsed Time (s)
				distance = (int) (parseFloat(tokenizer.nextToken()) * 1000); // 		2  Distance (m)
				tokenizer.nextToken(); // 												3  Phys. kJoule
				tokenizer.nextToken(); // 												4  Slope (%)
				tokenizer.nextToken(); // 												5  NM
				cadence = parseFloat(tokenizer.nextToken()); // 					6  RPM
				speed = parseFloat(tokenizer.nextToken()); // 					7  Speed (km/h)
				power = Integer.parseInt(tokenizer.nextToken()); //						8  Watt
				tokenizer.nextToken(); // 												9  Gear
				tokenizer.nextToken(); // 												10 Device Active
				pulse = Integer.parseInt(tokenizer.nextToken()); // 					11 Pulse

				timeDataList.add(timeData = new TimeData());

				if (isFirstTime) {
					isFirstTime = false;
					timeData.time = 0;
				} else {
					timeData.time = time - previousTime;
				}
				timeData.distance = distance - previousDistance;
				timeData.cadence = cadence;
				timeData.pulse = pulse;
				timeData.power = power;
				timeData.speed = speed;

				// prepare next data point
				previousTime = time;
				previousDistance = distance;
			}

			fileReader.close();

			if (timeDataList.size() == 0) {
				/*
				 * data are valid but have no data points
				 */
				return true;
			}

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			/*
			 * Set the start distance, this is not available in a .crp file but it's required to
			 * create the tour-id.
			 */
			tourData.setStartDistance(distance);

			tourData.createTimeSeries(timeDataList, false);

			// after all data are added, the tour id can be created
			final int tourDistance = (int) Math.abs(tourData.getStartDistance());
			final String uniqueId = createUniqueId_Legacy(tourData, tourDistance);
			final Long tourId = tourData.createTourId(uniqueId);

			// check if the tour is in the tour map
			if (alreadyImportedTours.containsKey(tourId) == false) {

				// add new tour to the map
				newlyImportedTours.put(tourId, tourData);

				// create additional data
				tourData.computeTourDrivingTime();
				tourData.computeComputedValues();
			}

			returnValue = true;

		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return returnValue;
	}

	/**
	 * checks if the data file has a valid .crp data format
	 * 
	 * @return true for a valid .crp data format
	 */
	@Override
	public boolean validateRawData(final String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			final String fileHeader = fileReader.readLine();
			if (fileHeader == null) {
				return false;
			}

			if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
				return false;
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

}
