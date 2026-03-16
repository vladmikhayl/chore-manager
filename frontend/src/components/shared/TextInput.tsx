import type { InputHTMLAttributes } from "react";

type TextInputProps = InputHTMLAttributes<HTMLInputElement> & {
  label?: string;
  error?: string;
};

export function TextInput({
  label,
  error,
  className = "",
  ...props
}: TextInputProps) {
  return (
    <label className="block">
      {label ? (
        <span className="mb-2 block text-sm font-medium text-slate-700">
          {label}
        </span>
      ) : null}

      <input
        {...props}
        className={[
          "w-full rounded-xl border bg-white px-4 py-3 text-slate-900 outline-none transition",
          "placeholder:text-slate-400",
          error
            ? "border-red-300 focus:border-red-500 focus:ring-4 focus:ring-red-100"
            : "border-slate-200 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100",
          className,
        ].join(" ")}
      />

      {error ? (
        <span className="mt-2 block text-sm text-red-600">{error}</span>
      ) : null}
    </label>
  );
}
