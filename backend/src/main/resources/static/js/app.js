import { request } from "/js/api.js";
import { currentUser, signOut } from "/js/session.js";

const root = document.querySelector("#view-root");
const permissions = new Set();
const pages = { home: "OVERVIEW", students: "STUDENTS", academics: "ACADEMICS", attendance: "ATTENDANCE", results: "ASSESSMENTS & RESULTS" };
let studentPage = 0;
let currentView = "home";

const safe = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
}[character]));
const can = (permission) => permissions.has(permission);
const dateText = (value) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`)) : "—";
const rowsTable = (headers, rows, caption) => `<div class="table-wrap"><table><caption class="visually-hidden">${safe(caption)}</caption><thead><tr>${headers.map((header) => `<th scope="col">${safe(header)}</th>`).join("")}</tr></thead><tbody>${rows}</tbody></table></div>`;
const emptyRow = (label, columns) => `<tr><td class="table-empty" colspan="${columns}">${safe(label)}</td></tr>`;
const heading = (eyebrow, title, description, action = "") => `<div class="page-heading"><div><p class="eyebrow">${safe(eyebrow)}</p><h1>${safe(title)}</h1><p class="page-description">${safe(description)}</p></div>${action}</div>`;

function setToast(message, error = false) {
  const region = document.querySelector("#toast-region");
  region.innerHTML = `<div class="toast${error ? " toast-error" : ""}" role="status">${safe(message)}</div>`;
  window.setTimeout(() => { region.innerHTML = ""; }, 3500);
}

function errorBlock(error) {
  const denied = error.status === 403;
  return `<div class="inline-notice${denied ? "" : " notice-error"}"><strong>${denied ? "Access not granted" : "Could not load this view"}</strong><span>${denied ? "Your account does not have permission to view this information." : safe(error.message || "Check the server connection and try again.")}</span></div>`;
}

function setActivePage(view) {
  currentView = Object.hasOwn(pages, view) ? view : "home";
  document.querySelector("#page-crumb").textContent = pages[currentView];
  document.querySelectorAll("[data-nav]").forEach((link) => {
    const selected = link.dataset.nav === currentView;
    link.classList.toggle("active", selected);
    if (selected) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  document.querySelector("#sidebar").classList.remove("sidebar-open");
  document.querySelector("#menu-toggle").setAttribute("aria-expanded", "false");
}

async function loadMetric(key, path, select) {
  try {
    const data = await request(path);
    const target = document.querySelector(`[data-metric="${key}"]`);
    if (target) target.textContent = new Intl.NumberFormat().format(select(data));
  } catch {
    const target = document.querySelector(`[data-metric="${key}"]`);
    if (target) target.textContent = "—";
  }
}

async function renderHome() {
  root.innerHTML = `${heading("SCHOOL DESK", "Your school at a glance", "A live view of the records and work you can access.")}
    <section class="metric-grid" aria-label="School summary">
      <div class="metric-tile" data-permission="STUDENT_VIEW"><span>STUDENT RECORDS</span><strong data-metric="students">...</strong><small>In the student register</small></div>
      <div class="metric-tile metric-accent" data-permission="ACADEMICS_VIEW"><span>ACTIVE YEARS</span><strong data-metric="years">...</strong><small>Academic calendars in use</small></div>
      <div class="metric-tile" data-permission="ATTENDANCE_VIEW"><span>ATTENDANCE SESSIONS</span><strong data-metric="attendance">...</strong><small>Sessions recorded</small></div>
      <div class="metric-tile" data-permission="ASSESSMENT_VIEW"><span>ASSESSMENTS</span><strong data-metric="assessments">...</strong><small>Configured for classes</small></div>
    </section>
    <section class="dashboard-grid">
      <div class="section-panel"><div class="section-heading"><div><p class="eyebrow">QUICK ACCESS</p><h2>Continue your work</h2></div></div><div class="quick-links" id="quick-links"></div></div>
      <div class="section-panel day-panel"><p class="eyebrow">TODAY</p><div class="day-date" id="day-date"></div><p class="day-note">Your school workspace is ready. Choose a section to continue.</p><div class="day-divider"></div><p class="day-foot"><span class="status-dot"></span>Local server connected</p></div>
    </section>
    <section class="section-panel recent-panel" data-permission="ASSESSMENT_VIEW"><div class="section-heading"><div><p class="eyebrow">ACADEMIC WORK</p><h2>Recent assessments</h2></div><a class="text-link" href="#results">Open results <span aria-hidden="true">&#8594;</span></a></div><div id="recent-assessments" class="table-slot"><div class="loading-state compact">Loading assessment activity...</div></div></section>`;

  document.querySelector("#day-date").textContent = new Intl.DateTimeFormat(undefined, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(new Date());
  const quickLinks = [
    ["students", "Student register", "Find and maintain student records", "STUDENT_VIEW"],
    ["academics", "Academic setup", "Years, classes and subjects", "ACADEMICS_VIEW"],
    ["attendance", "Attendance", "Review class sessions", "ATTENDANCE_VIEW"],
    ["results", "Assessments", "Assessment and grading setup", "ASSESSMENT_VIEW"]
  ].filter((item) => can(item[3]));
  document.querySelector("#quick-links").innerHTML = quickLinks.map(([view, title, description], index) => `<a class="quick-link" href="#${view}"><span class="quick-index">0${index + 1}</span><span><strong>${safe(title)}</strong><small>${safe(description)}</small></span><span class="quick-arrow" aria-hidden="true">&#8599;</span></a>`).join("") || `<p class="muted">No workspace sections have been assigned to this account.</p>`;

  const metrics = [
    ["students", "/students?page=0&size=1", (data) => data.totalElements, "STUDENT_VIEW"],
    ["years", "/academic-years?page=0&size=100", (data) => data.content.filter((item) => item.active).length, "ACADEMICS_VIEW"],
    ["attendance", "/attendance-sessions?page=0&size=1", (data) => data.totalElements, "ATTENDANCE_VIEW"],
    ["assessments", "/assessments?page=0&size=1", (data) => data.totalElements, "ASSESSMENT_VIEW"]
  ];
  await Promise.all(metrics.filter(([, , , permission]) => can(permission))
    .map(([key, path, select]) => loadMetric(key, path, select)));

  if (can("ASSESSMENT_VIEW")) {
    try {
      const data = await request("/assessments?page=0&size=5");
      const rows = data.content.map((item) => `<tr><td><strong>${safe(item.assessmentType)}</strong><small class="cell-sub">${safe(item.subjectCode)}</small></td><td>${safe(item.schoolClassCode)}</td><td>${safe(item.academicYearCode)}</td><td>${dateText(item.assessmentDate)}</td></tr>`).join("") || emptyRow("No assessments have been configured yet.", 4);
      document.querySelector("#recent-assessments").innerHTML = rowsTable(["ASSESSMENT", "CLASS", "ACADEMIC YEAR", "DATE"], rows, "Recent assessments");
    } catch (error) {
      document.querySelector("#recent-assessments").innerHTML = errorBlock(error);
    }
  }
}

