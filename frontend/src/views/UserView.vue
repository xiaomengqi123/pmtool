<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../api";
import type { User } from "../types";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<User[]>([]),
  keyword = ref(""),
  roleCode = ref(""),
  page = ref(1),
  total = ref(0),
  pageSize = 20,
  dialog = ref(false),
  form = reactive<any>({
    username: "",
    password: "",
    displayName: "",
    roleCode: "MEMBER",
    enabled: true,
  });
async function load() {
  const result = await api.userPage(page.value, pageSize, {
    keyword: keyword.value,
    roleCode: roleCode.value,
  });
  rows.value = result.items;
  total.value = result.total;
}
function search() {
  page.value = 1;
  load();
}
onMounted(load);
function create() {
  Object.assign(form, {
    id: null,
    username: "",
    password: "",
    displayName: "",
    roleCode: "MEMBER",
    enabled: true,
  });
  dialog.value = true;
}
function edit(row: User) {
  Object.assign(form, { ...row, password: "" });
  dialog.value = true;
}
async function save() {
  if (form.id) {
    await api.updateUser(form.id, form);
  } else {
    await api.createUser(form);
  }
  dialog.value = false;
  ElMessage.success("用户已保存");
  load();
}
async function reset(row: User) {
  const result = await ElMessageBox.prompt(
    `为“${row.displayName}”设置新密码`,
    "重置密码",
    {
      inputType: "password",
      inputPattern: /.{8,}/,
      inputErrorMessage: "密码至少 8 位",
    },
  );
  await api.resetUserPassword(row.id, result.value);
  ElMessage.success("密码已重置");
}
async function remove(row: User) {
  await ElMessageBox.confirm(
    `删除用户“${row.displayName}”？该用户将不能再登录。`,
    "确认删除",
    { type: "warning" },
  );
  await api.deleteUser(row.id);
  ElMessage.success("用户已删除");
  load();
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">人员管理</h1>
      <el-button type="primary" @click="create">新增用户</el-button>
    </div>
    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索账号或姓名"
          style="width: 220px"
          @keyup.enter="search" />
        <el-select v-model="roleCode" clearable placeholder="角色" style="width: 150px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="项目经理" value="PM" />
          <el-option label="成员" value="MEMBER" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <el-table :data="rows"
        ><el-table-column prop="username" label="账号" /><el-table-column
          prop="displayName"
          label="姓名"
        /><el-table-column prop="roleCode" label="角色" /><el-table-column
          prop="enabled"
          label="状态"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'">{{
              row.enabled ? "启用" : "停用"
            }}</el-tag></template
          ></el-table-column
        ><el-table-column label="操作" width="200"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="edit(row)">编辑</el-button
            ><el-button link type="warning" @click="reset(row)"
              >重置密码</el-button
            ><el-button link type="danger" @click="remove(row)"
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
    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'"
      ><el-form label-width="80"
        ><el-form-item label="账号"
          ><el-input
            v-model="form.username"
            :disabled="!!form.id" /></el-form-item
        ><el-form-item v-if="!form.id" label="密码"
          ><el-input v-model="form.password" type="password" /></el-form-item
        ><el-form-item label="姓名"
          ><el-input v-model="form.displayName" /></el-form-item
        ><el-form-item label="角色"
          ><el-select v-model="form.roleCode"
            ><el-option label="管理员" value="ADMIN" /><el-option
              label="项目经理"
              value="PM" /><el-option
              label="成员"
              value="MEMBER" /></el-select></el-form-item
        ><el-form-item v-if="form.id" label="状态"
          ><el-switch
            v-model="form.enabled"
            active-text="启用"
            inactive-text="停用" /></el-form-item></el-form
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
