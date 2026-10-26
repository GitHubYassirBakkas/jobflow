import { AppShell, shellIcons } from "./AppShell";

export function CandidateLayout() {
  return (
    <AppShell
      title="Candidate workspace"
      subtitle="Track opportunities"
      profilePath="/candidate/profile"
      navItems={[
        { label: "Overview", to: "/candidate", icon: shellIcons.home },
        { label: "Jobs", to: "/candidate/jobs", icon: shellIcons.jobs },
        { label: "Saved Jobs", to: "/candidate/saved-jobs", icon: shellIcons.savedJobs },
        { label: "Applications", to: "/candidate/applications", icon: shellIcons.applications },
        { label: "Interviews", to: "/candidate/interviews", icon: shellIcons.interviews }
      ]}
    />
  );
}
