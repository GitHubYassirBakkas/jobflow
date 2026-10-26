package com.yassir.jobflow.dto;

import java.util.List;

public class CandidateDashboardResponse {

    private long totalApplications;
    private long activeApplications;
    private long withdrawnApplications;
    private long upcomingInterviews;
    private long savedJobs;
    private List<ApplicationResponse> recentApplications;
    private List<InterviewResponse> nextInterviews;

    public CandidateDashboardResponse(
            long totalApplications,
            long activeApplications,
            long withdrawnApplications,
            long upcomingInterviews,
            long savedJobs,
            List<ApplicationResponse> recentApplications,
            List<InterviewResponse> nextInterviews) {

        this.totalApplications = totalApplications;
        this.activeApplications = activeApplications;
        this.withdrawnApplications = withdrawnApplications;
        this.upcomingInterviews = upcomingInterviews;
        this.savedJobs = savedJobs;
        this.recentApplications = recentApplications;
        this.nextInterviews = nextInterviews;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public long getActiveApplications() {
        return activeApplications;
    }

    public long getWithdrawnApplications() {
        return withdrawnApplications;
    }

    public long getUpcomingInterviews() {
        return upcomingInterviews;
    }

    public long getSavedJobs() {
        return savedJobs;
    }

    public List<ApplicationResponse> getRecentApplications() {
        return recentApplications;
    }

    public List<InterviewResponse> getNextInterviews() {
        return nextInterviews;
    }
}
