<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../api";
import type { Department, Role } from "../types";
import { ElMessage, ElMessageBox } from "element-plus";
const departments = ref<Department[]>([]),
  roles = ref<Role[]>([]),
  dialog = ref(false),
  form = reactive<Partial<Department>>({ name: "", parentId: 0 });
async function load() {
  [departments.value, roles.value] = await Promise.all([
    api.departments(),
    api.roles(),
  ]);
}
onMounted(load);
function edit(item?: Department) {
  Object.assign(form, item ?? { id: undefined, name: "", parentId: 0 });
  dialog.value = true;
}
async function save() {
  await api.saveDepartment({ ...form, parentId: form.parentId || null } as any);
  dialog.value = false;
  ElMessage.success("部门已保存");
  load();
}
async function remove(item: Department) {
  await ElMessageBox.confirm(`删除部门“${item.name}”？`, "确认删除", {
    type: "warning",
  });
  await api.deleteDepartment(item.id);
  ElMessage.success("已删除");
  load();
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">组织与角色</h1>
      <el-button type="primary" @click="edit()">新增部门</el-button>
    </div>
    <section class="card">
      <h3>部门管理</h3>
      <el-table :data="departments"
        ><el-table-column prop="name" label="部门名称" /><el-table-column
          label="上级部门"
          ><template #default="{ row }">{{
            departments.find((d) => d.id === row.parentId)?.name || "-"
          }}</template></el-table-column
        ><el-table-column label="操作" width="130"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="edit(row)">编辑</el-button
            ><el-button link type="danger" @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <section class="card">
      <h3>系统角色</h3>
      <el-table :data="roles"
        ><el-table-column prop="name" label="角色" /><el-table-column
          prop="code"
          label="编码"
        /><el-table-column
          prop="description"
          label="权限范围"
        /><el-table-column label="权限编码"
          ><template #default="{ row }">{{
            row.permissions?.join(", ") || "-"
          }}</template></el-table-column
        ></el-table
      >
    </section>
    <el-dialog v-model="dialog" title="部门"
      ><el-form label-width="90"
        ><el-form-item label="名称"
          ><el-input v-model="form.name" /></el-form-item
        ><el-form-item label="上级部门"
          ><el-select v-model="form.parentId" clearable
            ><el-option label="无" :value="0" /><el-option
              v-for="d in departments.filter((d) => d.id !== form.id)"
              :key="d.id"
              :label="d.name"
              :value="d.id" /></el-select></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
      ></el-dialog
    >
  </div>
</template>
