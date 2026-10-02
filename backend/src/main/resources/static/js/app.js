import { request } from "/js/api.js";
import { currentUser, signOut } from "/js/session.js";

const root = document.querySelector("#view-root");
const permissions = new Set();
const pages = {
  home: "OVERVIEW",
  students: "STUDENTS",
  admissions: "ADMISSIONS",
  academics: "ACADEMICS",
  attendance: "ATTENDANCE",
  results: "ASSESSMENTS & RESULTS",
  users: "USERS & ACCESS",
  account: "MY ACCOUNT"
};
let studentPage = 0;
let admissionPage = 0;
let userPage = 0;
let availableRoles = [];
let signedInUserId = null;
let signedInUser = null;
let currentView = "home";
let canGenerateResultPdfs = false;
let canEditAcademicCatalogue = false;
let academicRecords = new Map();

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
  window.setTimeout(() => { region.innerHTML = ""; }, 4200);
}

function errorBlock(error) {
  const denied = error.status === 403;
  return `<div class="inline-notice${denied ? "" : " notice-error"}"><strong>${denied ? "Access not granted" : "Could not load this view"}</strong><span>${denied ? "Your account does not have permission to use this information." : safe(error.message || "Check the server connection and try again.")}</span></div>`;
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

function bindCloseDialogs(scope = document) {
  scope.querySelectorAll("[data-close-dialog]").forEach((button) => button.addEventListener("click", () => button.closest("dialog")?.close()));
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
    ["admissions", "Admissions", "Review and update applications", "ADMISSION_REVIEW"],
    ["academics", "Academic setup", "Years, terms, classes, streams and subjects", "ACADEMICS_VIEW"],
    ["attendance", "Attendance", "Sessions and attendance records", "ATTENDANCE_VIEW"],
    ["results", "Assessments", "Tests, scores, grading and PDFs", "ASSESSMENT_VIEW"],
    ["users", "Users & access", "Create accounts and assign roles", "USER_MANAGE"],
    ["account", "My account", "Change your password", null]
  ].filter((item) => !item[3] || can(item[3]));
  document.querySelector("#quick-links").innerHTML = quickLinks.map(([view, title, description], index) => `<a class="quick-link" href="#${view}"><span class="quick-index">${String(index + 1).padStart(2, "0")}</span><span><strong>${safe(title)}</strong><small>${safe(description)}</small></span><span class="quick-arrow" aria-hidden="true">&#8599;</span></a>`).join("");

  const metrics = [
    ["students", "/students?page=0&size=1", (data) => data.totalElements, "STUDENT_VIEW"],
    ["years", "/academic-years?page=0&size=100", (data) => data.content.filter((item) => item.active).length, "ACADEMICS_VIEW"],
    ["attendance", "/attendance-sessions?page=0&size=1", (data) => data.totalElements, "ATTENDANCE_VIEW"],
    ["assessments", "/assessments?page=0&size=1", (data) => data.totalElements, "ASSESSMENT_VIEW"]
  ];
  await Promise.all(metrics.filter(([, , , permission]) => can(permission)).map(([key, path, select]) => loadMetric(key, path, select)));

  if (can("ASSESSMENT_VIEW")) {
    try {
      const data = await request("/assessments?page=0&size=5");
      const rows = data.content.map((item) => `<tr><td><strong>${safe(item.assessmentType)}</strong><small class="cell-sub">${safe(item.subjectCode)}</small></td><td>${safe(item.schoolClassCode)}</td><td>${safe(item.academicYearCode)}</td><td>${dateText(item.assessmentDate)}</td></tr>`).join("") || emptyRow("No assessments have been configured yet.", 4);
      document.querySelector("#recent-assessments").innerHTML = rowsTable(["ASSESSMENT", "CLASS", "ACADEMIC YEAR", "DATE"], rows, "Recent assessments");
    } catch (error) { document.querySelector("#recent-assessments").innerHTML = errorBlock(error); }
  }
}

function studentDialog(edit = false) {
  return `<dialog class="form-dialog" id="student-dialog"><form id="student-form" class="dialog-form">
    <input type="hidden" name="id">
    <div class="dialog-heading"><div><p class="eyebrow">STUDENT RECORD</p><h2>${edit ? "Edit student" : "Add student"}</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div>
    ${edit ? `<div class="form-grid"><label>Status<select name="status"><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="GRADUATED">Graduated</option><option value="TRANSFERRED">Transferred</option></select></label><label>Phone<input name="phone" maxlength="30"></label><label class="field-span">Address<input name="address" maxlength="255"></label></div>` : `<div class="form-grid">
      <label>Admission number<input name="admissionNumber" required maxlength="80"></label><label>First name<input name="firstName" required maxlength="120"></label>
      <label>Middle name<input name="middleName" maxlength="120"></label><label>Last name<input name="lastName" required maxlength="120"></label>
      <label>Date of birth<input name="dateOfBirth" type="date" required></label><label>Gender<select name="gender" required><option value="">Choose gender</option><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option><option value="UNKNOWN">Unknown / not stated</option></select></label>
      <label>Nationality<input name="nationality" maxlength="120"></label><label>Religion<input name="religion" maxlength="120"></label>
      <label>Phone<input name="phone" type="tel" maxlength="30"></label><label>Email<input name="email" type="email" maxlength="254"></label>
      <label class="field-span">Address<input name="address" maxlength="255"></label>
    </div>`}
    <p class="form-error" id="student-form-error" role="alert" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">${edit ? "Save changes" : "Create student"}</button></div>
  </form></dialog>`;
}

async function renderStudents(pageNumber = studentPage) {
  studentPage = pageNumber;
  const action = can("STUDENT_CREATE") ? `<button class="button button-primary" id="add-student">Add student <span aria-hidden="true">+</span></button>` : "";
  root.innerHTML = `${heading("STUDENT RECORDS", "Student register", "Search, review and maintain student records.", action)}
    <section class="section-panel data-panel"><div class="table-toolbar"><label class="filter-field">FILTER THIS PAGE<input id="student-filter" type="search" placeholder="Name or admission number" autocomplete="off"></label><span class="result-count" id="student-count">Loading records...</span></div><div id="student-table" class="table-slot"><div class="loading-state compact">Loading student records...</div></div><div id="student-pagination" class="pagination"></div></section>
    ${can("STUDENT_CREATE") ? studentDialog(false) : ""}${can("STUDENT_EDIT") ? studentDialog(true).replaceAll('id="student-dialog"', 'id="student-edit-dialog"').replaceAll('id="student-form"', 'id="student-edit-form"') : ""}`;
  bindCloseDialogs();
  document.querySelector("#add-student")?.addEventListener("click", () => document.querySelector("#student-dialog").showModal());
  document.querySelector("#student-form")?.addEventListener("submit", createStudent);
  document.querySelector("#student-edit-form")?.addEventListener("submit", updateStudent);
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
      const actions = can("STUDENT_EDIT") ? `<button class="text-button" data-edit-student="${safe(student.id)}">Edit</button>` : "";
      return `<tr data-search="${safe(`${name} ${student.admissionNumber}`.toLowerCase())}"><td class="mono-cell">${safe(student.admissionNumber)}</td><td><strong>${safe(name)}</strong><small class="cell-sub">${safe(student.gender || "")}</small></td><td>${dateText(student.dateOfBirth)}</td><td>${safe(student.phone || "—")}</td><td><span class="status-pill">${safe(student.status)}</span></td><td>${actions}</td></tr>`;
    }).join("") || emptyRow("No student records found.", 6);
    slot.innerHTML = rowsTable(["ADMISSION NO.", "STUDENT", "DATE OF BIRTH", "PHONE", "STATUS", "ACTIONS"], rows, "Student register");
    document.querySelector("#student-count").textContent = `${new Intl.NumberFormat().format(data.totalElements)} records`;
    document.querySelector("#student-pagination").innerHTML = `<button class="button button-quiet" data-page="${studentPage - 1}" ${studentPage <= 0 ? "disabled" : ""}>Previous</button><span>Page ${studentPage + 1} of ${Math.max(data.totalPages, 1)}</span><button class="button button-quiet" data-page="${studentPage + 1}" ${studentPage + 1 >= data.totalPages ? "disabled" : ""}>Next</button>`;
    document.querySelectorAll("[data-page]").forEach((button) => button.addEventListener("click", () => renderStudents(Number(button.dataset.page))));
    document.querySelectorAll("[data-edit-student]").forEach((button) => button.addEventListener("click", async () => {
      try {
        const student = await request(`/students/${button.dataset.editStudent}`);
        const form = document.querySelector("#student-edit-form");
        form.elements.id.value = student.id; form.elements.status.value = student.status || "ACTIVE"; form.elements.phone.value = student.phone || ""; form.elements.address.value = student.address || "";
        document.querySelector("#student-edit-dialog").showModal();
      } catch (error) { setToast(error.message, true); }
    }));
  } catch (error) { slot.innerHTML = errorBlock(error); document.querySelector("#student-count").textContent = ""; }
}

async function createStudent(event) {
  event.preventDefault();
  const form = event.currentTarget; const error = form.querySelector("#student-form-error");
  const fields = Object.fromEntries(new FormData(form)); fields.status = "ACTIVE";
  for (const key of ["middleName", "nationality", "religion", "phone", "email", "address"]) if (!fields[key]) fields[key] = null;
  const button = form.querySelector("button[type=submit]"); button.disabled = true; error.hidden = true;
  try { await request("/students", { method: "POST", body: JSON.stringify(fields) }); document.querySelector("#student-dialog").close(); form.reset(); setToast("Student record created."); await renderStudents(0); }
  catch (requestError) { error.textContent = requestError.message; error.hidden = false; } finally { button.disabled = false; }
}

