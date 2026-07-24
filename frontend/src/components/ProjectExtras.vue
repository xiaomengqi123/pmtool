<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api } from "../api";
import { ElMessage, ElMessageBox } from "element-plus";
const props = defineProps<{ projectId: number; isManager: boolean }>(),
  documents = ref<any[]>([]),
  risks = ref<any[]>([]),
  users = ref<any[]>([]),
  documentDialog = ref(false),
  riskDialog = ref(false),
  documentForm = reactive<any>({ title: "", content: "" }),
  riskForm = reactive<any>({
    title: "",
    level: "medium",
    status: "open",
    ownerId: null,
  });
async function load() {
  [documents.value, risks.value, users.value] = await Promise.all([
    api.documents(props.projectId),
    api.risks(props.projectId),
    api.users(),
  ]);
}
onMounted(load);
function editDocument(item?: any) {
  Object.assign(
    documentForm,
    item ?? { id: undefined, title: "", content: "" },
  );
  documentDialog.value = true;
}
async function saveDocument() {
  await api.saveDocument(props.projectId, documentForm);
  documentDialog.value = false;
  ElMessage.success("项目文档已保存");
  load();
}
async function removeDocument(item: any) {
  await ElMessageBox.confirm(`删除文档“${item.title}”？`, "确认", {
    type: "warning",
  });
  await api.deleteDocument(props.projectId, item.id);
  ElMessage.success("已删除");
  load();
}
function editRisk(item?: any) {
  Object.assign(
    riskForm,
    item ?? {
      id: undefined,
      title: "",
      level: "medium",
      status: "open",
      ownerId: null,
    },
  );
  riskDialog.value = true;
}
async function saveRisk() {
  await api.saveRisk(props.projectId, riskForm);
  riskDialog.value = false;
  ElMessage.success("风险已保存");
  load();
}
async function removeRisk(item: any) {
  await ElMessageBox.confirm(`删除风险“${item.title}”？`, "确认", {
    type: "warning",
  });
  await api.deleteRisk(props.projectId, item.id);
  ElMessage.success("已删除");
  load();
}
</script>
<template>
  <section class="card">
    <div class="title">
      <h3>项目文档</h3>
      <el-button
        v-if="isManager"
        type="primary"
        size="small"
        @click="editDocument()"
        >新增文档</el-button
      >
    </div>
    <el-empty
      v-if="!documents.length"
      description="暂无项目文档"
      :image-size="50"
    /><el-table v-else :data="documents"
      ><el-table-column prop="title" label="标题" /><el-table-column
        prop="content"
        label="内容"
        show-overflow-tooltip
      /><el-table-column v-if="isManager" label="操作" width="130"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="editDocument(row)"
            >编辑</el-button
          ><el-button link type="danger" @click="removeDocument(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </section>
  <section class="card">
    <div class="title">
      <h3>项目风险</h3>
      <el-button
        v-if="isManager"
        type="primary"
        size="small"
        @click="editRisk()"
        >新增风险</el-button
      >
    </div>
    <el-empty
      v-if="!risks.length"
      description="暂无项目风险"
      :image-size="50"
    /><el-table v-else :data="risks"
      ><el-table-column prop="title" label="风险" /><el-table-column
        prop="level"
        label="等级"
        width="100"
      /><el-table-column
        prop="status"
        label="状态"
        width="120"
      /><el-table-column prop="ownerId" label="负责人" width="100"
        ><template #default="{ row }">{{
          users.find((u) => u.id === row.ownerId)?.displayName || "-"
        }}</template></el-table-column
      ><el-table-column v-if="isManager" label="操作" width="130"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="editRisk(row)">编辑</el-button
          ><el-button link type="danger" @click="removeRisk(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </section>
  <el-dialog v-model="documentDialog" title="项目文档" width="560"
    ><el-form label-width="70"
      ><el-form-item label="标题"
        ><el-input v-model="documentForm.title" /></el-form-item
      ><el-form-item label="内容"
        ><el-input
          v-model="documentForm.content"
          type="textarea"
          :rows="6" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="documentDialog = false">取消</el-button
      ><el-button type="primary" @click="saveDocument"
        >保存</el-button
      ></template
    ></el-dialog
  ><el-dialog v-model="riskDialog" title="项目风险" width="500"
    ><el-form label-width="80"
      ><el-form-item label="风险"
        ><el-input v-model="riskForm.title" /></el-form-item
      ><el-form-item label="等级"
        ><el-select v-model="riskForm.level"
          ><el-option label="低" value="low" /><el-option
            label="中"
            value="medium" /><el-option
            label="高"
            value="high" /></el-select></el-form-item
      ><el-form-item label="状态"
        ><el-select v-model="riskForm.status"
          ><el-option label="开放" value="open" /><el-option
            label="处理中"
            value="mitigating" /><el-option
            label="已关闭"
            value="closed" /></el-select></el-form-item
      ><el-form-item label="负责人"
        ><el-select v-model="riskForm.ownerId" clearable
          ><el-option
            v-for="u in users"
            :key="u.id"
            :label="u.displayName"
            :value="u.id" /></el-select></el-form-item></el-form
    ><template #footer
      ><el-button @click="riskDialog = false">取消</el-button
      ><el-button type="primary" @click="saveRisk">保存</el-button></template
    ></el-dialog
  >
</template>
<style scoped>
.title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
