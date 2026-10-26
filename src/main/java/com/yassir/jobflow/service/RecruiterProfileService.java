package com.yassir.jobflow.service;

import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.entity.RecruiterProfile;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.repository.RecruiterProfileRepository;
import org.springframework.stereotype.Service;

import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.exception.ForbiddenException;
@Service
public class RecruiterProfileService {

    private final RecruiterProfileRepository recruiterProfileRepository;

    public RecruiterProfileService(
            RecruiterProfileRepository recruiterProfileRepository) {
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    public RecruiterProfile createProfile(
        User user,
        Company company,
        String jobTitle) {

    if (company.getCreatedBy() == null
            || user.getId() == null
            || !user.getId().equals(company.getCreatedBy().getId())) {

        throw new ForbiddenException(
                "You are not allowed to join this company"
        );
    }

    RecruiterProfile profile = new RecruiterProfile();

    profile.setUser(user);
    profile.setCompany(company);
    profile.setJobTitle(jobTitle);

    return recruiterProfileRepository.save(profile);
}

    public RecruiterProfile getProfileByUserId(Long userId) {

    RecruiterProfile profile =
            recruiterProfileRepository.findByUserId(userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Recruiter profile not found"
                            ));

    Company company = profile.getCompany();

    Long creatorId = company.getCreatedBy() == null
            ? null
            : company.getCreatedBy().getId();

    if (userId == null
            || creatorId == null
            || !userId.equals(creatorId)
            || profile.getUser().getId() == null
            || !profile.getUser().getId().equals(creatorId)) {

        throw new ForbiddenException(
                "Recruiter company membership is not verified"
        );
    }

    return profile;
}

    

}