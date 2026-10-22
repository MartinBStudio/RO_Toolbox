import {test, mock} from "node:test";
import assert from "node:assert/strict";
import {matchesPackageMetadata, packageMetadataOptions} from "../src/utils/packageMetadata.ts";

let response;
mock.module(new URL("../src/backendConnector/apiClient.ts", import.meta.url).href, {
    namedExports: {request: async path => path === "/status" ? {version: "test", troseRunning: false} : response}
});
const {getStatus} = await import("../src/backendConnector/appApi.ts");

const services = [
    ["availableProfiles", "installedProfile"],
    ["combatTextAvailableProfiles", "combatTextInstalledProfile"],
    ["userInterfaceAvailableProfiles", "userInterfaceInstalledProfile"],
    ["buffIconsAvailableProfiles", "buffIconsInstalledProfile"],
    ["buffsAvailableProfiles", "buffsInstalledProfile"]
];

test("API status mapping preserves metadata and exposes filter options for every service", async () => {
    const profile = {id: "plains", name: "Adventurer's Plains", version: "1.3.5", made: "ai", mode: "dark", tags: ["kind:theme", "made:ai", "color:green", "color:yellow", "cursors"]};
    response = {installedProfile: profile, availableProfiles: [profile]};
    const status = await getStatus();
    for (const [available, installed] of services) {
        assert.equal("made" in status[available][0], false);
        assert.equal("mode" in status[available][0], false);
        assert.deepEqual(status[available][0].tags, profile.tags, available);
        assert.deepEqual(status[installed].tags, profile.tags, installed);
        assert.deepEqual(packageMetadataOptions(status[available]).tags, [...profile.tags].sort());
        assert.equal(matchesPackageMetadata(status[available][0], {tags: ["color:green"]}), true);
    }
});

test("API mapping safely defaults missing or malformed optional metadata", async () => {
    response = {installedProfile: {name: "Legacy"}, availableProfiles: [{id: "legacy"}, {id: "invalid", made: 12, mode: false, tags: [null, 1, "color:red"]}]};
    const status = await getStatus();
    for (const [available, installed] of services) {
        assert.deepEqual(status[available][0].tags, []);
        assert.deepEqual(status[installed].tags, []);
        assert.deepEqual(status[available][1].tags, ["color:red"]);
    }
});
