import { useNavigate } from "react-router-dom";
import { removeAccessToken } from "../utils/authStorage";
import { PrimaryButton } from "../components/shared/PrimaryButton";
import toast from "react-hot-toast";

export function TasksPage() {
  const navigate = useNavigate();

  function handleLogout() {
    removeAccessToken();
    navigate("/login", { replace: true });
    toast.success("Вы успешно вышли из аккаунта");
  }

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-5xl">
        <div className="rounded-3xl bg-white p-8 shadow-xl">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="inline-flex rounded-full bg-indigo-100 px-3 py-1 text-sm font-medium text-indigo-700">
                Chore Manager
              </div>

              <h1 className="mt-4 text-3xl font-bold text-slate-900">
                Главная страница
              </h1>

              <p className="mt-3 max-w-2xl text-slate-600">
                Вы успешно вошли в систему. Здесь позже будет основной интерфейс
                приложения: списки дел, задачи, распределение обязанностей и
                профиль.
              </p>
            </div>

            <div className="w-40">
              <PrimaryButton type="button" onClick={handleLogout}>
                Выйти
              </PrimaryButton>
            </div>
          </div>

          <div className="mt-8 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-slate-600">
            Заглушка приватной страницы
          </div>
        </div>
      </div>
    </div>
  );
}
