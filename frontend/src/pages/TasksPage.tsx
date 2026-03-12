import { AppLayout } from "../components/AppLayout";

const mockTasks = [
  {
    id: 1,
    title: "Вынести мусор",
    listName: "Дом",
    assignee: "Вы",
    dueLabel: "Сегодня",
  },
  {
    id: 2,
    title: "Купить продукты",
    listName: "Покупки",
    assignee: "Вы",
    dueLabel: "Сегодня",
  },
  {
    id: 3,
    title: "Помыть посуду",
    listName: "Квартира",
    assignee: "Анна",
    dueLabel: "Сегодня",
  },
];

export function TasksPage() {
  return (
    <AppLayout
      title="Задачи"
      description="Здесь отображаются ваши задачи из всех списков дел на выбранную дату."
    >
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">На сегодня</h2>
            <p className="mt-1 text-sm text-slate-600">
              3 задачи требуют внимания
            </p>
          </div>

          <button
            type="button"
            className="cursor-pointer rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700"
          >
            Создать задачу
          </button>
        </div>

        <div className="mt-6 grid gap-4">
          {mockTasks.map((task) => (
            <article
              key={task.id}
              className="rounded-2xl border border-slate-200 bg-slate-50 p-4 transition hover:border-slate-300 hover:bg-slate-100"
            >
              <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-medium text-indigo-700">
                      {task.listName}
                    </span>

                    <span className="rounded-full bg-slate-200 px-2.5 py-1 text-xs font-medium text-slate-700">
                      {task.dueLabel}
                    </span>
                  </div>

                  <h3 className="mt-3 text-lg font-semibold text-slate-900">
                    {task.title}
                  </h3>

                  <p className="mt-1 text-sm text-slate-600">
                    Ответственный: {task.assignee}
                  </p>
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
                    className="cursor-pointer rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700"
                  >
                    Выполнено
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </AppLayout>
  );
}
