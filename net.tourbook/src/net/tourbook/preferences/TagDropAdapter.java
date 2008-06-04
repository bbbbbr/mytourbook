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

package net.tourbook.preferences;

import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIRootItem;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

final class TagDropAdapter extends ViewerDropAdapter {

	private PrefPageTags	fPrefPageTags;
	private TreeViewer		fTagViewer;

	TagDropAdapter(final PrefPageTags prefPageTags, final TreeViewer tagViewer) {

		super(tagViewer);

		fPrefPageTags = prefPageTags;
		fTagViewer = tagViewer;
	}

	/**
	 * A tag item is dragged and is dropped into a target
	 * 
	 * @param draggedTagItem
	 *            tag tree item which is dragged
	 * @return Returns <code>true</code> when the tag is dropped
	 */
	private boolean dropTag(final TVITourTag draggedTagItem) {

		final Object hoveredTarget = getCurrentTarget();

		/*
		 * check if drag was startet from this tree
		 */
		if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() != fPrefPageTags.getDragStartTime()) {
			return false;
		}

		if (hoveredTarget instanceof TVITourTag) {

			final TVITourTag targetTagItem = (TVITourTag) hoveredTarget;
			final TourTag targetTourTag = targetTagItem.getTourTag();

			if (targetTourTag.isRoot()) {

				/*
				 * drop tag into a root tag
				 */

				dropTagIntoRoot(draggedTagItem);

			} else {

				/*
				 * drop the dragged tag into the parent category of the hovered tag
				 */

				final TreeViewerItem tagParentItem = targetTagItem.getParentItem();
				if (tagParentItem instanceof TVITourTagCategory) {
					dropTagIntoCategory(draggedTagItem, (TVITourTagCategory) tagParentItem);
				}
			}

		} else if (hoveredTarget instanceof TVITourTagCategory) {

			/*
			 * drop the dragged tag into the hovered target category
			 */

			dropTagIntoCategory(draggedTagItem, (TVITourTagCategory) hoveredTarget);

		} else if (hoveredTarget == null) {

			/*
			 * drop tag item into the root
			 */

			dropTagIntoRoot(draggedTagItem);
		}

		fPrefPageTags.setIsModified(true);

