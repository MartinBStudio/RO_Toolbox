import {useEffect, useState} from "react";
import {applyGlobalTheme, getGlobalTheme, restoreGlobalTheme, type GlobalThemeStatus} from "../backendConnector/themeEditorApi";
import {ConfirmationModal} from "../elements/ConfirmationModal";
import {ArrowPathIcon, ArrowUturnLeftIcon} from "@heroicons/react/24/outline";
import {FloppyDiskIcon} from "../elements/FloppyDiskIcon";

type Props = {onMessage: (message: string) => void};
export function ThemeEditorManager({onMessage}: Props) {
    const [status, setStatus] = useState<GlobalThemeStatus | null>(null);
    const [tint, setTint] = useState<string | null>(null);
    const [transparency, setTransparency] = useState(0);
    const [busy, setBusy] = useState(true);
    const [error, setError] = useState("");
    const [restoreOpen, setRestoreOpen] = useState(false);

    function accept(next: GlobalThemeStatus) {
        setStatus(next); setTint(next.tint); setTransparency(next.transparency);
    }
    useEffect(() => {
        let active = true;
        getGlobalTheme().then(next => {if (active) accept(next);})
            .catch(err => {if (active) setError(err instanceof Error ? err.message : String(err));})
            .finally(() => {if (active) setBusy(false);});
        return () => {active = false;};
    }, []);

    async function reload() {
        setBusy(true); setError("");
        try {accept(await getGlobalTheme());}
        catch (err) {setError(err instanceof Error ? err.message : String(err));}
        finally {setBusy(false);}
    }
    async function save(restore = false) {
        if (!status || busy) return;
        setRestoreOpen(false); setBusy(true); setError("");
        try {
            accept(await (restore ? restoreGlobalTheme(status.revision) : applyGlobalTheme(status.revision, tint, transparency)));
            onMessage(restore ? "Default UI appearance restored." : "Global theme applied. Reopen game windows or restart ROSE Online to see the changes.");
        } catch (err) {setError(err instanceof Error ? err.message : String(err));}
        finally {setBusy(false);}
    }
    const background = tint ?? "#212121";
    return <section className="themeEditor" aria-busy={busy}>
        <div className="themeEditorHeader">
            <p className="sectionTitle">Theme editor</p>
            <p className="activeProfileMeta">Adjust background tint and transparency across your entire UI.</p>
        </div>
        {error && <div className="themeError" role="alert">{error} <button disabled={busy} onClick={() => void reload()}>Reload</button></div>}
        {!status ? <p className="activeProfileMeta">{busy ? "Remembering default appearance..." : "Theme settings could not be loaded."}</p> : <>
            <div className="themeEditorBody">
            <section className="themeSection" aria-labelledby="themeAppearanceTitle">
            <div className="themeSectionHeader">
                <p className="settingsSectionLabel" id="themeAppearanceTitle">Appearance</p>
                <div className="headerActions configEditorActions">
                    <button type="button" className="iconBtn iconBtnSubtle" disabled={busy} title="Reload theme settings" aria-label="Reload theme settings" onClick={() => void reload()}><ArrowPathIcon className="heroIcon" aria-hidden="true"/></button>
                </div>
            </div>
            <div className="themeGlobalControls">
                <label className="themeTintControl">Background tint
                    <input type="color" value={background} disabled={busy} onChange={e => setTint(e.target.value)}/>
                    <span>{tint ?? "Original colors"}</span>
                </label>
                <button className="buttonSubtle" disabled={busy || tint === null} onClick={() => setTint(null)}>Use original colors</button>
                <label className="themeTransparencyControl">Transparency <strong>{transparency}%</strong>
                    <input type="range" min="0" max="100" value={transparency} disabled={busy} onChange={e => setTransparency(Number(e.target.value))}/>
                    <small>Original opacity at 0% · Invisible backgrounds at 100%</small>
                </label>
            </div>
            <div className="themeActions configEditorActions">
                <button type="button" className="iconBtn buttonStrong" title="Save theme" aria-label="Save theme" disabled={busy || status.fileCount === 0} onClick={() => void save()}><FloppyDiskIcon/></button>
                <button type="button" className="iconBtn iconBtnSubtle" title="Restore defaults" aria-label="Restore defaults" disabled={busy || status.fileCount === 0} onClick={() => setRestoreOpen(true)}><ArrowUturnLeftIcon className="heroIcon" aria-hidden="true"/></button>
            </div>
            <p className="themeHint">Defaults are remembered automatically and kept between app restarts. Restore defaults undoes all Theme editor changes.</p>
            </section>
            <section className="themeSection" aria-labelledby="themePreviewTitle">
            <p className="settingsSectionLabel" id="themePreviewTitle">Live preview</p>
            <div className="themePreviewGrid" aria-label="Live background preview">
                {["checker", "scene"].map(scene => <div key={scene} className={`themePreview themePreview-${scene}`}>
                    <div className="themeSample" style={{backgroundColor: `${background}${Math.round(255 * (1 - transparency / 100)).toString(16).padStart(2, "0")}`}}>
                        <strong>Window preview</strong><div className="themeSampleDivider"/>
                        <p>Character · Inventory · Chat</p>
                        <div className="themeSlots">{[1, 2, 3, 4].map(i => <span key={i}>✦</span>)}</div>
                        <p>Text and icons stay clear</p>
                    </div>
                </div>)}
            </div>
            <p className="themeHint">Preview is approximate. Each background keeps its original opacity proportions.</p>
            </section>
            </div>
        </>}
        <ConfirmationModal open={restoreOpen} title="Restore default appearance?" message="Restore all UI files to their remembered defaults? Your current tint and transparency will be removed." confirmLabel="Restore defaults" onConfirm={() => void save(true)} onClose={() => setRestoreOpen(false)}/>
    </section>;
}
