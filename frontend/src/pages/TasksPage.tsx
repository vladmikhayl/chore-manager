import { useEffect, useMemo, useState } from "react";
import { AppLayout } from "../components/AppLayout";
import { TaskCard } from "../components/tasks/TaskCard";
import { getTasksForDay } from "../api/tasksApi";
import type { TaskListItem, TaskResponse } from "../types/tasks";
import { parseApiError } from "../utils/parseApiError";

function getTodayDateString(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function mapTaskResponseToTaskListItem(task: TaskResponse): TaskListItem {
  return {
    id: task.id,
    title: task.title,
    listTitle: task.listTitle,
    completed: task.completed,
  };
}

export function TasksPage() {
  const todayDate = useMemo(() => getTodayDateString(), []);
  const [selectedDate, setSelectedDate] = useState(todayDate);
  const [tasks, setTasks] = useState<TaskListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadTasks() {
      try {
        setIsLoading(true);
        setErrorMessage(null);

        const response = await getTasksForDay(selectedDate);
        const mappedTasks = response.map(mapTaskResponseToTaskListItem);

        setTasks(mappedTasks);
      } catch (error) {
        const parsedError = parseApiError(error);
        setErrorMessage(parsedError.message);
        setTasks([]);
      } finally {
        setIsLoading(false);
      }
    }

    void loadTasks();
  }, [selectedDate]);

  const completedCount = tasks.filter((task) => task.completed).length;
  const pendingCount = tasks.length - completedCount;

  return (
    <AppLayout
      title="Задачи"
      description="Здесь отображаются задачи из всех списков дел, за которые вы отвечаете на выбранную дату."
    >
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-4">
            <label
              htmlFor="tasks-date"
              className="text-sm font-medium text-slate-700"
            >
              Выберите дату
            </label>

            <input
              id="tasks-date"
              type="date"
              value={selectedDate}
              min={todayDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full max-w-xs rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100"
            />
          </div>

          <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
            Всего задач на выбранную дату:{" "}
            <span className="font-semibold text-slate-900">{tasks.length}</span>
            . Не выполнено:{" "}
            <span className="font-semibold text-slate-900">{pendingCount}</span>
            . Выполнено:{" "}
            <span className="font-semibold text-slate-900">
              {completedCount}
            </span>
            .
          </div>

          {isLoading && (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              Загрузка задач...
            </div>
          )}

          {!isLoading && errorMessage && (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-6 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          {!isLoading && !errorMessage && tasks.length === 0 && (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              На выбранную дату задач нет.
            </div>
          )}

          {!isLoading && !errorMessage && tasks.length > 0 && (
            <div className="grid gap-4">
              {tasks.map((task) => (
                <TaskCard
                  key={task.id}
                  title={task.title}
                  listTitle={task.listTitle}
                  completed={task.completed}
                  onToggleCompleted={() => {}}
                />
              ))}
            </div>
          )}
        </div>
      </section>
    </AppLayout>
  );
}
