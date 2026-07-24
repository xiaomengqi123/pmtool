<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api } from "../api";
import type { Project } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<Project[]>([]),
  keyword = ref(""),
  status = ref(""),
  page = ref(1),
  total = ref(0),
  pageSize = 20,
  dialog = ref(false),
  router = useRouter(),
  auth = useAuthStore(),
  form = reactive<any>({
    name: "",
    code: "",
    status: "planning",
    description: "",
    startDate: null,
    endDate: null,
  });
async function load() {
  const result = await api.projects(page.value, pageSize, {
    keyword: keyword.value,
    status: status.value,
  });
  rows.value = result.items;
  total.value = result.total;
}
function search() {
  page.value = 1;
  load();
}
onMounted(load);
function canManageProject(project: Project) {
  return auth.isAdmin || project.managerId === auth.user?.id;
}
function edit(row?: Project) {
  Object.assign(
    form,
    row ?? {
      id: undefined,
      name: "",
      code: "",
      status: "planning",
      description: "",
      startDate: null,
      endDate: null,
    },
  );
  dialog.value = true;
}
async function save() {
  await api.saveProject({
    ...form,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
  });
  dialog.value = false;
  ElMessage.success("项目已保存");
  load();
}
async function remove(row: Project) {
  await ElMessageBox.confirm(
    `删除项目“${row.name}”？项目数据将进入软删除状态。`,
    "确认删除",
    { type: "warning" },
  );
  await api.deleteProject(row.id);
  ElMessage.success("项目已删除");
  load();
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">项目管理</h1>
      <el-button v-if="auth.isManager" type="primary" @click="edit()"
        >新建项目</el-button
      >
    </div>
    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索项目名称或编码"
          style="width: 240px"
          @keyup.enter="search" />
        <el-select v-model="status" clearable placeholder="项目状态" style="width: 150px">
          <el-option label="规划中" value="planning" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="暂停" value="paused" />
          <el-option label="完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <el-table
        :data="rows"
        @row-click="(r: Project) => router.push(`/projects/${r.id}`)"
        ><el-table-column prop="name" label="项目" /><el-table-column
          prop="code"
          label="编码"
        /><el-table-column prop="status" label="状态" /><el-table-column
          label="进度"
          ><template #default="s"
            ><el-progress
              :percentage="
                Number(s.row.progress)
              " /></template></el-table-column
        ><el-table-column v-if="rows.some(canManageProject)" label="操作" width="150"
          ><template #default="s"
            ><template v-if="canManageProject(s.row)"><el-button link type="primary" @click.stop="edit(s.row)"
              >编辑</el-button
            ><el-button link type="danger" @click.stop="remove(s.row)"
              >删除</el-button></template
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
    <el-dialog v-model="dialog" title="项目"
      ><el-form label-width="90"
        ><el-form-item label="项目名称"
          ><el-input v-model="form.name" /></el-form-item
        ><el-form-item label="项目编码"
          ><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item
        ><el-form-item label="状态"
          ><el-select v-model="form.status"
            ><el-option label="规划中" value="planning" /><el-option
              label="进行中"
              value="in_progress" /><el-option
              label="暂停"
              value="paused" /><el-option
              label="完成"
              value="completed" /><el-option
              label="已取消"
              value="cancelled" /></el-select></el-form-item
        ><el-form-item label="说明"
          ><el-input
            v-model="form.description"
            type="textarea" /></el-form-item
        ><el-form-item label="计划周期"
          ><el-date-picker
            v-model="form.startDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="开始时间" /><el-date-picker
            v-model="form.endDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="结束时间" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
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
