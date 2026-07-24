export interface ApiResponse<T> { code: number; message: string; data: T }
export interface PageResult<T> { items: T[]; total: number; page: number; pageSize: number }
export interface User { id: number; username: string; displayName: string; roleCode: 'ADMIN' | 'PM' | 'MEMBER'; departmentId: number; enabled: boolean }
export interface Department { id:number; name:string; parentId:number }
export interface Role { id:number; code:string; name:string; description:string; permissions?:string[] }
export interface Project { id:number; name:string; code:string; managerId:number; customerId:number; status:string; progress:number; version:number }
export interface Milestone { id:number; projectId:number; name:string; dueDate:string; status:string }
export interface GanttTask { id:number; title:string; startDate:string; dueDate:string; progress:number; status:string; dependsOn:number[] }
export interface Task { id:number; projectId:number; title:string; description?:string; assigneeId:number; status:string; priority:string; estimatedHours:number; progress:number; sortOrder:number; startDate?:string; dueDate?:string; version:number }
export interface Customer { id:number; name:string; level:string; status:string; contactName:string; phone:string }
