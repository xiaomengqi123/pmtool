<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import { ElMessage, ElMessageBox } from "element-plus";
import { useAuthStore } from "../stores/auth";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id)),
  rows = ref<any[]>([]);
async function load() {
  rows.value = await api.attachments("project", projectId.value);
}
onMounted(load);
async function upload(options: any) {
  try {
    await api.uploadAttachment("project", projectId.value, options.file);
    options.onSuccess();
    ElMessage.success("上传成功");
    load();
  } catch (error) {
    options.onError(error);
  }
}
async function download(row: any) {
  const blob = await api.downloadAttachment(row.id);
  const url = URL.createObjectURL(blob),
    link = document.createElement("a");
  link.href = url;
  link.download = row.name;
  link.click();
  URL.revokeObjectURL(url);
}
async function remove(row: any) {
  await ElMessageBox.confirm(`删除附件“${row.name}”？`, "确认", {
    type: "warning",
  });
  await api.deleteAttachment(row.id);
  ElMessage.success("已删除");
  load();
}
function canDelete(row: any) {
  return auth.isManager || row.uploaderId === auth.user?.id;
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目附件</h1>
        <p>任意文件类型，单文件最大 2 MB。</p>
      </div>
      <el-upload :show-file-list="false" :http-request="upload"
        ><el-button type="primary">上传附件</el-button></el-upload
      >
    </div>
    <section class="card">
      <el-empty v-if="!rows.length" description="暂无附件" /><el-table
        v-else
        :data="rows"
        ><el-table-column prop="name" label="文件名" /><el-table-column
          prop="contentType"
          label="类型"
        /><el-table-column prop="size" label="大小"
          ><template #default="{ row }"
            >{{ (row.size / 1024).toFixed(1) }} KB</template
          ></el-table-column
        ><el-table-column prop="createdAt" label="上传时间" /><el-table-column
          label="操作"
          width="130"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="download(row)"
              >下载</el-button
            ><el-button
              v-if="canDelete(row)"
              link
              type="danger"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
  </div>
</template>
