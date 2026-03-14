type CreateListModalProps = {
  title: string;
  onTitleChange: (value: string) => void;
  onClose: () => void;
  onSubmit: () => void;
  isSubmitting?: boolean;
  errorMessage?: string | null;
};

export function CreateListModal({
  title,
  onTitleChange,
  onClose,
  onSubmit,
  isSubmitting = false,
  errorMessage = null,
}: CreateListModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <div className="flex flex-col gap-5">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">
              Новый список дел
            </h2>
          </div>

          <div className="flex flex-col gap-2">
            <label
              htmlFor="list-title"
              className="text-sm font-medium text-slate-700"
            >
              Название списка
            </label>

            <input
              id="list-title"
              type="text"
              value={title}
              onChange={(e) => onTitleChange(e.target.value)}
              disabled={isSubmitting}
              placeholder="Например, Домашние дела"
              className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 disabled:bg-slate-50"
            />
          </div>

          {errorMessage && (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="cursor-pointer rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Отмена
            </button>

            <button
              type="button"
              onClick={onSubmit}
              disabled={isSubmitting}
              className="cursor-pointer rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-400"
            >
              {isSubmitting ? "Создание..." : "Создать список"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
