import {useEffect, useState} from "react";
import type {AvailableProfile} from "../types";
import {emptyMetadataFilters, packageMetadataOptions} from "../utils/packageMetadata";

export function usePackageMetadataFilters(profiles: AvailableProfile[], open: boolean) {
    const [filters, setFilters] = useState(emptyMetadataFilters);
    useEffect(() => {if (!open) setFilters(emptyMetadataFilters());}, [open]);
    return {filters, setFilters, options: packageMetadataOptions(profiles)};
}
