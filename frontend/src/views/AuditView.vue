<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api } from "../api";
const rows = ref<any[]>([]),
  total = ref(0),
  page = ref(1);
async function load() {
  const result = await api.operationLogs(page.value);
  rows.value = result.items;
  total.value = result.total;
}
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">操作审计</h1>
        <p>系统自动保留最近 180 天的操作记录。</p>
      </div>
    </div>
    <section class="card">
      <el-table :data="rows"
        ><el-table-column
          prop="createdAt"
          label="时间"
          width="190" /><el-table-column
          prop="userName"
          label="操作人"
          width="120" /><el-table-column
          prop="action"
          label="动作"
          width="100" /><el-table-column
          prop="resourceType"
          label="资源类型"
          width="150" /><el-table-column
          prop="resourceId"
          label="资源 ID"
          width="100" /><el-table-column prop="detail" label="详情"
      /></el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </section>
  </div>
</template>
<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
