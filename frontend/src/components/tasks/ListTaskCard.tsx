import type { TodoListMemberResponse } from "../../types/lists";
import type { TaskResponse } from "../../types/tasks";

type ListTaskCardProps = {
  task: TaskResponse;
  members: TodoListMemberResponse[];
  onDelete: (taskId: string) => void;
  onEditAssignmentRule: (task: TaskResponse) => void;
  isDeleting: boolean;
  isEditingRule: boolean;
};

const weekdayLabels: Record<number, string> = {
  0: "пн",
  1: "вт",
  2: "ср",
  3: "чт",
  4: "пт",
  5: "сб",
  6: "вс",
};

function formatWeekdays(weekdays: number[] | null): string {
  if (!weekdays || weekdays.length === 0) {
    return "не указаны";
  }

  return [...weekdays]
    .sort((a, b) => a - b)
    .map((day) => weekdayLabels[day] ?? String(day))
    .join(", ");
}

function findLoginByUserId(
  userId: string,
  members: TodoListMemberResponse[],
): string {
  return (
    members.find((member) => member.userId === userId)?.login ??
    "Неизвестный участник"
  );
}

function formatRecurrence(task: TaskResponse): string {
  if (task.recurrenceType === "WeeklyByDays") {
    return `по дням недели (${formatWeekdays(task.weekdays)})`;
  }

  if (task.recurrenceType === "EveryNdays") {
    return task.intervalDays
      ? `каждые ${task.intervalDays} дн.`
      : "через фиксированный интервал";
  }

  return "не указано";
}

function formatAssignment(
  task: TaskResponse,
  members: TodoListMemberResponse[],
): string {
  if (task.assignmentType === "FixedUser") {
    if (!task.fixedUserId) {
      return "всегда один исполнитель";
    }

    return `всегда ${findLoginByUserId(task.fixedUserId, members)}`;
  }

  if (task.assignmentType === "RoundRobin") {
    const users = task.roundRobinUsers?.map((user) => user.login) ?? [];

    return users.length > 0 ? `по кругу (${users.join(" → ")})` : "по кругу";
  }

  if (task.assignmentType === "ByWeekday") {
    if (!task.weekdayAssignees) {
      return "по дням недели";
    }

    const orderedDays = Object.entries(task.weekdayAssignees)
      .map(([day, userId]) => [Number(day), userId] as const)
      .sort((a, b) => a[0] - b[0]);

    if (orderedDays.length === 0) {
      return "по дням недели";
    }

    return `по дням недели (${orderedDays
      .map(
        ([day, userId]) =>
          `${weekdayLabels[day]} - ${findLoginByUserId(userId, members)}`,
      )
      .join(", ")})`;
  }

  return "не указано";
}

export function ListTaskCard({
  task,
  members,
  onDelete,
  onEditAssignmentRule,
  isDeleting,
  isEditingRule,
}: ListTaskCardProps) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 p-4 transition hover:border-slate-300">
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-col gap-3">
            <h3 className="break-words text-lg font-semibold text-slate-900">
              {task.title}
            </h3>

            <div className="grid gap-2 text-sm text-slate-700">
              <p>
                <span className="font-medium text-slate-900">Повторение:</span>{" "}
                {formatRecurrence(task)}
              </p>

              <p>
                <span className="font-medium text-slate-900">Назначение:</span>{" "}
                {formatAssignment(task, members)}
              </p>
            </div>
          </div>

          <div className="flex flex-col gap-2 self-start sm:self-auto">
            <button
              type="button"
              onClick={() => onEditAssignmentRule(task)}
              disabled={isDeleting || isEditingRule}
              className="cursor-pointer rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-60 sm:w-52"
            >
              {isEditingRule ? "Сохраняем..." : "Редактировать правило"}
            </button>

            <button
              type="button"
              onClick={() => onDelete(task.id)}
              disabled={isDeleting || isEditingRule}
              className="cursor-pointer rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60 sm:w-52"
            >
              {isDeleting ? "Удаляем..." : "Удалить задачу"}
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}
