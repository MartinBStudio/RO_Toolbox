import {test} from "node:test";
import assert from "node:assert/strict";
import {filterPackageProfiles} from "../src/utils/packageBrowserFilters.ts";

const installed = {id: "installed", name: "Installed", version: "1", tags: ["color:red"]};
const favoriteMismatch = {id: "favorite-red", name: "Favorite red", version: "1", tags: ["color:red"]};
const favoriteMatch = {id: "favorite-green", name: "Favorite green", version: "1", tags: ["color:green"]};
const normalMatch = {id: "green", name: "Green", version: "1", tags: ["color:green"]};
const profiles = [installed, favoriteMismatch, favoriteMatch, normalMatch];
const filters = {installedProfile: installed, search: "", favoritesOnly: false, isFavorite: name => name.startsWith("Favorite"), metadata: {tags: ["color:green"]}};
const ids = result => result.map(profile => profile.id);

test("nonmatching favorites are hidden but installed packages bypass tag filters", () => {
    assert.deepEqual(ids(filterPackageProfiles(profiles, filters)), ["installed", "favorite-green", "green"]);
});
test("favorites-only still applies tags to favorites and keeps installed packages", () => {
    assert.deepEqual(ids(filterPackageProfiles(profiles, {...filters, favoritesOnly: true})), ["installed", "favorite-green"]);
});
test("installed packages alone bypass name searches and multiple tag filters", () => {
    assert.deepEqual(ids(filterPackageProfiles(profiles, {...filters, search: "no matches", favoritesOnly: true, metadata: {tags: ["color:green", "style:flat"]}})), ["installed"]);
});
test("without an installed package no favorites bypass filters", () => {
    assert.deepEqual(filterPackageProfiles(profiles, {...filters, installedProfile: null, search: "no matches"}), []);
});
