/*
 * Copyright 2000-2014 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.client.extensions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.LayoutManager;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.layout.ElementResizeEvent;
import com.vaadin.client.ui.layout.ElementResizeListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.Connect;

/**
 * The client side connector for the Responsive extension.
 * 
 * @author Vaadin Ltd
 * @since 7.2
 */
@SuppressWarnings("GwtInconsistentSerializableClass")
@Connect(Responsive.class)
public class ResponsiveConnector extends AbstractExtensionConnector implements
        ElementResizeListener {

    /**
     * The target component which we will monitor for width changes
     */
    protected AbstractComponentConnector target;

    /**
     * All the width breakpoints found for this particular instance
     */
    protected JavaScriptObject widthBreakpoints;

    /**
     * All the height breakpoints found for this particular instance
     */
    protected JavaScriptObject heightBreakpoints;

    /**
     * All width-range breakpoints found from the style sheets on the page.
     * Common for all instances.
     */
    protected static JavaScriptObject widthRangeCache;

    /**
     * All height-range breakpoints found from the style sheets on the page.
     * Common for all instances.
     */
    protected static JavaScriptObject heightRangeCache;

    private static Logger getLogger() {
        return Logger.getLogger(ResponsiveConnector.class.getName());
    }

    private static void error(String message) {
        getLogger().log(Level.SEVERE, message);
    }

    @Override
    protected void extend(ServerConnector target) {
        // Initialize cache if not already done
        if (widthRangeCache == null) {
            searchForBreakPoints();
        }

        this.target = (AbstractComponentConnector) target;

        // Get any breakpoints from the styles defined for this widget
        getBreakPointsFor(constructSelectorsForTarget());

        // Start listening for size changes
        LayoutManager.get(getConnection()).addElementResizeListener(
                this.target.getWidget().getElement(), this);
    }

    /**
     * Construct the list of selectors that should be matched against in the
     * range selectors
     * 
     * @return The selectors in a comma delimited string.
     */
    protected String constructSelectorsForTarget() {
        String primaryStyle = target.getState().primaryStyleName;
        StringBuilder selectors = new StringBuilder();
        selectors.append(".").append(primaryStyle);

        if (target.getState().styles != null
                && target.getState().styles.size() > 0) {
            for (String style : target.getState().styles) {
                selectors.append(",.").append(style);
                selectors.append(",.").append(primaryStyle).append(".")
                        .append(style);
                selectors.append(",.").append(style).append(".")
                        .append(primaryStyle);
                selectors.append(",.").append(primaryStyle).append("-")
                        .append(style);
            }
        }

        // Allow the ID to be used as the selector as well for ranges
        if (target.getState().id != null) {
            selectors.append(",#").append(target.getState().id);
        }
        return selectors.toString();
    }

    @Override
    public void onUnregister() {
        super.onUnregister();
        LayoutManager.get(getConnection()).removeElementResizeListener(
                target.getWidget().getElement(), this);
    }

    /**
     * Build a cache of all 'width-range' and 'height-range' attribute selectors
     * found in the stylesheets.
     */
    private static native void searchForBreakPoints()
    /*-{

        // Initialize variables
        @com.vaadin.client.extensions.ResponsiveConnector::widthRangeCache = [];
        @com.vaadin.client.extensions.ResponsiveConnector::heightRangeCache = [];

        var widthRanges = @com.vaadin.client.extensions.ResponsiveConnector::widthRangeCache;
        var heightRanges = @com.vaadin.client.extensions.ResponsiveConnector::heightRangeCache;

        // Can't do squat if we can't parse stylesheets
        if(!$doc.styleSheets)
            return;

        var sheets = $doc.styleSheets;

        // Loop all stylesheets on the page and process them individually
        for(var i = 0, len = sheets.length; i < len; i++) {
            var sheet = sheets[i];
            @com.vaadin.client.extensions.ResponsiveConnector::searchStylesheetForBreakPoints(Lcom/google/gwt/core/client/JavaScriptObject;)(sheet);
        }

    }-*/;

    /**
     * Process an individual stylesheet object. Any @import statements are
     * handled recursively. Regular rule declarations are searched for
     * 'width-range' and 'height-range' attribute selectors.
     * 
     * @param sheet
     */
    private static native void searchStylesheetForBreakPoints(
            final JavaScriptObject sheet)
    /*-{

        // Inline variables for easier reading
        var widthRanges = @com.vaadin.client.extensions.ResponsiveConnector::widthRangeCache;
        var heightRanges = @com.vaadin.client.extensions.ResponsiveConnector::heightRangeCache;

        // Get all the rulesets from the stylesheet
        var theRules = new Array();
        var IE = @com.vaadin.client.BrowserInfo::get()().@com.vaadin.client.BrowserInfo::isIE()();
        var IE8 = @com.vaadin.client.BrowserInfo::get()().@com.vaadin.client.BrowserInfo::isIE8()();

        if (sheet.cssRules) {
            theRules = sheet.cssRules
        } else if (sheet.rules) {
            theRules = sheet.rules
        }

        // Special import handling for IE8
        if (IE8) {
            try {
                for(var i = 0, len = sheet.imports.length; i < len; i++) {
                    @com.vaadin.client.extensions.ResponsiveConnector::searchStylesheetForBreakPoints(Lcom/google/gwt/core/client/JavaScriptObject;)(sheet.imports[i]);
                }
            } catch(e) {
                // This is added due to IE8 failing to handle imports of some sheets for unknown reason (throws a permission denied exception)
                @com.vaadin.client.extensions.ResponsiveConnector::error(Ljava/lang/String;)("Failed to handle imports of CSS style sheet: " + sheet.href);
            }
        }

        // Loop through the rulesets
        for(var i = 0, len = theRules.length; i < len; i++) {
            var rule = theRules[i];

            if(rule.type == 3) {
                // @import rule, traverse recursively
                @com.vaadin.client.extensions.ResponsiveConnector::searchStylesheetForBreakPoints(Lcom/google/gwt/core/client/JavaScriptObject;)(rule.styleSheet);

            } else if(rule.type == 1 || !rule.type) {
                // Regular selector rule

                // IE parses CSS like .class[attr="val"] into [attr="val"].class so we need to check for both

                // Pattern for matching [width-range] selectors
                var widths = IE? /\[width-range~?=["|'](.*)-(.*)["|']\]([\.|#]\S+)/i : /([\.|#]\S+)\[width-range~?=["|'](.*)-(.*)["|']\]/i;

                // Patter for matching [height-range] selectors
                var heights = IE? /\[height-range~?=["|'](.*)-(.*)["|']\]([\.|#]\S+)/i : /([\.|#]\S+)\[height-range~?=["|'](.*)-(.*)["|']\]/i;

                // Array of all of the separate selectors in this ruleset
                var haystack = rule.selectorText.split(",");

                // Loop all the selectors in this ruleset
                for(var k = 0, len2 = haystack.length; k < len2; k++) {
                    var result;

                    // Check for width-range matches
                    if(result = haystack[k].match(widths)) {
                        var selector = IE? result[3] : result[1]
                        var min = IE? result[1] : result[2];
                        var max = IE? result[2] : result[3];

                        // Avoid adding duplicates
                        var duplicate = false;
                        for(var l = 0, len3 = widthRanges.length; l < len3; l++) {
                            var bp = widthRanges[l];
                            if(selector == bp[0] && min == bp[1] && max == bp[2]) {
                                duplicate = true;
                                break;
                            }
                        }
                        if(!duplicate) {
                            widthRanges.push([selector, min, max]);
                        }
                    }

                    // Check for height-range matches
                    if(result = haystack[k].match(heights)) {
                        var selector = IE? result[3] : result[1]
                        var min = IE? result[1] : result[2];
                        var max = IE? result[2] : result[3];

                        // Avoid adding duplicates
                        var duplicate = false;
                        for(var l = 0, len3 = heightRanges.length; l < len3; l++) {
                            var bp = heightRanges[l];
                            if(selector == bp[0] && min == bp[1] && max == bp[2]) {
                                duplicate = true;
                                break;
                            }
                        }
                        if(!duplicate) {
                            heightRanges.push([selector, min, max]);
                        }
                    }
                }
            }
        }

    }-*/;

    /**
     * Get all matching ranges from the cache for this particular instance.
     * 
     * @param selectors
     */
    private native void getBreakPointsFor(final String selectors)
    /*-{

        var selectors = selectors.split(",");

        var widthBreakpoints = this.@com.vaadin.client.extensions.ResponsiveConnector::widthBreakpoints = [];
        var heightBreakpoints = this.@com.vaadin.client.extensions.ResponsiveConnector::heightBreakpoints = [];

        var widthRanges = @com.vaadin.client.extensions.ResponsiveConnector::widthRangeCache;
        var heightRanges = @com.vaadin.client.extensions.ResponsiveConnector::heightRangeCache;

        for(var i = 0, len = widthRanges.length; i < len; i++) {
            var bp = widthRanges[i];
            for(var j = 0, len2 = selectors.length; j < len2; j++) {
                if(bp[0] == selectors[j])
                    widthBreakpoints.push(bp);
            }
        }

        for(var i = 0, len = heightRanges.length; i < len; i++) {
            var bp = heightRanges[i];
            for(var j = 0, len2 = selectors.length; j < len2; j++) {
                if(bp[0] == selectors[j])
                    heightBreakpoints.push(bp);
            }
        }

        // Only for debugging
        // console.log("Breakpoints for", selectors.join(","), widthBreakpoints, heightBreakpoints);

    }-*/;

    private String currentWidthRanges;
    private String currentHeightRanges;

    @Override
    public void onElementResize(ElementResizeEvent event) {
        int width = event.getLayoutManager().getOuterWidth(event.getElement());
        int height = event.getLayoutManager()
                .getOuterHeight(event.getElement());

        com.google.gwt.user.client.Element element = target.getWidget()
                .getElement();
        boolean forceRedraw = false;

        // Loop through breakpoints and see which one applies to this width
        currentWidthRanges = resolveBreakpoint("width", width,
                event.getElement());

        if (!"".equals(currentWidthRanges)) {
            target.getWidget().getElement()
                    .setAttribute("width-range", currentWidthRanges);
            forceRedraw = true;
        } else {
            element.removeAttribute("width-range");
        }

        // Loop through breakpoints and see which one applies to this height
        currentHeightRanges = resolveBreakpoint("height", height,
                event.getElement());

        if (!"".equals(currentHeightRanges)) {
            target.getWidget().getElement()
                    .setAttribute("height-range", currentHeightRanges);
            forceRedraw = true;
        } else {
            element.removeAttribute("height-range");
        }

        if (forceRedraw) {
            forceRedrawIfIE8(element);
        }
    }

    /**
     * Forces IE8 to reinterpret CSS rules.
     * {@link com.vaadin.client.Util#forceIE8Redraw(com.google.gwt.dom.client.Element)}
     * doesn't work in this case.
     * 
     * @param element
     *            the element to redraw
     */
    private void forceRedrawIfIE8(Element element) {
        if (BrowserInfo.get().isIE8()) {
            element.addClassName("foo");
            element.removeClassName("foo");
        }
    }

    private native String resolveBreakpoint(String which, int size,
            Element element)
    /*-{

        // Default to "width" breakpoints
        var breakpoints = this.@com.vaadin.client.extensions.ResponsiveConnector::widthBreakpoints;

        // Use height breakpoints if we're measuring the height
        if(which == "height")
            breakpoints = this.@com.vaadin.client.extensions.ResponsiveConnector::heightBreakpoints;

        // Output string that goes into either the "width-range" or "height-range" attribute in the element
        var ranges = "";

        // Loop the breakpoints
        for(var i = 0, len = breakpoints.length; i < len; i++) {
            var bp = breakpoints[i];

            var min = parseInt(bp[1]);
            var max = parseInt(bp[2]);

            if(min && max) {
                if(min <= size && size <= max) {
                    ranges += " " + bp[1] + "-" + bp[2];
                }
            } else if (min) {
                if(min <= size) {
                    ranges += " " + bp[1] + "-";
                }
            } else if (max) {
                if (size <= max) {
                    ranges += " -" + bp[2];
                }
            }
        }

        // Trim the output and return it
        return ranges.replace(/^\s+/, "");

    }-*/;

}
