import { currentUser, signIn } from "/js/session.js";

const form = document.querySelector("#login-form");
const errorMessage = document.querySelector("#login-error");

try {
  if (await currentUser()) window.location.replace("/");
} catch {
  errorMessage.textContent = "The school server is unavailable. Try again shortly.";
  errorMessage.hidden = false;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const submit = form.querySelector("button[type=submit]");
  const values = new FormData(form);
  submit.disabled = true;
  submit.textContent = "Signing in...";
  errorMessage.hidden = true;
  try {
    await signIn(values.get("username").trim(), values.get("password"));
    window.location.replace("/");
  } catch (error) {
    errorMessage.textContent = error.status === 401
      ? "The username or password is incorrect."
      : error.message || "Sign in could not be completed.";
    errorMessage.hidden = false;
    submit.disabled = false;
    submit.innerHTML = 'Sign in <span aria-hidden="true">&#8594;</span>';
  }
});