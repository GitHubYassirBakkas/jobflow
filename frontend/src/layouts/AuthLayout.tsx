import { Outlet } from "react-router-dom";

export function AuthLayout() {
  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div className="brand-lockup">
          <span className="brand-mark">JF</span>
          <div>
            <p className="eyebrow">JobFlow</p>
            <h1>Recruiting work, neatly organized.</h1>
          </div>
        </div>
        <p className="auth-copy">
          A focused workspace for candidates tracking opportunities and recruiters moving hiring
          forward.
        </p>
      </section>
      <section className="auth-card">
        <Outlet />
      </section>
    </main>
  );
}