async function updateStudent(event) {
  event.preventDefault(); const form = event.currentTarget; const error = form.querySelector("#student-form-error"); const data = new FormData(form);
  const payload = { status: data.get("status") || null, phone: data.get("phone") || null, address: data.get("address") || null };
  const button = form.querySelector("button[type=submit]"); button.disabled = true; error.hidden = true;
  try { await request(`/students/${encodeURIComponent(data.get("id"))}`, { method: "PUT", body: JSON.stringify(payload) }); document.querySelector("#student-edit-dialog").close(); setToast("Student record updated."); await loadStudents(); }
  catch (requestError) { error.textContent = requestError.message; error.hidden = false; } finally { button.disabled = false; }
}

async function renderAdmissions(pageNumber = admissionPage) {
  admissionPage = pageNumber;
  if (!can("ADMISSION_REVIEW")) { root.innerHTML = errorBlock({ status: 403 }); return; }
  root.innerHTML = `${heading("ADMISSIONS", "Admission applications", "Create applications and review their status.", `<button class="button button-primary" id="add-admission">New application +</button>`)}
    <section class="section-panel data-panel"><div id="admission-table" class="table-slot"><div class="loading-state compact">Loading applications...</div></div><div id="admission-pagination" class="pagination"></div></section>
    <dialog class="form-dialog" id="admission-dialog"><form id="admission-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ADMISSION</p><h2>New application</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid">
      <label>Application no.<input name="applicationNumber" required maxlength="80"></label><label>First name<input name="firstName" required maxlength="120"></label><label>Middle name<input name="middleName" maxlength="120"></label><label>Last name<input name="lastName" required maxlength="120"></label><label>Date of birth<input type="date" name="dateOfBirth" required></label><label>Gender<select name="gender" required><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></label><label>Nationality<input name="nationality" maxlength="120"></label><label>Phone<input name="phone" maxlength="30"></label><label>Email<input name="email" type="email" maxlength="254"></label><label class="field-span">Address<input name="address" maxlength="255"></label>
    </div><p class="form-error" id="admission-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create application</button></div></form></dialog>`;
  bindCloseDialogs(); document.querySelector("#add-admission").addEventListener("click", () => document.querySelector("#admission-dialog").showModal()); document.querySelector("#admission-form").addEventListener("submit", createAdmission);
  await loadAdmissions();
}

async function loadAdmissions() {
  const slot = document.querySelector("#admission-table");
  try {
    const data = await request(`/admissions?page=${admissionPage}&size=20`);
    const rows = data.content.map((item) => {
      const name = [item.firstName, item.middleName, item.lastName].filter(Boolean).join(" ");
      const actions = can("ADMISSION_APPROVE") ? `<button class="text-button" data-admission-status="${item.id}" data-status="APPROVED">Approve</button> <button class="text-button danger-link" data-admission-status="${item.id}" data-status="REJECTED">Reject</button>` : "";
      return `<tr><td class="mono-cell">${safe(item.applicationNumber)}</td><td><strong>${safe(name)}</strong></td><td>${dateText(item.dateOfBirth)}</td><td>${safe(item.gender)}</td><td><span class="status-pill">${safe(item.status)}</span></td><td>${actions}</td></tr>`;
    }).join("") || emptyRow("No admission applications found.", 6);
    slot.innerHTML = rowsTable(["APPLICATION", "APPLICANT", "DOB", "GENDER", "STATUS", "ACTIONS"], rows, "Admission applications");
    document.querySelector("#admission-pagination").innerHTML = `<button class="button button-quiet" data-adm-page="${admissionPage - 1}" ${admissionPage <= 0 ? "disabled" : ""}>Previous</button><span>Page ${admissionPage + 1} of ${Math.max(data.totalPages, 1)}</span><button class="button button-quiet" data-adm-page="${admissionPage + 1}" ${admissionPage + 1 >= data.totalPages ? "disabled" : ""}>Next</button>`;
    document.querySelectorAll("[data-adm-page]").forEach((b) => b.addEventListener("click", () => renderAdmissions(Number(b.dataset.admPage))));
    document.querySelectorAll("[data-admission-status]").forEach((b) => b.addEventListener("click", async () => { try { await request(`/admissions/${b.dataset.admissionStatus}/status`, { method: "PUT", body: JSON.stringify(b.dataset.status) }); setToast(`Application ${b.dataset.status.toLowerCase()}.`); await loadAdmissions(); } catch (error) { setToast(error.message, true); } }));
  } catch (error) { slot.innerHTML = errorBlock(error); }
}

async function createAdmission(event) {
  event.preventDefault(); const form = event.currentTarget; const error = document.querySelector("#admission-error"); const payload = Object.fromEntries(new FormData(form)); payload.status = "PENDING";
  for (const key of ["middleName", "nationality", "phone", "email", "address"]) if (!payload[key]) payload[key] = null;
  try { await request("/admissions", { method: "POST", body: JSON.stringify(payload) }); document.querySelector("#admission-dialog").close(); form.reset(); setToast("Admission application created."); await loadAdmissions(); } catch (e) { error.textContent = e.message; error.hidden = false; }
}

function academicCreateDialog() {
  return `<dialog class="form-dialog" id="academic-dialog"><form id="academic-form" class="dialog-form"><input type="hidden" name="kind"><div class="dialog-heading"><div><p class="eyebrow">ACADEMIC SETUP</p><h2 id="academic-dialog-title">Create record</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div id="academic-form-fields" class="form-grid"></div><p class="form-error" id="academic-form-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save</button></div></form></dialog>`;
}

async function renderAcademics() {
  const tabs = ["years", "terms", "classes", "streams", "subjects", "enrollments", "teacher-assignments", "timetable"];
  const canManageTimetable = can("TIMETABLE_MANAGE");
  const canAddAcademicRecords = can("ACADEMICS_MANAGE") || canManageTimetable;
  root.innerHTML = `${heading("ACADEMIC SETUP", "Academic catalogue", "Manage years, terms, classes, streams, subjects and student enrolments.")}
    <section class="section-panel data-panel"><div class="tab-row" role="tablist" aria-label="Academic catalogue">${tabs.map((t, i) => `<button class="tab-button ${i === 0 ? "active" : ""}" data-academic-tab="${t}" role="tab" aria-selected="${i === 0}">${t === "teacher-assignments" ? "Teacher assignments" : t.charAt(0).toUpperCase() + t.slice(1)}</button>`).join("")}</div><div class="table-toolbar"><span id="academic-context" class="result-count"></span>${canAddAcademicRecords ? `<button class="button button-primary" id="academic-add">Add record +</button>` : ""}</div><div id="academic-table" class="table-slot"><div class="loading-state compact">Loading catalogue...</div></div></section>${can("ACADEMICS_MANAGE") ? academicCreateDialog() : ""}${canManageTimetable ? teacherAssignmentDialog()+timetableEntryDialog() : ""}`;
  bindCloseDialogs();
  let currentTab = "years";
  const selectTab = async (type) => { currentTab = type; document.querySelectorAll("[data-academic-tab]").forEach((tab) => { const selected = tab.dataset.academicTab === type; tab.classList.toggle("active", selected); tab.setAttribute("aria-selected", String(selected)); }); const add = document.querySelector("#academic-add"); if (add) { add.dataset.kind = type; add.textContent = type === "teacher-assignments" ? "Assign teacher +" : type === "timetable" ? "Add timetable entry +" : "Add record +"; add.hidden = type === "teacher-assignments" || type === "timetable" ? !canManageTimetable : !can("ACADEMICS_MANAGE"); } await loadAcademicTable(type); };
  document.querySelectorAll("[data-academic-tab]").forEach((button) => button.addEventListener("click", () => selectTab(button.dataset.academicTab)));
  document.querySelector("#academic-add")?.addEventListener("click", () => { if (currentTab === "teacher-assignments") openTeacherAssignmentDialog(); else if (currentTab === "timetable") openTimetableEntryDialog(); else openAcademicDialog(currentTab); });
  document.querySelector("#academic-form")?.addEventListener("submit", submitAcademicRecord);
  document.querySelector("#teacher-assignment-form")?.addEventListener("submit", submitTeacherAssignment);
  document.querySelector("#timetable-entry-form")?.addEventListener("submit", submitTimetableEntry);
  await selectTab("years");
}

