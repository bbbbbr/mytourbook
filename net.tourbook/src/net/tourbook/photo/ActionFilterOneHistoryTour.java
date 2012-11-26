/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionFilterOneHistoryTour extends Action {

	private TourPhotoLinkView	_photosAndToursView;

	public ActionFilterOneHistoryTour(final TourPhotoLinkView photosAndToursView) {

		super(null, AS_CHECK_BOX);

		_photosAndToursView = photosAndToursView;

		setToolTipText(Messages.Action_PhotosAndTours_FilterNoTours_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(//
				Messages.Image__Photo_FilterOneHistoryTour));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(//
				Messages.Image__Photo_FilterOneHistoryTour_Disabled));
	}

	@Override
	public void run() {
		_photosAndToursView.actionFilterOneHistoryTour();
	}

}
