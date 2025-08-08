"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
const userId = localStorage.getItem("userId");
if (!userId) {
    window.location.href = "login.html";
}
// Element references
const startButton = document.getElementById("startButton");
const refreshButton = document.getElementById("refreshButton");
const cancelTaskButton = document.getElementById("cancelTaskButton");
const resumeTaskButton = document.getElementById("resumeTaskButton");
const requestedRecordsInput = document.getElementById("requestedRecordsInput");
const viewDataButton = document.getElementById("viewData");
const dataTableBody = document.querySelector("#dataTable tbody");
const dataTable = document.getElementById("dataTable");
if (!startButton ||
    !refreshButton ||
    !cancelTaskButton ||
    !resumeTaskButton ||
    !requestedRecordsInput) {
    throw new Error("One or more required elements not found in the DOM.");
}
function getTaskStatus() {
    return fetch(`http://localhost:8081/task/progress/${userId}`)
        .then((res) => res.json())
        .then((data) => {
        if (data.status === "COMPLETED") {
            viewDataButton.style.display = "inline-block"; // Show button
        }
        return data.status;
    });
}
// Simulate API call to get completed task's data
function fetchCompletedTaskData() {
    return fetch(`http://localhost:8081/task/view-data/${userId}`)
        .then((res) => res.json());
}
// Populate task data into UI
function updateTaskUI(data) {
    const { status, startedAt, processedRecords, requestedRecords } = data;
    document.getElementById("taskStatusText").innerText = `Task Status: ${status}`;
    document.getElementById("taskStartTimeText").innerText = `Start Time: ${new Date(startedAt).toLocaleString()}`;
    document.getElementById("taskProcessedRecords").innerText = `Records Processed Till Now: ${processedRecords}`;
    document.getElementById("requestedRecords").innerText = `Requested Records : ${requestedRecords}`;
    const progress = Math.floor((processedRecords / requestedRecords) * 100);
    const progressBar = document.getElementById("taskProgressBar");
    progressBar.style.width = `${progress}%`;
    progressBar.innerText = `${progress}%`;
    document.getElementById("taskProgressText").innerText = `Progress : ${progress}%`;
    startButton.disabled = ["CANCELLED", "COMPLETED"].indexOf(status) === -1;
    requestedRecordsInput.disabled =
        ["CANCELLED", "COMPLETED"].indexOf(status) === -1;
    resumeTaskButton.disabled = ["CANCELLED", "STOPPED"].indexOf(status) === -1;
    cancelTaskButton.disabled =
        ["CANCELLED", "COMPLETED", "COLLECTED"].indexOf(status) !== -1;
}
function loadTaskStatus() {
    fetch(`http://localhost:8081/task/progress/${userId}`)
        .then((res) => res.json())
        .then(updateTaskUI)
        .catch(() => {
        resumeTaskButton.disabled = true;
        cancelTaskButton.disabled = true;
        // alert("You don't have any ongoing task.");
    });
}
// Generic POST call
function postRequest(url, body) {
    return __awaiter(this, void 0, void 0, function* () {
        return fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
    });
}
// Task creation handler
startButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    const records = parseInt(requestedRecordsInput.value.trim(), 10);
    if (isNaN(records) || records <= 0) {
        alert("Please enter a valid number of records.");
        return;
    }
    const response = yield postRequest("http://localhost:8081/task/create", {
        userId,
        recordsRequested: records,
    });
    if (response.status === 201) {
        alert("Task received successfully!");
    }
    else {
        alert("You currently have an ongoing task!");
    }
    window.location.reload();
}));
// Refresh button handler
refreshButton.addEventListener("click", loadTaskStatus);
// Cancel task handler
cancelTaskButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    try {
        yield postRequest("http://localhost:8081/task/cancel", { userId });
        window.location.reload();
    }
    catch (err) {
        console.error("Error cancelling task:", err);
    }
}));
// Resume task handler
resumeTaskButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const response = yield postRequest("http://localhost:8081/task/resume", {
            userId,
        });
        if (response.ok) {
            alert("Resumed task successfully!");
            window.location.reload();
        }
        else {
            alert("Error occurred while resuming the task.");
        }
    }
    catch (err) {
        console.error("Error resuming task:", err);
    }
}));
viewDataButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const data = yield fetchCompletedTaskData();
        // To show the table
        dataTable.style.display = "block";
        dataTableBody.innerHTML = ""; // Clear existing rows
        data.forEach((item) => {
            const row = document.createElement("tr");
            row.innerHTML = `
        <td>${item.age}</td>
        <td>${item.rollNo}</td>
        <td>${item.name}</td>
      `;
            dataTableBody.appendChild(row);
            viewDataButton.disabled = true;
        });
    }
    catch (error) {
        console.error("Error fetching data:", error);
        alert("Failed to load data.");
    }
}));
// Initialize everything on page load
loadTaskStatus();
getTaskStatus();
