import type { ReactNode } from "react";
import { AppHeader } from "./AppHeader";

type AppLayoutProps = {
  title: string;
  description: string;
  children: ReactNode;
};

export function AppLayout({ title, description, children }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-slate-100">
      <AppHeader />

      <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="grid gap-6">
          <section className="rounded-3xl bg-gradient-to-br from-indigo-600 to-blue-500 p-6 text-white shadow-xl">
            <h1 className="text-3xl font-bold tracking-tight">{title}</h1>

            <p className="mt-3 max-w-2xl text-sm leading-6 text-blue-50">
              {description}
            </p>
          </section>

          {children}
        </div>
      </main>
    </div>
  );
}
