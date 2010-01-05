/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.map;

public enum OverlayImageState {

	/**
	 * The overlay image state is not yet set
	 */
	NOT_SET,

	/**
	 * An overlay image for this tile is not available
	 */
	NO_IMAGE,

	/**
	 * This tile has content for an overlay image, this state is set when the tile has content not
	 * when the content is from a part
	 */
	TILE_HAS_CONTENT,

	/**
	 * This tile has content for a part overlay image, this state is set when the tile has no
	 * content but the content is from a part
	 */
	TILE_HAS_PART_CONTENT,

}
