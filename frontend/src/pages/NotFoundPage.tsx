import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <main className="not-found">
      <span className="brand-mark">JF</span>
      <p className="eyebrow">404</p>
      <h1>Page not found</h1>
      <p>The page you are looking for does not exist or has moved.</p>
      <Link className="primary-button" to="/">
        Back to JobFlow
      </Link>
    </main>
  );
}
