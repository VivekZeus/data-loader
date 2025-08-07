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
if (localStorage.getItem("userId") == null) {
    window.location.href = "login.html";
}
const startButton = document.getElementById("startButton");
const refreshButton = document.getElementById("refreshButton");
const cancelTaskButton = document.getElementById("cancelTaskButton");
const resumeTaskButton = document.getElementById("resumeTaskButton");
function putDataToText(data) {
    document.getElementById("taskStatusText").innerText = `Task Status: ${data.status}`;
    const startTime = new Date(data.startedAt);
    const formattedTime = startTime.toLocaleString();
    document.getElementById("taskStartTimeText").innerText = `Start Time: ${formattedTime}`;
    document.getElementById("taskProgressText").innerText = `Progress: ${(data.processedRecords / data.requestedRecords) * 100} %`;
    document.getElementById("taskProcessedRecords").innerText = `Records Processed Till Now: ${data.processedRecords}`;
}
function initialize() {
    let userId = localStorage.getItem("userId");
    const requestOptions = {
        method: "GET",
    };
    fetch(`http://localhost:8081/task/progress/${userId}`, requestOptions)
        .then((response) => response.json())
        .then((data) => {
        if (!(data.status === "CANCELLED" || data.status === "COMPLETED")) {
            startButton.disabled = true;
        }
        if (!(data.status === "CANCELLED" || data.status === "STOPPED")) {
            resumeTaskButton.disabled = true;
        }
        if (data.status == "CANCELLED" || data.status == "COMPLETED" || data.status == "COLLECTED") {
            cancelTaskButton.disabled = true;
        }
        putDataToText(data);
    })
        .catch((error) => {
        alert("some error occured");
    });
}
if (resumeTaskButton == null ||
    startButton == null ||
    cancelTaskButton == null ||
    refreshButton == null) {
    throw new Error("");
}
initialize();
function createTask(requestOptions) {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            const response = yield fetch("http://localhost:8081/task/create", requestOptions);
            return response.status === 201;
        }
        catch (error) {
            console.error("Error creating task:", error);
            return false;
        }
    });
}
startButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    const requestedRecordsInput = document.getElementById("requestedRecordsInput");
    if (requestedRecordsInput === null) {
        alert("Enter Requested Records...");
        return;
    }
    const reqRecords = parseInt(requestedRecordsInput.value.trim(), 10);
    let userId = localStorage.getItem("userId");
    const myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    const raw = JSON.stringify({
        userId: userId,
        recordsRequested: reqRecords,
    });
    const requestOptions = {
        method: "POST",
        headers: myHeaders,
        body: raw,
    };
    const success = yield createTask(requestOptions);
    if (success) {
        alert("Task Recieved Successfully!");
        window.location.reload();
    }
    else {
        alert("You Currently have a ongoing task !");
        window.location.reload();
    }
}));
refreshButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    let userId = localStorage.getItem("userId");
    const requestOptions = {
        method: "GET",
    };
    fetch(`http://localhost:8081/task/progress/${userId}`, requestOptions)
        .then((response) => response.json())
        .then((result) => {
        putDataToText(result);
    })
        .catch((error) => {
        alert("some error occured");
    });
}));
cancelTaskButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    let userId = localStorage.getItem("userId");
    const myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    const raw = JSON.stringify({
        userId: userId,
    });
    const requestOptions = {
        method: "POST",
        headers: myHeaders,
        body: raw,
    };
    fetch("http://localhost:8081/task/cancel", requestOptions)
        .then((response) => response.json())
        .then((data) => { })
        .catch((error) => console.error(error));
    window.location.reload();
}));
resumeTaskButton.addEventListener("click", () => __awaiter(void 0, void 0, void 0, function* () {
    let userId = localStorage.getItem("userId");
    const myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    const raw = JSON.stringify({
        userId: userId,
    });
    const requestOptions = {
        method: "POST",
        headers: myHeaders,
        body: raw,
    };
    fetch("http://localhost:8081/task/resume", requestOptions)
        .then((response) => {
        if (response.status == 200) {
            alert("Resumed Task successfully");
            window.location.reload();
        }
        else {
            alert("Error Ocurred Resuming Task...");
        }
    })
        .catch((error) => console.error(error));
}));
