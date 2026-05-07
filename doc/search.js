/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
"use strict";

let moduleSearchIndex = [];
let updateSearchResults;

const messages = {
    enterTerm: "Enter a search term",
    noResult: "No results found",
    oneResult: "Found one result",
    manyResults: "Found {0} results",
    loading: "Loading search index...",
    searching: "Searching...",
    redirecting: "Redirecting to first result...",
    linkIcon: "Link icon",
    linkToSection: "Link to this section"
}
const categories = {
    modules: "Modules",
    packages: "Packages",
    types: "Classes and Interfaces",
    members: "Members",
    searchTags: "Search Tags"
};
const highlight = "<span class='result-highlight'>$&</span>";
const NO_MATCH = {};
const MAX_RESULTS = 300;

function checkUnnamed(name, separator) {
    return name === "<Unnamed>" || !name ? "" : name + separator;
}
function escapeHtml(str) {
    return str.replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function getHighlightedText(str, boundaries, from, to) {
    var start = from;
    var text = "";
    for (var i = 0; i < boundaries.length; i += 2) {
        var b0 = boundaries[i];
        var b1 = boundaries[i + 1];
        if (b0 >= to || b1 <= from) {
            continue;
        }
        text += escapeHtml(str.slice(start, Math.max(start, b0)));
        text += "<span class='result-highlight'>";
        text += escapeHtml(str.slice(Math.max(start, b0), Math.min(to, b1)));
        text += "</span>";
        start = Math.min(to, b1);
    }
    text += escapeHtml(str.slice(start, to));
    return text;
}
function getURLPrefix(item, category) {
    var urlPrefix = "";
    var slash = "/";
    if (category === "modules") {
        return item.l + slash;
    } else if (category === "packages" && item.m) {
        return item.m + slash;
    } else if (category === "types" || category === "members") {
        if (item.m) {
            urlPrefix = item.m + slash;
        } else {
            $.each(packageSearchIndex, function(index, it) {
                if (it.m && item.p === it.l) {
                    urlPrefix = it.m + slash;
                }
            });
        }
    }
    return urlPrefix;
}
function getURL(item, category) {
    if (item.url) {
        return item.url;
    }
    var url = getURLPrefix(item, category);
    if (category === "modules") {
        url += "module-summary.html";
    } else if (category === "packages") {
        if (item.u) {
            url = item.u;
        } else {
            url += item.l.replace(/\./g, '/') + "/package-summary.html";
        }
    } else if (category === "types") {
        if (item.u) {
            url = item.u;
        } else {
            url += checkUnnamed(item.p, "/").replace(/\./g, '/') + item.l + ".html";
        }
    } else if (category === "members") {
        url += checkUnnamed(item.p, "/").replace(/\./g, '/') + item.c + ".html" + "#";
        if (item.u) {
            url += item.u;
        } else {
            url += item.l;
        }
    } else if (category === "searchTags") {
        url += item.u;
    }
    item.url = url;
    return url;
}

function doSearch(request, response) {
    var term = request.term.trim();
    var maxResults = request.maxResults || MAX_RESULTS;
    if (term.length === 0) {
        return this.close();
    }

    var indexLoaded = indexFilesLoaded();

    var result = searchIndex(moduleSearchIndex, "modules")
        .concat(searchIndex(packageSearchIndex, "packages"))
        .concat(searchIndex(typeSearchIndex, "types"))
        .concat(searchIndex(memberSearchIndex, "members"))
        .concat(searchIndex(tagSearchIndex, "searchTags"));

    if (!indexLoaded) {
        updateSearchResults = function() {
            doSearch(request, response);
        }
    } else {
        updateSearchResults = function() {};
    }
    response(result);
}