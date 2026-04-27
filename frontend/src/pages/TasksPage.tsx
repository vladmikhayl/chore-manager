import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppLayout } from "../components/AppLayout";
import { TaskCard } from "../components/tasks/TaskCard";
import {
  completeTask,
  deleteTaskCompletion,
  getTasksForDay,
} from "../api/tasksApi";
import type { TaskListItem, TaskResponse } from "../types/tasks";
import { parseApiError } from "../utils/parseApiError";
import { TaskDatePicker } from "../components/tasks/TaskDatePicker";

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
    listId: task.listId,
    listTitle: task.listTitle,
    completed: task.completed,
  };
}

export function TasksPage() {
  const navigate = useNavigate();
  const todayDate = useMemo(() => getTodayDateString(), []);
  const [selectedDate, setSelectedDate] = useState(todayDate);
  const [tasks, setTasks] = useState<TaskListItem[]>([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [processingTaskId, setProcessingTaskId] = useState<string | null>(null);

  const loadTasks = useCallback(async (date: string) => {
    try {
      setErrorMessage(null);

      const response = await getTasksForDay(date);
      const mappedTasks = response.map(mapTaskResponseToTaskListItem);

      setTasks(mappedTasks);
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);
      setTasks([]);
    } finally {
      setIsInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!selectedDate) {
      setTasks([]);
      setErrorMessage(null);
      setIsInitialLoading(false);
      return;
    }

    void loadTasks(selectedDate);
  }, [selectedDate, loadTasks]);

  async function handleToggleCompleted(task: TaskListItem) {
    if (!selectedDate) {
      setErrorMessage("Сначала выберите дату.");
      return;
    }

    try {
      setProcessingTaskId(task.id);
      setErrorMessage(null);

      if (task.completed) {
        await deleteTaskCompletion(task.id, selectedDate);
      } else {
        await completeTask(task.id, selectedDate);
      }

      await loadTasks(selectedDate);
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);
    } finally {
      setProcessingTaskId(null);
    }
  }

  function handleOpenTaskDetails(task: TaskListItem) {
    navigate(`/lists/${task.listId}?taskId=${task.id}`);
  }

  const completedCount = tasks.filter((task) => task.completed).length;
  const pendingCount = tasks.length - completedCount;

  return (
    <AppLayout
      title="Задачи"
      description="Здесь отображаются задачи из всех списков дел, за которые вы отвечаете на выбранную дату."
    >
      <section className="min-w-0 max-w-full overflow-hidden rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col gap-5">
          <div className="px-4">
            <TaskDatePicker
              id="tasks-date"
              label="Выберите дату"
              value={selectedDate}
              min={todayDate}
              onChange={setSelectedDate}
              className="max-w-xs"
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

          {!selectedDate ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              Выберите дату, чтобы посмотреть задачи.
            </div>
          ) : isInitialLoading ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              Загрузка задач...
            </div>
          ) : errorMessage ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-6 text-sm text-red-700">
              {errorMessage}
            </div>
          ) : tasks.length === 0 ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              <p>На выбранную дату задач пока нет 🙂</p>
              <p className="mt-2">
                Когда в одном из списков для вас появится задача с назначением
                на этот день, она отобразится здесь.
              </p>
            </div>
          ) : (
            <div className="grid min-w-0 max-w-full gap-4">
              {tasks.map((task) => (
                <TaskCard
                  key={task.id}
                  title={task.title}
                  listTitle={task.listTitle}
                  completed={task.completed}
                  onOpenDetails={() => handleOpenTaskDetails(task)}
                  isToggleLoading={processingTaskId === task.id}
                  onToggleCompleted={() => void handleToggleCompleted(task)}
                />
              ))}
            </div>
          )}
        </div>
      </section>
    </AppLayout>
  );
}
