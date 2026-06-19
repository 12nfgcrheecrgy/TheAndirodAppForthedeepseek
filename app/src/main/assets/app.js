(function() {
    const container = document.getElementById('chat-container');
    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const stopBtn = document.getElementById('stop-btn');
    const statusBar = document.getElementById('status-bar');
    const welcome = document.getElementById('welcome');

    let isStreaming = false;
    let currentAssistantBubble = null;
    let currentReasoningBody = null;
    let currentContentEl = null;
    let messageActionsEnabled = true;

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function simpleMarkdown(text) {
        let html = escapeHtml(text);
        html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
        html = html.replace(/^(#{1,6})\s+(.*)$/gm, (_, hashes, content) => {
            const level = hashes.length;
            return `<h${level}>${content}</h${level}>`;
        });
        const blocks = html.split(/\n\n+/);
        return blocks.map(block => {
            if (block.trim().startsWith('<pre') || block.trim().startsWith('<h')) return block;
            return `<p>${block.replace(/\n/g, '<br>')}</p>`;
        }).join('');
    }

    function scrollToBottom() {
        container.scrollTop = container.scrollHeight;
    }

    function hideWelcome() {
        if (welcome && welcome.parentNode) {
            welcome.style.display = 'none';
        }
    }

    function showWelcome() {
        container.innerHTML = '';
        if (welcome) {
            welcome.style.display = '';
            container.appendChild(welcome);
        }
    }

    function createMessage(role) {
        hideWelcome();
        const msg = document.createElement('div');
        msg.className = 'message ' + role;
        const bubble = document.createElement('div');
        bubble.className = 'bubble';
        msg.appendChild(bubble);
        container.appendChild(msg);
        scrollToBottom();
        return bubble;
    }

    function appendMessageActions(bubble, content, role, isLast) {
        if (!messageActionsEnabled) return;
        const existing = bubble.querySelector('.message-actions');
        if (existing) existing.remove();
        const actions = document.createElement('div');
        actions.className = 'message-actions';

        const copyBtn = document.createElement('button');
        copyBtn.textContent = '复制';
        copyBtn.onclick = () => {
            if (window.AndroidBridge) {
                window.AndroidBridge.copyMessage(content);
            }
        };
        actions.appendChild(copyBtn);

        if (role === 'assistant' && isLast) {
            const regenBtn = document.createElement('button');
            regenBtn.textContent = '重新生成';
            regenBtn.onclick = () => {
                if (window.AndroidBridge) {
                    window.AndroidBridge.regenerateLastMessage();
                }
            };
            actions.appendChild(regenBtn);
        }

        bubble.appendChild(actions);
    }

    function renderMessage(role, content, reasoning) {
        const bubble = createMessage(role);
        if (reasoning && reasoning.length > 0) {
            const reasoningEl = document.createElement('div');
            reasoningEl.className = 'reasoning expanded';
            const header = document.createElement('div');
            header.className = 'reasoning-header';
            header.textContent = '思考过程';
            const body = document.createElement('div');
            body.className = 'reasoning-body';
            body.textContent = reasoning;
            reasoningEl.appendChild(header);
            reasoningEl.appendChild(body);
            bubble.appendChild(reasoningEl);
            reasoningEl.addEventListener('click', () => {
                reasoningEl.classList.toggle('expanded');
            });
        }
        const contentEl = document.createElement('div');
        contentEl.className = 'content';
        contentEl.innerHTML = simpleMarkdown(content);
        bubble.appendChild(contentEl);
        return bubble;
    }

    function setStatus(text, isError) {
        statusBar.textContent = text || '';
        statusBar.classList.toggle('error', !!isError);
    }

    function updateInputState() {
        input.disabled = isStreaming;
        sendBtn.style.display = isStreaming ? 'none' : 'flex';
        stopBtn.style.display = isStreaming ? 'flex' : 'none';
    }

    function sendMessage() {
        const text = input.value.trim();
        if (!text || isStreaming) return;
        input.value = '';
        input.style.height = 'auto';

        if (window.AndroidBridge) {
            window.AndroidBridge.sendMessage(JSON.stringify({ content: text }));
        } else {
            setStatus('桥接未就绪', true);
        }
    }

    function adjustInputHeight() {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 120) + 'px';
    }

    sendBtn.addEventListener('click', sendMessage);
    stopBtn.addEventListener('click', () => {
        if (window.AndroidBridge) {
            window.AndroidBridge.stopGeneration();
        }
    });
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    input.addEventListener('input', adjustInputHeight);

    window.loadHistory = function(messages) {
        showWelcome();
        if (!messages || messages.length === 0) {
            showWelcome();
            return;
        }
        messages.forEach((msg, index) => {
            const bubble = renderMessage(msg.role, msg.content, msg.reasoning);
            appendMessageActions(bubble, msg.content, msg.role, index === messages.length - 1);
        });
        scrollToBottom();
    };

    window.clearChat = function() {
        showWelcome();
        isStreaming = false;
        currentAssistantBubble = null;
        currentReasoningBody = null;
        currentContentEl = null;
        updateInputState();
    };

    window.setLoading = function(loading) {
        isStreaming = loading;
        updateInputState();
    };

    window.onUserMessage = function(content) {
        const bubble = renderMessage('user', content, '');
        appendMessageActions(bubble, content, 'user', false);
        scrollToBottom();
    };

    window.onAssistantStart = function() {
        isStreaming = true;
        updateInputState();
        currentAssistantBubble = createMessage('assistant');
        currentContentEl = document.createElement('div');
        currentContentEl.className = 'content';
        currentAssistantBubble.appendChild(currentContentEl);
        setStatus('正在思考…');
    };

    window.onReasoningDelta = function(delta) {
        if (!currentAssistantBubble) return;
        if (!currentReasoningBody) {
            const reasoning = document.createElement('div');
            reasoning.className = 'reasoning';
            const header = document.createElement('div');
            header.className = 'reasoning-header';
            header.textContent = '思考过程';
            currentReasoningBody = document.createElement('div');
            currentReasoningBody.className = 'reasoning-body';
            reasoning.appendChild(header);
            reasoning.appendChild(currentReasoningBody);
            currentAssistantBubble.insertBefore(reasoning, currentContentEl);
            reasoning.addEventListener('click', () => {
                reasoning.classList.toggle('expanded');
            });
        }
        currentReasoningBody.textContent += delta;
        scrollToBottom();
    };

    window.onContentDelta = function(delta) {
        if (!currentContentEl) return;
        currentContentEl.dataset.raw = (currentContentEl.dataset.raw || '') + delta;
        currentContentEl.innerHTML = simpleMarkdown(currentContentEl.dataset.raw);
        scrollToBottom();
    };

    window.onError = function(message) {
        isStreaming = false;
        updateInputState();
        setStatus(message, true);
    };

    window.onAssistantComplete = function() {
        isStreaming = false;
        updateInputState();
        if (currentAssistantBubble && currentContentEl) {
            appendMessageActions(currentAssistantBubble, currentContentEl.dataset.raw || '', 'assistant', true);
        }
        currentAssistantBubble = null;
        currentReasoningBody = null;
        currentContentEl = null;
        setStatus('');
    };

    function init() {
        if (window.AndroidBridge) {
            try {
                const cfg = JSON.parse(window.AndroidBridge.getConfig() || '{}');
                if (!cfg.apiKeySet) {
                    setStatus('请配置 API Key 后使用', true);
                }
            } catch (e) {
                console.error(e);
            }
        }
        updateInputState();
    }

    init();
})();
