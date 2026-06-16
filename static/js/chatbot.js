document.addEventListener("DOMContentLoaded", () => {
    const chatToggle = document.getElementById("chatToggleBtn");
    const chatPanel = document.getElementById("chatPanel");
    const closeChat = document.getElementById("closeChatBtn");
    const sendChat = document.getElementById("sendChatBtn");
    const chatInput = document.getElementById("chatInput");
    const chatBody = document.getElementById("chatBody");

    let isHistoryLoaded = false;

    // Toggle Chat Panel
    if (chatToggle && chatPanel) {
        chatToggle.addEventListener("click", () => {
            if (chatPanel.style.display === "none" || chatPanel.style.display === "") {
                chatPanel.style.display = "flex";
                chatInput.focus();
                if (!isHistoryLoaded) {
                    loadChatHistory();
                }
            } else {
                chatPanel.style.display = "none";
            }
        });
    }

    if (closeChat && chatPanel) {
        closeChat.addEventListener("click", () => {
            chatPanel.style.display = "none";
        });
    }

    // Send Message on click or Enter key
    if (sendChat && chatInput) {
        sendChat.addEventListener("click", sendMessage);
        chatInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                sendMessage();
            }
        });
    }

    function loadChatHistory() {
        fetch("/chat/history")
            .then(res => res.json())
            .then(data => {
                chatBody.innerHTML = ""; // Clear loader
                if (data.length === 0) {
                    appendMessage("TUTOR", "👋 Hi! I'm your Personal Tutor. Ask me for hints, OOP cheat sheets, or concept explanations!");
                } else {
                    data.forEach(msg => {
                        appendMessage(msg.sender, msg.text);
                    });
                }
                isHistoryLoaded = true;
                scrollToBottom();
            })
            .catch(err => {
                console.error("Failed to load chat history:", err);
                appendMessage("TUTOR", "⚠️ Failed to connect to tutor chat history.");
            });
    }

    function sendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        // Append user message instantly
        appendMessage("USER", text);
        chatInput.value = "";
        scrollToBottom();

        // Show typing indicator
        const typingIndicator = showTypingIndicator();
        scrollToBottom();

        // Prepare Fetch POST request with CSRF support
        const csrfHeader = getCsrfHeaderName();
        const csrfToken = getCsrfToken();

        const headers = {
            "Content-Type": "application/x-www-form-urlencoded"
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch("/chat/send", {
            method: "POST",
            headers: headers,
            body: new URLSearchParams({ message: text })
        })
        .then(res => {
            if (!res.ok) throw new Error("Server error");
            return res.json();
        })
        .then(data => {
            typingIndicator.remove();
            appendMessage("TUTOR", data.tutorResponse);
            scrollToBottom();
        })
        .catch(err => {
            typingIndicator.remove();
            console.error(err);
            appendMessage("TUTOR", "⚠️ Sorry, I encountered a connection error. Please try again.");
            scrollToBottom();
        });
    }

    function appendMessage(sender, text) {
        const msgDiv = document.createElement("div");
        msgDiv.className = `chat-message ${sender.toLowerCase()}`;
        
        // Basic Markdown-to-HTML formatter for code blocks, bold text, lists
        msgDiv.innerHTML = formatMarkdown(text);
        chatBody.appendChild(msgDiv);
    }

    function showTypingIndicator() {
        const div = document.createElement("div");
        div.className = "chat-message tutor typing-indicator-msg";
        div.innerHTML = "<span>💬 Tutor is thinking...</span>";
        chatBody.appendChild(div);
        return div;
    }

    function scrollToBottom() {
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    // Helper functions to fetch Spring Security CSRF tokens
    function getCsrfToken() {
        const tokenMeta = document.querySelector("meta[name='_csrf']");
        return tokenMeta ? tokenMeta.getAttribute("content") : null;
    }

    function getCsrfHeaderName() {
        const headerMeta = document.querySelector("meta[name='_csrf_header']");
        return headerMeta ? headerMeta.getAttribute("content") : null;
    }

    // Basic regex markdown parser to render tutor notes beautifully
    function formatMarkdown(text) {
        if (!text) return "";
        let formatted = text
            // Escape HTML entities to prevent XSS
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            // Code blocks: ```java ... ```
            .replace(/```(?:[a-zA-Z]+)?\n([\s\S]*?)```/g, "<pre class='bg-dark text-warning p-2 rounded my-1' style='font-size: 0.82rem;'><code>$1</code></pre>")
            // Inline code: `var`
            .replace(/`([^`]+)`/g, "<code class='bg-dark text-warning px-1 rounded'>$1</code>")
            // Bold: **text** or *text*
            .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
            // Bullet points: * item
            .replace(/^\*\s+(.+)$/gm, "<li>$1</li>")
            // Newlines to breaks
            .replace(/\n/g, "<br>");

        return formatted;
    }
});