async function loadAcademicTable(type) {
  const slot = document.querySelector("#academic-table"); slot.innerHTML = `<div class="loading-state compact">Loading ${safe(type)}...</div>`;
  try {
    if (type === "teacher-assignments") { await loadTeacherAssignmentsTable(slot); return; }
    if (type === "timetable") { await loadTimetableTable(slot); return; }
    if (type === "years") {
      const data = await request("/academic-years?page=0&size=100");
      academicRecords = new Map(data.content.map((item) => [item.id, item]));
      const rows = data.content.map((item) => `<tr><td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td>${dateText(item.startDate)}</td><td>${dateText(item.endDate)}</td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td><td>${editAcademicButton("years", item)}</td></tr>`).join("") || emptyRow("No academic years configured.", 6);
      slot.innerHTML = rowsTable(["CODE", "ACADEMIC YEAR", "START", "END", "STATUS", "ACTIONS"], rows, "Academic years");
    } else if (type === "terms") {
      const years = await request("/academic-years?page=0&size=100");
      const all = []; for (const year of years.content) { try { const terms = await request(`/academic-years/${encodeURIComponent(year.code)}/terms`); all.push(...terms); } catch {} }
      academicRecords = new Map(all.map((item) => [item.id, item]));
      const rows = all.map((item) => `<tr><td class="mono-cell">${safe(item.academicYearCode)}</td><td><strong>${safe(item.name)}</strong></td><td>${dateText(item.startDate)}</td><td>${dateText(item.endDate)}</td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td><td>${editAcademicButton("terms", item)}</td></tr>`).join("") || emptyRow("No terms configured.", 6);
      slot.innerHTML = rowsTable(["YEAR", "TERM", "START", "END", "STATUS", "ACTIONS"], rows, "Terms");
    } else if (type === "classes") {
      const data = await request("/academic-classes?page=0&size=100");
      academicRecords = new Map(data.content.map((item) => [item.id, item]));
      const rows = data.content.map((item) => `<tr><td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td><td>${editAcademicButton("classes", item)}</td></tr>`).join("") || emptyRow("No classes configured.", 4);
      slot.innerHTML = rowsTable(["CODE", "CLASS", "STATUS", "ACTIONS"], rows, "Classes");
    } else if (type === "streams") {
      const classes = await request("/academic-classes?page=0&size=100"); const streams = [];
      for (const c of classes.content) { try { const page = await request(`/academic-classes/${c.id}/streams?page=0&size=100`); streams.push(...page.content); } catch {} }
      academicRecords = new Map(streams.map((item) => [item.id, item]));
      const rows = streams.map((item) => `<tr><td class="mono-cell">${safe(item.schoolClassCode)}</td><td><strong>${safe(item.name)}</strong></td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td><td>${editAcademicButton("streams", item)}</td></tr>`).join("") || emptyRow("No class streams configured.", 4);
      slot.innerHTML = rowsTable(["CLASS", "STREAM", "STATUS", "ACTIONS"], rows, "Class streams");
    } else if (type === "subjects") {
      const data = await request("/subjects?page=0&size=100");
      academicRecords = new Map(data.content.map((item) => [item.id, item]));
      const rows = data.content.map((item) => `<tr><td class="mono-cell">${safe(item.code)}</td><td><strong>${safe(item.name)}</strong></td><td>${safe(item.description || "—")}</td><td><span class="status-pill ${item.active ? "status-active" : ""}">${item.active ? "Active" : "Inactive"}</span></td><td>${editAcademicButton("subjects", item)}${can("ACADEMICS_MANAGE") ? ` <button class="text-button danger-link" data-delete-subject="${item.id}" data-name="${safe(item.name)}">Remove</button>` : ""}</td></tr>`).join("") || emptyRow("No subjects configured.", 5);
      slot.innerHTML = rowsTable(["CODE", "SUBJECT", "DESCRIPTION", "STATUS", "ACTIONS"], rows, "Subjects");
      document.querySelectorAll("[data-delete-subject]").forEach((button) => button.addEventListener("click", async () => { if (!confirm(`Remove ${button.dataset.name}? This cannot be undone.`)) return; try { await request(`/subjects/${button.dataset.deleteSubject}`, { method: "DELETE" }); setToast("Subject removed."); await loadAcademicTable("subjects"); } catch (error) { setToast(error.message, true); } }));
    } else if (type === "enrollments") {
      const data = await request("/student-enrollments?page=0&size=100");
      academicRecords = new Map(data.content.map((item) => [item.id, item]));
      const rows = data.content.map((item) => `<tr><td class="mono-cell">${safe(item.studentAdmissionNumber)}</td><td>${safe(item.academicYearCode)}</td><td><strong>${safe(item.schoolClassCode)}</strong></td><td>${safe(item.classStreamName || "—")}</td><td><span class="status-pill">${safe(item.status)}</span></td><td>${dateText(item.enrollmentDate)}</td><td>${editAcademicButton("enrollments", item)}</td></tr>`).join("") || emptyRow("No enrollments configured.", 7);
      slot.innerHTML = rowsTable(["STUDENT", "YEAR", "CLASS", "STREAM", "STATUS", "DATE", "ACTIONS"], rows, "Student enrollments");
    }
    document.querySelectorAll("[data-edit-academic]").forEach((button) => button.addEventListener("click", () => openAcademicDialog(button.dataset.kind, academicRecords.get(button.dataset.id))));
  } catch (error) { slot.innerHTML = errorBlock(error); }
}

function editAcademicButton(kind, item) { return canEditAcademicCatalogue ? `<button class="text-button" type="button" data-edit-academic data-kind="${kind}" data-id="${item.id}">Edit</button>` : ""; }

function teacherAssignmentDialog() { return `<dialog class="form-dialog" id="teacher-assignment-dialog"><form id="teacher-assignment-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">TEACHER ASSIGNMENT</p><h2>Assign teacher</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label>Academic year<select name="academicYearId" required></select></label><label>Class<select name="schoolClassId" required></select></label><label>Subject<select name="subjectId" required></select></label><label>Teacher<select name="teacherUserId" required></select></label></div><p class="form-error" id="teacher-assignment-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save assignment</button></div></form></dialog>`; }

function timetableEntryDialog() { return `<dialog class="form-dialog" id="timetable-entry-dialog"><form id="timetable-entry-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">TIMETABLE</p><h2>Add timetable entry</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label class="field-span">Assigned class and subject<select name="assignmentId" required></select></label><label>Day<select name="dayOfWeek" required><option value="MONDAY">Monday</option><option value="TUESDAY">Tuesday</option><option value="WEDNESDAY">Wednesday</option><option value="THURSDAY">Thursday</option><option value="FRIDAY">Friday</option><option value="SATURDAY">Saturday</option><option value="SUNDAY">Sunday</option></select></label><label>Room<input name="room" maxlength="80" required></label><label>Starts<input name="startTime" type="time" required></label><label>Ends<input name="endTime" type="time" required></label></div><p class="form-error" id="timetable-entry-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save timetable entry</button></div></form></dialog>`; }

async function openTeacherAssignmentDialog() {
  const [years, classes, subjects, teachers] = await Promise.all([
    request("/academic-years?page=0&size=100"), request("/academic-classes?page=0&size=100"),
    request("/subjects?page=0&size=100"), request("/teachers/available")
  ]);
  const form = document.querySelector("#teacher-assignment-form");
  form.elements.academicYearId.innerHTML = years.content.map(y => `<option value="${safe(y.id)}">${safe(y.name)}</option>`).join("");
  form.elements.schoolClassId.innerHTML = classes.content.filter(c => c.active).map(c => `<option value="${safe(c.id)}">${safe(c.name)}</option>`).join("");
  form.elements.subjectId.innerHTML = subjects.content.filter(s => s.active).map(s => `<option value="${safe(s.id)}">${safe(s.name)}</option>`).join("");
  form.elements.teacherUserId.innerHTML = teachers.map(t => `<option value="${safe(t.id)}">${safe(t.displayName)} (${safe(t.username)})</option>`).join("");
  if (!teachers.length) { setToast("Create or enable a user with the TEACHER role before assigning classes.", true); return; }
  document.querySelector("#teacher-assignment-dialog").showModal();
}

async function submitTeacherAssignment(event) {
  event.preventDefault(); const form = event.currentTarget; const error = document.querySelector("#teacher-assignment-error");
  try { await request("/teacher-subject-assignments", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(form))) }); document.querySelector("#teacher-assignment-dialog").close(); form.reset(); setToast("Teacher assigned to the class and subject."); await loadAcademicTable("teacher-assignments"); }
  catch (e) { error.textContent = e.message; error.hidden = false; }
}

async function loadTeacherAssignmentsTable(slot) {
  const assignments = await request("/teacher-subject-assignments");
  const rows = assignments.map(a => `<tr><td>${safe(a.academicYearCode)}</td><td><strong>${safe(a.schoolClassName)}</strong></td><td>${safe(a.subjectName)}</td><td>${safe(a.teacherName)}</td><td>${can("TIMETABLE_MANAGE") ? `<button class="text-button danger-link" data-remove-assignment="${safe(a.id)}">Remove</button>` : ""}</td></tr>`).join("") || emptyRow("No teacher assignments configured.", 5);
  slot.innerHTML = rowsTable(["ACADEMIC YEAR", "CLASS", "SUBJECT", "TEACHER", "ACTIONS"], rows, "Teacher class and subject assignments");
  slot.querySelectorAll("[data-remove-assignment]").forEach(button => button.addEventListener("click", async () => { if (!confirm("Remove this teacher assignment?")) return; try { await request(`/teacher-subject-assignments/${button.dataset.removeAssignment}`, { method: "DELETE" }); setToast("Teacher assignment removed."); await loadAcademicTable("teacher-assignments"); } catch (e) { setToast(e.message, true); } }));
}

async function openTimetableEntryDialog() {
  const assignments = await request("/teacher-subject-assignments");
  if (!assignments.length) { setToast("Assign teachers to classes and subjects before creating the timetable.", true); return; }
  const form = document.querySelector("#timetable-entry-form");
  form.elements.assignmentId.innerHTML = assignments.map(a => `<option value="${safe(a.id)}">${safe(a.schoolClassName)} · ${safe(a.subjectName)} · ${safe(a.teacherName)} · ${safe(a.academicYearCode)}</option>`).join("");
  document.querySelector("#timetable-entry-dialog").showModal();
}

async function submitTimetableEntry(event) {
  event.preventDefault(); const form = event.currentTarget; const error = document.querySelector("#timetable-entry-error");
  try { await request("/timetable-entries", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(form))) }); document.querySelector("#timetable-entry-dialog").close(); form.reset(); setToast("Timetable entry created."); await loadAcademicTable("timetable"); }
  catch (e) { error.textContent = e.message; error.hidden = false; }
}

