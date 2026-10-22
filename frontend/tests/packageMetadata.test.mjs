import {test} from "node:test";
import assert from "node:assert/strict";
import {emptyMetadataFilters, matchesPackageMetadata, packageMetadataOptions, groupPackageTags} from "../src/utils/packageMetadata.ts";

const plains = {name: "Adventurer's Plains", made: "ai", mode: "dark", tags: ["kind:theme", "made:ai", "color:green", "color:yellow", "cursors"]};

test("unfiltered packages without tags remain visible", () => {
    for (const profile of [{}, {tags: null}, plains]) {
        assert.equal(matchesPackageMetadata(profile, emptyMetadataFilters()), true);
    }
    assert.deepEqual(packageMetadataOptions([{}]), {tags: []});
});

test("packages must match all selected tags", () => {
    assert.equal(matchesPackageMetadata(plains, {tags: ["color:green", "color:yellow"]}), true);
    assert.equal(matchesPackageMetadata(plains, {tags: ["color:green", "style:flat"]}), false);
    assert.equal(matchesPackageMetadata({}, {tags: ["color:green"]}), false);
});

test("tag options are deduplicated, sorted and ignore made and mode fields", () => {
    assert.deepEqual(packageMetadataOptions([plains, {tags: ["color:GREEN", "", "  ", "style:flat"]}, {made: "human", mode: "light"}]), {
        tags: ["color:green", "color:yellow", "cursors", "kind:theme", "made:ai", "style:flat"]
    });
    assert.deepEqual(packageMetadataOptions([{made: "ai", mode: "dark"}]), {tags: []});
});

test("tag matching ignores case and whitespace and made tags are ordinary tags", () => {
    assert.equal(matchesPackageMetadata({made: "human", tags: ["made:ai", " COLOR:Green "]}, {tags: ["made:ai", "color:green"]}), true);
    assert.equal(matchesPackageMetadata({made: "ai"}, {tags: ["made:ai"]}), false);
});

test("tag picker groups by prefix while keeping original filter values", () => {
    const groups = groupPackageTags(["color:yellow", "style:flat", "color:green", "cursors", "made:ai"]);
    assert.deepEqual(groups.map(group => group.name), ["Color", "Made", "Style", "Other"]);
    assert.deepEqual(groups[0].tags, [{value: "color:green", label: "green"}, {value: "color:yellow", label: "yellow"}]);
    assert.deepEqual(groups.at(-1).tags, [{value: "cursors", label: "cursors"}]);
});

test("tag grouping handles duplicates, whitespace and additional colons", () => {
    const groups = groupPackageTags([" Color:green ", "color:GREEN", "", "  ", "style:art:deco", "other:test", "cursors"]);
    assert.equal(groups.length, 4);
    assert.deepEqual(groups.find(group => group.id === "color").tags, [{value: "Color:green", label: "green"}]);
    assert.equal(groups.find(group => group.id === "style").tags[0].label, "art:deco");
    assert.notEqual(groups.find(group => group.id === "other").id, groups.find(group => group.id === "").id);
});
