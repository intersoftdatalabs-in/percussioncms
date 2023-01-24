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

/**
 * Schedules Dialog
 */
(function($){
    $.PercScheduleDialog = {
        open: openDialog
    };
    /**
     * Opens the schedule dialog.
     * @param itemId(String), assumed to be a valid guid of the item (Page or Asset)
     */
    function openDialog(itemId)
    {
        var itemId = itemId;
        //Makes a service call and gets the dates
        $.PercItemPublisherService.getScheduleDates(itemId,function(status, result){
            var scheduleDates = eval("(" + result + ")").ItemDates;
            createDialog("OK", {'publishDate':scheduleDates.startDate, 'removalDate':scheduleDates.endDate});
        });


        var dialog;
        function createDialog(status,result)
        {
            var self = this;

            if(status === $.PercServiceUtils.STATUS_ERROR)
            {
                var defaultMsg = $.PercServiceUtils.extractDefaultErrorMessage(result.request);
                $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: defaultMsg});
                callback(false);
                return;
            }

            var dialogHTML = createSchedule(result);
            dialog = $(dialogHTML).perc_dialog( {
                resizable : false,
                title: I18N.message("perc.ui.schedule.dialog@Schedule"),
                modal: true,
                closeOnEscape : true,
                percButtons:{
                    "Save":{
                        click: function(){

                            var startDate = $("#perc_publish_date_input").val();
                            var endDate = $("#perc_removal_date").val();

                            $.datepicker._hideDatepicker();
                            dialog.remove();
                            showCommentsDialog(itemId,startDate,endDate);



                        },
                        id: "perc-schedule-dialog-save"
                    },
                    "Cancel":{
                        click: function(){
                            $.datepicker._hideDatepicker();
                            dialog.remove();
                        },
                        id: "perc-schedule-dialog-cancel"
                    }
                },
                id: "perc-schedule-dialog",
                open:function(){
                    attachFormDatepicker();
                },
                width: 350,
                height: 312
            });
            $("#ui-datepicker-div").css("z-index", "100000");
            $("#ui-timepicker-div").css("z-index", "100000");
            $("#ui-datepicker-div").wrap('<div class="perc-schedule" />');
            $("#ui-timepicker-div").wrap('<div class="perc-schedule" />');
        }

        function createSchedule(result)
        {
            var labelPublish =  $('<div id = "perc-publish-label"/>').
            append(
                $('<label/>').
                attr('for', 'publish_date')
                    .text('Publish date:')
            );
            var inputPublish =  $('<div/>').css('position', 'relative').
            append(
                $('<input/>').
                attr('type', 'text').
                css('width', '130px').
                css('height', '20px').
                attr('readonly', 'readonly').
                addClass('perc-datetime-picker').
                attr('id', 'perc_publish_date_input').
                attr('name', 'publish_date').
                val(result.publishDate)
            );
            var labelRemoval =   $('<div id = "perc-removal-label"/>').
            append(
                $('<label/>').
                attr('for', 'removal_date').
                text('Removal date:')
            );
            var inputRemoval =  $('<div/>').css('position', 'relative').
            append(
                $('<input/>').
                attr('type', 'text').
                css('width', '130px').
                css('height', '15px').
                attr('readonly', 'readonly').
                addClass('perc-datetime-picker').
                attr('id', 'perc_removal_date').
                attr('name', 'removal_date').
                val(result.removalDate)
            );
            var $dialogHtml = $('<div/>').addClass('perc-schedule').
            append(labelPublish).
            append(inputPublish).
            append(labelRemoval).
            append(inputRemoval).
            append($('<script/>')
            );
            return $dialogHtml;
        }

        /*
        Validate controls fields
        */
        function validateFields()
        {
            var publishDate = parseDateTime($('#perc_publish_date_input').val());
            var removalDate = parseDateTime($('#perc_removal_date').val());
            var serverTime = new Date();//parseDateTime($.PercServiceUtils.getServerDatetime());
            if (publishDate !== "" && removalDate !==""){
                //with -0 convert the time to milliseconds
                if (publishDate-0 === removalDate-0)
                {
                    $.perc_utils.alert_dialog({content:I18N.message("perc.ui.schedule.dialog@Dates Cannot Be Same"), title:I18N.message("perc.ui.publish.title@Error")});
                    return false;
                }
                if (publishDate-0 > removalDate-0)
                {
                    $.perc_utils.alert_dialog({content:I18N.messsage("perc.ui.schedule.dialog@Enter Valid Date Range"), title:I18N.message("perc.ui.publish.title@Error")});
                    return false;
                }
            }
            /*if (publishDate != "" && publishDate-0 < serverTime-0)
            {
                $.perc_utils.alert_dialog({content:"The Publishing time precedes the current time.", title:"Error"});
                return false;
            }
            if (removalDate != "" && removalDate-0 < serverTime-0)
            {
                $.perc_utils.alert_dialog({content:"The date range is invalid. The Removal date precedes the Publishing date. ", title:"Error"});
                return false;
            }*/
            return true;
        }

        function parseDateTime(strDateTime){
            if (strDateTime !== "")
            {
                var date = strDateTime.split(" ")[0].split("/");
                var time = strDateTime.split(" ")[1].split(":");
                //the month start in 0 (year, month, day, hour, min)
                return new Date(date[2], date[0]-1, date[1], time[0], time[1]);
            }
            return "";
        }

        //Attach a Jquery Datepicker  to Date field
        function attachFormDatepicker(){
            var dates = $('.perc-datetime-picker').each(function(){
                var options = {dlg_width:'350',dlg_height:'300',aarenderer:'MODAL',dummy : 0};
                options.inputName = $(this).attr('name');
                options.inputId = $(this).attr('id');
                $(this).datepicker({
                    showOn: "button",
                    buttonImage: "../images/images/buttonCalendar.gif",
                    duration: '',
                    dateFormat: "mm/dd/yy",
                    buttonImageOnly: true,
                    showTime: true,
                    constrainInput: true,
                    stepMinutes: 10,
                    stepHours: 1,
                    altTimeField: '',
                    time24h: false,
                    minDate: new Date(),
                    changeMonth: true,
                    changeYear: true,
                    buttonText: '',
                    // hook called when date is selected
                    // mark asset as dirty when date is selected
                    onSelect : function(dateText, inst) {
                        // if the top most jquery is defined
                        if($.topFrameJQuery !== undefined)
                            // mark the asset as dirty
                            $.topFrameJQuery.PercDirtyController.setDirty(true, "asset");
                    }
                })
                    .on('paste', function(evt){evt.preventDefault();})
                    .on('keypress keydown', function(evt){
                        if(evt.keyCode === 46 || evt.keyCode === 8 )
                        {
                            var field = evt.target;
                            field.value = "";
                            //made below change to clean up removal date and schedule date
                            $.datepicker._curInst="";
                            evt.preventDefault();
                            return;
                        }
                        if(evt.charCode === 0 || typeof(evt.charCode) == 'undefined')
                            return;
                        evt.preventDefault();
                    });
            });
        }

    }// End open dialog

    function showCommentsDialog(itemId,startDate,endDate){
        var buttons = {};
        buttons["Save"] = {
            click: function()
            {
                userComment = $("#perc-workflow-comment").val();
                commentDialog.remove();
                var sendDates = {"ItemDates":{"itemId":itemId,"startDate":startDate,"endDate":endDate,"comments":userComment}};
                commentDialog.remove();

                $.PercItemPublisherService.setScheduleDates(sendDates, function(status, results){
                    if(!status)
                    {
                        $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: results});
                    }else
                    {
                        commentDialog.remove();
                    }
                });
            },
            id: "perc-workflow-comment-ok"
        };
        buttons.Cancel = {
            click: function()
            {

                commentDialog.remove();
            },
            id: "perc-workflow-comment-cancel"
        };
        var userComment = "";
        var commentDialog = $("<div><div class='perc-workflow-comment-label' data='" + "Approve" + "'>" +I18N.message("perc.ui.finder.view@Enter Comments Limit") + "</div><textarea id=\"perc-workflow-comment\" name=\"perc-workflow-comment\" maxlength=\"500\"></textarea></div>")
            .perc_dialog(
                {
                    dialogClass: 'perc-workflow-comment-dialog',
                    title: I18N.message("perc.ui.finder.view@Enter Comments"),
                    modal: true,
                    resizable: false,
                    "percButtons" : buttons,
                    width:400,
                    id: "perc-workflow-comment-dialog"
                });
    }
})(jQuery);
