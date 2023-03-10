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

Refine.OSMImportUI = function (controller) {
    this._controller = controller;
}

Refine.OSMImportUI.prototype.attachUI = function (body) {
    var self = this;
    self._body = body;

    self._body.html(DOM.loadHTML("osm-extractor", "scripts/osm-import-form.html"));
    self._elmts = DOM.bind(this._body);

    //self._elmts.dialogRawQuery.html($.i18n('osm-extractor/raw-query-tab'));
    self._elmts.enterRawQuery.html($.i18n('osm-extractor/enter-raw-query'));
    // self._elmts.rawQueryMappings.html($.i18n('osm-extractor/raw-query-mappings'));

    self._elmts.nextButton.html($.i18n('osm-extractor/next->'));
    self._elmts.helpButton.html($.i18n('osm-extractor/help'));
    // self._elmts.saveRawQueryButton.html($.i18n('osm-extractor/save-query'));

    self._elmts.selectInstance.html($.i18n('osm-extractor/select-overpass-instance'));

    // self._createAndPopulateKeyAndValue();
    self._createAndPopulateSettingsTab();

    self._elmts.nextButton.click(function (_) {
        var overpassQuery = self._elmts.rawQueryInput[0] && self._elmts.rawQueryInput[0].value;
        var overpassAPI = $("#selectInstance").val();

        self._controller.startImportingData(overpassQuery, overpassAPI);
    });
}

Refine.OSMImportUI.prototype._createAndPopulateSettingsTab = function () {
    const overpassInstanceSelect = $('<select>').appendTo('body');
    overpassInstanceSelect.attr("id", "selectInstance");
    overpassInstanceSelect.css("font-weight", "normal");

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
Refine.OSMImportUI.prototype.focus = function () {
};

