import { Link, useNavigate } from "react-router-dom";
import { removeAccessToken } from "../utils/authStorage";
import { AppNavLink } from "./AppNavLink";
import toast from "react-hot-toast";

export function AppHeader() {
  const navigate = useNavigate();

  function handleLogout() {
    removeAccessToken();
    navigate("/login", { replace: true });
    toast.success("Вы успешно вышли из аккаунта");
  }

  return (
    <header className="border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto max-w-6xl px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:gap-8">
            <div className="flex items-center justify-between gap-3 md:block">
              <Link
                to="/tasks"
                className="shrink-0 text-lg font-bold tracking-tight text-slate-900 transition hover:text-indigo-600"
              >
                Chore Manager
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="cursor-pointer rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 md:hidden"
              >
                Выйти
              </button>
            </div>

            <nav className="grid grid-cols-1 gap-2 sm:grid-cols-3 md:flex md:items-center md:gap-2">
              <AppNavLink to="/tasks">Задачи</AppNavLink>
              <AppNavLink to="/lists">Списки дел</AppNavLink>
              <AppNavLink to="/profile">Профиль</AppNavLink>
            </nav>
          </div>

          <div className="hidden md:block">
            <button
              type="button"
              onClick={handleLogout}
              className="cursor-pointer rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
            >
              Выйти
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
