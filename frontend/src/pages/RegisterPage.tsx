import { Link } from "react-router-dom";
import { useState, type SyntheticEvent } from "react";
import { AuthLayout } from "../components/auth/AuthLayout";
import { AuthCard } from "../components/auth/AuthCard";
import { TextInput } from "../components/shared/TextInput";
import { PrimaryButton } from "../components/shared/PrimaryButton";

export function RegisterPage() {
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    console.log("register submit", { login, password, confirmPassword });
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
            onChange={(e) => setLogin(e.target.value)}
          />

          <TextInput
            label="Пароль"
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="Придумайте пароль"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <TextInput
            label="Подтверждение пароля"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="Повторите пароль"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />

          <PrimaryButton type="submit">Зарегистрироваться</PrimaryButton>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          Уже есть аккаунт?{" "}
          <Link
            to="/login"
            className="font-medium text-indigo-600 transition hover:text-indigo-700"
          >
            Войти
          </Link>
        </p>
      </AuthCard>
    </AuthLayout>
  );
}
