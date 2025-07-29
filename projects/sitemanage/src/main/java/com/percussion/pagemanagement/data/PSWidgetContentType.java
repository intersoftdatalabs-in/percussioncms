/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * POJO class to hold the widget and content type information.
 * @author Sunny Sal
 */
@XmlRootElement(name = "WidgetContentType")
@JsonRootName("WidgetContentType")
public class PSWidgetContentType {

    private String widgetId;
    private String widgetLabel;
    private String contentTypeId;
    private String contentTypeName;
    private String icon;

    public PSWidgetContentType() {}

    public PSWidgetContentType(String widgetId, String widgetLabel, String contentTypeId, String contentTypeName, String icon) {
        this.widgetId = widgetId;
        this.widgetLabel = widgetLabel;
        this.contentTypeId = contentTypeId;
        this.contentTypeName = contentTypeName;
        this.icon = icon;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public String getWidgetLabel() {
        return widgetLabel;
    }

    public void setWidgetLabel(String widgetLabel) {
        this.widgetLabel = widgetLabel;
    }

    public String getContentTypeId() {
        return contentTypeId;
    }

    public void setContentTypeId(String contentTypeId) {
        this.contentTypeId = contentTypeId;
    }

    public String getContentTypeName() {
        return contentTypeName;
    }

    public void setContentTypeName(String contentTypeName) {
        this.contentTypeName = contentTypeName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "PSWidgetContentType{" +
                "widgetId='" + widgetId + '\'' +
                ", widgetLabel='" + widgetLabel + '\'' +
                ", contentTypeId='" + contentTypeId + '\'' +
                ", contentTypeName='" + contentTypeName + '\'' +
                ", icon='" + icon + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSWidgetContentType)) return false;
        PSWidgetContentType that = (PSWidgetContentType) o;
        return Objects.equals(widgetId, that.widgetId) &&
                Objects.equals(widgetLabel, that.widgetLabel) &&
                Objects.equals(contentTypeId, that.contentTypeId) &&
                Objects.equals(contentTypeName, that.contentTypeName) &&
                Objects.equals(icon, that.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(widgetId, widgetLabel, contentTypeId, contentTypeName, icon);
    }
}
