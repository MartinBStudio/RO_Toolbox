import { useEffect, useMemo, useRef, useState } from "react";
import type { ProfileOptionGroup } from "../formatting.ts";
import { formatManifestVersion } from "../formatting.ts";

type ProfileDropdownProps = {
  groups: ProfileOptionGroup[];
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
};

export function ProfileDropdown({
  groups,
  value,
  disabled = false,
  onChange
}: ProfileDropdownProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const options = useMemo(
    () => groups.flatMap((group) => group.options),
    [groups]
  );
  const selectedOption = options.find((option) => option.profile.id === value) ?? null;

  useEffect(() => {
    if (!open) {
      return;
    }

    function onDocumentMouseDown(event: MouseEvent) {
      if (!containerRef.current) {
        return;
      }
      if (containerRef.current.contains(event.target as Node)) {
        return;
      }
      setOpen(false);
    }

    function onEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", onDocumentMouseDown);
    document.addEventListener("keydown", onEscape);
    return () => {
      document.removeEventListener("mousedown", onDocumentMouseDown);
      document.removeEventListener("keydown", onEscape);
    };
  }, [open]);

  const triggerLabel = selectedOption
    ? `${selectedOption.label}${selectedOption.profile.version ? ` (${formatManifestVersion(selectedOption.profile.version)})` : ""}`
    : "No profile selected";

  return (
    <div className="profileDropdown" ref={containerRef}>
      <button
        type="button"
        className="profileDropdownTrigger"
        disabled={disabled || options.length === 0}
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        {triggerLabel}
      </button>
      {open ? (
        <div className="profileDropdownMenu" role="listbox">
          {groups.map((group) => (
            <div key={group.label || "__default__"} className="profileDropdownGroup">
              {group.label ? (
                <div className="profileDropdownGroupLabel">{group.label}</div>
              ) : null}
              {group.options.map((option) => {
                const isSelected = option.profile.id === value;
                return (
                  <button
                    key={option.profile.id}
                    type="button"
                    className={`profileDropdownOption${isSelected ? " isSelected" : ""}`}
                    role="option"
                    aria-selected={isSelected}
                    onClick={() => {
                      onChange(option.profile.id);
                      setOpen(false);
                    }}
                  >
                    {option.label}
                    {option.profile.version ? ` (${formatManifestVersion(option.profile.version)})` : ""}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
