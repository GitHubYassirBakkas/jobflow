import {
  BriefcaseBusiness,
  Building2,
  BookmarkCheck,
  CalendarDays,
  ClipboardList,
  ChevronDown,
  Home,
  LogOut,
  UserRound,
  UsersRound
} from "lucide-react";
import { useEffect, useId, useRef, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { focusFirstElement } from "../lib/focusManagement";
import type { AuthUser } from "../types/auth";

interface NavItem {
  label: string;
  to?: string;
  icon: React.ComponentType<{ size?: number }>;
  disabled?: boolean;
}

interface AppShellProps {
  title: string;
  subtitle: string;
  navItems: NavItem[];
  profilePath?: string;
}

const iconMap = {
  home: Home,
  jobs: BriefcaseBusiness,
  savedJobs: BookmarkCheck,
  applications: ClipboardList,
  interviews: CalendarDays,
  profile: UserRound,
  users: UsersRound,
  companies: Building2
};

export const shellIcons = iconMap;

export function getUserDisplayName(user: AuthUser | null) {
  return `${user?.firstName ?? ""} ${user?.lastName ?? ""}`.trim() || "User";
}

export function AppShell({ title, subtitle, navItems, profilePath }: AppShellProps) {
  const { user, logout } = useAuth();
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const accountMenuId = useId();
  const accountMenuRef = useRef<HTMLDivElement | null>(null);
  const accountMenuButtonRef = useRef<HTMLButtonElement | null>(null);
  const accountDropdownRef = useRef<HTMLDivElement | null>(null);
  const displayName = getUserDisplayName(user);
  const userInitial = displayName.charAt(0).toUpperCase();

  function closeAccountMenu(shouldRestoreFocus = false) {
    setIsAccountMenuOpen(false);

    if (shouldRestoreFocus) {
      window.requestAnimationFrame(() => {
        accountMenuButtonRef.current?.focus();
      });
    }
  }

  useEffect(() => {
    if (!isAccountMenuOpen) {
      return;
    }

    focusFirstElement(accountDropdownRef.current);

    function handlePointerDown(event: PointerEvent) {
      if (
        accountMenuRef.current
          && !accountMenuRef.current.contains(event.target as Node)
      ) {
        closeAccountMenu();
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        closeAccountMenu(true);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isAccountMenuOpen]);

  function handleLogout() {
    closeAccountMenu();
    logout();
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-mark">JF</span>
          <div>
            <strong>JobFlow</strong>
            <span>{user?.role === "ADMIN" ? "Admin" : user?.role === "RECRUITER" ? "Recruiter" : "Candidate"}</span>
          </div>
        </div>
        <nav className="sidebar-nav" aria-label="Primary navigation">
          {navItems.map((item) => {
            const Icon = item.icon;

            if (item.disabled || !item.to) {
              return (
                <span aria-disabled="true" className="nav-disabled" key={item.label}>
                  <Icon size={18} />
                  <span>{item.label}</span>
                </span>
              );
            }

            return (
              <NavLink key={item.to} to={item.to} end={["/candidate", "/recruiter", "/admin"].includes(item.to)}>
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{subtitle}</p>
            <h1>{title}</h1>
          </div>
          <div className="account-menu" ref={accountMenuRef}>
            <button
              aria-controls={isAccountMenuOpen ? accountMenuId : undefined}
              aria-expanded={isAccountMenuOpen}
              aria-label={`Account menu for ${displayName}`}
              className="user-chip"
              onClick={() => setIsAccountMenuOpen((current) => !current)}
              ref={accountMenuButtonRef}
              type="button"
            >
              <span aria-hidden="true">{userInitial}</span>
              <div>
                <strong>{displayName}</strong>
                <small>{user?.email}</small>
              </div>
              <ChevronDown
                aria-hidden="true"
                className={isAccountMenuOpen ? "account-menu-chevron account-menu-chevron-open" : "account-menu-chevron"}
                size={16}
              />
            </button>

            {isAccountMenuOpen ? (
              <div
                aria-label="Account"
                className="account-dropdown"
                id={accountMenuId}
                ref={accountDropdownRef}
              >
                <div className="account-dropdown-header">
                  <strong>{displayName}</strong>
                  <small>{user?.email}</small>
                </div>
                {profilePath ? (
                  <Link
                    className="account-menu-item"
                    onClick={() => closeAccountMenu()}
                    to={profilePath}
                  >
                    <UserRound size={16} />
                    My Profile
                  </Link>
                ) : null}
                <button className="account-menu-item account-menu-logout" onClick={handleLogout} type="button">
                  <LogOut size={16} />
                  Log Out
                </button>
              </div>
            ) : null}
          </div>
        </header>
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
