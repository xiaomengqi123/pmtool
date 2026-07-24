import http from './http'
import type { AxiosResponse } from 'axios'
import type { Customer, GanttTask, Milestone, PageResult, Project, Task, User } from '../types'
const unwrap = <T>(request: Promise<AxiosResponse<{ data: T }>>) => request.then(response => response.data.data)
export const api = {
  login: (username:string,password:string) => unwrap<{token:string;user:User}>(http.post('/auth/login',{username,password})),
  me: () => unwrap<User>(http.get('/auth/info')),
  dashboard: () => unwrap<Record<string, unknown>>(http.get('/dashboard')),
  users: () => unwrap<User[]>(http.get('/users/all')), createUser:(data:Partial<User>&{password:string})=>unwrap(http.post('/users',data)),
  customers:(page=1,pageSize=20)=>unwrap<PageResult<Customer>>(http.get('/customers',{params:{page,pageSize}})), saveCustomer:(data:Partial<Customer>)=>data.id?unwrap(http.put(`/customers/${data.id}`,data)):unwrap(http.post('/customers',data)),
  projects:(page=1,pageSize=20)=>unwrap<PageResult<Project>>(http.get('/projects',{params:{page,pageSize}})), project:(id:number)=>unwrap<Project>(http.get(`/projects/${id}`)), saveProject:(data:Partial<Project>)=>data.id?unwrap(http.put(`/projects/${data.id}`,data)):unwrap(http.post('/projects',data)),
  milestones:(projectId:number)=>unwrap<Milestone[]>(http.get(`/projects/${projectId}/milestones`)), saveMilestone:(projectId:number,data:Partial<Milestone>)=>data.id?unwrap(http.put(`/projects/${projectId}/milestones/${data.id}`,data)):unwrap(http.post(`/projects/${projectId}/milestones`,data)),
  gantt:(projectId:number)=>unwrap<GanttTask[]>(http.get(`/projects/${projectId}/gantt`)),
  projectTasks:(id:number)=>unwrap<Task[]>(http.get(`/projects/${id}/tasks`)), tasks:(page=1,pageSize=20)=>unwrap<PageResult<Task>>(http.get('/tasks',{params:{page,pageSize}})), saveTask:(data:Partial<Task>)=>data.id?unwrap(http.put(`/tasks/${data.id}`,data)):unwrap(http.post('/tasks',data)), status:(id:number,status:string,version:number)=>unwrap(http.patch(`/tasks/${id}/status`,{status,version})),
  workLogs:(page=1,pageSize=20)=>unwrap<PageResult<any>>(http.get('/work-logs',{params:{page,pageSize}})), saveWorkLog:(data:any)=>data.id?unwrap(http.put(`/work-logs/${data.id}`,data)):unwrap(http.post('/work-logs',data)), review:(id:number,approved:boolean,comment?:string)=>unwrap(http.post(`/work-logs/${id}/${approved?'approve':'reject'}`,comment?{comment}:{})),
  notifications:()=>unwrap<any[]>(http.get('/notifications')), unread:()=>unwrap<{count:number}>(http.get('/notifications/unread-count')), read:(id:number)=>unwrap(http.post(`/notifications/${id}/read`))
}
