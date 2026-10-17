

type Props = {checked: boolean; disabled: boolean; onChange: (checked: boolean) => void};

export function FavoritesFilterToggle({checked, disabled, onChange}: Props) {
    return <label className="packageFavoritesFilter" title="Show favorites and the installed package">
        <input type="checkbox" role="switch" checked={checked} disabled={disabled} onChange={event => onChange(event.target.checked)}/>
        <span>Favorites only</span>
    </label>;
}
