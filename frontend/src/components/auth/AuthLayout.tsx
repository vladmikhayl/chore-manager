import type { ReactNode } from "react";

type AuthLayoutProps = {
  children: ReactNode;
};

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-slate-100">
      <div className="mx-auto flex min-h-screen max-w-6xl items-center justify-center p-4">
        <div className="grid w-full overflow-hidden rounded-3xl bg-white shadow-2xl md:grid-cols-2">
          <div className="hidden bg-gradient-to-br from-indigo-600 to-blue-500 p-10 text-white md:flex md:flex-col md:justify-between">
            <div>
              <div className="inline-flex rounded-full bg-white/15 px-3 py-1 text-sm font-medium backdrop-blur">
                Chore Manager
              </div>

              <h1 className="mt-6 text-4xl font-bold leading-tight">
                Совместное управление
                <br />
                бытовыми задачами
              </h1>

              <p className="mt-4 max-w-md text-sm leading-6 text-blue-50">
                Приложение для ведения общих списков дел и автоматического
                распределения обязанностей.
              </p>
            </div>

            <div className="text-sm text-blue-100">
              Удобно для семьи, соседей и небольших коллективов
            </div>
          </div>

          <div className="flex items-center justify-center bg-white p-6 sm:p-10">
            <div className="w-full max-w-md">{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
