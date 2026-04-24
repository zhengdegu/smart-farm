/**
 * AI Helper Functions — Pure logic utilities for AI agent frontend
 * These functions are designed to be testable with property-based testing.
 */

// Patrol type enum mapping
const PATROL_TYPE_MAP = {
  TREND_CHECK: '趋势检查',
  DEVICE_HEALTH: '设备健康',
  DAILY_SUMMARY: '每日摘要',
  IRRIGATION_EVAL: '灌溉评估',
}

export function mapPatrolType(type) {
  return PATROL_TYPE_MAP[type] || type || '未知'
}

// Severity enum mapping (returns { label, color })
const SEVERITY_MAP = {
  INFO: { label: '信息', color: '#1976D2' },
  WARNING: { label: '警告', color: '#F57C00' },
  CRITICAL: { label: '严重', color: '#D32F2F' },
}

export function mapSeverity(severity) {
  return SEVERITY_MAP[severity] || { label: severity || '未知', color: '#757575' }
}

// Suggestion type enum mapping
const SUGGESTION_TYPE_MAP = {
  THRESHOLD: '阈值优化',
  DURATION: '时长优化',
  SCHEDULE: '计划调整',
}

export function mapSuggestionType(type) {
  return SUGGESTION_TYPE_MAP[type] || type || '未知'
}

// Suggestion status enum mapping (returns { label, color })
const SUGGESTION_STATUS_MAP = {
  PENDING: { label: '待审批', color: '#F57C00' },
  ACCEPTED: { label: '已接受', color: '#2E7D32' },
  REJECTED: { label: '已拒绝', color: '#757575' },
}

export function mapSuggestionStatus(status) {
  return SUGGESTION_STATUS_MAP[status] || { label: status || '未知', color: '#757575' }
}

// Action taken enum mapping
const ACTION_TAKEN_MAP = {
  NOTIFIED: '已通知',
  AUTO_EXECUTED: '自动执行',
  IGNORED: '已忽略',
}

export function mapActionTaken(action) {
  return ACTION_TAKEN_MAP[action] || action || '未知'
}

// Generic list filter by field value
export function filterByField(list, field, value) {
  if (!value || value === 'all' || value === '') return list
  return list.filter(item => item[field] === value)
}

// Confirm keywords detection
const CONFIRM_KEYWORDS = ['确认', '确定', '回复', '同意', '执行']

export function hasConfirmKeywords(text) {
  if (!text || typeof text !== 'string') return false
  return CONFIRM_KEYWORDS.some(keyword => text.includes(keyword))
}

// Rate limit check (pure function)
export function checkRateLimit(timestamps, now, windowMs = 60000, maxCount = 10) {
  const recentCount = timestamps.filter(t => now - t < windowMs).length
  return recentCount >= maxCount
}