async function loadTimetableTable(slot) {
  const [years, classes] = await Promise.all([request("/academic-years?page=0&size=100"), request("/academic-classes?page=0&size=100")]);
  slot.innerHTML = `<div class="table-toolbar"><label>Academic year<select id="timetable-year-filter">${years.content.map(y => `<option value="${safe(y.id)}">${safe(y.name)}</option>`).join("")}</select></label><label>Class<select id="timetable-class-filter"><option value="">All classes</option>${classes.content.map(c => `<option value="${safe(c.id)}">${safe(c.name)}</option>`).join("")}</select></label><button class="button button-quiet" type="button" id="print-timetable">Print PDF</button></div><div id="timetable-table" class="table-slot"></div>`;
  const yearFilter = slot.querySelector("#timetable-year-filter"); const classFilter = slot.querySelector("#timetable-class-filter");
  const activeYear = years.content.find(y => y.active); if (activeYear) yearFilter.value = activeYear.id;
  const refresh = async () => {
    if (!yearFilter.value) { slot.querySelector("#timetable-table").innerHTML = `<div class="inline-notice"><span>No academic years configured.</span></div>`; return; }
    const params = new URLSearchParams({ academicYearId: yearFilter.value }); if (classFilter.value) params.set("schoolClassId", classFilter.value);
    const entries = await request(`/timetable-entries?${params}`);
    const rows = entries.map(item => `<tr><td>${safe(item.dayOfWeek)}</td><td>${safe(item.startTime)}–${safe(item.endTime)}</td><td><strong>${safe(item.schoolClassName)}</strong></td><td>${safe(item.subjectName)}</td><td>${safe(item.teacherName)}</td><td>${safe(item.room)}</td><td>${can("TIMETABLE_MANAGE") ? `<button class="text-button danger-link" data-remove-timetable="${safe(item.id)}">Remove</button>` : ""}</td></tr>`).join("") || emptyRow("No timetable entries for this selection.", 7);
    slot.querySelector("#timetable-table").innerHTML = rowsTable(["DAY", "TIME", "CLASS", "SUBJECT", "TEACHER", "ROOM", "ACTIONS"], rows, "School timetable");
    slot.querySelectorAll("[data-remove-timetable]").forEach(button => button.addEventListener("click", async () => { if (!confirm("Remove this timetable entry?")) return; try { await request(`/timetable-entries/${button.dataset.removeTimetable}`, { method: "DELETE" }); setToast("Timetable entry removed."); await refresh(); } catch (e) { setToast(e.message, true); } }));
    slot.querySelector("#print-timetable").onclick = () => { const pdfParams = new URLSearchParams({ academicYearId: yearFilter.value, inline: "true" }); if (classFilter.value) pdfParams.set("schoolClassId", classFilter.value); window.open(`/api/v1/timetable.pdf?${pdfParams}`, "_blank", "noopener,noreferrer"); };
  };
  yearFilter.addEventListener("change", refresh); classFilter.addEventListener("change", refresh); await refresh();
}

async function openAcademicDialog(kind, record = null) {
  const dialog = document.querySelector("#academic-dialog"); const form = document.querySelector("#academic-form"); form.reset(); form.dataset.recordId = record?.id || ""; form.elements.kind.value = kind; const fields = document.querySelector("#academic-form-fields"); const title = document.querySelector("#academic-dialog-title");
  if (kind === "years") { title.textContent = `${record ? "Edit" : "Add"} academic year`; fields.innerHTML = `<label>Code<input name="code" required maxlength="40" placeholder="2026-2027"></label><label>Name<input name="name" required maxlength="150"></label><label>Start date<input type="date" name="startDate" required></label><label>End date<input type="date" name="endDate" required></label><label><span>Active</span><select name="active"><option value="true">Yes</option><option value="false">No</option></select></label>`; }
  if (kind === "terms") { const years = await request("/academic-years?page=0&size=100"); title.textContent = `${record ? "Edit" : "Add"} term`; fields.innerHTML = `<label>Academic year<select name="academicYearCode" required>${years.content.map(y => `<option value="${safe(y.code)}">${safe(y.name)}</option>`).join("")}</select></label><label>Term name<input name="name" required maxlength="80" placeholder="Term 1"></label><label>Start date<input type="date" name="startDate" required></label><label>End date<input type="date" name="endDate" required></label>${record ? `<label>Active<select name="active"><option value="true">Yes</option><option value="false">No</option></select></label>` : ""}`; }
  if (kind === "classes") { title.textContent = `${record ? "Edit" : "Add"} class`; fields.innerHTML = `<label>Code<input name="code" required maxlength="40" placeholder="BASIC6"></label><label>Name<input name="name" required maxlength="150" placeholder="Basic 6"></label><label>Active<select name="active"><option value="true">Yes</option><option value="false">No</option></select></label>`; }
  if (kind === "streams") { const classes = await request("/academic-classes?page=0&size=100"); title.textContent = `${record ? "Edit" : "Add"} class stream`; fields.innerHTML = `<label>Class<select name="schoolClassId" required>${classes.content.map(c => `<option value="${c.id}">${safe(c.name)}</option>`).join("")}</select></label><label>Stream name<input name="name" required maxlength="100" placeholder="A"></label><label>Active<select name="active"><option value="true">Yes</option><option value="false">No</option></select></label>`; }
  if (kind === "subjects") { title.textContent = `${record ? "Edit" : "Add"} subject`; fields.innerHTML = `<label>Code<input name="code" required maxlength="40" placeholder="MATH"></label><label>Name<input name="name" required maxlength="150" placeholder="Mathematics"></label><label class="field-span">Description<input name="description" maxlength="500"></label><label>Active<select name="active"><option value="true">Yes</option><option value="false">No</option></select></label>`; }
  if (kind === "enrollments") { const [students, years, classes] = await Promise.all([request("/students?page=0&size=500"), request("/academic-years?page=0&size=100"), request("/academic-classes?page=0&size=100")]); title.textContent = `${record ? "Edit" : "Enroll"} student`; fields.innerHTML = `<label>Student<select name="studentId" required>${students.content.map(s => `<option value="${s.id}">${safe(s.admissionNumber)} — ${safe([s.firstName,s.lastName].join(" "))}</option>`).join("")}</select></label><label>Academic year<select name="academicYearId" required>${years.content.map(y => `<option value="${y.id}">${safe(y.name)}</option>`).join("")}</select></label><label>Class<select name="schoolClassId" required>${classes.content.map(c => `<option value="${c.id}">${safe(c.name)}</option>`).join("")}</select></label><label>Stream<select name="classStreamId"><option value="">No stream</option></select></label><label>Status<select name="status"><option value="ACTIVE">Active</option><option value="TRANSFERRED">Transferred</option><option value="GRADUATED">Graduated</option></select></label>`; const classSelect = fields.querySelector('[name="schoolClassId"]'); const streamSelect = fields.querySelector('[name="classStreamId"]'); const loadStreams = async () => { const data = await request(`/academic-classes/${classSelect.value}/streams?page=0&size=100`); streamSelect.innerHTML = `<option value="">No stream</option>${data.content.map(s => `<option value="${s.id}">${safe(s.name)}</option>`).join("")}`; }; classSelect.addEventListener("change", loadStreams); await loadStreams(); }
  if (record) { for (const [name, value] of Object.entries(record)) { const control = form.elements.namedItem(name); if (control) control.value = value ?? ""; } }
  if (kind === "enrollments" && record) { const classSelect = fields.querySelector('[name="schoolClassId"]'); const streamSelect = fields.querySelector('[name="classStreamId"]'); const data = await request(`/academic-classes/${classSelect.value}/streams?page=0&size=100`); streamSelect.innerHTML = `<option value="">No stream</option>${data.content.map(s => `<option value="${s.id}">${safe(s.name)}</option>`).join("")}`; streamSelect.value = record.classStreamId || ""; }
  dialog.showModal();
}

async function submitAcademicRecord(event) {
  event.preventDefault(); const form = event.currentTarget; const error = document.querySelector("#academic-form-error"); const data = Object.fromEntries(new FormData(form)); const kind = data.kind; delete data.kind;
  for (const key of ["active"]) if (key in data) data[key] = data[key] === "true";
  if (data.classStreamId === "") data.classStreamId = null;
  const endpoints = { years: "/academic-years", terms: "/terms", classes: "/academic-classes", streams: "/class-streams", subjects: "/subjects", enrollments: "/student-enrollments" };
  const recordId = form.dataset.recordId;
  const endpoint = recordId ? `${endpoints[kind]}/${encodeURIComponent(recordId)}` : endpoints[kind];
  try { await request(endpoint, { method: recordId ? "PUT" : "POST", body: JSON.stringify(data) }); document.querySelector("#academic-dialog").close(); setToast(recordId ? "Academic record updated." : "Academic record created."); await loadAcademicTable(kind); } catch (e) { error.textContent = e.message; error.hidden = false; }
}

