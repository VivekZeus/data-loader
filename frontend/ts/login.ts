const buttonElement = document.getElementById(
  "loginSubmitButton"
) as HTMLButtonElement;

function verifyUser(userId: String): Promise<boolean> {
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

  return fetch("http://localhost:8081/user/verify", requestOptions)
    .then((response) => {
      return response.status === 200 || response.status === 201;
    })
    .catch((error) => {
      console.error("Error:", error);
      return false;
    });
}

async function handleLogin(userId: string) {
  const success = await verifyUser(userId);

  if (success) {
    localStorage.setItem("userId", userId);
    window.location.href = "index.html";
  } else {
    alert("Login failed!");
  }
}

buttonElement.addEventListener("click", (event: MouseEvent) => {
  event.preventDefault();

  const userIdInput = document.getElementById(
    "userIdInput"
  ) as HTMLInputElement | null;
  if (userIdInput === null) throw new Error("User ID input not found");

  const userId = userIdInput.value.trim();
  if (userId === "") {
    alert("Please enter a User ID.");
    return;
  }

  handleLogin(userId);
});