function studentDialog() {
  return `<dialog class="form-dialog" id="student-dialog"><form id="student-form" class="dialog-form">
    <div class="dialog-heading"><div><p class="eyebrow">STUDENT RECORD</p><h2>Add student</h2></div><button class="close-button" type="button" data-close-dialog aria-label="Close">CLOSE</button></div>
    <div class="form-grid">
      <label>Admission number<input name="admissionNumber" required maxlength="80"></label><label>First name<input name="firstName" required maxlength="120"></label>
      <label>Middle name<input name="middleName" maxlength="120"></label><label>Last name<input name="lastName" required maxlength="120"></label>
      <label>Date of birth<input name="dateOfBirth" type="date" required></label><label>Gender<input name="gender" required maxlength="30"></label>
      <label>Phone<input name="phone" type="tel" maxlength="30"></label><label>Email<input name="email" type="email" maxlength="254"></label>
      <label class="field-span">Address<input name="address" maxlength="255"></label>
    </div>
    <p class="form-error" id="student-form-error" role="alert" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create student</button></div>
  </form></dialog>`;
}

function bindCloseDialogs() {
  document.querySelectorAll("[data-close-dialog]").forEach((button) => button.addEventListener("click", () => button.closest("dialog").close()));
}

async function renderStudents(pageNumber = studentPage) {
  studentPage = pageNumber;
  const action = can("STUDENT_CREATE") ? `<button class="button button-primary" id="add-student">Add student <span aria-hidden="true">+</span></button>` : "";
  root.innerHTML = `${heading("STUDENT RECORDS", "Student register", "Search this page, review a record, or add a student.", action)}
    <section class="section-panel data-panel"><div class="table-toolbar"><label class="filter-field">FILTER THIS PAGE<input id="student-filter" type="search" placeholder="Name or admission number" autocomplete="off"></label><span class="result-count" id="student-count">Loading records...</span></div><div id="student-table" class="table-slot"><div class="loading-state compact">Loading student records...</div></div><div id="student-pagination" class="pagination"></div></section>
    ${can("STUDENT_CREATE") ? studentDialog() : ""}`;
  bindCloseDialogs();
  document.querySelector("#add-student")?.addEventListener("click", () => document.querySelector("#student-dialog").showModal());
  document.querySelector("#student-form")?.addEventListener("submit", createStudent);
  document.querySelector("#student-filter")?.addEventListener("input", (event) => {
    const query = event.target.value.trim().toLowerCase();
    document.querySelectorAll("#student-table tbody tr[data-search]").forEach((row) => { row.hidden = !row.dataset.search.includes(query); });
  });
  await loadStudents();
}

