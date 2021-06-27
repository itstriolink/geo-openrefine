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

function OSMExtractorDialog() {
    this._createDialog();
}

OSMExtractorDialog.prototype._createDialog = function () {
    var self = this;

    this._dialog = $(DOM.loadHTML("osm-extractor", "scripts/dialog.html"));
    this._elmts = DOM.bind(this._dialog);
    this._level = DialogSystem.showDialog(this._dialog);

    this._elmts.dialogHeader.html($.i18n('osm-extractor/osm-extractor'));

    this._elmts.dialogQuery.html($.i18n('osm-extractor/query-tab'));
    this._elmts.dialogRawQuery.html($.i18n('osm-extractor/raw-query-tab'));
    this._elmts.dialogSettings.html($.i18n('osm-extractor/settings-tab'));

    this._elmts.selectInstance.html($.i18n('osm-extractor/select-overpass-instance'));
    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'))
    this._elmts.cancelButton.click(function() { self._dismiss(); });

    $("#tabs-query").css("display", "");
    $("#tabs-raw-query").css("display", "");
    $("#custom-tabular-exporter-tabs").tabs();

    self._createSettingsTab();
};

OSMExtractorDialog.prototype._createSettingsTab = function () {
    const overpassInstanceSelect = $('<select>').appendTo('body');
    overpassInstanceSelect.attr("id", "selectInstance");

    overpassInstanceSelect.appendTo($("#select-overpass-instance")[0]);

    $.getJSON(
        "command/osm-extractor/get-overpass-instances",
        null,
        function(data) {
            if(data.instances && data.instances.length > 0) {
                var overpassInstances = data.instances;

                for(const instance of overpassInstances) {
                    overpassInstanceSelect.append($("<option>").val(instance).text(instance));
                }

                overpassInstanceSelect.val(overpassInstances[0]);
            }
        },
        "json"
    );
};

OSMExtractorDialog.prototype._dismiss = function () {
    DialogSystem.dismissUntil(this._level - 1);
};

