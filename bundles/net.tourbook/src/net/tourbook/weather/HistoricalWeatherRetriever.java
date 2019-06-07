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
package net.tourbook.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.tourbook.common.util.StatusUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * A class that retrieves, for a given track, the historical weather data.
 */
public class HistoricalWeatherRetriever {

   private String       startDate;
   private String       startTimeOfDay;
   private String       apiKey;

   private LatLng       searchAreaCenter;

   private WeatherData  historicalWeatherData;

   private final String apiUrl = "https://api.worldweatheronline.com/premium/v1/past-weather.ashx?key="; //$NON-NLS-1$

   private HistoricalWeatherRetriever(final LatLng searchAreaCenter) {
      this.searchAreaCenter = searchAreaCenter;
   }

   /**
    * Assigns the weather search area center to a new object.
    *
    * @return A new object.
    */
   public static HistoricalWeatherRetriever where(final LatLng searchAreaCenter) {
      return new HistoricalWeatherRetriever(searchAreaCenter);
   }

   /**
    * Assigns an API key to the current object.
    *
    * @param apiKey
    *           The API key to be used for the queries.
    * @return The current object.
    */
   public HistoricalWeatherRetriever forUser(final String apiKey) {
      this.apiKey = apiKey;
      return this;
   }

   public WeatherData getHistoricalWeatherData() {
      return historicalWeatherData;
   }

   /**
    * Parses a JSON weather data object into a WeatherData object.
    *
    * @param dailyNormalsData
    *           A string containing a historical weather data JSON object.
    * @return The parsed weather data.
    */
   private WeatherData parseWeatherData(final String weatherDataResponse) {

      final WeatherData weatherData = new WeatherData();
      try {
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class).get("data").get("weather").get(0).toString(); //$NON-NLS-1$ //$NON-NLS-2$

         final WWOWeatherResults rawWeatherData = mapper.readValue(weatherResults, WWOWeatherResults.class);

         // Within the hourly data, find the time that corresponds to the tour start time
         // and extract the weather data.
         for (final WWOHourlyResults hourlyData : rawWeatherData.gethourly()) {
            if (hourlyData.gettime().equals(startTimeOfDay)) {
               weatherData.setWindDirection(Integer.parseInt(hourlyData.getWinddirDegree()));
               weatherData.setWindSpeed(Integer.parseInt(hourlyData.getWindspeedKmph()));
               weatherData.setWeatherDescription(hourlyData.getWeatherDescription(rawWeatherData));
               weatherData.setWeatherType(hourlyData.getWeatherCode());
               break;
            }
         }

         weatherData.setTemperatureMax(rawWeatherData.getmaxtempC());
         weatherData.setTemperatureMin(rawWeatherData.getmintempC());
         weatherData.setTemperatureAverage(rawWeatherData.getavgtempC());

      } catch (final IOException e) {
         StatusUtil.log(
               "WeatherHistoryRetriever.parseWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherData + "\n" + e.getMessage()); //$NON-NLS-1$
      }

      return weatherData;
   }

   /**
    * Processes a query against the weather API.
    *
    * @return The result of the weather API query.
    */
   private String processRequest() {
      final StringBuffer weatherHistory = new StringBuffer();
      final String weatherRequestParameters = apiUrl + apiKey + "&q=" + searchAreaCenter.getLatitude() + "," + searchAreaCenter.getLongitude() //$NON-NLS-1$//$NON-NLS-2$
            + "&date=" + startDate + "&tp=1&format=json"; //$NON-NLS-1$ //$NON-NLS-2$
      //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour

      BufferedReader rd = null;
      try {
         final HttpClient client = HttpClientBuilder.create().build();
         final HttpGet request = new HttpGet(weatherRequestParameters);
         final HttpResponse response = client.execute(request);

         rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

         String line = ""; //$NON-NLS-1$
         while ((line = rd.readLine()) != null) {
            weatherHistory.append(line);
         }
      } catch (final Exception ex) {
         StatusUtil.log(
               "WeatherHistoryRetriever.processRequest : Error while executing the historical weather request with the parameters " //$NON-NLS-1$
                     + weatherRequestParameters + "\n" + ex.getMessage()); //$NON-NLS-1$
         return ""; //$NON-NLS-1$
      } finally {
         try {
            // close resources
            if (rd != null) {
               rd.close();
            }

         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      return weatherHistory.toString();
   }

   /**
    * This method builds a HistoricalWeatherRetriever object and retrieves, if
    * found, the weather data.
    *
    * @return the collected weather data.
    */
   public HistoricalWeatherRetriever retrieve() {

      historicalWeatherData = retrieveHistoricalWeatherData();

      return this;
   }

   /**
    * Retrieves the historical weather data
    *
    * @return The weather data, if found.
    */
   private WeatherData retrieveHistoricalWeatherData() {

      final String rawWeatherData = processRequest();
      if (!rawWeatherData.contains("weather")) { //$NON-NLS-1$
         return null;
      }
      final WeatherData historicalWeatherData = parseWeatherData(rawWeatherData);
      return historicalWeatherData;
   }

   /**
    * @param dateTime
    * @return
    */
   public HistoricalWeatherRetriever when(final String dateTime, final int startTimeOfDay) {
      this.startDate = dateTime;
      this.startTimeOfDay = startTimeOfDay + "00"; //$NON-NLS-1$
      return this;
   }
}
