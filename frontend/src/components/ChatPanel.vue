<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { useAiStore } from '../stores/ai'
import { hasConfirmKeywords } from '../utils/ai-helpers'

const aiStore = useAiStore()
const inputText = ref('')
const msgListRef = ref(null)

// Auto-scroll to bottom when messages change
watch(
  () => aiStore.messages.length,
  () => {
    nextTick(() => {
      if (msgListRef.value) {
        msgListRef.value.scrollTop = msgListRef.value.scrollHeight
      }
    })
  }
)

// Render message content with confirm keyword highlighting
function renderContent(msg) {
  if (msg.role !== 'assistant' || !hasConfirmKeywords(msg.content)) {
    return msg.content
  }
  // Highlight confirm keywords
  const keywords = ['确认', '确定', '回复', '同意', '执行']
  let html = msg.content
  for (const kw of keywords) {
    html = html.replaceAll(kw, `<span class="confirm-highlight">${kw}</span>`)
  }
  return html
}

function needsHtml(msg) {
  return msg.role === 'assistant' && hasConfirmKeywords(msg.content)
}

const charCount = computed(() => inputText.value.length)

function handleSend() {
  if (!aiStore.canSend || !inputText.value.trim()) return
  aiStore.sendMessage(inputText.value)
  inputText.value = ''
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <transition name="chat-panel">
    <div v-show="aiStore.chatOpen" class="chat-panel">
      <!-- Title bar -->
      <div class="chat-header">
        <span class="chat-title">小农助手 🌱</span>
        <button class="chat-close" @click="aiStore.toggleChat()" title="关闭">✕</button>
      </div>

      <!-- Message list -->
      <div ref="msgListRef" class="chat-messages">
        <div
          v-for="msg in aiStore.messages"
          :key="msg.id"
          class="msg-row"
          :class="{
            'msg-user': msg.role === 'user',
            'msg-ai': msg.role === 'assistant',
            'msg-error': msg.role === 'error'
          }"
        >
          <div class="msg-bubble">
            <span v-if="needsHtml(msg)" v-html="renderContent(msg)"></span>
            <span v-else>{{ msg.content }}</span>
          </div>
          <div class="msg-time">{{ msg.time }}</div>
        </div>

        <!-- Loading indicator -->
        <div v-if="aiStore.loading" class="msg-row msg-ai">
          <div class="msg-bubble msg-loading">小农正在思考... ⏳</div>
        </div>
      </div>

      <!-- Input area -->
      <div class="chat-input-area">
        <div v-if="aiStore.rateLimited" class="rate-limit-tip">发送过于频繁，请稍后再试</div>
        <div class="chat-input-row">
          <input
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息..."
            maxlength="500"
            :disabled="aiStore.loading || aiStore.rateLimited"
            @keydown="handleKeydown"
          />
          <button
            class="chat-send-btn"
            :disabled="!aiStore.canSend || !inputText.trim()"
            @click="handleSend"
          >
            发送
          </button>
        </div>
        <div class="char-count">{{ charCount }}/500</div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.chat-panel {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 1001;
  overflow: hidden;
  transform-origin: bottom right;
}

/* Open/close animation */
.chat-panel-enter-active,
.chat-panel-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.chat-panel-enter-from,
.chat-panel-leave-to {
  transform: scale(0);
  opacity: 0;
}

/* Header */
.chat-header {
  height: 44px;
  background: var(--sf-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;
}
.chat-title {
  font-size: 15px;
  font-weight: 600;
}
.chat-close {
  background: none;
  border: none;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1;
}
.chat-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-row {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}
.msg-user {
  align-self: flex-end;
  align-items: flex-end;
}
.msg-ai {
  align-self: flex-start;
  align-items: flex-start;
}
.msg-error {
  align-self: flex-start;
  align-items: flex-start;
}

.msg-bubble {
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.msg-user .msg-bubble {
  background: var(--sf-primary-light);
  color: #fff;
}
.msg-ai .msg-bubble {
  background: #f5f5f5;
  color: var(--sf-text);
}
.msg-error .msg-bubble {
  background: #fff;
  color: var(--sf-danger);
  border: 1px solid var(--sf-danger);
}
.msg-loading {
  color: var(--sf-text-secondary);
  font-style: italic;
}

.msg-time {
  font-size: 11px;
  color: var(--sf-text-secondary);
  margin-top: 2px;
  padding: 0 4px;
}

/* Confirm keyword highlight */
:deep(.confirm-highlight) {
  background: #FFF9C4;
  padding: 1px 4px;
  border-radius: 3px;
  font-weight: 600;
}

/* Input area */
.chat-input-area {
  height: 56px;
  border-top: 1px solid var(--sf-border);
  padding: 8px 12px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.rate-limit-tip {
  font-size: 12px;
  color: var(--sf-danger);
  margin-bottom: 4px;
}
.chat-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.chat-input {
  flex: 1;
  border: 1px solid var(--sf-border);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
}
.chat-input:focus {
  border-color: var(--sf-primary-light);
}
.chat-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}
.chat-send-btn {
  background: var(--sf-primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.chat-send-btn:hover:not(:disabled) {
  background: var(--sf-primary-dark);
}
.chat-send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.char-count {
  font-size: 11px;
  color: var(--sf-text-secondary);
  text-align: right;
  margin-top: 2px;
}
</style>
