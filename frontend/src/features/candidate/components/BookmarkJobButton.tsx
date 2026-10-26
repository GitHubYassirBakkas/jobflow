import { Bookmark, BookmarkCheck, LoaderCircle } from "lucide-react";

interface BookmarkJobButtonProps {
  isPending: boolean;
  isSaved: boolean;
  jobTitle: string;
  onToggle: () => void;
}

export function BookmarkJobButton({
  isPending,
  isSaved,
  jobTitle,
  onToggle
}: BookmarkJobButtonProps) {
  const action = isSaved ? "Unsave" : "Save";
  const label = isPending
    ? isSaved
      ? "Removing"
      : "Saving"
    : isSaved
      ? "Saved"
      : "Save";

  return (
    <button
      aria-label={`${action} ${jobTitle}`}
      aria-pressed={isSaved}
      className={`secondary-button bookmark-button ${isSaved ? "bookmark-button-saved" : ""}`}
      disabled={isPending}
      onClick={onToggle}
      type="button"
    >
      {isPending ? (
        <LoaderCircle className="spin" size={16} />
      ) : isSaved ? (
        <BookmarkCheck size={16} />
      ) : (
        <Bookmark size={16} />
      )}
      {label}
    </button>
  );
}
