<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import type { GanttTask } from "../types";
const route = useRoute(),
  projectId = Number(route.params.id),
  rows = ref<GanttTask[]>([]),
  cursor = ref(new Date());
async function load() {
  rows.value = await api.gantt(projectId);
}
onMounted(load);
const dated = computed(() =>
  rows.value.filter((row) => row.startDate && row.dueDate),
);
const range = computed(() => {
  const values = dated.value.flatMap((row) => [
    new Date(row.startDate).getTime(),
    new Date(row.dueDate).getTime(),
  ]);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return {
    start: values.length
      ? Math.min(...values, today.getTime())
      : today.getTime(),
    end: values.length
      ? Math.max(...values, today.getTime() + 86400000 * 30)
      : today.getTime() + 86400000 * 30,
  };
});
const days = computed(() =>
  Math.max(1, Math.ceil((range.value.end - range.value.start) / 86400000) + 1),
);
function bar(row: GanttTask) {
  if (!row.startDate || !row.dueDate) return { display: "none" };
  const start = new Date(row.startDate).getTime(),
    end = new Date(row.dueDate).getTime();
  return {
    left: `${Math.max(0, ((start - range.value.start) / 86400000 / days.value) * 100)}%`,
    width: `${Math.max(2, ((end - start) / 86400000 / days.value) * 100)}%`,
  };
}
function date(ms: number) {
  return new Date(ms).toLocaleDateString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
  });
}
</script>
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目甘特图</h1>
        <p>任务条由开始日期与截止日期生成；未设排期的任务不显示条形。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <section class="card gantt">
      <div class="gantt-head">
        <span>任务</span>
        <div class="axis">
          <span>{{ date(range.start) }}</span
          ><span>{{ date(range.end) }}</span>
        </div>
      </div>
      <div v-for="row in rows" :key="row.id" class="gantt-row">
        <div class="name">
          <b>{{ row.title }}</b
          ><small>{{
            row.dependsOn.length ? `依赖 ${row.dependsOn.length} 项` : ""
          }}</small>
        </div>
        <div class="track">
          <div class="bar" :class="row.status" :style="bar(row)">
            <span>{{ row.progress }}%</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!rows.length" description="暂无任务" />
    </section>
  </div>
</template>
<style scoped>
.gantt {
  min-width: 900px;
}
.gantt-head,
.gantt-row {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 54px;
  border-bottom: 1px solid #e5e7eb;
}
.gantt-head {
  font-weight: 600;
  background: #f8fafc;
  align-items: center;
}
.gantt-head > span,
.name {
  padding: 10px 14px;
}
.axis {
  display: flex;
  justify-content: space-between;
  padding: 10px 14px;
}
.name {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.name small {
  color: #94a3b8;
}
.track {
  position: relative;
  background: repeating-linear-gradient(
    90deg,
    #fff 0,
    #fff calc(10% - 1px),
    #e2e8f0 calc(10% - 1px),
    #e2e8f0 10%
  );
}
.bar {
  position: absolute;
  top: 15px;
  height: 24px;
  min-width: 32px;
  border-radius: 5px;
  background: #009982;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar.done {
  background: #22c55e;
}
.bar.review {
  background: #f59e0b;
}
.bar.todo {
  background: #94a3b8;
}
</style>
