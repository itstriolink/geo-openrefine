/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

var dictionary = "";
$.ajax({
    url: "command/core/load-language?",
    type: "POST",
    async: false,
    data: {
        module: "osm-extractor",
    },
    success: function (data) {
        dictionary = data['dictionary'];
        lang = data['lang'];
    }
});
$.i18n().load(dictionary, lang);

Refine.OSMImportingController = function (createProjectUI) {

    this._createProjectUI = createProjectUI;

    this._parsingPanel = createProjectUI.addCustomPanel();
    createProjectUI.addSourceSelectionUI({
        label: "OpenStreetMap (Overpass)",
        id: "openstreetmap-overpass",
        ui: new Refine.OSMImportUI(this)
    });
};

Refine.OSMImportingController._overpassQuery = null;
Refine.OSMImportingController._overpassInstance = null;
// DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
//     MenuSystem.insertAfter(menu, ["core/edit-column", "core/add-column-by-reconciliation"], [
//         {
//             "id": "osm-extractor/osm-extractor",
//             "label": $.i18n('osm-extractor/add-columns-from-osm'),
//             "click": function () {
//                 new OSMExtractorDialog();
//             }
//         },
//     ]);
// });

Refine.CreateProjectUI.controllers.push(Refine.OSMImportingController);
debugger;
Refine.OSMImportingController.prototype.startImportingData = function (overpassQuery, overpassInstance) {
    var self = this;
    self._overpassQuery = overpassQuery;
    self._overpassInstance = overpassInstance;
    Refine.postCSRF(
        "command/core/create-importing-job",
        null,
        function (data) {
            Refine.wrapCSRF(function (token) {
                $.post(
                    "command/core/importing-controller?" + $.param({
                        "controller": "osm-extractor/osm-data-importing-controller",
                        "subCommand": "initialize-parser-ui",
                        "overpassQuery": overpassQuery,
                        "overpassInstance": overpassInstance,
                        "csrf_token": token
                    }),
                    null,
                    function (data2) {

                        if (data2.status == 'ok') {
                            self._options = data2.options;
                            self._jobID = data.jobID;
                            self._showParsingPanel();
                        } else {
                            alert(data2.message);
                        }
                    },
                    "json"
                );
            });
        },
        "json"
    );
};

Refine.OSMImportingController.prototype._showParsingPanel = function () {
    var self = this;

    this._parsingPanel.unbind().empty().html(
        DOM.loadHTML("osm-extractor", 'scripts/osm-parsing-panel.html'));
    this._parsingPanelElmts = DOM.bind(this._parsingPanel);


    this._parsingPanelElmts.startOverButton.html($.i18n('osm-extractor/start-over'));
    this._parsingPanelElmts.configureMapping.html($.i18n('osm-extractor/configure-mapping'));
    this._parsingPanelElmts.projectName.html($.i18n('osm-extractor/project-name'));
    this._parsingPanelElmts.createProjectButton.html($.i18n('osm-extractor/create-project'));
    this._parsingPanelElmts.includeLabel.html($.i18n('osm-extractor/include-label'));

    this._parsingPanelElmts.selectAllButton.html($.i18n('osm-extractor/select-all-tags'));
    this._parsingPanelElmts.deselectAllButton.html($.i18n('osm-extractor/deselect-all-tags'));

    this._parsingPanelElmts.pointsLabel.html($.i18n('osm-extractor/points'));
    this._parsingPanelElmts.pointsLatLonLabel.html($.i18n('osm-extractor/points-lat-lon'));
    this._parsingPanelElmts.pointsDelimitedLabel.html($.i18n('osm-extractor/points-delimited'));
    this._parsingPanelElmts.pointsWKTLabel.html($.i18n('osm-extractor/points-wkt'));
    this._parsingPanelElmts.linesLabel.html($.i18n('osm-extractor/lines-wkt'));
    this._parsingPanelElmts.multiLinesLabel.html($.i18n('osm-extractor/multi-lines-wkt'));
    this._parsingPanelElmts.multiPolygonsLabel.html($.i18n('osm-extractor/multi-polygons-wkt'));

    this._parsingPanelElmts.loadingMessage.html($.i18n('osm-extractor/loading-message'));

    if (this._parsingPanelResizer) {
        $(window).unbind('resize', this._parsingPanelResizer);
    }

    this._parsingPanelResizer = function () {
        var elmts = self._parsingPanelElmts;
        var width = self._parsingPanel.width();
        var height = self._parsingPanel.height();
        var headerHeight = elmts.wizardHeader.outerHeight(true);
        var controlPanelHeight = 250;

        // elmts.dataPanel
        //     .css("left", "0px")
        //     .css("top", headerHeight + "px")
        //     .css("width", (width - DOM.getHPaddings(elmts.dataPanel)) + "px")
        //     .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.dataPanel)) + "px");
        elmts.progressPanel
            .css("left", "0px")
            .css("top", headerHeight + "px")
            .css("width", (width - DOM.getHPaddings(elmts.progressPanel)) + "px")
            .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.progressPanel)) + "px");

    };
    $(window).resize(this._parsingPanelResizer);
    this._parsingPanelResizer();

    $("#pointsDelimitedSeparatorInput").val(", ");

    this._parsingPanelElmts.pointsCheckbox.change(function () {
        if (!this.checked) {
            $("input.pointsCheckboxes, input#pointsDelimitedSeparatorInput").each(function () {
                $(this).prop("disabled", true);
            });

        } else {
            $("input.pointsCheckboxes, input#pointsDelimitedSeparatorInput").each(function () {
                $(this).prop("disabled", false);
            });
        }
    });

    this._parsingPanelElmts.selectAllButton.click(function () {
        self._parsingPanelElmts.tagsTable.find('input[type="checkbox"]').prop('checked', true);
        self._parsingPanelElmts.tagsTable.find('input[type="text"]').prop("disabled", false);
    });

    this._parsingPanelElmts.deselectAllButton.click(function () {
        self._parsingPanelElmts.tagsTable.find('input[type="checkbox"]').prop('checked', false);
        self._parsingPanelElmts.tagsTable.find('input[type="text"]').prop("disabled", true);
    });

    this._parsingPanelElmts.startOverButton.click(function () {
        Refine.CreateProjectUI.cancelImportingJob(self._jobID);

        delete self._doc;
        delete self._jobID;
        delete self._options;

        self._createProjectUI.showSourceSelectionPanel();
    });
    this._parsingPanelElmts.createProjectButton.click(function () {
        self._createProject();
    });

    this._parsingPanelElmts.projectNameInput[0].value = "OpenstreetMap (Overpass) project";

    this._createProjectUI.showCustomPanel(this._parsingPanel);
    this._updatePreview();
}

