import { Link, useNavigate } from "react-router-dom";
import { removeAccessToken } from "../utils/authStorage";
import { AppNavLink } from "./AppNavLink";

export function AppHeader() {
  const navigate = useNavigate();

  function handleLogout() {
    removeAccessToken();
    navigate("/login", { replace: true });
  }

  return (
    <header className="border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6">
          <div className="shrink-0">
            <Link
              to="/tasks"
              className="text-lg font-bold tracking-tight text-slate-900 transition hover:text-indigo-600"
            >
              Chore Manager
            </Link>
          </div>

          <nav className="flex items-center gap-2">
            <AppNavLink to="/tasks">Задачи</AppNavLink>
            <AppNavLink to="/lists">Списки дел</AppNavLink>
            <AppNavLink to="/profile">Профиль</AppNavLink>
          </nav>
        </div>

        <button
          type="button"
          onClick={handleLogout}
          className="cursor-pointer rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
        >
          Выйти
        </button>
      </div>
    </header>
  );
}
