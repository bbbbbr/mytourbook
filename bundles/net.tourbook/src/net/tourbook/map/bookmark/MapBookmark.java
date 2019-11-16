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
package net.tourbook.map.bookmark;

import java.util.UUID;

import org.oscim.core.MapPosition;

public class MapBookmark {

	public String		id	= UUID.randomUUID().toString();

	public String		name;

	private double		_latitude;
	private double		_longitude;

	private MapPosition_with_MarkerPosition	_mapPosition;

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final MapBookmark other = (MapBookmark) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		return true;
	}

	public double getLatitude() {
		return _latitude;
	}

	public double getLongitude() {
		return _longitude;
	}

	public MapPosition_with_MarkerPosition getMapPosition() {
		return _mapPosition;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;

		result = prime * result + ((id == null) ? 0 : id.hashCode());

		return result;
	}

	public void setMapPosition(final MapPosition_with_MarkerPosition mapPosition) {

		_mapPosition = mapPosition;

		_latitude = mapPosition.getLatitude();
		_longitude = mapPosition.getLongitude();
	}

	@Override
	public String toString() {
		return "\n" //$NON-NLS-1$

				+ "MapBookmark " //$NON-NLS-1$

				+ "[" //$NON-NLS-1$

				+ "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "name=" + name + ", " //$NON-NLS-1$ //$NON-NLS-2$
				//				+ "latitude=" + latitude + ", "
				//				+ "longitude=" + longitude

				+ "]"; //$NON-NLS-1$
	}

}
