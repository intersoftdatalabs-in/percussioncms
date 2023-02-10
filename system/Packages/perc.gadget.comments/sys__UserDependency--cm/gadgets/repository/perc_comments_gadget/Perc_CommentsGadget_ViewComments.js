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

(function($){
    /**
     * Creates a table using jQuery.dataTable.js
     * A lot of this functionality should be pulled out and refactored into a central function, to provide guard rails for the developers.
     */
    function createDataTable(data, site, pagePath){

        var itemsPerPage = 999999999; // No paging support yet.  Change to sensible number once we implement it.
        var isLargeColumn = true;
        window.gadgets = false;
        var tableDiv = $('#perc-gadget-comments-viewComments-container');
        statusTable = $("<table id='perc-gadget-comments-viewComments-table' cellspacing='0'>");
        tableDiv.append(statusTable);

        var percData = [];

        $.PercCommentsGadgetService().getArticleDescription(site, pagePath, function(article, callbackOptions){

            var articleDesc = $('<div />').
            append(
                $('<div />').
                attr('id', 'perc-gadget-comments-viewComments-articleSummary').
                append(
                    $('<span />').
                    text(
                        $('<div />').
                        append(article.title).
                        text()
                    )
                ).append(
                    $('<span />').
                    text(
                        $('<div />').
                        append(article.summary).
                        text()
                    )
                )
            ).append(
                $('<div />').
                attr('id', 'perc-gadget-comments-viewComments-numComments').
                text(data.length + ((data.length>1)?' comments:':' comment:'))
            );

            var allModeration = newRejectApproveAllActions();

            var row =
                {
                    rowContent: [
                        [
                            {content: articleDesc.prop("outerHTML") }
                        ],
                        [
                            {content: allModeration.prop("outerHTML") }
                        ]
                    ]
                };

            percData.push(row);

            $.each(data, function(){
                var commentData = this;
                var commentTitle   = $('<h2 />').addClass('perc-gadget-comments-viewComments-commentTitle').text($('<div />').append(commentData.title).text());
                var commentDateRaw = $.timeago.parse(commentData.createdDate);
                var dateParts = $.perc_utils.splitDateTime(commentData.createdDate);

                var commentDate    = $('<span />').
                addClass('perc-gadget-comments-viewComments-commentDate').
                append(
                    $.timeago(
                        $('<span />').
                        text(commentData.createdDate).
                        attr('title', commentData.createdDate))
                ).append(
                    $('<span />').
                    text(

                        ', ' + dateParts.date + ", " + dateParts.time
                    )
                );
                var commentAuthor  = $('<span />').addClass('perc-gadget-comments-viewComments-commentAuthor').text($('<div />').append( ( commentData.username ? commentData.username : "Anonymous" ) ).text() + " said...");
                var commentText    = $('<div />').addClass('perc-gadget-comments-viewComments-commentText').text($('<div />').append(commentData.text).text());

                var commentAggregate = $('<div />').addClass('perc-datatable-firstrow').append(commentDate).append(commentAuthor).append(commentText);

                var commentId = commentData.id;
                var rejectApproveActions = newRejectApproveActions(commentData.approvalState, commentId, site);

                var row = {rowContent: [[ {content: commentAggregate.prop("outerHTML")}], [ {content: rejectApproveActions.prop("outerHTML")} ]] };

                percData.push(row);
            });

            var dataTypeCols0 = [
                { bSortable: false, sClass : "perc-datatable-cell-string perc-gadget-comments-viewComments-comment" },
                { bSortable: false, sClass : "perc-datatable-cell-action perc-gadget-comments-viewComments-action" }
            ];
            var dataTypeCols1 = [
                { bSortable: false, sClass : "perc-datatable-cell-string perc-gadget-comments-viewComments-comment" },
                { bSortable: false, sClass : "perc-datatable-cell-action perc-gadget-comments-viewComments-action" }
            ];
            var dataTypeCols = isLargeColumn?dataTypeCols1:dataTypeCols0;

            var tableConfig = {
                iDisplayLength : itemsPerPage,
                aoColumns: dataTypeCols,
                percHeaderClasses : ["perc-datatable-cell-string perc-gadget-comments-viewComments-comment", "perc-datatable-cell-string perc-gadget-comments-viewComments-action"],
                percHeaders : ["<span>Title</span>", "<span>Action</span>"],
                percData : percData,
                oLanguage: {"sZeroRecords": "No Comments Found", "sInfo" : "Showing _START_ to _END_ of _TOTAL_ total results", "sInfoEmpty" : "No results found"},
                percColumnWidths : [["82%", "18%"]],
                aaSorting : [],
                singlePage: true
            };

            statusTable.PercDataTable(tableConfig);

            console.log(statusTable);

            statusTable.on("click", 'div.perc-gadget-comments-rejectAction-all', rejectAll);
            statusTable.on("click", 'div.perc-gadget-comments-approveAction-all', approveAll);
            statusTable.on("click", 'div.perc-gadget-comments-deleteAction-all', deleteAll);

            statusTable.on("click", 'div.perc-gadget-comments-rejectAction', reject);
            statusTable.on("click", 'div.perc-gadget-comments-approveAction', approve);
            statusTable.on("click", 'div.perc-gadget-comments-deleteAction', deleteComment);

            // if the last row does not fill the bottom of the dialog
            // expand the height of the last row to the bottom of the dialog
            var container = $("#perc-gadget-comments-viewComments-container");
            var containerHeight = container.height();
            var lastRow = $("td.perc-gadget-comments-viewComments-action:last");
            var lastRowTop = lastRow.position().top;
            var lastRowHeight = lastRow.height();
            var lastRowNewHeight = containerHeight - lastRowTop;
            if(lastRowNewHeight > lastRowHeight)
                lastRow.height(lastRowNewHeight);

            $(window).trigger('perc-datatable-doneLoading');

            updateApproveRejectAllActions();
        }, {});
    }

    function newRejectApproveAllActions() {
        var approvalActionMenu          =  $("<div class='perc-gadget-comments-actionMenu-all'>");
        var approvalActionRejectAction  = $("<div class='perc-gadget-comments-rejectAction-all'  title='Reject All Comments' />");
        var approvalActionApproveAction = $("<div class='perc-gadget-comments-approveAction-all' title='Approve All Comments' />");
        var approvalActionDeleteAction  = $("<div class='perc-gadget-comments-deleteAction-all' title='Delete All Comments' />");

        var approvalActionAllLabel      = $("<div class='perc-gadget-comments-label-all'>ALL</div></div>");

        approvalActionMenu
            .append(approvalActionRejectAction)
            .append(approvalActionApproveAction)
            .append(approvalActionDeleteAction)
            .append(approvalActionAllLabel)
            .addClass("perc-gadget-comments-default-all");

        approvalActionDeleteAction.on("click", deleteAll);

        return approvalActionMenu;
    }

    function deleteAll() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "ALL_DELETED")
            .removeClass("perc-gadget-comments-approved-all")
            .removeClass("perc-gadget-comments-rejected-all")
            .addClass("perc-gadget-comments-deleted-all");

        var allIndividualMenus = $(".perc-gadget-comments-actionMenu");
        allIndividualMenus
            .attr("currentState", "DELETED")
            .removeClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-rejected")
            .addClass("perc-gadget-comments-deleted");
    }

    function rejectAll() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "ALL_REJECTED")
            .removeClass("perc-gadget-comments-approved-all")
            .removeClass("perc-gadget-comments-deleted-all")
            .addClass("perc-gadget-comments-rejected-all");

        var allIndividualMenus = $(".perc-gadget-comments-actionMenu");
        allIndividualMenus
            .attr("currentState", "REJECTED")
            .removeClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-deleted")
            .addClass("perc-gadget-comments-rejected");
    }

    function approveAll() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "ALL_APPROVED")
            .addClass("perc-gadget-comments-approved-all")
            .removeClass("perc-gadget-comments-deleted-all")
            .removeClass("perc-gadget-comments-rejected-all");

        var allIndividualMenus = $(".perc-gadget-comments-actionMenu");
        allIndividualMenus
            .attr("currentState", "APPROVED")
            .addClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-deleted")
            .removeClass("perc-gadget-comments-rejected");
    }

    function newRejectApproveActions(approvalState, commentId, site) {

        var approvalStateClass          = approvalState == "APPROVED" ? "perc-gadget-comments-approved" : "perc-gadget-comments-rejected";
        var approvalStateClassOriginal  = approvalStateClass;

        var approvalActionMenu          = $("<div class='perc-gadget-comments-actionMenu' site='"+site+"' commentId='"+commentId+"'>");
        var approvalActionRejectAction  = $("<div class='perc-gadget-comments-rejectAction'  title='Reject Comment'>");
        var approvalActionApproveAction = $("<div class='perc-gadget-comments-approveAction' title='Approve Comment'>");
        var approvalActionDeleteAction  = $("<div class='perc-gadget-comments-deleteAction' title='Delete Comment'>");

        approvalActionMenu
            .append(approvalActionRejectAction)
            .append(approvalActionApproveAction)
            .append(approvalActionDeleteAction)
            .addClass(approvalStateClass)
            .attr("currentState",  approvalState)
            .attr("originalState", approvalState);

        return approvalActionMenu;
    }

    function reject() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "REJECTED")
            .removeClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-deleted")
            .addClass("perc-gadget-comments-rejected");
        updateApproveRejectAllActions();
    }

    function approve() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "APPROVED")
            .addClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-deleted")
            .removeClass("perc-gadget-comments-rejected");
        updateApproveRejectAllActions();
    }

    function deleteComment() {
        var approvalActionMenu = $(this).parent();
        approvalActionMenu
            .attr("currentState", "DELETED")
            .addClass("perc-gadget-comments-deleted")
            .removeClass("perc-gadget-comments-approved")
            .removeClass("perc-gadget-comments-rejected");
        updateApproveRejectAllActions();
    }

    function updateApproveRejectAllActions() {
        var allIndividualMenus = $(".perc-gadget-comments-actionMenu");
        var individualMenuCount = allIndividualMenus.length;
        var allTheSame = true;
        var firstState = $(allIndividualMenus[0]).attr("currentState");
        $.each(allIndividualMenus, function(){
            var action = $(this);
            var state = action.attr("currentState");
            if(state !== firstState) {
                allTheSame = false;
                return false;
            }
        });
        var approveRejectAllAction = $(".perc-gadget-comments-actionMenu-all");
        if(allTheSame) {
            if(firstState == "APPROVED") {
                approveRejectAllAction
                    .attr("currentState", "ALL_APPROVED")
                    .addClass("perc-gadget-comments-approved-all")
                    .removeClass("perc-gadget-comments-deleted-all")
                    .removeClass("perc-gadget-comments-rejected-all");
            } if(firstState == "REJECTED") {
                approveRejectAllAction
                    .attr("currentState", "ALL_REJECTED")
                    .addClass("perc-gadget-comments-rejected-all")
                    .removeClass("perc-gadget-comments-deleted-all")
                    .removeClass("perc-gadget-comments-approved-all");
            } if(firstState == "DELETED") {
                approveRejectAllAction
                    .attr("currentState", "ALL_DELETED")
                    .addClass("perc-gadget-comments-deleted-all")
                    .removeClass("perc-gadget-comments-rejected-all")
                    .removeClass("perc-gadget-comments-approved-all");
            }
        } else {
            approveRejectAllAction
                .attr("currentState", "")
                .removeClass("perc-gadget-comments-approved-all")
                .removeClass("perc-gadget-comments-rejected-all")
                .removeClass("perc-gadget-comments-deleted-all")
                .addClass("perc-gadget-comments-default-all");
        }
    }

    $(document).ready(function(){

        var site     = $(document).getUrlParam('site');
        var pagePath = $(document).getUrlParam('pagePath');
        window.parent.jQuery("#" + window.name).parent().css('padding', '0');
        $.PercCommentsGadgetService().getCommentsOnPage(site, pagePath, function(comments, callbackOptions){
            createDataTable(comments, site, pagePath);
        }, {});
    });
})(jQuery);