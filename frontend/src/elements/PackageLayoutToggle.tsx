import {ListBulletIcon, Squares2X2Icon} from "@heroicons/react/24/outline";

export type PackageLayout = "grid" | "list";

type Props = {layout: PackageLayout; onChange: (layout: PackageLayout) => void};

export function PackageLayoutToggle({layout, onChange}: Props) {
    const next = layout === "grid" ? "list" : "grid";
    return <button type="button" className="iconBtn iconBtnSubtle packageLayoutToggle"
        aria-label={`Switch to ${next} layout`}
        title={`${layout === "grid" ? "Grid" : "List"} layout — switch to ${next}`}
        onClick={() => onChange(next)}>
        {layout === "grid" ? <Squares2X2Icon className="heroIcon" aria-hidden="true"/> : <ListBulletIcon className="heroIcon" aria-hidden="true"/>}
    </button>;
}