function userDialog() {
  return `<dialog class="form-dialog user-dialog" id="user-dialog"><form id="user-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ACCESS CONTROL</p><h2>Create user account</h2><p class="dialog-note">Create a separate sign-in and assign one or more backend roles.</p></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label>Username<input name="username" required maxlength="120" pattern="[A-Za-z0-9._-]+"></label><label>Display name<input name="displayName" required maxlength="180"></label><label>Email <span class="optional-label">optional</span><input name="email" type="email" maxlength="254"></label><label>Initial password<input name="password" type="password" required minlength="12" maxlength="72"></label></div><fieldset class="role-picker"><legend>Roles</legend><p>Select at least one role.</p><div class="role-option-grid" id="create-role-options"></div></fieldset><p class="form-error" id="user-form-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create account</button></div></form></dialog>`;
}
function roleEditorDialog() { return `<dialog class="form-dialog user-dialog" id="role-dialog"><form id="role-form" class="dialog-form"><input type="hidden" name="userId"><div class="dialog-heading"><div><p class="eyebrow">ROLE ASSIGNMENT</p><h2>Edit user roles</h2><p class="dialog-note" id="role-user-label"></p></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><fieldset class="role-picker"><legend>Assigned roles</legend><div class="role-option-grid" id="edit-role-options"></div></fieldset><p class="form-error" id="role-form-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save roles</button></div></form></dialog>`; }
function roleOptions(selected = []) { return availableRoles.length ? availableRoles.map((role) => `<label class="role-option"><input type="checkbox" name="roleCodes" value="${safe(role.code)}" ${selected.includes(role.code) ? "checked" : ""}><span><strong>${safe(role.name)}</strong><small>${safe(role.description || role.code)}</small></span></label>`).join("") : `<p class="muted">No roles are available to assign.</p>`; }
async function loadAvailableRoles() { availableRoles = await request("/roles"); }

async function renderUsers(pageNumber = userPage) {
  userPage = pageNumber; if (!can("USER_MANAGE")) { root.innerHTML = errorBlock({status:403}); return; }
  await loadAvailableRoles();
  const permissionButton = can("PERMISSION_ASSIGN") ? `<button class="button button-quiet" id="manage-role-permissions">Role permissions</button>` : "";
  root.innerHTML = `${heading("IDENTITY & ACCESS", "Users & access", "Create staff sign-ins, assign roles and control account access.", `<div class="action-cluster">${permissionButton}<button class="button button-primary" id="add-user">Create user +</button></div>`)}
    <section class="section-panel data-panel"><div class="table-toolbar"><label class="filter-field">FILTER USERS<input id="user-filter" type="search" placeholder="Username or name"></label><span class="result-count" id="user-count">Loading accounts...</span></div><div id="user-table" class="table-slot"><div class="loading-state compact">Loading user accounts...</div></div><div id="user-pagination" class="pagination"></div></section>${userDialog()}${roleEditorDialog()}
    ${can("PERMISSION_ASSIGN") ? `<dialog class="form-dialog" id="permission-dialog"><form id="permission-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ROLE PERMISSIONS</p><h2>Manage role permissions</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><label>Role<select id="permission-role" required></select></label><div class="role-option-grid" id="permission-options" style="margin-top:14px"></div><p class="form-error" id="permission-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save permissions</button></div></form></dialog>` : ""}`;
  bindCloseDialogs(); document.querySelector("#add-user").addEventListener("click", () => { document.querySelector("#create-role-options").innerHTML = roleOptions(); document.querySelector("#user-dialog").showModal(); }); document.querySelector("#user-form").addEventListener("submit", createUser); document.querySelector("#role-form").addEventListener("submit", saveUserRoles); document.querySelector("#user-filter").addEventListener("input", filterUserRows);
  if (can("PERMISSION_ASSIGN")) { document.querySelector("#manage-role-permissions").addEventListener("click", openPermissionEditor); document.querySelector("#permission-form").addEventListener("submit", saveRolePermissions); }
  await loadUsers();
}

function filterUserRows(event) { const q = event.target.value.trim().toLowerCase(); document.querySelectorAll("#user-table tbody tr[data-search]").forEach((row) => row.hidden = !row.dataset.search.includes(q)); }
async function loadUsers() {
  const slot = document.querySelector("#user-table");
  try {
    const data = await request(`/users?page=${userPage}&size=20`);
    const rows = data.content.map((user) => { const self = user.id === signedInUserId; const roleCodes = user.roles.join(","); return `<tr data-search="${safe(`${user.username} ${user.displayName} ${user.roles.join(" ")}`.toLowerCase())}"><td><strong>${safe(user.displayName)}</strong><small class="cell-sub mono-cell">${safe(user.username)}</small></td><td>${safe(user.email || "—")}</td><td>${user.roles.map(r => `<span class="status-pill">${safe(r)}</span>`).join(" ") || "—"}</td><td><span class="status-pill ${user.enabled ? "status-active" : ""}">${user.enabled ? "Active" : "Disabled"}</span></td><td><button class="text-button" data-edit-roles="${user.id}" data-name="${safe(user.displayName)}" data-roles="${safe(roleCodes)}" ${self ? "disabled title=\"Use another admin to change your own roles\"" : ""}>Roles</button> ${self ? "" : `<button class="text-button ${user.enabled ? "danger-link" : ""}" data-toggle-user="${user.id}" data-enabled="${user.enabled}">${user.enabled ? "Disable" : "Enable"}</button>`}</td></tr>`; }).join("") || emptyRow("No user accounts found.", 5);
    slot.innerHTML = rowsTable(["USER", "EMAIL", "ROLES", "STATUS", "ACTIONS"], rows, "User accounts"); document.querySelector("#user-count").textContent = `${data.totalElements} accounts`;
    document.querySelector("#user-pagination").innerHTML = `<button class="button button-quiet" data-user-page="${userPage - 1}" ${userPage <= 0 ? "disabled" : ""}>Previous</button><span>Page ${userPage + 1} of ${Math.max(data.totalPages, 1)}</span><button class="button button-quiet" data-user-page="${userPage + 1}" ${userPage + 1 >= data.totalPages ? "disabled" : ""}>Next</button>`;
    document.querySelectorAll("[data-user-page]").forEach(b => b.addEventListener("click", () => renderUsers(Number(b.dataset.userPage)))); document.querySelectorAll("[data-edit-roles]").forEach(b => b.addEventListener("click", () => openRoleEditor(b))); document.querySelectorAll("[data-toggle-user]").forEach(b => b.addEventListener("click", () => toggleUserAccess(b)));
  } catch (error) { slot.innerHTML = errorBlock(error); document.querySelector("#user-count").textContent = ""; }
}
async function createUser(event) { event.preventDefault(); const form = event.currentTarget; const error = document.querySelector("#user-form-error"); const data = new FormData(form); const roleCodes = data.getAll("roleCodes"); if (!roleCodes.length) { error.textContent="Select at least one role."; error.hidden=false; return; } try { await request("/users", { method:"POST", body:JSON.stringify({username:data.get("username"),displayName:data.get("displayName"),email:data.get("email")||null,password:data.get("password"),roleCodes}) }); document.querySelector("#user-dialog").close(); form.reset(); document.querySelector("#create-role-options").innerHTML=roleOptions(); setToast("User account created."); await loadUsers(); } catch(e){ error.textContent=e.message; error.hidden=false; } }
function openRoleEditor(button) { const form=document.querySelector("#role-form"); form.elements.userId.value=button.dataset.editRoles; document.querySelector("#role-user-label").textContent=`Editing access for ${button.dataset.name}.`; document.querySelector("#edit-role-options").innerHTML=roleOptions((button.dataset.roles||"").split(",").filter(Boolean)); document.querySelector("#role-dialog").showModal(); }
async function saveUserRoles(event){ event.preventDefault(); const form=event.currentTarget; const error=document.querySelector("#role-form-error"); const data=new FormData(form); const roleCodes=data.getAll("roleCodes"); if(!roleCodes.length){error.textContent="A user must have at least one role.";error.hidden=false;return;} try{await request(`/users/${data.get("userId")}/roles`,{method:"PUT",body:JSON.stringify({roleCodes})});document.querySelector("#role-dialog").close();setToast("User roles updated.");await loadUsers();}catch(e){error.textContent=e.message;error.hidden=false;} }
async function toggleUserAccess(button){ try{await request(`/users/${button.dataset.toggleUser}/${button.dataset.enabled==="true"?"deactivate":"activate"}`,{method:"POST"});setToast(button.dataset.enabled==="true"?"User disabled.":"User enabled.");await loadUsers();}catch(e){setToast(e.message,true);} }
async function openPermissionEditor(){ const perms=await request("/permissions"); const roleSelect=document.querySelector("#permission-role"); roleSelect.innerHTML=availableRoles.map(r=>`<option value="${safe(r.code)}">${safe(r.name)}</option>`).join(""); const draw=()=>{const role=availableRoles.find(r=>r.code===roleSelect.value);document.querySelector("#permission-options").innerHTML=perms.map(p=>`<label class="role-option"><input type="checkbox" name="permissionCodes" value="${safe(p.code)}" ${(role?.permissions||[]).includes(p.code)?"checked":""}><span><strong>${safe(p.code)}</strong><small>${safe(p.description)}</small></span></label>`).join("");};roleSelect.addEventListener("change",draw);draw();document.querySelector("#permission-dialog").showModal(); }
async function saveRolePermissions(event){event.preventDefault();const data=new FormData(event.currentTarget);try{await request(`/roles/${encodeURIComponent(document.querySelector("#permission-role").value)}/permissions`,{method:"PUT",body:JSON.stringify({permissionCodes:data.getAll("permissionCodes")})});setToast("Role permissions updated.");document.querySelector("#permission-dialog").close();await loadAvailableRoles();}catch(e){document.querySelector("#permission-error").textContent=e.message;document.querySelector("#permission-error").hidden=false;} }

