import {ListBulletIcon, Squares2X2Icon} from "@heroicons/react/24/outline";

export type PackageLayout = "grid" | "list";

type Props = {layout: PackageLayout; onChange: (layout: PackageLayout) => void};

export function PackageLayoutToggle({layout, onChange}: Props) {
    return <div className="packageLayoutToggle" role="group" aria-label="Package layout">
        <button type="button" className="iconBtn iconBtnSubtle" aria-label="Grid layout" title="Grid layout" aria-pressed={layout === "grid"} onClick={() => onChange("grid")}><Squares2X2Icon className="heroIcon"/></button>
        <button type="button" className="iconBtn iconBtnSubtle" aria-label="List layout" title="List layout" aria-pressed={layout === "list"} onClick={() => onChange("list")}><ListBulletIcon className="heroIcon"/></button>
    </div>;
}
