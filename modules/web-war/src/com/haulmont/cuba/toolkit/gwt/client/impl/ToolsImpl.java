/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * Author: Nikolay Gorodnov
 * Created: 19.12.2008 14:16:24
 * $Id$
 */
package com.haulmont.cuba.toolkit.gwt.client.impl;

import com.google.gwt.dom.client.Element;

public class ToolsImpl {

    public native int parseSize(String s) /*-{
        try {
            var result = /^(\d+)(%|px|em|ex|in|cm|mm|pt|pc)$/.exec(s);
            return parseInt(result[1]);
        } catch (e) {
            return -1;
        }
    }-*/;

    public native String format(String message) /*-{
        return message.replace(/\[br\]/g, "<br/>")
                .replace(/\[b\]/g, "<b>")
                .replace(/\[\/b\]/g, "</b>")
                .replace(/\[i\]/g, "<i>")
                .replace(/\[\/i\]/g, "</i>");
    }-*/;

    public native void setInnerHTML(Element elem, String text) /*-{
        elem.innerHTML = text; //todo gorodnov: support line breaks
    }-*/;

    public native void setInnerText(Element elem, String text) /*-{
        while (elem.firstChild) {
            elem.removeChild(elem.firstChild);
        }
        if (text != null) {
            var arr = new Array();
            arr = text.replace(/\r/g, "").split("\n");
            if (arr.length > 0) {
                for (var i = 0; i < arr.length; i++) {
                    if (arr[i]) {
                        elem.appendChild($doc.createTextNode(arr[i]));
                        elem.appendChild($doc.createElement("br"));
                    }
                }
            } else {
                elem.appendChild($doc.createTextNode(text));
            }
        }
    }-*/;

    public native boolean isRadio(Element e) /*-{
        return (e && e.tagName.toUpperCase() == "INPUT" && e.type == "radio");
    }-*/;

    public native boolean isCheckbox(Element e) /*-{
        return (e && e.tagName.toUpperCase() == "INPUT" && e.type == "checkbox");
    }-*/;

    public native void alert(String msg)/*-{
        $wnd.alert(msg);
    }-*/;

    public native void textSelectionEnable(Element el, boolean b) /*-{
        if (b) {
            $wnd.jQuery(el).enableTextSelect();
        } else {
            $wnd.jQuery(el).disableTextSelect();
        }
    }-*/;

    public native void updatePrimaryAndDependentStyleNames(Element elem,
                                                           String newPrimaryStyle) /*-{
        var classes = elem.className.split(/\s+/);
        if (!classes) {
            return;
        }

        var oldPrimaryStyle = classes[0];
        var oldPrimaryStyleLen = oldPrimaryStyle.length;

        classes[0] = newPrimaryStyle;
        for (var i = 1, n = classes.length; i < n; i++) {
            var name = classes[i];
            if (name.length > oldPrimaryStyleLen
                    && name.charAt(oldPrimaryStyleLen) == '-'
                    && name.indexOf(oldPrimaryStyle) == 0) {
                classes[i] = newPrimaryStyle + name.substring(oldPrimaryStyleLen);
            }
        }
        elem.className = classes.join(" ");
    }-*/;

    public native boolean hasStyleName(Element el, String style) /*-{
        var classes = elem.className.split(/\s+/);
        if (!classes) {
            return false;
        }
        for (var i = 0; i < classes.length; i++) {
            if (classes[i] == style) return true;
        }
        return false;
    }-*/;

}
