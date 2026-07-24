<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api } from "../api";
import type { Task, User } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<Task[]>([]),
  keyword = ref(""),
  statusFilter = ref(""),
  page = ref(1),
  total = ref(0),
  pageSize = 20,
  assignees = ref<User[]>([]),
  selected = ref<Task[]>([]),
  bulkStatus = ref(""),
  router = useRouter(),
  auth = useAuthStore(),
  dialog = ref(false),
  form = reactive<Partial<Task>>({});
async function load() {
  const taskPage = await api.tasks(page.value, pageSize, {
    keyword: keyword.value,
    status: statusFilter.value,
  });
  rows.value = taskPage.items;
  total.value = taskPage.total;
}
function search() {
  page.value = 1;
  load();
}
onMounted(load);
async function edit(task: Task) {
  Object.assign(form, task);
  dialog.value = true;
  if (task.canManage) {
    assignees.value = (await api.projectMembers(task.projectId)).map(
      (member: any) => ({
        id: member.userId,
        username: member.username,
        displayName: member.displayName,
        roleCode: member.roleCode,
        departmentId: 0,
        enabled: true,
      }),
    );
  }
}
function canManageTask(task: Partial<Task>) {
  return task.canManage === true;
}
function canUpdateTask(task: Partial<Task>) {
  return canManageTask(task) || task.assigneeId === auth.user?.id;
}
async function save() {
  await api.saveTask({
    ...form,
    startDate: form.startDate || null,
    dueDate: form.dueDate || null,
  });
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
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索任务标题或描述"
          style="width: 240px"
          @keyup.enter="search" />
        <el-select v-model="statusFilter" clearable placeholder="任务状态" style="width: 150px">
          <el-option label="待处理" value="todo" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="待验收" value="review" />
          <el-option label="已完成" value="done" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <div v-if="rows.some(canManageTask)" class="bulk">
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
          v-if="rows.some(canManageTask)"
          type="selection"
          :selectable="canManageTask"
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
              v-if="canManageTask(row)"
              link
              type="danger"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        background
        layout="total, prev, pager, next"
        @current-change="load" />
    </section>
    <el-dialog v-model="dialog" title="任务详情" width="600"
      ><el-form label-width="90"
        ><el-form-item label="标题"
          ><el-input
            v-model="form.title"
            :disabled="!canManageTask(form)" /></el-form-item
        ><el-form-item label="描述"
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            :disabled="!canManageTask(form)" /></el-form-item
        ><el-form-item label="负责人"
          ><el-select v-model="form.assigneeId" :disabled="!canManageTask(form)"
            ><el-option
              v-for="user in assignees"
              :key="user.id"
              :label="user.displayName"
              :value="user.id" /></el-select></el-form-item
        ><el-form-item label="优先级"
          ><el-select v-model="form.priority" :disabled="!canManageTask(form)"
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
            :disabled="!canManageTask(form)" /><el-date-picker
            v-model="form.dueDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled="!canManageTask(form)" /></el-form-item
        ><el-form-item label="预估工时"
          ><el-input-number
            v-model="form.estimatedHours"
            :min="0"
            :disabled="!canManageTask(form)" /></el-form-item
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
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
