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

function GeoJSONExporterDialog() {
    this._columnOptionMap = {};
    this._createDialog();
}

GeoJSONExporterDialog.prototype._createDialog = function () {
    var self = this;

    this._dialog = $(DOM.loadHTML("geojson", "scripts/dialog.html"));
    this._elmts = DOM.bind(this._dialog);
    this._level = DialogSystem.showDialog(this._dialog);

    this._elmts.dialogHeader.html($.i18n('geojson/geojson-exporter'));
    this._elmts.or_dialog_selAndOrd1.html($.i18n('geojson/select-columns-for-properties-1'));
    this._elmts.or_dialog_selAndOrd2.html($.i18n('geojson/select-columns-for-properties-2'));

    this._elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
    this._elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));

    this._elmts.exportButton.html($.i18n('geojson/export-button'));
    this._elmts.cancelButton.html($.i18n('core-buttons/cancel'));


    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var column = theProject.columnModel.columns[i];
        var name = column.name;
        if(!name.match("^(lat|lon).*$")) {
            var div = $('<div>')
                .addClass("geojson-exporter-dialog-row")
                .attr("column", name)
                .appendTo(this._elmts.columnList);

            $('<input>')
                .attr('type', 'checkbox')
                .prop('checked', true)
                .appendTo(div);

            $('<span>')
                .text(name)
                .appendTo(div);

            this._columnOptionMap[name] = {
                name: name,
            };
        }

    }

    this._elmts.selectAllButton.click(function () {
        self._elmts.columnList.find('input[type="checkbox"]').prop('checked', true);
    });
    this._elmts.deselectAllButton.click(function () {
        self._elmts.columnList.find('input[type="checkbox"]').prop('checked', false);
    });


    this._elmts.cancelButton.click(function () {
        self._dismiss();
    });
    this._elmts.exportButton.click(function () {
        self._exportData();
    });
};


GeoJSONExporterDialog.prototype._dismiss = function () {
    DialogSystem.dismissUntil(this._level - 1);
};

GeoJSONExporterDialog.prototype._exportData = function () {
    const options = this._getOptionCode();
    const form = ExporterManager.prepareExportRowsForm(options.format, true, options.extension);
    $('<input />')
        .attr("name", "options")
        .val(JSON.stringify(options))
        .appendTo(form);

    $('<input />')
        .attr("name", "preview")
        .val("prev")
        .appendTo(form);

    document.body.appendChild(form);

    window.open(" ", "refine-export");
    form.submit();

    document.body.removeChild(form);
    this._dismiss();
};

GeoJSONExporterDialog.prototype._getOptionCode = function () {
    const options = {
        format: 'geojson',
        extension: 'geojson',
        encoding: 'UTF-8',
        outputBlankRows: false,
        propertyColumns: [],
    };

    this._elmts.columnList.find('.geojson-exporter-dialog-row').each(function () {
        if ($(this).find('input[type="checkbox"]')[0].checked) {
            options.propertyColumns.push({
                name: this.getAttribute('column')
            });
        }
    });

    return options;
};

