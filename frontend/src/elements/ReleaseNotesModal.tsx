import { useMemo, useState, useEffect } from "react";
import {
  SparklesIcon,
  XMarkIcon,
  MagnifyingGlassIcon,
  PaintBrushIcon,
  WrenchScrewdriverIcon,
  RocketLaunchIcon,
  TagIcon,
  CalendarDaysIcon,
  CheckCircleIcon,
  ChevronRightIcon,
} from "@heroicons/react/24/outline";
import { parseReleaseNotes, ReleaseCategory, ReleaseEntry } from "../utils/releaseNotesParser";
import roseLogo from "../assets/rose-logo-bg.webp";

type ReleaseNotesModalProps = {
  open: boolean;
  content: string;
  onClose: () => void;
};

function renderCategoryIcon(iconType: ReleaseCategory["iconType"]) {
  switch (iconType) {
    case "feature":
      return <SparklesIcon className="releaseCategoryIcon feature" aria-hidden="true" />;
    case "ui":
      return <PaintBrushIcon className="releaseCategoryIcon ui" aria-hidden="true" />;
    case "fix":
      return <WrenchScrewdriverIcon className="releaseCategoryIcon fix" aria-hidden="true" />;
    case "improvement":
      return <RocketLaunchIcon className="releaseCategoryIcon improvement" aria-hidden="true" />;
    case "tool":
      return <TagIcon className="releaseCategoryIcon tool" aria-hidden="true" />;
    default:
      return <CheckCircleIcon className="releaseCategoryIcon general" aria-hidden="true" />;
  }
}

