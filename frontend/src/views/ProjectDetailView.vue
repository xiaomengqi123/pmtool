<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../api";
import type { GanttTask, Milestone, Project, Task, User } from "../types";
import { useAuthStore } from "../stores/auth";
import { ElMessage, ElMessageBox } from "element-plus";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  project = ref<Project | null>(null),
  tasks = ref<Task[]>([]),
  users = ref<User[]>([]),
  milestones = ref<Milestone[]>([]),
  gantt = ref<GanttTask[]>([]),
  attachments = ref<any[]>([]),
  dialog = ref(false),
  milestoneDialog = ref(false),
  form = reactive<Partial<Task>>({
    title: "",
    status: "todo",
    priority: "medium",
    progress: 0,
    estimatedHours: 0,
  }),
  milestoneForm = reactive<Partial<Milestone>>({
    name: "",
    status: "pending",
    dueDate: "",
  });
const id = Number(route.params.id);
const canManageProject = computed(
  () => auth.isAdmin || project.value?.managerId === auth.user?.id,
);
function canUpdateTask(task: Pick<Task, "assigneeId">) {
  return canManageProject.value || task.assigneeId === auth.user?.id;
}
async function load() {
  const [projectData, taskData, memberData, milestoneData, ganttData, attachmentData] = await Promise.all([
    api.project(id),
    api.projectTasks(id),
    api.projectMembers(id),
    api.milestones(id),
    api.gantt(id),
    api.attachments("project", id),
  ]);
  project.value = projectData;
  tasks.value = taskData;
  users.value = memberData.map((member: any) => ({
    id: member.userId,
    username: member.username,
    displayName: member.displayName,
    roleCode: member.roleCode,
    departmentId: 0,
    enabled: true,
  }));
  milestones.value = milestoneData;
  gantt.value = ganttData;
  attachments.value = attachmentData;
}
onMounted(load);
function edit(t?: Task) {
  Object.assign(
    form,
    t ?? {
      id: undefined,
      projectId: id,
      title: "",
      status: "todo",
      priority: "medium",
      progress: 0,
      estimatedHours: 0,
    },
  );
  dialog.value = true;
}
async function save() {
  await api.saveTask(form);
  dialog.value = false;
  ElMessage.success("任务已保存");
  load();
}
async function status(t: Task, s: string) {
  await api.status(t.id, s, t.version);
  load();
}
function editMilestone(item?: Milestone) {
  Object.assign(
    milestoneForm,
    item ?? { id: undefined, name: "", status: "pending", dueDate: "" },
  );
  milestoneDialog.value = true;
}
async function saveMilestone() {
  await api.saveMilestone(id, {
    ...milestoneForm,
    dueDate: milestoneForm.dueDate || null,
  });
  milestoneDialog.value = false;
  ElMessage.success("里程碑已保存");
  load();
}
async function removeMilestone(item: Milestone) {
  await ElMessageBox.confirm(`删除里程碑“${item.name}”？`, "确认删除", {
    type: "warning",
  });
  await api.deleteMilestone(id, item.id);
  ElMessage.success("里程碑已删除");
  load();
}
async function upload(options: any) {
  try {
    await api.uploadAttachment("project", id, options.file);
    options.onSuccess();
    ElMessage.success("附件已上传");
    load();
  } catch (error) {
    options.onError(error);
  }
}
async function removeAttachment(item: any) {
  await api.deleteAttachment(item.id);
  ElMessage.success("附件已删除");
  load();
}
function canDeleteAttachment(item: any) {
  return canManageProject.value || item.uploaderId === auth.user?.id;
}
</script>
<template>
  <div class="page" v-if="project">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ project.name }}</h1>
        <p>{{ project.code }} · {{ project.status }}</p>
      </div>
      <el-button v-if="canManageProject" type="primary" @click="edit()"
        >新增任务</el-button
      >
    </div>
    <div class="project-links">
      <el-button plain @click="router.push(`/projects/${id}/members`)"
        >成员管理</el-button
      ><el-button plain @click="router.push(`/projects/${id}/extras`)"
        >文档与风险</el-button
      ><el-button plain @click="router.push(`/projects/${id}/dependencies`)"
        >任务依赖</el-button
      ><el-button plain @click="router.push(`/projects/${id}/board`)"
        >看板排序</el-button
      ><el-button plain @click="router.push(`/projects/${id}/gantt`)"
        >甘特视图</el-button
      ><el-button plain @click="router.push(`/projects/${id}/attachments`)"
        >附件管理</el-button
      >
    </div>
    <section class="card">
      <el-progress :percentage="Number(project.progress)" />
      <h3>项目看板</h3>
      <div class="board">
        <div
          v-for="column in ['todo', 'in_progress', 'review', 'done']"
          :key="column"
          class="column"
        >
          <strong>{{ column }}</strong>
          <div
            v-for="task in tasks.filter((t) => t.status === column)"
            :key="task.id"
            class="task"
          >
            <b>{{ task.title }}</b
            ><span>{{ task.priority }} · {{ task.progress }}%</span>
            <div>
              <el-button
                v-if="task.status !== 'done' && canUpdateTask(task)"
                size="small"
                @click="
                  status(task, task.status === 'todo' ? 'in_progress' : 'done')
                "
                >推进</el-button
              ><el-button size="small" link @click="edit(task)">详情</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="card">
      <div class="section-title">
        <h3>里程碑</h3>
        <el-button
          v-if="canManageProject"
          type="primary"
          size="small"
          @click="editMilestone()"
          >新增里程碑</el-button
        >
      </div>
      <el-empty
        v-if="!milestones.length"
        description="暂无里程碑"
        :image-size="56"
      /><el-table v-else :data="milestones"
        ><el-table-column prop="name" label="名称" /><el-table-column
          prop="dueDate"
          label="截止时间"
        /><el-table-column prop="status" label="状态" /><el-table-column
          v-if="canManageProject"
          label="操作"
          width="130"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="editMilestone(row)"
              >编辑</el-button
            ><el-button link type="danger" @click="removeMilestone(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <section class="card">
      <h3>甘特数据</h3>
      <el-empty
        v-if="!gantt.length"
        description="暂无排期任务"
        :image-size="56"
      /><el-table v-else :data="gantt"
        ><el-table-column prop="title" label="任务" /><el-table-column
          prop="startDate"
          label="开始"
        /><el-table-column prop="dueDate" label="截止" /><el-table-column
          prop="progress"
          label="进度"
          ><template #default="{ row }"
            >{{ row.progress }}%</template
          ></el-table-column
        ><el-table-column label="依赖"
          ><template #default="{ row }">{{
            row.dependsOn.length ? row.dependsOn.join(", ") : "-"
          }}</template></el-table-column
        ></el-table
      >
    </section>
    <section class="card">
      <div class="section-title">
        <h3>项目附件</h3>
        <el-upload
          :show-file-list="false"
          :http-request="upload"
          :limit="1"
          :on-exceed="() => ElMessage.warning('单次仅可上传一个文件')"
          ><el-button type="primary" size="small">上传附件</el-button
          ><template #tip
            ><div class="el-upload__tip">
              任意格式，单文件不超过 2 MB
            </div></template
          ></el-upload
        >
      </div>
      <el-empty
        v-if="!attachments.length"
        description="暂无附件"
        :image-size="56"
      /><el-table v-else :data="attachments"
        ><el-table-column prop="name" label="文件名" /><el-table-column
          prop="contentType"
          label="类型"
        /><el-table-column prop="size" label="大小"
          ><template #default="{ row }"
            >{{ (row.size / 1024).toFixed(1) }} KB</template
          ></el-table-column
        ><el-table-column label="操作" width="80"
          ><template #default="{ row }"
            ><el-button
              v-if="canDeleteAttachment(row)"
              link
              type="danger"
              @click="removeAttachment(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <el-dialog v-model="dialog" title="任务"
      ><el-form label-width="90"
        ><el-form-item label="标题"
          ><el-input
            v-model="form.title"
            :disabled="!canManageProject" /></el-form-item
        ><el-form-item label="负责人"
          ><el-select
            v-model="form.assigneeId"
            clearable
            :disabled="!canManageProject"
            ><el-option
              v-for="u in users"
              :key="u.id"
              :label="u.displayName"
              :value="u.id" /></el-select></el-form-item
        ><el-form-item label="优先级"
          ><el-select v-model="form.priority" :disabled="!canManageProject"
            ><el-option label="低" value="low" /><el-option
              label="中"
              value="medium" /><el-option label="高" value="high" /><el-option
              label="紧急"
              value="urgent" /></el-select></el-form-item
        ><el-form-item label="预估工时"
          ><el-input-number
            v-model="form.estimatedHours"
            :min="0"
            :disabled="!canManageProject" /></el-form-item
        ><el-form-item label="执行进度"
          ><el-slider
            v-model="form.progress"
            :disabled="
              !canManageProject && form.assigneeId !== auth.user?.id
            " /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button
          v-if="canManageProject || form.assigneeId === auth.user?.id"
          type="primary"
          @click="save"
          >保存</el-button
        ></template
      ></el-dialog
    ><el-dialog v-model="milestoneDialog" title="里程碑"
      ><el-form label-width="80"
        ><el-form-item label="名称"
          ><el-input v-model="milestoneForm.name" /></el-form-item
        ><el-form-item label="截止时间"
          ><el-date-picker
            v-model="milestoneForm.dueDate"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item
        ><el-form-item label="状态"
          ><el-select v-model="milestoneForm.status"
            ><el-option label="待开始" value="pending" /><el-option
              label="进行中"
              value="in_progress" /><el-option
              label="已完成"
              value="completed" /></el-select></el-form-item></el-form
      ><template #footer
        ><el-button @click="milestoneDialog = false">取消</el-button
        ><el-button type="primary" @click="saveMilestone"
          >保存</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<style scoped>
.board {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.column {
  background: #f1f5f9;
  padding: 12px;
  border-radius: 6px;
  min-height: 300px;
}
.task {
  margin-top: 10px;
  padding: 12px;
  background: #fff;
  border-radius: 5px;
  box-shadow: 0 1px 2px #0001;
}
.task span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin: 6px 0;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.project-links {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
