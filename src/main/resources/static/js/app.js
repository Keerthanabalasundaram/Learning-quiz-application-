document.addEventListener("DOMContentLoaded", () => {
    // 1. Question Option Select Styling
    const optionButtons = document.querySelectorAll(".btn-glass-option");
    const optionInput = document.getElementById("selectedOptionInput");

    optionButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            // Remove 'selected' class from all options
            optionButtons.forEach(b => b.classList.remove("selected"));
            
            // Add 'selected' class to clicked option
            btn.classList.add("selected");
            
            // Update hidden input value
            if (optionInput) {
                optionInput.value = btn.getAttribute("data-option");
            }
        });
    });

    // 2. Circular Question Timer Countdown
    const timerCircle = document.querySelector(".timer-circle-fg");
    const timerText = document.querySelector(".timer-text");
    const quizForm = document.getElementById("quizForm");

    if (timerCircle && timerText && quizForm) {
        const totalTime = parseInt(document.getElementById("questionLimitSeconds").value) || 45;
        let timeRemaining = totalTime;
        const radius = 35;
        const circumference = 2 * Math.PI * radius; // 220
        
        timerCircle.style.strokeDasharray = circumference;

        const questionTimer = setInterval(() => {
            timeRemaining--;
            timerText.textContent = timeRemaining;

            // Update Circle Ring Offset
            const offset = circumference - (timeRemaining / totalTime) * circumference;
            timerCircle.style.strokeDashoffset = offset;

            // Color change at warning limits
            if (timeRemaining <= 10) {
                timerCircle.style.stroke = "#ef4444"; // red
                timerText.style.color = "#ef4444";
            } else if (timeRemaining <= 20) {
                timerCircle.style.stroke = "#f59e0b"; // orange
                timerText.style.color = "#f59e0b";
            }

            if (timeRemaining <= 0) {
                clearInterval(questionTimer);
                // Trigger auto submit
                console.log("Question timer expired. Auto-submitting...");
                quizForm.submit();
            }
        }, 1000);
    }

    // 3. Global Quiz Session Timer Bar
    const globalProgress = document.getElementById("globalTimerProgress");
    if (globalProgress) {
        let globalRemaining = parseInt(document.getElementById("globalRemainingSeconds").value) || 600;
        const globalLimit = 600; // 10 minutes limit
        
        const globalTimer = setInterval(() => {
            globalRemaining--;
            
            // Update progress bar percentage
            const percentage = (globalRemaining / globalLimit) * 100;
            globalProgress.style.width = percentage + "%";

            if (globalRemaining <= 0) {
                clearInterval(globalTimer);
                console.log("Global quiz session timer expired! Force completing...");
                
                // Simple and safe GET redirection to matching controller path
                window.location.href = "/quiz/complete?timeout=true";
            }
        }, 1000);
    }
});
