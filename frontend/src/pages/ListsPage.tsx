import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppLayout } from "../components/AppLayout";
import { TodoListCard } from "../components/lists/TodoListCard";
import { getLists } from "../api/listsApi";
import type { TodoListShortResponse } from "../types/lists";
import { parseApiError } from "../utils/parseApiError";

export function ListsPage() {
  const navigate = useNavigate();

  const [lists, setLists] = useState<TodoListShortResponse[]>([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const hasLoadedOnceRef = useRef(false);

  const loadLists = useCallback(async () => {
    try {
      setErrorMessage(null);

      const response = await getLists();
      setLists(response);
      hasLoadedOnceRef.current = true;
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);

      if (!hasLoadedOnceRef.current) {
        setLists([]);
      }
    } finally {
      setIsInitialLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadLists();
  }, [loadLists]);

  function handleOpenList(listId: string) {
    navigate(`/lists/${listId}`);
  }

  return (
    <AppLayout
      title="Списки дел"
      description="Здесь отображаются все списки дел, в которых вы участвуете."
    >
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col gap-5">
          <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
            Всего списков дел:{" "}
            <span className="font-semibold text-slate-900">{lists.length}</span>
            .
          </div>

          {isInitialLoading ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              Загрузка списков дел...
            </div>
          ) : errorMessage && lists.length === 0 ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-6 text-sm text-red-700">
              {errorMessage}
            </div>
          ) : lists.length === 0 ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
              <p>У вас пока нет списков дел 🙂</p>
              <p className="mt-2">
                Когда вы создадите новый список или вас добавят в существующий,
                он появится на этой странице.
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {errorMessage && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-700">
                  {errorMessage}
                </div>
              )}

              <div className="grid gap-4">
                {lists.map((list) => (
                  <TodoListCard
                    key={list.id}
                    title={list.title}
                    membersCount={list.membersCount}
                    isOwner={list.isOwner}
                    onOpen={() => handleOpenList(list.id)}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </AppLayout>
  );
}
