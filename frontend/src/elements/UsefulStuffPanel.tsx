import { ArrowTopRightOnSquareIcon, ChevronDownIcon, ChevronRightIcon } from "@heroicons/react/24/outline";
import { openUrl } from "@tauri-apps/plugin-opener";
import { useEffect, useState, type CSSProperties } from "react";
import roseLogo from "../assets/rose-logo-bg.webp";
import { loadUsefulStuffLinks, type UsefulStuffLink } from "../utils/usefulStuffLinks.ts";

type UsefulStuffPanelProps = {
  onMessage: (message: string) => void;
};

type UsefulStuffTileStyle = CSSProperties & {
  "--useful-stuff-accent-from": string;
  "--useful-stuff-accent-to": string;
};

const USEFUL_STUFF_COLLAPSED_STORAGE_KEY = "roToolbox.usefulStuffCollapsed";

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  if (typeof error === "string" && error.trim()) {
    return error;
  }
  if (error && typeof error === "object" && "message" in error && typeof error.message === "string") {
    return error.message;
  }
  return fallback;
}

function readInitialCollapsedState() {
  try {
    return window.localStorage.getItem(USEFUL_STUFF_COLLAPSED_STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

function renderLinkTitle(title: string) {
  if (!title.startsWith("ROSE ")) {
    return title;
  }

  return (
    <>
      <img src={roseLogo} alt="ROSE" className="usefulStuffTitleLogo" />
      <span className="usefulStuffTitleText">{title.slice("ROSE ".length)}</span>
    </>
  );
}

export function UsefulStuffPanel({ onMessage }: UsefulStuffPanelProps) {
  const [links, setLinks] = useState<UsefulStuffLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState(readInitialCollapsedState);

  useEffect(() => {
    let cancelled = false;

    loadUsefulStuffLinks()
      .then((items) => {
        if (cancelled) {
          return;
        }
        setLinks(items);
        setLoadError(null);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        const message = toErrorMessage(error, "Failed to load useful links.");
        setLinks([]);
        setLoadError(message);
        onMessage(message);
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [onMessage]);

  useEffect(() => {
    try {
      window.localStorage.setItem(USEFUL_STUFF_COLLAPSED_STORAGE_KEY, String(collapsed));
    } catch {
      // Ignore storage failures and keep the in-memory toggle state.
    }
  }, [collapsed]);

  async function onOpenLink(link: UsefulStuffLink) {
    try {
      await openUrl(link.url);
    } catch (error) {
      onMessage(toErrorMessage(error, `Failed to open ${link.title}.`));
    }
  }

  return (
    <div className="card sidebarPanel usefulStuffPanel">
      <div className="usefulStuffHeader">
        <div>
          <p className="sectionTitle usefulStuffPanelTitle">Useful stuff</p>
          {!collapsed ? <p className="activeProfileMeta usefulStuffPanelMeta">Quick links to favorite ROSE services.</p> : null}
        </div>
        <button
          type="button"
          className="buttonSubtle usefulStuffCollapseButton"
          onClick={() => setCollapsed((value) => !value)}
          aria-expanded={!collapsed}
          aria-label={collapsed ? "Show useful stuff links" : "Hide useful stuff links"}
          title={collapsed ? "Show useful stuff links" : "Hide useful stuff links"}
        >
          {collapsed ? <ChevronRightIcon /> : <ChevronDownIcon />}
        </button>
      </div>
      {collapsed ? null : loading ? (
        <p className="usefulStuffEmpty">Loading links...</p>
      ) : loadError ? (
        <p className="usefulStuffEmpty">{loadError}</p>
      ) : (
        <div className="usefulStuffList">
          {links.map((link) => {
            const tileStyle: UsefulStuffTileStyle = {
              "--useful-stuff-accent-from": link.accentFrom,
              "--useful-stuff-accent-to": link.accentTo
            };

            return (
              <button
                key={link.id}
                type="button"
                className="usefulStuffTile"
                style={tileStyle}
                onClick={() => {
                  void onOpenLink(link);
                }}
                title={`Open ${link.title}`}
              >
                <span className="usefulStuffTileContent">
                  <span className="usefulStuffTileTitle">{renderLinkTitle(link.title)}</span>
                  <span className="usefulStuffTileDescription">{link.description}</span>
                </span>
                <span className="usefulStuffExternalIcon" aria-hidden="true">
                  <ArrowTopRightOnSquareIcon />
                </span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
