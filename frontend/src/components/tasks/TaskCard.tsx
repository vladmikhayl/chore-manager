type TaskCardProps = {
  title: string;
  listName: string;
  isCompleted: boolean;
  onToggleCompleted: () => void;
};

export function TaskCard({
  title,
  listName,
  isCompleted,
  onToggleCompleted,
}: TaskCardProps) {
  return (
    <article
      className={[
        "rounded-2xl border p-4 transition",
        isCompleted
          ? "border-emerald-200 bg-emerald-50"
          : "border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-slate-100",
      ].join(" ")}
    >
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-medium text-indigo-700">
              {listName}
            </span>

            <span
              className={[
                "rounded-full px-2.5 py-1 text-xs font-medium",
                isCompleted
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-slate-200 text-slate-700",
              ].join(" ")}
            >
              {isCompleted ? "Выполнено" : "Не выполнено"}
            </span>
          </div>

          <h3
            className={[
              "mt-3 text-lg font-semibold",
              isCompleted ? "text-slate-700 line-through" : "text-slate-900",
            ].join(" ")}
          >
            {title}
          </h3>

          <p className="mt-1 text-sm text-slate-600">Список дел: {listName}</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="cursor-pointer rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Подробнее
          </button>

          <button
            type="button"
            onClick={onToggleCompleted}
            className={[
              "cursor-pointer rounded-xl px-4 py-2 text-sm font-semibold text-white transition",
              isCompleted
                ? "bg-slate-600 hover:bg-slate-700"
                : "bg-emerald-600 hover:bg-emerald-700",
            ].join(" ")}
          >
            {isCompleted ? "Снять выполнение" : "Отметить выполненной"}
          </button>
        </div>
      </div>
    </article>
  );
}