async function loadStudents() {
  const slot = document.querySelector("#student-table");
  try {
    const data = await request(`/students?page=${studentPage}&size=20`);
    const rows = data.content.map((student) => {
      const name = [student.firstName, student.middleName, student.lastName].filter(Boolean).join(" ");
      return `<tr data-search="${safe(`${name} ${student.admissionNumber}`.toLowerCase())}"><td class="mono-cell">${safe(student.admissionNumber)}</td><td><strong>${safe(name)}</strong><small class="cell-sub">${safe(student.gender || "")}</small></td><td>${dateText(student.dateOfBirth)}</td><td>${safe(student.phone || "—")}</td><td><span class="status-pill">${safe(student.status)}</span></td></tr>`;
    }).join("") || emptyRow("No student records found.", 5);
    slot.innerHTML = rowsTable(["ADMISSION NO.", "STUDENT", "DATE OF BIRTH", "PHONE", "STATUS"], rows, "Student register");
    document.querySelector("#student-count").textContent = `${new Intl.NumberFormat().format(data.totalElements)} records`;
    document.querySelector("#student-pagination").innerHTML = `<button class="button button-quiet" data-page="${studentPage - 1}" ${studentPage <= 0 ? "disabled" : ""}>Previous</button><span>Page ${studentPage + 1} of ${Math.max(data.totalPages, 1)}</span><button class="button button-quiet" data-page="${studentPage + 1}" ${studentPage + 1 >= data.totalPages ? "disabled" : ""}>Next</button>`;
    document.querySelectorAll("[data-page]").forEach((button) => button.addEventListener("click", () => renderStudents(Number(button.dataset.page))));
  } catch (error) {
    slot.innerHTML = errorBlock(error);
    document.querySelector("#student-count").textContent = "";
  }
}

