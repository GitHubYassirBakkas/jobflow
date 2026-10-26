import { AppShell, shellIcons } from "./AppShell";

export function AdminLayout() {
  return (
    <AppShell
      title="Admin workspace"
      subtitle="Platform operations"
      navItems={[
        { label: "Overview", to: "/admin", icon: shellIcons.home },
        { label: "Users", to: "/admin/users", icon: shellIcons.users },
        { label: "Companies", to: "/admin/companies", icon: shellIcons.companies },
        { label: "Jobs", to: "/admin/jobs", icon: shellIcons.jobs },
        { label: "Applications", to: "/admin/applications", icon: shellIcons.applications },
        { label: "Interviews", to: "/admin/interviews", icon: shellIcons.interviews }
      ]}
    />
  );
}
