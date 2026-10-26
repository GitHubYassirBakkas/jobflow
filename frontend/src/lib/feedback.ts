export type FeedbackTone = "success" | "error";

export function getFeedbackRole(type: FeedbackTone) {
  return type === "success" ? "status" : "alert";
}
