import { request, refreshCsrf } from "/js/api.js";

export async function currentUser() {
  try {
    return await request("/auth/me");
  } catch (error) {
    if (error.status === 401) return null;
    throw error;
  }
}

export async function signIn(username, password) {
  const user = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
  await refreshCsrf();
  return user;
}

export async function signOut() {
  await request("/auth/logout", { method: "POST" });
  await refreshCsrf();
}