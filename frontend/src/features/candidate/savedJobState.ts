export function createSavedJobIdSet(jobIds: number[]) {
  return new Set(jobIds);
}

export function isJobSaved(savedJobIds: ReadonlySet<number>, jobId: number) {
  return savedJobIds.has(jobId);
}

export function updateSavedJobIdSet(
  savedJobIds: ReadonlySet<number>,
  jobId: number,
  saved: boolean
) {
  const nextSavedJobIds = new Set(savedJobIds);

  if (saved) {
    nextSavedJobIds.add(jobId);
  } else {
    nextSavedJobIds.delete(jobId);
  }

  return nextSavedJobIds;
}
