import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { ApplicationProvider } from "./context/ApplicationContext.tsx";
import { RootErrorBoundary } from "./elements/RootErrorBoundary.tsx";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <RootErrorBoundary>
      <ApplicationProvider>
        <App />
      </ApplicationProvider>
    </RootErrorBoundary>
  </React.StrictMode>
);
