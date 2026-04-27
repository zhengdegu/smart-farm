<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-icon">🌱</div>
        <h1 class="brand-title">智慧农业平台</h1>
        <p class="brand-subtitle">设备接入 · 智能灌溉 · 数据驱动决策</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">📡</span>
            <div>
              <div class="feature-name">实时监测</div>
              <div class="feature-desc">传感器数据 5 分钟采集，24 小时不间断</div>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">💧</span>
            <div>
              <div class="feature-name">智能灌溉</div>
              <div class="feature-desc">基于土壤湿度自动触发，节水率 30%+</div>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🤖</span>
            <div>
              <div class="feature-name">AI 助手</div>
              <div class="feature-desc">对话式交互，说一句话管理温室</div>
            </div>
          </div>
        </div>
      </div>
      <div class="brand-footer">© 2026 SmartFarm · 让每一滴水都有价值</div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-area">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">登录你的账号，开始管理温室</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-field">
            <label class="field-label">用户名</label>
            <div class="input-wrapper" :class="{ focused: usernameFocused }">
              <span class="input-icon">👤</span>
              <input
                v-model="form.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
                @focus="usernameFocused = true"
                @blur="usernameFocused = false"
              />
            </div>
          </div>

          <div class="form-field">
            <label class="field-label">密码</label>
            <div class="input-wrapper" :class="{ focused: passwordFocused }">
              <span class="input-icon">🔒</span>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                @focus="passwordFocused = true"
                @blur="passwordFocused = false"
              />
              <span class="toggle-password" @click="showPassword = !showPassword">
                {{ showPassword ? '🙈' : '👁' }}
              </span>
            </div>
          </div>

          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="rememberMe" />
              <span>记住我</span>
            </label>
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading">⏳</span>
            <span v-else>登 录</span>
          </button>
        </form>

        <div class="login-footer">
          <span class="demo-hint">演示账号: admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)
const usernameFocused = ref(false)
const passwordFocused = ref(false)

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login(form.value)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', form.value.username)
    router.push('/')
    ElMessage.success('登录成功')
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 左侧品牌区 ===== */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #1B5E20 0%, #2E7D32 40%, #4CAF50 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 70%, rgba(255,255,255,0.08) 0%, transparent 50%);
  animation: brandShimmer 20s ease-in-out infinite;
}

@keyframes brandShimmer {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(5%, -5%); }
}

.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
  max-width: 420px;
}

.brand-icon {
  font-size: 64px;
  margin-bottom: 16px;
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.2));
}

.brand-title {
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.85;
  margin: 0 0 48px;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  transition: background 0.3s;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.18);
}

.feature-icon {
  font-size: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}

.feature-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 13px;
  opacity: 0.75;
  line-height: 1.4;
}

.brand-footer {
  position: absolute;
  bottom: 32px;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  z-index: 1;
}

/* ===== 右侧登录区 ===== */
.login-form-area {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  flex-shrink: 0;
}

.login-card {
  width: 380px;
  padding: 20px;
}

.login-header {
  margin-bottom: 36px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #212121;
  margin: 0 0 8px;
}

.login-desc {
  font-size: 15px;
  color: #757575;
  margin: 0;
}

/* ===== 表单 ===== */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 14px;
  font-weight: 500;
  color: #424242;
}

.input-wrapper {
  display: flex;
  align-items: center;
  border: 2px solid #E0E0E0;
  border-radius: 10px;
  padding: 0 14px;
  height: 48px;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #FAFAFA;
}

.input-wrapper.focused {
  border-color: #2E7D32;
  box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.12);
  background: #fff;
}

.input-icon {
  font-size: 18px;
  margin-right: 10px;
  flex-shrink: 0;
}

.input-wrapper input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #212121;
  background: transparent;
  height: 100%;
}

.input-wrapper input::placeholder {
  color: #BDBDBD;
}

.toggle-password {
  cursor: pointer;
  font-size: 18px;
  margin-left: 8px;
  user-select: none;
}

/* ===== 选项行 ===== */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #757575;
  cursor: pointer;
}

.remember-me input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #2E7D32;
  cursor: pointer;
}

/* ===== 登录按钮 ===== */
.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2E7D32, #4CAF50);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s;
  letter-spacing: 4px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(46, 125, 50, 0.35);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-loading {
  font-size: 18px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 底部 ===== */
.login-footer {
  margin-top: 32px;
  text-align: center;
}

.demo-hint {
  font-size: 13px;
  color: #BDBDBD;
  background: #F5F5F5;
  padding: 6px 16px;
  border-radius: 20px;
}

/* ===== 响应式 ===== */
@media (max-width: 960px) {
  .login-brand {
    display: none;
  }
  .login-form-area {
    width: 100%;
  }
}
</style>
