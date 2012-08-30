/* 
 * Copyright 2011 Vaadin Ltd.
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

package com.vaadin.tests.components.window;

import com.vaadin.terminal.WrappedRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class TooltipInWindow extends AbstractTestUI {

    @Override
    protected void setup(WrappedRequest request) {
        Window window = new Window("Window");
        window.getContent().setSizeUndefined();
        window.center();
        window.addComponent(createTextField());

        addWindow(window);
        addComponent(createTextField());
    }

    private TextField createTextField() {
        TextField tf = new TextField("TextField with a tooltip");
        tf.setDescription("My tooltip");
        return tf;
    }

    @Override
    protected String getTestDescription() {
        return "Tooltips should also work in a Window (as well as in other overlays)";
    }

    @Override
    protected Integer getTicketNumber() {
        return Integer.valueOf(9172);
    }

}