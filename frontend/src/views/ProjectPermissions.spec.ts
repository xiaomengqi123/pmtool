import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "../stores/auth";
import ProjectDetailView from "./ProjectDetailView.vue";
import ProjectView from "./ProjectView.vue";
import TaskView from "./TaskView.vue";
import WorkLogView from "./WorkLogView.vue";

const api = vi.hoisted(() => ({
  projects: vi.fn(),
  project: vi.fn(),
  projectTasks: vi.fn(),
  tasks: vi.fn(),
  workLogs: vi.fn(),
  users: vi.fn(),
  milestones: vi.fn(),
  gantt: vi.fn(),
  attachments: vi.fn(),
}));

vi.mock("../api", () => ({ api }));
vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { id: "1" } }),
  useRouter: () => ({ push: vi.fn() }),
}));

const stubs = {
  "el-button": { template: "<button><slot /></button>" },
  "el-table": { template: "<section><slot /></section>" },
  "el-table-column": { template: '<section><slot :row="{}" /></section>' },
  "el-progress": true,
  "el-tag": true,
  "el-empty": true,
  "el-upload": { template: "<section><slot /></section>" },
  "el-dialog": {
    template: '<section><slot /><slot name="footer" /></section>',
  },
  "el-form": { template: "<form><slot /></form>" },
  "el-form-item": { template: "<section><slot /></section>" },
  "el-input": true,
  "el-select": { template: "<section><slot /></section>" },
  "el-option": true,
  "el-input-number": true,
  "el-slider": true,
  "el-date-picker": true,
  "el-pagination": true,
};

function buttons(wrapper: ReturnType<typeof mount>, label: string) {
  return wrapper.findAll("button").filter((button) => button.text() === label);
}

describe("project permission entries", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    api.projects.mockResolvedValue({
      items: [
        { id: 1, name: "项目 A", code: "A-1", status: "planning", progress: 0 },
      ],
    });
    api.project.mockResolvedValue({
      id: 1,
      name: "项目 A",
      code: "A-1",
      status: "planning",
      progress: 0,
    });
    api.projectTasks.mockResolvedValue([
      {
        id: 11,
        projectId: 1,
        title: "我的任务",
        assigneeId: 2,
        status: "todo",
        priority: "medium",
        progress: 0,
        version: 1,
      },
      {
        id: 12,
        projectId: 1,
        title: "他人任务",
        assigneeId: 3,
        status: "todo",
        priority: "medium",
        progress: 0,
        version: 1,
      },
    ]);
    api.tasks.mockResolvedValue({
      items: [
        {
          id: 11,
          projectId: 1,
          title: "我的任务",
          assigneeId: 2,
          status: "todo",
          priority: "medium",
          progress: 0,
          version: 1,
        },
        {
          id: 12,
          projectId: 1,
          title: "他人任务",
          assigneeId: 3,
          status: "todo",
          priority: "medium",
          progress: 0,
          version: 1,
        },
      ],
    });
    api.workLogs.mockResolvedValue({ items: [] });
    api.users.mockResolvedValue([]);
    api.milestones.mockResolvedValue([]);
    api.gantt.mockResolvedValue([]);
    api.attachments.mockResolvedValue([]);
  });

  it("hides project editing and another members task transition from a member", async () => {
    useAuthStore().user = {
      id: 2,
      username: "member",
      displayName: "成员",
      roleCode: "MEMBER",
      departmentId: 0,
      enabled: true,
    };
    const project = mount(ProjectView, { global: { stubs } });
    const detail = mount(ProjectDetailView, { global: { stubs } });
    await flushPromises();

    expect(buttons(project, "编辑")).toHaveLength(0);
    expect(buttons(detail, "推进")).toHaveLength(1);
    expect(detail.text()).toContain("我的任务");
    expect(detail.text()).toContain("他人任务");
  });

  it("shows project editing and all task transitions to a project manager", async () => {
    useAuthStore().user = {
      id: 1,
      username: "manager",
      displayName: "经理",
      roleCode: "PM",
      departmentId: 0,
      enabled: true,
    };
    const project = mount(ProjectView, { global: { stubs } });
    const detail = mount(ProjectDetailView, { global: { stubs } });
    await flushPromises();

    expect(buttons(project, "编辑")).toHaveLength(1);
    expect(buttons(detail, "推进")).toHaveLength(2);
  });

  it("only allows a member to save their own task from the global task list", async () => {
    useAuthStore().user = {
      id: 2,
      username: "member",
      displayName: "成员",
      roleCode: "MEMBER",
      departmentId: 0,
      enabled: true,
    };
    const task = mount(TaskView, { global: { stubs } });
    await flushPromises();

    (task.vm as any).edit({ assigneeId: 3 });
    await task.vm.$nextTick();
    expect(buttons(task, "保存")).toHaveLength(0);
    (task.vm as any).edit({ assigneeId: 2 });
    await task.vm.$nextTick();
    expect(buttons(task, "保存")).toHaveLength(1);
  });

  it("only exposes worklog management actions for the record's project manager", async () => {
    useAuthStore().user = {
      id: 2,
      username: "manager",
      displayName: "项目经理",
      roleCode: "PM",
      departmentId: 0,
      enabled: true,
    };
    const workLog = mount(WorkLogView, { global: { stubs } });
    await flushPromises();

    expect(
      (workLog.vm as any).canEdit({
        userId: 3,
        status: "approved",
        canManage: false,
      }),
    ).toBe(false);
    expect(
      (workLog.vm as any).canEdit({
        userId: 3,
        status: "approved",
        canManage: true,
      }),
    ).toBe(true);
  });
});
