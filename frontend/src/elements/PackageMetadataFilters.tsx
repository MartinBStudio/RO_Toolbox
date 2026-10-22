import type {MetadataFilters, MetadataOptions} from "../utils/packageMetadata";
import {emptyMetadataFilters} from "../utils/packageMetadata";
import {useCallback, useState} from "react";
import {PackageTagsModal} from "./PackageTagsModal";

type Props = {filters: MetadataFilters; options: MetadataOptions; onChange: (filters: MetadataFilters) => void};
export function PackageMetadataFilters({filters, options, onChange}: Props) {
    const [pickerOpen, setPickerOpen] = useState(false);
    const closePicker = useCallback(() => setPickerOpen(false), []);
    const active = filters.tags.length > 0;
    if (!options.tags.length && !active) return null;
    return <div className="packageMetadataFilters" role="group" aria-label="Package metadata filters">
        <div className="packageMetadataFilterRow">
            {options.tags.length > 0 && <button type="button" className="buttonSubtle" aria-haspopup="dialog" onClick={() => setPickerOpen(true)}>Add tags{active ? ` (${filters.tags.length})` : ""}</button>}
            {active && <button type="button" className="buttonSubtle" onClick={() => onChange(emptyMetadataFilters())}>Remove All</button>}
        </div>
        {filters.tags.length > 0 && <div className="packageSelectedTags">{filters.tags.map(tag => <button type="button" key={tag} className="packageTagChip" aria-label={`Remove tag ${tag}`} onClick={() => onChange({...filters, tags: filters.tags.filter(value => value !== tag)})}>{tag.replace(":", ": ")} <span aria-hidden="true">×</span></button>)}</div>}
        <PackageTagsModal open={pickerOpen} tags={options.tags} selected={filters.tags} onChange={tags => onChange({...filters, tags})} onClose={closePicker}/>
    </div>;
}