Refine.OSMImportingController.prototype._updatePreview = function () {
    var self = this;

    self._parsingPanelElmts.mainContainer.hide();
    self._parsingPanelElmts.statisticsContainer.hide();
    self._parsingPanelElmts.progressPanel.show();
    self._parsingPanelElmts.createProjectButton.addClass("button-disabled").prop("disabled", true);


    Refine.wrapCSRF(function (token) {
        $.post(
            "command/core/importing-controller?" + $.param({
                "controller": "osm-extractor/osm-data-importing-controller",
                "jobID": self._jobID,
                "subCommand": "parse-preview",
                "overpassQuery": self._overpassQuery,
                "overpassInstance": self._overpassInstance,
                "csrf_token": token
            }),
            null,
            function (result) {
                if (result.status == "ok") {
                    $(".osm-extractor-dialog-row").remove();
                    self._parsingPanelElmts.progressPanel.hide();
                    self._parsingPanelElmts.mainContainer.show();
                    self._parsingPanelElmts.statisticsContainer.show();

                    var tags = result.tags;
                    var stats = result.stats;

                    if (tags && tags.length > 0) {
                        var tagsHeaderRow = $('<tr>').appendTo(self._parsingPanelElmts.tagsTable);
                        var tagsHeaderCell = $('<td>')
                            .addClass("column-header")
                            .addClass("text-center")
                            .attr("colspan", 3)
                            .html("Tags")
                            .appendTo(tagsHeaderRow);
                        var tagsHeaderCells = $('<tr>').appendTo(self._parsingPanelElmts.tagsTable);
                        var indexHeaderCell = $('<td>').addClass("column-header").html(" ").appendTo(tagsHeaderCells);
                        var tagHeaderCell = $('<td>').addClass("column-header").html("Tag name").appendTo(tagsHeaderCells);
                        var newColumnHeaderCell = $('<td>').addClass("column-header").html("Column name").appendTo(tagsHeaderCells);
                        for (var i = 0; i < tags.length; i++) {
                            var column = tags[i];

                            var row = $('<tr>')
                                .addClass("osm-extractor-dialog-row")
                                .addClass("tagRow")
                                .addClass(i % 2 == 0 ? "odd" : "even")
                                .attr("column", column)
                                .attr("rowIndex", i)
                                .appendTo(self._parsingPanelElmts.tagsTable);

                            var indexCell = $('<td>')
                                .attr("width", "10%")
                                .appendTo(row);

                            var tagNameCell = $('<td>').attr("width", "45%").appendTo(row);
                            var tagNameDiv = $('<div>').addClass("data-table-cell-content").appendTo(tagNameCell);
                            $('<div>')
                                .text((i + 1) + ".")
                                .attr("rowIndex", i)
                                .appendTo(indexCell);

                            $('<input>')
                                .attr('type', 'checkbox')
                                .attr("id", "tagCheckbox-" + i)
                                .prop("checked", i == 0 ? true : false)
                                .attr("class", "includeTagCheckbox")
                                .attr("rowIndex", i)
                                .appendTo(tagNameDiv);

                            $('<label>')
                                .text(column)
                                .attr("for", "tagCheckbox-" + i)
                                .attr("class", "osmTagName")
                                .attr("rowIndex", i)
                                .appendTo(tagNameDiv);

                            var newColumnNameCell = $('<td>').attr("width", "45%").appendTo(row);
                            var newColumnNameDiv = $('<div>').addClass("data-table-cell-content").appendTo(newColumnNameCell);

                            $('<input>')
                                .attr('type', 'text')
                                .attr("class", "newColumnName")
                                .attr("disabled", i > 0 ? true : false)
                                .attr("rowIndex", i)
                                .val(column)
                                .appendTo(newColumnNameDiv);
                        }
                    }

                    if (stats) {
                        for (var [key, value] of Object.entries(stats)) {
                            var cell = $('<td>')
                                .appendTo(self._parsingPanelElmts.statisticsRow);

                            $('<span>')
                                .text(key.charAt(0).toUpperCase() + key.slice(1) + ": ")
                                .attr(key, value)
                                .appendTo(cell);

                            $('<strong>')
                                .text(value)
                                .appendTo(cell);
                        }
                    }
                    $(".includeTagCheckbox, .includeGeometryCheckbox").click(function () {
                        if (this.checked) {
                            $(this).parent().parent().siblings().find("input.newColumnName").attr("disabled", false);
                        } else {
                            $(this).parent().parent().siblings().find("input.newColumnName").attr("disabled", true);
                        }
                    });

                    self._parsingPanelElmts.createProjectButton.removeClass("button-disabled").prop("disabled", false);
                } else {
                    self._parsingPanelElmts.progressPanel.hide();
                    alert('Errors:\n' +
                    (result.message) ? result.message : Refine.CreateProjectUI.composeErrorMessage(job));
                }
            },
            "json"
        );
    });
}

