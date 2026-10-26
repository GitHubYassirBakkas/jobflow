import { AlertCircle, Inbox, LoaderCircle } from "lucide-react";

interface StateBlockProps {
  title: string;
  message: string;
  action?: React.ReactNode;
}

export function LoadingState({ message = "Loading..." }: { message?: string }) {
  return (
    <div className="state-block" role="status">
      <LoaderCircle className="spin" size={26} />
      <p>{message}</p>
    </div>
  );
}

export function EmptyState({ title, message, action }: StateBlockProps) {
  return (
    <div className="state-block">
      <Inbox size={28} />
      <h2>{title}</h2>
      <p>{message}</p>
      {action}
    </div>
  );
}

export function ErrorState({ title, message, action }: StateBlockProps) {
  return (
    <div className="state-block state-block-error" role="alert">
      <AlertCircle size={28} />
      <h2>{title}</h2>
      <p>{message}</p>
      {action}
    </div>
  );
}
