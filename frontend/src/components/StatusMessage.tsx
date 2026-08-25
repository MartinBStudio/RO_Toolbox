type StatusMessageProps = {
  message: string;
};

export function StatusMessage({ message }: StatusMessageProps) {
  if (!message) {
    return null;
  }
  return (
    <section className="card status statusToast" role="status" aria-live="polite">
      {message}
    </section>
  );
}
