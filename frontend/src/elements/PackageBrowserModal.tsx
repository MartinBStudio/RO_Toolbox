import {useEffect, useState} from "react";
import {openUrl} from "@tauri-apps/plugin-opener";
import {ArrowDownTrayIcon, ArrowTopRightOnSquareIcon, ChevronLeftIcon, ChevronRightIcon, Cog6ToothIcon, MagnifyingGlassIcon, TrashIcon, XMarkIcon} from "@heroicons/react/24/outline";
import type {AvailableProfile, ProfileInfo} from "../types";
import {buildProfileMeta, isProfileAlreadyInstalled, resolveProfileName} from "../formatting";
import {ServiceDetailModal} from "./ServiceDetailModal";
import {FavoritePackageButton} from "./FavoritePackageButton";
import {FavoritesFilterToggle} from "./FavoritesFilterToggle";
import {PackageLayoutToggle, type PackageLayout} from "./PackageLayoutToggle";
import {packageBrowserSummary} from "../utils/packageBrowserSummary";
import {usePackageFavorites, type ResourceType} from "../hooks/usePackageFavorites";
import {usePackageMetadataFilters} from "../hooks/usePackageMetadataFilters";
import {PackageMetadataFilters} from "./PackageMetadataFilters";
import {filterPackageProfiles} from "../utils/packageBrowserFilters";

type Props = {
    open: boolean;
    resourceType: ResourceType;
    title: string;
    profiles: AvailableProfile[];
    installedProfile?: ProfileInfo | null;
    selectedProfile: string;
    recommendedId?: string;
    loading: boolean;
    canInstall: boolean;
    onSelect: (id: string) => void;
    onInstall: (id: string) => void | Promise<void>;
    onUninstall: () => void | Promise<void>;
    onDetails?: (profile: AvailableProfile) => void;
    onManage?: () => void;
    onClose: () => void;
    onMessage: (message: string) => void;
};

