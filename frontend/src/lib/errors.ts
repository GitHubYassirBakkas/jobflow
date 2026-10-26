import { ApiError } from "./apiClient";

export function getFriendlyErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    const lowerMessage = error.message.toLowerCase();

    if (error.message.toLowerCase().includes("already applied")) {
      return "You have already applied to this job.";
    }

    if (lowerMessage.includes("interview already exists") || lowerMessage.includes("already exists")) {
      return "An interview already exists for this application.";
    }

    if (lowerMessage.includes("not allowed") || lowerMessage.includes("not owned")) {
      return "You are not allowed to manage this item.";
    }

    if (lowerMessage.includes("future")) {
      return "Interview date must be in the future.";
    }

    if (error.status === 404) {
      return "We could not find that information.";
    }

    if (error.status === 403) {
      return "You do not have access to this information.";
    }

    return error.message || "Something went wrong. Please try again.";
  }

  if (error instanceof Error && error.message) {
    if (error.message.toLowerCase().includes("not owned")) {
      return "This job is not owned by your company.";
    }

    return "Something went wrong. Please try again.";
  }

  return "Something went wrong. Please try again.";
}
