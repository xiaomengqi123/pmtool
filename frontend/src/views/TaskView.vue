<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api } from "../api";
import type { Task, User } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<Task[]>([]),
  users = ref<User[]>([]),
  selected = ref<Task[]>([]),
  bulkStatus = ref(""),
  router = useRouter(),
  auth = useAuthStore(),
  dialog = ref(false),
  form = reactive<Partial<Task>>({});
async function load() {
  const [taskPage, allUsers] = await Promise.all([api.tasks(), api.users()]);
  rows.value = taskPage.items;
  users.value = allUsers;
}
onMounted(load);
function edit(task: Task) {
  Object.assign(form, task);
  dialog.value = true;
}
function canUpdateTask(task: Partial<Task>) {
  return auth.isManager || task.assigneeId === auth.user?.id;
}
async function save() {
  await api.saveTask(form);
  dialog.value = false;
  ElMessage.success("任务已保存");
  load();
}
async function batch() {
  if (!selected.value.length || !bulkStatus.value)
    return ElMessage.warning("请选择任务和目标状态");
  await api.batchTaskStatus(
    selected.value.map((t) => t.id),
    bulkStatus.value,
  );
  ElMessage.success("任务状态已批量更新");
  selected.value = [];
  bulkStatus.value = "";
  load();
}
async function remove(task: Task) {
  await ElMessageBox.confirm(
    `删除任务“${task.title}”？相关工时、附件和依赖记录将保留用于审计。`,
    "确认删除",
    { type: "warning" },
  );
  await api.deleteTask(task.id);
  ElMessage.success("任务已删除");
  load();
}
</script>
<template>
  <div class="page">
    <div class="page-header"><h1 class="page-title">任务管理</h1></div>
    <section class="card">
      <div v-if="auth.isManager" class="bulk">
        <span>已选 {{ selected.length }} 项</span
        ><el-select
          v-model="bulkStatus"
          placeholder="批量状态"
          style="width: 140px"
          ><el-option label="待处理" value="todo" /><el-option
            label="进行中"
            value="in_progress" /><el-option
            label="待验收"
            value="review" /><el-option
            label="已完成"
            value="done" /></el-select
        ><el-button type="primary" @click="batch">批量更新</el-button>
      </div>
      <el-table :data="rows" @selection-change="selected = $event"
        ><el-table-column
          v-if="auth.isManager"
          type="selection"
          width="50"
        /><el-table-column prop="title" label="任务" /><el-table-column
          prop="projectId"
          label="项目"
          ><template #default="{ row }"
            ><el-button
              link
              type="primary"
              @click="router.push(`/projects/${row.projectId}`)"
              >#{{ row.projectId }}</el-button
            ></template
          ></el-table-column
        ><el-table-column prop="status" label="状态" /><el-table-column
          prop="priority"
          label="优先级"
        /><el-table-column prop="dueDate" label="截止时间" /><el-table-column
          label="进度"
          ><template #default="{ row }"
            ><el-progress
              :percentage="Number(row.progress)" /></template></el-table-column
        ><el-table-column label="操作" width="130"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="edit(row)">详情</el-button
            ><el-button
              v-if="auth.isManager"
              link
              type="danger"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <el-dialog v-model="dialog" title="任务详情" width="600"
      ><el-form label-width="90"
        ><el-form-item label="标题"
          ><el-input
            v-model="form.title"
            :disabled="!auth.isManager" /></el-form-item
        ><el-form-item label="描述"
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            :disabled="!auth.isManager" /></el-form-item
        ><el-form-item label="负责人"
          ><el-select v-model="form.assigneeId" :disabled="!auth.isManager"
            ><el-option
              v-for="user in users"
              :key="user.id"
              :label="user.displayName"
              :value="user.id" /></el-select></el-form-item
        ><el-form-item label="优先级"
          ><el-select v-model="form.priority" :disabled="!auth.isManager"
            ><el-option label="低" value="low" /><el-option
              label="中"
              value="medium" /><el-option label="高" value="high" /><el-option
              label="紧急"
              value="urgent" /></el-select></el-form-item
        ><el-form-item label="开始/截止"
          ><el-date-picker
            v-model="form.startDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled="!auth.isManager" /><el-date-picker
            v-model="form.dueDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled="!auth.isManager" /></el-form-item
        ><el-form-item label="预估工时"
          ><el-input-number
            v-model="form.estimatedHours"
            :min="0"
            :disabled="!auth.isManager" /></el-form-item
        ><el-form-item label="状态"
          ><el-select v-model="form.status"
            :disabled="!canUpdateTask(form)"
            ><el-option label="待处理" value="todo" /><el-option
              label="进行中"
              value="in_progress" /><el-option
              label="待验收"
              value="review" /><el-option
              label="已完成"
              value="done" /></el-select></el-form-item
        ><el-form-item label="进度"
          ><el-slider
            v-model="form.progress"
            :disabled="!canUpdateTask(form)" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button v-if="canUpdateTask(form)" type="primary" @click="save"
          >保存</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.bulk {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
