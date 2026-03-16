import type { ReactNode } from "react";

type PageSectionProps = {
  title?: string;
  description?: string;
  children: ReactNode;
};

export function PageSection({
  title,
  description,
  children,
}: PageSectionProps) {
  return (
    <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
      <div className="flex flex-col gap-4">
        {(title || description) && (
          <div>
            {title && (
              <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
            )}

            {description && (
              <p className="mt-1 text-sm leading-6 text-slate-600">
                {description}
              </p>
            )}
          </div>
        )}

        {children}
      </div>
    </section>
  );
}
