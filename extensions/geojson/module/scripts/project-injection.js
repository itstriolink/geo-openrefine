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

var lang = navigator.language.split("-")[0]
    || navigator.userLanguage.split("-")[0];
var dictionary = "";
$.ajax({
    url : "command/core/load-language?",
    type : "POST",
    async : false,
    data : {
        module : "geojson",
    },
    success : function(data) {
        dictionary = data['dictionary'];
        lang = data['lang'];
    }
});
$.i18n().load(dictionary, lang);
DataTableColumnHeaderUI.extendMenu((column, columnHeaderUI, menu) => {
    MenuSystem.appendTo(menu, [ "core/edit-cells" ], [
        {
            id: "geojson/sample-button",
            label: $.i18n('geojson/generate-random-number') + "...",
            click: function() {
                Refine.wrapCSRF(function(token) {
                    $.post(
                        "command/geojson/generate-random-number?" + $.param({
                            "numberA": 1,
                            "numberB": 10,
                            "csrf_token": token
                        }),
                        null,
                        function(o) {
                            window.alert(o);
                        },
                        "json"
                    );
                });
            }
        }
    ]);
});