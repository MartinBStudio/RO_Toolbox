import {StarIcon} from "@heroicons/react/24/outline";

type Props = {name: string; favorite: boolean; disabled: boolean; onToggle: () => void};

export function FavoritePackageButton({name, favorite, disabled, onToggle}: Props) {
    const label = `${favorite ? "Remove" : "Add"} ${name} ${favorite ? "from" : "to"} favorites`;
    return <button type="button" className={`iconBtn iconBtnSubtle packageFavoriteButton${favorite ? " isFavorite" : ""}`} aria-label={label} title={label} aria-pressed={favorite} disabled={disabled} onClick={onToggle}>
        <StarIcon className="heroIcon"/>
    </button>;
}
