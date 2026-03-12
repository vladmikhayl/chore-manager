import { useMemo, useState } from "react";
import { AppLayout } from "../components/AppLayout";
import { TaskCard } from "../components/tasks/TaskCard";

type MockTask = {
  id: number;
  title: string;
  listName: string;
  isCompleted: boolean;
};

const initialMockTasks: MockTask[] = [
  {
    id: 1,
    title: "Вынести мусор",
    listName: "Дом",
    isCompleted: false,
  },
  {
    id: 2,
    title: "Купить продукты",
    listName: "Покупки",
    isCompleted: true,
  },
  {
    id: 3,
    title: "Помыть посуду",
    listName: "Квартира",
    isCompleted: false,
  },
];

function getTodayDateString(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export function TasksPage() {
  const todayDate = useMemo(() => getTodayDateString(), []);
  const [selectedDate, setSelectedDate] = useState(todayDate);
  const [tasks, setTasks] = useState(initialMockTasks);

  const completedCount = tasks.filter((task) => task.isCompleted).length;
  const pendingCount = tasks.length - completedCount;

  function handleToggleTaskCompleted(taskId: number) {
    setTasks((previousTasks) =>
      previousTasks.map((task) =>
        task.id === taskId ? { ...task, isCompleted: !task.isCompleted } : task,
      ),
    );
  }

  return (
    <AppLayout
      title="Задачи"
      description="Здесь отображаются задачи из всех списков дел, за которые вы отвечаете на выбранную дату."
    >
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-4 mb-5">
          <span className="text-sm font-medium text-slate-700">
            Выберите дату
          </span>

          <input
            type="date"
            value={selectedDate}
            min={todayDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="w-full max-w-xs rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100"
          />
        </div>

        <p className="text-sm text-slate-600">
          Всего задач на выбранную дату: {tasks.length}. Не выполнено:{" "}
          {pendingCount}. Выполнено: {completedCount}.
        </p>

        <div className="mt-6 grid gap-4">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              title={task.title}
              listName={task.listName}
              isCompleted={task.isCompleted}
              onToggleCompleted={() => handleToggleTaskCompleted(task.id)}
            />
          ))}
        </div>
      </section>
    </AppLayout>
  );
}
