<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import type { Project, Task } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id)),
  project = ref<Project>(),
  tasks = ref<Task[]>([]),
  selectedId = ref<number>(),
  dependencies = ref<number[]>([]),
  newDependency = ref<number>();
const canManageProject = computed(
  () => auth.isAdmin || project.value?.managerId === auth.user?.id,
);
async function load() {
  const [projectData, taskData] = await Promise.all([
    api.project(projectId.value),
    api.projectTasks(projectId.value),
  ]);
  project.value = projectData;
  tasks.value = taskData;
  if (!selectedId.value && tasks.value.length)
    selectedId.value = tasks.value[0].id;
  await loadDependencies();
}
onMounted(load);
async function loadDependencies() {
  dependencies.value = selectedId.value
    ? await api.dependencies(selectedId.value)
    : [];
}
async function add() {
  if (!selectedId.value || !newDependency.value)
    return ElMessage.warning("请选择前置任务");
  await api.addDependency(selectedId.value, newDependency.value);
  newDependency.value = undefined;
  ElMessage.success("依赖已添加");
  loadDependencies();
}
async function remove(id: number) {
  if (!selectedId.value) return;
  await ElMessageBox.confirm("移除此任务依赖？", "确认", { type: "warning" });
  await api.removeDependency(selectedId.value, id);
  ElMessage.success("已移除");
  loadDependencies();
}
function taskName(id: number) {
  return tasks.value.find((t) => t.id === id)?.title || `任务 #${id}`;
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">任务依赖</h1>
        <p>设置前置任务；循环依赖将被系统拒绝。</p>
      </div>
    </div>
    <section class="card">
      <el-form inline
        ><el-form-item label="当前任务"
          ><el-select v-model="selectedId" @change="loadDependencies"
            ><el-option
              v-for="t in tasks"
              :key="t.id"
              :label="t.title"
              :value="t.id" /></el-select></el-form-item
        ><template v-if="canManageProject"
          ><el-form-item label="前置任务"
            ><el-select v-model="newDependency" filterable
              ><el-option
                v-for="t in tasks.filter(
                  (t) => t.id !== selectedId && !dependencies.includes(t.id),
                )"
                :key="t.id"
                :label="t.title"
                :value="t.id" /></el-select></el-form-item
          ><el-button type="primary" @click="add">添加依赖</el-button></template
        ></el-form
      ><el-empty v-if="!selectedId" description="暂无任务" /><el-table
        v-else
        :data="dependencies.map((id) => ({ id, title: taskName(id) }))"
        ><el-table-column
          prop="id"
          label="任务 ID"
          width="120"
        /><el-table-column prop="title" label="前置任务" /><el-table-column
          v-if="canManageProject"
          label="操作"
          width="100"
          ><template #default="{ row }"
            ><el-button link type="danger" @click="remove(row.id)"
              >移除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
  </div>
</template>
