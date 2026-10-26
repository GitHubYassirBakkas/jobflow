import { createBrowserRouter, Navigate } from "react-router-dom";
import { App } from "../App";
import { AdminLayout } from "../layouts/AdminLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { CandidateLayout } from "../layouts/CandidateLayout";
import { RecruiterLayout } from "../layouts/RecruiterLayout";
import { AdminApplicationsPage } from "../pages/admin/AdminApplicationsPage";
import { AdminCompaniesPage } from "../pages/admin/AdminCompaniesPage";
import { AdminCandidateDetailsPage } from "../pages/admin/AdminCandidateDetailsPage";
import { AdminHomePage } from "../pages/admin/AdminHomePage";
import { AdminInterviewsPage } from "../pages/admin/AdminInterviewsPage";
import { AdminJobsPage } from "../pages/admin/AdminJobsPage";
import { AdminUsersPage } from "../pages/admin/AdminUsersPage";
import { LoginPage } from "../pages/auth/LoginPage";
import { RegisterPage } from "../pages/auth/RegisterPage";
import { CandidateApplicationsPage } from "../pages/candidate/CandidateApplicationsPage";
import { CandidateApplicationDetailsPage } from "../pages/candidate/CandidateApplicationDetailsPage";
import { CandidateHomePage } from "../pages/candidate/CandidateHomePage";
import { CandidateInterviewDetailsPage } from "../pages/candidate/CandidateInterviewDetailsPage";
import { CandidateInterviewsPage } from "../pages/candidate/CandidateInterviewsPage";
import { CandidateJobDetailsPage } from "../pages/candidate/CandidateJobDetailsPage";
import { CandidateJobsPage } from "../pages/candidate/CandidateJobsPage";
import { CandidateProfilePage } from "../pages/candidate/CandidateProfilePage";
import { CandidateSavedJobsPage } from "../pages/candidate/CandidateSavedJobsPage";
import { NotFoundPage } from "../pages/NotFoundPage";
import { RecruiterApplicationsPage } from "../pages/recruiter/RecruiterApplicationsPage";
import { RecruiterCreateJobPage } from "../pages/recruiter/RecruiterCreateJobPage";
import { RecruiterHomePage } from "../pages/recruiter/RecruiterHomePage";
import { RecruiterInterviewsPage } from "../pages/recruiter/RecruiterInterviewsPage";
import { RecruiterJobApplicationsPage } from "../pages/recruiter/RecruiterJobApplicationsPage";
import { RecruiterJobDetailsPage } from "../pages/recruiter/RecruiterJobDetailsPage";
import { RecruiterJobsPage } from "../pages/recruiter/RecruiterJobsPage";
import { RoleRedirectPage } from "../pages/RoleRedirectPage";
import { ProtectedRoute } from "./ProtectedRoute";

export const router = createBrowserRouter([
  {
    element: <App />,
    children: [
      {
        path: "/",
        element: <RoleRedirectPage />
      },
      {
        element: <AuthLayout />,
        children: [
          {
            path: "/login",
            element: <LoginPage />
          },
          {
            path: "/register",
            element: <RegisterPage />
          }
        ]
      },
      {
        element: <ProtectedRoute allowedRoles={["CANDIDATE"]} />,
        children: [
          {
            path: "/candidate",
            element: <CandidateLayout />,
            children: [
              {
                index: true,
                element: <CandidateHomePage />
              },
              {
                path: "jobs",
                element: <CandidateJobsPage />
              },
              {
                path: "jobs/:jobId",
                element: <CandidateJobDetailsPage />
              },
              {
                path: "saved-jobs",
                element: <CandidateSavedJobsPage />
              },
              {
                path: "applications",
                element: <CandidateApplicationsPage />
              },
              {
                path: "applications/:applicationId",
                element: <CandidateApplicationDetailsPage />
              },
              {
                path: "interviews",
                element: <CandidateInterviewsPage />
              },
              {
                path: "interviews/:applicationId",
                element: <CandidateInterviewDetailsPage />
              },
              {
                path: "profile",
                element: <CandidateProfilePage />
              }
            ]
          }
        ]
      },
      {
        element: <ProtectedRoute allowedRoles={["RECRUITER"]} />,
        children: [
          {
            path: "/recruiter",
            element: <RecruiterLayout />,
            children: [
              {
                index: true,
                element: <RecruiterHomePage />
              },
              {
                path: "jobs",
                element: <RecruiterJobsPage />
              },
              {
                path: "jobs/new",
                element: <RecruiterCreateJobPage />
              },
              {
                path: "jobs/:jobId",
                element: <RecruiterJobDetailsPage />
              },
              {
                path: "jobs/:jobId/applications",
                element: <RecruiterJobApplicationsPage />
              },
              {
                path: "applications",
                element: <RecruiterApplicationsPage />
              },
              {
                path: "interviews",
                element: <RecruiterInterviewsPage />
              }
            ]
          }
        ]
      },
      {
        element: <ProtectedRoute allowedRoles={["ADMIN"]} />,
        children: [
          {
            path: "/admin",
            element: <AdminLayout />,
            children: [
              {
                index: true,
                element: <AdminHomePage />
              },
              {
                path: "users",
                element: <AdminUsersPage />
              },
              {
                path: "candidates/:userId",
                element: <AdminCandidateDetailsPage />
              },
              {
                path: "companies",
                element: <AdminCompaniesPage />
              },
              {
                path: "jobs",
                element: <AdminJobsPage />
              },
              {
                path: "applications",
                element: <AdminApplicationsPage />
              },
              {
                path: "interviews",
                element: <AdminInterviewsPage />
              }
            ]
          }
        ]
      },
      {
        path: "/dashboard",
        element: <Navigate to="/" replace />
      },
      {
        path: "*",
        element: <NotFoundPage />
      }
    ]
  }
]);
