import type { ReactNode } from "react";

type AuthCardProps = {
  title: string;
  children: ReactNode;
};

export function AuthCard({ title, children }: AuthCardProps) {
  return (
    <div>
      <h2 className="text-3xl font-bold tracking-tight text-slate-900">
        {title}
      </h2>
      <div className="mt-8">{children}</div>
    </div>
  );
}
