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
package com.percussion.delivery.comments.data;

import java.util.Date;
import java.util.Set;

/**
 * Represents a REST comment in the system.
 * @author erikserating
 */
public class PSRestComment implements IPSComment {

    private APPROVAL_STATE approvalState = APPROVAL_STATE.APPROVED;
    private Date createdDate;
    private String id;
    private String pagePath;
    private String email;
    private String username;
    private String text;
    private String title;
    private String parent;
    private Set<String> tags;
    private boolean moderated;
    private boolean viewed;
    private String site;
    private String url;
    private String commentCreatedDate;

    public PSRestComment() {
        // Default constructor
    }

    /**
     * Creates a new comment by copying values from the given comment.
     * @param comment The comment to copy.
     */
    public PSRestComment(IPSComment comment) {
        this.id = comment.getId();
        this.approvalState = comment.getApprovalState();
        this.createdDate = comment.getCreatedDate();
        this.email = comment.getEmail();
        this.moderated = comment.isModerated();
        this.pagePath = comment.getPagePath();
        this.parent = comment.getParent();
        this.site = comment.getSite();
        setTags(comment.getTags());
        this.text = comment.getText();
        this.title = comment.getTitle();
        this.url = comment.getUrl();
        this.username = comment.getUsername();
        this.viewed = comment.isViewed();
        this.commentCreatedDate = comment.getCommentCreatedDate();
    }

    @Override
    public APPROVAL_STATE getApprovalState() {
        return approvalState;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPagePath() {
        return pagePath;
    }

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public void setApprovalState(APPROVAL_STATE approvalState) {
        this.approvalState = approvalState;
    }

    @Override
    public boolean isModerated() {
        return moderated;
    }

    @Override
    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }

    @Override
    public boolean isViewed() {
        return viewed;
    }

    @Override
    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    @Override
    public String getSite() {
        return site;
    }

    @Override
    public void setSite(String site) {
        this.site = site;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getCommentCreatedDate() {
        return commentCreatedDate;
    }

    @Override
    public void setCommentCreatedDate(String commentCreatedDate) {
        this.commentCreatedDate = commentCreatedDate;
    }
}

