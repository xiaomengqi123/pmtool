<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api } from "../api";
const data = ref<any>({
  projectTotal: 0,
  taskTotal: 0,
  taskDone: 0,
  taskCompletion: 0,
  recentProjects: [],
  recentTasks: [],
  statusDistribution: {},
  worklogTrend: [],
  memberLoad: [],
  recentMilestones: [],
});
onMounted(async () => {
  data.value = await api.dashboard();
});
const maxHours = () =>
  Math.max(1, ...data.value.worklogTrend.map((x: any) => Number(x.hours)));
</script>
<template>
  <div class="page">
    <div class="page-header"><h1 class="page-title">工作台</h1></div>
    <div class="stat-grid">
      <div class="stat">
        项目总数<strong>{{ data.projectTotal }}</strong>
      </div>
      <div class="stat">
        任务总数<strong>{{ data.taskTotal }}</strong>
      </div>
      <div class="stat">
        已完成任务<strong>{{ data.taskDone }}</strong>
      </div>
      <div class="stat">
        任务完成率<strong>{{ data.taskCompletion }}%</strong>
      </div>
    </div>
    <div class="two-col">
      <section class="card">
        <h3>任务状态分布</h3>
        <div
          v-for="(count, status) in data.statusDistribution"
          :key="String(status)"
          class="distribution"
        >
          <span>{{ status }}</span
          ><el-progress
            :percentage="
              data.taskTotal
                ? Math.round((Number(count) / data.taskTotal) * 100)
                : 0
            "
          /><b>{{ count }}</b>
        </div>
      </section>
      <section class="card">
        <h3>近 7 天已审批工时</h3>
        <div class="trend">
          <div
            v-for="item in data.worklogTrend"
            :key="item.date"
            class="trend-item"
          >
            <span
              class="bar"
              :style="{ height: `${(Number(item.hours) / maxHours()) * 100}%` }"
            /><small>{{ item.date.slice(5) }}</small
            ><b>{{ item.hours }}</b>
          </div>
        </div>
      </section>
    </div>
    <div class="two-col">
      <section class="card">
        <h3>成员负载</h3>
        <el-table :data="data.memberLoad"
          ><el-table-column prop="name" label="成员" /><el-table-column
            prop="taskCount"
            label="任务数"
        /></el-table>
      </section>
      <section class="card">
        <h3>近期里程碑</h3>
        <el-table :data="data.recentMilestones"
          ><el-table-column prop="name" label="里程碑" /><el-table-column
            prop="dueDate"
            label="截止时间" /><el-table-column prop="status" label="状态"
        /></el-table>
      </section>
    </div>
    <section class="card">
      <h3>近期任务</h3>
      <el-table :data="data.recentTasks"
        ><el-table-column prop="title" label="任务" /><el-table-column
          prop="projectId"
          label="项目" /><el-table-column
          prop="status"
          label="状态" /><el-table-column prop="progress" label="进度"
          ><template #default="{ row }"
            ><el-progress
              :percentage="Number(row.progress)" /></template></el-table-column
      ></el-table>
    </section>
    <section class="card">
      <h3>最近项目</h3>
      <el-table :data="data.recentProjects"
        ><el-table-column prop="name" label="项目" /><el-table-column
          prop="code"
          label="编码" /><el-table-column
          prop="status"
          label="状态" /><el-table-column prop="progress" label="进度"
          ><template #default="{ row }"
            ><el-progress
              :percentage="Number(row.progress)" /></template></el-table-column
      ></el-table>
    </section>
  </div>
</template>
<style scoped>
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}
.distribution {
  display: grid;
  grid-template-columns: 100px 1fr 35px;
  gap: 10px;
  align-items: center;
  margin: 12px 0;
}
.trend {
  height: 175px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 12px;
}
.trend-item {
  height: 145px;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
}
.bar {
  width: 28px;
  min-height: 3px;
  background: #009982;
  border-radius: 4px 4px 0 0;
}
.trend-item b {
  font-size: 12px;
}
</style>
