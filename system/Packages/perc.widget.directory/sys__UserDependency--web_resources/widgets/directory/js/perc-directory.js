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

$(document).ready(function() {
    var percDisplayFullDir;
    var DirectoryList;

    if ($('#percDirectoryList').data("directory-results-size") > 0) {
        var percDirectorySearchAllOrgs = ($('#percDirectoryList').data("search-all-orgs") === true);
        var percDirectorySearchAllDepts = ($('#percDirectoryList').data("show-dpt-filter") === true);
        percDisplayFullDir = ($('#percDirectoryList').data("display-full-directory") === true);

        var percSortOptions = {
            valueNames: [
                'perc-person-first-name',
                'perc-person-last-name',
                'perc-person-org',
                'perc-person-dpt',
                'perc-person-title',
                'perc-person-phone',
                'perc-person-office-location',
                'perc-person-email'
            ],
            plugins: []
        };
        DirectoryList = new List('percDirectoryList', percSortOptions);

        var alphaFilterLetters = getFilterLetters(DirectoryList.items, "perc-person-last-name");
        populateAlphaFilters(alphaFilterLetters);

        var firstLetterFilter = $('#perc-directory-alphabet-sort .perc-alpha-sort:first');
        firstLetterFilter.addClass("active");
        $('#perc-clear-alpha-filter').show();

        // populate dpt-filter dropdown
        configureDptDropDown();

        applyDirectoryFilters();
    }
    
    //
    // Directory widget functions
    //

    function getFilterLetters(array, filterKey) {
        var filterLetters = [];
        array.forEach(function(element) {
            if (element._values.hasOwnProperty(filterKey)) {
                var firstLetter = element._values[filterKey].charAt(0).toLowerCase();
                if ($.inArray(firstLetter, filterLetters) == -1) {
                    filterLetters.push(firstLetter);
                }
            }
        }, this);
        return filterLetters;
    }

    function populateAlphaFilters(alphaFilterLetters) {
        alphaFilterLetters.forEach(function(letter) {
            if( typeof(letter) !== "undefined" && letter.trim() !== "" ) {
                $('#perc-directory-alphabet-sort #perc-alpha-sort-letters').append('<a id="perc-alpha-sort-' + letter + '" class="perc-alpha-sort" aria-label="Filter by letter ' + letter.toUpperCase() + '" tabindex="0">' + letter.toUpperCase() + '</a>');
            }
        }, this);
    }

    function applyDirectoryFilters() {
        if (!DirectoryList) return;

        var orgName = null;
        if (percDirectorySearchAllOrgs) {
            orgName = $('#perc-org-filter').val();
            if (orgName === "all") orgName = null;
        } else {
            orgName = $('#percDirectoryList').data("directory-org-name");
        }

        var dptName = $('#perc-dpt-filter').val();
        if (dptName === "all") dptName = null;

        var activeLetterElement = $('#perc-directory-alphabet-sort .perc-alpha-sort.active');
        var letter = activeLetterElement.length ? activeLetterElement.text().toLowerCase() : null;

        DirectoryList.filter(function(item) {
            var matchOrg = !orgName || _.includes(item.values()['perc-person-org'], orgName);
            var matchDpt = !dptName || _.includes(item.values()['perc-person-dpt'], dptName);
            var matchLetter = !letter || _.includes(item.values()['perc-person-last-name'].charAt(0).toLowerCase(), letter);
            return matchOrg && matchDpt && matchLetter;
        });

        updateAlphaFilters();
    }

    function updateAlphaFilters() {
        if (!DirectoryList) return;

        var orgName = null;
        if (percDirectorySearchAllOrgs) {
            orgName = $('#perc-org-filter').val();
            if (orgName === "all") orgName = null;
        } else {
            orgName = $('#percDirectoryList').data("directory-org-name");
        }

        var dptName = $('#perc-dpt-filter').val();
        if (dptName === "all") dptName = null;

        var filteredItems = DirectoryList.items.filter(function(item) {
            var matchOrg = !orgName || _.includes(item.values()['perc-person-org'], orgName);
            var matchDpt = !dptName || _.includes(item.values()['perc-person-dpt'], dptName);
            return matchOrg && matchDpt;
        });

        var letters = getFilterLetters(filteredItems, "perc-person-last-name");
        var activeLetterElement = $('#perc-directory-alphabet-sort .perc-alpha-sort.active');
        var activeLetter = activeLetterElement.length ? activeLetterElement.text().toLowerCase() : null;

        $('#perc-directory-alphabet-sort #perc-alpha-sort-letters').empty();
        populateAlphaFilters(letters);

        if (activeLetter && $.inArray(activeLetter, letters) !== -1) {
            $('#perc-alpha-sort-' + activeLetter).addClass("active");
        } else if (activeLetter) {
            $('#perc-clear-alpha-filter').hide();
            applyDirectoryFilters();
        }
    }

    function resetAlphaFilters() {
        if (!DirectoryList) return;
        var letters = getFilterLetters(DirectoryList.items, "perc-person-last-name");
        $('#perc-directory-alphabet-sort #perc-alpha-sort-letters').empty();
        populateAlphaFilters(letters);
    }

    function configureDptFilterbyOrg(orgName) {
        var dptList = [];
        $('.perc-person').each(function() {
            var org = $(this).find('.perc-person-org').text();
            if (orgName == org) {
                var dpt = $(this).find('.perc-person-dpt').text();
                if ($.inArray(dpt, dptList) == -1 && dpt != '') {
                    dptList.push(dpt);
                }
            }
            dptList.sort();
            $('#perc-dpt-filter').empty();
            $('#perc-dpt-filter').append('<option value="all">Filter By Department</option>');
            for(var i = 0; i < dptList.length; i++) {
                $('#perc-dpt-filter').append('<option value="' + dptList[i] + '">' + dptList[i] + '</option>');
            }
            if (percDirectorySearchAllDepts)
                $('#perc-dpt-filter').show();
        });
    }

    function configureDptDropDown() {
        var dptList = [];
            $('.perc-person').each(function() {
            var dpt = $(this).find('.perc-person-dpt').text();
            if ($.inArray(dpt, dptList) == -1 && dpt != '') {
                dptList.push(dpt);
            }
            dptList.sort();
            $('#perc-dpt-filter').empty();
            $('#perc-dpt-filter').append('<option value="all">Filter By Department</option>');
            for(var i = 0; i < dptList.length; i++){
                $('#perc-dpt-filter').append('<option value="' + dptList[i] + '">' + dptList[i] + '</option>');
            }
        });
    }

    //
    // Directory widget event handlers
    //
    
    $('.perc-directory-sort-buttons').on('click', "button.sort", function() {
        var list = $('.perc-pagination').find('li');
        $(list[0]).trigger('click');

    });

    $('#perc-org-filter').on("change", function() {
        if (DirectoryList) {
            $('#perc-dpt-filter').hide();

            var orgName = $(this).val();

            if (orgName == "all"){
                $('#perc-dpt-filter').val("all");
                applyDirectoryFilters();
            } else {
                configureDptFilterbyOrg(orgName);
                $('#perc-dpt-filter').val("all");
                applyDirectoryFilters();
            }
       }
    });

    $('#perc-dpt-filter').on("change", function() {
        if (DirectoryList) {
            applyDirectoryFilters();
        }
    });

    // alphabet sort function on 'click'
    $('#perc-directory-alphabet-sort').on('click', '.perc-alpha-sort', function(event) {
        var letter = $(this).text().toLowerCase();
        if (DirectoryList) {
            $('#search-directory').val("");
            DirectoryList.search();
            $('#perc-directory-alphabet-sort .perc-alpha-sort.active').removeClass('active');
            $(this).addClass("active");
            applyDirectoryFilters();
            $('#perc-clear-alpha-filter').show();
        }
    });

    // alpha sort on keypress "enter"
    $('#perc-directory-alphabet-sort').on('keypress', '.perc-alpha-sort', function(event) {
        if (event.keyCode == 13) {
            var letter = $(this).text().toLowerCase();
            if (DirectoryList) {
                $('#search-directory').val("");
                DirectoryList.search();
                $('#perc-directory-alphabet-sort .perc-alpha-sort.active').removeClass('active');
                $(this).addClass("active");
                applyDirectoryFilters();
                $('#perc-clear-alpha-filter').show();
            }
        }
    });

    $('#perc-clear-alpha-filter').on('click', function() {
        $('#perc-directory-alphabet-sort .perc-alpha-sort.active').removeClass('active');
        if (DirectoryList) {
            $('#search-directory').val("");
            DirectoryList.search();
            applyDirectoryFilters();
            configureDptDropDown();
            $('#perc-clear-alpha-filter').hide();
        }
    });

    // Presubmit Event handler to encode
    $('#percDirectoryList').on('click', '.perc-person-email a', function (element) {
        // grab the href value
        var url = $(this).attr("href");
        // URI encode param
        url = encodeURI(url);
        // update the element href attribute prior to navigation
        $(this).attr("href", url);
    });

    if(percDisplayFullDir){
        if (DirectoryList) {
            DirectoryList.filter();
        }
    }

});  // End Document Ready Function
