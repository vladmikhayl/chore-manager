import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { PublicRoute } from "./components/routing/PublicRoute";
import { PrivateRoute } from "./components/routing/PrivateRoute";
import { TasksPage } from "./pages/TasksPage";
import { ListsPage } from "./pages/ListsPage";
import { ListDetailsPage } from "./pages/ListDetailsPage";
import { AcceptInvitePage } from "./pages/AcceptInvitePage";
import { ProfilePage } from "./pages/ProfilePage";
import { AliceLinkPage } from "./pages/AliceLinkPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/tasks" replace />} />

        <Route
          path="/login"
          element={
            <PublicRoute>
              <LoginPage />
            </PublicRoute>
          }
        />

        <Route
          path="/register"
          element={
            <PublicRoute>
              <RegisterPage />
            </PublicRoute>
          }
        />

        <Route path="/alice/link" element={<AliceLinkPage />} />

        <Route
          path="/tasks"
          element={
            <PrivateRoute>
              <TasksPage />
            </PrivateRoute>
          }
        />

        <Route
          path="/lists"
          element={
            <PrivateRoute>
              <ListsPage />
            </PrivateRoute>
          }
        />

        <Route
          path="/lists/:listId"
          element={
            <PrivateRoute>
              <ListDetailsPage />
            </PrivateRoute>
          }
        />

        <Route
          path="/invites/:token"
          element={
            <PrivateRoute>
              <AcceptInvitePage />
            </PrivateRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <PrivateRoute>
              <ProfilePage />
            </PrivateRoute>
          }
        />

        <Route path="*" element={<Navigate to="/tasks" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
