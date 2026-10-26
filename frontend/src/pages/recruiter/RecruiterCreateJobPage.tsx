import { FormEvent, useState } from "react";
import { ArrowLeft, LoaderCircle, PlusCircle } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { createRecruiterJob } from "../../features/recruiter/recruiterApi";
import { getFriendlyErrorMessage } from "../../lib/errors";
import { formatEnumLabel } from "../../lib/formatters";
import type { EmploymentType } from "../../types/candidate";
import type { CreateJobRequest } from "../../types/recruiter";

const employmentTypes: EmploymentType[] = ["FULL_TIME", "PART_TIME", "INTERNSHIP", "CONTRACT", "FREELANCE"];

export function RecruiterCreateJobPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateJobRequest>({
    title: "",
    description: "",
    location: "",
    employmentType: "FULL_TIME"
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(field: keyof CreateJobRequest, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!form.title.trim() || !form.description.trim() || !form.location.trim()) {
      setError("Title, description, and location are required.");
      return;
    }

    setIsSubmitting(true);

    try {
      const job = await createRecruiterJob({
        title: form.title.trim(),
        description: form.description.trim(),
        location: form.location.trim(),
        employmentType: form.employmentType
      });
      navigate(`/recruiter/jobs/${job.id}`, {
        replace: true,
        state: { success: "Job created successfully." }
      });
    } catch (caughtError) {
      setError(getFriendlyErrorMessage(caughtError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <article className="detail-panel">
      <Link className="back-link" to="/recruiter/jobs">
        <ArrowLeft size={16} />
        Back to jobs
      </Link>
      <div className="detail-header">
        <div>
          <p className="eyebrow">Create job</p>
          <h2>Post a new role</h2>
        </div>
      </div>
      <form className="form-card wide-form" onSubmit={handleSubmit}>
        <label>
          <span>Title</span>
          <input
            onChange={(event) => updateField("title", event.target.value)}
            placeholder="Java Backend Developer"
            required
            type="text"
            value={form.title}
          />
        </label>
        <label>
          <span>Description</span>
          <textarea
            onChange={(event) => updateField("description", event.target.value)}
            placeholder="Role responsibilities, requirements, and team context"
            required
            value={form.description}
          />
        </label>
        <div className="form-grid">
          <label>
            <span>Location</span>
            <input
              onChange={(event) => updateField("location", event.target.value)}
              placeholder="Casablanca, Morocco"
              required
              type="text"
              value={form.location}
            />
          </label>
          <label>
            <span>Employment type</span>
            <select
              onChange={(event) => updateField("employmentType", event.target.value as EmploymentType)}
              value={form.employmentType}
            >
              {employmentTypes.map((employmentType) => (
                <option key={employmentType} value={employmentType}>
                  {formatEnumLabel(employmentType)}
                </option>
              ))}
            </select>
          </label>
        </div>
        {error ? <p className="form-error">{error}</p> : null}
        <button className="primary-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? <LoaderCircle className="spin" size={18} /> : <PlusCircle size={18} />}
          <span>{isSubmitting ? "Creating" : "Create job"}</span>
        </button>
      </form>
    </article>
  );
}
