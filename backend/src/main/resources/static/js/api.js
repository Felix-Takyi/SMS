const API_ROOT = "/api/v1";
let csrf = null;

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function readCsrf() {
  const response = await fetch(`${API_ROOT}/auth/csrf`, {
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  });
  if (!response.ok) throw new ApiError("Could not start a secure session.", response.status);
  csrf = await response.json();
  return csrf;
}

export async function request(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const token = csrf || await readCsrf();
    headers.set(token.headerName, token.token);
  }

  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    method,
    headers,
    credentials: "same-origin"
  });
  if (response.status === 204) return null;

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(payload?.message || "The request could not be completed.", response.status, payload?.code);
  }
  return payload;
}

export function refreshCsrf() {
  csrf = null;
  return readCsrf();
}