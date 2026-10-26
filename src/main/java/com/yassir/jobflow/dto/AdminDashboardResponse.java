package com.yassir.jobflow.dto;

public class AdminDashboardResponse {

    private long totalUsers;
    private long totalCandidates;
    private long totalRecruiters;
    private long totalAdmins;
    private long totalCompanies;
    private long totalJobs;
    private long totalApplications;

    public AdminDashboardResponse(
            long totalUsers,
            long totalCandidates,
            long totalRecruiters,
            long totalAdmins,
            long totalCompanies,
            long totalJobs,
            long totalApplications) {

        this.totalUsers = totalUsers;
        this.totalCandidates = totalCandidates;
        this.totalRecruiters = totalRecruiters;
        this.totalAdmins = totalAdmins;
        this.totalCompanies = totalCompanies;
        this.totalJobs = totalJobs;
        this.totalApplications = totalApplications;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalCandidates() {
        return totalCandidates;
    }

    public long getTotalRecruiters() {
        return totalRecruiters;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public long getTotalApplications() {
        return totalApplications;
    }
}