async function renderAttendance() {
  const action = can("ATTENDANCE_RECORD") ? `<button class="button button-primary" id="add-session">New session +</button>` : "";
  root.innerHTML = `${heading("DAILY OPERATIONS", "Attendance", "Create class attendance sessions and record student attendance.", action)}<section class="section-panel data-panel"><div class="table-toolbar"><span class="result-count" id="attendance-count">Loading sessions...</span></div><div id="attendance-table" class="table-slot"><div class="loading-state compact">Loading attendance...</div></div></section>
  ${can("ATTENDANCE_RECORD") ? `<dialog class="form-dialog" id="attendance-dialog"><form id="attendance-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ATTENDANCE</p><h2>Open a session</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label>Academic year<select name="academicYearId" required></select></label><label>Class<select name="schoolClassId" required></select></label><label>Date<input type="date" name="attendanceDate" required></label><label>Session name<input name="sessionName" maxlength="80" placeholder="Morning" required></label></div><p class="form-error" id="attendance-form-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create session</button></div></form></dialog>
  <dialog class="form-dialog" id="attendance-record-dialog"><form id="attendance-record-form" class="dialog-form"><input type="hidden" name="attendanceSessionId"><div class="dialog-heading"><div><p class="eyebrow">ATTENDANCE RECORD</p><h2>Record student</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label class="field-span">Student<select name="studentId" required></select></label><label>Status<select name="status"><option value="PRESENT">Present</option><option value="ABSENT">Absent</option><option value="LATE">Late</option><option value="EXCUSED">Excused</option></select></label><label>Note<input name="note" maxlength="255"></label></div><p class="form-error" id="attendance-record-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save attendance</button></div></form></dialog>` : ""}`;
  bindCloseDialogs(); document.querySelector("#add-session")?.addEventListener("click", openAttendanceDialog); document.querySelector("#attendance-form")?.addEventListener("submit", createAttendanceSession); document.querySelector("#attendance-record-form")?.addEventListener("submit", recordAttendance); await loadAttendanceSessions();
}
async function loadAttendanceSessions(){const slot=document.querySelector("#attendance-table");try{const data=await request("/attendance-sessions?page=0&size=50");const rows=data.content.map(s=>`<tr><td>${dateText(s.attendanceDate)}</td><td><strong>${safe(s.schoolClassCode)}</strong></td><td>${safe(s.academicYearCode)}</td><td>${safe(s.sessionName)}</td><td>${can("ATTENDANCE_RECORD")?`<button class="text-button" data-record-session="${s.id}">Record student</button>`:""}</td></tr>`).join("")||emptyRow("No attendance sessions recorded.",5);slot.innerHTML=rowsTable(["DATE","CLASS","YEAR","SESSION","ACTIONS"],rows,"Attendance sessions");document.querySelector("#attendance-count").textContent=`${data.totalElements} sessions`;document.querySelectorAll("[data-record-session]").forEach(b=>b.addEventListener("click",()=>openAttendanceRecordDialog(b.dataset.recordSession)));}catch(e){slot.innerHTML=errorBlock(e);} }
async function openAttendanceDialog(){const [years,classes]=await Promise.all([request("/academic-years?page=0&size=100"),request("/academic-classes?page=0&size=100")]);document.querySelector('[name="academicYearId"]').innerHTML=years.content.map(y=>`<option value="${y.id}">${safe(y.name)}</option>`).join("");document.querySelector('[name="schoolClassId"]').innerHTML=classes.content.map(c=>`<option value="${c.id}">${safe(c.name)}</option>`).join("");document.querySelector('[name="attendanceDate"]').value=new Date().toISOString().slice(0,10);document.querySelector("#attendance-dialog").showModal();}
async function createAttendanceSession(event){event.preventDefault();const form=event.currentTarget;const error=document.querySelector("#attendance-form-error");try{await request("/attendance-sessions",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(form)))});document.querySelector("#attendance-dialog").close();setToast("Attendance session created.");await loadAttendanceSessions();}catch(e){error.textContent=e.message;error.hidden=false;}}
async function openAttendanceRecordDialog(sessionId){const students=await request("/students?page=0&size=500");const form=document.querySelector("#attendance-record-form");form.elements.attendanceSessionId.value=sessionId;form.elements.studentId.innerHTML=students.content.map(s=>`<option value="${s.id}">${safe(s.admissionNumber)} — ${safe([s.firstName,s.lastName].join(" "))}</option>`).join("");document.querySelector("#attendance-record-dialog").showModal();}
async function recordAttendance(event){event.preventDefault();const form=event.currentTarget;const error=document.querySelector("#attendance-record-error");const payload=Object.fromEntries(new FormData(form));if(!payload.note)payload.note=null;try{await request("/attendance-records",{method:"POST",body:JSON.stringify(payload)});document.querySelector("#attendance-record-dialog").close();setToast("Attendance recorded.");}catch(e){error.textContent=e.message;error.hidden=false;}}

function assessmentDialog(){return `<dialog class="form-dialog" id="assessment-dialog"><form id="assessment-form" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">ASSESSMENT</p><h2>Create assessment / test</h2></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label>Subject<select name="subjectId" required></select></label><label>Academic year<select name="academicYearId" required></select></label><label>Class<select name="schoolClassId" required></select></label><label>Assessment type<input name="assessmentType" required maxlength="40" placeholder="MID_TERM"></label><label>Total marks<input name="totalMarks" type="number" min="0.01" step="0.01" required></label><label>Assessment date<input name="assessmentDate" type="date" required></label><label>Due date<input name="dueDate" type="date" required></label></div><p class="form-error" id="assessment-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create assessment</button></div></form></dialog>`;}
function scoreDialog(){return `<dialog class="form-dialog" id="score-dialog"><form id="score-form" class="dialog-form"><input type="hidden" name="assessmentId"><div class="dialog-heading"><div><p class="eyebrow">SCORE ENTRY</p><h2>Add student score</h2><p class="dialog-note" id="score-assessment-label"></p></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><div class="form-grid"><label class="field-span">Student<select name="studentId" required></select></label><label>Score<input name="score" type="number" min="0" step="0.01" required></label></div><p class="form-error" id="score-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Save score</button></div></form></dialog>`;}
function gradingScaleDialog(){return `<dialog class="form-dialog" id="grading-dialog"><form id="grading-form-create" class="dialog-form"><div class="dialog-heading"><div><p class="eyebrow">GRADING</p><h2>Create grading scale</h2><p class="dialog-note">Define non-overlapping percentage ranges from 0 to 100.</p></div><button class="close-button" type="button" data-close-dialog>CLOSE</button></div><label>Scale name<input name="name" required maxlength="120" placeholder="Standard scale"></label><div id="grade-band-editor" class="band-editor"></div><button type="button" class="button button-quiet" id="add-grade-band" style="margin-top:12px">Add band +</button><p class="form-error" id="grading-create-error" hidden></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-close-dialog>Cancel</button><button class="button button-primary" type="submit">Create scale</button></div></form></dialog>`;}

async function renderResults(){
  const write = can("ASSESSMENT_WRITE");
  const superAdmin = signedInUser?.roles?.includes("SUPER_ADMIN") === true;
  const actions = `${superAdmin ? `<button class="button button-quiet" id="add-grading-scale">New grading scale</button>` : ""}${write ? `<button class="button button-primary" id="add-assessment">New assessment +</button>` : ""}`;
  root.innerHTML = `${heading("ASSESSMENT & RESULTS", "Assessment desk", "Create tests, enter or remove scores, use grading scales, and generate official result PDFs.", actions ? `<div class="action-cluster">${actions}</div>` : "")}
    <section class="section-panel data-panel"><div class="section-heading"><div><p class="eyebrow">ASSESSMENT REGISTER</p><h2>Configured assessments</h2></div></div><div id="assessment-table" class="table-slot"><div class="loading-state compact">Loading assessments...</div></div></section>
    <section class="section-panel data-panel"><div class="section-heading"><div><p class="eyebrow">SCORE REGISTER</p><h2>Student scores</h2><p class="dialog-note">Choose an assessment to view its scores.</p></div></div><div class="grade-form"><label>Assessment<select id="score-assessment-select"><option value="">Choose assessment</option></select></label>${write ? `<button class="button button-primary" id="add-score" type="button">Add score +</button>` : ""}</div><div id="score-table" class="table-slot" style="margin-top:16px"><div class="inline-notice"><span>Select an assessment.</span></div></div></section>
    ${canGenerateResultPdfs ? `<section class="section-panel grade-tool"><div class="section-heading"><div><p class="eyebrow">OFFICIAL RESULT PDF</p><h2>Generate report card</h2></div><span class="tool-index">02</span></div><div class="grade-form"><label>Academic year<select id="result-year"><option value="">Choose a year</option></select></label><label>Term<select id="result-term"><option value="">No term selected</option></select></label><label>Class<select id="result-class"><option value="">Choose a class</option></select></label><label>Student<select id="result-student"><option value="">Choose a student</option></select></label><label>Grading scale<select id="result-grading-scale" required><option value="">Choose a scale</option></select></label></div><div class="dialog-actions" style="justify-content:flex-start"><button class="button button-primary" type="button" id="preview-student-pdf">Preview Result</button><button class="button button-quiet" type="button" id="download-student-pdf">Download PDF</button><button class="button button-quiet" type="button" id="print-student-pdf">Print</button><button class="button button-primary" type="button" id="download-class-pdf">Class Results PDF</button></div></section>` : ""}
    <section class="section-panel grade-tool"><div class="section-heading"><div><p class="eyebrow">GRADE LOOKUP</p><h2>Apply a grading scale</h2></div><span class="tool-index">01</span></div><form id="grade-form" class="grade-form"><label>Grading scale<select name="gradingScaleId" required><option value="">Choose a scale</option></select></label><label>Percentage<input name="percentage" type="number" min="0" max="100" step="0.01" required></label><button class="button button-primary" type="submit">Find grade →</button></form><div id="grade-result" class="grade-result">Choose a scale and enter a percentage.</div></section>
    ${write ? assessmentDialog()+scoreDialog() : ""}${superAdmin ? gradingScaleDialog() : ""}`;
  bindCloseDialogs();
  if(write){document.querySelector("#add-assessment").addEventListener("click",openAssessmentDialog);document.querySelector("#assessment-form").addEventListener("submit",createAssessment);document.querySelector("#add-score").addEventListener("click",openScoreDialog);document.querySelector("#score-form").addEventListener("submit",createScore);}
  if(superAdmin){document.querySelector("#add-grading-scale").addEventListener("click",()=>{document.querySelector("#grade-band-editor").innerHTML="";addGradeBand("A",80,100,"Excellent");addGradeBand("B",70,79.99,"Very good");addGradeBand("C",60,69.99,"Good");addGradeBand("D",50,59.99,"Pass");addGradeBand("F",0,49.99,"Needs improvement");document.querySelector("#grading-dialog").showModal();});document.querySelector("#add-grade-band").addEventListener("click",()=>addGradeBand("",0,100,""));document.querySelector("#grading-form-create").addEventListener("submit",createGradingScale);}
  document.querySelector("#grade-form").addEventListener("submit",findGrade); document.querySelector("#score-assessment-select").addEventListener("change",loadScoresForSelectedAssessment);
  await Promise.all([loadAssessments(),loadGradingScales()]); if(canGenerateResultPdfs){const initialization=await Promise.allSettled([initializePdfControls(),initializePdfGradingScale()]);if(initialization[0].status==="rejected")setToast(initialization[0].reason?.message||"Result filters could not be loaded.",true);}
}

