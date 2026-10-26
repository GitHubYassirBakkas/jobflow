import { AppShell, shellIcons } from "./AppShell";

export function RecruiterLayout() {
  return (
    <AppShell
      title="Recruiter workspace"
      subtitle="Manage hiring"
      navItems={[
        { label: "Overview", to: "/recruiter", icon: shellIcons.home },
        { label: "Jobs", to: "/recruiter/jobs", icon: shellIcons.jobs },
        { label: "Applications", to: "/recruiter/applications", icon: shellIcons.applications },
        { label: "Interviews", to: "/recruiter/interviews", icon: shellIcons.interviews }
      ]}
    />
  );
}
