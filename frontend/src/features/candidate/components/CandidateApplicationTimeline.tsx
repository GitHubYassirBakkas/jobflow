import { ArrowRight, CircleDot, LoaderCircle, RotateCcw } from "lucide-react";
import { useEffect, useState } from "react";
import { ApplicationStatusBadge } from "../../../components/StatusBadge";
import { getMyApplicationTimeline } from "../candidateApi";
import { getFriendlyErrorMessage } from "../../../lib/errors";
import { formatEnumLabel } from "../../../lib/formatters";
import type { CandidateApplicationTimelineEvent } from "../../../types/candidate";

interface CandidateApplicationTimelineProps {
  applicationId: string | number;
  reloadKey?: number;
}

interface TimelineEventViewModel {
  statusLabel: string;
  changedAtLabel: string;
  changedAtDateTime: string | undefined;
  eventTypeLabel: string;
  description: string;
}

export function formatTimelineChangedAt(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Date unavailable";
  }

  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(date);
}

export function getTimelineDateTimeAttribute(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return undefined;
  }

  return value;
}

export function getTimelineEventDescription(
  event: CandidateApplicationTimelineEvent
) {
  const statusLabel = formatEnumLabel(event.status);

  if (event.eventType === "BASELINE") {
    return "Recorded status snapshot";
  }

  return `Status changed to ${statusLabel}`;
}

export function toTimelineEventViewModels(
  events: CandidateApplicationTimelineEvent[]
): TimelineEventViewModel[] {
  return events.map((event) => ({
    statusLabel: formatEnumLabel(event.status),
    changedAtLabel: formatTimelineChangedAt(event.changedAt),
    changedAtDateTime: getTimelineDateTimeAttribute(event.changedAt),
    eventTypeLabel: formatEnumLabel(event.eventType),
    description: getTimelineEventDescription(event)
  }));
}

export function CandidateApplicationTimeline({
  applicationId,
  reloadKey = 0
}: CandidateApplicationTimelineProps) {
  const [events, setEvents] = useState<CandidateApplicationTimelineEvent[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [localReloadKey, setLocalReloadKey] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function loadTimeline() {
      setIsLoading(true);
      setError("");

      try {
        const response = await getMyApplicationTimeline(applicationId);

        if (isMounted) {
          setEvents(response);
        }
      } catch (caughtError) {
        if (isMounted) {
          setError(getFriendlyErrorMessage(caughtError));
          setEvents([]);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadTimeline();

    return () => {
      isMounted = false;
    };
  }, [applicationId, localReloadKey, reloadKey]);

  const timelineItems = toTimelineEventViewModels(events);

  return (
    <section
      aria-labelledby="application-timeline-title"
      className="detail-section application-timeline-section"
    >
      <div className="section-title-row">
        <div>
          <p className="eyebrow">Status history</p>
          <h3 id="application-timeline-title">Application timeline</h3>
        </div>
      </div>

      {isLoading ? (
        <div className="timeline-state" role="status">
          <LoaderCircle className="spin" size={18} />
          <span>Loading timeline...</span>
        </div>
      ) : null}

      {!isLoading && error ? (
        <div className="timeline-state timeline-state-error" role="alert">
          <span>{error}</span>
          <button
            className="secondary-button"
            onClick={() => setLocalReloadKey((currentKey) => currentKey + 1)}
            type="button"
          >
            <RotateCcw size={16} />
            Retry
          </button>
        </div>
      ) : null}

      {!isLoading && !error && timelineItems.length === 0 ? (
        <div className="timeline-state">
          <span>No status history has been recorded for this application yet.</span>
        </div>
      ) : null}

      {!isLoading && !error && timelineItems.length > 0 ? (
        <ol className="application-timeline" aria-label="Application status timeline">
          {events.map((event, index) => {
            const item = timelineItems[index];
            const Icon = event.eventType === "BASELINE" ? CircleDot : ArrowRight;

            return (
              <li
                className={`timeline-event timeline-event-${event.eventType.toLowerCase()}`}
                key={`${event.eventType}-${event.status}-${event.changedAt}-${index}`}
              >
                <span className="timeline-marker" aria-hidden="true">
                  <Icon size={16} />
                </span>
                <div className="timeline-event-body">
                  <div className="timeline-event-heading">
                    <ApplicationStatusBadge status={event.status} />
                    <span className="timeline-event-type">{item.eventTypeLabel}</span>
                  </div>
                  <p>{item.description}</p>
                  <time dateTime={item.changedAtDateTime}>
                    {item.changedAtLabel}
                  </time>
                </div>
              </li>
            );
          })}
        </ol>
      ) : null}
    </section>
  );
}
