<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import type { Task } from '../types'
const rows = ref<Task[]>([])
const router = useRouter()
async function load() { rows.value = (await api.tasks()).items }
onMounted(load)
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">任务管理</h1></div><section class="card"><el-table :data="rows"><el-table-column prop="title" label="任务"/><el-table-column prop="projectId" label="项目"><template #default="s"><el-button link type="primary" @click="router.push(`/projects/${s.row.projectId}`)">#{{s.row.projectId}}</el-button></template></el-table-column><el-table-column prop="status" label="状态"/><el-table-column prop="priority" label="优先级"/><el-table-column label="进度"><template #default="s"><el-progress :percentage="Number(s.row.progress)"/></template></el-table-column></el-table></section></div></template>
