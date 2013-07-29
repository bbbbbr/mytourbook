/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;


public interface ILegendProviderGradientColors extends ILegendProvider {

	/**
	 * @param graphValue
	 * @return Returns the RGB value for a graph value.
	 */
	abstract int getColorValue(float graphValue);

	abstract LegendColor getLegendColor();

	abstract LegendConfig getLegendConfig();

	/**
	 * Set the colors for the legend, the values will not be changed
	 * 
	 * @param newLegendColor
	 */
	abstract void setLegendColorColors(LegendColor newLegendColor);

	abstract void setLegendColorValues(	int legendHeight,
										float minValue,
										float maxValue,
										String unitText,
										LegendUnitFormat unitFormat);
}