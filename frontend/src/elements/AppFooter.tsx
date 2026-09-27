type AppFooterProps = {
  loading: boolean;
  onOpenWhatsNew: () => void;
};

export function AppFooter({ loading, onOpenWhatsNew }: AppFooterProps) {
  return (
    <footer className="card appFooter">
      <p className="appFooterLine">
        Created by BStudio 2026 •{" "}
        <a href="https://martinbstudio.github.io/RO_Toolbox/" target="_blank" rel="noreferrer">
          RO Toolbox website
        </a>{" "}
        •{" "}
        <a
          href="https://www.roseonlinegame.com/"
          target="_blank"
          rel="noreferrer"
        >
          Official ROSE Online
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
