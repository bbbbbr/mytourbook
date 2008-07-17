/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVIWizardCompareRoot extends TVIWizardCompareItem {

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT");

		sb.append(" startYear ");

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA);

		sb.append(" GROUP BY startYear");
		sb.append(" ORDER BY startYear");
		
		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVIWizardCompareYear yearItem = new TVIWizardCompareYear(this);
				children.add(yearItem);

				yearItem.treeColumn = Integer.toString(dbYear);
				yearItem.tourYear = dbYear;
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}
}
