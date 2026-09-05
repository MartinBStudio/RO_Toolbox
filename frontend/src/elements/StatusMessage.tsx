import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  XMarkIcon
} from "@heroicons/react/24/solid";

type StatusMessageProps = {
  message: string;
  onDismiss: () => void;
};

type StatusTone = "success" | "error" | "info";

function getStatusTone(message: string): StatusTone {
  const normalized = message.trim().toLowerCase();

  if (
    normalized.includes("failed") ||
    normalized.includes("error") ||
    normalized.includes("unable") ||
    normalized.includes("cannot") ||
    normalized.includes("invalid") ||
    normalized.includes("missing")
  ) {
    return "error";
  }

  if (
    normalized.includes("launched") ||
    normalized.includes("saved") ||
    normalized.includes("installed") ||
    normalized.includes("downloaded") ||
    normalized.includes("enabled") ||
    normalized.includes("disabled") ||
    normalized.includes("updated") ||
    normalized.includes("imported") ||
    normalized.includes("exported") ||
    normalized.includes("opened") ||
    normalized.includes("cleared")
  ) {
    return "success";
  }

  return "info";
}

function getStatusMeta(message: string) {
  const tone = getStatusTone(message);

  if (tone === "error") {
    return {
      tone,
      Icon: ExclamationTriangleIcon
    };
  }

  if (tone === "success") {
    return {
      tone,
      Icon: CheckCircleIcon
    };
  }

  return {
    tone,
    Icon: InformationCircleIcon
  };
}

export function StatusMessage({ message, onDismiss }: StatusMessageProps) {
  if (!message) {
    return null;
  }

  const { tone, Icon } = getStatusMeta(message);

  return (
    <section
      className={`statusToast statusToast${tone[0].toUpperCase()}${tone.slice(1)}`}
      role={tone === "error" ? "alert" : "status"}
      aria-live={tone === "error" ? "assertive" : "polite"}
    >
      <div className="statusToastIcon" aria-hidden="true">
        <Icon />
      </div>
      <div className="statusToastBody">
        <p className="statusToastText">{message}</p>
      </div>
      <button
        type="button"
        className="statusToastClose"
        onClick={onDismiss}
        aria-label="Dismiss notification"
      >
        <XMarkIcon />
      </button>
      <span className="statusToastProgress" aria-hidden="true" />
    </section>
  );
}