async function loadAssessments(){const slot=document.querySelector("#assessment-table");try{const data=await request("/assessments?page=0&size=100");const rows=data.content.map(item=>`<tr><td><strong>${safe(item.assessmentType)}</strong></td><td>${safe(item.subjectCode)}</td><td>${safe(item.schoolClassCode)}</td><td>${safe(item.academicYearCode)}</td><td>${safe(item.totalMarks)}</td><td>${dateText(item.assessmentDate)}</td><td>${can("ASSESSMENT_WRITE")?`<button class="text-button" data-manage-scores="${item.id}">Scores</button> <button class="text-button danger-link" data-delete-assessment="${item.id}">Remove</button>`:""}</td></tr>`).join("")||emptyRow("No assessments configured.",7);slot.innerHTML=rowsTable(["TYPE","SUBJECT","CLASS","YEAR","MAX MARKS","DATE","ACTIONS"],rows,"Assessment register");const select=document.querySelector("#score-assessment-select");select.innerHTML=`<option value="">Choose assessment</option>${data.content.map(a=>`<option value="${a.id}" data-label="${safe(`${a.assessmentType} • ${a.subjectCode} • ${a.schoolClassCode}`)}" data-max="${safe(a.totalMarks)}">${safe(a.assessmentType)} — ${safe(a.subjectCode)} — ${safe(a.schoolClassCode)}</option>`).join("")}`;document.querySelectorAll("[data-manage-scores]").forEach(b=>b.addEventListener("click",()=>{select.value=b.dataset.manageScores;loadScoresForSelectedAssessment();document.querySelector("#score-table").scrollIntoView({behavior:"smooth"});}));document.querySelectorAll("[data-delete-assessment]").forEach(b=>b.addEventListener("click",async()=>{if(!confirm("Remove this assessment and all of its recorded scores?"))return;try{await request(`/assessments/${b.dataset.deleteAssessment}`,{method:"DELETE"});setToast("Assessment removed.");await loadAssessments();document.querySelector("#score-table").innerHTML="";}catch(e){setToast(e.message,true);}}));}catch(e){slot.innerHTML=errorBlock(e);} }
async function openAssessmentDialog(){const [subjects,years,classes]=await Promise.all([request("/subjects?page=0&size=100"),request("/academic-years?page=0&size=100"),request("/academic-classes?page=0&size=100")]);const form=document.querySelector("#assessment-form");form.elements.subjectId.innerHTML=subjects.content.filter(s=>s.active).map(s=>`<option value="${s.id}">${safe(s.name)}</option>`).join("");form.elements.academicYearId.innerHTML=years.content.map(y=>`<option value="${y.id}">${safe(y.name)}</option>`).join("");form.elements.schoolClassId.innerHTML=classes.content.filter(c=>c.active).map(c=>`<option value="${c.id}">${safe(c.name)}</option>`).join("");const today=new Date().toISOString().slice(0,10);form.elements.assessmentDate.value=today;form.elements.dueDate.value=today;document.querySelector("#assessment-dialog").showModal();}
async function createAssessment(event){event.preventDefault();const form=event.currentTarget;const error=document.querySelector("#assessment-error");try{await request("/assessments",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(form)))});document.querySelector("#assessment-dialog").close();form.reset();setToast("Assessment created.");await loadAssessments();}catch(e){error.textContent=e.message;error.hidden=false;}}
async function loadScoresForSelectedAssessment(){const id=document.querySelector("#score-assessment-select").value;const slot=document.querySelector("#score-table");if(!id){slot.innerHTML=`<div class="inline-notice"><span>Select an assessment.</span></div>`;return;}slot.innerHTML=`<div class="loading-state compact">Loading scores...</div>`;try{const data=await request(`/assessments/${id}/scores?page=0&size=500`);const rows=data.content.map(s=>`<tr><td class="mono-cell">${safe(s.studentAdmissionNumber)}</td><td><strong>${safe(s.studentName)}</strong></td><td>${safe(s.score)} / ${safe(s.totalMarks)}</td><td>${Number(s.totalMarks)>0?(Number(s.score)/Number(s.totalMarks)*100).toFixed(1):"0.0"}%</td><td>${can("ASSESSMENT_WRITE")?`<button class="text-button" data-edit-score="${s.id}" data-score="${s.score}" data-max="${s.totalMarks}">Edit</button> <button class="text-button danger-link" data-delete-score="${s.id}">Remove</button>`:""}</td></tr>`).join("")||emptyRow("No scores recorded for this assessment.",5);slot.innerHTML=rowsTable(["ADMISSION","STUDENT","SCORE","PERCENTAGE","ACTIONS"],rows,"Assessment scores");document.querySelectorAll("[data-edit-score]").forEach(b=>b.addEventListener("click",()=>editScore(b)));document.querySelectorAll("[data-delete-score]").forEach(b=>b.addEventListener("click",async()=>{if(!confirm("Remove this score?"))return;try{await request(`/assessment-scores/${b.dataset.deleteScore}`,{method:"DELETE"});setToast("Score removed.");await loadScoresForSelectedAssessment();}catch(e){setToast(e.message,true);}}));}catch(e){slot.innerHTML=errorBlock(e);} }
async function openScoreDialog(){const select=document.querySelector("#score-assessment-select");if(!select.value){setToast("Choose an assessment first.",true);return;}const students=await request("/students?page=0&size=500");const form=document.querySelector("#score-form");form.elements.assessmentId.value=select.value;form.elements.studentId.innerHTML=students.content.map(s=>`<option value="${s.id}">${safe(s.admissionNumber)} — ${safe([s.firstName,s.middleName,s.lastName].filter(Boolean).join(" "))}</option>`).join("");form.elements.score.max=select.selectedOptions[0]?.dataset.max||"";document.querySelector("#score-assessment-label").textContent=select.selectedOptions[0]?.dataset.label||"";document.querySelector("#score-dialog").showModal();}
async function createScore(event){event.preventDefault();const form=event.currentTarget;const error=document.querySelector("#score-error");try{await request("/assessment-scores",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(form)))});document.querySelector("#score-dialog").close();form.reset();setToast("Score recorded.");await loadScoresForSelectedAssessment();}catch(e){error.textContent=e.message;error.hidden=false;}}
async function editScore(button){const next=prompt(`Enter new score (maximum ${button.dataset.max}):`,button.dataset.score);if(next===null)return;const numeric=Number(next);if(!Number.isFinite(numeric)||numeric<0||numeric>Number(button.dataset.max)){setToast("Enter a valid score within the assessment maximum.",true);return;}try{await request(`/assessment-scores/${button.dataset.editScore}`,{method:"PUT",body:JSON.stringify({score:numeric})});setToast("Score updated.");await loadScoresForSelectedAssessment();}catch(e){setToast(e.message,true);}}
function addGradeBand(label,min,max,remark){const editor=document.querySelector("#grade-band-editor");const row=document.createElement("div");row.className="band-row";row.innerHTML=`<input class="band-label" placeholder="Grade" value="${safe(label)}" required maxlength="20"><input class="band-min" type="number" min="0" max="100" step="0.01" value="${min}" required><input class="band-max" type="number" min="0" max="100" step="0.01" value="${max}" required><input class="band-remark" placeholder="Remark" maxlength="300" value="${safe(remark)}"><button class="text-button danger-link" type="button">Remove</button>`;row.querySelector("button").addEventListener("click",()=>row.remove());editor.appendChild(row);}
async function createGradingScale(event){event.preventDefault();const error=document.querySelector("#grading-create-error");const bands=[...document.querySelectorAll(".band-row")].map(r=>({gradeLabel:r.querySelector(".band-label").value,minimumPercentage:Number(r.querySelector(".band-min").value),maximumPercentage:Number(r.querySelector(".band-max").value),remark:r.querySelector(".band-remark").value||null}));try{await request("/grading-scales",{method:"POST",body:JSON.stringify({name:event.currentTarget.elements.name.value,bands})});document.querySelector("#grading-dialog").close();setToast("Grading scale created.");await loadGradingScales();}catch(e){error.textContent=e.message;error.hidden=false;}}
async function loadGradingScales(){const select=document.querySelector('[name="gradingScaleId"]');try{const data=await request("/grading-scales?page=0&size=100");select.innerHTML=`<option value="">Choose a scale</option>${data.content.map(s=>`<option value="${s.id}">${safe(s.name)}</option>`).join("")}`;if(!data.content.length)document.querySelector("#grade-result").textContent="No grading scales are configured yet.";}catch(e){select.innerHTML='<option value="">Unavailable</option>';document.querySelector("#grade-result").textContent=e.message;}}
async function findGrade(event){event.preventDefault();const values=new FormData(event.currentTarget);const result=document.querySelector("#grade-result");try{const band=await request(`/grading-scales/${values.get("gradingScaleId")}/grade?percentage=${encodeURIComponent(values.get("percentage"))}`);result.className="grade-result grade-found";result.innerHTML=`<span class="grade-letter">${safe(band.gradeLabel)}</span><span><strong>${safe(band.minimumPercentage)}% to ${safe(band.maximumPercentage)}%</strong><small>${safe(band.remark||"Configured grade band")}</small></span>`;}catch(e){result.className="grade-result grade-error";result.textContent=e.message;}}

