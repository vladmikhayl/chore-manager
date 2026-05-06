import { Link, useLocation, useNavigate } from "react-router-dom";
import { useState, type SyntheticEvent } from "react";
import toast from "react-hot-toast";
import { AuthLayout } from "../components/auth/AuthLayout";
import { AuthCard } from "../components/auth/AuthCard";
import { TextInput } from "../components/shared/TextInput";
import { PrimaryButton } from "../components/shared/PrimaryButton";
import { login as loginRequest } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";
import { setAccessToken } from "../utils/authStorage";

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const searchParams = new URLSearchParams(location.search);
  const redirectPath = searchParams.get("redirect");

  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");

  const [loginError, setLoginError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function getSafeRedirectPath(redirectPath: string | null) {
    if (!redirectPath || !redirectPath.startsWith("/")) {
      return "/tasks";
    }

    if (redirectPath.startsWith("//")) {
      return "/tasks";
    }

    return redirectPath;
  }

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedLogin = login.trim();
    let hasErrors = false;

    setLoginError("");
    setPasswordError("");

    if (!trimmedLogin) {
      setLoginError("Введите логин");
      hasErrors = true;
    }

    if (!password) {
      setPasswordError("Введите пароль");
      hasErrors = true;
    }

    if (hasErrors) {
      return;
    }

    try {
      setIsSubmitting(true);

      const response = await loginRequest({
        login: trimmedLogin,
        password,
      });

      setAccessToken(response.token);
      toast.success("Вы успешно вошли в аккаунт");

      navigate(getSafeRedirectPath(redirectPath), { replace: true });
    } catch (error) {
      const parsedError = parseApiError(error);
      toast.error(parsedError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <AuthCard title="Вход">
        <form onSubmit={handleSubmit} className="space-y-5">
          <TextInput
            label="Логин"
            name="login"
            type="text"
            autoComplete="username"
            placeholder="Введите логин"
            value={login}
            disabled={isSubmitting}
            onChange={(e) => {
              setLogin(e.target.value);

              if (loginError) {
                setLoginError("");
              }
            }}
            error={loginError}
          />

          <TextInput
            label="Пароль"
            name="password"
            type="password"
            autoComplete="current-password"
            placeholder="Введите пароль"
            value={password}
            disabled={isSubmitting}
            onChange={(e) => {
              setPassword(e.target.value);

              if (passwordError) {
                setPasswordError("");
              }
            }}
            error={passwordError}
          />

          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Вход..." : "Войти"}
          </PrimaryButton>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          Нет аккаунта?{" "}
          <Link
            to={
              redirectPath
                ? `/register?redirect=${encodeURIComponent(redirectPath)}`
                : "/register"
            }
            className="font-medium text-indigo-600 transition hover:text-indigo-700"
          >
            Зарегистрироваться
          </Link>
        </p>
      </AuthCard>
    </AuthLayout>
  );
}
