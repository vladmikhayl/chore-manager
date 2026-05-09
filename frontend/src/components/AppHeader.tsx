import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { AppNavLink } from "./AppNavLink";
import { removeAccessToken } from "../utils/authStorage";

export function AppHeader() {
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  function handleLogout() {
    removeAccessToken();
    navigate("/login", { replace: true });
    toast.success("Вы успешно вышли из аккаунта");
  }

  function closeMenu() {
    setIsMenuOpen(false);
  }

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto max-w-6xl px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-8">
            <Link
              to="/tasks"
              onClick={closeMenu}
              className="shrink-0 text-lg font-bold tracking-tight text-slate-900 transition hover:text-indigo-600"
            >
              Chore Manager
            </Link>

            <nav className="hidden md:flex md:items-center md:gap-2">
              <AppNavLink to="/tasks">Задачи</AppNavLink>
              <AppNavLink to="/lists">Списки дел</AppNavLink>
              <AppNavLink to="/profile">Профиль</AppNavLink>
            </nav>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="hidden cursor-pointer rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 md:block"
          >
            Выйти
          </button>

          <button
            type="button"
            onClick={() => setIsMenuOpen((value) => !value)}
            className="cursor-pointer rounded-xl px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 md:hidden"
            aria-label="Открыть меню"
            aria-expanded={isMenuOpen}
          >
            <span
              className={[
                "inline-block transition-transform duration-200",
                isMenuOpen ? "rotate-90" : "rotate-0",
              ].join(" ")}
            >
              ☰
            </span>
          </button>
        </div>

        <div
          className={[
            "grid transition-all duration-200 ease-out md:hidden",
            isMenuOpen
              ? "grid-rows-[1fr] opacity-100"
              : "grid-rows-[0fr] opacity-0",
          ].join(" ")}
        >
          <div className="overflow-hidden">
            <div className="mt-3 rounded-2xl border border-slate-200 bg-white p-2 shadow-lg">
              <nav className="grid gap-1">
                <AppNavLink to="/tasks" onClick={closeMenu}>
                  Задачи
                </AppNavLink>
                <AppNavLink to="/lists" onClick={closeMenu}>
                  Списки дел
                </AppNavLink>
                <AppNavLink to="/profile" onClick={closeMenu}>
                  Профиль
                </AppNavLink>
              </nav>

              <button
                type="button"
                onClick={handleLogout}
                className="mt-2 w-full cursor-pointer rounded-xl px-4 py-2 text-center text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
              >
                Выйти
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