async function initializePdfGradingScale(){const select=document.querySelector("#result-grading-scale");if(!select)return;try{const data=await request("/grading-scales?page=0&size=100");select.innerHTML=`<option value="">Choose a scale</option>${data.content.map(s=>`<option value="${safe(s.id)}">${safe(s.name)}</option>`).join("")}`;}catch(e){select.innerHTML='<option value="">Unavailable</option>';setToast(e.message||"Could not load grading scales.",true);return;}const year=document.querySelector("#result-year"),cls=document.querySelector("#result-class"),student=document.querySelector("#result-student"),term=document.querySelector("#result-term");const params=()=>({academicYearId:year.value,termId:term.value,gradingScaleId:select.value});const valid=(needStudent=true)=>{if(!year.value||!cls.value||!select.value||(needStudent&&!student.value)){setToast("Select an academic year, class, grading scale and student before generating the report.",true);return false;}return true;};const studentPath=()=>`/api/v1/results/students/${encodeURIComponent(student.value)}/pdf`;document.querySelector("#preview-student-pdf").onclick=()=>valid()&&openPdfDocument(studentPath(),{...params(),classId:cls.value},"preview");document.querySelector("#download-student-pdf").onclick=()=>valid()&&openPdfDocument(studentPath(),{...params(),classId:cls.value},"download");document.querySelector("#print-student-pdf").onclick=()=>valid()&&openPdfDocument(studentPath(),{...params(),classId:cls.value},"print");document.querySelector("#download-class-pdf").onclick=()=>valid(false)&&openPdfDocument(`/api/v1/results/classes/${encodeURIComponent(cls.value)}/pdf`,params(),"download");}

function buildResultPdfUrl(path,params={}){const query=new URLSearchParams(Object.entries(params).filter(([,v])=>v!==null&&v!==undefined&&v!=="")).toString();return `${path}${query?`?${query}`:""}`;}
function openPdfDocument(path,params={},mode="preview"){const url=buildResultPdfUrl(path,mode==="download"?params:{...params,inline:"true"});const w=window.open(url,"_blank","noopener,noreferrer");if(!w)setToast("Please allow pop-ups for result PDFs.",true);}
async function initializePdfControls(){const year=document.querySelector("#result-year"),cls=document.querySelector("#result-class"),student=document.querySelector("#result-student"),term=document.querySelector("#result-term");const [years,classes,students]=await Promise.all([request("/academic-years?page=0&size=100"),request("/academic-classes?page=0&size=100"),request("/students?page=0&size=500")]);year.innerHTML=`<option value="">Choose a year</option>${years.content.map(y=>`<option value="${y.id}" data-code="${safe(y.code)}">${safe(y.name)}</option>`).join("")}`;cls.innerHTML=`<option value="">Choose a class</option>${classes.content.map(c=>`<option value="${c.id}">${safe(c.name)}</option>`).join("")}`;student.innerHTML=`<option value="">Choose a student</option>${students.content.map(s=>`<option value="${s.id}">${safe([s.firstName,s.middleName,s.lastName].filter(Boolean).join(" "))}</option>`).join("")}`;const loadTerms=async()=>{term.innerHTML='<option value="">No term selected</option>';const code=year.selectedOptions[0]?.dataset.code;if(code){const data=await request(`/academic-years/${encodeURIComponent(code)}/terms`);term.innerHTML+=data.map(t=>`<option value="${t.id}">${safe(t.name)}</option>`).join("");}};year.addEventListener("change",loadTerms);if(years.content.length){year.value=(years.content.find(y=>y.active)||years.content[0]).id;await loadTerms();}const args=()=>({academicYearId:year.value,classId:cls.value,termId:term.value});const validate=(needStudent=true)=>{if(!year.value||!cls.value||(needStudent&&!student.value)){setToast(`Please select an academic year, class${needStudent?" and student":""}.`,true);return false;}return true;};document.querySelector("#preview-student-pdf").onclick=()=>validate()&&openPdfDocument(`/api/v1/results/students/${student.value}/pdf`,args(),"preview");document.querySelector("#download-student-pdf").onclick=()=>validate()&&openPdfDocument(`/api/v1/results/students/${student.value}/pdf`,args(),"download");document.querySelector("#print-student-pdf").onclick=()=>validate()&&openPdfDocument(`/api/v1/results/students/${student.value}/pdf`,args(),"print");document.querySelector("#download-class-pdf").onclick=()=>validate(false)&&openPdfDocument(`/api/v1/results/classes/${cls.value}/pdf`,{academicYearId:year.value,termId:term.value},"download");}

async function renderAccount(){root.innerHTML=`${heading("MY ACCOUNT","Account security","Change the password for your signed-in account.")}<section class="section-panel"><div class="section-heading"><div><p class="eyebrow">SIGNED IN AS</p><h2>${safe(signedInUser?.displayName||"")}</h2><p class="dialog-note">${safe(signedInUser?.username||"")} • ${(signedInUser?.roles||[]).map(safe).join(" / ")}</p></div></div><form id="password-form" class="form-grid" style="max-width:650px"><label>Current password<input type="password" name="currentPassword" required></label><label>New password<input type="password" name="newPassword" minlength="12" maxlength="72" required></label><div class="field-span"><button class="button button-primary" type="submit">Change password</button></div><p class="form-error field-span" id="password-error" hidden></p></form></section>`;document.querySelector("#password-form").addEventListener("submit",async(e)=>{e.preventDefault();const error=document.querySelector("#password-error");try{await request("/auth/change-password",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(e.currentTarget)))});e.currentTarget.reset();setToast("Password changed successfully.");}catch(err){error.textContent=err.message;error.hidden=false;}});}

async function render(){const view=window.location.hash.slice(1)||"home";setActivePage(view);root.innerHTML=`<div class="loading-state">Loading ${safe(pages[currentView].toLowerCase())}...</div>`;try{if(currentView==="home")await renderHome();else if(currentView==="students")await renderStudents();else if(currentView==="admissions")await renderAdmissions();else if(currentView==="academics")await renderAcademics();else if(currentView==="attendance")await renderAttendance();else if(currentView==="results")await renderResults();else if(currentView==="users")await renderUsers();else if(currentView==="account")await renderAccount();}catch(error){root.innerHTML=errorBlock(error);}}

try {
  const user = await currentUser();
  if (!user) window.location.replace("/login.html");
  else {
    signedInUser = user; signedInUserId = user.id; user.permissions.forEach((permission) => permissions.add(permission));
    canGenerateResultPdfs = (user.roles || []).some((role) => role === "ADMIN" || role === "SUPER_ADMIN") && (can("RESULT_VIEW") || can("REPORT_VIEW"));
    canEditAcademicCatalogue = (user.roles || []).includes("SUPER_ADMIN") && can("ACADEMICS_MANAGE");
    document.querySelectorAll("[data-permission]").forEach((link) => { if (!can(link.dataset.permission)) link.remove(); });
    document.querySelector("#user-name").textContent = user.displayName; document.querySelector("#user-role").textContent = user.roles.join(" / ") || "School staff"; document.querySelector("#user-avatar").textContent = user.displayName.split(/\s+/).filter(Boolean).slice(0,2).map(p=>p[0]).join("").toUpperCase() || "--"; document.querySelector("#topbar-user").textContent=user.displayName; document.querySelector("#today-label").textContent=new Intl.DateTimeFormat(undefined,{day:"numeric",month:"short",year:"numeric"}).format(new Date());
    document.querySelector("#sign-out").addEventListener("click",async()=>{try{await signOut();}catch{}window.location.replace("/login.html");}); document.querySelector("#menu-toggle").addEventListener("click",(event)=>{const open=document.querySelector("#sidebar").classList.toggle("sidebar-open");event.currentTarget.setAttribute("aria-expanded",String(open));}); window.addEventListener("hashchange",render); await render();
  }
} catch (error) { root.innerHTML = errorBlock(error); }
