package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.AdminDashboardResponse;
import com.yassir.jobflow.entity.Role;
import com.yassir.jobflow.repository.ApplicationRepository;
import com.yassir.jobflow.repository.CompanyRepository;
import com.yassir.jobflow.repository.JobRepository;
import com.yassir.jobflow.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public AdminService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository) {

        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    public AdminDashboardResponse getDashboardStats() {

        long totalUsers = userRepository.count();
        long totalCandidates = userRepository.countByRole(Role.CANDIDATE);
        long totalRecruiters = userRepository.countByRole(Role.RECRUITER);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();

        return new AdminDashboardResponse(
                totalUsers,
                totalCandidates,
                totalRecruiters,
                totalAdmins,
                totalCompanies,
                totalJobs,
                totalApplications
        );
    }
}