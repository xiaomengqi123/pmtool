<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id)),
  members = ref<any[]>([]),
  users = ref<any[]>([]),
  dialog = ref(false),
  form = reactive({ userId: null as number | null, roleCode: "MEMBER" });
async function load() {
  [members.value, users.value] = await Promise.all([
    api.projectMembers(projectId.value),
    auth.isManager ? api.users() : Promise.resolve([]),
  ]);
}
onMounted(load);
function add() {
  form.userId = null;
  form.roleCode = "MEMBER";
  dialog.value = true;
}
async function save() {
  if (!form.userId) return ElMessage.warning("请选择用户");
  await api.addProjectMember(projectId.value, form.userId, form.roleCode);
  dialog.value = false;
  ElMessage.success("成员已添加");
  load();
}
async function remove(row: any) {
  await ElMessageBox.confirm(`移除成员“${row.displayName}”？`, "确认", {
    type: "warning",
  });
  await api.removeProjectMember(projectId.value, row.userId);
  ElMessage.success("成员已移除");
  load();
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目成员</h1>
        <p>项目经理和管理员可维护项目成员。</p>
      </div>
      <el-button v-if="auth.isManager" type="primary" @click="add"
        >添加成员</el-button
      >
    </div>
    <section class="card">
      <el-table :data="members"
        ><el-table-column prop="displayName" label="成员" /><el-table-column
          prop="username"
          label="账号"
        /><el-table-column prop="roleCode" label="项目角色" /><el-table-column
          prop="isManager"
          label="项目经理"
          ><template #default="{ row }"
            ><el-tag v-if="row.isManager" type="success">是</el-tag
            ><span v-else>-</span></template
          ></el-table-column
        ><el-table-column v-if="auth.isManager" label="操作" width="90"
          ><template #default="{ row }"
            ><el-button
              v-if="!row.isManager"
              link
              type="danger"
              @click="remove(row)"
              >移除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <el-dialog v-model="dialog" title="添加项目成员" width="420"
      ><el-form label-width="85"
        ><el-form-item label="用户"
          ><el-select v-model="form.userId" filterable
            ><el-option
              v-for="u in users.filter(
                (u) => !members.some((m) => m.userId === u.id),
              )"
              :key="u.id"
              :label="`${u.displayName}（${u.username}）`"
              :value="u.id" /></el-select></el-form-item
        ><el-form-item label="项目角色"
          ><el-select v-model="form.roleCode"
            ><el-option label="成员" value="MEMBER" /><el-option
              label="项目经理"
              value="PM" /></el-select></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
      ></el-dialog
    >
  </div>
</template>
