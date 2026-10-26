import { describe, expect, it } from "vitest";
import type { CandidateApplicationTimelineEvent } from "../../../types/candidate";
import {
  formatTimelineChangedAt,
  getTimelineDateTimeAttribute,
  getTimelineEventDescription,
  toTimelineEventViewModels
} from "./CandidateApplicationTimeline";

function makeTimelineEvent(
  status: CandidateApplicationTimelineEvent["status"],
  eventType: CandidateApplicationTimelineEvent["eventType"],
  changedAt: string
): CandidateApplicationTimelineEvent {
  return {
    status,
    eventType,
    changedAt
  };
}

describe("CandidateApplicationTimeline helpers", () => {
  it("describes baseline events as recorded snapshots", () => {
    const event = makeTimelineEvent(
      "APPLIED",
      "BASELINE",
      "2026-10-10T09:00:00"
    );

    expect(getTimelineEventDescription(event))
      .toBe("Recorded status snapshot");
  });

  it("describes transition events as status changes", () => {
    const event = makeTimelineEvent(
      "REVIEWING",
      "TRANSITION",
      "2026-10-10T10:00:00"
    );

    expect(getTimelineEventDescription(event))
      .toBe("Status changed to Reviewing");
  });

  it("preserves backend chronological order when mapping events", () => {
    const events = [
      makeTimelineEvent("APPLIED", "BASELINE", "2026-10-10T09:00:00"),
      makeTimelineEvent("REVIEWING", "TRANSITION", "2026-10-10T10:00:00"),
      makeTimelineEvent("INTERVIEW", "TRANSITION", "2026-10-10T11:00:00")
    ];

    expect(toTimelineEventViewModels(events).map((event) => event.statusLabel))
      .toEqual(["Applied", "Reviewing", "Interview"]);
  });

  it("maps event type labels without changing semantics", () => {
    const events = [
      makeTimelineEvent("APPLIED", "BASELINE", "2026-10-10T09:00:00"),
      makeTimelineEvent("ACCEPTED", "TRANSITION", "2026-10-10T10:00:00")
    ];

    expect(toTimelineEventViewModels(events).map((event) => event.eventTypeLabel))
      .toEqual(["Baseline", "Transition"]);
  });

  it("returns no mapped events for an empty history", () => {
    expect(toTimelineEventViewModels([])).toEqual([]);
  });

  it("handles invalid dates gracefully", () => {
    expect(formatTimelineChangedAt("not-a-date"))
      .toBe("Date unavailable");
    expect(getTimelineDateTimeAttribute("not-a-date"))
      .toBeUndefined();
  });

  it("formats offset-bearing UTC timestamps in the browser timezone", () => {
    const value = "2026-10-13T20:11:21.034614Z";
    const expected = new Intl.DateTimeFormat(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit"
    }).format(new Date(value));

    expect(formatTimelineChangedAt(value)).toBe(expected);
    expect(getTimelineDateTimeAttribute(value)).toBe(value);
  });

  it("keeps legacy no-offset timestamps readable", () => {
    const value = "2026-10-10T09:00:00";

    expect(formatTimelineChangedAt(value)).not.toBe("Date unavailable");
    expect(getTimelineDateTimeAttribute(value)).toBe(value);
  });
});
