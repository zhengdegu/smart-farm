<script setup>
import { useAiStore } from '../stores/ai'
const aiStore = useAiStore()
</script>

<template>
  <div v-show="!aiStore.chatOpen" class="floating-btn" @click="aiStore.toggleChat()" title="问问小农">
    🤖
  </div>
</template>

<style scoped>
.floating-btn {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--sf-primary);
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(46, 125, 50, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 1000;
  user-select: none;
}
.floating-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(46, 125, 50, 0.5);
}
</style>