export function ReleaseNotesModal({ open, content, onClose }: ReleaseNotesModalProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [activeVersionFilter, setActiveVersionFilter] = useState<string>("all");

  const parsed = useMemo(() => parseReleaseNotes(content), [content]);

  // Handle Escape key to close modal
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  // Reset filter when opened
  useEffect(() => {
    if (open) {
      setSearchQuery("");
      setActiveVersionFilter("all");
    }
  }, [open]);

  const filteredReleases = useMemo(() => {
    let list = parsed.releases;

    if (activeVersionFilter !== "all") {
      list = list.filter((r) => r.version === activeVersionFilter);
    }

    if (!searchQuery.trim()) {
      return list;
    }

    const query = searchQuery.toLowerCase().trim();

    return list
      .map((release) => {
        // Check if release matches header
        const releaseMatches =
          release.version.toLowerCase().includes(query) ||
          release.title.toLowerCase().includes(query);

        if (releaseMatches) {
          return release;
        }

        // Filter categories and items
        const matchingCategories = release.categories
          .map((cat) => {
            const catMatches = cat.title.toLowerCase().includes(query);
            if (catMatches) {
              return cat;
            }
            const matchingItems = cat.items.filter(
              (item) =>
                item.rawText.toLowerCase().includes(query) ||
                (item.tag && item.tag.toLowerCase().includes(query))
            );
            return matchingItems.length > 0 ? { ...cat, items: matchingItems } : null;
          })
          .filter(Boolean) as ReleaseCategory[];

        if (matchingCategories.length > 0) {
          return { ...release, categories: matchingCategories };
        }

        return null;
      })
      .filter(Boolean) as ReleaseEntry[];
  }, [parsed.releases, activeVersionFilter, searchQuery]);

  if (!open) {
    return null;
  }

  const totalItemsCount = parsed.releases.reduce(
    (acc, rel) => acc + rel.categories.reduce((cAcc, cat) => cAcc + cat.items.length, 0),
    0
  );

  return (
    <div className="modalBackdrop" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="whatsNewModalTitle">
      <section
        className="card modalCardWide releaseNotesModalCard"
        onClick={(event) => event.stopPropagation()}
      >
        {/* Modal Header */}
        <header className="releaseNotesHeader">
          <div className="releaseNotesHeaderMain">
            <div className="releaseNotesBadgeIconWrapper">
              <img src={roseLogo} alt="ROSE" className="releaseNotesHeaderLogo" />
              <span className="releaseNotesSparklePill">
                <SparklesIcon className="releaseNotesSparkleIcon" />
              </span>
            </div>
            <div className="releaseNotesHeaderText">
              <div className="releaseNotesEyebrow">RO TOOLBOX UPDATES</div>
              <h2 id="whatsNewModalTitle" className="releaseNotesTitle">
                What&apos;s New
              </h2>
            </div>
          </div>
          <button
            type="button"
            className="buttonSubtle releaseNotesCloseButton"
            onClick={onClose}
            aria-label="Close What's New modal"
            title="Close"
          >
            <XMarkIcon className="releaseNotesCloseIcon" />
          </button>
        </header>

        {/* Toolbar: Search & Version Filters */}
        <div className="releaseNotesToolbar">
          <div className="releaseNotesSearchWrapper">
            <MagnifyingGlassIcon className="releaseNotesSearchIcon" />
            <input
              type="text"
              className="releaseNotesSearchInput"
              placeholder="Search features, fixes, or keywords..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search release notes"
            />
            {searchQuery && (
              <button
                type="button"
                className="releaseNotesSearchClear"
                onClick={() => setSearchQuery("")}
                aria-label="Clear search"
              >
                ✕
              </button>
            )}
          </div>

          {parsed.releases.length > 1 && (
            <div className="releaseNotesVersionTabs" role="tablist" aria-label="Version filter">
              <button
                type="button"
                className={`releaseNotesVersionTab ${activeVersionFilter === "all" ? "active" : ""}`}
                onClick={() => setActiveVersionFilter("all")}
                role="tab"
                aria-selected={activeVersionFilter === "all"}
              >
                All Releases ({parsed.releases.length})
              </button>
              {parsed.releases.slice(0, 4).map((rel) => (
                <button
                  key={rel.id}
                  type="button"
                  className={`releaseNotesVersionTab ${activeVersionFilter === rel.version ? "active" : ""}`}
                  onClick={() => setActiveVersionFilter(rel.version)}
                  role="tab"
                  aria-selected={activeVersionFilter === rel.version}
                >
                  {rel.version}
                  {rel.isLatest && <span className="tabLatestBadge">Latest</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Modal Scrollable Body */}
        <div className="releaseNotesBody">
          {filteredReleases.length === 0 ? (
            <div className="releaseNotesEmptyState">
              <MagnifyingGlassIcon className="releaseNotesEmptyIcon" />
              <div className="releaseNotesEmptyTitle">No matching updates found</div>
              <p className="releaseNotesEmptyDesc">
                Try searching for a different keyword or reset the filter.
              </p>
              <button
                type="button"
                className="buttonSubtle releaseNotesResetSearch"
                onClick={() => {
                  setSearchQuery("");
                  setActiveVersionFilter("all");
                }}
              >
                Reset Search
              </button>
            </div>
          ) : (
            <div className="releaseTimeline">
              {filteredReleases.map((release) => (
                <article key={release.id} className="releaseTimelineCard">
                  {/* Release Version Header */}
                  <div className="releaseCardHeader">
                    <div className="releaseCardHeaderLeft">
                      <div className="releaseVersionBadgeGroup">
                        <span className={`releaseVersionPill ${release.isLatest ? "latest" : ""}`}>
                          {release.version}
                        </span>
                        {release.isLatest && (
                          <span className="releaseLatestTag">
                            <SparklesIcon className="releaseLatestTagIcon" /> Latest
                          </span>
                        )}
                      </div>
                      <h3 className="releaseCardTitle">{release.title}</h3>
                    </div>

                    {release.date && (
                      <div className="releaseCardDate">
                        <CalendarDaysIcon className="releaseDateIcon" />
                        <span>{release.date}</span>
                      </div>
                    )}
                  </div>

                  {release.summary && (
                    <p className="releaseSummaryText">{release.summary}</p>
                  )}

                  {/* Categories & Items */}
                  <div className="releaseCategoriesList">
                    {release.categories.map((cat) => (
                      <section key={cat.id} className="releaseCategorySection">
                        <div className="releaseCategoryHeader">
                          <span className="releaseCategoryIconBox">
                            {renderCategoryIcon(cat.iconType)}
                          </span>
                          <h4 className="releaseCategoryTitle">{cat.title}</h4>
                          <span className="releaseCategoryCount">{cat.items.length}</span>
                        </div>

                        <ul className="releaseItemsList">
                          {cat.items.map((item) => (
                            <li key={item.id} className="releaseItemRow">
                              <span className="releaseItemBullet">
                                <ChevronRightIcon className="releaseBulletIcon" />
                              </span>
                              <div className="releaseItemContent">
                                {item.tag && (
                                  <span className="releaseItemTag">{item.tag}</span>
                                )}
                                <span className="releaseItemText">{item.content}</span>
                              </div>
                            </li>
                          ))}
                        </ul>
                      </section>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <footer className="releaseNotesFooter">
          <div className="releaseNotesFooterMeta">
            <span className="releaseNotesVersionInfo">
              RO Toolbox • Showing {filteredReleases.length} release{filteredReleases.length === 1 ? "" : "s"} ({totalItemsCount} improvements)
            </span>
          </div>
          <button
            type="button"
            className="buttonPrimary releaseNotesAcknowledgeButton"
            onClick={onClose}
          >
            Got it!
          </button>
        </footer>
      </section>
    </div>
  );
}
