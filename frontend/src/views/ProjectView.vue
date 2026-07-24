<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api } from "../api";
import type { Project } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<Project[]>([]),
  dialog = ref(false),
  router = useRouter(),
  auth = useAuthStore(),
  form = reactive<any>({
    name: "",
    code: "",
    status: "planning",
    description: "",
  });
async function load() {
  rows.value = (await api.projects()).items;
}
onMounted(load);
function edit(row?: Project) {
  Object.assign(
    form,
    row ?? {
      id: undefined,
      name: "",
      code: "",
      status: "planning",
      description: "",
    },
  );
  dialog.value = true;
}
async function save() {
  await api.saveProject(form);
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
        ><el-table-column v-if="auth.isManager" label="操作" width="150"
          ><template #default="s"
            ><el-button link type="primary" @click.stop="edit(s.row)"
              >编辑</el-button
            ><el-button link type="danger" @click.stop="remove(s.row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
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
            type="textarea" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
      ></el-dialog
    >
  </div>
</template>
