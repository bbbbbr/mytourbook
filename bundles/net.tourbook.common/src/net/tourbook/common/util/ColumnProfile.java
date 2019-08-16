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
package net.tourbook.common.util;

import java.util.ArrayList;

import net.tourbook.common.UI;

public class ColumnProfile implements Cloneable {

   private static long                _idCreator;

   String                             name                     = UI.EMPTY_STRING;

   /**
    * Contains column definitions which are visible in the table/tree in the sort order of the
    * table/tree.
    */
   ArrayList<ColumnDefinition>        visibleColumnDefinitions = new ArrayList<>();

   /**
    * Contains the column ids which are visible in the viewer.
    */
   String[]                           visibleColumnIds;

   /**
    * Contains a pair with column id/column width for visible columns.
    */
   String[]                           visibleColumnIdsAndWidth;

   /**
    * Contains a pair with column id/column format for all columns.
    *
    * @since 16.5
    */
   public ArrayList<ColumnProperties> columnProperties         = new ArrayList<>();

   private long                       _id;

   public ColumnProfile() {
      _id = ++_idCreator;
   }

   @Override
   protected ColumnProfile clone() {

      ColumnProfile clonedObject = null;

      try {

         clonedObject = (ColumnProfile) super.clone();

         clonedObject._id = ++_idCreator;

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
   }

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
      final ColumnProfile other = (ColumnProfile) obj;
      if (_id != other._id) {
         return false;
      }
      return true;
   }

   /**
    * @param columnId
    * @return Returns the visual index of the column or -1 when column is not visible.
    */
   public int getColumnIndex(final String columnId) {

      for (int columnIndex = 0; columnIndex < visibleColumnIds.length; columnIndex++) {
         final String visibleColumnId = visibleColumnIds[columnIndex];
         if (visibleColumnId.equals(columnId)) {
            return columnIndex;
         }
      }

      return -1;
   }

   public long getID() {
      return _id;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (_id ^ (_id >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "ColumnProfile [" // //$NON-NLS-1$

            + ("name=" + name + ", ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_id=" + _id + ", ") //$NON-NLS-1$ //$NON-NLS-2$

            + "]"; //$NON-NLS-1$
   }

}
