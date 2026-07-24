<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'
const data = ref<any>({ projectTotal: 0, taskTotal: 0, taskDone: 0, taskCompletion: 0, recentProjects: [] })
onMounted(async () => { data.value = await api.dashboard() })
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">工作台</h1></div><div class="stat-grid"><div class="stat">项目总数<strong>{{data.projectTotal}}</strong></div><div class="stat">任务总数<strong>{{data.taskTotal}}</strong></div><div class="stat">已完成任务<strong>{{data.taskDone}}</strong></div><div class="stat">任务完成率<strong>{{data.taskCompletion}}%</strong></div></div><section class="card" style="margin-top:16px"><h3>最近项目</h3><el-table :data="data.recentProjects"><el-table-column prop="name" label="项目"/><el-table-column prop="code" label="编码"/><el-table-column prop="status" label="状态"/><el-table-column prop="progress" label="进度"><template #default="s"><el-progress :percentage="Number(s.row.progress)"/></template></el-table-column></el-table></section></div></template>
