import {useEffect, useId, useRef, useState} from "react";
import {createPortal} from "react-dom";
import {XMarkIcon} from "@heroicons/react/24/outline";
import {groupPackageTags} from "../utils/packageMetadata";

type Props = {open: boolean; tags: string[]; selected: string[]; onChange: (tags: string[]) => void; onClose: () => void};
export function PackageTagsModal({open, tags, selected, onChange, onClose}: Props) {
    const [search, setSearch] = useState("");
    const dialog = useRef<HTMLElement>(null);
    const titleId = useId();
    const helpId = useId();
    useEffect(() => {
        if (!open) return;
        setSearch("");
        const previous = document.activeElement;
        dialog.current?.querySelector<HTMLInputElement>("input")?.focus();
        const keydown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {event.preventDefault(); event.stopPropagation(); onClose();}
            if (event.key !== "Tab") return;
            const elements = dialog.current?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), [tabindex="0"]');
            if (!elements?.length) return;
            const first = elements[0], last = elements[elements.length - 1];
            if (event.shiftKey && document.activeElement === first) {event.preventDefault(); last.focus();}
            else if (!event.shiftKey && document.activeElement === last) {event.preventDefault(); first.focus();}
        };
        document.addEventListener("keydown", keydown, true);
        return () => {document.removeEventListener("keydown", keydown, true); if (previous instanceof HTMLElement && previous.isConnected) previous.focus();};
    }, [open, onClose]);
    if (!open) return null;
    const query = search.trim().toLowerCase();
    const groups = groupPackageTags(tags).map(group => ({...group, tags: group.tags.filter(tag => `${group.name} ${tag.value}`.toLowerCase().includes(query))})).filter(group => group.tags.length);
    function toggle(tag: string) {onChange(selected.includes(tag) ? selected.filter(value => value !== tag) : [...selected, tag]);}
    return createPortal(<div className="modalBackdrop packageTagsBackdrop" onClick={onClose}>
        <section ref={dialog} className="card modalCard packageTagsModal" role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={helpId} onClick={event => event.stopPropagation()}>
            <div className="modalHeader"><h2 id={titleId}>Filter by tags</h2><button type="button" className="iconBtn iconBtnSubtle" aria-label="Close tag filters" onClick={onClose}><XMarkIcon className="heroIcon"/></button></div>
            <p className="activeProfileMeta" id={helpId}>Toggle tags to filter packages. Packages must match all selected tags.</p>
            <input type="search" placeholder="Find tags..." aria-label="Find tags" value={search} onChange={event => setSearch(event.target.value)}/>
            <div className="packageTagGroups">
                {groups.map(group => <section className="packageTagGroup" key={group.id} aria-label={group.name}>
                    <h3 className="settingsSectionLabel">{group.name}</h3>
                    <div className="packageTagToggles">{group.tags.map(tag => <button type="button" role="switch" aria-checked={selected.includes(tag.value)} aria-label={`${group.name}: ${tag.label}`} key={tag.value} className="packageTagToggle" onClick={() => toggle(tag.value)}>
                        <span>{tag.label}</span><span className="packageTagSwitch" aria-hidden="true"/>
                    </button>)}</div>
                </section>)}
                {!groups.length && <p className="profileCardEmpty" role="status">No tags match your search.</p>}
            </div>
            <div className="packageTagsFooter"><span className="activeProfileMeta">{selected.length} selected</span><button type="button" className="buttonSubtle" disabled={!selected.length} onClick={() => onChange([])}>Remove All</button><button type="button" className="buttonStrong" onClick={onClose}>Done</button></div>
        </section>
    </div>, document.body);
}
