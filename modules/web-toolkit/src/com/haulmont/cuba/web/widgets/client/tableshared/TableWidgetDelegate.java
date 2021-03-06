/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.web.widgets.client.tableshared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import com.haulmont.cuba.web.widgets.client.Tools;
import com.haulmont.cuba.web.widgets.client.aggregation.TableAggregationRow;
import com.haulmont.cuba.web.widgets.client.tableshared.TableWidget.AfterBodyUpdateListener;
import com.vaadin.client.*;
import com.vaadin.client.Focusable;
import com.vaadin.client.ui.AbstractLayoutConnector;
import com.vaadin.client.ui.ManagedLayout;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.v7.client.ui.VScrollTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TableWidgetDelegate {
    public static final String CUBA_TABLE_CLICKABLE_CELL_CONTENT = "c-table-clickable-cell-content";
    public static final String CUBA_TABLE_CLICKABLE_CELL_STYLE = "c-table-clickable-cell";
    public static final String CUBA_TABLE_CLICKABLE_TEXT_STYLE = "c-table-clickable-text";
    public static final String WIDGET_CELL_CLASSNAME = "widget-container";
    public static final String TABLE_SORT_CONTEXTMENU_ITEM = "c-table-contextmenu-item";
    public static final String TREE_TABLE_SPACER = "v-treetable-treespacer";

    public VScrollTable table;
    public TableWidget tableWidget;

    public TableWidgetDelegate(VScrollTable table, TableWidget tableWidget) {
        this.table = table;
        this.tableWidget = tableWidget;
    }

    public ShortcutActionHandler shortcutHandler;

    public boolean textSelectionEnabled = false;
    public boolean contextMenuEnabled = true;

    public VOverlay presentationsEditorPopup;
    public VOverlay customContextMenuPopup;

    public Widget presentationsMenu;
    public Widget customContextMenu;

    public boolean multiLineCells = false;

    public TableAggregationRow aggregationRow;

    public TableEmptyState tableEmptyState;
    public Runnable emptyStateLinkClickHandler;

    public Set<String> clickableColumns;
    public Set<String> clickableTableColumns;
    public Set<String> htmlCaptionColumns;
    public TableCellClickListener cellClickListener;

    public TotalAggregationInputListener totalAggregationInputHandler;
    public GroupAggregationInputListener groupAggregationInputHandler;

    public VOverlay customPopupOverlay;
    public Widget customPopupWidget;
    public boolean customPopupAutoClose = false;
    public int lastClickClientX;
    public int lastClickClientY;

    public String tableSortResetLabel;
    public String tableSortAscendingLabel;
    public String tableSortDescendingLabel;

    public String selectAllLabel;
    public String deselectAllLabel;

    public List<AfterBodyUpdateListener> afterBodyUpdateListeners;

    /*
     * Table page can contain partially visible rows and when we try to scroll to the last row (which in the current page
     * is partially visible) it will not be fully shown. Given property should be used only when scrollTo is invoked for
     * last item.
     */
    public boolean scrollToLastItemEnabled = false;

    public void requestFocus(final String itemKey, final String columnKey) {
        Scheduler.get().scheduleDeferred(() -> {
            try {
                setFocus(itemKey, columnKey);
            } catch (Exception e) {
                VConsole.error(e);
            }
        });
    }

    public void setFocus(String itemKey, String columnKey) {
        HasWidgets row = tableWidget.getRenderedRowByKey(itemKey);
        int columnIndex = Arrays.asList(tableWidget.getVisibleColOrder()).indexOf(columnKey);

        for (Widget childWidget : row) {
            Element element = ((Widget) row).getElement();
            if (element.getChild(columnIndex).getFirstChild() == childWidget.getElement().getParentNode()) {
                this.focusWidget(childWidget);
                break;
            }
        }
    }

    public void showPresentationEditorPopup(Event event, Widget presentationsEditIcon) {
        if (event.getEventTarget().cast() == presentationsEditIcon.getElement() && tableWidget.isEnabled()) {
            this.presentationsEditorPopup = new VOverlay();
            this.presentationsEditorPopup.setStyleName("c-table-prefs-editor");
            this.presentationsEditorPopup.setOwner(table);
            this.presentationsEditorPopup.setWidget(this.presentationsMenu);

            // Store the currently focused element, which will be re-focused when
            // context menu is closed
            Element focusedElement = WidgetUtil.getFocusedElement();

            this.presentationsEditorPopup.addCloseHandler(e -> {
                Element currentFocus = WidgetUtil.getFocusedElement();
                if (focusedElement != null && (currentFocus == null
                        || presentationsEditorPopup.getElement().isOrHasChild(currentFocus)
                        || RootPanel.getBodyElement().equals(currentFocus))) {
                    focusedElement.focus();
                }

                presentationsEditorPopup = null;
            });

            this.presentationsEditorPopup.setAutoHideEnabled(true);
            this.presentationsEditorPopup.showRelativeTo(presentationsEditIcon);
        }
    }

    public void reassignHeaderCellWidth(int colIndex, VScrollTable.HeaderCell hCell, int minWidth) {
        if (tableWidget.isCustomColumn(colIndex)) {
            return;
        }

        for (Widget rowWidget : tableWidget.getRenderedRows()) {
            if (isAggregationVisible()
                    && tableWidget.isGenericRow(rowWidget)) {
                VScrollTable.VScrollTableBody.VScrollTableRow row = (VScrollTable.VScrollTableBody.VScrollTableRow) rowWidget;

                double realColWidth = row.getRealCellWidth(colIndex);
                if (realColWidth > 0) {
                    updateHeaderCellWidth(hCell, minWidth, realColWidth);
                    break;
                }
            }
        }

        if (tableWidget.getRenderedRows().isEmpty()
                && isAggregationVisible()
                && aggregationRow.isInitialized()) {
            double realColWidth = aggregationRow.getRealCellWidth(colIndex);
            if (!Double.isNaN(realColWidth)) {
                if (realColWidth > 0) {
                    updateHeaderCellWidth(hCell, minWidth, realColWidth);
                }
            } else {
                // schedule reassigning in case of empty table grouping due to aggregation row is not displayed yet
                Scheduler.get().scheduleDeferred(() -> {
                    double rcw = aggregationRow.getRealCellWidth(colIndex);
                    if (rcw > 0) {
                        updateHeaderCellWidth(hCell, minWidth, rcw);
                    }
                });
            }
        }
    }

    public void updateHeaderCellWidth(VScrollTable.HeaderCell hCell, int minWidth, double realColWidth) {
        if (realColWidth > minWidth) {
            Style hStyle = hCell.getElement().getStyle();
            hStyle.setProperty("width", realColWidth + "px");
            hStyle.setProperty("minWidth", realColWidth + "px");
            hStyle.setProperty("maxWidth", realColWidth + "px");
        }
    }

    public void scheduleLayoutForChildWidgets() {
        if (table.scrollBody != null) {
            // Fix for #VAADIN-12970, relayout cell widgets
            // Haulmont API
            ComponentConnector connector = Util.findConnectorFor(table);
            // may be null if we switch tabs fast
            if (connector != null) {
                LayoutManager lm = connector.getLayoutManager();

                for (Widget w : table.scrollBody) {
                    HasWidgets row = (HasWidgets) w;
                    for (Widget child : row) {
                        ComponentConnector childConnector = Util.findConnectorFor(child);
                        if (childConnector != null && childConnector.getConnectorId() != null) {
                            if (childConnector instanceof ManagedLayout
                                    || childConnector instanceof AbstractLayoutConnector) {
                                lm.setNeedsMeasure(childConnector);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean focusWidget(Widget widget) {
        if (widget instanceof Focusable) {
            ((Focusable) widget).focus();
            return true;
        } else if (widget instanceof com.google.gwt.user.client.ui.Focusable) {
            ((com.google.gwt.user.client.ui.Focusable) widget).setFocus(true);
            return true;
        } else if (widget instanceof HasWidgets) {
            for (Widget childWidget : (HasWidgets) widget) {
                if (focusWidget(childWidget)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void showContextMenuPopup(int left, int top) {
        if (this.customContextMenu instanceof HasWidgets) {
            if (!((HasWidgets) this.customContextMenu).iterator().hasNext()) {
                // there are no actions to show
                return;
            }
        }

        // Store the currently focused element, which will be re-focused when
        // context menu is closed
        Element focusedElement = WidgetUtil.getFocusedElement();

        this.customContextMenuPopup = Tools.createCubaTableContextMenu();
        this.customContextMenuPopup.setOwner(table);
        this.customContextMenuPopup.setWidget(this.customContextMenu);

        this.customContextMenuPopup.addCloseHandler(e -> {
            Element currentFocus = WidgetUtil.getFocusedElement();
            if (focusedElement != null && (currentFocus == null
                    || customContextMenuPopup.getElement().isOrHasChild(currentFocus)
                    || RootPanel.getBodyElement().equals(currentFocus))) {
                focusedElement.focus();
            }

            customContextMenuPopup = null;
        });

        Tools.showPopup(this.customContextMenuPopup, left, top);
    }

    public void showCustomPopup() {
        if (this.customPopupWidget != null) {
            if (this.customPopupWidget instanceof HasWidgets) {
                if (!((HasWidgets) this.customPopupWidget).iterator().hasNext()) {
                    // there are no component to show
                    return;
                }
            }

            // Store the currently focused element, which will be re-focused when
            // context menu is closed
            Element focusedElement = WidgetUtil.getFocusedElement();

            this.customPopupOverlay = Tools.createCubaTablePopup(this.customPopupAutoClose);
            this.customPopupOverlay.setOwner(table);
            this.customPopupOverlay.setWidget(this.customPopupWidget);

            this.customPopupOverlay.addCloseHandler(e -> {

                Element currentFocus = WidgetUtil.getFocusedElement();
                if (focusedElement != null && (currentFocus == null
                        || customPopupOverlay.getElement().isOrHasChild(currentFocus)
                        || RootPanel.getBodyElement().equals(currentFocus))) {
                    focusedElement.focus();
                }

                customPopupOverlay = null;
            });

            Tools.showPopup(this.customPopupOverlay, this.lastClickClientX, this.lastClickClientY);
        }
    }

    public void showSortMenu(final Element target, final String columnId) {
        final VOverlay sortDirectionPopup = GWT.create(VOverlay.class);
        sortDirectionPopup.setOwner(tableWidget.getOwner());

        FlowPanel sortDirectionMenu = new FlowPanel();
        Label sortByDescendingButton = new Label(tableWidget.getSortDescendingLabel());
        Label sortByAscendingButton = new Label(tableWidget.getSortAscendingLabel());
        Label sortClearSortButton = new Label(tableWidget.getSortResetLabel());

        sortByDescendingButton.addStyleName(TABLE_SORT_CONTEXTMENU_ITEM);
        sortByAscendingButton.addStyleName(TABLE_SORT_CONTEXTMENU_ITEM);
        sortClearSortButton.addStyleName(TABLE_SORT_CONTEXTMENU_ITEM);

        sortDirectionMenu.add(sortByAscendingButton);
        sortDirectionMenu.add(sortByDescendingButton);
        sortDirectionMenu.add(sortClearSortButton);

        sortByDescendingButton.addClickHandler(event -> {
            updateVariable("sortcolumn", columnId, false);
            updateVariable( "sortascending", false, false);

            tableWidget.getRowRequestHandler().deferRowFetch(); // some validation +
            // defer 250ms
            tableWidget.getRowRequestHandler().cancel(); // instead of waiting
            tableWidget.getRowRequestHandler().run(); // run immediately
            sortDirectionPopup.hide();
        });

        sortByAscendingButton.addClickHandler(event -> {
            updateVariable("sortcolumn", columnId, false);
            updateVariable("sortascending", true, false);

            tableWidget.getRowRequestHandler().deferRowFetch(); // some validation +
            // defer 250ms
            tableWidget.getRowRequestHandler().cancel(); // instead of waiting
            tableWidget.getRowRequestHandler().run(); // run immediately
            sortDirectionPopup.hide();
        });

        sortClearSortButton.addClickHandler(event -> {
            updateVariable( "resetsortorder", columnId, true);
            sortDirectionPopup.hide();
        });

        sortDirectionMenu.addStyleName("c-table-contextmenu");
        sortDirectionPopup.setWidget(sortDirectionMenu);

        sortDirectionPopup.setAutoHideEnabled(true);
        ComputedStyle sortIndicatorStyle = new ComputedStyle(target);

        Tools.showPopup(sortDirectionPopup, target.getAbsoluteLeft(), target.getAbsoluteTop() +
                ((int) sortIndicatorStyle.getHeight()));
    }

    private void updateVariable(String variableName, String newValue, boolean immediate) {
        tableWidget.getClient().updateVariable(tableWidget.getPaintableId(), variableName, newValue, immediate);
    }

    private void updateVariable(String variableName,  boolean newValue, boolean immediate) {
        tableWidget.getClient().updateVariable(tableWidget.getPaintableId(), variableName, newValue, immediate);
    }

    public void addAfterUpdateBodyListener(AfterBodyUpdateListener listener) {
        if (afterBodyUpdateListeners == null) {
            afterBodyUpdateListeners = new ArrayList<>();
        }

        if (!afterBodyUpdateListeners.contains(listener)) {
            afterBodyUpdateListeners.add(listener);
        }
    }

    public void removeAfterUpdateListener(AfterBodyUpdateListener listener) {
        if (afterBodyUpdateListeners == null) {
            return;
        }

        afterBodyUpdateListeners.remove(listener);

        if (afterBodyUpdateListeners.isEmpty()) {
            afterBodyUpdateListeners = null;
        }
    }

    public void handleUpdateBody() {
        if (afterBodyUpdateListeners == null
                || afterBodyUpdateListeners.isEmpty()) {
            return;
        }

        for (AfterBodyUpdateListener listener : afterBodyUpdateListeners) {
            listener.onAfterBodyUpdate();
        }
    }

    public boolean isAggregationVisible() {
        return aggregationRow != null
                && aggregationRow.isVisible();
    }

    public boolean isScrollToLastItemEnabled() {
        return scrollToLastItemEnabled;
    }

    public void setScrollToLastItemEnabled(boolean scrollToLastItemEnabled) {
        this.scrollToLastItemEnabled = scrollToLastItemEnabled;
    }
}