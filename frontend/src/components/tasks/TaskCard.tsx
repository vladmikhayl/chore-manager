type TaskCardProps = {
  title: string;
  listTitle: string;
  completed: boolean;
  onOpenDetails: () => void;
  onToggleCompleted: () => void;
  isToggleLoading?: boolean;
};

export function TaskCard({
  title,
  listTitle,
  completed,
  onOpenDetails,
  onToggleCompleted,
  isToggleLoading = false,
}: TaskCardProps) {
  return (
    <article
      className={[
        "rounded-2xl border p-5 transition",
        completed
          ? "border-emerald-200 bg-emerald-50"
          : "border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-slate-100",
      ].join(" ")}
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-indigo-100 px-3 py-1 text-xs font-medium text-indigo-700">
              {listTitle}
            </span>

            <span
              className={[
                "rounded-full px-3 py-1 text-xs font-medium",
                completed
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-slate-200 text-slate-700",
              ].join(" ")}
            >
              {completed ? "Выполнено" : "Не выполнено"}
            </span>
          </div>

          <h3
            className={[
              "mt-3 break-words text-lg font-semibold",
              completed ? "text-slate-700 line-through" : "text-slate-900",
            ].join(" ")}
          >
            {title}
          </h3>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <button
            type="button"
            onClick={onOpenDetails}
            className="cursor-pointer rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Подробнее
          </button>

          <button
            type="button"
            onClick={onToggleCompleted}
            disabled={isToggleLoading}
            className={[
              "rounded-xl px-4 py-2 text-sm font-semibold text-white transition",
              isToggleLoading
                ? "cursor-not-allowed bg-slate-400"
                : completed
                  ? "cursor-pointer bg-slate-600 hover:bg-slate-700"
                  : "cursor-pointer bg-emerald-600 hover:bg-emerald-700",
            ].join(" ")}
          >
            {isToggleLoading
              ? "Сохранение..."
              : completed
                ? "Снять выполнение"
                : "Отметить выполненной"}
          </button>
        </div>
      </div>
    </article>
  );
}
