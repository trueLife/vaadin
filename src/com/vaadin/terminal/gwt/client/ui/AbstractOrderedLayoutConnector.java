/* 
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.DirectionalManagedLayout;
import com.vaadin.terminal.gwt.client.LayoutManager;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VCaption;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.ui.layout.ComponentConnectorLayoutSlot;
import com.vaadin.terminal.gwt.client.ui.layout.VLayoutSlot;

public abstract class AbstractOrderedLayoutConnector extends
        AbstractComponentContainerConnector implements DirectionalManagedLayout {

    @Override
    public void init() {
        getLayoutManager().registerDependency(this,
                getWidget().spacingMeasureElement);
    }

    public void updateCaption(ComponentConnector component, UIDL uidl) {
        VMeasuringOrderedLayout layout = getWidget();
        if (VCaption.isNeeded(uidl, component.getState())) {
            VLayoutSlot layoutSlot = layout.getSlotForChild(component
                    .getWidget());
            VCaption caption = layoutSlot.getCaption();
            if (caption == null) {
                caption = new VCaption(component, getConnection());

                Widget widget = component.getWidget();

                layout.setCaption(widget, caption);
            }
            caption.updateCaption(uidl);
        } else {
            layout.setCaption(component.getWidget(), null);
            getLayoutManager().setNeedsUpdate(this);
        }
    }

    @Override
    public VMeasuringOrderedLayout getWidget() {
        return (VMeasuringOrderedLayout) super.getWidget();
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        super.updateFromUIDL(uidl, client);
        if (!isRealUpdate(uidl)) {
            return;
        }

        VMeasuringOrderedLayout layout = getWidget();

        ValueMap expandRatios = uidl.getMapAttribute("expandRatios");
        ValueMap alignments = uidl.getMapAttribute("alignments");

        for (ComponentConnector child : getChildren()) {
            VLayoutSlot slot = layout.getSlotForChild(child.getWidget());
            String pid = child.getConnectorId();

            AlignmentInfo alignment;
            if (alignments.containsKey(pid)) {
                alignment = new AlignmentInfo(alignments.getInt(pid));
            } else {
                alignment = AlignmentInfo.TOP_LEFT;
            }
            slot.setAlignment(alignment);

            double expandRatio;
            if (expandRatios.containsKey(pid)) {
                expandRatio = expandRatios.getRawNumber(pid);
            } else {
                expandRatio = 0;
            }
            slot.setExpandRatio(expandRatio);
        }

        int bitMask = uidl.getIntAttribute("margins");
        layout.updateMarginStyleNames(new VMarginInfo(bitMask));

        layout.updateSpacingStyleName(uidl.getBooleanAttribute("spacing"));

        getLayoutManager().setNeedsUpdate(this);
    }

    private int getSizeForInnerSize(int size, boolean isVertical) {
        LayoutManager layoutManager = getLayoutManager();
        Element element = getWidget().getElement();
        if (isVertical) {
            return size + layoutManager.getBorderHeight(element)
                    + layoutManager.getPaddingHeight(element);
        } else {
            return size + layoutManager.getBorderWidth(element)
                    + layoutManager.getPaddingWidth(element);
        }
    }

    private static String getSizeProperty(boolean isVertical) {
        return isVertical ? "height" : "width";
    }

    private boolean isUndefinedInDirection(boolean isVertical) {
        if (isVertical) {
            return isUndefinedHeight();
        } else {
            return isUndefinedWidth();
        }
    }

    private int getInnerSizeInDirection(boolean isVertical) {
        if (isVertical) {
            return getLayoutManager().getInnerHeight(getWidget().getElement());
        } else {
            return getLayoutManager().getInnerWidth(getWidget().getElement());
        }
    }

    private void layoutPrimaryDirection() {
        VMeasuringOrderedLayout layout = getWidget();
        boolean isVertical = layout.isVertical;
        boolean isUndefined = isUndefinedInDirection(isVertical);

        int startPadding = getStartPadding(isVertical);
        int spacingSize = getSpacingInDirection(isVertical);
        int allocatedSize;

        if (isUndefined) {
            allocatedSize = -1;
        } else {
            allocatedSize = getInnerSizeInDirection(isVertical);
        }

        allocatedSize = layout.layoutPrimaryDirection(spacingSize,
                allocatedSize, startPadding);

        Style ownStyle = getWidget().getElement().getStyle();
        if (isUndefined) {
            ownStyle.setPropertyPx(getSizeProperty(isVertical),
                    getSizeForInnerSize(allocatedSize, isVertical));
        } else {
            ownStyle.setProperty(getSizeProperty(isVertical),
                    getDefinedSize(isVertical));
        }
    }

    private int getSpacingInDirection(boolean isVertical) {
        if (isVertical) {
            return getLayoutManager().getOuterHeight(
                    getWidget().spacingMeasureElement);
        } else {
            return getLayoutManager().getOuterWidth(
                    getWidget().spacingMeasureElement);
        }
    }

    private void layoutSecondaryDirection() {
        VMeasuringOrderedLayout layout = getWidget();
        boolean isVertical = layout.isVertical;
        boolean isUndefined = isUndefinedInDirection(!isVertical);

        int startPadding = getStartPadding(!isVertical);

        int allocatedSize;
        if (isUndefined) {
            allocatedSize = -1;
        } else {
            allocatedSize = getInnerSizeInDirection(!isVertical);
        }

        allocatedSize = layout.layoutSecondaryDirection(allocatedSize,
                startPadding);

        Style ownStyle = getWidget().getElement().getStyle();

        if (isUndefined) {
            ownStyle.setPropertyPx(getSizeProperty(!getWidget().isVertical),
                    getSizeForInnerSize(allocatedSize, !getWidget().isVertical));
        } else {
            ownStyle.setProperty(getSizeProperty(!getWidget().isVertical),
                    getDefinedSize(!getWidget().isVertical));
        }
    }

    private String getDefinedSize(boolean isVertical) {
        if (isVertical) {
            return getDeclaredHeight();
        } else {
            return getDeclaredWidth();
        }
    }

    private int getStartPadding(boolean isVertical) {
        if (isVertical) {
            return getLayoutManager().getPaddingTop(getWidget().getElement());
        } else {
            return getLayoutManager().getPaddingLeft(getWidget().getElement());
        }
    }

    public void layoutHorizontally() {
        if (getWidget().isVertical) {
            layoutSecondaryDirection();
        } else {
            layoutPrimaryDirection();
        }
    }

    public void layoutVertically() {
        if (getWidget().isVertical) {
            layoutPrimaryDirection();
        } else {
            layoutSecondaryDirection();
        }
    }

    @Override
    public void connectorHierarchyChanged(
            com.vaadin.terminal.gwt.client.ConnectorHierarchyChangedEvent event) {
        super.connectorHierarchyChanged(event);
        List<ComponentConnector> previousChildren = event.getOldChildren();
        int currentIndex = 0;
        VMeasuringOrderedLayout layout = getWidget();

        for (ComponentConnector child : getChildren()) {
            Widget childWidget = child.getWidget();
            VLayoutSlot slot = layout.getSlotForChild(childWidget);

            if (childWidget.getParent() != layout) {
                // If the child widget was previously attached to another
                // AbstractOrderedLayout a slot might be found that belongs to
                // another AbstractOrderedLayout. In this case we discard it and
                // create a new slot.
                slot = new ComponentConnectorLayoutSlot(getWidget()
                        .getStylePrimaryName(), child);
            }
            layout.addOrMove(slot, currentIndex++);
        }

        // TODO: Move this to layout and base it on DOM and "currentIndex" 
        for (ComponentConnector child : previousChildren) {
            Widget widget = child.getWidget();
            if (child.getParent() != this) {
                // Remove slot if the connector is no longer a child of this
                // layout
                layout.removeSlot(layout.getSlotForChild(widget));
            }
        }

    };

}