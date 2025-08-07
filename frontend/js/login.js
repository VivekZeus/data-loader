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
const buttonElement = document.getElementById("loginSubmitButton");
function verifyUser(userId) {
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
function handleLogin(userId) {
    return __awaiter(this, void 0, void 0, function* () {
        const success = yield verifyUser(userId);
        if (success) {
            localStorage.setItem("userId", userId);
            window.location.href = "index.html";
        }
        else {
            alert("Login failed!");
        }
    });
}
buttonElement.addEventListener("click", (event) => {
    event.preventDefault();
    const userIdInput = document.getElementById("userIdInput");
    if (userIdInput === null)
        throw new Error("User ID input not found");
    const userId = userIdInput.value.trim();
    if (userId === "") {
        alert("Please enter a User ID.");
        return;
    }
    handleLogin(userId);
});
