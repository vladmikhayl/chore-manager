type TodoListCardProps = {
  title: string;
  membersCount: number;
  isOwner: boolean;
  onOpen: () => void;
};

export function TodoListCard({
  title,
  membersCount,
  isOwner,
  onOpen,
}: TodoListCardProps) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 p-5 transition hover:border-slate-300 hover:bg-slate-100">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-slate-200 px-3 py-1 text-xs font-medium text-slate-700">
              Участников: {membersCount}
            </span>

            {isOwner ? (
              <span className="rounded-full bg-indigo-100 px-3 py-1 text-xs font-medium text-indigo-700">
                Вы создатель
              </span>
            ) : (
              <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">
                Вы приглашённый участник
              </span>
            )}
          </div>

          <h3 className="mt-3 break-words text-xl font-semibold text-slate-900">
            {title}
          </h3>
        </div>

        <div className="flex sm:justify-end">
          <button
            type="button"
            onClick={onOpen}
            className="cursor-pointer rounded-xl bg-indigo-600 px-8 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700"
          >
            Открыть список
          </button>
        </div>
      </div>
    </article>
  );
}
