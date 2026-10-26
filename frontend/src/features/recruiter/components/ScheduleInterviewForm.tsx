import { FormEvent, useMemo, useState } from "react";
import { CalendarPlus, LoaderCircle } from "lucide-react";
import { scheduleRecruiterInterview } from "../recruiterApi";
import { getFriendlyErrorMessage } from "../../../lib/errors";
import { formatEnumLabel } from "../../../lib/formatters";
import type { InterviewType } from "../../../types/candidate";
import type { RecruiterInterview, ScheduleInterviewRequest } from "../../../types/recruiter";

interface ScheduleInterviewFormProps {
  applicationId: number;
  onCancel: () => void;
  onScheduled: (interview: RecruiterInterview) => void;
}

const interviewTypes: InterviewType[] = ["ONLINE", "ONSITE", "PHONE"];

function toNullable(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function getMinDateTime() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset() + 5);
  return now.toISOString().slice(0, 16);
}

export function ScheduleInterviewForm({ applicationId, onCancel, onScheduled }: ScheduleInterviewFormProps) {
  const [scheduledAt, setScheduledAt] = useState("");
  const [type, setType] = useState<InterviewType>("ONLINE");
  const [meetingLink, setMeetingLink] = useState("");
  const [location, setLocation] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const minDateTime = useMemo(() => getMinDateTime(), []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!scheduledAt) {
      setError("Choose a future date and time for the interview.");
      return;
    }

    if (new Date(scheduledAt).getTime() <= Date.now()) {
      setError("Interview date must be in the future.");
      return;
    }

    setIsSubmitting(true);

    const payload: ScheduleInterviewRequest = {
      scheduledAt,
      type,
      meetingLink: type === "ONLINE" ? toNullable(meetingLink) : null,
      location: type === "ONSITE" ? toNullable(location) : null,
      notes: toNullable(notes)
    };

    try {
      const interview = await scheduleRecruiterInterview(applicationId, payload);
      onScheduled(interview);
    } catch (caughtError) {
      setError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <div className="form-grid">
        <label>
          <span>Date and time</span>
          <input
            min={minDateTime}
            onChange={(event) => setScheduledAt(event.target.value)}
            required
            type="datetime-local"
            value={scheduledAt}
          />
        </label>
        <label>
          <span>Type</span>
          <select onChange={(event) => setType(event.target.value as InterviewType)} value={type}>
            {interviewTypes.map((interviewType) => (
              <option key={interviewType} value={interviewType}>
                {formatEnumLabel(interviewType)}
              </option>
            ))}
          </select>
        </label>
      </div>
      {type === "ONLINE" ? (
        <label>
          <span>Meeting link</span>
          <input
            onChange={(event) => setMeetingLink(event.target.value)}
            placeholder="https://..."
            type="url"
            value={meetingLink}
          />
        </label>
      ) : null}
      {type === "ONSITE" ? (
        <label>
          <span>Location</span>
          <input
            onChange={(event) => setLocation(event.target.value)}
            placeholder="Office address or room"
            type="text"
            value={location}
          />
        </label>
      ) : null}
      <label>
        <span>Notes</span>
        <textarea
          maxLength={2000}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Preparation notes, agenda, or contact details"
          value={notes}
        />
      </label>
      {error ? <p className="form-error">{error}</p> : null}
      <div className="form-actions">
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? <LoaderCircle className="spin" size={18} /> : <CalendarPlus size={18} />}
          <span>{isSubmitting ? "Scheduling" : "Schedule interview"}</span>
        </button>
        <button className="secondary-button" disabled={isSubmitting} onClick={onCancel} type="button">
          Cancel
        </button>
      </div>
    </form>
  );
}