Refine.OSMImportingController.prototype._createProject = function () {
    var self = this;
    var projectName = $.trim(self._parsingPanelElmts.projectNameInput[0].value);
    if (projectName.length == 0) {
        window.alert("Please enter a valid project name.");
        self._parsingPanelElmts.projectNameInput.focus();
        return;
    }

    var projectOptions = {
        "projectName": projectName,
        "encoding": "UTF-8"
    }
    var tags = [];
    var geometry = [];
    var importOptions = {
        "points": self._parsingPanelElmts.pointsCheckbox[0].checked,
        "pointsAsLatLon": self._parsingPanelElmts.pointsLatLonCheckbox[0].checked,
        "pointsDelimited": self._parsingPanelElmts.pointsDelimitedCheckbox[0].checked,
        "pointsSeparator": self._parsingPanelElmts.pointsDelimitedSeparatorInput.val(),
        "pointsAsWKT": self._parsingPanelElmts.pointsWKTCheckbox[0].checked,
        "lines": self._parsingPanelElmts.linesCheckbox[0].checked,
        "multiLines": self._parsingPanelElmts.multiLinesCheckbox[0].checked,
        "multiPolygons": self._parsingPanelElmts.multiPolygonsCheckbox[0].checked
    }

    $('#raw-query-response-table tbody tr.tagRow').each(function () {
        var row = $(this);
        var checkbox = row.find('input.includeTagCheckbox')[0];
        var osmTag = row.find('label.osmTagName').html();
        var newColumnName = row.find('input.newColumnName').val();

        if (checkbox && checkbox.checked) {
            tags.push({
                osmTag,
                newColumnName,
            });
        }
    });

    Refine.wrapCSRF(function (token) {
        $.post(
            "command/core/importing-controller?" + $.param({
                "controller": "osm-extractor/osm-data-importing-controller",
                "jobID": self._jobID,
                "subCommand": "create-project",
                "csrf_token": token
            }),
            {
                "projectOptions": JSON.stringify(projectOptions),
                "tags": JSON.stringify(tags),
                "importOptions": JSON.stringify(importOptions)
            },
            function (o) {
                if (o.status == 'error') {
                    alert(o.message);
                } else {
                    var start = new Date();
                    var timerID = window.setInterval(
                        function () {
                            self._createProjectUI.pollImportJob(
                                start,
                                self._jobID,
                                timerID,
                                function (job) {
                                    return "projectID" in job.config;
                                },
                                function (jobID, job) {
                                    window.clearInterval(timerID);
                                    Refine.CreateProjectUI.cancelImportingJob(jobID);
                                    document.location = "project?project=" + job.config.projectID;
                                },
                                function (job) {
                                    alert(Refine.CreateProjectUI.composeErrorMessage(job));
                                }
                            );
                        },
                        1000
                    );
                    self._createProjectUI.showImportProgressPanel($.i18n('gdata-import/creating'), function () {
                        // stop the timed polling
                        window.clearInterval(timerID);

                        // explicitly cancel the import job
                        Refine.CreateProjectUI.cancelImportingJob(self._jobID);

                        self._createProjectUI.showSourceSelectionPanel();
                    });
                }
            },
            "json"
        );
    });
}

Refine.OSMImportingController.prototype._getOptions = function () {
    var options = {
        // docUrl: this._doc.docSelfLink,
        // docType: this._doc.type,
    };

    return options;
};