		return true;
	}

	private void dropTagIntoCategory(final TVITourTag itemDraggedTag, final TVITourTagCategory itemTargetCategory) {

		final TourTag draggedTag = itemDraggedTag.getTourTag();
		final TreeViewerItem itemDraggedParent = itemDraggedTag.getParentItem();

		final TourTagCategory targetCategory = itemTargetCategory.getTourTagCategory();
		TVITourTagCategory itemDraggedParentCategory = null;

		boolean isUpdateViewer = false;

		if (itemDraggedParent instanceof TVITourTagCategory) {

			/*
			 * dragged tag is from a tag category
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				itemDraggedParentCategory = (TVITourTagCategory) itemDraggedParent;
				final TourTagCategory draggedCategory = itemDraggedParentCategory.getTourTagCategory();

				/*
				 * remove tag from old category
				 */
				itemDraggedParentCategory.removeChild(itemDraggedTag);
				itemDraggedParentCategory.setTourTagCategory(updateModelRemoveTag(draggedTag, draggedCategory, em));

				/*
				 * add tag to the new category (target)
				 */
				itemTargetCategory.addChild(itemDraggedTag);
				itemTargetCategory.setTourTagCategory(updateModelAddTag(draggedTag, targetCategory, em));

				em.close();

				isUpdateViewer = true;
			}

		} else if (itemDraggedParent instanceof TVIRootItem) {

			/*
			 * dragged tag is a root tag item
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final TVIRootItem draggedRootItem = (TVIRootItem) itemDraggedParent;

				/*
				 * remove tag from root item
				 */
				draggedRootItem.removeChild(itemDraggedTag);

				/*
				 * update tag in db
				 */
				draggedTag.setRoot(false);
				TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class, em);

				/*
				 * add tag to the new category (target)
				 */
				itemTargetCategory.addChild(itemDraggedTag);
				itemTargetCategory.setTourTagCategory(updateModelAddTag(draggedTag, targetCategory, em));

				em.close();

				isUpdateViewer = true;
			}
		}

		if (isUpdateViewer) {

			// update tag viewer

			fTagViewer.remove(itemDraggedTag);
			fTagViewer.add(itemTargetCategory, itemDraggedTag);

			if (itemDraggedParentCategory != null) {
				fTagViewer.update(itemDraggedParentCategory, null);
			}
			fTagViewer.update(itemTargetCategory, null);
		}
	}

	private void dropTagIntoRoot(final TVITourTag itemDraggedTag) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		if (em != null) {

			final TourTag draggedTag = itemDraggedTag.getTourTag();
			final TreeViewerItem itemDraggedTagParent = itemDraggedTag.getParentItem();

			TVITourTagCategory itemDraggedParentCategory = null;

			if (itemDraggedTagParent instanceof TVITourTagCategory) {

				/*
				 * remove tag from old category
				 */

				itemDraggedParentCategory = (TVITourTagCategory) itemDraggedTagParent;
				final TourTagCategory draggedParentCategory = itemDraggedParentCategory.getTourTagCategory();

				itemDraggedParentCategory.removeChild(itemDraggedTag);
				itemDraggedParentCategory.setTourTagCategory(updateModelRemoveTag(draggedTag, draggedParentCategory, em));
			}

			/*
			 * update tag in db
			 */
			draggedTag.setRoot(true);
			TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class, em);

			/*
			 * add tag to the root item (target)
			 */
			final TVIRootItem rootItem = fPrefPageTags.getRootItem();
			rootItem.addChild(itemDraggedTag);

			em.close();

			/*
			 * update tag viewer
			 */
			fTagViewer.remove(itemDraggedTag);
			fTagViewer.add(fPrefPageTags, itemDraggedTag);

			if (itemDraggedParentCategory != null) {
				fTagViewer.update(itemDraggedParentCategory, null);
			}
		}
	}

	@Override
	public boolean performDrop(final Object dropData) {

		boolean returnValue = false;

		if (dropData instanceof StructuredSelection) {

			final StructuredSelection selection = (StructuredSelection) dropData;

			for (final Object element : selection.toList()) {
				if (element instanceof TVITourTag) {
					returnValue |= dropTag((TVITourTag) element);
				}
			}
		}

		return returnValue;
	}

	/**
	 * Add tag to the category
	 * 
	 * @param dropTag
	 * @param category
	 * @param em
	 * @return Returns the saved entity
	 */
	private TourTagCategory updateModelAddTag(	final TourTag dropTag,
												final TourTagCategory category,
												final EntityManager em) {

		final TourTagCategory lazyCategory = em.find(TourTagCategory.class, category.getCategoryId());

		// add new tag
		final Set<TourTag> lazyTourTags = lazyCategory.getTourTags();

		lazyTourTags.add(dropTag);
		lazyCategory.setTagCounter(lazyTourTags.size());

		return TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourTagCategory.class, em);
	}

	/**
	 * Remove tag from the category
	 * 
	 * @param dropTag
	 * @param category
	 * @param em
	 * @return Returns the saved entity
	 */
	private TourTagCategory updateModelRemoveTag(	final TourTag dropTag,
													final TourTagCategory category,
													final EntityManager em) {

		final TourTagCategory lazyCategory = em.find(TourTagCategory.class, category.getCategoryId());

		// remove tag
		final Set<TourTag> lazyTourTags = lazyCategory.getTourTags();

		lazyTourTags.remove(dropTag);
		lazyCategory.setTagCounter(lazyTourTags.size());

		return TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourTagCategory.class, em);
	}

	@Override
	public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

		final LocalSelectionTransfer localTransfer = LocalSelectionTransfer.getTransfer();

		if (localTransfer.isSupportedType(transferType) == false) {
			return false;
		}

		final ISelection selection = localTransfer.getSelection();
		if (selection instanceof StructuredSelection) {

			final Object draggedItem = ((StructuredSelection) selection).getFirstElement();

			if (target == draggedItem) {

				// don't drop on itself

				return false;

			} else {

//				if (draggedItem instanceof TVITourTag) {
//					if (target instanceof TVITourTagCategory || target instanceof TVITourTag) {
//						// drag tag into category or on a tag which will use the parent as category
//						return true;
//					} else if (target == null) {
//						// drag tag into root item
//						return true;
//					}
//				}

				return true;
			}
		}

		return false;
	}
}
