/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * Neither the name of Google nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

const OSM_ELEMENTS = {
    NODE: "node",
    WAY: "way",
    RELATION: "relation"
}

let _response = {};

function OSMExtractorDialog() {
    this._createDialog();
}

OSMExtractorDialog.prototype._createDialog = function () {
    var self = this;

    self._dialog = $(DOM.loadHTML("osm-extractor", "scripts/dialog.html"));
    self._elmts = DOM.bind(this._dialog);
    self._level = DialogSystem.showDialog(this._dialog);

    self._elmts.dialogHeader.html($.i18n('osm-extractor/osm-extractor'));

    self._elmts.dialogQuery.html($.i18n('osm-extractor/query-tab'));
    self._elmts.selectKey.html($.i18n('osm-extractor/osm-key-label'));
    self._elmts.selectValue.html($.i18n('osm-extractor/osm-value-label'));
    self._elmts.dialogRawQuery.html($.i18n('osm-extractor/raw-query-tab'));
    self._elmts.dialogSettings.html($.i18n('osm-extractor/settings-tab'));
    self._elmts.enterRawQuery.html($.i18n('osm-extractor/enter-raw-query'));

    self._elmts.runRawQueryButton.html($.i18n('osm-extractor/run-query'));
    self._elmts.saveRawQueryButton.html($.i18n('osm-extractor/save-query'));

    self._elmts.selectInstance.html($.i18n('osm-extractor/select-overpass-instance'));
    self._elmts.cancelButton.html($.i18n('core-buttons/cancel'))


    $("#tabs-query").css("display", "");
    $("#tabs-raw-query").css("display", "");
    $("#osm-extractor-tabs").tabs();

    self._createAndPopulateKeyAndValue();
    self._createAndPopulateSettingsTab();

    this._elmts.cancelButton.click(function () {
        self._dismiss();
    });

    this._elmts.runRawQueryButton.click(function (_) {
        var data = self._elmts.rawQueryInput[0] && self._elmts.rawQueryInput[0].value;
        var overpassAPI = $("#selectInstance").val();

        if (data && overpassAPI) {
            $.post(
                overpassAPI,
                {
                    "data": data
                },
                function (response) {
                    _response = response.elements;
                    if (response.elements && response.elements.length > 0) {
                        const osmElements = response.elements;
                        const columns = Object.keys(osmElements[0].tags);
                        $(".osm-extractor-dialog-row").remove();
                        for (var i = 0; i < columns.length; i++) {
                            var column = columns[i];

                            var row = $('<tr>')
                                .addClass("osm-extractor-dialog-row")
                                .attr("column", column)
                                .attr("rowIndex", i)
                                .appendTo(self._elmts.columnList);
                            var tagNameCell = $('<td>')
                                .attr('width', '100px')
                                .appendTo(row);

                            $('<input>')
                                .attr('type', 'checkbox')
                                .attr("id", "checkbox-" + i)
                                .attr("class", "includeTagCheckbox")
                                .prop('checked', true)
                                .attr("rowIndex", i)
                                .appendTo(tagNameCell);
                            $('<span>')
                                .text(column)
                                .attr("class", "osmTagName")
                                .attr("rowIndex", i)
                                .appendTo(tagNameCell);

                            var newColumnNameCell = $('<td>')
                                .attr('width', '100px')
                                .appendTo(row);
                            $('<input>')
                                .attr('type', 'text')
                                .attr('size', '8px')
                                .attr("class", "newColumnName")
                                .attr("id", "newColumnInput-" + i)
                                .attr("rowIndex", i)
                                .appendTo(newColumnNameCell);


                            $("#checkbox-" + i).click(function () {
                                var index = this.getAttribute('rowIndex');
                                if (this.checked) {
                                    $("#newColumnInput-" + index).attr("disabled", false);
                                } else {
                                    $("#newColumnInput-" + index).attr("disabled", true);
                                }
                            });
                        }
                    }
                },
                "json"
            )
        }
    });

    this._elmts.saveRawQueryButton.click(function (_) {
        var mappings = [];
        var data = _response;

        $('#raw-query-response-table tbody tr').each(function () {
            var row = $(this);
            var checkbox = row.find('input.includeTagCheckbox')[0];
            var osmTag = row.find('span.osmTagName').html();
            var newColumnName = row.find('input.newColumnName').val();

            if (checkbox.checked) {
                mappings.push({
                    osmTag,
                    newColumnName,
                });
            }
        });

        Refine.postProcess(
            "osm-extractor",
            "add-osm-data-to-project",
            {},
            {
                mappings: JSON.stringify(mappings),
                data: JSON.stringify(data)
            },
            {modelsChanged: true},
            {
                onDone: function () {
                    self._dismiss();
                },
                onError: function (e) {
                    alert("Something went wrong...");
                },
            }
        );
    });
};

OSMExtractorDialog.prototype._createAndPopulateSettingsTab = function () {
    const overpassInstanceSelect = $('<select>').appendTo('body');
    overpassInstanceSelect.attr("id", "selectInstance");

    overpassInstanceSelect.appendTo($("#settings-select-overpass-instance")[0]);

    $.getJSON(
        "command/osm-extractor/get-overpass-instances",
        null,
        function (data) {
            if (data.instances && data.instances.length > 0) {
                var overpassInstances = data.instances;

                for (const instance of overpassInstances) {
                    overpassInstanceSelect.append($("<option>").val(instance).text(instance));
                }

                overpassInstanceSelect.val(overpassInstances[0]);
            } else {
                window.alert($.i18n("osm-extractor/overpass-instance-error"));
                console.error(data);
            }
        }
    );
};

OSMExtractorDialog.prototype._createAndPopulateKeyAndValue = function () {
    const elementKeyInput = $('<input>').appendTo('body');
    elementKeyInput.attr("id", "selectKey");
    elementKeyInput.attr("type", "text");
    elementKeyInput.attr("name", "osmKey");
    elementKeyInput.attr("list", "osmKeys");
    elementKeyInput.appendTo($("#query-select-tag-key")[0]);

    const elementKeyDataList = $('<datalist>').appendTo('body');
    elementKeyDataList.attr("id", "osmKeys");
    elementKeyDataList.appendTo(elementKeyInput);

    const elementValueInput = $('<input>').appendTo('body');
    elementValueInput.attr("id", "selectValue");
    elementValueInput.attr("type", "text");
    elementValueInput.attr("name", "osmValue");
    elementValueInput.attr("list", "osmValues");
    elementValueInput.appendTo($("#query-select-tag-value")[0]);

    const elementValueDataList = $('<datalist>').appendTo('body');
    elementValueDataList.attr("id", "osmValues");
    elementValueDataList.appendTo(elementValueInput);

    $.getJSON(
        "command/osm-extractor/get-osm-tags",
        null,
        function (data) {
            if (data.tags && data.tags.length > 0) {
                var osmTags = data.tags;

                for (const tag of osmTags) {
                    elementKeySelect.append($("<option>").val(tag).text(tag));
                }

            } else {
                window.alert($.i18n("osm-extractor/osm-tags-error"));
                console.error(data);
            }
        }
    );
}

OSMExtractorDialog.prototype._dismiss = function () {
    DialogSystem.dismissUntil(this._level - 1);
};

