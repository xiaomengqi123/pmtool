import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import DashboardView from './DashboardView.vue'

const { dashboard } = vi.hoisted(() => ({ dashboard: vi.fn() }))
vi.mock('../api', () => ({ api: { dashboard } }))

describe('DashboardView', () => {
  it('loads and displays dashboard aggregates', async () => {
    dashboard.mockResolvedValue({ projectTotal: 2, taskTotal: 8, taskDone: 3, taskCompletion: 38, recentProjects: [], recentTasks: [], statusDistribution: { todo: 2, in_progress: 3, review: 1, done: 2 }, worklogTrend: [{ date: '2026-07-24', hours: 4 }], memberLoad: [], recentMilestones: [] })
    const wrapper = mount(DashboardView, { global: { stubs: ['el-table', 'el-table-column', 'el-progress'] } })
    await flushPromises()
    expect(dashboard).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('项目总数')
    expect(wrapper.text()).toContain('8')
    expect(wrapper.text()).toContain('近 7 天已审批工时')
  })
})
