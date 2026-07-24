<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
import type { Task } from "../types";
const auth = useAuthStore(),
  rows = ref<any[]>([]),
  tasks = ref<Task[]>([]),
  statusFilter = ref(""),
  page = ref(1),
  total = ref(0),
  pageSize = 20,
  dialog = ref(false),
  form = reactive<any>({
    taskId: null,
    hours: 1,
    workDate: new Date().toISOString().slice(0, 10),
    description: "",
    version: null,
  });
async function load() {
  const [logs, taskPage] = await Promise.all([
    api.workLogs(page.value, pageSize, { status: statusFilter.value }),
    api.tasks(1, 100),
  ]);
  rows.value = logs.items;
  tasks.value = taskPage.items;
  total.value = logs.total;
}
function search() {
  page.value = 1;
  load();
}
onMounted(load);
function create() {
  Object.assign(form, {
    id: null,
    taskId: null,
    hours: 1,
    workDate: new Date().toISOString().slice(0, 10),
    description: "",
    version: null,
  });
  dialog.value = true;
}
function edit(row: any) {
  Object.assign(form, row);
  dialog.value = true;
}
async function save() {
  await api.saveWorkLog(form);
  dialog.value = false;
  ElMessage.success(form.id ? "工时已更新并进入审批" : "已提交");
  load();
}
async function review(id: number, ok: boolean) {
  let comment: string | undefined;
  if (!ok) {
    const result = await ElMessageBox.prompt("请输入驳回原因", "驳回工时", {
      confirmButtonText: "确认驳回",
      inputPattern: /\S+/,
      inputErrorMessage: "驳回原因不能为空",
    });
    comment = result.value;
  }
  await api.review(id, ok, comment);
  ElMessage.success(ok ? "已审批" : "已驳回");
  load();
}
function taskName(id: number) {
  return tasks.value.find((t) => t.id === id)?.title || `任务 #${id}`;
}
function canEdit(row: any) {
  const own = row.userId === auth.user?.id;
  return (
    (own && row.status === "rejected") ||
    (row.canManage && ["rejected", "approved"].includes(row.status))
  );
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">工时管理</h1>
        <p>
          驳回后的工时可修改后重新提交；已审批记录仅管理员或项目经理可修改。
        </p>
      </div>
      <el-button type="primary" @click="create">登记工时</el-button>
    </div>
    <section class="card">
      <div class="toolbar">
        <el-select v-model="statusFilter" clearable placeholder="工时状态" style="width: 150px">
          <el-option label="待审批" value="pending" />
          <el-option label="已审批" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <el-table :data="rows"
        ><el-table-column label="任务" min-width="160"
          ><template #default="{ row }">{{
            taskName(row.taskId)
          }}</template></el-table-column
        ><el-table-column
          prop="hours"
          label="工时"
          width="90"
        /><el-table-column
          prop="workDate"
          label="日期"
          width="120"
        /><el-table-column prop="status" label="状态" width="110"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.status === 'approved'
                  ? 'success'
                  : row.status === 'rejected'
                    ? 'danger'
                    : 'warning'
              "
              >{{ row.status }}</el-tag
            ></template
          ></el-table-column
        ><el-table-column
          prop="reviewComment"
          label="审批说明"
          min-width="160"
        /><el-table-column label="操作" width="190"
          ><template #default="{ row }"
            ><el-button
              v-if="canEdit(row)"
              link
              type="primary"
              @click="edit(row)"
              >{{ row.status === "rejected" ? "修改重提" : "修改" }}</el-button
            ><template v-if="row.canManage && row.status === 'pending'"
              ><el-button link type="success" @click="review(row.id, true)"
                >通过</el-button
              ><el-button link type="danger" @click="review(row.id, false)"
                >驳回</el-button
              ></template
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
    <el-dialog
      v-model="dialog"
      :title="form.id ? '修改工时' : '登记工时'"
      width="500"
      ><el-form label-width="90"
        ><el-form-item label="任务"
          ><el-select v-model="form.taskId" filterable :disabled="!!form.id"
            ><el-option
              v-for="task in tasks"
              :key="task.id"
              :label="task.title"
              :value="task.id" /></el-select></el-form-item
        ><el-form-item label="工时"
          ><el-input-number
            v-model="form.hours"
            :min="0.5"
            :step="0.5" /></el-form-item
        ><el-form-item label="日期"
          ><el-date-picker
            v-model="form.workDate"
            type="date"
            value-format="YYYY-MM-DD" /></el-form-item
        ><el-form-item label="说明"
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="3" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">{{
          form.id ? "保存并重提" : "提交"
        }}</el-button></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
