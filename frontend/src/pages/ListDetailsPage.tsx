import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  createInvite,
  deleteList,
  getListDetails,
  leaveList,
} from "../api/listsApi";
import { AppLayout } from "../components/AppLayout";
import { PageSection } from "../components/shared/PageSection";
import type { TodoListDetailsResponse } from "../types/lists";
import { parseApiError } from "../utils/parseApiError";
import { CreateInviteModal } from "../components/lists/CreateInviteModal";
import toast from "react-hot-toast";
import type { CreateTaskRequest, TaskResponse } from "../types/tasks";
import { createTask, deleteTask, getListTasks } from "../api/tasksApi";
import { CreateTaskModal } from "../components/tasks/CreateTaskModal";
import { ListTaskCard } from "../components/tasks/ListTaskCard";

export function ListDetailsPage() {
  const navigate = useNavigate();

  const { listId } = useParams<{ listId: string }>();

  const [listDetails, setListDetails] =
    useState<TodoListDetailsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isMembersExpanded, setIsMembersExpanded] = useState(false);

  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [inviteToken, setInviteToken] = useState<string | null>(null);
  const [inviteErrorMessage, setInviteErrorMessage] = useState<string | null>(
    null,
  );
  const [isCreatingInvite, setIsCreatingInvite] = useState(false);

  const [isLeavingList, setIsLeavingList] = useState(false);
  const [isDeletingList, setIsDeletingList] = useState(false);

  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
  const [isCreatingTask, setIsCreatingTask] = useState(false);
  const [createTaskErrorMessage, setCreateTaskErrorMessage] = useState<
    string | null
  >(null);

  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [isTasksLoading, setIsTasksLoading] = useState(true);
  const [tasksErrorMessage, setTasksErrorMessage] = useState<string | null>(
    null,
  );
  const [deletingTaskId, setDeletingTaskId] = useState<string | null>(null);

  const loadTasks = useCallback(async () => {
    if (!listId) {
      setTasks([]);
      setIsTasksLoading(false);
      return;
    }

    try {
      setTasksErrorMessage(null);
      setIsTasksLoading(true);

      const response = await getListTasks(listId);
      setTasks(response);
    } catch (error) {
      const parsedError = parseApiError(error);
      setTasksErrorMessage(parsedError.message);
      setTasks([]);
    } finally {
      setIsTasksLoading(false);
    }
  }, [listId]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  const loadListDetails = useCallback(async () => {
    if (!listId) {
      setErrorMessage("Не удалось определить список дел.");
      setIsLoading(false);
      return;
    }

    try {
      setErrorMessage(null);
      const response = await getListDetails(listId);
      setListDetails(response);
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);
    } finally {
      setIsLoading(false);
    }
  }, [listId]);

  useEffect(() => {
    void loadListDetails();
  }, [loadListDetails]);

  if (isLoading) {
    return (
      <AppLayout>
        <div className="flex flex-col gap-6">
          <Link
            to="/lists"
            className="inline-flex w-fit items-center text-base font-semibold text-slate-700 transition hover:text-indigo-700"
          >
            ← К спискам дел
          </Link>

          <PageSection title="Список дел">
            <div className="rounded-2xl bg-slate-50 px-4 py-5 text-sm text-slate-600">
              Загружаем информацию о списке...
            </div>
          </PageSection>
        </div>
      </AppLayout>
    );
  }

  if (errorMessage && !listDetails) {
    return (
      <AppLayout>
        <div className="flex flex-col gap-6">
          <Link
            to="/lists"
            className="inline-flex w-fit items-center text-base font-semibold text-slate-700 transition hover:text-indigo-700"
          >
            ← К спискам дел
          </Link>

          <PageSection title="Список дел">
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700">
              {errorMessage}
            </div>
          </PageSection>
        </div>
      </AppLayout>
    );
  }

  if (!listDetails) {
    return null;
  }

  async function handleOpenInviteModal() {
    if (!listId) {
      return;
    }

    setInviteToken(null);
    setInviteErrorMessage(null);
    setIsCreatingInvite(true);
    setIsInviteModalOpen(true);

    try {
      const response = await createInvite(listId);
      setInviteToken(response.token);
    } catch (error) {
      const parsedError = parseApiError(error);
      setInviteErrorMessage(parsedError.message);
    } finally {
      setIsCreatingInvite(false);
    }
  }

  function handleCloseInviteModal() {
    if (isCreatingInvite) {
      return;
    }

    setIsInviteModalOpen(false);
    setInviteToken(null);
    setInviteErrorMessage(null);
  }

  async function handleLeaveList() {
    if (!listId || isLeavingList) {
      return;
    }

    try {
      setIsLeavingList(true);

      await leaveList(listId);

      toast.success("Вы успешно вышли из списка");
      navigate("/lists", { replace: true });
    } catch (error) {
      const parsedError = parseApiError(error);
      toast.error(parsedError.message);
    } finally {
      setIsLeavingList(false);
    }
  }

  async function handleDeleteList() {
    if (!listId || isDeletingList) {
      return;
    }

    const isConfirmed = window.confirm(
      "Вы действительно хотите удалить этот список дел? Это действие нельзя отменить.",
    );

    if (!isConfirmed) {
      return;
    }

    try {
      setIsDeletingList(true);

      await deleteList(listId);

      toast.success("Список дел успешно удалён");
      navigate("/lists", { replace: true });
    } catch (error) {
      const parsedError = parseApiError(error);
      toast.error(parsedError.message);
    } finally {
      setIsDeletingList(false);
    }
  }

  async function handleDeleteTask(taskId: string) {
    if (deletingTaskId) {
      return;
    }

    const isConfirmed = window.confirm(
      "Вы действительно хотите удалить эту задачу? Это действие нельзя отменить.",
    );

    if (!isConfirmed) {
      return;
    }

    try {
      setDeletingTaskId(taskId);

      await deleteTask(taskId);
      await loadTasks();

      toast.success("Задача успешно удалена");
    } catch (error) {
      const parsedError = parseApiError(error);
      toast.error(parsedError.message);
    } finally {
      setDeletingTaskId(null);
    }
  }

  function handleOpenTaskModal() {
    setCreateTaskErrorMessage(null);
    setIsTaskModalOpen(true);
  }

  function handleCloseTaskModal() {
    if (isCreatingTask) {
      return;
    }

    setIsTaskModalOpen(false);
    setCreateTaskErrorMessage(null);
  }

  async function handleCreateTask(request: CreateTaskRequest) {
    if (!listId) {
      return;
    }

    try {
      setIsCreatingTask(true);
      setCreateTaskErrorMessage(null);

      await createTask(listId, request);
      await loadTasks();

      toast.success("Задача успешно создана");
      setIsTaskModalOpen(false);
    } catch (error) {
      const parsedError = parseApiError(error);
      setCreateTaskErrorMessage(parsedError.message);
    } finally {
      setIsCreatingTask(false);
    }
  }

  const membersCount = listDetails.members.length;

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <Link
          to="/lists"
          className="inline-flex w-fit items-center text-base font-semibold text-slate-700 transition hover:text-indigo-700"
        >
          ← К спискам дел
        </Link>

        <PageSection>
          <div className="flex flex-col gap-2">
            <div className="flex flex-wrap items-center gap-3">
              <h2 className="text-2xl font-bold leading-none text-slate-900">
                {listDetails.title}
              </h2>

              {listDetails.isOwner ? (
                <span className="inline-flex items-center rounded-full bg-indigo-100 px-3 py-1 text-xs font-medium text-indigo-700">
                  Вы создатель
                </span>
              ) : (
                <span className="inline-flex items-center rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">
                  Вы приглашённый участник
                </span>
              )}
            </div>

            <p className="text-sm leading-6 text-slate-600">
              Здесь собрана основная информация об этом списке дел.
            </p>
          </div>
        </PageSection>

        <PageSection title="Участники списка">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-slate-600">
                Всего участников:{" "}
                <span className="font-semibold text-slate-900">
                  {membersCount}
                </span>
              </p>

              <div className="flex flex-wrap items-center gap-2">
                {membersCount > 0 && (
                  <button
                    type="button"
                    onClick={() => setIsMembersExpanded((prev) => !prev)}
                    className="cursor-pointer rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700"
                  >
                    <span className="inline-flex items-center gap-2">
                      <span>
                        {isMembersExpanded
                          ? "Скрыть участников"
                          : "Показать участников"}
                      </span>

                      <span className="text-[11px] leading-none text-slate-500">
                        {isMembersExpanded ? "▲" : "▼"}
                      </span>
                    </span>
                  </button>
                )}

                {listDetails.isOwner && (
                  <button
                    type="button"
                    onClick={() => void handleOpenInviteModal()}
                    className="w-full rounded-xl border border-indigo-200 bg-indigo-50 px-5 py-2 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 sm:w-56"
                  >
                    Пригласить участника
                  </button>
                )}
              </div>
            </div>

            {membersCount === 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-600">
                Пока в этом списке нет участников.
              </div>
            ) : (
              isMembersExpanded && (
                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
                  {listDetails.members.map((member, index) => {
                    const isOwner = member.userId === listDetails.ownerUserId;

                    return (
                      <div
                        key={member.userId}
                        className={`flex items-center justify-between gap-3 px-4 py-3 ${
                          index !== listDetails.members.length - 1
                            ? "border-b border-slate-200"
                            : ""
                        }`}
                      >
                        <span className="text-sm font-medium text-slate-900">
                          {member.login}
                        </span>

                        {isOwner && (
                          <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-700">
                            Создатель
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>
              )
            )}
          </div>
        </PageSection>

        <PageSection title="Задачи списка">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-slate-600">
                Всего задач:{" "}
                <span className="font-semibold text-slate-900">
                  {tasks.length}
                </span>
              </p>

              <button
                type="button"
                onClick={handleOpenTaskModal}
                className="w-full rounded-xl border border-indigo-200 bg-indigo-50 px-5 py-2 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 sm:w-56"
              >
                Создать задачу
              </button>
            </div>

            {isTasksLoading ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
                Загрузка задач...
              </div>
            ) : tasksErrorMessage ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-6 text-sm text-red-700">
                {tasksErrorMessage}
              </div>
            ) : tasks.length === 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
                <p>В этом списке пока нет задач 🙂</p>
                <p className="mt-2">
                  Создайте первую задачу, и она появится здесь.
                </p>
              </div>
            ) : (
              <div className="grid gap-3">
                {tasks.map((task) => (
                  <ListTaskCard
                    key={task.id}
                    task={task}
                    members={listDetails.members}
                    onDelete={(taskId) => void handleDeleteTask(taskId)}
                    isDeleting={deletingTaskId === task.id}
                  />
                ))}
              </div>
            )}
          </div>
        </PageSection>

        {!listDetails.isOwner ? (
          <button
            type="button"
            onClick={() => void handleLeaveList()}
            disabled={isLeavingList}
            className="cursor-pointer rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLeavingList ? "Выходим из списка..." : "Покинуть список"}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => void handleDeleteList()}
            disabled={isDeletingList}
            className="cursor-pointer rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isDeletingList ? "Удаляем список..." : "Удалить список"}
          </button>
        )}
      </div>

      {isInviteModalOpen && (
        <CreateInviteModal
          token={inviteToken}
          isLoading={isCreatingInvite}
          errorMessage={inviteErrorMessage}
          onClose={handleCloseInviteModal}
        />
      )}

      {isTaskModalOpen && (
        <CreateTaskModal
          members={listDetails.members}
          isLoading={isCreatingTask}
          errorMessage={createTaskErrorMessage}
          onClose={handleCloseTaskModal}
          onSubmit={handleCreateTask}
        />
      )}
    </AppLayout>
  );
}
