import { useState } from "react";

type HowToUseModalProps = {
  open: boolean;
  onClose: () => void;
};

const GUIDE_SECTIONS = [
  { id: "quick-launch", label: "Quick Launch" },
  { id: "login-manager", label: "Login Manager" },
  { id: "texture-replacer", label: "Texture replacer" },
  { id: "config-editor", label: "Config editor" }
] as const;

export function HowToUseModal({ open, onClose }: HowToUseModalProps) {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(["quick-launch"])
  );

  if (!open) {
    return null;
  }

  const toggleSection = (id: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedSections(newExpanded);
  };

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
          {GUIDE_SECTIONS.map((section) => {
            const isExpanded = expandedSections.has(section.id);
            return (
              <div key={section.id} className="accordionItem">
                <button
                  type="button"
                  className="accordionHeader"
                  onClick={() => toggleSection(section.id)}
                >
                  <span className="accordionLabel">{section.label}</span>
                  <span className="accordionIcon">{isExpanded ? "▼" : "▶"}</span>
                </button>
                {isExpanded && (
                  <div className="accordionContent">
                    {section.id === "quick-launch" && (
                      <>
                        <p className="howToUseIntro">
                          Save game accounts and launch ROSE Online instantly with stored credentials.
                        </p>
                        <ol className="howToUseSteps">
                          <li>Open the <strong>Login Manager</strong> service to add and manage accounts.</li>
                          <li>In Login Manager, click <strong>+</strong> to create a new account with name, email, password, and icon.</li>
                          <li>Toggle <strong>Display in quick launch</strong> to show the account in the Quick Launch section.</li>
                          <li>In the <strong>Quick Launch</strong> section, click any account icon to launch ROSE with those credentials.</li>
                          <li>Use <strong>Rose Launcher</strong> to start the updater instead of the game client.</li>
                        </ol>
                        <p className="howToUseTip">
                          Tip: Passwords are encrypted locally and never stored in plaintext.
                        </p>
                      </>
                    )}
                    {section.id === "login-manager" && (
                      <>
                        <p className="howToUseIntro">
                          Store and manage multiple ROSE Online game accounts with encryption.
                        </p>
                        <ol className="howToUseSteps">
                          <li>Open the <strong>Login Manager</strong> service tab from the sidebar.</li>
                          <li>Click <strong>+</strong> to add a new account with name, email, password, and custom icon.</li>
                          <li>Click the <strong>⭐</strong> toggle to show/hide the account in Quick Launch.</li>
                          <li>Click <strong>📝</strong> to edit account details (name, email, password, icon).</li>
                          <li>Click <strong>🗑</strong> to delete an account permanently.</li>
                          <li>Use <strong>Export</strong> to download a backup file (passwords are encrypted).</li>
                          <li>Use <strong>Import</strong> to restore accounts from a previously exported backup.</li>
                        </ol>
                        <p className="howToUseTip">
                          Tip: Factory reset will delete all saved accounts. Export first if you need to keep them!
                        </p>
                      </>
                    )}
                    {section.id === "texture-replacer" && (
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
                    )}
                    {section.id === "config-editor" && (
                      <>
                        <p className="howToUseIntro">
                          Use this service to view and edit ROSE Online config TOML files in <code>%APPDATA%\Rednim Games\ROSE Online\config</code>.
                        </p>
                        <ol className="howToUseSteps">
                          <li>Open the <strong>Config editor</strong> service tab from the sidebar.</li>
                          <li>Select <strong>ignore.toml</strong> or <strong>rose.toml</strong> from the file tabs.</li>
                          <li>For <strong>ignore.toml</strong>, use the ignore list manager to <strong>Add</strong> or <strong>Delete</strong> entries quickly.</li>
                          <li>For <strong>rose.toml</strong>, use the boolean values manager to switch any <strong>true/false</strong> setting.</li>
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
                )}
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
