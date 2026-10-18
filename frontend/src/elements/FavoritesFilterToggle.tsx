import {StarIcon} from "@heroicons/react/24/outline";

type Props = {checked: boolean; disabled: boolean; onChange: (checked: boolean) => void};

export function FavoritesFilterToggle({checked, disabled, onChange}: Props) {
    return <button type="button" className="iconBtn iconBtnSubtle packageFavoritesFilter" title="Show favorites and the installed package" aria-label="Show favorites and the installed package" aria-pressed={checked} disabled={disabled} onClick={() => onChange(!checked)}>
        <StarIcon className="heroIcon"/>
    </button>;
}
