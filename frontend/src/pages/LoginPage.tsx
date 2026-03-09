import { Link } from "react-router-dom";
import { useState, type SyntheticEvent } from "react";
import { AuthLayout } from "../components/auth/AuthLayout";
import { AuthCard } from "../components/auth/AuthCard";
import { TextInput } from "../components/shared/TextInput";
import { PrimaryButton } from "../components/shared/PrimaryButton";

export function LoginPage() {
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");

  function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    console.log("login submit", { login, password });
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
            onChange={(e) => setLogin(e.target.value)}
          />

          <TextInput
            label="Пароль"
            name="password"
            type="password"
            autoComplete="current-password"
            placeholder="Введите пароль"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <PrimaryButton type="submit">Войти</PrimaryButton>
        </form>

        <p className="mt-6 text-sm text-slate-600">
          Нет аккаунта?{" "}
          <Link
            to="/register"
            className="font-medium text-indigo-600 transition hover:text-indigo-700"
          >
            Зарегистрироваться
          </Link>
        </p>
      </AuthCard>
    </AuthLayout>
  );
}
