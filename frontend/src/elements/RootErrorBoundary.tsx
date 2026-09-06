import React from "react";

type RootErrorBoundaryProps = {
  children: React.ReactNode;
};

type RootErrorBoundaryState = {
  hasError: boolean;
};

export class RootErrorBoundary extends React.Component<RootErrorBoundaryProps, RootErrorBoundaryState> {
  state: RootErrorBoundaryState = {
    hasError: false
  };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error("Root render error:", error);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }
    return (
      <main className="layout">
        <div className="card" style={{ maxWidth: 560, margin: "2.5rem auto", textAlign: "center" }}>
          <h2>UI crashed</h2>
          <p>The app recovered into safe mode. Reload to continue.</p>
          <button type="button" className="buttonStrong" onClick={() => window.location.reload()}>
            Reload app
          </button>
        </div>
      </main>
    );
  }
}
