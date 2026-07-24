<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import type { Project, Task } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage } from "element-plus";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id)),
  project = ref<Project>(),
  tasks = ref<Task[]>([]);
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
}
onMounted(load);
async function move(index: number, delta: number) {
  const next = index + delta;
  if (next < 0 || next >= tasks.value.length) return;
  const [task] = tasks.value.splice(index, 1);
  tasks.value.splice(next, 0, task);
  await api.reorderTasks(
    projectId.value,
    tasks.value.map((t) => t.id),
  );
  ElMessage.success("排序已更新");
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目看板排序</h1>
        <p>拖拽排序可后续扩展；当前支持逐项上下调整。</p>
      </div>
    </div>
    <section class="card">
      <el-table :data="tasks"
        ><el-table-column type="index" label="#" width="60" /><el-table-column
          prop="title"
          label="任务"
        /><el-table-column prop="status" label="状态" /><el-table-column
          prop="sortOrder"
          label="排序"
        /><el-table-column v-if="canManageProject" label="调整" width="140"
          ><template #default="{ row, $index }"
            ><el-button link :disabled="$index === 0" @click="move($index, -1)"
              >上移</el-button
            ><el-button
              link
              :disabled="$index === tasks.length - 1"
              @click="move($index, 1)"
              >下移</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
  </div>
</template>
