type AppFooterProps = {
  loading: boolean;
  onOpenWhatsNew: () => void;
};

export function AppFooter({ loading, onOpenWhatsNew }: AppFooterProps) {
  return (
    <footer className="card appFooter">
      <p className="appFooterLine">
        Created by BStudio 2026 •{" "}
        <a href="https://github.com/MartinBStudio/RO_Toolbox/releases" target="_blank" rel="noreferrer">
          GitHub
        </a>{" "}
        •{" "}
        <a
          href="https://forum.roseonlinegame.com/topic/7761-ro_toolbox-loot-models/#comment-26680"
          target="_blank"
          rel="noreferrer"
        >
          Forum thread
        </a>{" "}
        •{" "}
        <button
          type="button"
          className="footerLinkButton"
          disabled={loading}
          onClick={onOpenWhatsNew}
        >
          What's new
        </button>
      </p>
    </footer>
  );
}
