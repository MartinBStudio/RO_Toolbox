import { useState } from "react";

type HowToUseModalProps = {
  open: boolean;
  onClose: () => void;
};

const GUIDE_TABS = [
  { id: "texture-replacer", label: "Texture replacer" },
  { id: "config-editor", label: "Config editor" }
] as const;

export function HowToUseModal({ open, onClose }: HowToUseModalProps) {
  const [selectedTab, setSelectedTab] = useState<(typeof GUIDE_TABS)[number]["id"]>("texture-replacer");

  if (!open) {
    return null;
  }

  return (
    <div className="modalBackdrop" onClick={onClose}>
      <section className="card modalCard howToUseModalCard" onClick={(event) => event.stopPropagation()}>
        <div className="modalHeader">
          <h2>How to use app</h2>
          <button type="button" className="buttonSubtle" onClick={onClose}>✕</button>
        </div>

        <div className="howToUseBody">
          <div className="howToUseTabs">
            {GUIDE_TABS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                className={`howToUseTab${selectedTab === tab.id ? " howToUseTabActive" : ""}`}
                onClick={() => setSelectedTab(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {selectedTab === "texture-replacer" ? (
            <>
              <p className="howToUseIntro">
                Use this service to manage Loot, Combat Text, and UI replacement profiles.
              </p>
              <ol className="howToUseSteps">
                <li>Open <strong>Settings</strong> and choose your ROSE Online game folder.</li>
                <li>In each section, click <strong>⟳</strong> to check or <strong>↓</strong> to download profile updates.</li>
                <li>Select a profile from the dropdown.</li>
                <li>Use <strong>Install</strong> to apply the profile to your game files.</li>
                <li>Use <strong>📁</strong> to open installed files or <strong>🗑</strong> to clear them.</li>
                <li>Use <strong>🔗</strong> to visit the profile author page when available.</li>
              </ol>
              <p className="howToUseTip">
                Tip: After installing a new profile, always restart your game client.
              </p>
            </>
          ) : (
            <>
              <p className="howToUseIntro">
                Use this service to view and edit ROSE Online config TOML files in <code>%APPDATA%\Rednim Games\ROSE Online\config</code>.
              </p>
              <ol className="howToUseSteps">
                <li>Open the <strong>Config editor</strong> service tab from the sidebar.</li>
                <li>Select <strong>ignore.toml</strong> or <strong>rose.toml</strong> from the file tabs.</li>
                <li>Edit TOML in the source panel and use <strong>Save</strong> to write changes.</li>
                <li>Use <strong>⟳</strong> to reload from disk and <strong>📂</strong> to open the config folder.</li>
                <li>Check the parsed preview panel to verify values are structured as expected.</li>
              </ol>
              <p className="howToUseTip">
                Tip: If TOML is invalid, save is blocked and the error tells you the line/column to fix.
              </p>
            </>
          )}
        </div>
      </section>
    </div>
  );
}
