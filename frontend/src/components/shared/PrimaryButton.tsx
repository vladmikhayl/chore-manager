import type { ButtonHTMLAttributes } from "react";

type PrimaryButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

export function PrimaryButton({
  children,
  className = "",
  disabled,
  ...props
}: PrimaryButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled}
      className={[
        "inline-flex w-full cursor-pointer items-center justify-center rounded-xl px-4 py-3 text-sm font-semibold text-white transition",
        "bg-indigo-600 hover:bg-indigo-700",
        "focus:outline-none focus:ring-4 focus:ring-indigo-200",
        "disabled:cursor-not-allowed disabled:bg-slate-300",
        className,
      ].join(" ")}
    >
      {children}
    </button>
  );
}