async function createStudent(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const error = form.querySelector("#student-form-error");
  const fields = Object.fromEntries(new FormData(form));
  fields.status = "ACTIVE";
  for (const key of ["middleName", "phone", "email", "address"]) if (!fields[key]) fields[key] = null;
  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  error.hidden = true;
  try {
    await request("/students", { method: "POST", body: JSON.stringify(fields) });
    document.querySelector("#student-dialog").close();
    setToast("Student record created.");
    await renderStudents(0);
  } catch (requestError) {
    error.textContent = requestError.message;
    error.hidden = false;
  } finally {
    button.disabled = false;
  }
}

async function renderAcademics() {
  root.innerHTML = `${heading("ACADEMIC SETUP", "Academic catalogue", "Review the academic years, classes and subjects configured for your school.")}
    <section class="section-panel data-panel"><div class="tab-row" role="tablist" aria-label="Academic catalogue"><button class="tab-button active" data-academic-tab="years" role="tab" aria-selected="true">Academic years</button><button class="tab-button" data-academic-tab="classes" role="tab" aria-selected="false">Classes</button><button class="tab-button" data-academic-tab="subjects" role="tab" aria-selected="false">Subjects</button></div><div id="academic-table" class="table-slot"><div class="loading-state compact">Loading catalogue...</div></div></section>`;
  document.querySelectorAll("[data-academic-tab]").forEach((button) => button.addEventListener("click", () => {
    document.querySelectorAll("[data-academic-tab]").forEach((tab) => {
      const selected = tab === button;
      tab.classList.toggle("active", selected);
      tab.setAttribute("aria-selected", String(selected));
    });
    loadAcademicTable(button.dataset.academicTab);
  }));
  await loadAcademicTable("years");
}

async function loadAcademicTable(type) {
  const config = {
    years: ["/academic-years?page=0&size=100", ["CODE", "ACADEMIC YEAR", "START DATE", "END DATE", "STATUS"], (item) => `<td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td>${dateText(item.startDate)}</td><td>${dateText(item.endDate)}</td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td>`],
    classes: ["/academic-classes?page=0&size=100", ["CODE", "CLASS", "STATUS"], (item) => `<td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td>`],
    subjects: ["/subjects?page=0&size=100", ["CODE", "SUBJECT", "DESCRIPTION", "STATUS"], (item) => `<td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td>${safe(item.description || "—")}</td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td>`]
  };
  const [path, headers, renderRow] = config[type];
  const slot = document.querySelector("#academic-table");
  slot.innerHTML = `<div class="loading-state compact">Loading ${safe(type)}...</div>`;
  try {
    const data = await request(path);
    const rows = data.content.map((item) => `<tr>${renderRow(item)}</tr>`).join("") || emptyRow("No records have been configured.", headers.length);
    slot.innerHTML = rowsTable(headers, rows, `Academic ${type}`);
  } catch (error) {
    slot.innerHTML = errorBlock(error);
  }
}

