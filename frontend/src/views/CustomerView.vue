<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../api";
import type { Customer } from "../types";
import { ElMessage, ElMessageBox } from "element-plus";
const rows = ref<Customer[]>([]),
  dialog = ref(false),
  detailDialog = ref(false),
  contactDialog = ref(false),
  followUpDialog = ref(false),
  current = ref<Customer | null>(null),
  contacts = ref<any[]>([]),
  followUps = ref<any[]>([]),
  form = reactive<Partial<Customer>>({
    name: "",
    level: "normal",
    status: "active",
    contactName: "",
    phone: "",
  }),
  contactForm = reactive<any>({
    name: "",
    positionName: "",
    phone: "",
    email: "",
  }),
  followUpForm = reactive<any>({ content: "", followUpAt: "" });
async function load() {
  rows.value = (await api.customers()).items;
}
onMounted(load);
function edit(row?: Customer) {
  Object.assign(
    form,
    row ?? {
      id: undefined,
      name: "",
      level: "normal",
      status: "active",
      contactName: "",
      phone: "",
    },
  );
  dialog.value = true;
}
async function save() {
  await api.saveCustomer(form);
  ElMessage.success("已保存");
  dialog.value = false;
  load();
}
async function remove(row: Customer) {
  await ElMessageBox.confirm(`删除客户“${row.name}”？`, "确认删除", {
    type: "warning",
  });
  await api.deleteCustomer(row.id);
  ElMessage.success("客户已删除");
  load();
}
async function detail(row: Customer) {
  current.value = row;
  await loadDetails();
  detailDialog.value = true;
}
async function loadDetails() {
  if (!current.value) return;
  [contacts.value, followUps.value] = await Promise.all([
    api.contacts(current.value.id),
    api.followUps(current.value.id),
  ]);
}
function editContact(item?: any) {
  Object.assign(
    contactForm,
    item ?? { id: undefined, name: "", positionName: "", phone: "", email: "" },
  );
  contactDialog.value = true;
}
async function saveContact() {
  if (!current.value) return;
  await api.saveContact(current.value.id, contactForm);
  contactDialog.value = false;
  ElMessage.success("联系人已保存");
  loadDetails();
}
async function removeContact(item: any) {
  if (!current.value) return;
  await ElMessageBox.confirm(`删除联系人“${item.name}”？`, "确认", {
    type: "warning",
  });
  await api.deleteContact(current.value.id, item.id);
  ElMessage.success("已删除");
  loadDetails();
}
function addFollowUp() {
  Object.assign(followUpForm, { content: "", followUpAt: "" });
  followUpDialog.value = true;
}
async function saveFollowUp() {
  if (!current.value) return;
  await api.saveFollowUp(current.value.id, followUpForm);
  followUpDialog.value = false;
  ElMessage.success("跟进记录已保存");
  loadDetails();
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">客户管理</h1>
      <el-button type="primary" @click="edit()">新增客户</el-button>
    </div>
    <section class="card">
      <el-table :data="rows"
        ><el-table-column prop="name" label="客户名称" /><el-table-column
          prop="level"
          label="等级"
        /><el-table-column prop="contactName" label="联系人" /><el-table-column
          prop="phone"
          label="电话"
        /><el-table-column label="操作" width="210"
          ><template #default="s"
            ><el-button link type="primary" @click="detail(s.row)"
              >详情</el-button
            ><el-button link type="primary" @click="edit(s.row)">编辑</el-button
            ><el-button link type="danger" @click="remove(s.row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <el-dialog v-model="dialog" title="客户"
      ><el-form label-width="80"
        ><el-form-item label="名称"
          ><el-input v-model="form.name" /></el-form-item
        ><el-form-item label="等级"
          ><el-select v-model="form.level"
            ><el-option label="普通" value="normal" /><el-option
              label="重要"
              value="important" /><el-option
              label="重点"
              value="vip" /></el-select></el-form-item
        ><el-form-item label="联系人"
          ><el-input v-model="form.contactName" /></el-form-item
        ><el-form-item label="电话"
          ><el-input v-model="form.phone" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
      ></el-dialog
    ><el-dialog
      v-model="detailDialog"
      :title="`${current?.name || ''} · 客户详情`"
      width="820"
      ><el-tabs
        ><el-tab-pane label="联系人"
          ><div class="toolbar">
            <el-button type="primary" size="small" @click="editContact()"
              >新增联系人</el-button
            >
          </div>
          <el-table :data="contacts"
            ><el-table-column prop="name" label="姓名" /><el-table-column
              prop="positionName"
              label="职位"
            /><el-table-column prop="phone" label="电话" /><el-table-column
              prop="email"
              label="邮箱"
            /><el-table-column label="操作" width="120"
              ><template #default="{ row }"
                ><el-button link type="primary" @click="editContact(row)"
                  >编辑</el-button
                ><el-button link type="danger" @click="removeContact(row)"
                  >删除</el-button
                ></template
              ></el-table-column
            ></el-table
          ></el-tab-pane
        ><el-tab-pane label="跟进记录"
          ><div class="toolbar">
            <el-button type="primary" size="small" @click="addFollowUp()"
              >新增跟进</el-button
            >
          </div>
          <el-timeline
            ><el-timeline-item
              v-for="item in followUps"
              :key="item.id"
              :timestamp="item.followUpAt"
              >{{ item.content }}</el-timeline-item
            ></el-timeline
          ><el-empty
            v-if="!followUps.length"
            description="暂无跟进记录"
            :image-size="56" /></el-tab-pane></el-tabs></el-dialog
    ><el-dialog v-model="contactDialog" title="联系人" width="460"
      ><el-form label-width="80"
        ><el-form-item label="姓名"
          ><el-input v-model="contactForm.name" /></el-form-item
        ><el-form-item label="职位"
          ><el-input v-model="contactForm.positionName" /></el-form-item
        ><el-form-item label="电话"
          ><el-input v-model="contactForm.phone" /></el-form-item
        ><el-form-item label="邮箱"
          ><el-input v-model="contactForm.email" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="contactDialog = false">取消</el-button
        ><el-button type="primary" @click="saveContact"
          >保存</el-button
        ></template
      ></el-dialog
    ><el-dialog v-model="followUpDialog" title="新增跟进" width="500"
      ><el-form label-width="80"
        ><el-form-item label="时间"
          ><el-date-picker
            v-model="followUpForm.followUpAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item
        ><el-form-item label="内容"
          ><el-input
            v-model="followUpForm.content"
            type="textarea"
            :rows="4" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="followUpDialog = false">取消</el-button
        ><el-button type="primary" @click="saveFollowUp"
          >保存</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
