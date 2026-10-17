import {request} from "./apiClient";

export type GlobalThemeStatus = {
    root: string;
    revision: string;
    tint: string | null;
    transparency: number;
    fileCount: number;
    applied: boolean;
};
export const getGlobalTheme = () => request<GlobalThemeStatus>("/theme-editor/global");
export const applyGlobalTheme = (revision: string, tint: string | null, transparency: number) =>
    request<GlobalThemeStatus>("/theme-editor/global", {
        method: "POST", body: JSON.stringify({revision, tint, transparency})
    });
export const restoreGlobalTheme = (revision: string) => request<GlobalThemeStatus>("/theme-editor/global/restore", {
    method: "POST", body: JSON.stringify({revision})
});
