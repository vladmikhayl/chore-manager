import { NavLink } from "react-router-dom";
import type { ReactNode } from "react";

type AppNavLinkProps = {
  to: string;
  children: ReactNode;
  onClick?: () => void;
};

export function AppNavLink({ to, children, onClick }: AppNavLinkProps) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) =>
        [
          "block rounded-xl px-4 py-2 text-center text-sm font-medium transition md:inline-block",
          isActive
            ? "bg-indigo-100 text-indigo-700"
            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
        ].join(" ")
      }
    >
      {children}
    </NavLink>
  );
}
