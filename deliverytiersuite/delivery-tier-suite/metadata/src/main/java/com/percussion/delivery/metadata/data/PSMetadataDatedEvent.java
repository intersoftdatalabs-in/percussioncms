// REFACTORED: CP-JAVA11
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
package com.percussion.delivery.metadata.data;

import java.util.Optional;
import org.apache.commons.lang.StringUtils;

/**
 * Contains the structure of the event information.
 * Properties:
 * <ul>
 *  <li>page title</li>
 *  <li>page summary</li>
 *  <li>page start date</li>
 *  <li>page end date</li>
 *  <li>page URL</li>
 * </ul>
 * Use {@link Builder} for construction.
 *
 * @author rafaelsalis
 */
public class PSMetadataDatedEvent {

    private String title;
    private String summary;
    private String start;
    private String end;
    private String url;
    private boolean allDay = false;
    private String textColor = StringUtils.EMPTY;
    private String textBackground = StringUtils.EMPTY;

    public PSMetadataDatedEvent() {}

    private PSMetadataDatedEvent(Builder builder) {
        this.title = builder.title;
        this.summary = builder.summary;
        this.start = builder.start;
        this.end = builder.end;
        this.url = builder.url;
        this.allDay = builder.allDay;
        this.textColor = builder.textColor;
        this.textBackground = builder.textBackground;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String summary;
        private String start;
        private String end;
        private String url;
        private boolean allDay = false;
        private String textColor = StringUtils.EMPTY;
        private String textBackground = StringUtils.EMPTY;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder start(String start) {
            this.start = start;
            return this;
        }

        public Builder end(String end) {
            this.end = end;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder allDay(boolean allDay) {
            this.allDay = allDay;
            return this;
        }

        public Builder textColor(String textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder textBackground(String textBackground) {
            this.textBackground = textBackground;
            return this;
        }

        public PSMetadataDatedEvent build() {
            return new PSMetadataDatedEvent(this);
        }
    }

    /**
     * @return the title of the page, never null or empty.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title sets the page title, never null or empty.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the page summary, may be null or empty if unknown.
     */
    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    /**
     * @param summary the page summary to set, may be null or empty.
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * @return the page start date, may be null or empty if unknown.
     */
    public Optional<String> getStart() {
        return Optional.ofNullable(start);
    }

    /**
     * @param start sets the page start date, may be null or empty.
     */
    public void setStart(String start) {
        this.start = start;
    }

    /**
     * @return the page end date, may be null or empty if unknown.
     */
    public Optional<String> getEnd() {
        return Optional.ofNullable(end);
    }

    /**
     * @param end sets the page end date, may be null or empty.
     */
    public void setEnd(String end) {
        this.end = end;
    }

    /**
     * @return the page URL, never null or empty.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url sets the page URL, never null or empty.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return true if event is all day.
     */
    public boolean isAllDay() {
        return allDay;
    }

    /**
     * @param allDay sets the all day flag.
     */
    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    /**
     * @return the text color, may be empty but never null.
     */
    public String getTextColor() {
        return textColor;
    }

    /**
     * @param textColor sets the text color, may be empty but never null.
     */
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    /**
     * @return the text background, may be empty but never null.
     */
    public String getTextBackground() {
        return textBackground;
    }

    /**
     * @param textBackground sets the text background, may be empty but never null.
     */
    public void setTextBackground(String textBackground) {
        this.textBackground = textBackground;
    }
}