async function renderAttendance() {
  const action = can("ATTENDANCE_RECORD") ? `<button class="button button-primary" id="add-session">New session <span aria-hidden="true">+</span></button>` : "";
  const dialog = can("ATTENDANCE_RECORD") ? `<dialog class="form-dialog" id="attendance-dialog"><form id="attendance-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ATTENDANCE</p><h2>Open a session</h2></div><button class="close-button" type="button" data-close-dialog aria-label="Close">CLOSE</button></div><div class="form-stack"><label>Academic year<select name="academicYearId" required></select></label><label>Class<select name="schoolClassId" required></select></label><label>Date<input type="date" name="attendanceDate" required></label><label>Session name<input name="sessionName" maxlength="80" placeholder="Morning" required></label></div><p class="form-error" id="attendance-form-error" role="alert" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create session</button></div></form></dialog>` : "";
  root.innerHTML = `${heading("DAILY OPERATIONS", "Attendance sessions", "Review sessions recorded for each class and date.", action)}<section class="section-panel data-panel"><div class="table-toolbar"><span class="result-count" id="attendance-count">Loading sessions...</span></div><div id="attendance-table" class="table-slot"><div class="loading-state compact">Loading attendance...</div></div></section>${dialog}`;
  bindCloseDialogs();
  document.querySelector("#add-session")?.addEventListener("click", openAttendanceDialog);
  document.querySelector("#attendance-form")?.addEventListener("submit", createAttendanceSession);
  try {
    const data = await request("/attendance-sessions?page=0&size=20");
    const rows = data.content.map((session) => `<tr><td>${dateText(session.attendanceDate)}</td><td><strong>${safe(session.schoolClassCode)}</strong></td><td>${safe(session.academicYearCode)}</td><td>${safe(session.sessionName)}</td></tr>`).join("") || emptyRow("No attendance sessions have been recorded.", 4);
    document.querySelector("#attendance-table").innerHTML = rowsTable(["DATE", "CLASS", "ACADEMIC YEAR", "SESSION"], rows, "Attendance sessions");
    document.querySelector("#attendance-count").textContent = `${new Intl.NumberFormat().format(data.totalElements)} sessions`;
  } catch (error) {
    document.querySelector("#attendance-table").innerHTML = errorBlock(error);
    document.querySelector("#attendance-count").textContent = "";
  }
}

async function openAttendanceDialog() {
  document.querySelector("#attendance-dialog").showModal();
  const [years, classes] = await Promise.allSettled([request("/academic-years?page=0&size=100"), request("/academic-classes?page=0&size=100")]);
  if (years.status === "fulfilled") document.querySelector('[name="academicYearId"]').innerHTML = years.value.content.map((year) => `<option value="${safe(year.id)}">${safe(year.name)}</option>`).join("");
  if (classes.status === "fulfilled") document.querySelector('[name="schoolClassId"]').innerHTML = classes.value.content.map((item) => `<option value="${safe(item.id)}">${safe(item.name)}</option>`).join("");
  document.querySelector('[name="attendanceDate"]').value = new Date().toISOString().slice(0, 10);
}

async function createAttendanceSession(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const error = form.querySelector("#attendance-form-error");
  const values = Object.fromEntries(new FormData(form));
  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  error.hidden = true;
  try {
    await request("/attendance-sessions", { method: "POST", body: JSON.stringify(values) });
    document.querySelector("#attendance-dialog").close();
    setToast("Attendance session created.");
    await renderAttendance();
  } catch (requestError) {
    error.textContent = requestError.message;
    error.hidden = false;
  } finally {
    button.disabled = false;
  }
}

async function renderResults() {
  root.innerHTML = `${heading("ASSESSMENT & RESULTS", "Assessment desk", "Review configured assessments and apply a school's grading scale to a percentage.")}
    <section class="section-panel data-panel"><div class="section-heading"><div><p class="eyebrow">ASSESSMENT REGISTER</p><h2>Configured assessments</h2></div></div><div id="assessment-table" class="table-slot"><div class="loading-state compact">Loading assessments...</div></div></section>
    <section class="section-panel grade-tool"><div class="section-heading"><div><p class="eyebrow">GRADE LOOKUP</p><h2>Apply a grading scale</h2></div><span class="tool-index">01</span></div><form id="grade-form" class="grade-form"><label>Grading scale<select name="gradingScaleId" required><option value="">Choose a scale</option></select></label><label>Percentage<input name="percentage" type="number" min="0" max="100" step="0.01" placeholder="e.g. 82.5" required></label><button class="button button-primary" type="submit">Find grade <span aria-hidden="true">&#8594;</span></button></form><div id="grade-result" class="grade-result" aria-live="polite">Choose a scale and enter a percentage.</div></section>`;
  loadAssessments();
  loadGradingScales();
  document.querySelector("#grade-form").addEventListener("submit", findGrade);
}

