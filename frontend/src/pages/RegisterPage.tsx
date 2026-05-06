import { Link, useLocation, useNavigate } from "react-router-dom";
import { useState, type SyntheticEvent } from "react";
import toast from "react-hot-toast";
import { AuthLayout } from "../components/auth/AuthLayout";
import { AuthCard } from "../components/auth/AuthCard";
import { TextInput } from "../components/shared/TextInput";
import { PrimaryButton } from "../components/shared/PrimaryButton";
import { register as registerRequest } from "../api/authApi";
import { parseApiError } from "../utils/parseApiError";

export function RegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const searchParams = new URLSearchParams(location.search);
  const redirectPath = searchParams.get("redirect");

  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [loginError, setLoginError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [confirmPasswordError, setConfirmPasswordError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedLogin = login.trim();
    let hasErrors = false;

    setLoginError("");
    setPasswordError("");
    setConfirmPasswordError("");

    if (!trimmedLogin) {
      setLoginError("Введите логин");
      hasErrors = true;
    } else if (trimmedLogin.length < 3 || trimmedLogin.length > 30) {
      setLoginError("Логин должен содержать от 3 до 30 символов");
      hasErrors = true;
    }

    if (!password) {
      setPasswordError("Введите пароль");
      hasErrors = true;
    } else if (password.length < 5 || password.length > 72) {
      setPasswordError("Пароль должен содержать от 5 до 72 символов");
      hasErrors = true;
    }

    if (!confirmPassword) {
      setConfirmPasswordError("Повторите пароль");
      hasErrors = true;
    } else if (password !== confirmPassword) {
      setConfirmPasswordError("Пароли не совпадают");
      hasErrors = true;
    }

    if (hasErrors) {
      return;
    }

    try {
      setIsSubmitting(true);

      await registerRequest({
        login: trimmedLogin,
        password,
      });

      toast.success("Аккаунт успешно создан");

      navigate(
        redirectPath
          ? `/login?redirect=${encodeURIComponent(redirectPath)}`
          : "/login",
        { replace: true },
      );
    } catch (error) {
      const parsedError = parseApiError(error);
      toast.error(parsedError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <AuthCard title="Регистрация">
        <form onSubmit={handleSubmit} className="space-y-5">
          <TextInput
            label="Логин"
            name="login"
            type="text"
            autoComplete="username"
            placeholder="Придумайте логин"
            value={login}
            minLength={3}
            maxLength={30}
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
            autoComplete="new-password"
            placeholder="Придумайте пароль"
            value={password}
            minLength={5}
            maxLength={72}
            disabled={isSubmitting}
            onChange={(e) => {
              setPassword(e.target.value);

              if (passwordError) {
                setPasswordError("");
              }

              if (confirmPasswordError) {
                setConfirmPasswordError("");
              }
            }}
            error={passwordError}
          />

          <TextInput
            label="Подтверждение пароля"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="Повторите пароль"
            value={confirmPassword}
            disabled={isSubmitting}
            onChange={(e) => {
              setConfirmPassword(e.target.value);

              if (confirmPasswordError) {
                setConfirmPasswordError("");
              }
            }}
            error={confirmPasswordError}
          />

          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Регистрация..." : "Зарегистрироваться"}
          </PrimaryButton>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          Уже есть аккаунт?{" "}
          <Link
            to={
              redirectPath
                ? `/login?redirect=${encodeURIComponent(redirectPath)}`
                : "/login"
            }
            className="font-medium text-indigo-600 transition hover:text-indigo-700"
          >
            Войти
          </Link>
        </p>
      </AuthCard>
    </AuthLayout>
  );
}
