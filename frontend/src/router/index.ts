import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router=createRouter({history:createWebHistory(),routes:[
  {path:'/login',component:()=>import('../views/LoginView.vue'),meta:{public:true}},
  {path:'/',component:()=>import('../layouts/MainLayout.vue'),children:[
    {path:'',redirect:'/dashboard'}, {path:'dashboard',component:()=>import('../views/DashboardView.vue')},
    {path:'customers',component:()=>import('../views/CustomerView.vue'),meta:{roles:['ADMIN','PM']}},
    {path:'projects',component:()=>import('../views/ProjectView.vue')}, {path:'projects/:id',component:()=>import('../views/ProjectDetailView.vue')}, {path:'projects/:id/extras',component:()=>import('../views/ProjectExtrasView.vue')}, {path:'projects/:id/members',component:()=>import('../views/ProjectMembersView.vue')}, {path:'projects/:id/dependencies',component:()=>import('../views/TaskDependenciesView.vue')}, {path:'projects/:id/board',component:()=>import('../views/ProjectBoardView.vue')},
    {path:'tasks',component:()=>import('../views/TaskView.vue')}, {path:'work-logs',component:()=>import('../views/WorkLogView.vue')},
    {path:'users',component:()=>import('../views/UserView.vue'),meta:{roles:['ADMIN']}}, {path:'organization',component:()=>import('../views/OrganizationView.vue'),meta:{roles:['ADMIN']}}, {path:'audit',component:()=>import('../views/AuditView.vue'),meta:{roles:['ADMIN']}}, {path:'notifications',component:()=>import('../views/NotificationView.vue')}, {path:'profile',component:()=>import('../views/ProfileView.vue')}
  ]}, {path:'/:pathMatch(.*)*',redirect:'/dashboard'}
]})
router.beforeEach(async to=>{const auth=useAuthStore();if(!auth.user&&localStorage.getItem('pmtool-token'))try{await auth.load()}catch{auth.logout()}if(!to.meta.public&&!auth.user)return '/login';const roles=to.meta.roles as string[]|undefined;if(roles&&!roles.includes(auth.user?.roleCode??''))return '/dashboard';return true})
export default router