export function PackageBrowserModal({open, resourceType, title, profiles, installedProfile, selectedProfile, recommendedId, loading, canInstall, onSelect, onInstall, onUninstall, onDetails, onManage, onClose, onMessage}: Props) {
    const {isFavorite, isSortingFavorite, toggleFavorite, favoritesDisabled, hasFavorites, favoritesOnly, setFavoritesOnly} = usePackageFavorites(resourceType, open, onMessage);
    const [search, setSearch] = useState("");
    const {filters, setFilters, options} = usePackageMetadataFilters(profiles, open);
    const [layout, setLayout] = useState<PackageLayout>("grid");
    const [gallery, setGallery] = useState<{images: string[]; index: number; name: string} | null>(null);
    const visible = filterPackageProfiles(profiles, {installedProfile, search, favoritesOnly, isFavorite, metadata: filters});
    const sorted = [...visible].sort((a, b) =>
        Number(isSortingFavorite(resolveProfileName(b.name, b.id))) - Number(isSortingFavorite(resolveProfileName(a.name, a.id)))
        || Number(isProfileAlreadyInstalled(b, installedProfile)) - Number(isProfileAlreadyInstalled(a, installedProfile))
        || Number(b.id === recommendedId) - Number(a.id === recommendedId)
    );

    useEffect(() => {if (!open) {setSearch(""); setGallery(null);}}, [open]);
    useEffect(() => {
        if (!gallery) return;
        const keydown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {event.preventDefault(); setGallery(null);}
            if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
                event.preventDefault();
                const direction = event.key === "ArrowLeft" ? -1 : 1;
                setGallery(current => current && {...current, index: (current.index + direction + current.images.length) % current.images.length});
            }
        };
        window.addEventListener("keydown", keydown);
        return () => window.removeEventListener("keydown", keydown);
    }, [gallery !== null]);

    async function visit(url: string) {
        try {await openUrl(url);} catch (error) {onMessage(error instanceof Error ? error.message : "Failed to open package page.");}
    }
    function move(direction: number) {
        setGallery(current => current && {...current, index: (current.index + direction + current.images.length) % current.images.length});
    }

    return <>
        <ServiceDetailModal open={open} title={title} description={packageBrowserSummary(profiles, visible, installedProfile, isFavorite, favoritesOnly)} onClose={onClose} headerContent={
            <>
            <div className="packageBrowserFilters">
                <input type="search" className="profileBrowserSearch" aria-label={`Search ${title.toLowerCase()} packages by name`} placeholder="Search packages by name..." value={search} onChange={event => setSearch(event.target.value)} autoFocus/>
                {hasFavorites && <FavoritesFilterToggle checked={favoritesOnly} disabled={favoritesDisabled} onChange={setFavoritesOnly}/>}
                <PackageLayoutToggle layout={layout} onChange={setLayout}/>
            </div>
            <PackageMetadataFilters filters={filters} options={options} onChange={setFilters}/>
            </>
        }>
            {profiles.length === 0 ? <p className="profileCardEmpty">No packages downloaded yet.</p> : sorted.length === 0 ? <p className="profileCardEmpty" role="status">No packages match your search or filters.</p> :
                <div className={layout === "list" ? "profileListBrowser" : "profileGridBrowser"}>
                    {sorted.map(profile => {
                        const name = resolveProfileName(profile.name, profile.id);
                        const images = (profile.previewImages ?? []).filter(image => image.trim());
                        const installed = isProfileAlreadyInstalled(profile, installedProfile);
                        const meta = buildProfileMeta({version: profile.version, author: profile.author, separator: " · "});
                        return <article key={profile.id} className={`profileBrowseCard${selectedProfile === profile.id ? " isSelected" : ""}`}>
                            <div className="profileBrowsePreviewWrap">
                                {images.length > 0 ? <button type="button" className="profileBrowsePreviewButton" aria-label={`Preview ${name}`} onClick={() => {onSelect(profile.id); setGallery({images, index: 0, name});}}>
                                    <img src={images[0]} alt={name} loading="lazy"/>
                                </button> : <button type="button" className="profileBrowsePlaceholder" aria-label={`No preview available for ${name}`} onClick={() => onSelect(profile.id)}>No preview</button>}
                                {images.length > 1 && <span className="profileBrowseCounter">{images.length} shots</span>}
                            </div>
                            <div className="profileBrowseBody">
                                <div className="profileBrowseHeader"><div>
                                    <div className="packageNameWithFavorite"><p className="profileBrowseName">{name}</p></div>
                                    {meta && <p className="profileCardMeta">{meta}</p>}
                                </div></div>
                                {profile.id === recommendedId && <span className="servicePanelMetaChip">Recommended</span>}
                                {profile.description && <p className="profileBrowseDesc">{profile.description}</p>}
                                <div className="profileBrowseActions">
                            {profile.url && <button type="button" className="iconBtn iconBtnSubtle iconBtnDim" title="Open package page" aria-label={`Open package page for ${name}`} onClick={() => void visit(profile.url!)}><ArrowTopRightOnSquareIcon className="heroIcon"/></button>}
                            <FavoritePackageButton name={name} favorite={isFavorite(name)} disabled={favoritesDisabled} onToggle={() => void toggleFavorite(name)}/>
                                    {installed && onManage ? <button type="button" className="iconBtn iconBtnSubtle iconBtnDim" title="Manage installed packages" aria-label="Manage installed packages" disabled={loading || !canInstall} onClick={onManage}><Cog6ToothIcon className="heroIcon"/></button> : onDetails && profile.managedSubfolders.length > 0 && <button type="button" className="iconBtn iconBtnSubtle iconBtnDim" title="View models" aria-label={`View models in ${name}`} onClick={() => {onSelect(profile.id); onDetails(profile);}}><MagnifyingGlassIcon className="heroIcon"/></button>}
                                    <button type="button" className={`iconBtn ${installed ? "iconBtnDanger iconBtnDim" : "buttonStrong packageInstallButton"}`} title={installed ? "Uninstall" : "Install"} aria-label={`${installed ? "Uninstall" : "Install"} ${name}`} disabled={loading || (!installed && !canInstall)} onClick={() => installed ? void onUninstall() : void onInstall(profile.id)}>{installed ? <TrashIcon className="heroIcon"/> : <ArrowDownTrayIcon className="heroIcon"/>}</button>
                                </div>
                            </div>
                        </article>;
                    })}
                </div>}
        </ServiceDetailModal>
        {open && gallery && <div className="imagePreviewModalOverlay" role="dialog" aria-modal="true" aria-label={`${gallery.name} preview gallery`} onClick={() => setGallery(null)}>
            <button type="button" className="imagePreviewModalClose iconBtn iconBtnSubtle" aria-label="Close preview" onClick={() => setGallery(null)}><XMarkIcon className="heroIcon"/></button>
            {gallery.images.length > 1 && <button type="button" className="packageGalleryNav packageGalleryPrev" aria-label="Previous preview" onClick={event => {event.stopPropagation(); move(-1);}}><ChevronLeftIcon className="heroIcon"/></button>}
            <div className="imagePreviewModalContent" onClick={event => event.stopPropagation()}><img className="imagePreviewModalImage" src={gallery.images[gallery.index]} alt={`${gallery.name} preview ${gallery.index + 1} of ${gallery.images.length}`}/></div>
            {gallery.images.length > 1 && <button type="button" className="packageGalleryNav packageGalleryNext" aria-label="Next preview" onClick={event => {event.stopPropagation(); move(1);}}><ChevronRightIcon className="heroIcon"/></button>}
        </div>}
    </>;
}
