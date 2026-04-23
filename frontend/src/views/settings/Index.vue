<template>
  <el-card header="系统设置">
    <el-form label-width="140px" style="max-width:600px">
      <el-divider content-position="left">MQTT 配置</el-divider>
      <el-form-item label="Broker 地址"><el-input v-model="config.mqttBroker" disabled /><template #label><span>Broker 地址 <el-tag size="small" type="success">已连接</el-tag></span></template></el-form-item>
      <el-form-item label="Client ID"><el-input v-model="config.mqttClientId" disabled /></el-form-item>
      <el-divider content-position="left">告警阈值</el-divider>
      <el-form-item label="土壤湿度下限(%)"><el-input-number v-model="config.moistureLow" :min="0" :max="100" /></el-form-item>
      <el-form-item label="土壤湿度上限(%)"><el-input-number v-model="config.moistureHigh" :min="0" :max="100" /></el-form-item>
      <el-form-item label="温度下限(°C)"><el-input-number v-model="config.tempLow" /></el-form-item>
      <el-form-item label="温度上限(°C)"><el-input-number v-model="config.tempHigh" /></el-form-item>
      <el-divider content-position="left">微信通知</el-divider>
      <el-form-item label="启用微信通知"><el-switch v-model="config.wechatEnabled" /></el-form-item>
      <el-form-item label="免打扰时段">
        <el-time-picker v-model="config.quietStart" placeholder="开始" style="width:120px" /> —
        <el-time-picker v-model="config.quietEnd" placeholder="结束" style="width:120px" />
      </el-form-item>
      <el-form-item><el-button type="primary">保存设置</el-button></el-form-item>
    </el-form>
  </el-card>
</template>
<script setup>
import { ref } from 'vue'
const config = ref({
  mqttBroker: 'tcp://localhost:1883', mqttClientId: 'smartfarm-server',
  moistureLow: 20, moistureHigh: 90, tempLow: 5, tempHigh: 40,
  wechatEnabled: false, quietStart: null, quietEnd: null,
})
</script>