async function loadAssessments() {
  const slot = document.querySelector("#assessment-table");
  try {
    const data = await request("/assessments?page=0&size=20");
    const rows = data.content.map((item) => `<tr><td><strong>${safe(item.assessmentType)}</strong></td><td>${safe(item.subjectCode)}</td><td>${safe(item.schoolClassCode)}</td><td>${safe(item.academicYearCode)}</td><td>${safe(item.totalMarks)}</td><td>${dateText(item.assessmentDate)}</td></tr>`).join("") || emptyRow("No assessments configured.", 6);
    slot.innerHTML = rowsTable(["TYPE", "SUBJECT", "CLASS", "YEAR", "MAX MARKS", "DATE"], rows, "Assessment register");
  } catch (error) {
    slot.innerHTML = errorBlock(error);
  }
}

async function loadGradingScales() {
  const select = document.querySelector('[name="gradingScaleId"]');
  try {
    const data = await request("/grading-scales?page=0&size=100");
    select.innerHTML = `<option value="">Choose a scale</option>${data.content.map((scale) => `<option value="${safe(scale.id)}">${safe(scale.name)}</option>`).join("")}`;
    if (!data.content.length) document.querySelector("#grade-result").textContent = "No grading scales are configured yet.";
  } catch (error) {
    select.innerHTML = "<option value=\"\">Unavailable</option>";
    document.querySelector("#grade-result").textContent = error.message;
  }
}

async function findGrade(event) {
  event.preventDefault();
  const values = new FormData(event.currentTarget);
  const result = document.querySelector("#grade-result");
  result.className = "grade-result";
  result.textContent = "Looking up grade...";
  try {
    const band = await request(`/grading-scales/${encodeURIComponent(values.get("gradingScaleId"))}/grade?percentage=${encodeURIComponent(values.get("percentage"))}`);
    result.className = "grade-result grade-found";
    result.innerHTML = `<span class="grade-letter">${safe(band.gradeLabel)}</span><span><strong>${safe(band.minimumPercentage)}% to ${safe(band.maximumPercentage)}%</strong><small>${safe(band.remark || "Configured grade band")}</small></span>`;
  } catch (error) {
    result.className = "grade-result grade-error";
    result.textContent = error.message;
  }
}

async function render() {
  const view = window.location.hash.slice(1) || "home";
  setActivePage(view);
  root.innerHTML = `<div class="loading-state">Loading ${safe(pages[currentView].toLowerCase())}...</div>`;
  try {
    if (currentView === "home") await renderHome();
    else if (currentView === "students") await renderStudents();
    else if (currentView === "academics") await renderAcademics();
    else if (currentView === "attendance") await renderAttendance();
    else if (currentView === "results") await renderResults();
  } catch (error) {
    root.innerHTML = errorBlock(error);
  }
}

try {
  const user = await currentUser();
  if (!user) {
    window.location.replace("/login.html");
  } else {
    user.permissions.forEach((permission) => permissions.add(permission));
    document.querySelectorAll("[data-permission]").forEach((link) => {
      if (!can(link.dataset.permission)) link.remove();
    });
    document.querySelector("#user-name").textContent = user.displayName;
    document.querySelector("#user-role").textContent = user.roles.join(" / ") || "School staff";
    document.querySelector("#user-avatar").textContent = user.displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase() || "--";
    document.querySelector("#topbar-user").textContent = user.displayName;
    document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, { day: "numeric", month: "short", year: "numeric" }).format(new Date());
    document.querySelector("#sign-out").addEventListener("click", async () => {
      try { await signOut(); } catch (error) { if (error.status !== 401) setToast(error.message, true); }
      window.location.replace("/login.html");
    });
    document.querySelector("#menu-toggle").addEventListener("click", (event) => {
      const open = document.querySelector("#sidebar").classList.toggle("sidebar-open");
      event.currentTarget.setAttribute("aria-expanded", String(open));
    });
    window.addEventListener("hashchange", render);
    await render();
  }
} catch (error) {
  root.innerHTML = errorBlock(error);
}