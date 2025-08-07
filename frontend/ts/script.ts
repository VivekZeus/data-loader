if (localStorage.getItem("userId") == null) {
  window.location.href = "login.html";
}

const startButton = document.getElementById("startButton") as HTMLButtonElement;

const refreshButton = document.getElementById(
  "refreshButton"
) as HTMLButtonElement;

const cancelTaskButton = document.getElementById(
  "cancelTaskButton"
) as HTMLButtonElement;

const resumeTaskButton = document.getElementById(
  "resumeTaskButton"
) as HTMLButtonElement;

function putDataToText(data: any) {
  document.getElementById(
    "taskStatusText"
  )!.innerText = `Task Status: ${data.status}`;
  const startTime = new Date(data.startedAt);
  const formattedTime = startTime.toLocaleString();

  document.getElementById(
    "taskStartTimeText"
  )!.innerText = `Start Time: ${formattedTime}`;
  document.getElementById("taskProgressText")!.innerText = `Progress: ${
    (data.processedRecords / data.requestedRecords) * 100
  } %`;
  document.getElementById(
    "taskProcessedRecords"
  )!.innerText = `Records Processed Till Now: ${data.processedRecords}`;
}

function initialize() {
  let userId: string | null = localStorage.getItem("userId");

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
      if (data.status == "CANCELLED" || data.status=="COMPLETED" || data.status=="COLLECTED") {
        cancelTaskButton.disabled = true;
      }


      putDataToText(data);
    })
    .catch((error) => {
      alert("some error occured");
    });
}

if (
  resumeTaskButton == null ||
  startButton == null ||
  cancelTaskButton == null ||
  refreshButton == null
) {
  throw new Error("");
}

initialize();

async function createTask(requestOptions: {}): Promise<boolean> {
  try {
    const response = await fetch(
      "http://localhost:8081/task/create",
      requestOptions
    );
    return response.status === 201;
  } catch (error) {
    console.error("Error creating task:", error);
    return false;
  }
}

startButton.addEventListener("click", async () => {
  const requestedRecordsInput = document.getElementById(
    "requestedRecordsInput"
  ) as HTMLInputElement | null;
  if (requestedRecordsInput === null) {
    alert("Enter Requested Records...");
    return;
  }
  const reqRecords = parseInt(requestedRecordsInput.value.trim(), 10);

  let userId: string | null = localStorage.getItem("userId");

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

  const success = await createTask(requestOptions);
  if (success) {
    alert("Task Recieved Successfully!");
    window.location.reload();
  } else {
    alert("You Currently have a ongoing task !");
    window.location.reload();
  }
});

refreshButton.addEventListener("click", async () => {
  let userId: string | null = localStorage.getItem("userId");

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
});

cancelTaskButton.addEventListener("click", async () => {
  let userId: string | null = localStorage.getItem("userId");

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
    .then((data) => {})
    .catch((error) => console.error(error));

    window.location.reload();
});

resumeTaskButton.addEventListener("click", async () => {
  let userId: string | null = localStorage.getItem("userId");

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
      } else {
        alert("Error Ocurred Resuming Task...");
      }
    })
    .catch((error) => console.error(error));
});
